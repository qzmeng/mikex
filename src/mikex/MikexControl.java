package mikex;

import mikex.Orderbook.MarketStatus;

public class MikexControl implements MikexControlMBean {

	// private final Logger logger = LoggerFactory.getLogger(getClass());
	private Orderbook orderbook;
	private ConnectionManager connectionManager;

	public void cancelAll() {
		this.orderbook.cancelAll("Mass cancel by operator");

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
		this.orderbook.setNonCxl(false);
	}

	@Override
	public void marketModeClosed() {
		this.orderbook.setMarketStatus(MarketStatus.CLOSED);
		this.orderbook.setNonCxl(false);
	}

	@Override
	public void marketModeAuction() {
		this.orderbook.setMarketStatus(MarketStatus.AUCTION);
		this.orderbook.setNonCxl(false);
	}
	@Override
	public void marketModeContinuousNonCxl() {
		this.orderbook.setMarketStatus(MarketStatus.CONTINUOUS);
		this.orderbook.setNonCxl(true);
	}

	@Override
	public void marketModeAuctionNonCxl() {
		this.orderbook.setMarketStatus(MarketStatus.AUCTION);
		this.orderbook.setNonCxl(true);

	}	


	@Override
	public void ackInhibit() {
		this.orderbook.setAckInhibit(true);
	}

	@Override
	public void ackGenerate() {
		this.orderbook.setAckInhibit(false);
	}

	@Override
	public void autoFillDisable() {
		this.orderbook.setAutoFill(false);
	}

	@Override
	public void autoFillEnable() {
		this.orderbook.setAutoFill(true);
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
