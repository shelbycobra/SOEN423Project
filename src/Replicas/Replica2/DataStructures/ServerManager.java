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
	private int currentRecordID = 0;

	public void example()
	{
		System.out.println("running example: " + currentRecordID);
	}

	public synchronized String getNextID()
    {
		String recordID = "";
		for (int i = 0; i < 5 - (currentRecordID + "").length(); i++)
			recordID += "0";
		recordID += currentRecordID++;
		return recordID;
	}
	
	public Record findRecordByID(String recordID, HashMap<Character, ArrayList<Record>> records)
	{
		for (Character key : records.keySet())
		{
			ArrayList<Record> listOfRecords = records.get(key);
			
			for (Record record : listOfRecords)
			{
				if (record.mRecordID.equals(recordID))
				{
					return record;
				}
			}
		}
		
		return null;
	}
	
	public byte[] RecordToByte(Record record) throws IOException
	{
	    ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
	    ObjectOutputStream objStream = new ObjectOutputStream(byteStream);
	    objStream.writeObject(record);

	    byte[] array = byteStream.toByteArray();

	    byteStream.close();
	    objStream.close();

	    return array;
	}

	public Record byteToRecord(byte[] bytes) throws IOException, ClassNotFoundException
	{
	    ByteArrayInputStream byteStream = new ByteArrayInputStream(bytes);
	    ObjectInputStream objStream = new ObjectInputStream(byteStream);
        Record record = (Record) objStream.readObject();

        byteStream.close();
        objStream.close();

        return record;
	}

	public synchronized int getRecordCount(HashMap<Character, ArrayList<Record>> records) {
	    int count = 0;
        for (Character key : records.keySet())
            count += records.get(key).size();
        return count;
    }
}
