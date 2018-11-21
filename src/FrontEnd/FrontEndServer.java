package FrontEnd;

import org.omg.CORBA.*;
import org.omg.CosNaming.*;
import org.omg.PortableServer.*;

public class FrontEndServer
{
	FrontEndImpl frontEndImpl;
	String orbArguments[];
	String location;
	
	public FrontEndServer(String orbArguments[], String location)
	{
		this.orbArguments = orbArguments;
		this.location = location;
		
		if (!location.matches("CA") && !location.matches("US") && !location.matches("UK"))
		{
			System.err.println("Invalid Location. Please choose CA, US, or UK.");
			System.exit(1);
		}
		
		try
		{
			// create and initialize the ORB
			ORB orb = ORB.init(orbArguments, null);
			
			// get reference to rootpoa & activate the POAManager (It allows an object implementation to function with different ORBs, hence the word portable)
			POA rootpoa = POAHelper.narrow(orb.resolve_initial_references("RootPOA"));
			rootpoa.the_POAManager().activate();
			
			// create servant and register it with the ORB
			frontEndImpl = new FrontEndImpl();
			frontEndImpl.setORB(orb);
			
			// get object reference from the servant
			org.omg.CORBA.Object ref = rootpoa.servant_to_reference(frontEndImpl);
			FrontEndInterface href = FrontEndInterfaceHelper.narrow(ref);
			
			// get the root naming context
			// NameService invokes the name service
			org.omg.CORBA.Object objRef = orb.resolve_initial_references("NameService");
			
			// Use NamingContextExt which is part of the Interoperable
			// Naming Service (INS) specification.
			NamingContextExt ncRef = NamingContextExtHelper.narrow(objRef);
			
			// bind the Object Reference in Naming
			NameComponent path[] = ncRef.to_name(location);
			ncRef.rebind(path, href);
			System.out.println("DEMS Server ready and waiting...");
			
			// wait for invocations from clients
			orb.run();
		}
		catch (Exception e)
		{
			System.err.println("ERROR: " + e);
			e.printStackTrace(System.out);
		}
		
		System.out.println("DEMS Server Exiting...");
	}
	
	public void shutdown()
	{
		frontEndImpl.shutdown();
	}
}
