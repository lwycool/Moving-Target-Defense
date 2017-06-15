package edu.csu.cs.mdt.ResourceServer;

public class NodeMessage {
	
		//.. Id of the machine from which the message is received
		public int NodeId;
		
		//.. Coordinator Id of the current round
		public int StartValue;
		
		//.. Current round number.
		public int Round;
		
		//.. Stores the Estimate values sent by all the other nodes
		public double EstimateValue;
		
		//.. Stores the Select Value sent by Coordinator
		public double SelectValue;
		
		//.. Stores the Confirmation value from all the other nodes
		public double ConfirmValue;
		
		public double ReadyValue;
		
		//.. Indicates whether this node treats the coordinator as suspect 
		public int SuspectCount;
		
		public boolean SuspectReceived;

}
