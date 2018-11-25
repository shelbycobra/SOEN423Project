package FrontEnd;

import javafx.util.Pair;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;

public class Message
{
	private int id;
	private ArrayList<Pair<Integer, String>> returnMessages; // Port number, message.
	private HashMap<Integer, Long> returnTimes; // Port number, response time.
	private long startTime;
	private JSONObject sendData;
	
	public Message(int id)
	{
		this.id = id;
		this.returnMessages = new ArrayList<>();
		this.returnTimes = new HashMap<>();
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

	public HashMap<Integer, Long> getReturnTimes()
	{
		return returnTimes;
	}

	public void setReturnTime(int port, long time)
	{
		returnTimes.put(port, time);
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
