package Client;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Timestamp;

import org.omg.CORBA.ORB;
import org.omg.CosNaming.NamingContextExt;
import org.omg.CosNaming.NamingContextExtHelper;

import FrontEnd.FrontEndInterface;
import FrontEnd.FrontEndInterfaceHelper;
import FrontEnd.Project;

public class Client
{
	FrontEndInterface server;
	String managerID;
	String managerLocale;
	String[] orbArguments;
	
	public Client(String[] orbArguments, String managerID) throws IOException
	{
		this.managerID = managerID;
		this.managerLocale = managerID.substring(0, 2);
		this.orbArguments = orbArguments;
	}
	
	public boolean connect() throws IOException
	{
		if (!managerLocale.matches("CA") && !managerLocale.matches("US") && !managerLocale.matches("UK"))
		{
			System.err.println("Invalid Manager ID. Must start with CA, US, or UK.");
			return false;
		}
		
		try
		{
			// create and initialize the ORB
			ORB orb = ORB.init(orbArguments, null);
			
			// get the root naming context
			org.omg.CORBA.Object objRef = orb.resolve_initial_references("NameService");
			
			// Use NamingContextExt instead of NamingContext. This is part of the Interoperable naming Service.  
			NamingContextExt ncRef = NamingContextExtHelper.narrow(objRef);
			
			// resolve the Object Reference in Naming
			server = FrontEndInterfaceHelper.narrow(ncRef.resolve_str("FrontEndServer"));
			switch (managerLocale)
			{
				case "CA":	System.out.println("Connected to Canada Server");
							break;
				case "US":	System.out.println("Connected to US Server");
							break;
				case "UK":	System.out.println("Connected to UK Server");
							break;
				default:	System.err.println("Invalid Manager ID. Must start with CA, US, or UK.");
							return false;
			}
			
		}
		catch (Exception e)
		{
			System.out.println("ERROR : " + e);
			e.printStackTrace(System.out);
			return false;
        }
		
		return true;
	}
	
	String createMRecord(String firstName, String lastName, String employeeID, String mailID, Project[] projects, String location)
	{
		return server.createMRecord(managerID, firstName, lastName, employeeID, mailID, new Project[]{new Project("PID", "ClientName", "ProjectName")}, managerLocale);
	}
	
	String createERecord(String firstName, String lastName, String employeeID, String mailID, String projectID)
	{
		return server.createERecord(managerID, firstName, lastName, employeeID, mailID, projectID);
	}
	
	String getRecordCount()
	{
		return server.getRecordCount(managerID);
	}
	
	String editRecord(String recordID, String fieldName, String newValue)
	{
		return server.editRecord(managerID, recordID, fieldName, newValue);
	}
	
	String transferRecord(String managerID, String recordID, String remoteCenterServerName)
	{
		return server.transferRecord(managerID, recordID, remoteCenterServerName);
	}
	
	public void log(String input) throws IOException
	{
		PrintWriter printWriter = null;
		
		try
		{
			File directory = new File("logs");
		    
			if (!directory.exists())
		    {
		        directory.mkdir();
		    }
			
			FileWriter fileWriter = new FileWriter("logs/" + managerID + "_ManagerClientLog.txt", true);
			printWriter = new PrintWriter(fileWriter);
			printWriter.printf("[%s] %s \r\n", new Timestamp(System.currentTimeMillis() / 1000), input);
			System.out.println(input);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			if (printWriter != null)
			{
				printWriter.close();
			}
		}
	}
	
	public boolean areAnyFieldsEmpty(String ... args)
	{
		for (String arg : args)
		{
			if (arg.trim().isEmpty())
			{
				System.err.println("\r\nNo empty fields allowed. Try again.\r\n");
				return true;
			}
		}
		
		return false;
	}
}
