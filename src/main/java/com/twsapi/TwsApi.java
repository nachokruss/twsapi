package com.twsapi;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;

import com.ib.client.Contract;
import com.ib.client.ContractDetails;
import com.ib.client.Order;
import com.ib.client.OrderState;
import com.ib.client.OrderStatus;
import com.ib.client.OrderType;
import com.ib.client.TagValue;
import com.ib.client.TickType;
import com.ib.client.Types;
import com.ib.client.Types.Action;
import com.ib.client.Types.AlgoStrategy;
import com.ib.client.Types.MktDataType;
import com.ib.controller.AccountSummaryTag;
import com.ib.controller.ApiConnection.ILogger;
import com.ib.controller.ApiController;
import com.ib.controller.ApiController.IConnectionHandler;
import com.ib.controller.ApiController.IContractDetailsHandler;
import com.ib.controller.ApiController.ITopMktDataHandler;

public class TwsApi implements IConnectionHandler {
	
	private boolean connectedToTws = false;
	private boolean twsConnectedToIb = false;
	private boolean reconnect = true;
	private boolean txFinished = true;
	private static final String HIDDEN = "HIDDEN";
	private final ILogger inLogger = new NullLogger();
	private final ILogger outLogger = new NullLogger();
	private final ApiController apiController = new ApiController(this, inLogger, outLogger);
	private Config config = new Config();
	private static final boolean hideSensitveInfo = false;
	private static final Collection<AccountSummaryTag> allowedTags;
	
	static {
		allowedTags = new LinkedList<>();
		allowedTags.add(AccountSummaryTag.AccountType);
		allowedTags.add(AccountSummaryTag.Cushion);
		allowedTags.add(AccountSummaryTag.DayTradesRemaining);
		allowedTags.add(AccountSummaryTag.LookAheadNextChange);
	}
	
	public TwsApi() {
		try {
			config.init();
		} catch (IOException e) {
			System.err.println("Error loading config file");
			System.exit(0);
		}
	}

	public void reqAccountData() {
		runTwsOperation(() -> {
			txFinished = false;
			apiController.reqAccountSummary("All", AccountSummaryTag.values(), new ApiController.IAccountSummaryHandler() {
				@Override
				public void accountSummary(String account, AccountSummaryTag tag, String value, String currency) {
					Object[] values;
					if (hideSensitveInfo) {
						values = new Object[]{HIDDEN, tag, allowedTags.contains(tag)? value : HIDDEN , currency};
					} else {
						values = new Object[]{account, tag, value , currency};
					}
					System.out.format("Account Summary - account: %s, tag: %s, value: %s, currency %s%n", values);
				}
	
				@Override
				public void accountSummaryEnd() {
					System.out.println("Account Summary - Account Summary End");
					apiController.cancelAccountSummary(this);
					txFinished = true;
				}
			});
			waitUntilTxFinished();
		});
	}
	
	public void openOrderMkt(String symbol, int quantity, Action action) {
		Contract contract = getContract(symbol);
		
		Order order = new Order();
		order.orderType(OrderType.MKT);
		order.totalQuantity(quantity);
		order.action(action);
		order.transmit(config.isTransmit());
		
		placeOrModifyOrder(contract, order);
	}

	public synchronized void openOrderVwap(String symbol, int quantity, Action action) {
		Contract contract = getContract(symbol);
		
		Order order = new Order();
		order.algoStrategy(AlgoStrategy.Vwap);
		order.algoParams().add(new TagValue("startTime", "9:00:00 EST"));
		order.algoParams().add(new TagValue("endTime", "15:00:00 EST"));
		order.algoParams().add(new TagValue("maxPctVol", "0.20"));
		order.algoParams().add(new TagValue("noTakeLiq", "false"));
//		order.tif(TimeInForce.DAY);
//		order.displaySize(100);
		order.orderType(OrderType.MKT);
		order.totalQuantity(quantity);
		order.action(action);
		order.transmit(config.isTransmit());
		
		placeOrModifyOrder(contract, order);
	}
	
	public void openOrderArrivalPx(String symbol, int quantity, Action action) {
		Contract contract = getContract(symbol);
		
		Order order = new Order();
		order.algoParams().add(new TagValue("maxPctVol","0.20") );
		order.algoParams().add(new TagValue("riskAversion","Passive") );
		order.algoParams().add(new TagValue("startTime","9:00:00 EST") );
		order.algoParams().add(new TagValue("endTime","15:00:00 EST") );
		order.algoParams().add(new TagValue("forceCompletion","false") );
		order.algoParams().add(new TagValue("allowPastEndTime","true") );
		order.orderType(OrderType.MKT);
		order.algoStrategy(AlgoStrategy.ArrivalPx);
		order.totalQuantity(quantity);
		order.action(action);
		order.transmit(config.isTransmit());
		
		placeOrModifyOrder(contract, order);
	}
	
	private void placeOrModifyOrder(Contract contract, Order order) {
		runTwsOperation(() -> {
			if (order.transmit()) {
				txFinished = false;
			}
			apiController.placeOrModifyOrder(contract, order, new ApiController.IOrderHandler() {
				@Override
				public void orderState(OrderState orderState) {
				}
	
				@Override
				public void orderStatus(OrderStatus status, double filled, double remaining, double avgFillPrice,
						long permId, int parentId, double lastFillPrice, int clientId, String whyHeld) {
					System.out.format("orderStatus - orderId: %d, status: %s, filled: %f, remaining: %f, avgFillPrice: %f, permId: %d, parentId: %d, lastFillPrice: %f, clientId: %d, whyHeld%s%n",
							order.orderId(), status, filled, remaining, avgFillPrice, permId, parentId, lastFillPrice, clientId, whyHeld);
					txFinished = true;
				}
	
				@Override
				public void handle(int errorCode, String errorMsg) {
					System.out.format("handle - orderId: %d, errorCode: %s, errorMsg: %s%n", 
							order.orderId(), errorCode, errorMsg);
					txFinished = true;
				}


			});
			System.out.println("order id: " + order.orderId() + " submitted.");
			waitUntilTxFinished();
		});
	}
	
	public void internalReqContractDetails(String symbol) {
		runTwsOperation(() -> {
			txFinished = false;
			Contract contract = getContract(symbol);
			IContractDetailsHandler handler = new ApiController.IContractDetailsHandler() {
				@Override
				public void contractDetails(ArrayList<ContractDetails> list) {
					txFinished = true;
					for (ContractDetails newContractDetails : list) {
						System.out.println("Contract Details:");
						System.out.println(newContractDetails.toString());
					}
				}
			};
			apiController.reqContractDetails(contract, handler);
			waitUntilTxFinished();
		});
	}

	public void requestMktData(String symbol) {
		runTwsOperation(() -> {
			Contract contract = getContract(symbol);
			String genericTickList = null;
			boolean snapshot = true;
			txFinished = false;
			ITopMktDataHandler handler = new ApiController.ITopMktDataHandler() {
				@Override
				public void tickString(TickType tickType, String value) {
					System.out.format("tickString - tickType: %s, value: %s%n", tickType, value);
				}
				
				@Override
				public void tickSnapshotEnd() {
					System.out.println("ITopMktDataHandler.tickSnapshotEnd");
					txFinished = true;
				}
				
				@Override
				public void tickSize(TickType tickType, int size) {
					System.out.format("tickSize - tickType: %s, value: %d%n", tickType, size);
				}
				
				@Override
				public void tickPrice(TickType tickType, double price, int canAutoExecute) {
					System.out.format("tickPrice - tickType: %s, price: %f, canAutoExecute: %d%n", tickType, price, canAutoExecute);
				}
				
				@Override
				public void marketDataType(MktDataType marketDataType) {
					System.out.format("marketDataType - marketDataType: %s%n", marketDataType);

				}
			};
			apiController.reqTopMktData(contract, genericTickList, snapshot, handler);
			waitUntilTxFinished();
		});
	}
	
	private void runTwsOperation(TwsOperation twsOperation) {
		if (connectedToTws && twsConnectedToIb) {
			twsOperation.execute();
		} else if(!connectedToTws) {
			System.out.println("Not Connected to TWS.");
		} else {
			System.out.println("TWS not Connected to IB.");
		}
	}
	
	private Contract getContract(String symbol) {
		Contract contract = new Contract();
		contract.symbol(symbol);
		contract.exchange("SMART");
		contract.primaryExch("NASDAQ");
		contract.secType(Types.SecType.STK);
		contract.currency("USD");
		return contract;
	}
	
	private void waitUntilTxFinished() {
		while (!txFinished  && connectedToTws && twsConnectedToIb) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	public void end() {
		reconnect = false;
		apiController.disconnect();
	}

	public void connect() {
		apiController.connect(config.getTwsIp(), config.getTwsPort(), 0, "");
		if (reconnect) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
			while (!connectedToTws) {
				try {
					Thread.sleep(5000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				if (!connectedToTws) {
					System.out.println("retrying connect...");
					this.connect();
				}
			}
		}
	}

	@Override
	public void connected() {
		this.connectedToTws = true;
		this.twsConnectedToIb = true;
		System.out.println("connected");
	}
	@Override
	public void disconnected() {
		System.out.println("disconnected");
		this.connectedToTws = false;
		this.twsConnectedToIb = false;
		if (reconnect) {
			connect();
		}
	}
	@Override
	public void accountList(ArrayList<String> list) {
		System.out.println("Accounts: " + list);
	}
	@Override
	public void error(Exception e) {
		System.out.println("Error: " + e.getMessage());
	}
	@Override
	public void message(int id, int errorCode, String errorMsg) {
		System.out.println("Message [id: " + id + ", errorCode: " + errorCode + "]: " + errorCode);
		if (errorCode == 1100) {
			this.twsConnectedToIb = false;
		} else if (errorCode == 1101 || errorCode == 1102) {
			this.twsConnectedToIb = true;
		}
	}
	@Override
	public void show(String string) {
		System.out.println("Show: " + string);
	}
	
	@FunctionalInterface
	private interface TwsOperation {
		public void execute();
	}
	
}
