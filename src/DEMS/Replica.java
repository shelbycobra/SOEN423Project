package DEMS;

import org.json.simple.JSONArray;

public interface Replica {

    void runServers();

    void shutdownServers();

    JSONArray getData();

    void setData(JSONArray array);

}
