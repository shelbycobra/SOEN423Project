package Replicas.Replica2.DataStructures;

import java.io.Serializable;

@SuppressWarnings("serial")
public abstract class Record implements Serializable
{
	private String mRecordID;
	private String mFirstName;
	private String mLastName;
	private int mEmployeeID;
	private String mMailID;
	private String mManagerID;
	
	public Record(String pFirstName, String pLastName, int pEmployeeID, String pMailID, String pManagerID)
	{
		mFirstName = pFirstName;
		mLastName = pLastName;
		mEmployeeID = pEmployeeID;
		mMailID = pMailID;
		mManagerID = pManagerID;
	}
	
	public String getRecordID()
	{
		return mRecordID;
	}

	public void setRecordID(String mRecordID)
	{
		this.mRecordID = mRecordID;
	}

	public String getFirstName()
	{
		return mFirstName;
	}

	public void setFirstName(String mFirstName)
	{
		this.mFirstName = mFirstName;
	}

	public String getLastName()
	{
		return mLastName;
	}

	public void setLastName(String mLastName)
	{
		this.mLastName = mLastName;
	}

	public int getEmployeeID()
	{
		return mEmployeeID;
	}

	public void setEmployeeID(int mEmployeeID)
	{
		this.mEmployeeID = mEmployeeID;
	}

	public String getMailID()
	{
		return mMailID;
	}

	public void setMailID(String mMailID)
	{
		this.mMailID = mMailID;
	}

	public String getManagerID()
	{
		return mManagerID;
	}

	public void setManagerID(String mManagerID)
	{
		this.mManagerID = mManagerID;
	}

	public boolean isManagerRecord()
	{
		return mRecordID.substring(0, 2).matches("MR");
	}
	
	public abstract String print();
}
