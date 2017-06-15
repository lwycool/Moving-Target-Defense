package slpimp;

import java.io.IOException;
import java.net.Socket;
import java.util.Properties;
import java.util.Vector;
import solers.slp.Locator;
import solers.slp.ServiceLocationEnumeration;
import solers.slp.ServiceLocationException;
import solers.slp.ServiceLocationManager;
import solers.slp.ServiceType;
import solers.slp.ServiceURL;

public class UserAgent {

	//.. Optional parameter attributes required for the service
	public static void main(String[] args) throws Exception{


		String attr = null;

		if(args.length > 0) {
			attr = args[0];
		}

		//.. the below things are for logging purpose we can add them with the full implementation
		org.apache.log4j.BasicConfigurator.configure();
		org.apache.log4j.Category.getRoot().setPriority(org.apache.log4j.Priority.WARN);
		Properties prop = new Properties();
		prop.put("net.slp.traceDATraffic", "true");
		prop.put("net.slp.traceMsg", "true");
		prop.put("net.slp.traceDrop", "true");
		prop.put("net.slp.traceReg", "true");
		//prop.put("net.slp.DAAddresses", "10.0.0.137");
		//prop.put("net.slp.isBroadcastOnly", "false");

		ServiceLocationManager.init(prop);

		Locator loc = null;
		try {

			loc = ServiceLocationManager.getLocator(java.util.Locale.getDefault());


		} catch (ServiceLocationException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}


		Vector scopes = new Vector();


		//.. add the default scopes
		scopes.add("default");

		try {


			ServiceLocationManager.findScopes();

		} catch (ServiceLocationException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}


		/*// we can also get the scopes using below method available, for now we have added default scope

		 	 scopes = ServiceLocationManager.findScopes();

		 */

		String selectionAttr = "";
		if(attr != null) {
			selectionAttr += "(Attribute1=" + attr + ")";
		}

		//.. get the service enumeration
		//.. service:ServiceName - should be same as what we have used in Service agent
		ServiceLocationEnumeration enumeration = loc.findServices(new ServiceType("service:ServiceName"), scopes, selectionAttr);

		ServiceURL serviceUrl = null;

		boolean connected = false;

		//.. we keep on trying to get the service URL
		while(!connected && enumeration.hasMoreElements()) {

			//.. next()  - Returns the next value or block until it becomes available. 
			serviceUrl = (ServiceURL)enumeration.next();

			if(serviceUrl != null) {

				System.out.println("Found URL: " + serviceUrl.toString());

				try {

					Socket s = new Socket(serviceUrl.getHost(), serviceUrl.getPort());

					//.. Now we got connected to the respective service and get the data from the server socket
					//.. TODO we can connect the leader server to process the requests

					connected = true;

				}
				catch(IOException e) {

					e.printStackTrace();
				}
			}
		}

	}

}
