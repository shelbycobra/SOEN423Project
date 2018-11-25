package DEMS;

import org.json.simple.JSONArray;

public interface Replica {

    void runServers(int i);

    void shutdownServers();

    /*
    Returns JSONArray in the format of
    [ { Record 1 }, { Record 2 }, ... , { Record N } ]
     */
    JSONArray getData();

    void setData(JSONArray array);

}
