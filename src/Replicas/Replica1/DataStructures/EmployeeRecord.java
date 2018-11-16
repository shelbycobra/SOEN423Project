package Replicas.Replica1.DataStructures;

public class EmployeeRecord extends Record {

	private String projectID = "";
	
	public EmployeeRecord(String firstName, String lastName, int employeeID, String mailID, String projectID) {
		super(firstName, lastName, employeeID, mailID);
		if (projectID.matches("[A-Z][0-9]+") && projectID.length() == 6)
			this.projectID = projectID;
	}
	
	//Setters
	public void setProjectID(String projectID){
		if (projectID.matches("[A-Z][0-9]+") && projectID.length() == 6)
			this.projectID = projectID;
	}
	
	//Getters
	public String getProjectID(){
		return projectID;
	}
	
	@Override
	public boolean equals(Object other){
		if (other == null)
			return false;
		else if (other.getClass() != this.getClass())
			return false;
		else {
			EmployeeRecord eRec = (EmployeeRecord) other;
			return (super.equals(eRec) && eRec.getProjectID().equals(this.projectID));
		}
	}
	
    public String getData() {
        return super.getData() + ":" + projectID;
        
	}
	
    public String printData() {
        String msg = "\n\tName:" + getFirstName() + " " + getLastName() + "\n\tEmployee ID: " + getEmployeeID() + "\n\tMail ID: " + getMailID() + "\n\tProjectID: " + projectID + "\n";
        System.out.println(msg);
        return msg;
	}

}
