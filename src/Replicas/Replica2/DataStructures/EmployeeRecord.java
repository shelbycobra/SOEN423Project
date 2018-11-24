package Replicas.Replica2.DataStructures;

@SuppressWarnings("serial")
public class EmployeeRecord extends Record
{
	public String mProjectID;
	
	public EmployeeRecord(String pFirstName, String pLastName, int pEmployeeID, String pMailID, String pProjectID, String pManagerID)
	{
		super(pFirstName, pLastName, pEmployeeID, pMailID, pManagerID);

		mProjectID = pProjectID;
	}
	
	public String getProjectID()
	{
		return mProjectID;
	}

	public void setProjectID(String mProjectID)
	{
		this.mProjectID = mProjectID;
	}

	@Override
	public String print()
	{
		return getRecordID() + ": " + getFirstName() + " " + getLastName() + " " + getEmployeeID() + " " + getMailID() + " " + getProjectID() + " " + getManagerID();
	}
}
