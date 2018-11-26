package Client;

import java.io.IOException;
import java.util.concurrent.Callable;

import DEMS.Config;
import FrontEnd.Project;

public class ClientThread implements Callable<String>
{
	Client client;
	String command;
	
	public ClientThread(String[] orbArguments, String managerID, String command) throws IOException
	{
		client = new Client(orbArguments, managerID);
		this.command = command;
	}
	
	@Override
	public String call()
	{
		String response = "";
		
		try
		{
			client.connect();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		
		switch (command)
		{
			case Config.CREATE_MANAGER_RECORD:	 response = client.createMRecord("Vik", "Singh", "1", "2", new Project[] {new Project("PID", "ClientName", "ProjectName")}, "CA");
					break;
			case Config.CREATE_EMPLOYEE_RECORD: response = client.createERecord("Peter", "Parker", "1", "peter@parker.com", "P12345");
					break;
			case Config.GET_RECORD_COUNT: response = client.getRecordCount();
					break;
			case Config.EDIT_RECORD: response = client.editRecord("MR10000", "mailID", "1234");
					break;
			case Config.TRANSFER_RECORD: response = client.transferRecord("CA1111", "MR10000", "US");
					break;
		}
		
		return response;
	}
}
