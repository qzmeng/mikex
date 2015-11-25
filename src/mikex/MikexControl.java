package mikex;

import mikex.Orderbook.MarketStatus;

public class MikexControl implements MikexControlMBean {

	// private final Logger logger = LoggerFactory.getLogger(getClass());
	private Orderbook orderbook;
	private ConnectionManager connectionManager;

	public void cancelAll() {
		this.orderbook.cancelAll();

	}

	public String getName() {
		return this.toString();
	}

	public void disconnectAll() {
		connectionManager.disconnectAll();
	}

	public void setOrderbook(Orderbook orderbook) {
		this.orderbook = orderbook;
	}

	public void setConnectionManager(ConnectionManager connectionManager) {
		this.connectionManager = connectionManager;
	}

	@Override
	public void marketModeContinuous() {
		this.orderbook.setMarketStatus(MarketStatus.CONTINUOUS);
	}

	@Override
	public void marketModeClosed() {
		this.orderbook.setMarketStatus(MarketStatus.CLOSED);

	}

	@Override
	public void marketModeAuction() {
		this.orderbook.setMarketStatus(MarketStatus.AUCTION);

	}
	
	public String getMarketStatus() {
		return this.orderbook.getMarketStatusString();
	}
	
	@Override
	public void setConnectionResponseDelay(int newResponseDelay) {
		this.connectionManager.setDelayAll(newResponseDelay);
	}
	
	@Override
	public int getConnectionResponseDelay() {
		return this.connectionManager.getResponseDelay();
	}

}
