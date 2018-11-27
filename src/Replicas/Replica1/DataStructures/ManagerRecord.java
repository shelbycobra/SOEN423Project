package Replicas.Replica1.DataStructures;

import DEMS.MessageKeys;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class ManagerRecord extends Record {
	
	private Project[] projects = new Project[10];
	private String location;

	public ManagerRecord(String firstName, String lastName, int employeeID, String mailID, Project[] projects, String location) {
		super(firstName, lastName, employeeID, mailID);
		if (projects != null)
			this.projects = projects;
		if (location.equals("CA") || location.equals("UK") || location.equals("US"))
			this.location =  location;
	}
	
	public void setLocation(String location){
		if (location.equals("CA") || location.equals("UK") || location.equals("US"))
			this.location =  location;
	}
	
	//Getters
	public Project getProject(int i) {
        return projects[i];
	}
	
	public Project[] getProjects(){
		return projects;
	}
	
	public String getLocation(){
		return location;
	}
		
    public String getData() {
        String project_strs = ":";
        for (int i = 0; i < projects.length; i++) {
            Project project = projects[i];
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
	public JSONObject getJSONObject() {
		JSONObject record = new JSONObject();

		record.put(MessageKeys.FIRST_NAME, getFirstName());
		record.put(MessageKeys.LAST_NAME, getLastName());
		record.put(MessageKeys.EMPLOYEE_ID, getEmployeeID()+"");
		record.put(MessageKeys.MAIL_ID, getMailID());
		record.put(MessageKeys.LOCATION, location);
		record.put(MessageKeys.PROJECTS, getProjectsAsJSONArray());

		return record;
	}

	private JSONArray getProjectsAsJSONArray() {
		JSONArray projects = new JSONArray();

		for (Project p : this.projects)
			projects.add(p.toJSONObject());

		return projects;
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
