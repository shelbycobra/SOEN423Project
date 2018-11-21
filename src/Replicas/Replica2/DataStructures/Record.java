package Replicas.Replica2.DataStructures;

import java.io.Serializable;

@SuppressWarnings("serial")
public abstract class Record implements Serializable
{
	public String mRecordID;
	String mFirstName;
	public String mLastName;
	int mEmployeeID;
	public String mMailID;
	
	public Record(String pFirstName, String pLastName, int pEmployeeID, String pMailID)
	{
		mFirstName = pFirstName;
		mLastName = pLastName;
		mEmployeeID = pEmployeeID;
		mMailID = pMailID;
	}
	
	public boolean isManagerRecord()
	{
		return mRecordID.substring(0, 2).matches("MR");
	}
	
	public abstract String print();
}
