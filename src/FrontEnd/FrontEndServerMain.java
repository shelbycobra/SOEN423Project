package FrontEnd;

public class FrontEndServerMain
{
	public static void main(String[] args)
	{
//		new FrontEndServer(new String[]{args[0], args[1], args[2], args[3]}, args[4]);
		
		FrontEndServerThread frontEndServerThread = new FrontEndServerThread(new String[]{args[0], args[1], args[2], args[3]});
		Thread thread = new Thread(frontEndServerThread);
		
		thread.start();
//		try
//		{
//			Thread.sleep(2000);
//			
//		} catch (InterruptedException e)
//		{
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		
//		frontEndServerThread.shutdown();
	}
}
