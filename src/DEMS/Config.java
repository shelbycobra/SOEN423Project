package DEMS;

public class Config {

	public static final String FRONT_END_HOST = "localhost";

	public static class Replica1 {
		public static final int RM_PORT = 9000;
		public static final int CA_PORT = 5000;
		public static final int UK_PORT = 5001;
		public static final int US_PORT = 5002;
	}

	public static class Replica2 {
		public static final int RM_PORT = 9001;
		public static final int CA_PORT = 6000;
		public static final int UK_PORT = 6001;
		public static final int US_PORT = 6002;
	}

	public static class Replica3 {
		public static final int RM_PORT = 9002;
		public static final int CA_PORT = 7000;
		public static final int UK_PORT = 7001;
		public static final int US_PORT = 7002;
	}

	public static class PortNumbers
	{
		public static final int FE_SEQ = 8000; // From FE to Sequencer
		public static final int SEQ_RE = 8001; // From Sequencer to Replica (Multicast)
		public static final int RE_FE = 8002; // From Replica to FE
		public static final int SEQ_FE = 8003; // From Sequencer to FE
	}
	
	public static class IPAddresses
	{
		public static final String REPLICA1 = "132.205.64.132";
		public static final String REPLICA2 = "132.205.64.137";
		public static final String REPLICA3 = "132.205.64.123";
		public static final String SEQUENCER = "132.205.64.132";
		public static final String FRONT_END = "132.205.64.137";
	}

	public enum StatusCode {
		SUCCESS,
		FAIL,
	}

	public enum Failure {
		NONE,
		BYZANTINE,
		PROCESS_CRASH,
	}

	public static final int MESSAGE_DELAY = 3;
	
	public static final String CREATE_MANAGER_RECORD = "create_manager_record", CREATE_EMPLOYEE_RECORD = "create_employee_record",
			GET_RECORD_COUNT = "get_record_count", EDIT_RECORD = "edit_record", TRANSFER_RECORD = "transfer_record",
			EXIT = "exit", ACK = "ack", RECORD_EXISTS = "record_exists", RESTART_REPLICA = "restart_replica",
			BAD_REPLICA_NUMBER = "bad_replica_number", FAILED_REPLICA_RESTART_FAILED = "failed_replica_restart_failed",
			FAILED_REPLICA_RESTARTED = "failed_replica_restarted", GET_DATA = "get_data", SET_DATA = "set_data";
}
