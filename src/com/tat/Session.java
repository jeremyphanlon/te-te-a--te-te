package com.tat;

import java.util.ArrayList;
import java.util.List;

public class Session {

	private String token;
	private String address;
	private long creationTime;
	private List<Message> messages = new ArrayList<Message>();
	private double latitude, longitude;
	private boolean validLocation = false;
	
	public Session(String token, String address) {
		this.token = token;
		this.address = address;
		this.creationTime = System.currentTimeMillis();
	}
	
	public void addMessage(Message m) {
		messages.add(m);
	}
	
	public List<Message> getMessages() {
		return messages;
	}
	
	public String getToken() {
		return token;
	}
	
	public String getAddress() {
		return address;
	}
	
	public boolean isExpired() {
		return (System.currentTimeMillis() - creationTime) > 60*60*1000; // Session expires after 1 hour (= 60*60*1000 milliseconds)
	}

	public void setLocation(double latitude, double longitude) {
		this.latitude = latitude;
		this.longitude = longitude;
		this.validLocation = true;
	}
	
	public double getLatitude() {
		return latitude;
	}
	
	public double getLongitude() {
		return longitude;
	}

	public int distanceTo(Session other) {
		if (other.validLocation) {
			int R = 6371000; // meters
			double lat1 = Math.toRadians(this.getLatitude());
			double lat2 = Math.toRadians(other.getLatitude());
			double long1 = Math.toRadians(this.getLongitude());
			double long2 = Math.toRadians(other.getLongitude());
			double dLat = Math.toRadians(other.getLatitude() - this.getLatitude());
			double dLong = Math.toRadians(other.getLongitude() - this.getLongitude());
			
			double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
			        Math.cos(lat1) * Math.cos(lat2) *
			        Math.sin(dLong/2) * Math.sin(dLong/2);
			double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));

			double kmDistance = R * c;
			
			double miDistance = kmDistance * 0.621371;
			
			return (int) Math.ceil(miDistance);
		}
		else {
			return Integer.MAX_VALUE;
		}
	}

	public int getDistanceThreshold() {
		return 100;// Miles (maybe make this user adjustable later)
	}
	
}
