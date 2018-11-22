package Test.Drivers;

import Replicas.Replica1.CenterServer;

public class Replica1Driver {

    public static void main (String[] args){
        CenterServer centerServer = new CenterServer();
        centerServer.runServers();
    }

}
