package com.ashrafishak.batchauth;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.logging.LogManager;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;


import com.paypal.exception.ClientActionRequiredException;
import com.paypal.exception.HttpErrorException;
import com.paypal.exception.InvalidCredentialException;
import com.paypal.exception.InvalidResponseDataException;
import com.paypal.exception.MissingCredentialException;
import com.paypal.exception.SSLConfigurationException;
import com.paypal.sdk.exceptions.OAuthException;

import urn.ebay.api.PayPalAPI.DoAuthorizationReq;
import urn.ebay.api.PayPalAPI.DoAuthorizationRequestType;
import urn.ebay.api.PayPalAPI.DoAuthorizationResponseType;
import urn.ebay.api.PayPalAPI.GetTransactionDetailsReq;
import urn.ebay.api.PayPalAPI.GetTransactionDetailsRequestType;
import urn.ebay.api.PayPalAPI.GetTransactionDetailsResponseType;
import urn.ebay.api.PayPalAPI.PayPalAPIInterfaceServiceService;
import urn.ebay.api.PayPalAPI.TransactionSearchReq;
import urn.ebay.api.PayPalAPI.TransactionSearchRequestType;
import urn.ebay.api.PayPalAPI.TransactionSearchResponseType;
import urn.ebay.apis.CoreComponentTypes.BasicAmountType;
import urn.ebay.apis.eBLBaseComponents.AbstractResponseType;
import urn.ebay.apis.eBLBaseComponents.AckCodeType;
import urn.ebay.apis.eBLBaseComponents.CurrencyCodeType;
import urn.ebay.apis.eBLBaseComponents.PaymentTransactionSearchResultType;
import urn.ebay.apis.eBLBaseComponents.PaymentTransactionStatusCodeType;
import urn.ebay.apis.eBLBaseComponents.PendingStatusCodeType;

public class Main {


	private static PayPalAPIInterfaceServiceService service;

	public Main (String mode, String apiUsername, String apiPassword, String apiSignature){
		// Initialize the API service here
		HashMap<String,String> configurationMap =  new HashMap<String,String>();
		configurationMap.put("mode", mode.toLowerCase());
		configurationMap.put("acct1.UserName", apiUsername);
		configurationMap.put("acct1.Password", apiPassword);
		configurationMap.put("acct1.Signature", apiSignature);
		service = new PayPalAPIInterfaceServiceService(configurationMap);
	}
	
	public PayPalAPIInterfaceServiceService getService(){
		return service;
	}

	// Run one API operation from different type
	public AbstractResponseType runSingleAPI(PayPalAPIInterfaceServiceService service, Object request, Operation operation){
		try {
			if (operation.equals(Operation.DOAUTHORIZATION)){
				DoAuthorizationReq casted = (DoAuthorizationReq) request;
				return service.doAuthorization(casted);
			} else if (operation.equals(Operation.TRANSACTIONSEARCH)){
				TransactionSearchReq casted = (TransactionSearchReq) request;
				return service.transactionSearch(casted);
			} else if (operation.equals(Operation.TRANSACTIONDETAILS)) {
				GetTransactionDetailsReq casted = (GetTransactionDetailsReq) request;
				return service.getTransactionDetails(casted);
			} else {
				try {
					throw new Exception("The operation is not supported");
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		} catch (SSLConfigurationException e) {
			e.printStackTrace();
		} catch (InvalidCredentialException e) {
			e.printStackTrace();
		} catch (HttpErrorException e) {
			e.printStackTrace();
		} catch (InvalidResponseDataException e) {
			e.printStackTrace();
		} catch (ClientActionRequiredException e) {
			e.printStackTrace();
		} catch (MissingCredentialException e) {
			e.printStackTrace();
		} catch (OAuthException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		}
		return null;
	}

	// Run one order authorization
	public DoAuthorizationReq buildSingleAuth(String authId, Double amtStr, String currency){
		DoAuthorizationReq req = new DoAuthorizationReq();
		BasicAmountType amount = new BasicAmountType(CurrencyCodeType.fromValue(currency), String.valueOf(amtStr));
		DoAuthorizationRequestType reqType = new DoAuthorizationRequestType(authId, amount);
		req.setDoAuthorizationRequest(reqType);
		return req;
	}

	// 1) Get list of authorizations that need to be done 
	// 2) For each element, run authorization on the orders and see if there is error
	public Map<String, DoAuthorizationResponseType> runBatchAuthSDK(PayPalAPIInterfaceServiceService service, List<DoAuthorizationReq> listAuth){
		if (listAuth != null && listAuth.size() > 0){
			HashMap<String, DoAuthorizationResponseType> responses = new HashMap<String, DoAuthorizationResponseType>();
			for (DoAuthorizationReq req: listAuth){
				AbstractResponseType response = runSingleAPI(service, req, Operation.DOAUTHORIZATION);
				if (response != null){
					responses.put(req.getDoAuthorizationRequest().getTransactionID(), (DoAuthorizationResponseType) response);
				} else {
					responses.put(req.getDoAuthorizationRequest().getTransactionID(), null);
				}
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			return responses;
		} else {
			return null;
		}
		
	}
	
	
	// 1) Search transactions with status = pending
	// 2) From txn detail, if the txn has pending reason = order, add into list
	public List<DoAuthorizationReq> getListAuthFromAPI(PayPalAPIInterfaceServiceService service,
													   String startDate, String endDate){
		ArrayList<DoAuthorizationReq> results = new ArrayList<DoAuthorizationReq>();

		TransactionSearchReq txnreq = new TransactionSearchReq();
		TransactionSearchRequestType type = new TransactionSearchRequestType();
		type.setStartDate(startDate);
		type.setEndDate(endDate);
		type.setStatus(PaymentTransactionStatusCodeType.PENDING);
		txnreq.setTransactionSearchRequest(type);
		
		TransactionSearchResponseType response = (TransactionSearchResponseType) 
												 runSingleAPI(service, txnreq, Operation.TRANSACTIONSEARCH);
		if (response != null && response.getPaymentTransactions().size() > 0){
			for (PaymentTransactionSearchResultType ind: response.getPaymentTransactions()){
				String transactionID = ind.getTransactionID();
				// Now, get the transaction detail
				GetTransactionDetailsReq req = new GetTransactionDetailsReq();
				GetTransactionDetailsRequestType reqType = new GetTransactionDetailsRequestType();
				reqType.setTransactionID(transactionID);
				req.setGetTransactionDetailsRequest(reqType);
				GetTransactionDetailsResponseType details = (GetTransactionDetailsResponseType)
														runSingleAPI(service, req, Operation.TRANSACTIONDETAILS);
				if (details.getAck().equals(AckCodeType.SUCCESS)){
					if (details.getPaymentTransactionDetails().getPaymentInfo().getPendingReason().equals(PendingStatusCodeType.ORDER)){
						Double amount = Double.valueOf(details.getPaymentTransactionDetails().getPaymentInfo().getGrossAmount().getValue());
						String currency = details.getPaymentTransactionDetails().getPaymentInfo().getGrossAmount().getCurrencyID().getValue();
						DoAuthorizationReq doAuth = buildSingleAuth(transactionID, amount, currency);
						results.add(doAuth);
					}
				}
				
			}
		}
		
		
		return results;
	}
	
	
	// TODO: Better file handling
	public List<DoAuthorizationReq> getListAuthFromFile(String inputFileName, String currency){
		File inputFile = new File(inputFileName); // The code should be better than this
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(inputFile));
			ArrayList<DoAuthorizationReq> results = new ArrayList<DoAuthorizationReq>();
			while (br.ready()){
				String line = br.readLine();
				if (line.contains(",")){
					String[] splitted = line.split(",");
					if (splitted.length == 2){
						String origId = URLDecoder.decode(splitted[0].trim(),"UTF-8");
						double amount = Double.valueOf(URLDecoder.decode(splitted[1].trim(),"UTF-8"));
						DoAuthorizationReq newReq = buildSingleAuth(origId, amount, currency);
						results.add(newReq);
					} else {
						return null;
					}
				} else {
					return null;
				}
			}
			br.close();
			return results;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			try {
				br.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return null;
	}
	
	
	// Input = map of <order id, doauthresponse>
	public String generateResultString(Map<String, DoAuthorizationResponseType> auths){
		if (auths.size() > 0){
			String resultStr = "";
			for (String orderId: auths.keySet()){
				DoAuthorizationResponseType resp = auths.get(orderId);
				if (resp != null){
					if (resp.getAck().equals(AckCodeType.SUCCESS)){
						Double amount = Double.valueOf(resp.getAmount().getValue());
						String currency = resp.getAmount().getCurrencyID().getValue();
						resultStr += "[SUCCESS] original order id = "+orderId+"; " +
								"new auth id = "+resp.getTransactionID()+"; "+
								"amount = "+currency+amount;
					} else {
						// No need for all errors
						String errorCode = resp.getErrors().get(0).getErrorCode();
						String errorMessage = resp.getErrors().get(0).getLongMessage();
						resultStr += "[FAILURE] original order id = "+orderId+"; "+
								"error code = "+errorCode+"; "+
								"error message = "+errorMessage;
					}
				} else {
					resultStr += "[FAILURE] original order id = "+orderId+"; " +
								"error = error while submitting API";
				}
				
				resultStr+= "\n";
			}
			return resultStr;
		} else {
			return null;
		}		
	}
	
	public String generateResultOverview(Map<String, DoAuthorizationResponseType> auths){
		if (auths.size() > 0){
			int success = 0;
			int failed = 0;
			for (String orderId: auths.keySet()){
				DoAuthorizationResponseType resp = auths.get(orderId);
				if (resp != null){
					if (resp.getAck().equals(AckCodeType.SUCCESS)){
						success++;
					} else {
						failed++;
					}
				} else {
					failed++;
				}
				
			}
			return "[END] successful count = "+success+"; failed count = "+failed+"; total = "+auths.size();
		} else {
			return null;
		}				
	}
	
	
	public String currentUTCFormat(){
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(System.currentTimeMillis());
		return new SimpleDateFormat("yyyy-MM-dd'T'kk:mm:ss'Z'").format(cal.getTime());
	}

	public String toUTCFormat(String date, boolean isStartDate){
		Calendar cal = Calendar.getInstance();
		Pattern pat = Pattern.compile("^(\\d\\d\\d\\d)\\-(\\d\\d)\\-(\\d\\d)$");
		Matcher matcher = pat.matcher(date);
		int year = 0;
		int month = 0;
		int day = 0;
		while (matcher.find()){
			year = Integer.valueOf(matcher.group(1));
			month = Integer.valueOf(matcher.group(2));
			day = Integer.valueOf(matcher.group(3));
		}

		if (isStartDate){
			cal.set(year, month - 1, day, 0, 0, 0);
		} else {
			cal.set(year, month - 1, day, 23, 59, 59);
		}

		return new SimpleDateFormat("yyyy-MM-dd'T'kk:mm:ss'Z'").format(cal.getTime());
	}



	public static void main(String[] args) {
		// To remove the logging statements from SDK
		LogManager.getLogManager().reset();
		
		Scanner sc = new Scanner(System.in);
		System.out.println("---- Order Authorization Tool ----");
		System.out.print("Enter API username = ");
		String apiUsername = sc.next();
		System.out.print("Enter API password = ");
		String apiPassword = sc.next();
		System.out.print("Enter API signature = ");
		String apiSignature = sc.next();
		
		String modeStr = "";
		do {
			System.out.print("Enter mode (Sandbox/Live) = ");
			modeStr = sc.next();
			if (modeStr.equals("Sandbox") || modeStr.equals("sandbox") || modeStr.equals("Live") || modeStr.equals("live")){
			} else {
				System.out.println("[ERROR] Wrong mode entered.");
			}		
		} while (modeStr == null);

		Main main = new Main(modeStr, apiUsername, apiPassword, apiSignature);
		PayPalAPIInterfaceServiceService serv = main.getService();
		
		
		int choice = 0;
		do {
			System.out.println("Select application mode: \n");
			System.out.println("1) Single order authorization \n"
							 + "2) Multiple orders authorization from file \n"
							 + "3) Multiple orders authorization from API \n");
			choice = sc.nextInt();
			if (choice != 1 && choice != 2 && choice != 3){
				System.out.println("[ERROR] Wrong choice entered. Please try again. \n");
			}
		} while (choice != 1 && choice != 2 && choice != 3);

		if (choice == 1){
			System.out.println("*** Single Order Authorization ***");
			System.out.print("Enter order Id = ");
			String origId = sc.next();
			System.out.print("Enter amount to be authorized = ");
			double amount = sc.nextDouble();
			System.out.print("Enter currency = ");
			String currency = sc.next();
			System.out.println("Processing authorization .....\n\n");
			DoAuthorizationReq req = main.buildSingleAuth(origId, amount, currency);
			DoAuthorizationResponseType result = (DoAuthorizationResponseType) main.runSingleAPI(serv, req, Operation.DOAUTHORIZATION);
			HashMap<String, DoAuthorizationResponseType> map = 
							new HashMap<String, DoAuthorizationResponseType>();
			map.put(origId, result);
			String display = main.generateResultString(map);
			System.out.println(display);
		} else if (choice == 2){
			System.out.println("*** Multiple Order Authorization (From File) ***");
			System.out.println("[Note] Please make sure you have placed the input file in the same directory as this JAR file. More information on README");
			System.out.print("Enter the input file name = ");
			String fileName = sc.next();
			System.out.print("Enter currency = ");
			String currency = sc.next();
			System.out.println("Processing authorizations in batch. Please wait.....\n\n");
			// Start processing in batch
			List<DoAuthorizationReq> list = main.getListAuthFromFile(fileName, currency);
			Map<String, DoAuthorizationResponseType> map = main.runBatchAuthSDK(serv, list);
			// Display result overview, and write log into a file
			System.out.println(main.generateResultOverview(map));
			String batchResult = main.generateResultString(map);
			PrintWriter pw = null;
			String logFileName = "batch_file_"+System.currentTimeMillis()+".log";
			try {
				pw = new PrintWriter(new File(logFileName));
				pw.write(batchResult);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} finally {
				pw.close();
			}
			System.out.println("The batch result has been logged into file "+logFileName);
		} else if (choice == 3) {
			System.out.println("*** Multiple Order Authorization (From API) ***");
			System.out.println("[Note] This mode will search your account for any pending orders and run authorizations on the orders");
			System.out.print("Enter start date (YYYY-MM-DD) = ");
			String startDate = sc.next();
			System.out.print("Enter end date (YYYY-MM-DD) = ");
			String endDate = sc.next();
			System.out.println("Processing authorizations in batch. Please wait.....\n\n.");
			// Start processing in batch
			String startDateStr = "";
			String endDateStr = "";
			if (startDate != null && startDate.length() > 0){
				startDateStr = main.toUTCFormat(startDate, true);
			} else {
				System.out.println("[ERROR] Start date is not entered");
			}
			if (endDate != null && startDate.length() > 0){
				endDateStr = main.toUTCFormat(endDate, false);
			} else {
				endDateStr = main.currentUTCFormat();
			}
			List<DoAuthorizationReq> list = main.getListAuthFromAPI(serv, startDateStr,
					endDateStr);
			Map<String, DoAuthorizationResponseType> map = main.runBatchAuthSDK(serv, list);
			// Display result overview, and write log into a file
			System.out.println(main.generateResultOverview(map));
			String batchResult = main.generateResultString(map);
			PrintWriter pw = null;
			String logFileName = "batch_file_"+System.currentTimeMillis()+".log";
			try {
				pw = new PrintWriter(new File(logFileName));
				pw.write(batchResult);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} finally {
				pw.close();
			}
			System.out.println("The batch result has been logged into file "+logFileName);
			
		}
		
	

	}


}
