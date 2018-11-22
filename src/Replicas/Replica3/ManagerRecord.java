package Replicas.Replica3;

import java.util.List;

public class ManagerRecord extends Record {
	private static final long serialVersionUID = -7309796150452080853L;

	public List<Project> projects;
	public String location;

	public ManagerRecord(String firstName, String lastName, int employeeID, String mailID, List<Project> projects, String location) {
		super("MR" + employeeID, firstName, lastName, employeeID, mailID);
		this.projects = projects;
		this.location = location;
	}

	@Override
	public String toString() {
		return "ManagerRecord [recordID=" + recordID + ", firstName=" + firstName + ", lastName=" + lastName
				+ ", employeeID=" + employeeID + ", mailID=" + mailID + ", location=" + location + "]";
	}
}
