package Replicas.Replica3;

import java.util.HashMap;

import javax.xml.ws.Endpoint;

public class Publisher {

	public static final HashMap<String, Integer> CenterServerPortMap;
	static {
		CenterServerPortMap = new HashMap<String, Integer>();
		CenterServerPortMap.put("ca", 8000);
		CenterServerPortMap.put("us", 8001);
		CenterServerPortMap.put("uk", 8002);
	}

	public static void main(String[] args) {
		String location = args[0];
		int port = CenterServerPortMap.get(location);
		String address = String.format("http://localhost:%d/CenterServer/%s", port, location);
		Endpoint.publish(address, new CenterServer(location));

		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				Logger.logger.log("exiting");
				Logger.logger.close();
			}
		});
	}

}
