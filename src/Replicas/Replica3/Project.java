package Replicas.Replica3;

import org.json.simple.JSONObject;

public class Project {
	private String ID;
	private String clientName;
	private String projectName;

	public Project(String ID, String clientName, String projectName) {
		this.ID = ID;
		this.clientName = clientName;
		this.projectName = projectName;
	}

	public Project(JSONObject jsonObject) {
		this.ID = (String) jsonObject.get(DEMS.MessageKeys.PROJECT_ID);
		this.clientName = (String) jsonObject.get(DEMS.MessageKeys.PROJECT_CLIENT);
		this.projectName = (String) jsonObject.get(DEMS.MessageKeys.PROJECT_NAME);
	}

	public String getID() {
		return ID;
	}

	public void setID(String iD) {
		ID = iD;
	}

	public String getClientName() {
		return clientName;
	}

	public void setClientName(String clientName) {
		this.clientName = clientName;
	}

	public String getProjectName() {
		return projectName;
	}

	public void setProjectName(String projectName) {
		this.projectName = projectName;
	}

	public JSONObject getJSONObject() {
		JSONObject jsonObject = new JSONObject();

		jsonObject.put(DEMS.MessageKeys.PROJECT_ID, ID);
		jsonObject.put(DEMS.MessageKeys.PROJECT_CLIENT, clientName);
		jsonObject.put(DEMS.MessageKeys.PROJECT_NAME, projectName);

		return jsonObject;
	}

}
