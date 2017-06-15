package edu.csu.cs.mdt.ResourceServer;

import java.net.*;
import java.io.*;
import slpimp.ServiceAgent;

public class ResourceServer implements Runnable
{  private Socket          socket   = null;
   private ServerSocket    server   = null;
   private DataInputStream streamIn =  null;
   private DataOutputStream streamOut =  null;
   private int PortNo;
   //private int NodeNo;
   ServiceAgent slpAgent = new ServiceAgent();
   LeaderElectionService LEP = null;
   PDPService PDP = null;
   
   
   public ResourceServer(ServerInfo currentserver)
   {     
	   try
      {  System.out.println("Binding to port " + currentserver.PortNo + ", please wait  ...");
         server = new ServerSocket(currentserver.PortNo);  
         System.out.println("Server started: " + server+ " "+server.getInetAddress().toString());
         PortNo = currentserver.PortNo;
         LEP = new LeaderElectionService(this,currentserver); 
         Thread LEPthread = new Thread(LEP, "LEP"+currentserver.NodeId); LEPthread.start();
      }
      catch(IOException ioe)
      {  System.out.println("Can not bind to port " + currentserver.NodeId + ": " + ioe.getMessage()); }
   }
   public void run()
   {   try
         {  System.out.println("Waiting for a client ..."); 
            
         	while(true)
         	{
         		socket = server.accept(); 
         		open();
         		String line = streamIn.readUTF();
         		if(PDP != null)
         		{
         			String info[] = line.split(",");
         			boolean decision = PDP.requestAccess(info[0], info[1], info[2]);
         			
         			streamOut.writeUTF(decision == true? "Approved":"Denied");
         			streamOut.flush();
         		}
         		else
         		{
         			System.out.println("Wrong Resource server comunicated."); 
         		}
         		//close();
         	}
         	
            
         }
         catch(IOException ioe)
         {  System.out.println("Server accept error: " + ioe); close();}
   }
   
   public void open() throws IOException
   {  
	   streamIn = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
	   streamOut = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
	   streamOut.flush();
   }
   
   public void close() 
   { 
	   try
	   {
		      if (streamIn != null)  streamIn.close();
		      if (streamOut != null) streamOut.close();
	   }
	  catch(IOException e)
	   {
		  e.printStackTrace();
	   }
   }
   
   public void ServerRegister()
   {
	   String URL= "service:ResourceServer://127.0.0.1:"+PortNo;
	   try
	   {
		   slpAgent.ServiceAgentRegister(URL, PortNo);
		   PDP = new PDPService();
	       PDP.init();
	   }
	   catch(Exception e)
	   {
		   e.printStackTrace();
	   }
   } 
   public void ServerDeregister()
   {
	   String URL= "service:ResourceServer://127.0.0.1:"+PortNo;
	   try
	   {
		   slpAgent.ServiceAgentDeregister(URL, PortNo);
		   PDP = null;
	   }
	   catch(Exception e)
	   {
		   e.printStackTrace();
	   }
   }
   
   
   
}
