package DEMS;

public class Config {

	public static class Replica1 {
		public static final String host = "localhost";
		public static final int rmPort = 9000;
		public static final int caPort = 3500;
		public static final int ukPort = 4500;
		public static final int usPort = 5500;
	}

	public static class Replica2 {
		public static final String host = "localhost";
		public static final int rmPort = 9001;
		public static final int caPort = 6000;
		public static final int ukPort = 6001;
		public static final int usPort = 6002;
	}

	public static class Replica3 {
		public static final String host = "localhost";
		public static final int rmPort = 9002;
		public static final int caPort = 7000;
		public static final int ukPort = 7001;
		public static final int usPort = 7002;
	}

}
