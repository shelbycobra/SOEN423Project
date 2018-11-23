package FrontEnd;

import java.util.ArrayList;

import org.json.simple.JSONObject;

import javafx.util.Pair;

public class Message
{
	private int id;
	private ArrayList<Pair<Integer, String>> returnMessages; // Port number, message.
	private ArrayList<Pair<Integer, Long>> returnTimes; // Port number, response time.
	private long startTime;
	private JSONObject sendData;
	
	public Message(int id)
	{
		this.id = id;
	}

	public int getId()
	{
		return id;
	}

	public void setId(int id)
	{
		this.id = id;
	}

	public ArrayList<Pair<Integer, String>> getReturnMessages()
	{
		return returnMessages;
	}

	public void setReturnMessage(Pair<Integer, String> returnMessage)
	{
		this.returnMessages.add(returnMessage);
	}

	public JSONObject getSendData()
	{
		return sendData;
	}

	public void setSendData(JSONObject sendData)
	{
		this.sendData = sendData;
	}

	public ArrayList<Pair<Integer, Long>> getReturnTimes()
	{
		return returnTimes;
	}

	public void setReturnTime(int port, long time)
	{
		returnTimes.add(new Pair<Integer, Long>(port, time));
	}

	public long getStartTime()
	{
		return startTime;
	}

	public void setStartTime(long startTime)
	{
		this.startTime = startTime;
	}
}
