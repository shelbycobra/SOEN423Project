package FrontEnd;


/**
* FrontEnd/ProjectsHolder.java .
* Generated by the IDL-to-Java compiler (portable), version "3.2"
* from DEMS.idl
* Wednesday, November 21, 2018 12:45:18 PM EST
*/

public final class ProjectsHolder implements org.omg.CORBA.portable.Streamable
{
  public FrontEnd.Project value[] = null;

  public ProjectsHolder ()
  {
  }

  public ProjectsHolder (FrontEnd.Project[] initialValue)
  {
    value = initialValue;
  }

  public void _read (org.omg.CORBA.portable.InputStream i)
  {
    value = FrontEnd.ProjectsHelper.read (i);
  }

  public void _write (org.omg.CORBA.portable.OutputStream o)
  {
    FrontEnd.ProjectsHelper.write (o, value);
  }

  public org.omg.CORBA.TypeCode _type ()
  {
    return FrontEnd.ProjectsHelper.type ();
  }

}
