package Replicas.Replica3;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.List;

public class ManagerRecord extends Record {
	private static final long serialVersionUID = -7309796150452080853L;

	private Projects projects;
	private String location;

	public ManagerRecord(String firstName, String lastName, int employeeID, String mailID, Projects projects, String location) {
		super("MR", firstName, lastName, employeeID, mailID);
		this.projects = projects;
		this.location = location;
	}

	public ManagerRecord(JSONObject jsonObject) {
		super(jsonObject);
		this.projects = new Projects((JSONArray) jsonObject.get(DEMS.MessageKeys.PROJECTS));
		this.location = (String) jsonObject.get(DEMS.MessageKeys.LOCATION);
	}

	public Projects getProjects() {
		return projects;
	}

	public String getLocation() {
		return location;
	}

	public void setProjects(Projects projects) {
		this.projects = projects;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	@Override
	public String toString() {
		String str =  "ManagerRecord \n\trecordID=" + getRecordID() + ", \n\tfirstName=" + getFirstName() + ", \n\tlastName=" + getLastName()
		+ "\n\temployeeID=" + getEmployeeID() + "\n\tmailID=" + getMailID() + "\n\tlocation=" + location + "\n\tProjects:";

		for (Project p : projects.getProjects()) {
			str += "\n\t\tProject ID: " + p.getID();
			str += "\n\t\tProject Client: " + p.getClientName();
			str += "\n\t\tProject Name: " + p.getProjectName();
		}

		return str;
	}

	@Override
	public JSONObject getJSONObject() {
		JSONObject jsonObject = super.getJSONObject();
		jsonObject.put(DEMS.MessageKeys.PROJECTS, projects.getJSONArray());
		jsonObject.put(DEMS.MessageKeys.LOCATION, location);
		return jsonObject;
	}

}
