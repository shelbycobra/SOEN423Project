package DEMS;

import com.sun.xml.internal.ws.policy.privateutil.PolicyUtils;

import java.io.IOException;
import java.net.*;
import java.sql.Statement;
import java.util.ArrayDeque;
import java.util.concurrent.Semaphore;

public class Sequencer {

    private static int sequenceNumber = 1;
    private static ArrayDeque<String> queue = new ArrayDeque<>();
    private static MulticastSocket multicastSocket;
    private static DatagramSocket datagramSocket;
    private static ListenForMessagesThread listenForMessages;
    private static InetAddress group;
    private static Semaphore mutex = new Semaphore(1);

    private static class ListenForMessagesThread extends Thread {

        public void run() {
            try {
                byte[] buffer = new byte[1000];
                DatagramPacket message = new DatagramPacket(buffer, buffer.length);

                while (true)
                {
                    System.out.println("Listening for messages");
                    datagramSocket.receive(message);
                    System.out.println("Received message");

                    mutex.acquire();
                    String data = new String(message.getData()).trim();
                    queue.add(data);
                    System.out.println("Queue size = " + queue.size());
                    mutex.release();
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public Sequencer() {

    }

//    public static void main (String[] args) {
//        System.out.println("Starting up sequencer\n");
//        try {
//            System.out.println("Setting up sockets\n");
//            group = InetAddress.getByName("228.5.6.7");
//            multicastSocket = new MulticastSocket(6789);
//            multicastSocket.joinGroup(group);
//            datagramSocket = new DatagramSocket(8000);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }

    public void startup () {
        try {
            setupSockets();
//            listenForMessages  = new ListenForMessagesThread();
//            listenForMessages.start();
            processMessage();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    private  void setupSockets() throws IOException{
        System.out.println("Setting up sockets\n");
        group = InetAddress.getByName("228.5.6.7");
        multicastSocket = new MulticastSocket(6789);
        multicastSocket.joinGroup(group);
        datagramSocket = new DatagramSocket(8888);

        byte[] buffer = "ACK".getBytes();
        DatagramPacket p = new DatagramPacket(buffer, buffer.length, InetAddress.getLocalHost(), 8000);

        datagramSocket.send(p);
    }

    private  void processMessage() throws IOException, InterruptedException {
        System.out.println("Processing messages");
        while (true) {
            boolean resendPacket = true;
            while (resendPacket) {

                // Wait until queue isn't empty
                System.out.println("Wait until queue isn't empty");
                while (queue.isEmpty());

                //  Add sequence number to message and send
                System.out.println("Add sequence number to message and send");
                byte[] buffer = (sequenceNumber+":"+queue.peekFirst()).getBytes();
                DatagramPacket message = new DatagramPacket(buffer, buffer.length, group,6789);
                multicastSocket.send(message);

                byte[] responseBuffer = new byte[3];
                int numAcks = 0;
                for (int i = 0; i < 4; i++) {
                    DatagramPacket response = new DatagramPacket(responseBuffer, responseBuffer.length);
                    multicastSocket.receive(response);

                    if ((new String(response.getData())).equals("ACK"))
                        numAcks++;
                }

                if (numAcks == 3) {
                    mutex.acquire();
                    if (!queue.isEmpty())
                        queue.removeFirst();
                    mutex.release();
                    sequenceNumber++;
                    resendPacket = false;
                    numAcks = 0;
                } else {
                    System.out.println("Not enough acks, must resend");
                }
            }
        }
    }
}
