package Test;

import org.junit.After;
import org.junit.Before;

import DEMS.ReplicaManager;

public class ReplicaManagerTest {

	ReplicaManager replicaManager1;
	ReplicaManager replicaManager2;
	ReplicaManager replicaManager3;

	@Before
	public void setup() {
		replicaManager1 = new ReplicaManager(1);
		replicaManager2 = new ReplicaManager(2);
		replicaManager3 = new ReplicaManager(3);

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
