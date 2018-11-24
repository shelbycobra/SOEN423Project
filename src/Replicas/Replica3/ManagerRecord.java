package Replicas.Replica3;

import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

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

	@Override
	public JSONObject getJSONObject() {
		JSONObject jsonObject = super.getJSONObject();

		JSONArray jsonArray = new JSONArray();
		for (Project project : projects) {
			jsonArray.add(project.getJSONObject());
		}

		jsonObject.put(DEMS.MessageKeys.PROJECTS, jsonArray);
		jsonObject.put(DEMS.MessageKeys.LOCATION, location);

		return jsonObject;
	}

}
