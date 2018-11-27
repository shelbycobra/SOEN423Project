package Test;

import org.json.simple.JSONArray;

import Replicas.Replica3.Records;
import Replicas.Replica3.EmployeeRecord;
import Replicas.Replica3.CenterServerController;

public class GetDataTest {

	public static void main(String[] args) {

		Records records = new Records();
		EmployeeRecord employeeRecord = new EmployeeRecord("firstName", "lastName", 1234, "a@b.c", "5678");
		records.addRecord(employeeRecord);
		
		CenterServerController centerServerController = new CenterServerController();
		centerServerController.runServers(0);
		JSONArray data = records.getJSONArray("CA");
		centerServerController.setData(data);
		data = centerServerController.getData();
		
		System.out.println(data.toJSONString());

	}

}
