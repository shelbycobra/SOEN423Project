package Replicas.Replica3;

import java.io.Serializable;

public abstract class Record implements Serializable {
	private static final long serialVersionUID = -4371452764387799673L;

	public String recordID;

	public String firstName;
	public String lastName;
	public int employeeID;
	public String mailID;

	Record(String recordID, String firstName, String lastName, int employeeID, String mailID) {
		this.recordID = recordID;

		this.firstName = firstName;
		this.lastName = lastName;
		this.employeeID = employeeID;
		this.mailID = mailID;
	}
}
