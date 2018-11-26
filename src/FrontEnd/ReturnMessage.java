package FrontEnd;

import DEMS.Config.StatusCode;

public class ReturnMessage
{
	int port;
	String message;
	StatusCode code;
	
	public ReturnMessage(int port, String message, int code)
	{
		this.port = port;
		this.message = message;
		
		switch (code)
		{
			case 0: this.code = StatusCode.SUCCESS;
			break;
			case 1: this.code = StatusCode.FAIL;
		}
	}
}
