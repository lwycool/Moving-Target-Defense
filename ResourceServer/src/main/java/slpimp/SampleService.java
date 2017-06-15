package slpimp;

import java.io.DataOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class SampleService {

	public static void main(String[] args) throws Exception{
		
		int port = 12121;
		
		ServerSocket ss = new ServerSocket(port);

		while(true) {
		    Socket s = ss.accept();
		    System.err.println("Accepted a connection");
		    DataOutputStream o = new DataOutputStream(s.getOutputStream());
		    o.writeUTF("Success");
		    o.close();
		}

	}

}
