package Replicas.Replica3;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Logger {

	public static Logger logger;

	private PrintWriter logFile;
	private String description;

	public Logger(String description) {
		this.description = description;
		String timeStamp = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(new Date());
		String logFileName = String.format("Logs/Replica3/%s-%s.log", description, timeStamp);
		try {
			logFile = new PrintWriter(logFileName, "UTF-8");
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void log(String message) {
		String timeStamp = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(new Date());
		String logMessage = String.format("%s | %s | %s", description, timeStamp, message);
		logFile.println(logMessage);
		System.out.println(logMessage);
	}

	public void close() {
		logFile.close();
	}
}
