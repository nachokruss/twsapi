package com.twsapi;

import java.util.Scanner;

import com.ib.controller.Types.Action;
import com.ib.controller.Types.AlgoStrategy;

public class Main {
	
	private static final int REQUEST_MKT_DATA = 0;
	private static final int SUBMIT_MKT_ORDER = 1;
	private static final int SUBMIT_VWAP_ORDER = 2;
	private static final int SUBMIT_ARRIVAL_PX_ORDER = 3;
	private static final int REQ_ACCOUNT_SUMMARY = 4;
	private static final int EXIT = 5;
	
	TwsApi twsApi = new TwsApi();
	
	public static void main(String[] args) throws InterruptedException {
		Main test = new Main();
		test.connect();
		test.run();
	}
	
	public void run() {
		Scanner scanner = new Scanner(System.in);
		while (true) {
			printMainMenu();
			int menuOption = -1;
			while (menuOption == -1) {
				try {
					menuOption = scanner.nextInt();
				} catch (Exception e) {
					scanner.next();
				}
			}
			System.out.println("Selected option: " + menuOption);
			System.out.println("=====================================");
			try {
				switch (menuOption) {
				case REQ_ACCOUNT_SUMMARY:
					twsApi.reqAccountData();
					break;
				case REQUEST_MKT_DATA:
					requestMktData(scanner);
					break;
				case SUBMIT_MKT_ORDER:
					submitOrder(scanner);
					break;
				case SUBMIT_VWAP_ORDER:
					submitOrder(scanner, AlgoStrategy.Vwap);
					break;
				case SUBMIT_ARRIVAL_PX_ORDER:
					submitOrder(scanner, AlgoStrategy.ArrivalPx);
					break;
				case EXIT:
					exit(scanner);
					break;
				default:
					System.out.println("Wrong option: " + menuOption);
					break;
				}
			} catch (Exception e) {
				System.out.println("Unexpected error: " + e.getMessage() + ", Please try again.");
			}
		}
	}

	private void requestMktData(Scanner scanner) {
		String symbol = getSymbol(scanner);
		twsApi.requestMktData(symbol);
	}

	private void submitOrder(Scanner scanner) {
		this.submitOrder(scanner, null);
	}
	
	private void submitOrder(Scanner scanner, AlgoStrategy algo) {
		String symbol = getSymbol(scanner);
		int quantity = getQuantity(scanner);
		Action action = getAction(scanner);
		if (algo == null) {
			twsApi.openOrderMkt(symbol, quantity, action);
		} else if (algo == AlgoStrategy.Vwap) {
			twsApi.openOrderVwap(symbol, quantity, action);
		} else if (algo == AlgoStrategy.ArrivalPx) {
			twsApi.openOrderArrivalPx(symbol, quantity, action);
		} else {
			System.out.println("AlgoStratgey not supported");
		}
	}
	
	private String getSymbol(Scanner scanner) {
		System.out.println("Enter Symbol:");
		return scanner.next();
	}
	
	private int getQuantity(Scanner scanner) {
		System.out.println("Enter Quantity:");
		return scanner.nextInt();
	}
	
	private Action getAction(Scanner scanner) {
		System.out.println("Enter Action (BUY/SELL):");
		Action action = null;
		while (action == null) {
			String actionStr = scanner.next();
			try {
				action = Action.valueOf(actionStr);
			} catch (Exception e) {
				System.out.println("Invalid action, please try again:");
			}
		}
		return action;
	}
	
	private void exit(Scanner scanner) {
		System.out.println("Exiting program...");
		twsApi.end();
		scanner.close();
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		System.exit(0);
	}
	
	private void printMainMenu() {
		System.out.println("");
		System.out.println("=====================================");
		System.out.println("Please select an option:");
		System.out.println(" " + REQUEST_MKT_DATA + ": Request Market Data");
		System.out.println(" " + SUBMIT_MKT_ORDER + ": Submit MKT Order");
		System.out.println(" " + SUBMIT_VWAP_ORDER + ": Submit VWAP Order");
		System.out.println(" " + SUBMIT_ARRIVAL_PX_ORDER + ": Submit ArrivalPx Order");
		System.out.println(" " + REQ_ACCOUNT_SUMMARY + ": Request Account Summary");
		System.out.println(" " + EXIT + ": Exit");
		System.out.println("=====================================");
	}
	
	private void connect() {
		twsApi.connect();
	}

}
