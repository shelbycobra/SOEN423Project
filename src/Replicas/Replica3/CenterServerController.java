package Replicas.Replica3;

public class CenterServerController {

	Thread centerServerCA;
	Thread centerServerUS;
	Thread centerServerUK;

	public CenterServerController() {
		centerServerCA = new Thread(new CenterServer("CA"));
		centerServerUS = new Thread(new CenterServer("US"));
		centerServerUK = new Thread(new CenterServer("UK"));
	}

	public void runServers() {
		centerServerCA.start();
		centerServerUS.start();
		centerServerUK.start();
	}

	public void shutdownServers() {
		centerServerCA.interrupt();
		centerServerUS.interrupt();
		centerServerUK.interrupt();
	}
}
