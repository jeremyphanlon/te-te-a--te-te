package com.tat;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.Random;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;


public class Server {

	public void start() throws IOException {
		HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);
        server.createContext("/", new RequestHandler());
        server.setExecutor(null); // creates a default executor
        server.start();
	}
	
	private static class RequestHandler implements HttpHandler {
		
		private static java.util.ArrayList<Session> SESSIONS = new java.util.ArrayList<Session>();
    	private static Random rand = new Random();
		
        @Override
        public void handle(HttpExchange t) throws IOException {

        	
            InputStream in = t.getRequestBody();
            ByteArrayOutputStream _out = new ByteArrayOutputStream();
            byte[] buf = new byte[2048];
            int read = 0;
            while ((read = in.read(buf)) != -1) {
                _out.write(buf, 0, read);
            }
            
            byte[] data = _out.toByteArray();
            
            if (data.length == 0) {
            	Headers headers = t.getResponseHeaders();
            	headers.add("Content-Type", "text/html");
            	@SuppressWarnings("resource")
				String response = new java.util.Scanner(Server.class.getResourceAsStream("index.html")).useDelimiter("\\Z").next();
                t.sendResponseHeaders(200, response.length());
                OutputStream os = t.getResponseBody();
                os.write(response.getBytes());
                os.close();
            }
            
        	Headers headers = t.getResponseHeaders();
        	headers.add("Content-Type", "application/json");
            
            JSONObject responseObj = handleRequest(data);
        	
        	String response = responseObj.toJSONString();
        	
            t.sendResponseHeaders(200, response.length());
            OutputStream os = t.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
        
    	private JSONObject handleRequest(byte[] data) {
    		String jsonString = new String(data);
    		JSONParser parser = new JSONParser();
    	      try{
    	         JSONObject obj = (JSONObject) parser.parse(jsonString);
    	 		 return handleRequest(obj);
    	      }catch(ParseException pe){
    			
    	         //System.out.println("position: " + pe.getPosition());
    	         //System.out.println(pe);
    	         return null;
    	      }
    	}
    	
    	private JSONObject handleRequest(JSONObject request) {
    		JSONObject response = new JSONObject();
    		switch((String)request.get("type")) {
    			case "geolocation":
    				String token = (String) request.get("token");
    				double latitude = (double) request.get("latitude");
    				double longitude = (double) request.get("longitude");
    				
    				for (Session s : SESSIONS) {
    					if (s.getToken().equals(token)) {
    						s.setLocation(latitude, longitude);
    						break;
    					}
    				}
    				break;
    			case "get_nearby_users":
    				token = (String) request.get("token");
    				
    				if (token == null) {
    					response.put("error", "No 'token' field provided in request.");
    					response.put("success", new Boolean(false));
    					break;
    				}
    				
    				JSONArray nearbyUsers = new JSONArray();
    			
    				boolean foundSession = false;
    				
    				for (Session s : SESSIONS) {
    					if (s.getToken().equals(token)) {
    						foundSession = true;
    	    				for (Session other : SESSIONS) {
    	    					
    	    					if (other.getToken().equals(token)) {
    	    						continue;
    	    					}
    	    					
    	    					int milesAway = s.distanceTo(other);
    	    					if (milesAway < s.getDistanceThreshold()) {
    	    						JSONObject nearbyUser = new JSONObject();
    	    						nearbyUser.put("address", other.getAddress());
    	    						nearbyUser.put("distance", milesAway);
    	    						nearbyUsers.add(nearbyUser);
    	    					}
    	    				}
    	    				break;
    					}
    				}
    				
    				if (foundSession) {
        				response.put("success", new Boolean(true));
        				response.put("nearby_users", nearbyUsers);
    				}
    				else {
    					response.put("success", new Boolean(false));
    					response.put("error", "Session does not exist or has expired.");
    				}
    				
    				break;
    			case "get_session":
    				response.put("success", true);
    				
    				token = generateSessionToken();
    				String address = generateSessionAddress();
    				
    				SESSIONS.add(new Session(token, address));
    				
    				response.put("session_token", token);
    				response.put("session_address", address); // The address you give to others to put in the 'to' field of their send requests
    				break;
    			case "send":
    				address = (String) request.get("to");
    				String message = (String) request.get("content");
    				
    				if (address == null) {
    					response.put("success", new Boolean(false));
    					response.put("error", "No 'to' field provided in request.");
    					break;
    				}
    				if (message == null) {
    					response.put("success", new Boolean(false));
    					response.put("error", "No 'content' field provided in request.");
    					break;
    				}
    				
    				boolean foundRecipient = false;
    				
    				for (Session s : SESSIONS) {
    					if (s.getAddress().equals(address)) {
    						
    						if (s.isExpired()) {
    							SESSIONS.remove(s);
    							break;
    						}
    						
    						s.addMessage(new Message(message));
    						foundRecipient = true;
    						break;
    					}
    				}
    				
    				if (foundRecipient) {
						response.put("success", new Boolean(true));
    				}
    				else {
    					response.put("success", new Boolean(false));
    					response.put("error", "Recipient does not exist or has expired.");
    				}
    				
    				break;
    			case "get_messages":
    				token = (String) request.get("token");
    				
    				if (token == null) {
    					response.put("success", new Boolean(false));
    					response.put("error", "No 'token' field provided in request.");
    					break;
    				}
    				
    				foundSession = false;
    				
    				for (Session s : SESSIONS) {
    					if (s.getToken().equals(token)) {
    						
    						if (s.isExpired()) {
    							SESSIONS.remove(s);
    							break;
    						}

    						foundSession = true;
    						JSONArray messages = new JSONArray();
    						for (Message m : s.getMessages()) {
    							messages.add(m.getContent());
    						}
    						response.put("messages", messages);
    						break;
    					}
    				}
    				
    				if (foundSession) {
    					response.put("success", new Boolean(true));
    				}
    				else {
    					response.put("success", new Boolean(false));
    					response.put("error", "Session does not exist or has expired.");
    				}
    				
    				break;
    		}
    		
    		return response;
    	}

		private String generateSessionAddress() {
			Long address = null;
			while (address == null) {
				address = rand.nextLong();
				
				if (address < 0) {
					address = null;
					continue;
				}
				
				String stringAddress = address.toString();
				
				boolean unique = true;
				
				for (Session s : SESSIONS) {
					if (s.getAddress().equals(stringAddress)) {
						unique = false;
						break;
					}
				}
				
				if (!unique) {
					address = null;
				}
				
			}
			
			return address.toString();
		}

		private String generateSessionToken() {
			Long token = null;
			while (token == null) {
				token = rand.nextLong();
				
				if (token < 0) {
					token = null;
					continue;
				}
				
				String stringToken = token.toString();
				
				boolean unique = true;
				
				for (Session s : SESSIONS) {
					if (s.getToken().equals(stringToken)) {
						unique = false;
						break;
					}
				}
				
				if (!unique) {
					token = null;
				}
				
			}
			
			return token.toString();
		}
    }
	
}
