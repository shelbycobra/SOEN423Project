package Replicas.Replica2;

import Replicas.Replica2.DataStructures.Project;

public interface CenterServerInterface
{
	String createMRecord (String managerID, String firstName, String lastName, int employeeID, String mailID, Project[] projects, String location);
	String createERecord (String managerID, String firstName, String lastName, int employeeID, String mailID, String projectID);
	String getRecordCount (String managerID);
	String editRecord (String managerID, String recordID, String fieldName, String newValue);
	String transferRecord (String managerID, String recordID, String remoteCenterServerName);
	String printRecords ();
}
