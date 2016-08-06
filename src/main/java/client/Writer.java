package client;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;

import util.Util;
import util.Util.AllProperties;

class Writer {
	boolean writerRunning = false;
	public static void main(String argv[]) throws Exception {
		Writer wr = new Writer();
		wr.run();
	}
	
	public void run() throws UnknownHostException, IOException {
		AllProperties props = Util.readProperties();
		String sentence;
		String modifiedSentence;
		//BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
		Socket clientSocket = new Socket("localhost", props.writerPort);

		OutputStream oStream = clientSocket.getOutputStream();
		DataOutputStream outToServer = new DataOutputStream(oStream);
		outToServer.write(0);
		outToServer.flush();

		DataInputStream inFromServer = new DataInputStream(clientSocket.getInputStream());
		this.writerRunning = true;
		while(writerRunning) {
			int offset = inFromServer.readInt();
			int number = inFromServer.readInt();
			for (int i = offset; i < offset+number; i++) {
				outToServer.writeInt(i);
			}
		}
		clientSocket.close();
	}
}
