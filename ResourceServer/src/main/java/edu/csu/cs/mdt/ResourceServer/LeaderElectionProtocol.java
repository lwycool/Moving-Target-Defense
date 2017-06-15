package edu.csu.cs.mdt.ResourceServer;

import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.*;
import java.net.*;
import java.net.ServerSocket;
import java.security.Signature;

import org.apache.log4j.Logger;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.SECONDS;


import org.mitre.secretsharing.*;
import org.mitre.secretsharing.codec.PartFormats;

public class LeaderElectionProtocol{	
	
	ServerInfo currentServerInfo = new ServerInfo("",0,0);
	private int ServerConfiguration = -1;
	ResourceServer resServer = null;
	
	private int CurrentCoordinator = 1;
	
	private int TotalNodes = -1; //Get this from a configuration file.
	//private int TotalBlackListedNodes = 0;
	
	private ServerSocket LEServerSocket = null;
	private Socket Sendsocket = null;

	
	//private DataInputStream  streamIn  =  null;
	private DataOutputStream streamOut = null;
	

	private boolean isSelectReceived = false;
	private boolean isEstimateReceived = false;
	private boolean isSuspectReceived = false;
	private boolean isServerRegisterd = false;
	
	private boolean ProtocolStop = true;
	
	private int TotalRounds = 0;
	
	InputStream inputStream;
	
	private List<ServerInfo> nodesList = null;
	//private List<ServerInfo> BlackListedNodesList = null;
	private volatile List<Integer> byzsetNodes = null;
	private volatile List<Integer> OutputD1 =null;
	private volatile List<Integer> OutputD2 =null;
	private volatile List<NodeMessage> nodeMessages = null;
	private volatile List<ExpectedMessage> expectedNodeMessages = null;
	private boolean startSendStatus = false;
	private boolean estimateSendStatus = false;
	private boolean selectSendStatus = false;
	private boolean confirmSendStatus = false;
	private boolean readySendStatus = false;
	private boolean suspectSendStatus = false;
	private boolean[] waitflags = new boolean[8];
	private double selectValue = 0;
	private int k = 0;
	private boolean serviceRegTimeoutFlag = true;
	private boolean aliveReceiveFlag = false;
	private int aliveErrorCount = 0;
	private int aliveMessageCount = 0;

	public enum State {
	   START, ESTIMATE, SELECT, CONFIRM, READY, NREADY, LEADER, SUSPECT, IDLE 
	}

	public enum MessageType {
		KEYSPARTS, MESSAGE
	}
	
	State CurrentState = State.START;
	
	Logger logger;
	
	private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
	 
	 public static final int TOTAL_PARTS = 10;
	 public static final int REQUIRED_PARTS = 5;
	 public int MESSAGE_TIMEOUT = 300;
	 public static final int TOTAL_ALIVE_MESSAGES = 10;
	 public static final int ALIVE_ERROR_MAXCOUNT = 2;
	 public static final int LEADER_TERM_SECONDS = 120;
	 public int ALIVE_MESSAGE_TIMEOUT = (LEADER_TERM_SECONDS*1000)/TOTAL_ALIVE_MESSAGES;
	 public static final int SERVICE_REG_TIMEOUT = 7000;
	
	public LeaderElectionProtocol(ResourceServer server, ServerInfo currentserver)
	{
		currentServerInfo = currentserver;
		currentServerInfo.PortNo += 40;
		nodesList = new ArrayList<ServerInfo>();
		byzsetNodes = new ArrayList<Integer>();
		OutputD1 = new ArrayList<Integer>();
		OutputD2 = new ArrayList<Integer>();
		nodeMessages = new ArrayList<NodeMessage>();
		expectedNodeMessages = new ArrayList<ExpectedMessage>();
		resServer = server;
		try
		{
			getPropValues();
		}
		catch(Exception e)
		{
			System.out.println("Exception occured in LeaderElectionProtocol(int ServerNodeNo, int ServerPort) : Read Configuration");
		}
		if(ServerConfiguration == 1)
			logger = Logger.getLogger("LeaderElectionLoggger"+currentServerInfo.NodeId);
		else
			logger = Logger.getLogger("LeaderElectionLoggger");
			
		logger.info("Current Node Server Information - Node- "+currentServerInfo.NodeId+" IP: "+currentServerInfo.IpAddress+" Port: "+currentServerInfo.PortNo);
		try
		{
			LEServerSocket = new ServerSocket(currentServerInfo.PortNo); 
		    //if(logger.isDebugEnabled())
			logger.info("LEPserverSocket binded to port " + currentServerInfo.PortNo);
		}
		catch(IOException ioe)
		{  logger.error("Can not bind to port " + currentServerInfo.PortNo + ": " + ioe.getMessage()); }
		LeaderElectionThread receiverThread = new LeaderElectionThread(this, LEServerSocket);
		receiverThread.start();
		
	}
	
	public void ElectNewCoordinator()
	{
		TotalRounds++;
		CurrentCoordinator = nodesList.get(TotalRounds % TotalNodes).NodeId;
	}
	
	public void run()
	{
		try
		{
			currentServerInfo.generateKeys();
			for(ServerInfo s: nodesList)
			{
				if(s.NodeId == currentServerInfo.NodeId)
				{
					s.publicKey = currentServerInfo.publicKey;
				}
			}
			BroadcastKeys();
			while(!allKeyPartsReceived());
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		
		try
		{
			resetControlMessages();
			TimeUnit.MILLISECONDS.sleep(TotalNodes*100);
			//sleep(1000);
			logger.debug("Got Off from sleep.");
			while(ProtocolStop)
			{
				switch(CurrentState)
				{
					case START:		if(!startSendStatus)
									{
										ElectNewCoordinator();
										broadcastInfo(currentServerInfo.NodeId+" START "+TotalRounds, MessageType.MESSAGE);
										startSendStatus = true;
									}
									if(CurrentCoordinator == currentServerInfo.NodeId)
									{
										if(waitflags[6] == false)
										{
											waitflags[6] = true;
											TimeUnit.MILLISECONDS.sleep(MESSAGE_TIMEOUT);
											logger.debug("START Timer has Timed-out.");
										}
										if(allStartRecieved() <= 0)
										{
											CurrentState = State.ESTIMATE;
											resetControlMessages();
											logger.info("OutputD1 after START (Only by the Coordinator of Current Round) - "+OutputD1.toString());
										}
									}
									break;
								
					case ESTIMATE: 	
									if(CurrentCoordinator == currentServerInfo.NodeId)
									{
										if(!estimateSendStatus)
										{
											double value = (double) Math.round(( Math.random() * 10)*100)/100;
//											if(currentServerInfo.NodeId == 4 || currentServerInfo.NodeId == 3)
//											{
//												TimeUnit.MILLISECONDS.sleep(500);
//											}
											broadcastInfo(currentServerInfo.NodeId+" ESTIMATE "+value, MessageType.MESSAGE);
											estimateSendStatus = true;
										}
										if(waitflags[0] == false)
										{
											waitflags[0] = true;
											TimeUnit.MILLISECONDS.sleep(MESSAGE_TIMEOUT);
											logger.debug("ESTIMATE Timer has Timed-out.");
										}
										if(allEstimatesRecieved() <= 0)
										{
											CurrentState = State.SELECT;
											logger.info("OutputD1 after ESTIMATE - "+OutputD1.toString());
										}
									}
									else
									{
										if(!estimateSendStatus && isEstimateReceived)
										{
											double value = (double) Math.round(( Math.random() * 10)*100)/100;
//											if(currentServerInfo.NodeId == 4 || currentServerInfo.NodeId == 3)
//											{
//												TimeUnit.MILLISECONDS.sleep(500);
//											}
											broadcastInfo(currentServerInfo.NodeId+" ESTIMATE "+value, MessageType.MESSAGE);
											estimateSendStatus = true;
										}
										if(waitflags[0] == false)
										{
											waitflags[0] = true;
											TimeUnit.MILLISECONDS.sleep(MESSAGE_TIMEOUT);
											logger.debug("ESTIMATE Timer has Timed-out.");
										}
										
										if(allEstimatesRecieved() <= 0)
										{
											CurrentState = State.SELECT;
											logger.info("OutputD1 after ESTIMATE - "+OutputD1.toString());
										}
									}
									break;
									
					case SELECT:
									if(CurrentCoordinator == currentServerInfo.NodeId)
									{
										if(!selectSendStatus)
										{
											broadcastInfo(currentServerInfo.NodeId+" SELECT "+chooseSelectValue(), MessageType.MESSAGE);
											selectSendStatus = true;
											TimeUnit.MILLISECONDS.sleep(TotalNodes*20);
											broadcastInfo(currentServerInfo.NodeId+" CONFIRM "+selectValue+" "+TotalRounds, MessageType.MESSAGE);
										}
										if(waitflags[2] == false)
										{
											waitflags[2] = true;
											TimeUnit.MILLISECONDS.sleep(MESSAGE_TIMEOUT);
											logger.debug("CONFIRM Timer has Timed-out.");
										}
										
										if(allConfirmRecieved() <= 0)
										{	
											logger.info("OutputD1 after CONFIRM - "+OutputD1.toString());
											CurrentState = State.READY;
										}
										else
										{
											logger.info("OutputD1 after CONFIRM - "+OutputD1.toString());
											CurrentState = State.NREADY;
										}
									}
									else 
									{
										if(waitflags[1] == false)
										{
											waitflags[1] = true;
											TimeUnit.MILLISECONDS.sleep(MESSAGE_TIMEOUT);
											logger.debug("SELECT Timer has Timed-out.");
										}
										if(isSelectReceived && !confirmSendStatus)
										{
//											if(errorTrigger && currentServerInfo.NodeId == 3)
//												{	
//													errorTrigger = false;
//													broadcastInfo(currentServerInfo.NodeId+" CONFIRM 3 "+TotalRounds, MessageType.MESSAGE);
//												}
//												else
											broadcastInfo(currentServerInfo.NodeId+" CONFIRM "+selectValue+" "+TotalRounds, MessageType.MESSAGE);
								   			confirmSendStatus = true;
										}
										if(waitflags[3] == false)
										{
											waitflags[3] = true;
											TimeUnit.MILLISECONDS.sleep(MESSAGE_TIMEOUT);
											logger.debug("CONFIRM Timer has Timed-out.");
										}
										if(allConfirmRecieved() <= 0)
										{
											logger.info("OutputD1 after CONFIRM - "+OutputD1.toString());
											CurrentState = State.READY;
										}
										else
										{
											logger.info("OutputD1 after CONFIRM - "+OutputD1.toString());
											CurrentState = State.NREADY;
										}
									}
									break;
					case READY:		if(!readySendStatus)
									{
										broadcastInfo(currentServerInfo.NodeId+" READY "+selectValue+" "+TotalRounds, MessageType.MESSAGE);
										readySendStatus = true;	
									}
									if(waitflags[4] == false)
									{
										waitflags[4] = true;
										TimeUnit.MILLISECONDS.sleep(MESSAGE_TIMEOUT);
										logger.debug("READY Timer has Timed-out.");
									}
									if(allReadyRecieved(1))
									{
										logger.info("OutputD1 after READY - "+OutputD1.toString());
										CurrentState = State.SUSPECT;
									}
									else
									{
										logger.info("OutputD1 after READY - "+OutputD1.toString());
										CurrentState = State.IDLE;
									}
									break;
									
					case NREADY:	if(!readySendStatus)
									{
										broadcastInfo(currentServerInfo.NodeId+" NREADY "+selectValue+" "+TotalRounds, MessageType.MESSAGE);
										readySendStatus = true;
									}
									if(waitflags[5] == false)
									{
										waitflags[5] = true;
										TimeUnit.MILLISECONDS.sleep(MESSAGE_TIMEOUT);
										logger.debug("NREADY Timer has Timed-out.");
									}
									if(allReadyRecieved(-1))
									{
										logger.info("OutputD1 after NREADY - "+OutputD1.toString());
										CurrentState = State.SUSPECT;
									}
									else
									{
										logger.info("OutputD1 after NREADY - "+OutputD1.toString());
										CurrentState = State.IDLE;
									}
									break;
									
					case LEADER:	if(!isServerRegisterd)
									{
										logger.info("Instruct Server to Register");
										resServer.ServerRegister();
										logger.info("Service Registration Completed");
										isServerRegisterd = true;
										startTimerTask();		
										CurrentState = State.IDLE;
									}
									break;
									
					case SUSPECT:	if(!suspectSendStatus)
									{
										String info = "";
										for(Integer i: OutputD1)
										{
											info += Integer.toString(i)+ " ";
											for(NodeMessage m: nodeMessages)
											{
												if(m.NodeId == i)
												{
													m.SuspectCount++;
												}
											}
										}
										broadcastInfo(currentServerInfo.NodeId+" SUSPECT "+info, MessageType.MESSAGE);
										suspectSendStatus = true;
									}
									TimeUnit.MILLISECONDS.sleep(MESSAGE_TIMEOUT);
									logger.debug("SUSPECT Timer has Timed-out.");
									if(allSuspectReceived())
									{
										if(CurrentCoordinator == currentServerInfo.NodeId)
										{
											logger.info("OutputD1 after SUSPECT - "+OutputD1.toString());
											logger.info("OutputD2 after SUSPECT - "+OutputD2.toString());
											CurrentState = State.LEADER;
										}
										else
										{
											logger.info("OutputD1 after SUSPECT - "+OutputD1.toString());
											logger.info("OutputD2 after SUSPECT - "+OutputD2.toString());
											CurrentState = State.IDLE;
										}
									}
									else
									{
										logger.info("OutputD1 after SUSPECT - "+OutputD1.toString());
										logger.info("OutputD2 after SUSPECT - "+OutputD2.toString());
										CurrentState = State.IDLE;
									}
									break;
						
					case IDLE:		startSendStatus = false;
									TimeUnit.MILLISECONDS.sleep(MESSAGE_TIMEOUT);
									if(CurrentCoordinator == currentServerInfo.NodeId)
									{
										if(!isServerRegisterd)
										{
											logger.info("Requesting to Restart LEP Algorithm");
											CurrentState = State.START;
										}
										else
										{
											TimeUnit.MILLISECONDS.sleep(ALIVE_MESSAGE_TIMEOUT);
											broadcastInfo(currentServerInfo.NodeId+" ALIVE ", MessageType.MESSAGE);
										}
									}
									else
									{
										if(TotalRounds == 1 && serviceRegTimeoutFlag)
										{
											TimeUnit.MILLISECONDS.sleep(ALIVE_MESSAGE_TIMEOUT+SERVICE_REG_TIMEOUT);
											serviceRegTimeoutFlag = false;
											if(!aliveReceiveFlag)
											{	
												logger.info("LEADER is NOT ALIVE.");
												aliveErrorCount++;
											}
											else
											{
												logger.info("LEADER is ALIVE.");
												aliveReceiveFlag = false;
											}
												
										}
										else
										{
											TimeUnit.MILLISECONDS.sleep(ALIVE_MESSAGE_TIMEOUT);
											if(!aliveReceiveFlag)
											{
												logger.info("LEADER is NOT ALIVE.");
												aliveErrorCount++;
											}
											else
											{
												logger.info("LEADER is ALIVE.");
												aliveReceiveFlag = false;
											}
										}
										if(aliveErrorCount >= ALIVE_ERROR_MAXCOUNT)
										{
											logger.info("Requesting to Restart LEP Algorithm");
											CurrentState = State.START;
										}
									}
									
									break;
				}
			}
		}
		catch(Exception e)
		{
			logger.error("Exception occured in Run() :",e);
		}
	}
	
	public void stop()
	{
		ProtocolStop = false;
	}
	private void resetControlMessages()
	{
		estimateSendStatus = false;
		selectSendStatus = false;
		confirmSendStatus = false;
		readySendStatus = false;
		suspectSendStatus = false;
		isSelectReceived = false;
		isEstimateReceived = false;
		isSuspectReceived = false;
		expectedNodeMessages.clear();
		nodeMessages.clear();
		OutputD1.clear();
		for(ServerInfo s : nodesList)
		{
			//if(!s.equals(currentServerInfo))
			{
				NodeMessage n = new NodeMessage();
				n.NodeId = s.NodeId;
				n.EstimateValue = -1;
				n.ConfirmValue = -1;
				n.SelectValue = -1;
				n.ReadyValue = -1;
				n.SuspectCount = 0;
				n.SuspectReceived = false;
				nodeMessages.add(n);
			}
		}
		for(Integer i: byzsetNodes)
		{
			AddtoSuspectedList(i);
		}
		selectValue = 0;
		for(int i = 0; i < 8; i++)
		{
			waitflags[i] = false;
		}
		//TotalNodes = nodesList.size() - OutputD2.size();
		aliveReceiveFlag = false;
		aliveErrorCount = 0;
		aliveMessageCount = 0;
	}
	private void startTimerTask()
	{
		scheduler.schedule(new Runnable() {
		       public void run() 
		       { 
		    	   //resetControlMessages();
		    	   startSendStatus = false;
		    	   CurrentState = State.START;
		    	   logger.info("Instruct Server to De-register");
		    	   resServer.ServerDeregister();
		    	   isServerRegisterd = false;
		       }
		     }, LEADER_TERM_SECONDS , SECONDS);
	}
	private void broadcastInfo(String input, MessageType type)
	{
		if(type == MessageType.KEYSPARTS)
		{
			for(ServerInfo s: nodesList)
			{
				if(!s.equals(currentServerInfo))
				{
					send(input, s);
				}
			}
		}
		else if(type == MessageType.MESSAGE)
		{
			for(ServerInfo s: nodesList)
			{
				//if(!s.equals(currentServerInfo))
				{
					send(input +"/"+ SignMessage(input), s);
				}
			}
		}
		
	}
	
	private void send(String input,ServerInfo s)
	{  
		try
		{  
			Sendsocket = new Socket(s.IpAddress, s.PortNo);
			streamOut = new DataOutputStream(new BufferedOutputStream(Sendsocket.getOutputStream()));
		  	if(logger.isDebugEnabled())
		  	{
		  		if(input.contains("KEYS"))
		  		{
		  			String[] info = input.split(" ");
		  			logger.debug("[S] Key Part - "+ info[2] + " to Node- "+s.NodeId);
		  		}
		  		else
		  		{
		  			String[] info = input.split("/",2);
		  			logger.debug("[S] " + info[0] + " to Node- "+s.NodeId);
		  		}
		  			
		  	}
		  	streamOut.writeUTF(input); 
		  	streamOut.flush();
		  	Sendsocket.close();
		  	Sendsocket = null;
		 }
		 catch(IOException ioe)
		 {  logger.error("LEP TCP Sending error to "+s.NodeId+": Message: "+input+" : " + ioe.getMessage()); } 
	}
	
	public void getServerDetails(int nodeno, ServerInfo server)
	{
		server = nodesList.get(nodeno-1);
	}
	
	public synchronized void receiveMessageHandle(String message)
	{	
		if(message.contains("KEYS"))
	   	 {
			String[] info = message.split(" ");
	   		for(ServerInfo sinfo: nodesList)
			{
	   			if(sinfo.NodeId == Integer.parseInt(info[0]))
	   			{
	   				sinfo.parts.add( PartFormats.parse(info[3]));
	   				break;
	   			}
	   		}
	   		logger.info("[R] Key Part - "+ info[2] +" from Node- "+Integer.parseInt(info[0]));
	   	 }
		else 
		{
			String[] verify = message.split("/",2);
			String[] info = verify[0].split(" ");
			
			if(VerifyMessage(verify[0],verify[1],Integer.parseInt(info[0])))
			{
				if (verify[0].contains("START"))
			      { 
					if(CurrentState == State.IDLE)
					{
						if((aliveMessageCount == TOTAL_ALIVE_MESSAGES) || (aliveErrorCount == ALIVE_ERROR_MAXCOUNT))
							CurrentState = State.START;
					}
					for(NodeMessage n : nodeMessages)
					{
						if(n.NodeId == Integer.parseInt(info[0]))
						{
							n.StartValue = Integer.parseInt(info[2]);
							RemovefromSuspectedList(n.NodeId, "START");
							break;
						}
						
					}
					if(logger.isDebugEnabled())
				      {
						logger.info("[R] " +verify[0]+ " from Node- "+Integer.parseInt(info[0]));
						
				      }
				  }
				else if (verify[0].contains("ESTIMATE"))
			      { 
					if(CurrentState == State.START)
					{
						resetControlMessages();
						isEstimateReceived = true;
				    	CurrentState = State.ESTIMATE;
					}
					for(NodeMessage n : nodeMessages)
					{
						if(n.NodeId == Integer.parseInt(info[0]))
						{
							n.EstimateValue = Double.parseDouble(info[2]);
							RemovefromSuspectedList(n.NodeId, "ESTIMATE");
							break;
						}
						
					}
					if(logger.isDebugEnabled())
				      {
						logger.info("[R] " +verify[0]+ " from Node- "+Integer.parseInt(info[0]));
						
				      }
				  }
				else if(verify[0].contains("SELECT"))
			   	 {
			   		double Select = 0 ;
			   		for(NodeMessage n : nodeMessages)
			   		{
			   			if(n.NodeId == Integer.parseInt(info[0]))
			   			{
			   				n.SelectValue = Double.parseDouble(info[2]);
			   				Select = n.SelectValue;
			   				isSelectReceived = true;
			   				RemovefromSuspectedList(n.NodeId, "SELECT");
			   		   		break;
			   			}
			   		}
			   		selectValue = Select;
			   		logger.info("[R] " +verify[0]+ " from Node- "+Integer.parseInt(info[0]));
			   	 }
				else if(verify[0].contains("CONFIRM"))
			   	 {
			   		for(NodeMessage n : nodeMessages)
			   		{
			   			if(n.NodeId == Integer.parseInt(info[0]))
			   			{
			   				n.ConfirmValue = Double.parseDouble(info[2]);
			   				n.Round = Integer.parseInt(info[3]);
			   				RemovefromSuspectedList(n.NodeId, "CONFIRM");
			   		   		break;
			   			}
			   		}
			   		logger.info("[R] " +verify[0]+ " from Node- "+Integer.parseInt(info[0]));
			   	 }
				else if(verify[0].contains("READY"))
			   	 {
					if(verify[0].contains("NREADY"))
					{
						for(NodeMessage n : nodeMessages)
				   		{
				   			if(n.NodeId == Integer.parseInt(info[0]))
				   			{
				   				n.ReadyValue = -1 * Double.parseDouble(info[2]);
				   				n.Round = Integer.parseInt(info[3]);
				   				RemovefromSuspectedList(n.NodeId, "READY");
				   		   		break;
				   			}
				   		}
					}
					else
					{
						for(NodeMessage n : nodeMessages)
				   		{
				   			if(n.NodeId == Integer.parseInt(info[0]))
				   			{
				   				n.ReadyValue = Double.parseDouble(info[2]);
				   				n.Round = Integer.parseInt(info[3]);
				   				RemovefromSuspectedList(n.NodeId, "READY");
				   		   		break;
				   			}
				   		}
					}
			   		logger.info("[R] " +verify[0]+ " from Node- "+Integer.parseInt(info[0]));
			   	 }
				else if(verify[0].contains("SUSPECT"))
			   	 {
					for(int i = 2; i < info.length; i++)
					{
						for(NodeMessage n : nodeMessages)
				   		{
							if(n.NodeId == Integer.parseInt(info[i]))
							{
								n.SuspectCount++;
								break;
							}
				   		}
					}
					for(NodeMessage n : nodeMessages)
					{
						if(n.NodeId == Integer.parseInt(info[0]))
						{
							n.SuspectReceived = true;
							break;
						}
					}
					isSuspectReceived = true;
			   		logger.info("[R] " +verify[0]+ " from Node- "+Integer.parseInt(info[0]));
			   	 }
				else if (verify[0].contains("ALIVE"))
				{
					aliveReceiveFlag = true;
					aliveMessageCount++;
					logger.info("[R] " +verify[0]+ " from Node- "+Integer.parseInt(info[0]));
				}
			}
		}
	}
	
	private void getPropValues() throws IOException 
	{
		 
		try {
			Properties prop = new Properties();
			String propFileName = "config.properties";
 
			inputStream = getClass().getClassLoader().getResourceAsStream(propFileName);
			try
			{
				if (inputStream != null) {
					prop.load(inputStream);
				}
			}
			catch(FileNotFoundException e)
			{
				logger.error("property file '" + propFileName + "' not found in the classpath");
			}
 
			// get the property value and print it out
			ServerConfiguration = Integer.parseInt(prop.getProperty("ServerConfiguration"));
			TotalNodes = Integer.parseInt(prop.getProperty("totalNodes"));
			k = (TotalNodes - 1 )/3;
			MESSAGE_TIMEOUT = TotalNodes*80;
			ALIVE_MESSAGE_TIMEOUT -= MESSAGE_TIMEOUT;
			for(int i = 1; i<= TotalNodes; i++)
			{
				//if(i != currentServerInfo.NodeId)
				{
					String data = prop.getProperty("Server"+i);
					String[] info = data.split("/");
					ServerInfo serverinfo = new ServerInfo(info[0],(Integer.parseInt(info[1])+40),i);
					nodesList.add(serverinfo);
				}	
			}
			
		} catch (Exception e) {
			System.out.println("Exception: " + e);
		} finally {
			inputStream.close();
		}
	}
	private Double chooseSelectValue()
	{
		List<Double> estimateVal = new ArrayList<Double>();
		for(NodeMessage n: nodeMessages)
		{
			estimateVal.add(n.EstimateValue);
			
		}
		selectValue =  Collections.max(estimateVal);
		return selectValue;
	}
	
	private synchronized int allStartRecieved()
	{
		int count = 0;
		for(NodeMessage m: nodeMessages)
		{
			//if(!OutputD2.contains(m.NodeId)) 
			{
				if(m.StartValue == TotalRounds)
					count++;
				else
					AddtoSuspectedList(m.NodeId, "START");
			}
		}
		return (TotalNodes-k-count);
	}
	
	private synchronized int allEstimatesRecieved()
	{
		int count = 0;
		for(NodeMessage m: nodeMessages)
		{
			//if(!OutputD2.contains(m.NodeId)) 
			{
				if(m.EstimateValue != -1)
					count++;
				else
					AddtoSuspectedList(m.NodeId, "ESTIMATE");
			}
		}
		return (TotalNodes-k-count);
	}
	
	private synchronized int allConfirmRecieved()
	{
		int totalconfirmrequired = ((TotalNodes + k)/2) + 1;
		if(!OutputD2.contains(CurrentCoordinator)) 
		{
			for(NodeMessage n: nodeMessages)
			{
				if(n.ConfirmValue == selectValue && n.Round == TotalRounds)
				{
					totalconfirmrequired--;
				}
				else
				{
					if(n.ConfirmValue == -1)
					{
						AddtoSuspectedList(n.NodeId, "CONFIRM");
					}
					else
					{
						AddtoSuspectedList(n.NodeId);
					}			
				}
			}
		}
			return totalconfirmrequired;
	}
	private synchronized boolean allReadyRecieved(int i)
	{
		int totalconfirmrequired = ((TotalNodes + k)/2) + 1;
		for(NodeMessage n: nodeMessages)
		{
			if(n.ReadyValue == i*n.ConfirmValue && n.Round == TotalRounds )
			{
				totalconfirmrequired--;
			}
			else
			{
				if(n.ReadyValue == -1)
				{
					AddtoSuspectedList(n.NodeId, "READY");
				}
				else
				{
					AddtoSuspectedList(n.NodeId);
				}
			}
		}
		if(totalconfirmrequired <= 0)
			return true;
		return false;
	}
//	private synchronized boolean isServerReady()
//	{
//		int totalconfirmrequired = ((TotalNodes + k)/2) + 1;
//		boolean isfound = false;
//		for(ServerInfo s : nodesList)
//		{
//			if(!OutputD2.contains(s.NodeId))
//			{
//				isfound = false;
//				for(NodeMessage n: nodeMessages)
//				{
//					if(n.ConfirmValue == selectValue && (s.NodeId == n.NodeId))
//					{
//						totalconfirmrequired--;
//						isfound = true;
//						break;
//					}
//				}
//				if(isfound == false)
//				{
//					AddtoSuspectedList(s.NodeId);
//				}
//			}
//		}
//		
//		if(totalconfirmrequired <= 0)
//			return true;
//		return false;
//	}
	
	
	private synchronized boolean allSuspectReceived()
	{
		int count = 0;
		for(ServerInfo s : nodesList)
		{
			if(!OutputD2.contains(s.NodeId))
			{
				for(NodeMessage m: nodeMessages)
				{
					if((s.NodeId == m.NodeId) && m.SuspectReceived == true) 
					{
						count++;
						if(m.SuspectCount > k + 1)
						{
							 AddtoFaultList(m.NodeId);
						}
						break;
					}
					
				}
			}
		}
		return ((TotalNodes-k) <= count);
	}
	
	private synchronized boolean allKeyPartsReceived()
	{
		int count = 0 ;
		for(ServerInfo s: nodesList)
		{
			if(s.parts.size() == REQUIRED_PARTS )
			{
				if(s.publicKey== null)
					s.reassemblePublicKeys();
				count++;
			}
		}
		if(count == (TotalNodes -1))
		{
			return true;
		}
		return false;
	}
	private void BroadcastKeys()
	{
		byte[] publickey = currentServerInfo.publicKey.getEncoded();
		Random partrandom = new Random();
		int partno = 0;
		Part[] parts = Secrets.split(publickey,TOTAL_PARTS,REQUIRED_PARTS,partrandom); 
		
		for(int i = 0; i < REQUIRED_PARTS; i++)
		{
			partno = 2 * i;
			String formatted = PartFormats.currentStringFormat().format(parts[partno]);
			broadcastInfo(currentServerInfo.NodeId +" KEYS "+ i + " "+ formatted, MessageType.KEYSPARTS);
		}		
	}
	private String SignMessage(String input)
	{
		Base64.Encoder enc = Base64.getEncoder();
		byte[] realSig = null;
		try
		{
			byte[] msgbytes = input.getBytes();
			Signature dsa = Signature.getInstance("SHA1withDSA", "SUN"); 
			dsa.initSign(currentServerInfo.privateKey);
		 	dsa.update(msgbytes, 0, msgbytes.length);
		 	realSig = dsa.sign();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		return enc.encodeToString(realSig);	
	}
	
	private synchronized boolean VerifyMessage(String input,String signature, int nodeId)
	{
		boolean decision = false;
		Base64.Decoder dec = Base64.getDecoder();
		byte[] msgbytes = input.getBytes();
		byte[] decodeBytes = dec.decode(signature);
		try
		{
			Signature sig = Signature.getInstance("SHA1withDSA", "SUN");
			for(ServerInfo s: nodesList)
			{
				if(nodeId == s.NodeId)
				{
					sig.initVerify(s.publicKey);
					sig.update(msgbytes, 0, msgbytes.length);
					decision = sig.verify(decodeBytes);
					break;
				}
			}
			if(decision == false)
			{
				AddtoByzantineList(nodeId);
			}
		}
		catch(Exception e)
		{
			logger.error("Error occured with message: "+ input +"Signature: "+signature);
			e.printStackTrace();
		}
		return decision;
	}
	private void AddtoSuspectedList(int NodeId, String message)
	{
		if(!expectedNodeMessages.contains(new ExpectedMessage(NodeId,message)))
		{
			expectedNodeMessages.add(new ExpectedMessage(NodeId,message));
			
			if(!OutputD1.contains(new Integer(NodeId)))
			{
				OutputD1.add(NodeId);
			}
			logger.info("OutputD1 after Adding to Suspect List - "+OutputD1.toString());
		}
	}
	private void AddtoSuspectedList(int NodeId)
	{
		if(!OutputD1.contains(new Integer(NodeId)))
		{
			OutputD1.add(NodeId);
		}
	}
	private void AddtoByzantineList(int NodeId)
	{
		if(!byzsetNodes.contains(new Integer(NodeId)))
		{
			byzsetNodes.add(NodeId);
			
			if(!OutputD1.contains(new Integer(NodeId)))
			{
				OutputD1.add(NodeId);
			}
		}
	}
	private void RemovefromSuspectedList(int NodeId, String message)
	{
		if(expectedNodeMessages.contains(new ExpectedMessage(NodeId,message)))
		{
			expectedNodeMessages.remove(new ExpectedMessage(NodeId,message));
			
			if(OutputD1.contains(new Integer(NodeId)))
			{
				OutputD1.remove(new Integer(NodeId));
			}
		}
		//logger.info("OutputD1 after removing to Suspect List - "+OutputD1.toString());
	}
	private void RemovefromSuspectedList(int NodeId)
	{
		if(OutputD1.contains(new Integer(NodeId)))
		{
			OutputD1.remove(new Integer(NodeId));
		}	
	}
	private void AddtoFaultList(int NodeId)
	{
		if(!OutputD2.contains(new Integer(NodeId)))
		{
			OutputD2.add(new Integer(NodeId));
		}	
	}
//	private boolean CheckinSuspectedList(int NodeId, String message)
//	{
//		
//		return false;
//	}
//	private boolean CheckinSuspectedList(int NodeId)
//	{
//		return false;
//	}
}