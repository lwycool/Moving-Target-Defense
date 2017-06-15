package edu.csu.cs.mdt.ResourceServer;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.util.Properties;

public class TestProtocol {
	
	static ServerInfo currentServerInfo = new ServerInfo("",0,0);

	private static void getCurrentServerInfo(int ServerConfiguration)
	{
		
		if(ServerConfiguration == 1)
		{
			currentServerInfo.IpAddress = "127.0.0.1";
		}
		if(ServerConfiguration == 2)
		{
			try
			{
				InetAddress ipAddr = InetAddress.getLocalHost();
				currentServerInfo.IpAddress = ipAddr.getHostAddress();
				
			}
			catch(Exception e)
			{
				System.out.println("Exception occured in getCurrentServerInfo(int portno) :");
				e.printStackTrace();
			}
		}
		
	}
	
	private void getPropValues() throws IOException 
	{
		 
		String propFileName = "config.properties";
		
	    InputStream inputStream = getClass().getClassLoader().getResourceAsStream(propFileName);
	    
	    
		try {
			Properties prop = new Properties();
			
			prop.load(inputStream);
			
			try
			{
				if (inputStream != null) {
					prop.load(inputStream);
				}
			}
			catch(FileNotFoundException e)
			{
				System.out.println("property file '" + propFileName + "' not found in the classpath");
			}
 
			// get the property value and print it out
			int ServerConfiguration = Integer.parseInt(prop.getProperty("ServerConfiguration"));
			int TotalNodes = Integer.parseInt(prop.getProperty("totalNodes"));
			
			getCurrentServerInfo(ServerConfiguration);
			
			if(ServerConfiguration == 1)
			{
				ResourceServer[] server = new ResourceServer[TotalNodes];
				Thread[] thread = new Thread[TotalNodes];
				
				for(int i = 1; i<= TotalNodes; i++)
				{
					//if(i != currentServerInfo.NodeId)
					{
						String data = prop.getProperty("Server"+i);
						String[] info = data.split("/");
						ServerInfo serverinfo = new ServerInfo(info[0],(Integer.parseInt(info[1])),i);
						server[i-1] = new ResourceServer(serverinfo);
						thread[i-1] = new Thread(server[i-1], "ResourceServer"+i); thread[i-1].start();
					}	
				}
			}
			else if(ServerConfiguration == 2)
			{
				currentServerInfo.NodeId = Integer.parseInt(prop.getProperty("ServerNode"));
				currentServerInfo.PortNo = Integer.parseInt(prop.getProperty("ServerPort"));
				ResourceServer server = new ResourceServer(currentServerInfo);
				Thread thread = new Thread(server, "ResourceServer"); thread.start();
			}
			
		} catch (Exception e) {
			System.out.println("Exception: " + e);
		} finally {
			inputStream.close();
		}
	}
	
	public static void main(String args[]) throws IOException 
	{
		TestProtocol a = new TestProtocol();
		a.getPropValues();
	}

}
