package MoneyRec.src;
import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeMap;

import com.bloomberglp.blpapi.CorrelationID;
import com.bloomberglp.blpapi.Element;
import com.bloomberglp.blpapi.Event;
import com.bloomberglp.blpapi.Message;
import com.bloomberglp.blpapi.MessageIterator;
import com.bloomberglp.blpapi.Request;
import com.bloomberglp.blpapi.Service;
import com.bloomberglp.blpapi.Session;
import com.bloomberglp.blpapi.SessionOptions;


public class MoneyRecApi {
	
	private static TreeMap<Double, String> recomendationList=new TreeMap<Double, String>();
	private static TreeMap<Double, String> idDictionary = new TreeMap<Double, String>();
	private static TreeMap<String, ArrayList<Double>> idTickData = new TreeMap<String, ArrayList<Double>>();
	private static TreeMap<Integer, ArrayList<Double>> topIdTickData = new TreeMap<Integer, ArrayList<Double>>();
	private static HashMap<String, String> idNameMap = new HashMap<String, String>();


	private static void handleResponseEvent(Event event) throws Exception 
	{
	 System.out.println("EventType =" + event.eventType());
	 MessageIterator iter = event.messageIterator();
	 while (iter.hasNext()) {
	 Message message = iter.next();
	 processHistoricalDataResponse(message);
	 }
	 }

	private static void handleOtherEvent(Event event) throws Exception
	 {
	 System.out.println("EventType=" + event.eventType());
	 MessageIterator iter = event.messageIterator();
	 while (iter.hasNext()) {
	 Message message = iter.next();
	 System.out.println("correlationID=" +
	 message.correlationID());
	 System.out.println("messageType=" + message.messageType());
	 message.print(System.out);
	 if (Event.EventType.Constants.SESSION_STATUS ==
	 event.eventType().intValue()
	 && "SessionTerminated" == 
	message.messageType().toString()){
	 System.out.println("Terminating: " +
	 message.messageType());
	 System.exit(1);
	 }
	 }
	 }
	
	private static void processHistoricalDataResponse(Message msg) throws 
	Exception {
	ArrayList<Double> idtick = new ArrayList<Double>(); 
	 Element securityData = msg.getElement("securityData");
	 Element fieldDataArray = securityData.getElement("fieldData");
	 System.out.println(securityData.getElement(0).getValueAsString());

//	 Thread.sleep(20000);
	 int previousFlag=0,weekCntr=0;
	 Double prevClosing=0.0;
	 
	 //Sstting up state transition Matrix
	 Double movngAvg=0.0;
	 Double[][] localStateMatrix = {
			 {0.0,0.0,0.0},
			 {0.0,0.0,0.0},
			 {0.0,0.0,0.0}
			 };
	 int flagPx1=0,flagPx2=0,flagPx3=0;
	 for (int j = 0; j < fieldDataArray.numValues(); ++j) {
	 Element fieldData = fieldDataArray.getValueAsElement(j);	

	 int flag=0;
	 Double closVal=0.0,Openval=0.0;
	 if(fieldData.numElements()>2){
		 closVal= Double.parseDouble(fieldData.getElement(1).getValueAsString());
		 Openval=Double.parseDouble(fieldData.getElement(2).getValueAsString());
	 }
	 

	 if((closVal-Openval)>.12)
	 {
		 flag = 1;
		 flagPx1++;
	 }
	 else if((closVal-Openval)<-.12)
	 {
		 flag = -1;
		 flagPx3++;
	 }
	 else
	 {
		 flag = 0;
		 flagPx2++;
	 }
	 
	 if(j>0)
	 {
		 localStateMatrix[1-(int)previousFlag][1-(int)flag]+=1.0;
	 }
	 
	 
	 //calculating moving averages
	 if(j>150)
	 {
		 weekCntr++;
		 movngAvg+=(closVal-prevClosing)/prevClosing;
		 idtick.add(closVal);
	 }
	 
	 prevClosing=closVal;
	 previousFlag = flag;
	 }

	 
	 localStateMatrix[0][0]/=flagPx1;
	 localStateMatrix[0][1]/=flagPx1;
	 localStateMatrix[0][2]/=flagPx1;
	 localStateMatrix[1][0]/=flagPx2;
	 localStateMatrix[1][1]/=flagPx2;
	 localStateMatrix[1][2]/=flagPx2;
	 localStateMatrix[2][0]/=flagPx3;
	 localStateMatrix[2][1]/=flagPx3;
	 localStateMatrix[2][2]/=flagPx3;
	 
	 Double[] stateProb = {1.0,0.0,0.0};
	 Double averageProb = StateMatrix(localStateMatrix, stateProb,4);
	 movngAvg/=weekCntr;
	 
	 // expected earning per dollar ...enjoy :)
	 Double expectedProfit=expextedValue(movngAvg,averageProb);
	 
	 String equityName=securityData.getElement(0).getValueAsString();
	 equityName=equityName.replaceAll("\\n", "");
	 if(!expectedProfit.isNaN())		 
		recomendationList.put(expectedProfit, equityName);
	 	idTickData.put(equityName, idtick);
	}
	
	public static Double expextedValue(Double movavg,Double avgProb){
		Double finalExpectationOfProfit=0.0;
		finalExpectationOfProfit=movavg*avgProb;
		return finalExpectationOfProfit;
	}
	
	public static Double StateMatrix(Double[][] localStateMatrix,Double[] stateProb,int weeks)
	{
		Double averageProb=0.0;
		for(int i=0;i<weeks;i++)
		{
			Double[] newStateProb = new Double[3];
			newStateProb[0] = stateProb[0]*localStateMatrix[0][0] + stateProb[1]*localStateMatrix[1][0] + stateProb[2]*localStateMatrix[2][0];
			newStateProb[1] = stateProb[0]*localStateMatrix[0][1] + stateProb[1]*localStateMatrix[1][1] + stateProb[2]*localStateMatrix[2][1];
			newStateProb[2] = stateProb[0]*localStateMatrix[0][2] + stateProb[1]*localStateMatrix[1][2] + stateProb[2]*localStateMatrix[2][2];
			averageProb+=newStateProb[0];
		}		
		averageProb/=weeks;
		return averageProb;
	}
	
	public static void main(String[] args) throws Exception {
		String[] split=null;
		String[] idsplit=null;
		 SessionOptions sessionOptions = new SessionOptions();
		 sessionOptions.setServerHost("10.8.8.1"); // default value
		 sessionOptions.setServerPort(8194); // default value
		 Session session = new Session(sessionOptions);
		 if (!session.start()) {
		 System.out.println("Could not start session.");
		 System.exit(1);
		 }
		 if (!session.openService("//blp/refdata")) {
		 System.out.println("Could not open service " +
		 "//blp/refdata");
		 System.exit(1);
		 }
		 CorrelationID requestID = new CorrelationID(1);
//		 Service refDataSvc = session.getService("//blp/refdata");
//		 Request request =
//		 refDataSvc.createRequest("ReferenceDataRequest");
//		 request.append("securities", "IBM US Equity");
//		 request.append("fields", "PX_LAST");
		 Service refDataService = session.getService("//blp/refdata");
		 Request request =  refDataService.createRequest("HistoricalDataRequest");
		 try
		 {
		 	File file = new File(args[1]);
		 	if(file.canRead())
		 	{
		 		RandomAccessFile accessFile = new RandomAccessFile(file, "r");
		 		byte[] bFile = new byte[(int)file.length()];
		 		accessFile.read(bFile,0,(int)file.length());
		 		
		 		String content = new String(bFile);
		 		split = content.split("\\n");
		 		for(int i=0;i<split.length;i++)
		 			request.append("securities",split[i]+" US Equity");
		 		accessFile.close();
		 	}
		 	File idfile = new File(args[0]);
		 	if(idfile.canRead())
		 	{
		 		RandomAccessFile accessFile = new RandomAccessFile(idfile, "r");
		 		byte[] bidFile = new byte[(int)idfile.length()];
		 		accessFile.read(bidFile,0,(int)idfile.length());
		 		
		 		String content = new String(bidFile);
		 		idsplit = content.split("\\n");
		 		
		 		for(String s:idsplit){
		 			String[] tok=s.split(",");
		 			if (tok.length>1){
		 				idNameMap.put(tok[0], tok[1]);
		 			}
		 			else{
		 				System.out.println("csv file not proper for id -name mapping");
		 			}
		 		}
		 		accessFile.close();
		 	}
		 }
		 catch(Exception ioe)
		 {			 
		 }
		 
//		 request.append("securities", "IBM US Equity");
//		 request.append("securities", "MSFT US Equity");
//		 request.append("securities", "INFY US Equity");
//		 request.append("securities", "ACN US Equity");
//		 request.append("securities", "AAPL US Equity");
		 request.append("fields", "PX_LAST");
		 request.append("fields", "OPEN");
		 request.set("startDate", "20100901");
		 request.set("endDate", "20141031");
		 request.set("periodicitySelection", "WEEKLY");
		 session.sendRequest(request, requestID);
		 boolean continueToLoop = true;
		 while (continueToLoop) {
		 Event event = session.nextEvent();
		 switch (event.eventType().intValue()) {
		 case Event.EventType.Constants.RESPONSE: // final event
		 continueToLoop = false; // fall through
		 case Event.EventType.Constants.PARTIAL_RESPONSE:
		 handleResponseEvent(event);
		 break;
		 default:
		 handleOtherEvent(event);
		 break;
		 }
		 }
		 System.out.println(recomendationList);
		 //preparing id-name index and id-tickdata index
		 Iterator<Double> reclist=recomendationList.descendingKeySet().iterator();
		 int lstcntr=0;
		 while(reclist.hasNext()){
			 lstcntr++;
			 double expextedret=reclist.next();
			 String key=recomendationList.get(expextedret);
			 
			 
			//build top k tickdata
			 if(idTickData.containsKey(key)){
				 topIdTickData.put(lstcntr, idTickData.get(key));
			 }else{
				 System.out.println("error!!!tickdata");
			 }
			 
			 key=key.substring(0, key.length()-10).trim();
			 if(idNameMap.containsKey(key)){
				 idDictionary.put(expextedret, idNameMap.get(key));
			 }else{
				 
				 System.out.println("error!!!dictionory");
			 }
			 
			 if(lstcntr==30)break;
		 }
		 //writing id-name index to disc
		 String filename=args[0].replaceAll(" ", "");
		 RandomAccessFile rafterm = new RandomAccessFile(filename+"id-name", "rw");
			ObjectOutputStream idNameIndex=new ObjectOutputStream(new FileOutputStream(rafterm.getFD()));
			idNameIndex.writeObject(idDictionary);
			idNameIndex.close();
			rafterm.close();
			
			 //writing id-tickdata index to disc
			 String tickfile=args[0].replaceAll(" ", "");
			 RandomAccessFile tickterm = new RandomAccessFile(tickfile+"tick", "rw");
				ObjectOutputStream tickindex=new ObjectOutputStream(new FileOutputStream(tickterm.getFD()));
				tickindex.writeObject(topIdTickData);
				tickindex.close();
				tickterm.close();
				
				System.out.println(idDictionary);
				recomendationList.clear();
				idDictionary.clear();
				idTickData.clear();
				topIdTickData.clear();
				idNameMap.clear();
		
		 }

	
}
