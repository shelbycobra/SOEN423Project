package Replicas.Replica1;

import java.util.Comparator;

public class MessageComparator implements Comparator<String> {

    @Override
    public int compare(String string, String t1) {

        // Extract sequence numbers
        int seqNum1 = Integer.parseInt(string.split(":")[0]);
        int seqNum2 = Integer.parseInt(t1.split(":")[0]);


        // Compare sequence numbers
        // if o1 < o2, value will be < 0;
        // if o1 == o2, value will be == 0;
        // if o1 > o2, value will be > 0;
        return seqNum1 - seqNum2;
    }
}
