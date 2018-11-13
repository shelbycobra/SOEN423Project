package Replicas.Replica1.DataStructures;
import Replicas.Project;
import Replicas.Record;

public class ManagerRecord extends Record {
	
	private Project[] projects = new Replicas.Replica1.DataStructures.Project[10];
	private String location;

	public ManagerRecord(String firstName, String lastName, int employeeID, String mailID, Replicas.Replica1.DataStructures.Project[] projects, String location) {
		super(firstName, lastName, employeeID, mailID);
		if (projects != null)
			this.projects = projects;
		if (location.equals("CA") || location.equals("UK") || location.equals("US"))
			this.location =  location;

        System.out.println("Projects of MR" + employeeID + ": ");
        for (Replicas.Replica1.DataStructures.Project p: projects) {
            System.out.println(p);
        }
	}
	
	public void setLocation(String location){
		if (location.equals("CA") || location.equals("UK") || location.equals("US"))
			this.location =  location;
	}
	
	//Getters
	public Replicas.Replica1.DataStructures.Project getProject(int i) {
        return projects[i];
	}
	
	public Replicas.Replica1.DataStructures.Project[] getProjects(){
		return projects;
	}
	
	public String getLocation(){
		return location;
	}
		
    public String getData() {
        String project_strs = ":";
        for (int i = 0; i < projects.length; i++) {
            Replicas.Replica1.DataStructures.Project project = projects[i];
            project_strs += project.getProjectID() + ":" + project.getProjectClient() + ":" + project.getProjectName() + ":";
        }
        return super.getData() + project_strs + location;
	}
	
    public String printData() {
        String msg = "\n\tName:" + getFirstName() + " " + getLastName() + "\n\tEmployee ID: " + getEmployeeID()
                + "\n\tMail ID: " + getMailID() + "\n\tLocation: " + location + "\n\tProjects:";
        for (int i  = 0; i < projects.length; i++){
            msg += "\n\t\tProjectID: " + projects[i].getProjectID() + "\n\t\tProject Client: " + projects[i].getProjectClient() + "\n\t\tProject Name: " + projects[i].getProjectName() + "\n";
        }
        
        System.out.println(msg);
        return msg;
	}
	
	@Override
	public boolean equals(Object other){
		if (other == null)
			return false;
		else if (other.getClass() != this.getClass())
			return false;
		else {
			ManagerRecord eRec = (ManagerRecord) other;
			return (super.equals(eRec) && eRec.getProjects().equals(this.projects) && eRec.getLocation().equals(this.location));
		}
	}
}
