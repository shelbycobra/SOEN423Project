package FrontEnd;

import DEMS.Config.StatusCode;

public class ReturnMessage
{
	int port;
	String message;
	StatusCode code;
	
	public ReturnMessage(int port, String message, String code)
	{
		this.port = port;
		this.message = message;
		this.code = code;
	}
}
