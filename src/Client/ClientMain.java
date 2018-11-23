package Client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

import FrontEnd.Project;

public class ClientMain
{
	public static void main(String[] args)  throws IOException
	{
		String[] orbArguments = new String[]{"-ORBInitialPort", "1050", "-ORBInitialHost", "localhost"};
		ArrayList<ClientThread> clients = new ArrayList<ClientThread>();
		ArrayList<FutureTask<String>> tasks = new ArrayList<>();
		ArrayList<String> responses = new ArrayList<>();
		
		for (int i = 0; i < 10; i++)
		{
			try
			{
				ClientThread clientThread = new ClientThread(orbArguments, "CA100"+i, 2);
				clients.add(clientThread);
				tasks.add(new FutureTask<>(clientThread));
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
		
		for (int i = 0; i < 10; i++)
		{
			Thread thread = new Thread(tasks.get(i));
			thread.start();
		}
		
		for (int i = 0; i < 10; i++)
		{
			try
			{
				responses.add(tasks.get(i).get());
			}
			catch (InterruptedException | ExecutionException e)
			{
				e.printStackTrace();
			}
		}
		
		for (int i = 0; i < 10; i++)
		{
			System.out.println(responses.get(i));
		}
		
//		Client client = new Client(new String[]{args[0], args[1], args[2], args[3]}, args[4]);
//
//		if (!client.connect())
//		{
//			System.exit(0);
//		}
//		
//		client.log(client.managerID + " logged into " + client.managerLocale + ".\r\n");
//		
//		Scanner sc = new Scanner(System.in);
//		
//		while(true)
//		{
//			System.out.println("Distributed Employee Management System Menu");
//			System.out.println("===========================================");
//			System.out.println("1) Create Manager Record");
//			System.out.println("2) Create Employee Record");
//			System.out.println("3) View Record Count");
//			System.out.println("4) Edit a Record");
//			System.out.println("5) View Records");
//			System.out.println("6) Transfer Record");
//			System.out.println("0) Exit");
//			
//			System.out.print("Enter choice: ");
//            int choice = sc.nextInt();
//            sc.nextLine(); // Consumes the \n from the sc.nextInt()
//            
//            if (choice == 0)
//            {
//            	System.out.println("Thank you for using the DEMS.");
//            	break;
//            }
//            else if (choice == 1)
//            {
//            	String firstName, lastName, employeeID, mailID;
//            	
//            	do
//            	{
//	        		System.out.print("First Name: ");
//	            	firstName = sc.nextLine();
//	            	System.out.print("Last Name: ");
//	            	lastName = sc.nextLine();
//	            	System.out.print("Employee ID: ");
//	            	employeeID = sc.nextLine();
//	            	System.out.print("Mail ID: ");
//	            	mailID = sc.nextLine();
//            	}
//            	while (client.areAnyFieldsEmpty(firstName, lastName, employeeID, mailID));
//            	
//            	String wasRecordCreated = client.createMRecord(firstName, lastName, employeeID, mailID, new Project[] {new Project("PID", "ClientName", "ProjectName")}, client.managerLocale);
//            	
//            	if (!wasRecordCreated.isEmpty())
//            	{
//            		client.log("Manager Record Created: " + firstName + ", " + lastName + ", " + employeeID + ", " + mailID + ", " + client.managerLocale);
//            	}
//            	else
//            	{
//            		System.out.println("Creation failed.");
//            	}
//			}
//            else if (choice == 2)
//            {
//            	String firstName, lastName, employeeID, mailID, projectID;
//            	
//            	do
//            	{
//	            	System.out.print("First Name: ");
//	            	firstName = sc.nextLine();
//	            	System.out.print("Last Name: ");
//	            	lastName = sc.nextLine();
//	            	System.out.print("Employee ID: ");
//	            	employeeID = sc.nextLine();
//	            	System.out.print("Mail ID: ");
//	            	mailID = sc.nextLine();
//	            	System.out.print("Project ID: ");
//	            	projectID = sc.nextLine();
//            	}
//            	while (client.areAnyFieldsEmpty(firstName, lastName, employeeID, mailID, projectID));
//            	
//            	String wasRecordCreated = client.createERecord(firstName, lastName, employeeID, mailID, projectID);
//            	
//            	if (!wasRecordCreated.isEmpty())
//            	{
//            		System.out.println("Creation was successful.");
//            		client.log("Employee Record Created: " + firstName + ", " + lastName + ", " + employeeID + ", " + mailID + ", " + projectID);
//            	}
//            	else
//            	{
//            		System.out.println("Creation failed.");
//            	}
//			}
//            else if (choice == 3)
//            {
//            	try
//            	{
//            		client.log("Record Count:");
//            		client.log(client.getRecordCount());
//            	}
//            	catch (Exception e)
//            	{
//            		System.out.println(e.getMessage());
//            	}
//			}
//            else if (choice == 4)
//            {
//            	String key, field, value;
//            	
//            	do
//            	{
//	            	System.out.print("Record ID: ");
//	            	key = sc.nextLine();
//	            	System.out.print("Field: ");
//	            	field = sc.nextLine();
//	            	System.out.print("New Value: ");
//	            	value = sc.nextLine();
//            	}
//            	while (client.areAnyFieldsEmpty(key, field, value));
//            	
//				String wasRecordEdited = client.editRecord(key, field, value);
//				
//				if (!wasRecordEdited.isEmpty())
//            	{
//            		System.out.println("Edit was successful.");
//            		client.log("Changed " + field + " for " + key + " to " + value);
//            	}
//            	else
//            	{
//            		System.out.println("Edit failed.");
//            	}
//			}
//            else if (choice == 5)
//            {
//            	String recordID, remoteServerName;
//            	
//            	do
//            	{
//            		System.out.print("Record ID: ");
//            		recordID = sc.nextLine();
//            		System.out.print("Remote Server Center Name: ");
//            		remoteServerName = sc.nextLine();
//            	}
//            	while (client.areAnyFieldsEmpty(recordID, remoteServerName));
//            	
//            	String wasRecordTransferred = client.transferRecord(client.managerID, recordID, remoteServerName);
//				
//				if (!wasRecordTransferred.isEmpty())
//            	{
//            		System.out.println("Transfer was successful.");
//            		client.log("Transferred record " + recordID + " from " + client.managerLocale + " to " + remoteServerName);
//            	}
//            	else
//            	{
//            		System.out.println("Edit failed.");
//            	}
//            }
//            else
//            {
//            	System.out.println("Invalid choice. Choose from 0 to 5.");
//			}
//            
//            System.out.println("");
//		}
//		
//		sc.close();
	}
}
