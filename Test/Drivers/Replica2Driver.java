package Test.Drivers;

import Replicas.Replica2.Server;

public class Replica2Driver {

    public static void main(String[] args) {

        Server server = new Server();
        server.runServers(0);
    }
}
