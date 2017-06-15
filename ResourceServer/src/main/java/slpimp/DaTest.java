package slpimp;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

public class DaTest {

	public static void main(String[] args) throws Exception {


		MulticastSocket sock = new MulticastSocket(427);

		sock.joinGroup(InetAddress.getByName("255.255.255.255"));

		byte[] buf = new byte[1400];
		DatagramPacket p = new DatagramPacket(buf, buf.length);
		sock.receive(p);
		System.out.println("Got something!");
	}
}
