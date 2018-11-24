package DEMS;

public class Config {

	public static final String FRONT_END_HOST = "localhost";

	public static class Replica1 {
		public static final String HOST = "localhost";
		public static final int RM_PORT = 9000;
		public static final int CA_PORT = 5000;
		public static final int UK_PORT = 5001;
		public static final int US_PORT = 5002;
	}

	public static class Replica2 {
		public static final String HOST = "localhost";
		public static final int RM_PORT = 9001;
		public static final int CA_PORT = 6000;
		public static final int UK_PORT = 6001;
		public static final int US_PORT = 6002;
	}

	public static class Replica3 {
		public static final String HOST = "localhost";
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

	public static final int CREATE_MANAGER_RECORD = 1, CREATE_EMPLOYEE_RECORD = 2, GET_RECORD_COUNT = 3,
			EDIT_RECORD = 4, TRANSFER_RECORD = 5, EXIT = 6, ACK = 7, RECORD_EXISTS = 8;

	public enum StatusCode {
		SUCCESS,
		FAIL,
	}

	public enum Failure {
		BYZANTINE,
		PROCESS_CRASH,
	}

	public enum CommandType {
		CREATE_MANAGER_RECORD,
		CREATE_EMPLOYEE_RECORD,
		GET_RECORD_COUNT,
		EDIT_RECORD,
		TRANSFER_RECORD,
		EXIT,
		ACK,
		RECORD_EXISTS,
		RESTART_REPLICA,
		BAD_REPLICA_NUMBER,
		FAILED_REPLICA_RESTART_FAILED,
		FAILED_REPLICA_RESTARTED,
		GET_DATA,
		SET_DATA,
	}
}
