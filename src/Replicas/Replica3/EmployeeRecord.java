package Replicas.Replica3;

public class EmployeeRecord extends Record {
	private static final long serialVersionUID = 4220125172871733314L;

	public int projectID;

	public EmployeeRecord(String firstName, String lastName, int employeeID, String mailID, int projectID) {
		super("ER" + employeeID, firstName, lastName, employeeID, mailID);
		this.projectID = projectID;
	}

	@Override
	public String toString() {
		return "EmployeeRecord [recordID=" + recordID + ", firstName=" + firstName + ", lastName=" + lastName
				+ ", employeeID=" + employeeID + ", mailID=" + mailID + ", projectID=" + projectID + "]";
	}
}
