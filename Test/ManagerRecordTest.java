package Test;

import Replicas.Replica1.DataStructures.ManagerRecord;
import Replicas.Replica1.DataStructures.Project;
import org.junit.Assert;
import org.junit.Before;

public class ManagerRecordTest {

    private ManagerRecord mRecord;
    @Before
    public void setup() {
        Project[] p1 = {new Project("P12356", "PClient", "PName")};
        mRecord = new ManagerRecord("John","Smith", 123, "john@mail.com", p1, "CA" );

    }

    @org.junit.Test
    public void setLocation() {
        mRecord.setLocation("UK");
        Assert.assertEquals(mRecord.getLocation(), "UK");
    }

    @org.junit.Test
    public void getProject() {
        Project p = mRecord.getProject(0);
        Assert.assertEquals(p.getProjectID(), "P12356");
        Assert.assertEquals(p.getProjectClient(), "PClient");
        Assert.assertEquals(p.getProjectName(), "PName");
    }

    @org.junit.Test
    public void getProjects() {
        Project[] projs = mRecord.getProjects();
        Project p = projs[0];
        Assert.assertEquals(p.getProjectID(), "P12356");
        Assert.assertEquals(p.getProjectClient(), "PClient");
        Assert.assertEquals(p.getProjectName(), "PName");
    }

    @org.junit.Test
    public void getData() {
        String wrong_data = "John:Smith:123:john@mail.com:P12356:PClient:PName:UK";
        String right_data = "John:Smith:123:john@mail.com:P12356:PClient:PName:CA";

        Assert.assertNotEquals(mRecord.getData(), wrong_data);
        Assert.assertEquals(mRecord.getData(), right_data);
    }
}
