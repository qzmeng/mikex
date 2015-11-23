package mikex;

public class Order {
	public quickfix.field.ClOrdID clOrdID = new quickfix.field.ClOrdID();
	public quickfix.field.OrigClOrdID origClOrdID = new quickfix.field.OrigClOrdID();
	public quickfix.field.Symbol symbol = new quickfix.field.Symbol();
	public quickfix.field.Side side = new quickfix.field.Side();
	public quickfix.field.Price price = new quickfix.field.Price();
	public quickfix.field.OrderQty orderqty = new quickfix.field.OrderQty();
	public quickfix.field.OrdStatus ordStatus = new quickfix.field.OrdStatus();
	public quickfix.field.LastPx lastPx = new quickfix.field.LastPx();
	public quickfix.field.OrderID orderID = new quickfix.field.OrderID();
	public quickfix.field.LastShares lastShares = new quickfix.field.LastShares();

	public quickfix.field.AvgPx avgPx = new quickfix.field.AvgPx();
	public quickfix.field.CumQty cumQty = new quickfix.field.CumQty();
	public quickfix.field.LeavesQty leavesQty = new quickfix.field.LeavesQty();

	public quickfix.field.SecurityID securityID = new quickfix.field.SecurityID();
	public quickfix.field.SecurityType securityType = new quickfix.field.SecurityType();
	public quickfix.field.MaturityMonthYear maturityMonthYear = new quickfix.field.MaturityMonthYear();
	public quickfix.field.StrikePrice strikePrice = new quickfix.field.StrikePrice();
	public quickfix.field.PutOrCall putOrCall = new quickfix.field.PutOrCall(' ');

	public quickfix.field.OrdType ordType = new quickfix.field.OrdType(quickfix.field.OrdType.LIMIT);
	
	private MessageParser parser;

	public MessageParser getParser() {
		return parser;
	}

	public void setParser(MessageParser parser) {
		this.parser = parser;
	}

	public String getKey() {
		return parser.toString() + "_" + clOrdID.getValue();
	}

	public String getOrigKey() {
		return parser.toString() + "_" + origClOrdID.getValue();
	}

	public quickfix.field.OrdStatus getStatus() {
		return ordStatus;
	}

	public String getSymbolKey() {
		return symbol.getValue() + "-" + securityID.getValue() + "-"
				+ securityType.getValue() + "-" + maturityMonthYear.getValue()
				+ "-" + putOrCall.getValue() + "-" + strikePrice.getValue();
	}

	long orderTimestamp;

	public void setTimestamp() {
		this.orderTimestamp = System.nanoTime();
	}

	public long getTimestamp() {
		return this.orderTimestamp;
	}

	double num = 0;

	public void fill(double qty, double px) {
		this.lastShares.setValue(qty);
		this.lastPx.setValue(px);

		this.cumQty.setValue(this.cumQty.getValue() + qty);
		this.leavesQty.setValue(this.leavesQty.getValue() - qty);

		num += px * qty;
		this.avgPx.setValue(num / this.cumQty.getValue());

		if (fullyFilled()) {
			ordStatus.setValue(quickfix.field.OrdStatus.FILLED);
			this.parser.sendFill(this);
		} else {
			ordStatus.setValue(quickfix.field.OrdStatus.PARTIALLY_FILLED);
			this.parser.sendPartialFill(this);
		}
	}

	public boolean fullyFilled() {
		return this.leavesQty.valueEquals(0);
	}

}
