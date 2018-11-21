package Replicas.Replica2.DataStructures;

public final class Project
{
	public String projectID = null;
	public String clientName = null;
	public String projectName = null;
	
	public Project ()
	{
	}
	
	public Project (String _projectID, String _clientName, String _projectName)
	{
		projectID = _projectID;
		clientName = _clientName;
		projectName = _projectName;
	}
}