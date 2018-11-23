import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import DEMS.MessageKeys;
import DEMS.UDPPortNumbers;

public class MockSystem
{
	public static void main(String[] args)
	{
		MockSequencer sequencer = new MockSystem().new MockSequencer();
		MockReplicaManager replicaManager = new MockSystem().new MockReplicaManager();
		Thread sequencerThread = new Thread(sequencer);
		Thread replicaManagerThread = new Thread(replicaManager);
		
		sequencerThread.start();
		replicaManagerThread.start();
	}
	
	public class MockSequencer implements Runnable
	{
		private JSONParser parser = new JSONParser();
		DatagramSocket datagramSocket;
		private final AtomicBoolean running = new AtomicBoolean(true);
		
		public void shutdown()
		{
			running.set(false);
		}
		
		@Override
		public void run()
		{
			System.out.println("Mock Sequencer up and running on port " + UDPPortNumbers.FE_SEQ);
			try
			{
				datagramSocket = new DatagramSocket(UDPPortNumbers.FE_SEQ);
				datagramSocket.setSoTimeout(1000);
				
                while (running.get())
                {
                	try
                	{
	                    byte[] buffer = new byte[1000];
	                    DatagramPacket message = new DatagramPacket(buffer, buffer.length);
	                    
						datagramSocket.receive(message);
	                    String data = new String(message.getData()).trim();
	                    System.out.println("Sequencer: Received a request from the Front End.");
	                    JSONObject jsonMessage;
	                    jsonMessage = (JSONObject) parser.parse(data);
	
	                    byte[] messageBuffer = jsonMessage.toString().getBytes();
	    				InetAddress host = InetAddress.getByName("localhost");
	    				DatagramPacket request = new DatagramPacket(messageBuffer, messageBuffer.length, host, UDPPortNumbers.SEQ_RM);
	    				datagramSocket.send(request);
	    				System.out.println("Sequencer: Sending request to Replica Manager...");
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
			}
		}
	}
	
	public class MockReplicaManager implements Runnable
	{
		private JSONParser parser = new JSONParser();
		DatagramSocket datagramSocket;
		private final AtomicBoolean running = new AtomicBoolean(true);
		
		public void shutdown()
		{
			running.set(false);
		}
		
		@Override
		public void run()
		{
			System.out.println("Mock Replica Manager up and running on port " + UDPPortNumbers.SEQ_RM);
			
			try
			{
				datagramSocket = new DatagramSocket(UDPPortNumbers.SEQ_RM);
				datagramSocket.setSoTimeout(1000);
				
                while (running.get())
                {
                	try
                	{
	                    byte[] buffer = new byte[1000];
	                    DatagramPacket message = new DatagramPacket(buffer, buffer.length);
	                    
						datagramSocket.receive(message);
						System.out.println("Replica Manager: Received a request from the Sequencer.");
	                    String data = new String(message.getData()).trim();
	                    JSONObject jsonMessage = (JSONObject) parser.parse(data);
	
	                    jsonMessage.put(MessageKeys.MESSAGE, "Successfully received client request ID: " + jsonMessage.get(MessageKeys.MESSAGE_ID).toString());
	                    
	                    byte[] messageBuffer = jsonMessage.toString().getBytes();
	    				InetAddress host = InetAddress.getByName("localhost");
	    				DatagramPacket request = new DatagramPacket(messageBuffer, messageBuffer.length, host, UDPPortNumbers.RM_FE);
	    				datagramSocket.send(request);
	    				System.out.println("Replica Manager: Sending response to Front End...");
	    				
	    				Random random = new Random();
	    				if (random.nextFloat() > 0.5)
	    				{
		    				// Sending "bad" second message
		    				JSONObject jsonMessage2 = new JSONObject();
		    				jsonMessage2.put(MessageKeys.MESSAGE_ID, jsonMessage.get(MessageKeys.MESSAGE_ID));
		                    jsonMessage2.put(MessageKeys.MESSAGE, "Bad message!");
		                    byte[] messageBuffer2 = jsonMessage2.toString().getBytes();
		    				DatagramPacket request2 = new DatagramPacket(messageBuffer2, messageBuffer2.length, host, UDPPortNumbers.RM_FE);
		    				datagramSocket.send(request2);
		    				System.out.println("Replica Manager: Sending response to Front End...");
	    				}
	    				
	    				datagramSocket.send(request);
	    				System.out.println("Replica Manager: Sending response to Front End...");
                	}
                	catch (ParseException | IOException e)
        			{
                        continue;
                    }
                }
            } catch (SocketException e1)
			{
				e1.printStackTrace();
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
}
