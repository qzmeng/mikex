package mikex;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import quickfix.*;

public class Connection implements quickfix.Application {

	private quickfix.Session session;
	private final Logger logger = LoggerFactory.getLogger(getClass());
	private Orderbook orderbook;
	private MessageParser messageParser;
	private String settingsFile;
	private int responseDelay = 0;
	
	public Connection() {
		super();
	}

	public void setSettings(String settingsFile) {
		this.settingsFile = settingsFile;
	}

	public void connect() throws InterruptedException {
		messageParser = new MessageParser(this);
		messageParser.setOrderbook(this.orderbook);
		try {
			SessionSettings settings = new SessionSettings(
					new java.io.FileInputStream(this.settingsFile));
			MessageStoreFactory storeFactory = new FileStoreFactory(settings);
			LogFactory logFactory = new FileLogFactory(settings);
			MessageFactory messageFactory = new DefaultMessageFactory();
			SocketAcceptor acceptor = null;
			acceptor = new SocketAcceptor(this, storeFactory, settings,
					logFactory, messageFactory);
			acceptor.start();

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (ConfigError e) {
			e.printStackTrace();
		} catch (RuntimeError e) {
			e.printStackTrace();
		}
	}

	@Override
	public void fromAdmin(Message arg0, SessionID arg1) throws FieldNotFound,
			IncorrectDataFormat, IncorrectTagValue, RejectLogon {
		logger.info("fromAdmin[" + arg1.toString() + "]: " + formatFix(arg0));

	}

	@Override
	public void fromApp(Message arg0, SessionID arg1) throws FieldNotFound,
			IncorrectDataFormat, IncorrectTagValue, UnsupportedMessageType {
		logger.info("fromApp[" + arg1.toString() + "]: " + formatFix(arg0));
		if (this.responseDelay != 0) {
			try {
				Thread.sleep(this.responseDelay);
			} catch (InterruptedException e) {
				// Just carry on
			}
		}
		messageParser.crack(arg0, arg1);

	}

    private String formatFix(Message msg) {
        return msg.toString().replace("\001","|");
    }

	@Override
	public void onCreate(SessionID arg0) {
		logger.info("onCreate[" + arg0.toString() + "]");

	}

	@Override
	public void onLogon(SessionID arg0) {
		logger.info("onLogon[" + arg0.toString() + "]");
		this.session = quickfix.Session.lookupSession(arg0);

	}

	@Override
	public void onLogout(SessionID arg0) {
		logger.info("onLogout[" + arg0.toString() + "]");

	}

	@Override
	public void toAdmin(Message arg0, SessionID arg1) {
		logger.info("toAdmin[" + arg1.toString() + "]: " + formatFix(arg0));
	}

	@Override
	public void toApp(Message arg0, SessionID arg1) throws DoNotSend {
		logger.info("toApp[" + arg1.toString() + "]: " + formatFix(arg0));

	}

	public void setOrderbook(Orderbook orderbook) {
		this.orderbook = orderbook;
	}

	public Session getSession() {
		return this.session;
	}

	public void disconnect() {
		try {
			session.disconnect("disconnected by operator", false);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void setDelay(int newResponseDelay) {
		logger.info("Updating new response delay to "+newResponseDelay+" ms");
		this.responseDelay=newResponseDelay;
		
	}

}
