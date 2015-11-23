package mikex;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConnectionManager {

	final Logger logger = LoggerFactory.getLogger(getClass());

	HashMap<String, Connection> connectionList = new HashMap<String, Connection>();
	Orderbook orderbook;

	public void startup() {

		String profileDir = System.getProperty("connectiondir", "profiles"
				+ File.separatorChar);
		File[] profileList = new File(profileDir).listFiles();

		if (profileList == null) {
			logger.error("Can't read profiles from directory " + profileDir);
			return;
		}

		connectionList.clear();

		for (File f : profileList) {
			if (!f.getName().endsWith(".txt"))
				continue;

			Connection c = new Connection();
			try {
				c.setSettings(f.getCanonicalPath());
				connectionList.put(f.getName(), c);
				logger.info("added connection from " + f.getName());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		if (connectionList.size() == 0) {
			logger.error("no connections loaded, unlikely to work hereon");
		}
	}

	public void connectAll() {
		for (Connection c : connectionList.values()) {
			try {
				c.setOrderbook(this.orderbook);
				c.connect();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	public void setOrderbook(Orderbook orderbook) {
		this.orderbook = orderbook;
	}

	public void disconnectAll() {
		for (Connection c : connectionList.values()) {
			c.disconnect();
		}
	}
	
	public void setDelayAll(int newResponseDelay) {
		for (Connection c : connectionList.values()) {
			c.setDelay(newResponseDelay);
		}
	}

}
