package edu.csu.cs.mdt.ResourceServer;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.List;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;

import org.mitre.secretsharing.*;
public class ServerInfo
{
	String IpAddress;
	int PortNo;
	int NodeId;
	//Part[] parts = new Part[LeaderElectionProtocol.REQUIRED_PARTS];
	List<Part> parts = null;
	
	KeyPairGenerator keygen = null;
	SecureRandom random = null;
	PrivateKey privateKey = null;
	PublicKey publicKey = null;
	
	public ServerInfo(String ip, int port, int nodeid)
	{
		IpAddress = ip;
		PortNo = port;
		NodeId = nodeid;
		parts = new ArrayList<Part>();
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((IpAddress == null) ? 0 : IpAddress.hashCode());
		result = prime * result + NodeId;
		result = prime * result + PortNo;
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ServerInfo other = (ServerInfo) obj;
		if (IpAddress == null) {
			if (other.IpAddress != null)
				return false;
		} else if (!IpAddress.equals(other.IpAddress))
			return false;
		if (NodeId != other.NodeId)
			return false;
		if (PortNo != other.PortNo)
			return false;
		return true;
	}
	
	public void generateKeys() throws NoSuchProviderException, NoSuchAlgorithmException
	{
			keygen = KeyPairGenerator.getInstance("DSA", "SUN");
	 		random = SecureRandom.getInstance("SHA1PRNG","SUN");
	 		keygen.initialize(512, random);
	 		KeyPair pair = keygen.generateKeyPair();
	 		privateKey = pair.getPrivate();
	 		publicKey = pair.getPublic();
	}
	
	public void reassemblePublicKeys()
	{
		Part[] secrets = parts.toArray(new Part[parts.size()]);
		byte[] publicKeyEnc = Secrets.join(secrets);
		try
		{
			publicKey = KeyFactory.getInstance("DSA","SUN").generatePublic(new X509EncodedKeySpec(publicKeyEnc));
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
};