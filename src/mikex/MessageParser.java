package mikex;

import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import quickfix.*;

public class MessageParser extends MessageCracker {
	final Logger logger = LoggerFactory.getLogger(getClass());
	Orderbook orderbook;
	Connection parentConnection;

	public MessageParser(Connection connection) {
		this.parentConnection = connection;
	}

	int ordseq = 0;

	public synchronized quickfix.field.OrderID getNextOrderID() {
		quickfix.field.OrderID newOrderID = new quickfix.field.OrderID("ord"
				+ Integer.toString(ordseq) + "-" + new Date().getTime());
		ordseq++;
		return newOrderID;
	}

	int execseq = 0;

	public synchronized quickfix.field.ExecID getNextExecID() {
		quickfix.field.ExecID newExecID = new quickfix.field.ExecID("ex"
				+ Integer.toString(execseq) + "-" + new Date().getTime());
		execseq++;
		return newExecID;
	}

	public void onMessage(quickfix.fix42.NewOrderSingle msg, SessionID sessionID) {

		logger.info("got New");

		Order ord = new Order();
		ord.setParser(this);
		try {
			ord.orderID = getNextOrderID();
			msg.get(ord.clOrdID);
			msg.get(ord.side);
			msg.get(ord.symbol);
			msg.get(ord.orderqty);
			// optional tags2
			getOptionalFields(ord, msg);
			if (ord.ordType.getValue() == quickfix.field.OrdType.LIMIT)
				msg.get(ord.price);

			if (ord.orderqty.getValue() < 1) {
				this.sendReject(ord, "Mikex: order qty must be 1 or greater");
			} else {
				// actual entry point
				orderbook.newOrder(ord);
			}
		} catch (FieldNotFound e) {

			e.printStackTrace();
			this.sendReject(ord, e.getMessage());
		} catch (RuntimeException e) {
			e.printStackTrace();
			this.sendReject(ord, e.getMessage());
		}
	}

	private void getOptionalFields(Order order,
			quickfix.fix42.NewOrderSingle msg) {
		try {msg.get(order.securityID);		} catch (FieldNotFound e) {		}
		try {msg.get(order.securityType);		} catch (FieldNotFound e) {		}
		try {msg.get(order.maturityMonthYear);		} catch (FieldNotFound e) {		}
		try {msg.get(order.putOrCall);		} catch (FieldNotFound e) {		}
		try {msg.get(order.strikePrice);		} catch (FieldNotFound e) {		}
		try {msg.get(order.currency);		} catch (FieldNotFound e) {		}
		try {msg.get(order.settlCurrency);} catch (FieldNotFound e) {}
		try {msg.get(order.clientid);} catch (FieldNotFound e) {}
		try {msg.get(order.account);} catch (FieldNotFound e) {}
		try {msg.get(order.rule80A);} catch (FieldNotFound e) {}
        try {msg.get(order.ordType);} catch (FieldNotFound e) {
            order.ordType=new quickfix.field.OrdType(quickfix.field.OrdType.LIMIT);
        }
		try {msg.get(order.timeInForce);} catch (FieldNotFound e) {
            order.timeInForce=new quickfix.field.TimeInForce(quickfix.field.TimeInForce.DAY);
        }

	}

	public void onMessage(quickfix.fix42.OrderCancelRequest msg,
			SessionID sessionID) {

		logger.info("got Cancel");

		Order delOrd = new Order();
		delOrd.setParser(this);
		try {
			msg.get(delOrd.clOrdID);
			msg.get(delOrd.origClOrdID);
			msg.get(delOrd.side);
			msg.get(delOrd.symbol);
			orderbook.delOrder(delOrd);
		} catch (FieldNotFound e) {
			e.printStackTrace();
			this.sendCxlRejUnknown(delOrd, e.getMessage());
		} catch (RuntimeException e) {
			e.printStackTrace();
			this.sendCxlRejUnknown(delOrd, e.getMessage());
		}
	}

	public void onMessage(quickfix.fix42.OrderCancelReplaceRequest msg,
			SessionID sessionID) {

		logger.info("got Replace");

		Order ord = new Order();
		ord.setParser(this);
		try {
			ord.orderID = getNextOrderID();

			msg.get(ord.clOrdID);
			msg.get(ord.origClOrdID);

			msg.get(ord.side);
			msg.get(ord.symbol);
			msg.get(ord.price);
			msg.get(ord.orderqty);

			// optional tags
			try {
				msg.get(ord.securityID);
			} catch (FieldNotFound e) {
			}
			try {
				msg.get(ord.securityType);
			} catch (FieldNotFound e) {
			}
			try {
				msg.get(ord.maturityMonthYear);
			} catch (FieldNotFound e) {
			}
			try {
				msg.get(ord.putOrCall);
			} catch (FieldNotFound e) {
			}
			try {
				msg.get(ord.strikePrice);
			} catch (FieldNotFound e) {
			}
			orderbook.replaceOrder(ord);
		} catch (FieldNotFound e) {
			e.printStackTrace();
			this.sendReplaceRejUnknown(ord, e.getMessage());
		} catch (RuntimeException e) {
			e.printStackTrace();
			this.sendReplaceRejUnknown(ord, e.getMessage());
		}

	}

	public void setOrderbook(Orderbook orderbook) {
		this.orderbook = orderbook;
	}

	void send(quickfix.fix42.Message msg) {
		try {
			this.parentConnection.getSession().send(msg);
		} catch (RuntimeException e) {
			e.printStackTrace();
		}
	}

	public void sendAck(Order origord) {
		quickfix.fix42.ExecutionReport msg = new quickfix.fix42.ExecutionReport();
		quickfix.field.ClOrdID newClOrdId = origord.clOrdID;
		quickfix.field.OrdStatus ordStatus = origord.ordStatus;
		quickfix.field.ExecType execType = new quickfix.field.ExecType(
				quickfix.field.ExecType.NEW);
		quickfix.field.ExecTransType execTransType = new quickfix.field.ExecTransType(
				quickfix.field.ExecTransType.NEW);
		quickfix.field.OrderID orderID = origord.orderID;
		quickfix.field.ExecID execID = getNextExecID();

		quickfix.field.Symbol symbol = origord.symbol;
		quickfix.field.Side side = origord.side;
		quickfix.field.LeavesQty leavesQty = new quickfix.field.LeavesQty(
				origord.orderqty.getValue());
		quickfix.field.AvgPx avgPx = new quickfix.field.AvgPx(0);
		quickfix.field.CumQty cumQty = new quickfix.field.CumQty(0);
		quickfix.field.Price price = origord.price;
		quickfix.field.OrderQty orderqty = origord.orderqty;
		quickfix.field.TransactTime transactTime = origord.transactTime;
		quickfix.field.Text text = new quickfix.field.Text("Accepted by Mikex SIMULATOR");


		msg.setField(newClOrdId);
		msg.setField(ordStatus);
		msg.setField(orderID);
		msg.setField(execID);
		msg.setField(symbol);
		msg.setField(side);
		msg.setField(leavesQty);
		msg.setField(avgPx);
		msg.setField(cumQty);
		msg.setField(execType);
		msg.setField(execTransType);
		msg.setField(price);
		msg.setField(orderqty);
		msg.setField(transactTime);
		msg.setField(text);


		// optional ones
		addOptionalFields(origord, msg);
		send(msg);
	}

	private void addOptionalFields(Order order,
			quickfix.fix42.Message msg) {
		quickfix.field.SecurityType securityType = order.securityType;
		quickfix.field.SecurityID securityID = order.securityID;
		quickfix.field.MaturityMonthYear maturityMonthYear = order.maturityMonthYear;
		quickfix.field.PutOrCall putOrCall = order.putOrCall;
		quickfix.field.StrikePrice strikePrice = order.strikePrice;
		quickfix.field.Currency currency = order.currency;
		quickfix.field.OrdType ordType = order.ordType;
		quickfix.field.TimeInForce timeInForce = order.timeInForce;
		quickfix.field.Account account = order.account;
		quickfix.field.ClientID clientid = order.clientid;
		quickfix.field.SettlCurrency settlCurrency = order.settlCurrency;
		quickfix.field.Rule80A rule80A = order.rule80A;
		if (!securityType.valueEquals(""))
			msg.setField(securityType);
		if (!securityID.valueEquals(""))
			msg.setField(securityID);
		if (!maturityMonthYear.valueEquals(""))
			msg.setField(maturityMonthYear);
		if (putOrCall.valueEquals(quickfix.field.PutOrCall.PUT)
				|| putOrCall.valueEquals(quickfix.field.PutOrCall.CALL))
			msg.setField(putOrCall);
		if (!strikePrice.valueEquals(0))
			msg.setField(strikePrice);
		if (!currency.valueEquals(""))
			msg.setField(currency);
		if (!ordType.equals(" "))
			msg.setField(ordType);
		if (!timeInForce.equals(" "))
			msg.setField(timeInForce);
		if (!account.valueEquals(""))
			msg.setField(account);
		if (!clientid.valueEquals(""))
			msg.setField(clientid);	
		if (!settlCurrency.valueEquals(""))
			msg.setField(settlCurrency);	
		if (rule80A.valueEquals(quickfix.field.Rule80A.AGENCY_SINGLE_ORDER) ||
            rule80A.valueEquals(quickfix.field.Rule80A.PRINCIPAL))
			msg.setField(rule80A);		
	}

	public void sendReject(Order origord, String rejectReason) {
		quickfix.fix42.ExecutionReport msg = new quickfix.fix42.ExecutionReport();
		quickfix.field.ClOrdID newClOrdId = origord.clOrdID;
		quickfix.field.OrdStatus ordStatus = new quickfix.field.OrdStatus(
				quickfix.field.OrdStatus.REJECTED);
		quickfix.field.ExecType execType = new quickfix.field.ExecType(
				quickfix.field.ExecType.REJECTED);
		quickfix.field.ExecTransType execTransType = new quickfix.field.ExecTransType(
				quickfix.field.ExecTransType.NEW);
		quickfix.field.OrderID orderID = origord.orderID;
		quickfix.field.Symbol symbol = origord.symbol;
		quickfix.field.Side side = origord.side;
		quickfix.field.LeavesQty leavesQty = new quickfix.field.LeavesQty(0);
		quickfix.field.AvgPx avgPx = new quickfix.field.AvgPx(0);
		quickfix.field.CumQty cumQty = new quickfix.field.CumQty(0);
		quickfix.field.Price price = origord.price;
		quickfix.field.OrderQty orderqty = origord.orderqty;
		quickfix.field.ExecID execID = getNextExecID();
		quickfix.field.Text text = new quickfix.field.Text(rejectReason);
		quickfix.field.TransactTime transactTime = origord.transactTime;
		msg.setField(newClOrdId);
		msg.setField(ordStatus);
		msg.setField(orderID);
		msg.setField(execID);
		msg.setField(symbol);
		msg.setField(side);
		msg.setField(leavesQty);
		msg.setField(avgPx);
		msg.setField(cumQty);
		msg.setField(execType);
		msg.setField(execTransType);
		msg.setField(price);
		msg.setField(orderqty);
		msg.setField(text);
		msg.setField(transactTime);

		addOptionalFields(origord, msg);
		send(msg);
	}

	public void sendCxlAck(Order delOrd, Order ord) {

		quickfix.fix42.ExecutionReport msg = new quickfix.fix42.ExecutionReport();
		quickfix.field.ClOrdID clOrdID = delOrd.clOrdID;
		quickfix.field.OrigClOrdID origClOrdID = delOrd.origClOrdID;
		quickfix.field.ExecType execType = new quickfix.field.ExecType(
				quickfix.field.ExecType.CANCELED);
		quickfix.field.ExecTransType execTransType = new quickfix.field.ExecTransType(
				quickfix.field.ExecTransType.NEW);
		quickfix.field.OrderID orderID = ord.orderID;
		quickfix.field.OrdStatus ordStatus = ord.ordStatus;
		quickfix.field.Symbol symbol = ord.symbol;
		quickfix.field.Side side = ord.side;
		quickfix.field.LeavesQty leavesQty = ord.leavesQty;
		quickfix.field.AvgPx avgPx = ord.avgPx;
		quickfix.field.CumQty cumQty = ord.cumQty;
		quickfix.field.Price price = ord.price;
		quickfix.field.OrderQty orderqty = ord.orderqty;
		quickfix.field.ExecID execID = getNextExecID();
		quickfix.field.TransactTime transactTime = ord.transactTime;

		msg.setField(clOrdID);
		msg.setField(origClOrdID);
		msg.setField(execID);
		msg.setField(ordStatus);
		msg.setField(orderID);
		msg.setField(symbol);
		msg.setField(side);
		msg.setField(leavesQty);
		msg.setField(avgPx);
		msg.setField(cumQty);
		msg.setField(execType);
		msg.setField(execTransType);
		msg.setField(price);
		msg.setField(orderqty);
		msg.setField(transactTime);

		addOptionalFields(ord, msg);

		send(msg);
	}

	public void sendCxlRejUnknown(Order delOrd, String rejReason) {

		quickfix.fix42.OrderCancelReject msg = new quickfix.fix42.OrderCancelReject();
		quickfix.field.OrderID orderID = getNextOrderID();
		quickfix.field.OrdStatus ordStatus = new quickfix.field.OrdStatus(
				quickfix.field.OrdStatus.REJECTED);
		quickfix.field.ClOrdID clOrdID = delOrd.clOrdID;
		quickfix.field.OrigClOrdID origClOrdID = delOrd.origClOrdID;
		quickfix.field.CxlRejReason cxlRejReason = new quickfix.field.CxlRejReason(
				quickfix.field.CxlRejReason.UNKNOWN_ORDER);
		quickfix.field.CxlRejResponseTo cxlRejResponseTo = new quickfix.field.CxlRejResponseTo(
				quickfix.field.CxlRejResponseTo.ORDER_CANCEL_REQUEST);
		quickfix.field.TransactTime transactTime = delOrd.transactTime;

		msg.setField(clOrdID);
		msg.setField(ordStatus);

		msg.setField(origClOrdID);
		msg.setField(orderID);
		msg.setField(cxlRejReason);
		msg.setField(cxlRejResponseTo);
		msg.setField(transactTime);
		quickfix.field.Text text = new quickfix.field.Text(rejReason);
		msg.setField(text);

		send(msg);

	}

	public void sendCxlRejKnown(Order delOrd, Order ord, String rejReason) {

		quickfix.fix42.OrderCancelReject msg = new quickfix.fix42.OrderCancelReject();
		quickfix.field.OrderID orderID = ord.orderID;
		quickfix.field.OrdStatus ordStatus = ord.ordStatus;
		quickfix.field.ClOrdID clOrdID = delOrd.clOrdID;
		quickfix.field.OrigClOrdID origClOrdID = delOrd.origClOrdID;
		quickfix.field.CxlRejReason cxlRejReason = new quickfix.field.CxlRejReason(
				quickfix.field.CxlRejReason.TOO_LATE_TO_CANCEL);
		quickfix.field.CxlRejResponseTo cxlRejResponseTo = new quickfix.field.CxlRejResponseTo(
				quickfix.field.CxlRejResponseTo.ORDER_CANCEL_REQUEST);
		quickfix.field.TransactTime transactTime = delOrd.transactTime;

		msg.setField(clOrdID);
		msg.setField(ordStatus);

		msg.setField(origClOrdID);
		msg.setField(orderID);
		msg.setField(cxlRejReason);
		msg.setField(cxlRejResponseTo);
		msg.setField(transactTime);
		quickfix.field.Text text = new quickfix.field.Text(rejReason);
		msg.setField(text);

		addOptionalFields(ord, msg);
		send(msg);

	}


	public void sendFill(Order order) {

		fill(order, quickfix.field.ExecType.FILL);
	}

	public void sendPartialFill(Order order) {

		fill(order, quickfix.field.ExecType.PARTIAL_FILL);
	}

	void fill(Order ord, char filltype) {

		quickfix.fix42.ExecutionReport msg = new quickfix.fix42.ExecutionReport();
		quickfix.field.ClOrdID clOrdID = ord.clOrdID;
		// quickfix.field.OrigClOrdID origClOrdID = delOrd.origClOrdID;
		quickfix.field.ExecType execType = new quickfix.field.ExecType(filltype);
		quickfix.field.ExecTransType execTransType = new quickfix.field.ExecTransType(
				quickfix.field.ExecTransType.NEW);


		msg.setField(clOrdID);
		msg.setField(execType);
		msg.setField(execTransType);
		
		quickfix.field.OrderID orderID = ord.orderID;
		quickfix.field.OrdStatus ordStatus = ord.ordStatus;
		quickfix.field.Symbol symbol = ord.symbol;
		quickfix.field.Side side = ord.side;
		quickfix.field.LeavesQty leavesQty = ord.leavesQty;
		quickfix.field.AvgPx avgPx = ord.avgPx;
		quickfix.field.CumQty cumQty = ord.cumQty;
		quickfix.field.Price price = ord.price;
		quickfix.field.OrderQty orderqty = ord.orderqty;
		quickfix.field.LastPx lastPx = ord.lastPx;
		quickfix.field.LastShares lastShares = ord.lastShares;
		quickfix.field.ExecID execID = getNextExecID();
		quickfix.field.TransactTime transactTime = ord.transactTime;
		msg.setField(execID);
		msg.setField(ordStatus);
		msg.setField(orderID);
		msg.setField(symbol);
		msg.setField(side);
		msg.setField(leavesQty);
		msg.setField(avgPx);
		msg.setField(lastPx);
		msg.setField(lastShares);
		msg.setField(cumQty);
		msg.setField(price);
		msg.setField(orderqty);
		msg.setField(transactTime);

		addOptionalFields(ord, msg);

		send(msg);
	}

	public void sendReplaceRejKnown(Order replOrd, Order ord, String rejReason) {

		quickfix.fix42.OrderCancelReject msg = new quickfix.fix42.OrderCancelReject();

		quickfix.field.OrderID orderID = ord.orderID;
		quickfix.field.OrdStatus ordStatus = ord.ordStatus;
		quickfix.field.ClOrdID clOrdID = replOrd.clOrdID;
		quickfix.field.OrigClOrdID origClOrdID = replOrd.origClOrdID;
		quickfix.field.TransactTime transactTime = replOrd.transactTime;

		msg.setField(clOrdID);
		msg.setField(ordStatus);

		msg.setField(origClOrdID);
		msg.setField(orderID);
		quickfix.field.CxlRejReason cxlRejReason = new quickfix.field.CxlRejReason(
				quickfix.field.CxlRejReason.OTHER);
		quickfix.field.CxlRejResponseTo cxlRejResponseTo = new quickfix.field.CxlRejResponseTo(
				quickfix.field.CxlRejResponseTo.ORDER_CANCEL_REPLACE_REQUEST);
		msg.setField(cxlRejReason);
		msg.setField(cxlRejResponseTo);
		quickfix.field.Text text = new quickfix.field.Text(rejReason);
		msg.setField(text);
		msg.setField(transactTime);

		addOptionalFields(ord, msg);

		send(msg);

	}

	public void sendReplAck(Order ord, Order origOrd) {

		quickfix.fix42.ExecutionReport msg = new quickfix.fix42.ExecutionReport();
		quickfix.field.ClOrdID newClOrdId = ord.clOrdID;
		quickfix.field.OrdStatus ordStatus = ord.ordStatus;
		quickfix.field.ExecType execType = new quickfix.field.ExecType(
				quickfix.field.ExecType.NEW);
		quickfix.field.ExecTransType execTransType = new quickfix.field.ExecTransType(
				quickfix.field.ExecTransType.NEW);
		quickfix.field.OrderID orderID = ord.orderID;
		quickfix.field.ExecID execID = getNextExecID();

		quickfix.field.OrigClOrdID origClOrdID = new quickfix.field.OrigClOrdID(origOrd.clOrdID.getValue());
		msg.setField(newClOrdId);
		msg.setField(origClOrdID);
		quickfix.field.Symbol symbol = ord.symbol;
		quickfix.field.Side side = ord.side;
		quickfix.field.LeavesQty leavesQty = ord.leavesQty;
		quickfix.field.AvgPx avgPx = ord.avgPx;
		quickfix.field.CumQty cumQty = ord.cumQty;
		quickfix.field.Price price = ord.price;
		quickfix.field.OrderQty orderqty = ord.orderqty;
		quickfix.field.TransactTime transactTime = ord.transactTime;


		msg.setField(ordStatus);
		msg.setField(orderID);
		msg.setField(execID);
		msg.setField(symbol);
		msg.setField(side);
		msg.setField(leavesQty);
		msg.setField(avgPx);
		msg.setField(cumQty);
		msg.setField(execType);
		msg.setField(execTransType);
		msg.setField(price);
		msg.setField(orderqty);
		msg.setField(transactTime);

		addOptionalFields(ord, msg);
		send(msg);

	}

	public void sendReplaceRejUnknown(Order replOrd, String rejReason) {

		quickfix.fix42.OrderCancelReject msg = new quickfix.fix42.OrderCancelReject();
		quickfix.field.OrderID orderID = getNextOrderID();
		quickfix.field.OrdStatus ordStatus = new quickfix.field.OrdStatus(
				quickfix.field.OrdStatus.REJECTED);
		quickfix.field.ClOrdID clOrdID = replOrd.clOrdID;
		quickfix.field.OrigClOrdID origClOrdID = replOrd.origClOrdID;
		quickfix.field.CxlRejReason cxlRejReason = new quickfix.field.CxlRejReason(
				quickfix.field.CxlRejReason.UNKNOWN_ORDER);
		quickfix.field.CxlRejResponseTo cxlRejResponseTo = new quickfix.field.CxlRejResponseTo(
				quickfix.field.CxlRejResponseTo.ORDER_CANCEL_REPLACE_REQUEST);
		quickfix.field.TransactTime transactTime = replOrd.transactTime;

		msg.setField(clOrdID);
		msg.setField(ordStatus);

		msg.setField(origClOrdID);
		msg.setField(orderID);
		msg.setField(cxlRejReason);
		msg.setField(cxlRejResponseTo);
		msg.setField(transactTime);
		quickfix.field.Text text = new quickfix.field.Text(rejReason);
		msg.setField(text);

		send(msg);
	}

	public void sendUnsolicitedCxl(Order ord, String message) {

		quickfix.fix42.ExecutionReport msg = new quickfix.fix42.ExecutionReport();
		quickfix.field.ClOrdID clOrdID = ord.clOrdID;
		quickfix.field.ExecType execType = new quickfix.field.ExecType(
				quickfix.field.ExecType.CANCELED);
		quickfix.field.ExecTransType execTransType = new quickfix.field.ExecTransType(
				quickfix.field.ExecTransType.NEW);
		quickfix.field.OrderID orderID = ord.orderID;
		quickfix.field.OrdStatus ordStatus = ord.ordStatus;
		quickfix.field.Symbol symbol = ord.symbol;
		quickfix.field.Side side = ord.side;
		quickfix.field.LeavesQty leavesQty = ord.leavesQty;
		quickfix.field.AvgPx avgPx = ord.avgPx;
		quickfix.field.CumQty cumQty = ord.cumQty;
		quickfix.field.Price price = ord.price;
		quickfix.field.OrderQty orderqty = ord.orderqty;
		quickfix.field.ExecID execID = getNextExecID();
		quickfix.field.TransactTime transactTime = ord.transactTime;
		if (message==null) message="Mikex: unsolicited cancel";
		quickfix.field.Text text = new quickfix.field.Text(message);


		msg.setField(clOrdID);
		msg.setField(execID);
		msg.setField(ordStatus);
		msg.setField(orderID);
		msg.setField(symbol);
		msg.setField(side);
		msg.setField(leavesQty);
		msg.setField(avgPx);
		msg.setField(cumQty);
		msg.setField(execType);
		msg.setField(execTransType);
		msg.setField(price);
		msg.setField(orderqty);
		msg.setField(transactTime);
		msg.setField(text);

		addOptionalFields(ord,msg);

		send(msg);
	}

}
