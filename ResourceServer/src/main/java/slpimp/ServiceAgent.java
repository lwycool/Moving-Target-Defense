package slpimp;

import solers.slp.Advertiser;
import solers.slp.ServiceLocationAttribute;
import solers.slp.ServiceLocationException;
import solers.slp.ServiceLocationManager;
import solers.slp.ServiceURL;

import java.io.*;
import java.util.*;

public class ServiceAgent {

	//.. Need to be supplied with mandatory first argument
	//.. args[0] - on which port the ServiceAgent to register the serviceURL
	//.. args[1] - optional parameter to pass any attributes
	public void ServiceAgentRegister(String service, int port) throws Exception {


//		if(args.length < 1) {
//			System.out.println("usage: ServiceAgent <ServiceURL>");
//			System.out.print("SerivceURL should be of format - service:ServiceName://IPAddress:PortNo");
//			System.exit(1);
//		}

		//int port = 12121;

		String attribute = null;

		String serviceURL = service;
			

		//.. the below things are for logging purpose we can add them with the full implementation
		org.apache.log4j.BasicConfigurator.configure();
		org.apache.log4j.Category.getRoot().setPriority(org.apache.log4j.Priority.WARN);
		Properties p = new Properties();
		p.put("net.slp.traceDATraffic", "true");
		p.put("net.slp.traceMsg", "true");
		p.put("net.slp.traceDrop", "true");
		p.put("net.slp.traceReg", "true");

		/*TODO work on the security*/
		// The following are necessary for secure use
		// p.put("net.slp.securityEnabled", "true");
		// p.put("net.slp.privateKey.myspi", "path to pk8 private key");
		// p.put("net.slp.spi", "myspi");

		File f = new File("slp.config");

		//.. Initialise the ServiceLocationManger with Configuration File
		ServiceLocationManager.init(p);

		try {
			//.. Create the Advertiser - which servese as Service Agent for the user
			Advertiser serviceAgent = ServiceLocationManager.getAdvertiser(java.util.Locale.getDefault());


			Vector attributes = new Vector();

			//.. if there is an attribute supplied with the arguments
			if(attribute != null) {

				Vector attrValues = new Vector();

				attrValues.add(attribute);

				attributes.add(new ServiceLocationAttribute("Attr1", attrValues));
			}

			//.. get the IP address of the leader which was elected using Leader Election Protocol
			//String Ipaddress = getTheLeaderDetails();

			ServiceURL serviceUrl = new ServiceURL(serviceURL,port);

			//.. register the leader service so that when User agent requests 
			serviceAgent.register(serviceUrl, attributes);


			System.out.println("Sucessfully registered the leader service");

		} catch (ServiceLocationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public void ServiceAgentDeregister(String service, int port) throws Exception 
	{
		org.apache.log4j.BasicConfigurator.configure();
		org.apache.log4j.Category.getRoot().setPriority(org.apache.log4j.Priority.WARN);
		Properties p = new Properties();
		p.put("net.slp.traceDATraffic", "true");
		p.put("net.slp.traceMsg", "true");
		p.put("net.slp.traceDrop", "true");
		p.put("net.slp.traceReg", "true");
		
		File f = new File("slp.config");
		try
		{
			Advertiser serviceAgent = ServiceLocationManager.getAdvertiser(java.util.Locale.getDefault());
			
			ServiceURL serviceUrl = new ServiceURL(service,port);
			
			serviceAgent.deregister(serviceUrl);
		}
		catch (ServiceLocationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
	}
//	private static String getTheLeaderDetails() throws Exception{
//
//		String leaderIp = "";
//
//		//.. Get the contents from the CURRENT_LEADER file which contains the IP address of the 
//		//.. elected leader from Leader Election Protocol.
//
//		InetAddress ia = InetAddress.getLocalHost();
//		leaderIp = ia.getHostAddress();
//
//		return leaderIp;
//
//
//	}

}