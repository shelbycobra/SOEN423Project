package Test;

import Replicas.Replica1.CenterServer;
import Replicas.Replica2.Server;
import Replicas.Replica3.CenterServerController;
import org.junit.After;
import org.junit.Before;

import DEMS.ReplicaManager;

public class ReplicaManagerTest {

	ReplicaManager replicaManager1;
	ReplicaManager replicaManager2;
	ReplicaManager replicaManager3;

	@Before
	public void setup() {
		CenterServer replica1 = new CenterServer();
		Server replica2 = new Server();
		CenterServerController replica3 = new CenterServerController();

		try {
			replicaManager1 = new ReplicaManager(replica1, 1);
			replicaManager2 = new ReplicaManager(replica2, 2);
			replicaManager3 = new ReplicaManager(replica3, 3);

		} catch (Exception e) {
			e.printStackTrace();
		}
		replicaManager1.start();
		replicaManager2.start();
		replicaManager3.start();
	}

	@After
	public void shutdown() {
		replicaManager1.stop();
		replicaManager2.stop();
		replicaManager3.stop();
	}

}
