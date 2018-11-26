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
		
		if (code.equals(StatusCode.SUCCESS.toString()))
		{
			this.code = StatusCode.SUCCESS;
		}
		else
		{
			this.code = StatusCode.FAIL;
		}
	}
}
