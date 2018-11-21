package Replicas.Replica2;

import org.json.simple.JSONObject;

import java.util.Comparator;

public class MessageComparator implements Comparator<JSONObject>
{
    @Override
    public int compare(JSONObject string, JSONObject t1)
    {
        // Extract sequence numbers
        int seqNum1 = Integer.parseInt( (String) string.get("sequenceNumber"));
        int seqNum2 = Integer.parseInt( (String) t1.get("sequenceNumber"));

        // Compare sequence numbers
        // if o1 < o2, value will be -1;
        // if o1 == o2, value will be == 0;
        // if o1 > o2, value will be 1;

        return Integer.compare(seqNum1, seqNum2);
    }
}
