package Replicas.Replica3;

public class Project {
	private int ID;
	private String clientName;
	private String projectName;

	public Project(int ID, String clientName, String projectName) {
		this.ID = ID;
		this.clientName = clientName;
		this.projectName = projectName;
	}

	public int getID() {
		return ID;
	}

	public void setID(int iD) {
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

}
