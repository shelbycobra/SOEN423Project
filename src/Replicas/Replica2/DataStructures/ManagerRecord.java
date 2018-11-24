package Replicas.Replica2.DataStructures;

import java.util.ArrayList;
import java.util.Arrays;

@SuppressWarnings("serial")
public class ManagerRecord extends Record
{
	ArrayList<Project> mProjects;
	public String mLocation;
	
	public ManagerRecord(String pFirstName, String pLastName, int pEmployeeID, String pMailID, Project[] pProjects, String pLocation, String pManagerID)
	{
		super(pFirstName, pLastName, pEmployeeID, pMailID, pManagerID);
		mProjects = new ArrayList<Project>(Arrays.asList(pProjects));
		mLocation = pLocation;
	}
	
	public ArrayList<Project> getProjects()
	{
		return mProjects;
	}

	public void setProjects(ArrayList<Project> mProjects)
	{
		this.mProjects = mProjects;
	}

	public String getLocation()
	{
		return mLocation;
	}

	public void setLocation(String mLocation)
	{
		this.mLocation = mLocation;
	}

	@Override
	public String print()
	{
		return getRecordID() + ": " + getFirstName() + " " + getLastName() + " " + getEmployeeID() + " " + getMailID() + " " + mLocation.toString() + " " + getManagerID();
	}
}
