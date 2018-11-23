package Test;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicBoolean;

import DEMS.Config;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import Client.ClientThread;
import DEMS.MessageKeys;
import FrontEnd.FrontEndServerThread;

import static org.junit.Assert.assertEquals;

public class FrontEndTest
{
	FrontEndServerThread frontEndServer;
	String[] orbArguments = new String[]{"-ORBInitialPort", "1050", "-ORBInitialHost", "localhost"};
	MockSequencer sequencer;
	MockReplicaManager replicaManager;
	
	@Before
	public void setup()
	{
		System.out.println("Setting up?");
//		frontEndServer = new FrontEndServerThread(orbArguments);
//		sequencer = new MockSequencer();
//		replicaManager = new MockReplicaManager();
//		
//		Thread frontEndServerThread = new Thread(frontEndServer);
//		Thread sequencerThread = new Thread(sequencer);
//		Thread replicaManagerThread = new Thread(replicaManager);
//		
//		frontEndServerThread.start();
//		sequencerThread.start();
//		replicaManagerThread.start();
	}
	
	@Test
	public void test1()
	{
		frontEndServer = new FrontEndServerThread(orbArguments);
		sequencer = new MockSequencer();
		replicaManager = new MockReplicaManager();
		
		Thread frontEndServerThread = new Thread(frontEndServer);
		Thread sequencerThread = new Thread(sequencer);
		Thread replicaManagerThread = new Thread(replicaManager);
		
		frontEndServerThread.start();
		
		try
		{
			Thread.sleep(2000);
			
		} catch (InterruptedException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		sequencerThread.start();
		replicaManagerThread.start();
		
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
		
		assertEquals(10, responses.size());
		
		frontEndServer.shutdown();
		sequencer.shutdown();
		replicaManager.shutdown();
	}
	
	@After
	public void teardown()
	{
//		frontEndServer.shutdown();
//		sequencer.shutdown();
//		replicaManager.shutdown();
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
			System.out.println("Mock Sequencer up and running on port " + Config.PortNumbers.FE_SEQ);
			try
			{
				datagramSocket = new DatagramSocket(Config.PortNumbers.FE_SEQ);
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
	    				DatagramPacket request = new DatagramPacket(messageBuffer, messageBuffer.length, host, Config.PortNumbers.SEQ_RE);
	    				datagramSocket.send(request);
	    				System.out.println("Sequencer: Sending request to Replica Manager...");
                	}
                	catch (ParseException | IOException e)
        			{
                        e.printStackTrace();
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
			System.out.println("Mock Replica Manager up and running on port " + Config.PortNumbers.SEQ_RE);
			
			try
			{
				datagramSocket = new DatagramSocket(Config.PortNumbers.SEQ_RE);
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
	    				DatagramPacket request = new DatagramPacket(messageBuffer, messageBuffer.length, host, Config.PortNumbers.RE_FE);
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
		    				DatagramPacket request2 = new DatagramPacket(messageBuffer2, messageBuffer2.length, host, Config.PortNumbers.RE_FE);
		    				datagramSocket.send(request2);
		    				System.out.println("Replica Manager: Sending response to Front End...");
	    				}
	    				
	    				datagramSocket.send(request);
	    				System.out.println("Replica Manager: Sending response to Front End...");
                	}
                	catch (ParseException | IOException e)
        			{
                        e.printStackTrace();
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
