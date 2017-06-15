package edu.csu.cs.mdt.ResourceServer;

public class ExpectedMessage {
	
	public int NodeId;
	public String message;
	
	public ExpectedMessage(int nodeId, String message) {
		super();
		NodeId = nodeId;
		this.message = message;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + NodeId;
		result = prime * result + ((message == null) ? 0 : message.hashCode());
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
		ExpectedMessage other = (ExpectedMessage) obj;
		if (NodeId != other.NodeId)
			return false;
		if (message == null) {
			if (other.message != null)
				return false;
		} else if (!message.equals(other.message))
			return false;
		return true;
	}
	
	

}
