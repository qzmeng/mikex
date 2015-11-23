package mikex;

import java.lang.management.ManagementFactory;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Startup {

	Orderbook orderbook;
	ConnectionManager connectionManager;

	void init(String[] args) {
	    Logger logger = LoggerFactory.getLogger(Startup.class);
		
		orderbook = new Orderbook();
		connectionManager = new ConnectionManager();
	    
		try {
			MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
			ObjectName mbobject = new ObjectName("mikex:type=MikexControl");
			MikexControl bean = new MikexControl();
			bean.setOrderbook(orderbook);
			bean.setConnectionManager(connectionManager);
			mbs.registerMBean(bean, mbobject);
			logger.info("registered MBean");
		} catch (NotCompliantMBeanException e) {
			e.printStackTrace();
		} catch (MalformedObjectNameException e) {
			e.printStackTrace();
		} catch (NullPointerException e) {
			e.printStackTrace();
		} catch (InstanceAlreadyExistsException e) {
			e.printStackTrace();
		} catch (MBeanRegistrationException e) {
			e.printStackTrace();
		} 
		
		connectionManager.startup();
		connectionManager.setOrderbook(orderbook);
		connectionManager.connectAll();
	}
	
	public static void main(String[] args) {
		
		


		
	    new Startup().init(args);
	}

}
