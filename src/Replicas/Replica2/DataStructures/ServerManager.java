package Replicas.Replica2.DataStructures;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;

public class ServerManager
{
	private static int currentRecordID = 10000;
	
	public static int caPort = 6000;
	public static int usPort = 6001;
	public static int ukPort = 6002;
	
	public static void example()
	{
		System.out.println("running example: " + currentRecordID);
	}

	public synchronized static int getNextID()
	{
		return currentRecordID++;
	}
	
	public static Record findRecordByID(String recordID, HashMap<Character, ArrayList<Record>> records)
	{
		for (Character key : records.keySet())
		{
			ArrayList<Record> listOfRecords = records.get(key);
			
			for (Record record : listOfRecords)
			{
				if (record.mRecordID.matches(recordID))
				{
					return record;
				}
			}
		}
		
		return null;
	}
	
	public static byte[] RecordToByte(Record record) throws IOException
	{
	    ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
	    ObjectOutputStream objStream = new ObjectOutputStream(byteStream);
	    objStream.writeObject(record);

	    return byteStream.toByteArray();
	}

	public static Record byteToRecord(byte[] bytes) throws IOException, ClassNotFoundException
	{
	    ByteArrayInputStream byteStream = new ByteArrayInputStream(bytes);
	    ObjectInputStream objStream = new ObjectInputStream(byteStream);

	    return (Record) objStream.readObject();
	}
}
