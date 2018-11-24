package Replicas.Replica3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class Projects implements Iterable<Project> {

	private List<Project> projects;

	public Projects(JSONArray jsonArray) {
		this.projects = new ArrayList<Project>();
		for (int i = 0; i < jsonArray.size(); i++) {
			JSONObject jsonObjectProject = (JSONObject) jsonArray.get(i);
			this.projects.add(new Project(jsonObjectProject));
		}
	}

	public List<Project> getProjects() {
		return projects;
	}

	public JSONArray getJSONArray() {
		JSONArray jsonArray = new JSONArray();
		for (Project project : projects) {
			jsonArray.add(project.getJSONObject());
		}
		return jsonArray;
	}

	public void addProject(Project project) {
		projects.add(project);
	}

	@Override
	public Iterator<Project> iterator() {
		class ProjectsIterator implements Iterator<Project> {
			private int index = 0;

			@Override
			public boolean hasNext() {
				return index < projects.size();
			}

			@Override
			public Project next() {
				return projects.get(index++);
			}
		}

		return new ProjectsIterator();
	}

}
