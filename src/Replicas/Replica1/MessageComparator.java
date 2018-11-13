package Replicas.Replica1;

import java.util.ArrayList;
import java.util.Comparator;

public class MessageComparator implements Comparator<ArrayList<String>> {

    @Override
    public int compare(ArrayList<String> strings, ArrayList<String> t1) {

        // Extract sequence numbers
        int obj1SeqNum = Integer.parseInt(strings.get(0));
        int obj2SeqNum = Integer.parseInt(t1.get(0));

        // Compare sequence numbers
        // if o1 < o2, value will be < 0;
        // if o1 == o2, value will be == 0;
        // if o1 > o2, value will be > 0;
        return obj1SeqNum - obj2SeqNum;
    }
}
