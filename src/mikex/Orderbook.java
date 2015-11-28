package mikex;

import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Orderbook {

	HashMap<String, Order> book = new HashMap<String, Order>();
	HashMap<String, Ita> asset = new HashMap<String, Ita>();

	final Logger logger = LoggerFactory.getLogger(getClass());
	private MarketStatus marketStatus;

	enum MarketStatus {
		CONTINUOUS, AUCTION, CLOSED
	};

	Orderbook() {
		setMarketStatus(MarketStatus.CONTINUOUS);
	}

	public MarketStatus getMarketStatus() {
		return marketStatus;
	}

	public String getMarketStatusString() {
		switch (getMarketStatus()) {
		case CONTINUOUS:
			return "CONTINUOUS";
		case AUCTION:
			return "AUCTION";
		case CLOSED:
			return "CLOSED";
		}
		return "unknown";
	}
	
	public void setMarketStatus(MarketStatus marketStatus) {
		this.marketStatus = MarketStatus.CONTINUOUS; // hack to force uncrossing
		uncrossAll(); 
		this.marketStatus = marketStatus;
		logger.info("Setting market mode to: "+getMarketStatusString());
		
		switch (getMarketStatus()) {
		case CLOSED:
			cancelAll("Market is closing, removing all orders");
			break;
		case AUCTION:
		case CONTINUOUS:
			break;
		}
		
	}

	public void newOrder(Order ord) {

		if (this.marketStatus == MarketStatus.CLOSED) {
			ord.getParser().sendReject(ord, "market is closed");
			return;
		}
		book.put(ord.getKey(), ord);
		ord.ordStatus = new quickfix.field.OrdStatus(
				quickfix.field.OrdStatus.NEW);
		ord.leavesQty = new quickfix.field.LeavesQty(ord.orderqty.getValue());
		logger.debug("entered in book " + ord.getKey());
		ord.getParser().sendAck(ord);

		String symbol = ord.getSymbolKey();
		Ita ita;
		if (asset.containsKey(symbol)) {
			ita = asset.get(symbol);
		} else {
			ita = new Ita();
			ita.setOrderbook(this);
			logger.info("creating new ita for symbol " + symbol);
			asset.put(symbol, ita);
		}

		ord.setTimestamp();
		ita.addOrder(ord);
	}

	public void delOrder(Order delOrd) {

		String remKey = delOrd.getOrigKey();
		delOrd.setTimestamp();
		if (book.containsKey(remKey)) {

			Order ord = book.get(remKey);

			if (ord.ordStatus.valueEquals(quickfix.field.OrdStatus.FILLED)) {
				delOrd.getParser().sendCxlRejKnown(delOrd, ord,
						"order already filled");
				return;
			} else if ((ord.ordStatus
					.valueEquals(quickfix.field.OrdStatus.CANCELED))) {
				delOrd.getParser().sendCxlRejKnown(delOrd, ord,
						"order already cancelled");
				return;
			} else if ((ord.ordStatus
					.valueEquals(quickfix.field.OrdStatus.REJECTED))) {
				delOrd.getParser().sendCxlRejKnown(delOrd, ord,
						"order already rejected");
				return;
			}

			String symbol = ord.getSymbolKey();
			if (asset.containsKey(symbol)) {
				Ita ita = asset.get(symbol);
				if (!ita.delOrder(ord)) {
					logger.info("Couldn't delete order ita Cancel rej "
							+ remKey);
					delOrd.getParser()
							.sendCxlRejKnown(delOrd, ord,
									"order found in book but couldn't delete it from ita");
				}
			} else {
				// uh-oh
				logger.error("Couldn't find order ita Cancel rej " + remKey);
				delOrd.getParser().sendCxlRejKnown(delOrd, ord,
						"order found in book but missing from ita");
				return;
			}
			ord.setTimestamp();
			ord.ordStatus = new quickfix.field.OrdStatus(
					quickfix.field.OrdStatus.CANCELED);
			ord.leavesQty = new quickfix.field.LeavesQty(0);
			// book.remove(remKey);
			logger.info("Cancel ack " + remKey);
			delOrd.getParser().sendCxlAck(delOrd, ord);

		} else {
			logger.info("Cancel rej " + remKey);
			delOrd.setTimestamp();
			delOrd.getParser().sendCxlRejUnknown(delOrd,
					"order not in order book");
		}
	}

	public void replaceOrder(Order replOrd) {

		String remKey = replOrd.getOrigKey();
		replOrd.setTimestamp();
		logger.info("Replace order start " + remKey);

		if (book.containsKey(remKey)) {

			Order ord = book.get(remKey);

			Ita ita = asset.get(ord.getSymbolKey());
			ita.canUncross();

			if (ord.ordStatus.valueEquals(quickfix.field.OrdStatus.FILLED)) {
				replOrd.getParser().sendReplaceRejKnown(replOrd, ord,
						"Mikex: order already filled");
				return;
			} else if ((ord.ordStatus
					.valueEquals(quickfix.field.OrdStatus.CANCELED))) {
				replOrd.getParser().sendReplaceRejKnown(replOrd, ord,
						"Mikex: order already cancelled");
				return;
			} else if ((ord.ordStatus
					.valueEquals(quickfix.field.OrdStatus.REJECTED))) {
				replOrd.getParser().sendReplaceRejKnown(replOrd, ord,
						"Mikex: order already rejected");
				return;
			}
			double px = replOrd.price.getValue();
			double qty = replOrd.orderqty.getValue();

			double qtydelta = replOrd.orderqty.getValue()
					- ord.orderqty.getValue();
			boolean pxdelta = px != ord.price.getValue();

			if (!ord.symbol.valueEquals(replOrd.symbol.getValue())) {
				replOrd.getParser().sendReplaceRejKnown(
						replOrd,
						ord,
						"Mikex: order symbol key does not match: old="
								+ ord.getSymbolKey() + " vs new="
								+ replOrd.getSymbolKey());
				return;
			}

			if (!ord.side.valueEquals(replOrd.side.getValue())) {
				replOrd.getParser().sendReplaceRejKnown(replOrd, ord,
						"Mikex: order side does not match");
				return;
			}

			if (qtydelta != 0 && qty <= ord.cumQty.getValue()) {
				replOrd.getParser().sendReplaceRejKnown(replOrd, ord,
						"Mikex: can't replace qty less than cum qty");
				return;
			}

			// all validations done, now start the amend

			replOrd.avgPx.setValue(ord.avgPx.getValue());
			replOrd.cumQty.setValue(ord.cumQty.getValue());
			replOrd.lastPx.setValue(ord.lastPx.getValue());
			replOrd.lastShares.setValue(ord.lastShares.getValue());
			replOrd.leavesQty.setValue(ord.leavesQty.getValue());
			replOrd.side.setValue(ord.side.getValue());

			// amend quantity
			if (qtydelta != 0) {
				logger.info("amending qty delta=" + qtydelta);
				replOrd.leavesQty.setValue(replOrd.leavesQty.getValue()
						+ qtydelta);
			}

			// amend price
			if (pxdelta) {
				logger.info("amending px old=" + ord.price.getValue()
						+ ", new=" + px);
			}

			if (qtydelta != 0 || pxdelta) {

				ita.delOrder(ord);
				ord.ordStatus = new quickfix.field.OrdStatus(
						quickfix.field.OrdStatus.CANCELED);

				book.put(replOrd.getKey(), replOrd);

				replOrd.ordStatus = new quickfix.field.OrdStatus(
						quickfix.field.OrdStatus.REPLACED);

				logger.info("Replace ack " + remKey);
				replOrd.getParser().sendReplAck(replOrd,ord);

				replOrd.setTimestamp();
				ita.addOrder(replOrd);

			} else {
				replOrd.getParser().sendCxlRejKnown(replOrd, ord,
						"nothing amended");
			}

		} else {
			logger.info("Replace rej " + remKey);
			replOrd.getParser().sendReplaceRejUnknown(replOrd,
					"order not in order book");
		}
	}

	public void cancelAll(String message) {

		for (Order ord : book.values()) {
			try {
				if (ord.leavesQty.getValue() > 0) {
					unsolicitedCancel(ord,message);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public void unsolicitedCancel(Order ord, String message) {
		String symbol = ord.getSymbolKey();
		if (asset.containsKey(symbol)) {
			Ita ita = asset.get(symbol);
			if (!ita.delOrder(ord)) {
				logger.info("Couldn't unsolicited cancel order  "
						+ ord);
			}
		}
		ord.setTimestamp();
		ord.ordStatus = new quickfix.field.OrdStatus(
				quickfix.field.OrdStatus.CANCELED);
		ord.leavesQty = new quickfix.field.LeavesQty(0);
		logger.info("Unsolicited cancelling " + ord.getKey());
		ord.getParser().sendUnsolicitedCxl(ord, message);
	}
	
	public void uncrossAll() {
		for (Ita a: asset.values()) {
			a.uncross();
		}
	}
}
