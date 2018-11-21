package Replicas.Replica2.DataStructures;

@SuppressWarnings("serial")
public class EmployeeRecord extends Record
{
	public String mProjectID;
	
	public EmployeeRecord(String pFirstName, String pLastName, int pEmployeeID, String pMailID, String pProjectID)
	{
		super(pFirstName, pLastName, pEmployeeID, pMailID);
		
		mRecordID = "ER" + ServerManager.getNextID();
		mProjectID = pProjectID;
	}
	
	@Override
	public String print()
	{
		return mRecordID + ": " + mFirstName + " " + mLastName + " " + mEmployeeID + " " + mMailID + " " + mProjectID;
	}
}
