package FrontEnd;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.omg.CORBA.*;

import DEMS.Config;
import DEMS.MessageKeys;
import javafx.util.Pair;

/*
 * 1) Receives a client request as a CORBA invocation. 
 * 2) Forwards the request to the sequencer.
 * 3) Receive the results from the replicas.
 * 4) Sends a single correct result back to the client as soon as possible. 
 * 5) Informs all the RMs of a possibly failed replica that produced incorrect result. 
 */

@SuppressWarnings("unchecked")
public class FrontEndImpl extends FrontEndInterfacePOA
{
	private ORB orb;
	private Semaphore mutex = new Semaphore(1);
	private Semaphore receiveFromReplica = new Semaphore(0);
	private HashMap<Integer, Message> messages = new HashMap<>();
	private long longestTimeout = 1000;
	private JSONParser parser = new JSONParser();
	private int messageID = 1;
	private final AtomicBoolean listeningForResponses = new AtomicBoolean(true); 

	public FrontEndImpl()
	{
		ReplicaResponseListener responseListener = new ReplicaResponseListener();
		ProcessCrashMonitor processCrashMonitor = new ProcessCrashMonitor();
		Thread responseListenerThread = new Thread(responseListener);
		Thread processCrashMonitorThread = new Thread(processCrashMonitor);
		
		responseListenerThread.start();
		processCrashMonitorThread.start();
	}

	public void setORB(ORB orb_val)
	{
		orb = orb_val;
	}

	/*
	 * 0. Start a ReplicaResponseListener thread.
	 * 1. Receive request from client.
	 * 2. Build JSON message with a new messageID.
	 * 3. Start a SendToSequencer thread.
	 * 4. SendToSequencer sends the payload to the sequencer and starts a WaitForReplicaResponse thread.
	 * 5. As ReplicaResponseListener receives responses, WaitForReplicaResponse processes them based on message ID.
	 */

	private class SendToSequencer implements Callable<String>
	{
		Message payload;

		public SendToSequencer(Message payload)
		{
			this.payload = payload;
		}

		public String call()
		{
			DatagramSocket socket = null;
			
			try
			{
				socket = new DatagramSocket();
				byte[] messageBuffer = payload.getSendData().toString().getBytes();
				InetAddress host = InetAddress.getByName("localhost");
				DatagramPacket request = new DatagramPacket(messageBuffer, messageBuffer.length, host, Config.PortNumbers.FE_SEQ);

				System.out.println("Sending message to sequencer...");
				socket.send(request);

//				byte[] buffer = new byte[1000];
//				DatagramPacket reply = new DatagramPacket(buffer, buffer.length);
//				socket.receive(reply);
//				String replyString = new String(reply.getData(), reply.getOffset(), reply.getLength());
//				JSONObject jsonMessage = (JSONObject) parser.parse(replyString);
			}
			catch (SocketTimeoutException e)
			{
				System.out.println("Server on port " + Config.PortNumbers.FE_SEQ + " is not responding.");
			}
			catch (SocketException e)
			{
				System.out.println("Socket: " + e.getMessage());
			}
			catch (IOException e)
			{
				System.out.println("IO: " + e.getMessage());
			}
			finally
			{
				if (socket != null)
				{
					socket.close();
				}
			}
			
			return waitForResponse();
		}
		
		private String waitForResponse()
		{
			String response = null;
			Callable<String> waitForResponseCall = new WaitForReplicaResponse(payload);
			FutureTask<String> waitForResponseTask = new FutureTask<>(waitForResponseCall);
			Thread waitForResponseThread = new Thread(waitForResponseTask);
			
			waitForResponseThread.start();
			
			try
			{
				response = waitForResponseTask.get();
			}
			catch (InterruptedException e)
			{
				e.printStackTrace();
			}
			catch (ExecutionException e)
			{
				e.printStackTrace();
			}
			
			return response;
		}
	}
	
	private class WaitForReplicaResponse implements Callable<String>
	{
		Message message;
		
		public WaitForReplicaResponse(Message message)
		{
			this.message = message;
			System.out.println("Waiting for responses for message ID: " + messageID);
		}
		
        public String call()
        {
        	String response = null;
        	Pair<Integer, String> message1 = null, message2 = null, message3 = null;

        	while (true)
            {
        		try
				{
        			System.out.println(messageID + ": Waiting for available permits...");
					receiveFromReplica.acquire(2);
					System.out.println(messageID + ": Acquired semaphore permit!");
				}
        		catch (InterruptedException e)
				{
					e.printStackTrace();
				}
                
        		if (message.getReturnMessages().isEmpty())
        		{
        			receiveFromReplica.release(2);
        			continue;
        		}
        		
        		if (message.getReturnMessages().size() == 2)
        		{
        			message1 = message.getReturnMessages().get(0);
        			message2 = message.getReturnMessages().get(1);
        			
        			if (message1.getValue().equals(message2.getValue()))
        			{
        				response = message1.getValue();
        				break;
        			}
        			
        			System.out.println("One of the messages was bad, waiting for the third...");
        			receiveFromReplica.release(2);
        			continue;
        		}
        		else if (message.getReturnMessages().size() == 3)
        		{
        			message1 = message.getReturnMessages().get(0);
        			message2 = message.getReturnMessages().get(1);
        			message3 = message.getReturnMessages().get(2);
        			
        			if (message1.getValue().equals(message3.getValue()))
        			{
        				response = message1.getValue();
        				notifyReplicaOfFailure(Config.Failure.BYZANTINE, message2.getKey());
        				break;
        			}
        			
        			if (message2.getValue().equals(message3.getValue()))
        			{
        				response = message2.getValue();
        				notifyReplicaOfFailure(Config.Failure.BYZANTINE, message1.getKey());
        				break;
        			}
				}
        		else
        		{
        			receiveFromReplica.release(2);
				}
            }
        	
        	return response;
        }
	}
	
	private class ReplicaResponseListener extends Thread
	{
		@Override
		public void run()
		{
			System.out.println("Listening for responses from the replicas on port " + Config.PortNumbers.RE_FE + "...");
						
			DatagramSocket datagramSocket = null;
			
			try
			{
				datagramSocket = new DatagramSocket(Config.PortNumbers.RE_FE);
				datagramSocket.setSoTimeout(1000);

	        	while (listeningForResponses.get())
	            {
	        		try
	                {
	                    byte[] buffer = new byte[1000];
	                    DatagramPacket responsePacket = new DatagramPacket(buffer, buffer.length);
	                    
	                    datagramSocket.receive(responsePacket);
	                    String data = new String(responsePacket.getData()).trim();
	                    JSONObject jsonMessage = (JSONObject) parser.parse(data);
	                    Message message = messages.get(Integer.parseInt(jsonMessage.get(MessageKeys.MESSAGE_ID).toString()));
	                    Pair<Integer, String> returnMessage = new Pair<Integer, String>(responsePacket.getPort(), jsonMessage.get(MessageKeys.MESSAGE).toString());
	                    message.setReturnMessage(returnMessage);
	                    
	                    clockTime(message, responsePacket.getPort());
	                    
	                    receiveFromReplica.release();
	                    System.out.println("Received response from Replica Manager for ID: " + jsonMessage.get(MessageKeys.MESSAGE_ID).toString() + " Semaphore: " + receiveFromReplica.availablePermits());
	                }
	        		catch (ParseException | IOException e)
	        		{
						continue;
					}
	            }
			}
			catch (SocketException e1)
			{
				e1.printStackTrace();
			}
			finally
			{
				if (datagramSocket != null)
				{
					datagramSocket.close();
				}
				
				System.out.println("Not listening for responses any longer.");
			}
		}
	}
	
	private class ProcessCrashMonitor implements Runnable
	{
		@Override
		public void run()
		{
			while (listeningForResponses.get())
			{
				for (Message message : messages.values())
				{
					for (Pair<Integer, Long> time : message.getReturnTimes())
					{
						if (time.getValue() > longestTimeout)
						{
							notifyReplicaOfFailure(Config.Failure.PROCESS_CRASH, time.getKey());
						}
					}
				}
			}
		}
	}
	
	private void notifyReplicaOfFailure(Config.Failure failureType, Integer port)
	{
		DatagramSocket socket = null;
		JSONObject payload = new JSONObject();
		
		payload.put(MessageKeys.FAILURE_TYPE, failureType.toString());
		payload.put(MessageKeys.RM_PORT_NUMBER, port);
		
		try
		{
			socket = new DatagramSocket();
			byte[] messageBuffer = payload.toString().getBytes();
			InetAddress host = InetAddress.getByName("localhost");
			DatagramPacket request = new DatagramPacket(messageBuffer, messageBuffer.length, host, port);

			System.out.println("Sending message to Replica Manager...");
			socket.send(request);
		}
		catch (SocketTimeoutException e)
		{
			System.out.println("Replica Manager on port " + port + " is not responding.");
		}
		catch (SocketException e)
		{
			System.out.println("Socket: " + e.getMessage());
		}
		catch (IOException e)
		{
			System.out.println("IO: " + e.getMessage());
		}
		finally
		{
			if (socket != null)
			{
				socket.close();
			}
		}
	}
	
	private String makeRequest(JSONObject payload)
	{
		String response = null;
		Integer messageID = Integer.parseInt(payload.get(MessageKeys.MESSAGE_ID).toString());
		Message message = new Message(messageID);
		Callable<String> sendToSequencerCall = new SendToSequencer(message);
		FutureTask<String> sendToSequencerTask = new FutureTask<>(sendToSequencerCall);
		Thread sendToSequencerThread = new Thread(sendToSequencerTask);
		
		messages.put(messageID, message);
		startTimer(message);
		sendToSequencerThread.start();
		
		try
		{
			response = sendToSequencerTask.get();
			stopTimer(message.getId());
		}
		catch (InterruptedException e)
		{
			e.printStackTrace();
		}
		catch (ExecutionException e)
		{
			e.printStackTrace();
		}
		
		return response;
	}
	
	// Removes the given timer from the HashMap so that the ProcessCrashMonitor doesn't have to iterate over it.
	private void stopTimer(Integer messageID)
	{
		messages.remove(messageID);
	}

	public void clockTime(Message message, int port)
	{
		long currentTime = System.currentTimeMillis();
		long startTime = message.getStartTime();
		long difference = currentTime - startTime;
		
		message.setReturnTime(port, difference);
		
		if (difference > longestTimeout)
		{
			longestTimeout = difference;
		}
	}

	private void startTimer(Message message)
	{
		message.setStartTime(System.currentTimeMillis());
	}

	private void addMessageID(JSONObject payload) throws InterruptedException
	{
		mutex.acquire();
		payload.put("message_id", messageID++);
        mutex.release();
	}

	@Override
	public void shutdown()
	{
		listeningForResponses.set(false);
		orb.shutdown(false);
	}

	@Override
	public String createMRecord(String managerID, String firstName, String lastName, String employeeID, String mailID, Project[] projects, String location)
	{
		JSONObject payload = new JSONObject();
		
		payload.put(MessageKeys.MANAGER_ID, managerID);
		payload.put(MessageKeys.FIRST_NAME, firstName);
		payload.put(MessageKeys.LAST_NAME, lastName);
		payload.put(MessageKeys.EMPLOYEE_ID, employeeID);
		payload.put(MessageKeys.PROJECTS, projects);
		payload.put(MessageKeys.LOCATION, location);
		payload.put(MessageKeys.COMMAND_TYPE, Config.CREATE_MANAGER_RECORD);
		
		try
		{
			addMessageID(payload);
		}
		catch (InterruptedException e)
		{
			e.printStackTrace();
		}
		
		return makeRequest(payload);
	}

	@Override
	public String createERecord(String managerID, String firstName, String lastName, String employeeID, String mailID, String projectID)
	{
		JSONObject payload = new JSONObject();
		
		payload.put(MessageKeys.MANAGER_ID, managerID);
		payload.put(MessageKeys.FIRST_NAME, firstName);
		payload.put(MessageKeys.LAST_NAME, lastName);
		payload.put(MessageKeys.EMPLOYEE_ID, employeeID);
		payload.put(MessageKeys.MAIL_ID, mailID);
		payload.put(MessageKeys.PROJECT_ID, projectID);
		payload.put(MessageKeys.COMMAND_TYPE, Config.CREATE_EMPLOYEE_RECORD);
		
		try
		{
			addMessageID(payload);
		}
		catch (InterruptedException e)
		{
			e.printStackTrace();
		}
		
		return makeRequest(payload);
	}

	@Override
	public String getRecordCount(String managerID)
	{
		JSONObject payload = new JSONObject();
		
		payload.put(MessageKeys.MANAGER_ID, managerID);
		payload.put(MessageKeys.COMMAND_TYPE, Config.GET_RECORD_COUNT);
		
		try
		{
			addMessageID(payload);
		}
		catch (InterruptedException e)
		{
			e.printStackTrace();
		}
		
		return makeRequest(payload);
	}

	@Override
	public String editRecord(String managerID, String recordID, String fieldName, String newValue)
	{
		JSONObject payload = new JSONObject();
		
		payload.put(MessageKeys.MANAGER_ID, managerID);
		payload.put(MessageKeys.RECORD_ID, recordID);
		payload.put(MessageKeys.FIELD_NAME, fieldName);
		payload.put(MessageKeys.NEW_VALUE, newValue);
		payload.put(MessageKeys.COMMAND_TYPE, Config.EDIT_RECORD);
		
		try
		{
			addMessageID(payload);
		}
		catch (InterruptedException e)
		{
			e.printStackTrace();
		}
		
		return makeRequest(payload);
	}

	@Override
	public String transferRecord(String managerID, String recordID, String remoteCenterServerName)
	{
		JSONObject payload = new JSONObject();
		
		payload.put(MessageKeys.MANAGER_ID, managerID);
		payload.put(MessageKeys.RECORD_ID, recordID);
		payload.put(MessageKeys.REMOTE_SERVER_NAME, remoteCenterServerName);
		payload.put(MessageKeys.COMMAND_TYPE, Config.TRANSFER_RECORD);
		
		try
		{
			addMessageID(payload);
		}
		catch (InterruptedException e)
		{
			e.printStackTrace();
		}
		
		return makeRequest(payload);
	}
}
