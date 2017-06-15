package edu.csu.cs.mdt.ResourceServer;

import java.net.ServerSocket;

import org.apache.log4j.Logger;

public class LeaderElectionService implements Runnable {
	 //Timer timer=new Timer();

	private LeaderElectionProtocol Protocol;

	public LeaderElectionService(ResourceServer server, ServerInfo currentserver) 
	{
		Protocol= new LeaderElectionProtocol(server,currentserver);
	}
	public void run()
	{
		Protocol.run();
	}
	
}
