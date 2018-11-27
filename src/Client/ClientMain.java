package Client;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.InputMismatchException;
import java.util.Scanner;

import DEMS.Config;
import FrontEnd.Project;

public class ClientMain
{

	private static String location = "";
	private static String managerID;
	
	public static void main(String[] args)  throws IOException
	{
//		String[] orbArguments = new String[]{"-ORBInitialPort", "1050", "-ORBInitialHost", "localhost"};
//		ArrayList<ClientThread> clients = new ArrayList<ClientThread>();
//		ArrayList<FutureTask<String>> tasks = new ArrayList<>();
//		ArrayList<String> responses = new ArrayList<>();
//		int numberOfClients = 1;
//		
//		for (int i = 0; i < numberOfClients; i++)
//		{
//			try
//			{
//				ClientThread clientThread = new ClientThread(orbArguments, "CA100"+i, Config.CREATE_EMPLOYEE_RECORD);
//				clients.add(clientThread);
//				tasks.add(new FutureTask<>(clientThread));
//			}
//			catch (IOException e)
//			{
//				e.printStackTrace();
//			}
//		}
//		
//		for (int i = 0; i < numberOfClients; i++)
//		{
//			Thread thread = new Thread(tasks.get(i));
//			thread.start();
//		}
//		
//		for (int i = 0; i < numberOfClients; i++)
//		{
//			try
//			{
//				responses.add(tasks.get(i).get());
//			}
//			catch (InterruptedException | ExecutionException e)
//			{
//				e.printStackTrace();
//			}
//		}
//		
//		for (int i = 0; i < numberOfClients; i++)
//		{
//			System.out.println(responses.get(i));
//		}
		String[] orbArguments = new String[]{"-ORBInitialPort", "1050", "-ORBInitialHost", Config.IPAddresses.FRONT_END};

		int port;
		BufferedWriter log;

		try {
			System.out.println("\n--- WELCOME TO THE DISTRUBTED EMPLOYEE MANAGEMENT SYSTEM ---\n");

			Scanner keyboard = new Scanner(System.in);

			// Manager login
			managerLogin(keyboard);

			Client client = new Client(orbArguments, managerID);

			if (!client.connect())
			{
				System.exit(0);
			}

			client.log(client.managerID + " logged into " + client.managerLocale + ".\r\n");

			// Create log file
			log = setupLogFile(managerID);
			writeToLogFile(managerID + " Successful login", log);

			// Access server
			writeToLogFile("Connected to " + location + " server.", log);

			// Server operations
			receiveClientInput(client, keyboard, log);

		} catch (Exception e){
			e.printStackTrace();;
		}


//		Client client = new Client(new String[]{orbArguments[0], orbArguments[1], orbArguments[2], orbArguments[3]}, args[0]);
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
//			System.out.println("6) Transfer Record");
//			System.out.println("0) Exit");
//
//			System.out.print("Enter choice: ");
//            int choice = sc.nextInt();
//            sc.nextLine(); // Consumes the \n from the sc.nextInt()
//
//            if (choice == 0)
//            {
//            	System.out.println("Thank you for using the Client.");
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

	/*
	 *  MANAGER LOGIN
	 */
	private static void managerLogin(Scanner in) {
		while(true) {
			System.out.println("\n--- Please log in: ---\n");
			System.out.print("Manager ID: ");
			String input = in.nextLine();
			if (verifyUsername(input)) {
				managerID = input;
				break;
			} else
				System.out.println("\n** Invalid managerID, please try again. **\n");
		}
	}

	/*
	 * VERIFY USERNAME
	 */
	public static boolean verifyUsername(String name) {
		try {
			location = name.substring(0,2).toUpperCase();
			return (name.substring(2).length() == 4 && (Integer.parseInt(name.substring(2)) > 0 && Integer.parseInt(name.substring(2)) < 10000) && ("CA".equals(location) || "US".equals(location) || "UK".equals(location)));
		} catch (NumberFormatException | StringIndexOutOfBoundsException e) {
			return false;
		}
	}

	/*
	 * SETUP LOG FILE
	 */
	private static BufferedWriter setupLogFile(String managerID) throws IOException{
		String logFileName = "Logs/ManagerLogs/" + managerID + "_log.txt";
		Path path = Paths.get(logFileName);
		File logFile = new File(path.toAbsolutePath().toString());
		return new BufferedWriter(new FileWriter(logFile, true));
	}

	/*
	 * WRITE TO LOG FILE
	 */
	public static void writeToLogFile(String message, BufferedWriter log) throws IOException{
		Calendar cal = Calendar.getInstance();
		SimpleDateFormat sdf = new SimpleDateFormat("MMMMMMMMM dd, yyyy HH:mm:ss");
		log.write( sdf.format(cal.getTime()) + " - " + "Manager ID: " + managerID + " - " + message + "\n" );
		log.flush();
	}

	/*
	 * RECEIVE CLIENT INPUT
	 */
	private static void receiveClientInput(Client client, Scanner keyboard,  BufferedWriter log) throws IOException {
		while(true) {

			printOptions();

			int choice;
			try {
				choice = keyboard.nextInt();
			} catch (InputMismatchException e) {
				keyboard.nextLine();
				System.out.println("\n** Invalid input, please try again. **\n");
				continue;
			}

			switch (choice){
				case 1: {
					createManagerRecord(client, keyboard, log);
					continue;
				} case 2: {
					createEmployeeRecord(client, keyboard, log);
					continue;
				} case 3: {
					keyboard.nextLine();
					getRecordCount(client, log);
					continue;
				} case 4: {
					editARecord(client, keyboard, log);
					continue;
				} case 5: {
					transferARecord(client, keyboard, log);
					continue;
				} case 6: {
					keyboard.nextLine();
					exitSystem(client, log);
					continue;
				} default: {
					System.out.println("Please choose a valid option.");
					continue;
				}
			}
		}
	}

	/*
	 * PRINT OPTIONS
	 */
	private static void printOptions(){
		System.out.println("\n--- Possible tasks: ---\n");
		System.out.println("\t1. Create a Manager Record");
		System.out.println("\t2. Create an Employee Record");
		System.out.println("\t3. Get Record Count");
		System.out.println("\t4. Edit a Record");
		System.out.println("\t5. Transfer A Record");
		System.out.println("\t6. Exit");
		System.out.print("\nTo choose a task, enter a number: ");
	}

	/*
	 * 1. CREATE MANAGER RECORD
	 */
	public static void createManagerRecord(Client client, Scanner in,  BufferedWriter log) throws IOException {
		in.nextLine();
		System.out.println("\n--- Create a Manager Record ---");
		writeToLogFile("\nCreating a Manager Record", log);

		String firstName = askFirstName(in, log);
		String lastName = askLastName(in, log);

		writeToLogFile("Manager Name: " + firstName + " " + lastName, log);

		int employeeID = askEmployeeID(in, log);
		writeToLogFile("Employee ID: " + employeeID, log);

		String mailID = askMailID(in, log);
		writeToLogFile("Mail ID " + mailID, log);

		ArrayList<Project> projects = askProjectInfo(in, log);

		System.out.println("Projects: ");

		for(int i = 0; i<projects.size(); i++) {
			System.out.println("\tProject ID:" + projects.get(i).projectID + "\tProject Client: " + projects.get(i).clientName + "\tProject Name: " + projects.get(i).projectName);
		}

		askLocation_Strict(in, log);

		System.out.println("\nCreating Manager Record ...");
		writeToLogFile("Creating Manager Record ...", log);

		Project[] prj_arr = new Project[projects.size()];

		String msg = client.createMRecord(firstName, lastName, employeeID+"", mailID, projects.toArray(prj_arr), location);
		writeToLogFile("Created Manager Record for "+ firstName + " " + lastName + ", Employee ID: "+ employeeID + ".", log);
		System.out.println(msg);
	}

	/*
	 * 2. CREATE EMPLOYEE RECORD
	 */
	public static void createEmployeeRecord(Client client, Scanner in,  BufferedWriter log) throws IOException {
		in.nextLine();
		System.out.println("\n--- Create an Employee Record ---");
		writeToLogFile("Creating an Employee Record", log);

		String firstName = askFirstName(in, log);
		String lastName = askLastName(in, log);

		writeToLogFile("Employee Name: " + firstName + " " + lastName, log);

		int employeeID = askEmployeeID(in, log);
		writeToLogFile("Employee ID: " + employeeID, log);

		String mailID = askMailID(in, log);
		writeToLogFile("Mail ID " + mailID, log);

		String projectID = askProjectID(in, log);
		writeToLogFile("Project ID: " + projectID, log);

		System.out.println("\nCreating Employee Record ...");
		writeToLogFile("Creating Employee Record ...", log);
		String msg = client.createERecord(firstName, lastName, employeeID+"", mailID, projectID);
		writeToLogFile("Created Employee Record for "+ firstName + " " + lastName + ", EmployeeID: " + employeeID + ".", log);
		System.out.println(msg);
	}

	/*
	 * 3. GET RECORD COUNT
	 */
	public static void getRecordCount(Client client, BufferedWriter log) throws IOException {
		System.out.println("\n--- Get Record Count ---");
		writeToLogFile("Getting Record Count ...", log);
		System.out.println("Contacting other servers...");
		String counts = client.getRecordCount();
		System.out.println("Record count: " + counts);
		writeToLogFile("Record count: " + counts, log);
	}

	/*
	 * 4. EDIT A RECORD
	 */
	public static void editARecord(Client client, Scanner in,  BufferedWriter log) throws IOException {
		in.nextLine();
		System.out.println("\n--- Edit a Record ---");

		String recordID = askRecordID(in, log);
		writeToLogFile("Record ID: " + recordID, log);

		String fieldName = askFieldName(in, log, recordID);
		writeToLogFile("Field Name: " + fieldName, log);

		String projectFieldName = "";
		if ("project".equals(fieldName))
			projectFieldName = askProjectFieldName(in, log, fieldName);

		String newValue = "";
		if ("project_id".equals(projectFieldName) || "project_id".equals(fieldName)){
			newValue = askProjectID(in, log);
		} else if ("location".equals(fieldName)){
			newValue = askLocation(in, log);
		} else if ("mail_id".equals(fieldName)) {
			newValue = askMailID(in, log);
		} else {
			System.out.print("New Value: ");
			newValue = in.nextLine();
			writeToLogFile("New Value: " + newValue, log);
		}

		System.out.println("\nEditing file ...");
		String output = client.editRecord(recordID, fieldName, newValue);
		System.out.println("\n" + output);

		writeToLogFile("Edit file message: " + output, log);
	}

	/*
	 * 5. TRANSFER A RECORD
	 */
	public static void transferARecord(Client client, Scanner in,  BufferedWriter log) throws IOException {
		in.nextLine();
		System.out.println("\n--- Transfer a Record ---");

		String recordID = askRecordID(in, log);
		writeToLogFile("Record ID: " + recordID, log);

		String loc = askLocation(in, log);
		writeToLogFile("Location: " + loc, log);

		String str = "\nAttempting to transfer record ...";
		System.out.println(str);
		writeToLogFile(str, log);

		String output = client.transferRecord(managerID, recordID, loc);
		System.out.println(output);
		writeToLogFile("Transfer A Record: " + output, log);

	}

	/*
	 *  6. EXIT SYSTEM
	 */
	public static void exitSystem(Client client, BufferedWriter log) throws IOException {
		System.out.println("\nLogging out and exiting system...\n");
		writeToLogFile("Logging out and exiting system.\n", log);
		log.close();
		System.exit(0);
	}

	/*
	 * ASK FIRST NAME
	 */
	private static String askFirstName(Scanner in, BufferedWriter log) throws IOException {
		String firstName;
		while (true) {
			System.out.print("First name: ");
			firstName = in.nextLine();
			if (firstName.equals("") || firstName.equals(null)) {
				System.out.println("Invalid first name. Cannot be empty.");
				writeToLogFile("Invalid first name. Cannot be empty", log);
			} else return firstName;
		}
	}

	/*
	 * ASK LAST NAME
	 */
	private static String askLastName(Scanner in, BufferedWriter log) throws IOException {
		String lastName;
		while (true) {
			System.out.print("Last name: ");
			lastName = in.nextLine();
			if (lastName.equals("") || lastName.equals(null)) {
				System.out.println("Invalid last name. Cannot be empty.");
				writeToLogFile("Invalid last name. Cannot be empty.", log);
			}
			else return lastName;
		}
	}

	/*
	 * ASK RECORD ID
	 */
	private static String askRecordID(Scanner in, BufferedWriter log) throws IOException {
		String recordID;
		while(true) {
			System.out.print("Record ID: ");
			recordID = in.nextLine();
			try {
				Integer.parseInt(recordID.substring(2)); // Will throw exception if it contains any letters.
				if ((!recordID.startsWith("ER") && !recordID.startsWith("er") && !recordID.startsWith("MR") && !recordID.startsWith("mr"))){
					throw new NumberFormatException();
				} else return recordID;
			} catch (NumberFormatException e) {
				System.out.println("\n** Invalid record ID. **\n");
				writeToLogFile("Invalid record: " + recordID, log);
			}
		}
	}

	/*
	 * ASK FIELD NAME
	 */
	private static String askFieldName(Scanner in, BufferedWriter log, String recordID) throws IOException {
		String fieldName;
		while(true){
			System.out.print("Field Name : ");
			fieldName = in.nextLine();
			if ((recordID.startsWith("ER") || recordID.startsWith("er")) && !"mail_id".equals(fieldName) && !"project_id".equals(fieldName) ){
				System.out.println("\n** Invalid fieldName. You can only choose \"mail_id\" or \"project_id\". **\n");
				writeToLogFile("Invalid Employee Record Field Name: " + fieldName, log);
			} else if ((recordID.startsWith("MR") || recordID.startsWith("mr")) && !"mail_id".equals(fieldName)  && !"project".equals(fieldName)  && !"location".equals(fieldName)  ) {
				System.out.println("\n** Invalid fieldName. You can only choose \"mail_id\", \"project\", or \"location\". **\n");
				writeToLogFile("Invalid Manager Record Field Name: " + fieldName, log);
			} else if (!recordID.startsWith("ER") && !recordID.startsWith("MR") && !recordID.startsWith("er") && !recordID.startsWith("mr")){
				System.out.println("\n** Invalid RecordID. Exiting System. **\n");
				writeToLogFile("Invalid Record ID: Exiting System", log);
				System.exit(0);
			} else return fieldName;
		}
	}

	/*
	 * ASK PROJECT FIELD NAME
	 */
	private static String askProjectFieldName(Scanner in, BufferedWriter log, String fieldName) throws IOException {
		String projectFieldName;
		while(true){
			System.out.print("Project Field: ");
			projectFieldName = in.nextLine();
			if ( !"project_id".equals(projectFieldName) && !"project_client".equals(projectFieldName) && !"project_name".equals(projectFieldName)){
				System.out.println("\n** Invalid project field name. You can only choose \"project_id\", \"project_client\", or \"project_name\". **\n");
				writeToLogFile("Invalid Project Field Name: " + projectFieldName, log);
			} else return projectFieldName;
		}
	}

	/*
	 * ASK EMPLOYEE ID
	 */
	private static int askEmployeeID(Scanner in, BufferedWriter log) throws IOException{
		int employeeID = 0;
		while(true){
			System.out.print("Employee ID: ");
			try{
				employeeID = Integer.parseInt(in.nextLine());
				if (employeeID < 1 || employeeID > 99999){
					System.out.println("\n** ID must be between 1 and 99999. **\n");
					writeToLogFile("Invalid ID number (ID must be between 1 and 99999): " + employeeID, log);
				} else return employeeID;
			} catch (NumberFormatException | InputMismatchException e){
				in.nextLine();
				System.out.println("\n** Invalid input. **\n");
				writeToLogFile("Invalid input type: " + employeeID, log);
			}
		}
	}

	/*
	 * ASK MAIL ID
	 */
	private static String askMailID(Scanner in, BufferedWriter log) throws IOException {
		String mailID;
		while(true){
			System.out.print("Mail ID: ");
			mailID = in.nextLine();
			if (!mailID.matches("[a-zA-Z0-9_]+@[a-zA-Z0-9.]+.[com|ca|edu|net|gov|org|uk]")){
				System.out.println("\n** Invalid email. **\n");
				writeToLogFile("Invalid Email: " + mailID, log);
			} else return mailID;
		}
	}

	/*
	 * ASK PROJECT INFO
	 */
	private static ArrayList<Project> askProjectInfo(Scanner in, BufferedWriter log) throws IOException {
		ArrayList<Project> projects = new ArrayList<>();

		while(true) {
			String projectID = askProjectID(in, log);

			System.out.print("Project client name: ");
			String projectClient = in.nextLine();

			System.out.print("Project name: ");
			String projectName = in.nextLine();

			Project project = new Project(projectID, projectClient, projectName);
			projects.add(project);
			writeToLogFile("Added project " + projectID + " to record.", log);

			while(true) {
				System.out.print("Add another project (y/n)?");
				String answer = in.nextLine();
				if ("N".equals(answer.toUpperCase())){
					writeToLogFile("Chose not to add another project.", log);
					return projects;
				} else if (!"Y".equals(answer.toUpperCase())){
					System.out.println("\n** Invalid input. Please try again. **\n");
					writeToLogFile("Invalid answer: " + answer, log);
					continue;
				}
				writeToLogFile("Chose to add another project.", log);
				break;
			}
		}
	}

	/*
	 * ASK PROJECT ID
	 */
	private static String askProjectID(Scanner in, BufferedWriter log) throws IOException {
		String projectID;
		while(true){
			System.out.print("Project ID: ");
			projectID = in.nextLine();
			if (!projectID.matches("[A-Z][0-9]+") || projectID.length() != 6){
				System.out.println("\n** Invalid project ID. **\n");
				writeToLogFile("Invalid project ID: " + projectID, log);
			} else return projectID;
		}
	}

	/*
	 * ASK LOCATION_STRICT (Can only enter current location)
	 */
	private static String askLocation_Strict(Scanner in, BufferedWriter log) throws IOException{
		String loc = "";
		while(true){
			System.out.print("Location: ");
			loc = in.nextLine();
			if (!loc.equals(location)) {
				System.out.println("\n** Access Denied: You can only create records for your own location. **\n");
				writeToLogFile("Access Denied: Attempted to create a record for another location.", log);
			} else return loc;
		}
	}

	/*
	 * ASK LOCATION (Not strict; used when editing a file).
	 */
	private static String askLocation(Scanner in, BufferedWriter log) throws IOException{
		String loc = "";
		while(true){
			System.out.print("Location: ");
			loc = in.nextLine();
			if (!"CA".equals(loc) && !"US".equals(loc) && !"UK".equals(loc)) {
				System.out.println("\n** Invalid location. Your options are \"CA\", \"US\" and \"UK\". **\n");
				writeToLogFile("Invalid location: "+ loc + ".", log);
			} else return loc;
		}
	}
}
