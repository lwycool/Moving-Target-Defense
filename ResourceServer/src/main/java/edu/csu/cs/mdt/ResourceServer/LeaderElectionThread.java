package edu.csu.cs.mdt.ResourceServer;

import java.io.DataInputStream;
import java.io.*;
import java.net.ServerSocket;
import java.net.*;



public class LeaderElectionThread extends Thread {
	
	   private ServerSocket       	LEPsocket  	= null;
	   private Socket 				recieveSocket = null;
	   private LeaderElectionProtocol   LEP			= null;
	   private DataInputStream  		streamIn 	= null;

	   public LeaderElectionThread(LeaderElectionProtocol _client, ServerSocket _socket)
	   {  
		  super();
		  LEP      = _client;
		  LEPsocket   = _socket;
	   }
	   public void run()
	   {  while (true)
	      {  try
	         {  
	    	  	recieveSocket = LEPsocket.accept();
	    	  	
	    	  	streamIn = new DataInputStream(new BufferedInputStream(recieveSocket.getInputStream()));
	         	//synchronized (LEP)
	         	{
	         		LEP.receiveMessageHandle(streamIn.readUTF());
	         	}
	         	recieveSocket.close();
	         	
	         }
	         catch(IOException ioe)
	         {  System.out.println("Listening error: " + ioe.getMessage());
	            LEP.stop();
	         }
	      }
	   }
	   
	   

}
