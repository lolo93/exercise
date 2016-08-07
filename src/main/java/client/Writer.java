package client;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.management.ManagementFactory;
import java.net.Socket;
import java.net.UnknownHostException;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;

import org.perf4j.StopWatch;
import org.perf4j.log4j.Log4JStopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import util.Util;
import util.Util.AllProperties;

class Writer {
	private static final Logger LOG = LoggerFactory.getLogger(Writer.class);
	boolean writerRunning = false;
	WriterBean bean = new WriterBean();
	int numberOfExecutedRequestst = 0;
	StopWatch stopWatch = new Log4JStopWatch("writer");

	public static void main(String argv[]) throws Exception {
		Writer wr = new Writer();
        wr.run();
	}

	public void run() throws UnknownHostException, IOException, MalformedObjectNameException, InstanceAlreadyExistsException, MBeanRegistrationException, NotCompliantMBeanException {
		MBeanServer mbs = ManagementFactory.getPlatformMBeanServer(); 
		ObjectName name = new ObjectName("client:type=WriterBean"); 
        mbs.registerMBean(bean, name);
		
		
		
		AllProperties props = Util.readProperties();
		while (true) {
			Socket clientSocket = null;
			DataOutputStream outToServer = null;
			DataInputStream inFromServer = null;
			while (true) {
				try {
					clientSocket = new Socket("localhost", props.writerPort);
					OutputStream oStream = clientSocket.getOutputStream();
					outToServer = new DataOutputStream(oStream);
					inFromServer = new DataInputStream(clientSocket.getInputStream());
					outToServer.write(0);
					outToServer.flush();
					LOG.info("Writer connect successfull");
					break;
				} catch (Exception ex) {
					LOG.error("Writer connect failed",ex);
					try {
						Thread.sleep(props.connectionRetryTimeout);
					} catch (InterruptedException e) {
						LOG.error("Sleep interrupted", e);
					}
				}
			}
			this.writerRunning = true;
			try {
				while (writerRunning) {
					stopWatch.start();
					int offset = inFromServer.readInt();
					int number = inFromServer.readInt();
					for (int i = offset; i < offset + number; i++) {
						outToServer.writeInt(i);
					}
					numberOfExecutedRequestst++;
					stopWatch.stop();
					//this.bean.numberOfExecutedRequests = numberOfExecutedRequestst;
				}
			} catch (Exception ex) {
				LOG.error("Writer communication failed",ex);
				try {
					clientSocket.close();
				} catch(IOException e) {
					LOG.error("Socket close on error failed",e);
				}
			}
		}
	}
	
	public static interface WriterBeanMBean {
		public int getNumberOfExecutedRequests();
	}
	
	public class WriterBean implements WriterBeanMBean {
		public int getNumberOfExecutedRequests() {
			return Writer.this.numberOfExecutedRequestst;
		}
	}
	
}
