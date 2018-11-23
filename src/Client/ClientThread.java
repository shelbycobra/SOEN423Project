package Client;

import java.io.IOException;
import java.util.concurrent.Callable;

import FrontEnd.Project;

public class ClientThread implements Callable<String>
{
	Client client;
	int command;
	
	public ClientThread(String[] orbArguments, String managerID, int command) throws IOException
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
			case 1:	response = client.createMRecord("Vik", "Singh", "1", "2", new Project[] {new Project("PID", "ClientName", "ProjectName")}, "CA");
					break;
			case 2:	response = client.createERecord("Peter", "Parker", "1", "2", "3");
					break;
			case 3:	response = client.getRecordCount();
					break;
			case 4:	response = client.editRecord("MR10000", "mailID", "1234");
					break;
			case 5:	response = client.transferRecord("CA1111", "MR10000", "US");
					break;
		}
		
		return response;
	}
}
