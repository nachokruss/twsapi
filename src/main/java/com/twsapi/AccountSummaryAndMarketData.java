package com.twsapi;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;

import com.ib.controller.AccountSummaryTag;
import com.ib.controller.ApiConnection.ILogger;
import com.ib.controller.ApiController;
import com.ib.controller.ApiController.IConnectionHandler;
import com.ib.controller.ApiController.ITopMktDataHandler;
import com.ib.controller.NewContract;
import com.ib.controller.NewTickType;
import com.ib.controller.Types;
import com.ib.controller.Types.MktDataType;

public class AccountSummaryAndMarketData implements IConnectionHandler {
	
	private static final String HIDDEN = "HIDDEN";
	private final ILogger inLogger = new NullLogger();
	private final ILogger outLogger = new NullLogger();
	private final ApiController apiController = new ApiController(this, inLogger, outLogger);
	private static final boolean hideSensitveInfo = false;
	private static final Collection<AccountSummaryTag> allowedTags;
	
	static {
		allowedTags = new LinkedList<>();
		allowedTags.add(AccountSummaryTag.AccountType);
		allowedTags.add(AccountSummaryTag.Cushion);
		allowedTags.add(AccountSummaryTag.DayTradesRemaining);
		allowedTags.add(AccountSummaryTag.LookAheadNextChange);
	}

	public static void main(String[] args) throws InterruptedException {
		AccountSummaryAndMarketData test = new AccountSummaryAndMarketData();
		test.start();
		Thread.sleep(2000);
		System.out.println("====================");
		System.out.println("Acount Summary:");
		test.reqAccountData();
		Thread.sleep(1000);
		System.out.println("====================");
		System.out.println("IBM Market Data:");
		test.requestMktData("IBM");
		Thread.sleep(1000);
		System.out.println("====================");
		test.end();
	}
	
	private void reqAccountData() {
		apiController.reqAccountSummary("All", AccountSummaryTag.values(), new ApiController.IAccountSummaryHandler() {
			@Override
			public void accountSummary(String account, AccountSummaryTag tag, String value, String currency) {
				Object[] values;
				if (hideSensitveInfo) {
					values = new Object[]{HIDDEN, tag, allowedTags.contains(tag)? value : HIDDEN , currency};
				} else {
					values = new Object[]{account, tag, value , currency};
				}
				System.out.format("account: %s, tag: %s, value: %s, currency %s%n", values);
			}

			@Override
			public void accountSummaryEnd() {
				System.out.println("Account Summary End");
			}
		});
	}

	private void requestMktData(String symbol) {
		NewContract contract = new NewContract();
		contract.symbol(symbol);
		contract.exchange("SMART");
		contract.secType(Types.SecType.STK);
		contract.currency("USD");
		String genericTickList = null;
		boolean snapshot = false;
		ITopMktDataHandler handler = new ITopMktDataHandler() {
			@Override
			public void tickString(NewTickType tickType, String value) {
				System.out.format("tickString - tickType: %s, value: %s%n", tickType, value);
			}
			
			@Override
			public void tickSnapshotEnd() {
				System.out.println("ITopMktDataHandler.tickSnapshotEnd");
			}
			
			@Override
			public void tickSize(NewTickType tickType, int size) {
				System.out.format("tickSize - tickType: %s, value: %d%n", tickType, size);
			}
			
			@Override
			public void tickPrice(NewTickType tickType, double price, int canAutoExecute) {
				System.out.format("tickPrice - tickType: %s, price: %f, canAutoExecute: %d%n", tickType, price, canAutoExecute);
			}
			
			@Override
			public void marketDataType(MktDataType marketDataType) {
				System.out.format("marketDataType - marketDataType: %s%n", marketDataType);
			}
		};
		apiController.reqTopMktData(contract, genericTickList, snapshot, handler);
	}
	
	private void end() {
		apiController.disconnect();
	}

	private void start() {
		apiController.connect("127.0.0.1", 7497, 0);
	}

	@Override
	public void connected() {
		System.out.println("connected");
	}
	@Override
	public void disconnected() {
		System.out.println("disconnected");
	}
	@Override
	public void accountList(ArrayList<String> list) {
		System.out.println("accountList");
	}
	@Override
	public void error(Exception e) {
		System.out.println("error: " + e.getMessage());
	}
	@Override
	public void message(int id, int errorCode, String errorMsg) {
		System.out.println("message [id: " + id + ", errorCode: " + errorCode + "]: " + errorCode);
	}
	@Override
	public void show(String string) {
		System.out.println("show: " + string);
	}
	
}
