package com.tat;

import java.io.IOException;

public class Main {

	public static void main(String[] args) throws IOException {
		startServer();
	}
	
	private static void startServer() throws IOException {
		new Server().start();
		System.out.println("[Server Running]");
	}
	
}
