package com.twsapi;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;

import com.ib.controller.AccountSummaryTag;
import com.ib.controller.ApiConnection.ILogger;
import com.ib.controller.ApiController;
import com.ib.controller.ApiController.IConnectionHandler;
import com.ib.controller.ApiController.IHistoricalDataHandler;
import com.ib.controller.Bar;
import com.ib.controller.NewContract;
import com.ib.controller.Types;
import com.ib.controller.Types.BarSize;
import com.ib.controller.Types.DurationUnit;
import com.ib.controller.Types.WhatToShow;

public class AccountSummaryAndHistoricDataTest implements IConnectionHandler {
	
	private static final String HIDDEN = "HIDDEN";
	private final ILogger inLogger = new NullLogger();
	private final ILogger outLogger = new NullLogger();
	private final ApiController apiController = new ApiController(this, inLogger, outLogger);
	private static final Collection<AccountSummaryTag> allowedTags;
	
	static {
		allowedTags = new LinkedList<>();
		allowedTags.add(AccountSummaryTag.AccountType);
		allowedTags.add(AccountSummaryTag.Cushion);
		allowedTags.add(AccountSummaryTag.DayTradesRemaining);
		allowedTags.add(AccountSummaryTag.LookAheadNextChange);
	}

	public static void main(String[] args) throws InterruptedException {
		AccountSummaryAndHistoricDataTest test = new AccountSummaryAndHistoricDataTest();
		test.start();
		Thread.sleep(2000);
		System.out.println("====================");
		test.reqAccountData();
		Thread.sleep(1000);
		System.out.println("====================");
		test.reqHistoricalData();
		Thread.sleep(1000);
		System.out.println("====================");
		test.end();
	}
	
	private void reqAccountData() {
		apiController.reqAccountSummary("All", AccountSummaryTag.values(), new ApiController.IAccountSummaryHandler() {
			@Override
			public void accountSummary(String account, AccountSummaryTag tag, String value, String currency) {
				System.out.format("account: %s, tag: %s, value: %s, currency %s%n", HIDDEN, tag,
						allowedTags.contains(tag) ? value : HIDDEN, currency);
			}

			@Override
			public void accountSummaryEnd() {
				System.out.println("Account Summary End");
			}
		});
	}

	private void reqHistoricalData() {
		NewContract contract = new NewContract();
		contract.symbol("IBM");
		contract.exchange("Smart");
		contract.secType(Types.SecType.STK);
		contract.currency("USD");
		IHistoricalDataHandler handler = new IHistoricalDataHandler() {
			@Override
			public void historicalData(Bar bar, boolean hasGaps) {
				System.out.println("IHistoricalDataHandler.historicalData");
			}

			@Override
			public void historicalDataEnd() {
				System.out.println("IHistoricalDataHandler.historicalDataEnd");
			}
		};
		String endDateTime = "20160628 12:00:00";
		int duration = 10;
		DurationUnit durationUnit = DurationUnit.DAY;
		BarSize barSize = BarSize._15_mins;
		WhatToShow whatToShow = WhatToShow.ASK;
		boolean rthOnly = false;
		apiController.reqHistoricalData(contract, endDateTime, duration, durationUnit, barSize, whatToShow, rthOnly,
				handler);
	}
	
	private void end() {
		apiController.disconnect();
	}

	private void start() {
		apiController.connect("127.0.0.1", 7496, 0);
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
