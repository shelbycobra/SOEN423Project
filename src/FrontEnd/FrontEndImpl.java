package FrontEnd;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.Semaphore;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.omg.CORBA.*;

import DEMS.Command;
import DEMS.MessageKeys;
import DEMS.UDPPortNumbers;

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
	private Semaphore sendToSequencer = new Semaphore(0);
	private Semaphore receiveFromReplica = new Semaphore(-1);
	HashMap<Integer, ArrayList<String>> replicaResponses = new HashMap<>();
	private JSONParser parser = new JSONParser();
	private int messageID = 1;
	
	public FrontEndImpl()
	{
		ReplicaResponseListener responseListener = new ReplicaResponseListener();
		Thread responseListenerThread = new Thread(responseListener);
		
		responseListenerThread.start();
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
	 * 5. As ReplicaResponseListener receives responses, WaitForReplicaResponse processes them bases on message ID.
	 * */
	
	private class SendToSequencer implements Callable<String>
	{
		JSONObject payload;
		
		public SendToSequencer(JSONObject payload)
		{
			this.payload = payload;
		}
		
		public String call()
		{
			DatagramSocket socket = null;
			
			try
			{
				sendToSequencer.acquire();
				
				socket = new DatagramSocket();
				byte[] messageBuffer = payload.toString().getBytes();
				InetAddress host = InetAddress.getByName("localhost");
				DatagramPacket request = new DatagramPacket(messageBuffer, messageBuffer.length, host, UDPPortNumbers.FE_SEQ);
				
				socket.send(request);
			
				byte[] buffer = new byte[1000];
				DatagramPacket reply = new DatagramPacket(buffer, buffer.length);
				socket.receive(reply);
				String replyString = new String(reply.getData(), reply.getOffset(), reply.getLength());
				JSONObject jsonMessage = (JSONObject) parser.parse(replyString);
			}
			catch (SocketTimeoutException | ParseException e)
			{
				System.out.println("Server on port " + UDPPortNumbers.FE_SEQ + " is not responding.");
			}
			catch (SocketException e)
			{
				System.out.println("Socket: " + e.getMessage());
			}
			catch (IOException e)
			{
				System.out.println("IO: " + e.getMessage());
			}
			catch (InterruptedException e)
			{
				e.printStackTrace();
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
			Callable<String> waitForResponseCall = new WaitForReplicaResponse(payload.get(MessageKeys.MESSAGE_ID).toString());
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
		Integer messageID;
		
		public WaitForReplicaResponse(String messageID)
		{
			this.messageID = Integer.parseInt(messageID);
		}
		
        public String call()
        {
        	String response = null, message1 = null, message2 = null, message3 = null;

        	while (true)
            {
        		try
				{
					receiveFromReplica.acquire();
				}
        		catch (InterruptedException e)
				{
					e.printStackTrace();
				}
                
        		if (!replicaResponses.containsKey(messageID))
        		{
        			receiveFromReplica.release();
        			continue;
        		}
        		
        		if (replicaResponses.get(messageID).isEmpty())
        		{            			
        			message1 = replicaResponses.get(messageID).get(0);
        		}
        		else if (replicaResponses.get(messageID).size() == 1)
        		{
        			message2 = replicaResponses.get(messageID).get(1);
        			
        			if (message1.equals(message2))
        			{
        				response = message1;
        				break;
        			}
        		}
        		else
        		{
        			message3 = replicaResponses.get(messageID).get(2);
        			
        			if (message1.equals(message3))
        			{
        				response = message1;
        				break;
        			}
        			else if (message2.equals(message3))
        			{
        				response = message2;
        				break;
        			}
        			else
        			{
        				sendErrorToReplicaManager();
					}
				}
            }
        	
        	return response;
        }
        
        private void sendErrorToReplicaManager()
        {
        	
        }
	}
	
	private class ReplicaResponseListener extends Thread
	{
		@Override
		public void run()
		{
			DatagramSocket datagramSocket = null;
			
			System.out.println("Listening for responses from the replicas on port " + UDPPortNumbers.RM_FE + "...");
			
			try
            {	
            	while (true)
                {
            		datagramSocket = new DatagramSocket(UDPPortNumbers.RM_FE);
                    byte[] buffer = new byte[1000];
                    DatagramPacket responseMessage = new DatagramPacket(buffer, buffer.length);
                    datagramSocket.receive(responseMessage);
                    String data = new String(responseMessage.getData()).trim();
                    JSONObject jsonMessage = (JSONObject) parser.parse(data);
                    Integer messageID = Integer.parseInt(jsonMessage.get(MessageKeys.MESSAGE_ID).toString());
                    String message = jsonMessage.get(MessageKeys.MESSAGE).toString();
                    
                    if (!replicaResponses.containsKey(messageID))
                    {
                    	replicaResponses.put(messageID, new ArrayList<String>());
                    }
                    
                    replicaResponses.get(messageID).add(message);
                    
                    receiveFromReplica.release();
                }
            }
            catch (ParseException | IOException e)
            {
                e.printStackTrace();
            }
            finally
			{
				if (datagramSocket != null)
				{
					datagramSocket.close();
				}
			}
		}
	}
	
	private String makeRequest(JSONObject payload)
	{
		String response = null;
		Callable<String> sendToSequencerCall = new SendToSequencer(payload);
		FutureTask<String> sendToSequencerTask = new FutureTask<>(sendToSequencerCall);
		Thread sendToSequencerThread = new Thread(sendToSequencerTask);
		
		sendToSequencerThread.start();
		
		try
		{
			response = sendToSequencerTask.get();
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
	
	private void addMessageID(JSONObject payload) throws InterruptedException
	{
		mutex.acquire();
		payload.put("message_id", messageID++);
        mutex.release();
	}

	@Override
	public void shutdown()
	{
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
		payload.put(MessageKeys.COMMAND_TYPE, Command.CREATE_MANAGER_RECORD);
		
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
		payload.put(MessageKeys.COMMAND_TYPE, Command.CREATE_EMPLOYEE_RECORD);
		
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
		payload.put(MessageKeys.COMMAND_TYPE, Command.GET_RECORD_COUNT);
		
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
		payload.put(MessageKeys.COMMAND_TYPE, Command.EDIT_RECORD);
		
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
		payload.put(MessageKeys.COMMAND_TYPE, Command.TRANSFER_RECORD);
		
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
