package Test;

import Replicas.Replica1.DataStructures.EmployeeRecord;
import org.junit.Assert;
import org.junit.Before;

public class EmployeeRecordTest {

    private EmployeeRecord eRecord;

    @Before
    public void setup() {
        eRecord = new EmployeeRecord("John", "Smith", 123, "john@mail.com", "P12345");
    }

    @org.junit.Test
    public void setProjectID() {
        eRecord.setProjectID("P99999");
        Assert.assertEquals(eRecord.getProjectID(), "P99999");
    }

    @org.junit.Test
    public void getProjectID() {
        Assert.assertEquals(eRecord.getProjectID(), "P12345");
    }

    @org.junit.Test
    public void getData() {
        String wrong_data = "John:Smith:123:john@mail.com:P99999";
        String right_data = "John:Smith:123:john@mail.com:P12345";

        Assert.assertNotEquals(eRecord.getData(), wrong_data);
        Assert.assertEquals(eRecord.getData(), right_data);
    }
}
