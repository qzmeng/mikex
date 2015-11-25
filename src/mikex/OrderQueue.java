package mikex;

import java.util.LinkedList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OrderQueue {

	// orders at a given side and price level

	final Logger logger = LoggerFactory.getLogger(getClass());

	private double price = Double.NaN;
	private LinkedList<Order> queue = new LinkedList<Order>();

	public OrderQueue(double d) {
		this.price = d;
		logger.debug("Creating queue at price " + this.price);
	}

	public OrderQueue() {
		logger.debug("Creating queue at MARKET");
	}
	
	
	public void add(Order ord) {
		queue.add(ord);
		logger.debug("Adding order, total " + queue.size()
				+ " orders at level");
	}

	public double getPrice() {
		return price;
	}

	public int getVolume() {
		int vol = 0;
		for (Order o : queue) {
			vol += o.leavesQty.getValue();
		}
		return vol;
	}

	public Order getFirst() {
		return queue.getFirst();
	}

	public boolean del(Order ord) {
		return queue.remove(ord);

	}

	public boolean hasContents() {
		return queue.size() > 0;
	}

}
