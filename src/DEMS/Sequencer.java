package DEMS;

import com.sun.xml.internal.ws.policy.privateutil.PolicyUtils;

import java.io.IOException;
import java.net.*;
import java.sql.Statement;
import java.util.ArrayDeque;
import java.util.concurrent.Semaphore;

public class Sequencer {

    private  int sequenceNumber = 1;
    private  ArrayDeque<String> queue = new ArrayDeque<>();
    private  MulticastSocket multicastSocket;
    private  DatagramSocket datagramSocket;
    private  ListenForMessagesThread listenForMessages;
    private  InetAddress group;
    private  Semaphore mutex = new Semaphore(1);
    private Semaphore processMessageSem = new Semaphore(0);

    private  class ListenForMessagesThread extends Thread {

        public void run() {
            try {
                byte[] buffer = new byte[1000];
                DatagramPacket message = new DatagramPacket(buffer, buffer.length);

                while (true)
                {
                    System.out.println("\nListen: Listening for messages");
                    datagramSocket.receive(message);

                    mutex.acquire();
                    String data = new String(message.getData()).trim();

                    System.out.println("Listen: Received message " + data);

                    queue.add(data);
                    processMessageSem.release();
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

    public void startup () {
        try {
            setupSockets();
            listenForMessages  = new ListenForMessagesThread();
            listenForMessages.start();
            processMessage();
        } catch (IOException e) {
            e.printStackTrace();
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    private  void setupSockets() throws IOException{
        System.out.println("Setting up sockets\n");
        group = InetAddress.getByName("228.5.6.7");
        multicastSocket = new MulticastSocket(6789);
        multicastSocket.joinGroup(group);
        datagramSocket = new DatagramSocket(8000);
    }

    private  void processMessage() throws IOException, InterruptedException {
        System.out.println("Processing messages");
        while (true) {
            boolean resendPacket = true;
//            while (resendPacket) {

                // Wait until queue isn't empty
                System.out.println("Process: Wait until queue isn't empty");
                processMessageSem.acquire();

                //  Add sequence number to message and send
                System.out.println("\nProcess: Add sequence number to message and send");
                byte[] buffer = (sequenceNumber+":"+queue.peekFirst()).getBytes();
                DatagramPacket message = new DatagramPacket(buffer, buffer.length, group,6789);
                System.out.println(new String(buffer).trim());
                System.out.println();
                multicastSocket.send(message);
//
//                byte[] responseBuffer = new byte[3];
//                int numAcks = 0;
//                for (int i = 0; i < 4; i++) {
//                    DatagramPacket response = new DatagramPacket(responseBuffer, responseBuffer.length);
//                    multicastSocket.receive(response);
//
//                    if ((new String(response.getData())).equals("ACK"))
//                        numAcks++;
//                }
//
//                if (numAcks == 3) {
//                    mutex.acquire();
//                    if (!queue.isEmpty())
//                        queue.removeFirst();
//                    mutex.release();
                    sequenceNumber++;
//                    resendPacket = false;
//                    numAcks = 0;
//                } else {
//                    System.out.println("Not enough acks, must resend");
//                }
//            }
        }
    }
}
