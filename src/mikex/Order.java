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

	public quickfix.field.ExDestination exDestination = new quickfix.field.ExDestination();
	public quickfix.field.SecurityExchange secExchange = new quickfix.field.SecurityExchange();
	public quickfix.field.SecurityID securityID = new quickfix.field.SecurityID();
	public quickfix.field.SecurityType securityType = new quickfix.field.SecurityType();
	public quickfix.field.MaturityMonthYear maturityMonthYear = new quickfix.field.MaturityMonthYear();
	public quickfix.field.StrikePrice strikePrice = new quickfix.field.StrikePrice();
	public quickfix.field.PutOrCall putOrCall = new quickfix.field.PutOrCall(' ');

	public quickfix.field.OrdType ordType = new quickfix.field.OrdType(quickfix.field.OrdType.LIMIT);
	public quickfix.field.TimeInForce timeInForce = new quickfix.field.TimeInForce(quickfix.field.TimeInForce.DAY);
	public quickfix.field.Currency currency = new quickfix.field.Currency();
	public quickfix.field.Account account = new quickfix.field.Account();
	public quickfix.field.ClientID clientid = new quickfix.field.ClientID();
	public quickfix.field.SettlCurrency settlCurrency = new quickfix.field.SettlCurrency();
	public quickfix.field.Rule80A rule80A  = new quickfix.field.Rule80A();
		
	public quickfix.field.TransactTime transactTime = new quickfix.field.TransactTime();
	
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
		this.transactTime=new quickfix.field.TransactTime();
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

	public void setTransactTimestamp(quickfix.field.TransactTime timestamp) {
		this.transactTime=timestamp;
	}
	
	public String getOverrideCurrency() {
			String ex="";
			if (this.secExchange != null) ex=this.secExchange.getValue();
			if (this.exDestination != null && (ex==null || ex.isEmpty())) ex=this.exDestination.getValue();
			if ("XHKF".equals(ex)) return "HKD";
			else if ("XOSE".equals(ex)) return "JPY";
			else if ("XSFE".equals(ex)) return "AUD";
			else if ("XSIM".equals(ex)) {
				if (this.symbol.getValue().startsWith("NK")) return "JPY";
				else return "USD";
			}
			
		return null;
	}
	
}
