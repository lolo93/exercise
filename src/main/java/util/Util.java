package util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Util {
	private static final Logger LOG = LoggerFactory.getLogger(Util.class);

	public static class AllProperties {
		public int readerPort;
		public int writerPort;
		public int numReaders;
		public int maxReadersOnServer;
		public int connectionRetryTimeout; 
		
		

		public AllProperties(int readerPort, int writerPort, int numReaders, int maxReadersOnServer, int connectionRetryTimeout) {
			super();
			this.readerPort = readerPort;
			this.writerPort = writerPort;
			this.numReaders = numReaders;
			this.maxReadersOnServer = maxReadersOnServer;
			this.connectionRetryTimeout = connectionRetryTimeout;
		}
	}

	public static AllProperties readProperties() {
		LOG.debug("readProperties");
		Properties prop = new Properties();
		InputStream input = null;

		try {
			input = ClassLoader.getSystemResourceAsStream("all.properties");
			//input = new FileInputStream("all.properties");

			prop.load(input);

			String rportS = prop.getProperty("readerPort");
			String wportS = prop.getProperty("writerPort");
			String numReadersS = prop.getProperty("numReaders");
			String maxReadersOnServerS = prop.getProperty("maxReadersOnServer");
			String connectionRetryTimeoutS = prop.getProperty("connectionRetryTimeout");
			LOG.info("Properties:"+prop.toString());
			
			int rport = Integer.parseInt(rportS);
			int wport = Integer.parseInt(wportS);
			int numReaders = Integer.parseInt(numReadersS);
			int maxReadersOnServer = Integer.parseInt(maxReadersOnServerS);
			int connectionRetryTimeout = Integer.parseInt(connectionRetryTimeoutS);
			AllProperties toReturn = new AllProperties(rport, wport, numReaders, maxReadersOnServer,connectionRetryTimeout);
			
			
			return toReturn;
		} catch (IOException ex) {
			LOG.error("Failed loading properties file: all.properties",ex);
			throw new IllegalArgumentException("Failed loading proprties file: all.properties", ex);
		} catch (NumberFormatException ex) {
			LOG.error("Failed loading properties file: all.properties",ex);
			throw new IllegalArgumentException("Failed parsing proprties file: all.properties", ex);
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
					LOG.error("Failed closing properties file: all.properties",e);
				}
			}
		}

	}

}
