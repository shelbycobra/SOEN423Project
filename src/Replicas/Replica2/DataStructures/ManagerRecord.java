package Replicas.Replica2.DataStructures;

import java.util.ArrayList;
import java.util.Arrays;

@SuppressWarnings("serial")
public class ManagerRecord extends Record
{
	ArrayList<Project> mProjects;
	public String mLocation;
	
	public ManagerRecord(String pFirstName, String pLastName, int pEmployeeID, String pMailID, Project[] pProjects, String pLocation)
	{
		super(pFirstName, pLastName, pEmployeeID, pMailID);
		
		mRecordID = "MR" + ServerManager.getNextID();
		mProjects = new ArrayList<Project>(Arrays.asList(pProjects));
		mLocation = pLocation;
	}
	
	@Override
	public String print()
	{
		return mRecordID + ": " + mFirstName + " " + mLastName + " " + mEmployeeID + " " + mMailID + " " + mLocation.toString();
	}
}
