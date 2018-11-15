package DEMS;

import java.io.IOException;
import java.net.*;
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
            byte[] buffer = new byte[1000];
            DatagramPacket message = new DatagramPacket(buffer, buffer.length);
            try {
                while (true) {
                    datagramSocket.receive(message);
                    mutex.acquire();
                    queue.add(new String(message.getData()).trim());
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

    public void startup () {

        System.out.println("Starting up sequencer\n");
        try {
            setupMulticastSocket();
            setupDatagramSocket();
            listenForMessages  = new ListenForMessagesThread();
            listenForMessages.start();
            processMessage();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    private static void setupMulticastSocket() throws IOException{
        group = InetAddress.getByName("228.5.6.7");
        multicastSocket = new MulticastSocket(6789);
        multicastSocket.joinGroup(group);
    }

    private static void setupDatagramSocket() throws IOException{
        InetAddress address = InetAddress.getByName("localhost");
        datagramSocket = new DatagramSocket(4000);
    }

    private static void processMessage() throws IOException, InterruptedException {
        System.out.println("Processing message");
        while (true) {
            boolean resendPacket = true;
            while (resendPacket) {
                // Wait until queue isn't empty
                while (!queue.isEmpty());

                //  Add sequence number to message and send
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
