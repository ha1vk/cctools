package com.pdaxrom.cmd;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.UnknownHostException;

public class dialog {
	public static void main(String[] args) {
		String rCmd = "";
		for (String s: args) {
			rCmd += s + "\n";
		}
		//System.out.println("Hello world! " + rCmd);
		try {
			Socket clientSocket = new Socket("localhost", 13527);
			DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
			BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
			outToServer.writeBytes(rCmd);
			System.out.println(inFromServer.readLine());
			clientSocket.close();
		} catch (UnknownHostException e) {
			System.err.println("Unknown host!");
		} catch (IOException e) {
			System.err.println("IO exception " + e);
		}
	}
}
