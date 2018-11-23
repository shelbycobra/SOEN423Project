package Test;

import org.junit.Before;

import DEMS.ReplicaManager;

public class ReplicaManagerTest {

	ReplicaManager replicaManager;

	@Before
	public void setup() {
		replicaManager = new ReplicaManager(1);
		replicaManager.start();
	}

}
