package client;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.perf4j.StopWatch;
import org.perf4j.log4j.Log4JStopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import util.Util;
import util.Util.AllProperties;

public class Reader {
	private static final Logger LOG = LoggerFactory.getLogger(Reader.class);
	long restartTime = System.currentTimeMillis();
	boolean shutdown = false;
	StopWatch stopWatch = new Log4JStopWatch("reader");

	public static void main(String argv[]) throws Exception {
		Reader r = new Reader();
		r.run();
	}

	public void run() throws UnknownHostException, IOException {
		AllProperties props = Util.readProperties();
		ExecutorService executor = Executors.newFixedThreadPool(props.numReaders + 1);
		for (int i = 0; i < props.numReaders; i++) {
			Server cli = new Server(i, restartTime);
			ServerTask task = new ServerTask(cli, props);
			executor.submit(task);
		}
	}

	class Server {
		public int threadId;
		public long restartTime;

		public Server(int threadId, long startTime) {
			super();
			this.threadId = threadId;
			this.restartTime = startTime;
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
			LOG.info("Started Reader thread:" + server.threadId);
			while (!Reader.this.shutdown) {
				Socket connectionSocket = null;
				DataInputStream inFromServer = null;
				DataOutputStream outToServer = null;
				server.restartTime = Reader.this.restartTime;
				while (true) {
					try {
						LOG.info("Reader connecting:" + server.threadId);
						connectionSocket = new Socket("localhost", props.readerPort);
						LOG.info("Reader Accepted on thread:" + server.threadId);
						inFromServer = new DataInputStream(connectionSocket.getInputStream());
						outToServer = new DataOutputStream(connectionSocket.getOutputStream());
						break;
					} catch (Exception ex) {
						LOG.error("Reader connect failed", ex);
						try {
							Thread.sleep(props.connectionRetryTimeout);
						} catch (InterruptedException e) {
							LOG.error("Sleep interrupted", e);
						}
					}

				}
				try {
					while (Reader.this.restartTime <= server.restartTime) {
						UUID uid = UUID.randomUUID();
						outToServer.writeUTF(uid.toString());
						outToServer.writeByte(0);

						List<Integer> sequence = new LinkedList<Integer>();
						while (Reader.this.restartTime <= server.restartTime) {
							stopWatch.start();
							String uidS = inFromServer.readUTF();
							UUID uidFromServer = UUID.fromString(uidS);
							// TODO wrong uid
							int val = inFromServer.readInt();
							if (val == -1) {
								LOG.info("Reader sequence finished:[" + uidS + "," + val + "]");
								LOG.info("Sequence:"+Arrays.toString(sequence.toArray()));
								break;
							} else {
								sequence.add(val);
								LOG.debug("Reader sequence value   :[" + uidS + "," + val + "]");
							}
							stopWatch.stop();
						}
					}
					if(Reader.this.restartTime > server.restartTime) {
						LOG.info("Resterting all Readers"+server.threadId);
					}
					

				} catch (Exception ex) {
					Reader.this.restartTime = System.currentTimeMillis();
					LOG.error("Reader communication failed", ex);
					try {
						connectionSocket.close();
					} catch (IOException e) {
						LOG.error("Socket close on error failed", e);
					}
				}

			}

			return this.server;
		}

	}

}
