package mikex;

import java.util.LinkedList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import quickfix.field.TransactTime;

public class Ita {

	final Logger logger = LoggerFactory.getLogger(getClass());

	double lastPx=100;
	int lastVol=0;
	
	
	LinkedList<OrderQueue> buyPrice = new LinkedList<OrderQueue>();
	LinkedList<OrderQueue> sellPrice = new LinkedList<OrderQueue>();
	OrderQueue buyMarket = new OrderQueue();
	OrderQueue sellMarket = new OrderQueue();
	
	private String asset = null;
	private Orderbook orderbook;

	public Ita() {
		super();

	}

	public void addOrder(Order ord) {
		if (asset==null) asset=ord.getSymbolKey();
		
		if (ord.ordType.getValue() == quickfix.field.OrdType.MARKET) {
			if (ord.side.getValue() == quickfix.field.Side.BUY) {
				logger.info("Market BUY");
				buyMarket.add(ord);
			} else {
				logger.info("Market SELL");
				sellMarket.add(ord);
			}
		
		} else {
			// Limit order
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

		if (ord.ordType.valueEquals(quickfix.field.OrdType.MARKET)) {
			if (ord.side.valueEquals(quickfix.field.Side.BUY)) 
				return buyMarket;
			else
				return sellMarket;
		} else { // search for limit order
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

            boolean sellMktMatch=false, buyMktMatch=false;
			Order bid;
			if (buyMarket.getVolume() > 0) {
				bid = buyMarket.getFirst();
				logger.info("Matching against Buy Market, leaves="+bid.leavesQty.getValue());
                buyMktMatch=true;
			} else
				bid = buyPrice.getLast().getFirst();
			
			Order ask; 
			if (sellMarket.getVolume() > 0) {
 				ask = sellMarket.getFirst();
				logger.info("Matching against Sell Market, leaves="+ask.leavesQty.getValue());
                sellMktMatch=true;
			} else
				ask = sellPrice.getFirst().getFirst();

			double uncrossqty = (bid.leavesQty.getValue() < ask.leavesQty
					.getValue() ? bid.leavesQty.getValue() : ask.leavesQty
					.getValue());

            assert (uncrossqty>=1); // sanity

            double uncrosspx;
            if (! sellMktMatch && ! buyMktMatch) {
                // Limit vs Limit
                 uncrosspx = (bid.getTimestamp() < ask.getTimestamp() ? bid.price
					.getValue() : ask.price.getValue());
            } else if ( sellMktMatch && ! buyMktMatch) {
                uncrosspx=bid.price.getValue();					
            } else if ( !sellMktMatch && buyMktMatch) {
                uncrosspx=ask.price.getValue();					
            } else {
                // Market vs Market 
                uncrosspx=this.lastPx;
			}
			
			logger.info("Uncross " + uncrossqty + " at " + uncrosspx);
			this.lastPx=uncrosspx;
			this.lastVol=(int) uncrossqty;

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
		boolean bidmarket = false, askmarket = false;
		if (buyMarket.getVolume() > 0) {
			bidqty = buyMarket.getVolume();
			bidmarket=true;
		} else if (buyPrice.size() > 0) {
			bid = buyPrice.getLast().getPrice();
			bidqty = buyPrice.getLast().getVolume();
		}
				
		if (sellMarket.getVolume() > 0) {
			askqty = sellMarket.getVolume();
			askmarket=true;
		} else if (sellPrice.size() > 0) {
			ask = sellPrice.getFirst().getPrice();
			askqty = sellPrice.getFirst().getVolume();
		}

		
		logger.info(asset+": Best=" + bidqty + "@" + (bidmarket?"MARKET":bid) + " / " + askqty + "@" + (askmarket?"MARKET":ask));

		if (this.orderbook.getMarketStatus() == Orderbook.MarketStatus.CONTINUOUS) {
			return  (bidmarket && askqty>0) ||
					(askmarket && bidqty > 0) ||
					(!bidmarket && !askmarket && bid >= ask);
		} else {
			return false;
		}

	}

	public void setOrderbook(Orderbook orderbook) {
		this.orderbook = orderbook;
	}
}
