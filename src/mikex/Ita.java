package mikex;

import java.util.LinkedList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import quickfix.field.TransactTime;

public class Ita {

	final Logger logger = LoggerFactory.getLogger(getClass());

	LinkedList<OrderQueue> buyPrice = new LinkedList<OrderQueue>();
	LinkedList<OrderQueue> sellPrice = new LinkedList<OrderQueue>();

	
	private String asset = null;
	private Orderbook orderbook;

	public Ita() {
		super();

	}

	public void addOrder(Order ord) {
		if (asset==null) asset=ord.getSymbolKey();
		LinkedList<OrderQueue> price;
		if (ord.side.getValue() == quickfix.field.Side.BUY) {
			price = buyPrice;
		} else {
			price = sellPrice;
		}

		if (price.size() == 0) {
			OrderQueue q = new OrderQueue(ord.price.getValue());
			q.add(ord);
			price.push(q);
		} else {
			OrderQueue tgtqueue = null;
			int i = 0;
			// might be better to do binary search here
			for (OrderQueue compareLevel : price) {
				logger.debug("position=" + i + ", ordprice="
						+ ord.price.getValue() + ", compare="
						+ compareLevel.getPrice());
				if (ord.price.getValue() == compareLevel.getPrice()) {
					tgtqueue = compareLevel;
					break;
				} else if (ord.price.getValue() < compareLevel.getPrice()) {
					tgtqueue = new OrderQueue(ord.price.getValue());
					price.add(i, tgtqueue);
					break;
				}
				i++;
			}
			if (tgtqueue == null) {
				tgtqueue = new OrderQueue(ord.price.getValue());
				price.add(tgtqueue);
			}

			tgtqueue.add(ord);
		}

		uncross();
	}

	LinkedList<OrderQueue> findPriceSide(Order ord) {

		if (ord.side.getValue() == quickfix.field.Side.BUY) {
			return buyPrice;
		} else {
			return sellPrice;
		}

	}

	OrderQueue findPriceLevel(Order ord) {

		LinkedList<OrderQueue> price = findPriceSide(ord);
		if (price.size() > 0) {
			// might be better to do binary search here...
			OrderQueue tgtqueue = null;
			for (OrderQueue compareLevel : price) {
				logger.debug("DELETE: searching... ordprice="
						+ ord.price.getValue() + ", compare="
						+ compareLevel.getPrice());
				if (ord.price.getValue() == compareLevel.getPrice()) {
					tgtqueue = compareLevel;
					logger.debug("found correct price level");
					return tgtqueue;
				}
			}
		}
		return null;
	}

	public boolean delOrder(Order ord) {

		LinkedList<OrderQueue> price = findPriceSide(ord);
		OrderQueue tgtqueue = findPriceLevel(ord);
		if (tgtqueue != null) {
			boolean delresult = tgtqueue.del(ord);
			if (delresult && !tgtqueue.hasContents()) {
				price.remove(tgtqueue);
				logger.debug("removing last order at price level");
			}
			return delresult;
		}
		return false;

	}

	public void uncross() {
		while (canUncross()) {
			logger.info("Book crossed...");

			Order bid = buyPrice.getLast().getFirst();
			Order ask = sellPrice.getFirst().getFirst();

			double uncrossqty = (bid.leavesQty.getValue() < ask.leavesQty
					.getValue() ? bid.leavesQty.getValue() : ask.leavesQty
					.getValue());

			double uncrosspx = (bid.getTimestamp() < ask.getTimestamp() ? bid.price
					.getValue() : ask.price.getValue());
			
			logger.info("Uncross " + uncrossqty + " at " + uncrosspx);

			TransactTime timestamp = new TransactTime();
			ask.setTransactTimestamp(timestamp);
			bid.setTransactTimestamp(timestamp);

			bid.fill(uncrossqty, uncrosspx);
			ask.fill(uncrossqty, uncrosspx);

			if (bid.fullyFilled())
				delOrder(bid);
			if (ask.fullyFilled())
				delOrder(ask);
		}
	}

	public boolean canUncross() {

		double bid = Double.NEGATIVE_INFINITY, ask = Double.POSITIVE_INFINITY;
		int bidqty = 0, askqty = 0;
		if (buyPrice.size() > 0) {
			bid = buyPrice.getLast().getPrice();
			bidqty = buyPrice.getLast().getVolume();
		}
		if (sellPrice.size() > 0) {
			ask = sellPrice.getFirst().getPrice();
			askqty = sellPrice.getFirst().getVolume();
		}

		logger.info(asset+": Best=" + bidqty + "@" + bid + " / " + askqty + "@" + ask);

		if (this.orderbook.getMarketStatus() == Orderbook.MarketStatus.CONTINUOUS) {
			return (bid >= ask);
		} else {
			return false;
		}

	}

	public void setOrderbook(Orderbook orderbook) {
		this.orderbook = orderbook;
	}
}
