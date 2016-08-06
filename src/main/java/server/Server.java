package server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;

import org.perf4j.LoggingStopWatch;
import org.perf4j.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import util.Util;
import util.Util.AllProperties;

class Server {
	private static final Logger LOG = LoggerFactory.getLogger(Server.class);
	boolean serverRunning = false;
	Object writerLock = new Object();
	DataInputStream writerIn;
	DataOutputStream writerOut;
	AtomicInteger numberOfConcurrentReaderRequests = new AtomicInteger(0);
	AtomicInteger numberOfConcurrentWriterRequests = new AtomicInteger(0);
	ServerBean bean = new ServerBean();
	StopWatch stopWatch = new LoggingStopWatch("servercall");

	AllProperties props = Util.readProperties();

	public static void main(String argv[]) throws Exception {
		Server serve = new Server();
		serve.run();
	}

	public void run() throws IOException, MalformedObjectNameException, InstanceAlreadyExistsException,
			MBeanRegistrationException, NotCompliantMBeanException {

		MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
		ObjectName name = new ObjectName("server:type=ServerBean");
		mbs.registerMBean(bean, name);

		ServerSocket readerSocket = new ServerSocket(props.readerPort);

		ExecutorService executor = Executors.newFixedThreadPool(props.maxReadersOnServer);
		serverRunning = true;

		for (int i = 0; i < props.maxReadersOnServer; i++) {
			Reader cli = new Reader(i);
			ReaderTask task = new ReaderTask(cli, readerSocket);
			executor.submit(task);
		}
		connectWriter();
	}

	void connectWriter() {
		synchronized (writerLock) {
			while (true) {
				ServerSocket writerSocket;
				try {
					writerSocket = new ServerSocket(props.writerPort);
					Socket writerClientSocket = writerSocket.accept();
					writerIn = new DataInputStream(writerClientSocket.getInputStream());
					writerOut = new DataOutputStream(writerClientSocket.getOutputStream());
					int code = writerIn.read();
					if (code == 0) {
						LOG.info("Writer connection");
						break;
					} else {
						LOG.error("Writer reconnect. Wrong initialization code (expected 0):" + code);
						writerClientSocket.close();
					}
				} catch (IOException e) {
					LOG.error("Connect writer failed", e);
					try {
						Thread.sleep(props.connectionRetryTimeout);
					} catch (InterruptedException ex) {
						LOG.error("Trying to reconnect interrupted", ex);
					}
				}

			}
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
		numberOfConcurrentWriterRequests.incrementAndGet();
		synchronized (writerLock) {
			while (true) {
				try {
					writerOut.writeInt(0);
					writerOut.writeInt(10);
					for (int i = 0; i < 10; i++) {
						int val = writerIn.readInt();
						toReturn.add(val);
					}
					break;
				} catch (Exception ex) {
					LOG.error("Failed getting sequence. Trying to reconnect", ex);
					this.connectWriter();
				}

			}

		}
		numberOfConcurrentWriterRequests.decrementAndGet();
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
			LOG.info("Started thread:" + client.threadId);
			while (Server.this.serverRunning) {
				synchronized (welcomeSocket) {
					connectionSocket = welcomeSocket.accept();
					LOG.info("Accepted on thread:" + client.threadId);
				}
				DataInputStream inFromClient = new DataInputStream(connectionSocket.getInputStream());
				DataOutputStream outToClient = new DataOutputStream(connectionSocket.getOutputStream());
				while (Server.this.serverRunning) {
					String uidS = inFromClient.readUTF();
					UUID uid = UUID.fromString(uidS);
					if (inFromClient.readByte() == 0) {
						numberOfConcurrentReaderRequests.incrementAndGet();
						stopWatch.start();
						List<Integer> sequence = Server.this.getSequence();
						for (Integer integer : sequence) {
							outToClient.writeUTF(uidS);
							outToClient.writeInt(integer);
						}
						outToClient.writeUTF(uidS);
						outToClient.writeInt(-1);
						outToClient.flush();
						stopWatch.stop();
						numberOfConcurrentReaderRequests.decrementAndGet();

					} else {
						// TODO implement
					}
				}

			}
			return this.client;
		}

	}

	public static interface ServerBeanMBean {
		// public int getNumberOfExecutedRequests();
		public int getNumberOfConcurrentReaderRequests();

		public int getNumberOfConcurrentWriterRequests();
	}

	public class ServerBean implements ServerBeanMBean {
		@Override
		public int getNumberOfConcurrentReaderRequests() {
			return Server.this.numberOfConcurrentReaderRequests.get();
		}

		@Override
		public int getNumberOfConcurrentWriterRequests() {
			return Server.this.numberOfConcurrentWriterRequests.get();
		}
		/*
		 * public int getNumberOfExecutedRequests() { //return
		 * Server.this.numberOfExecutedRequestst; }
		 */
	}

}
