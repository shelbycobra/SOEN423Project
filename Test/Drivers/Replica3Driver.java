package Test.Drivers;

import Replicas.Replica3.CenterServerController;

public class Replica3Driver {

    public static void main (String[] args) {
        CenterServerController servers = new CenterServerController();
        servers.runServers(0);
    }
}
