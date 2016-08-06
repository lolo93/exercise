package client;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import util.Util;
import util.Util.AllProperties;

public class Reader {
	boolean readerRunning = false;

	public static void main(String argv[]) throws Exception {
		Reader r = new Reader();
		r.run();
	}

	public void run() throws UnknownHostException, IOException {
		AllProperties props = Util.readProperties();
		ExecutorService executor = Executors.newFixedThreadPool(props.numReaders + 1);
		readerRunning = true;

		for (int i = 0; i < props.numReaders + 1; i++) {
			Server cli = new Server(i);
			ServerTask task = new ServerTask(cli, props);
			executor.submit(task);
		}

	}

	class Server {
		public int threadId;

		public Server(int threadId) {
			super();
			this.threadId = threadId;
		}
	}

	class ServerTask implements Callable<Server> {
		Server server;
		AllProperties props;

		ServerTask(Server server, AllProperties props) {
			this.server = server;
			this.props = props;
		}

		@Override
		public Server call() throws Exception {
			String clientSentence;
			String capitalizedSentence;
			Socket connectionSocket = null;
			System.out.println("Started Reader thread:" + server.threadId);
			connectionSocket = new Socket("localhost", props.readerPort);
			System.out.println("Reader Accepted on thread:" + server.threadId);
			DataInputStream inFromServer = new DataInputStream(connectionSocket.getInputStream());
			DataOutputStream outToServer = new DataOutputStream(connectionSocket.getOutputStream());
			while (Reader.this.readerRunning) {
				UUID uid = UUID.randomUUID();
				outToServer.writeUTF(uid.toString());
				outToServer.writeByte(0);
				
				while(true) {
					String uidS = inFromServer.readUTF();
					UUID uidFromServer = UUID.fromString(uidS);
					//TODO wrong uid
					int val = inFromServer.readInt();
					if(val==-1) {
						System.out.println("Reader sequence finished:["+uidS+","+val+"]");
						break;
					} else {
						System.out.println("Reader sequence value   :["+uidS+","+val+"]");
					}
				}
			}
			return this.server;
		}

	}

}
