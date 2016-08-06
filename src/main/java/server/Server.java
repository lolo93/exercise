package server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import util.Util;
import util.Util.AllProperties;

class Server {
	boolean serverRunning = false;
	Object writerLock = new Object();
	DataInputStream writerIn;
	DataOutputStream writerOut;

	public static void main(String argv[]) throws Exception {
		Server serve = new Server();
		serve.run();
	}

	public void run() throws IOException {
		AllProperties props = Util.readProperties();
		ServerSocket readerSocket = new ServerSocket(props.readerPort);
		ServerSocket writerSocket = new ServerSocket(props.writerPort);

		ExecutorService executor = Executors.newFixedThreadPool(props.numReaders + 1);
		serverRunning = true;

		for (int i = 0; i < props.numReaders + 1; i++) {
			Reader cli = new Reader(i);
			ReaderTask task = new ReaderTask(cli, readerSocket);
			executor.submit(task);
		}

		Socket writerClientSocket = writerSocket.accept();
		writerIn = new DataInputStream(writerClientSocket.getInputStream());
		writerOut = new DataOutputStream(writerClientSocket.getOutputStream());
		if (writerIn.read() == 0) {
			System.out.println("Writer connection");
		} else {
			System.out.println("Writer reconnect");
			// TODO reconnect
		}
	}

	static class Reader {
		public int threadId;

		public Reader(int threadId) {
			super();
			this.threadId = threadId;
		}
	}
	
	List<Integer> getSequence() throws IOException {
		List<Integer> toReturn = new LinkedList<Integer>(); 
		synchronized(writerLock) {
			writerOut.writeInt(0);
			writerOut.writeInt(10);
			for (int i = 0; i < 10; i++) {
				int val = writerIn.readInt();
				toReturn.add(val);
			}
		}
		return toReturn;
	}

	class ReaderTask implements Callable<Reader> {
		Reader client;
		ServerSocket welcomeSocket;

		ReaderTask(Reader client, ServerSocket welcomeSocket) {
			this.client = client;
			this.welcomeSocket = welcomeSocket;
		}

		@Override
		public Reader call() throws Exception {
			String clientSentence;
			String capitalizedSentence;
			Socket connectionSocket = null;
			System.out.println("Started thread:" + client.threadId);
			while (Server.this.serverRunning) {
				synchronized (welcomeSocket) {
					connectionSocket = welcomeSocket.accept();
					System.out.println("Accepted on thread:" + client.threadId);
				}
				DataInputStream inFromClient = new DataInputStream(connectionSocket.getInputStream());
				DataOutputStream outToClient = new DataOutputStream(connectionSocket.getOutputStream());
				while (Server.this.serverRunning) {
					String uidS = inFromClient.readUTF();
					UUID uid = UUID.fromString(uidS);
					if (inFromClient.readByte() == 0) {
						List<Integer> sequence = Server.this.getSequence();
						for (Integer integer : sequence) {
							outToClient.writeUTF(uidS);
							outToClient.writeInt(integer);
						}
						outToClient.writeUTF(uidS);
						outToClient.writeInt(-1);
						outToClient.flush();
					} else {
						// TODO implement
					}
				}

			}
			return this.client;
		}

	}

}
