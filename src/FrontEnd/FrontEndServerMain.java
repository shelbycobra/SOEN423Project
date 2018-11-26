package FrontEnd;

import static DEMS.Config.*;
import DEMS.MessageKeys;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.URL;;

public class FrontEndServerMain
{
	public static JSONParser parser = new JSONParser();

	public static void main(String[] args)
	{
		getIPAddresses();

//		new FrontEndServer(new String[]{args[0], args[1], args[2], args[3]}, args[4]);
		
		FrontEndServerThread frontEndServerThread = new FrontEndServerThread(new String[]{args[0], args[1], args[2], args[3]});
		Thread thread = new Thread(frontEndServerThread);
		
		thread.start();
//		try
//		{
//			Thread.sleep(2000);
//			
//		} catch (InterruptedException e)
//		{
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		
//		frontEndServerThread.shutdown();
	}

	public static void getIPAddresses() {

		try {
			InetAddress group = InetAddress.getByName(MULTICAST_SOCKET);
			MulticastSocket socket = new MulticastSocket(MULTICAST_PORT);
			socket.joinGroup(group);

			IPAddresses.FRONT_END = getIPFromURL();
			System.out.println("FE = " + IPAddresses.FRONT_END);

			JSONObject obj = new JSONObject();
			obj.put(MessageKeys.COMMAND_TYPE, IP_ADDRESS_REQUEST);
			obj.put(MessageKeys.IP_ADDRESS, IPAddresses.FRONT_END);

			byte[] buffer = obj.toString().getBytes();
			DatagramPacket packet = new DatagramPacket(buffer, buffer.length, group, MULTICAST_PORT);

			socket.send(packet);

			for (int i = 0; i < 4; i++) {
				buffer = new byte[1000];

				DatagramPacket receive = new DatagramPacket(buffer, buffer.length);
				socket.receive(receive);

				JSONObject message = (JSONObject) parser.parse(new String(receive.getData()).trim());

				if (message.get(MessageKeys.COMMAND_TYPE).equals(IP_ADDRESS_RESPONSE)) {
					System.out.println("REPLICA NUMBER = " + message.get(MessageKeys.REPLICA_NUMBER));
					if (message.get(MessageKeys.REPLICA_NUMBER) != null) {
						int RMNum = Integer.parseInt(message.get(MessageKeys.REPLICA_NUMBER).toString());
						if (RMNum == 1) {
							IPAddresses.REPLICA1 = message.get(MessageKeys.IP_ADDRESS).toString();
							System.out.println("REPLICA 1 = " + IPAddresses.REPLICA1);
						}
						else if (RMNum == 2) {
							IPAddresses.REPLICA2 = message.get(MessageKeys.IP_ADDRESS).toString();
							System.out.println("REPLICA 2 = " + IPAddresses.REPLICA2);
						}
						else if (RMNum == 3) {
							IPAddresses.REPLICA3 = message.get(MessageKeys.IP_ADDRESS).toString();
							System.out.println("REPLICA 3 = " + IPAddresses.REPLICA3);
						}
					} else {
						IPAddresses.SEQUENCER = message.get(MessageKeys.IP_ADDRESS).toString();
						System.out.println("SEQUENCER = " + IPAddresses.SEQUENCER);
					}

				} else {
					System.out.println("Invalid Message Type");
				}
			}

			socket.leaveGroup(group);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		}
	}

	private static String getIPFromURL() {
		String systemipaddress = "";
		try
		{
			URL url_name = new URL("http://bot.whatismyipaddress.com");

			BufferedReader sc =
					new BufferedReader(new InputStreamReader(url_name.openStream()));

			// reads system IPAddress
			systemipaddress = sc.readLine().trim();
			sc.close();
		}
		catch (Exception e)
		{
			systemipaddress = "Cannot Execute Properly";
		}
		System.out.println("Public IP Address: " + systemipaddress +"\n");
		return systemipaddress;
	}
}
