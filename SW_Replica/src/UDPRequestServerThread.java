import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class UDPRequestServerThread extends Thread{

    private DatagramPacket packet;
    private DatagramSocket aSocket;
    private int port;
    private byte[] buffer;
    private String message;
    
    public UDPRequestServerThread(DatagramSocket aSocket, int port, byte msg){
        this.aSocket = aSocket;
        this.port = port;
        buffer = new byte[256];
        buffer[0] = msg;
    }
    
    public void run() {
        InetAddress address;
        try {
            address = InetAddress.getByName("localhost");
            packet = new DatagramPacket(buffer, buffer.length, address, port + 1);
            aSocket.send(packet);
            packet = new DatagramPacket(buffer, buffer.length);
            aSocket.receive(packet);
            message = new String (packet.getData(), 0, packet.getLength());
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public String getMessage(){
        return message;
    }
}
