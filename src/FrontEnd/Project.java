package FrontEnd;


/**
* FrontEnd/Project.java .
* Generated by the IDL-to-Java compiler (portable), version "3.2"
* from DEMS.idl
* Wednesday, November 21, 2018 12:45:18 PM EST
*/

public final class Project implements org.omg.CORBA.portable.IDLEntity
{
  public String projectID = null;
  public String clientName = null;
  public String projectName = null;

  public Project ()
  {
  } // ctor

  public Project (String _projectID, String _clientName, String _projectName)
  {
    projectID = _projectID;
    clientName = _clientName;
    projectName = _projectName;
  } // ctor

} // class Project
