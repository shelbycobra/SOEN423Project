package Replicas.Replica1;
import Replicas.Replica1.CenterServer;

public class Replica1Driver {

    public static void main (String[] args){
        CenterServer centerServer = new CenterServer();
        centerServer.runServers(0);
        try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    
        centerServer.shutdownServers();
        try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
        centerServer.runServers(0);
    }

}
