package Replicas.Replica1.DataStructures;

import org.json.simple.JSONObject;

public abstract class Record {

	private String firstName;
	private String lastName;
	private int employeeID = -1;
	private String mailID  = "example@email.com";
	
	public Record(String firstName, String lastName, int employeeID, String mailID){
		if (firstName != null && firstName != "")
			this.firstName = firstName;
		if (lastName != null && lastName != "")
			this.lastName = lastName;
		if (employeeID > 0)
			this.employeeID = employeeID;
		if (employeeID >= 0 && employeeID <= 99999)
			this.employeeID = employeeID;
		else this.employeeID = -1;
		if (mailID.matches("[a-zA-Z0-9_]+@[a-zA-Z0-9.]+[com|ca|edu|net|gov|org|uk]"))
			this.mailID = mailID;
	}
	
	//Setters
	public void setFirstName(String firstName){
		if (firstName != null && firstName != "")
			this.firstName = firstName;
	}
	
	public void setLastName(String lastName){
		if (lastName != null && lastName != "")
			this.lastName = lastName;
	}
	
	public void setEmployeeID(int employeeID){
		if (employeeID >= 0 && employeeID <= 99999)
			this.employeeID = employeeID;
	}
	
	public void setMailID(String mailID){
		if (mailID.matches("[a-zA-Z0-9_]+@[a-zA-Z0-9.]+[com|ca|edu|net|gov|org]"))
			this.mailID = mailID;
	}
	
	//Getters
	public String getFirstName(){
		return firstName;
	}
	
	public String getLastName(){
		return lastName;
	}
	
	public int getEmployeeID(){
		return employeeID;
	}
	
	public String getMailID(){
		return mailID;
	}
	
    public String getData() {
        return firstName + ":" + lastName + ":" + employeeID + ":" + mailID;
	}
	
	@Override
	public boolean equals(Object other){
		if (other == null)
			return false;
		else if (other.getClass() != this.getClass())
			return false;
		else {
			Record rec = (Record) other;
			return (rec.getEmployeeID() == this.employeeID && rec.getFirstName().equals(this.firstName)
					&& rec.getLastName().equals(this.lastName) && rec.getMailID().equals(this.mailID));
		}
	}

	public abstract JSONObject getJSONObject();
}
