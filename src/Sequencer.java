import java.io.IOException;
import java.net.*;
import java.util.ArrayDeque;
import java.util.concurrent.Semaphore;

public class Sequencer {

    private static int sequenceNumber = 1;
    private static ArrayDeque<String> queue;
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
                    queue.add(new String(message.getData()).trim());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public Sequencer() {
        queue = new ArrayDeque<>();
        sequenceNumber = 0;
    }

    public static void main(String[] args) {

        try {
            setupMulticastSocket();
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
        while (true) {
            boolean resendPacket = true;
            while (resendPacket) {
                byte[] buffer = (sequenceNumber+":"+queue.peekFirst()).getBytes();
                DatagramPacket message = new DatagramPacket(buffer, buffer.length, group,6789);
                multicastSocket.send(message);

                byte[] responseBuffer = new byte[10];
                for (int i = 0; i < 3; i++) {
                    DatagramPacket response = new DatagramPacket(responseBuffer, responseBuffer.length);
                    multicastSocket.receive(response);
                    mutex.acquire();
                    if ((new String(response.getData())).equals("ACK")){
                        if (!queue.isEmpty())
                            queue.removeFirst();
                        sequenceNumber++;
                        resendPacket = false;
                    }
                    mutex.release();
                }
            }
        }
    }
}
