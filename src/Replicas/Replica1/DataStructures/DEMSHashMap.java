package Replicas.Replica1.DataStructures;

import DEMS.MessageKeys;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class DEMSHashMap {

    private class HashNode {
		
		private int key;
		private String recordID;
		private Record record;
		private HashNode next;
		
		HashNode(char key, String recordID, Record record, HashNode link) {
			this.key = key - 65;
			this.recordID = recordID;
			this.record = record;
			this.next = link;
		}
		
		void setNext(HashNode link) {this.next = link;}
		public void setRecord(Record record) {this.record = record;}
		int getKey() {return key;}
		public Record getRecord() {return record;}
		HashNode getNext() {return next;}
		String getRecordID() {return recordID;}
	}
	
	private HashNode map[] = new HashNode[26];
	private int recordCount = 0;
	private int ID_count = 0;
	private String location;

	public DEMSHashMap(String loc) {
	    location = loc;
    }
	
	public synchronized String addRecord(Record record, String recordType) {
		HashNode newNode = new HashNode(record.getLastName().toUpperCase().charAt(0),
                recordType + createRecordIDString("" + ID_count), record, null);
		return addHashNode(newNode);
	}
	
	public synchronized String addRecord(String recordID, Record record) {
		HashNode newNode = new HashNode(record.getLastName().toUpperCase().charAt(0), recordID, record, null);
		return addHashNode(newNode);
	}

	private synchronized String addHashNode(HashNode newNode) {
        int key = newNode.getKey();
        if (map[key] == null)
            map[key] = newNode;
        else {
            HashNode node = map[key];
            while (node.getNext() != null)
                node = node.getNext();
            node.setNext(newNode);
        }

        ID_count++;
        recordCount++;
        return newNode.getRecordID();

    }
	public synchronized Record getRecord(String recordID) {
            for (HashNode node : map) {
                while (node != null) {
                    if (node.getRecordID().equals(recordID))
                        return node.getRecord();
                    node = node.getNext();
                }
            }
		return null;
	}
	
	public synchronized boolean replaceRecord(Record record) {
		int id = record.getEmployeeID();
        for (HashNode node : map) {
            while (node != null) {
                if (node.getRecord().getEmployeeID() == id) {
                    node.setRecord(record);
                    return true;
                }
                node = node.getNext();
            }
        }
		return false;
	}
	
    public synchronized boolean removeRecord(String recordID) {
        for (int i  = 0; i < map.length; i++) {
            HashNode currentNode = map[i];
            int len = 0;
//            HashNode prevNode = null;
            //Cycles through linked list at map[i]
            while (currentNode != null) {
                if (currentNode.getRecordID().equals(recordID)) {
                    if (len == 0)
                        map[i] = currentNode.getNext();
//                    else {
//                        prevNode = currentNode.getNext();
//                    }
                    recordCount--;
                    return true;
                }
//                prevNode = currentNode; //Will only be set when len >0
                currentNode = currentNode.getNext();
                len++;
            }
        }
		return false;
	}

    public JSONArray getData() {
        JSONArray recordArray = new JSONArray();
        JSONObject record;
        for (HashNode currentNode : map) {
            //Cycles through linked list at map[i]
            while (currentNode != null) {
                record = currentNode.getRecord().getJSONObject();
                record.put(MessageKeys.SERVER_LOCATION, location);
                record.put(MessageKeys.RECORD_ID, currentNode.getRecordID());
                recordArray.add(record);
                currentNode = currentNode.getNext();
            }
        }
        return recordArray;
    }

    public void setData(JSONArray array) {
        Record record = null;
        JSONObject obj;
        String recordType;
        String recordID;
	    for (int i = 0; i < array.size(); i++) {
	        obj = (JSONObject) array.get(i);
	        recordID = (String) obj.get(MessageKeys.RECORD_ID);
	        recordType = ((String) obj.get(MessageKeys.RECORD_ID)).substring(0, 2);

	        if ("ER".equals(recordType))
	            record = new EmployeeRecord((String) obj.get(MessageKeys.FIRST_NAME), (String) obj.get(MessageKeys.LAST_NAME),
                        Integer.parseInt((String) obj.get(MessageKeys.EMPLOYEE_ID)), (String) obj.get(MessageKeys.MAIL_ID),
                        (String) obj.get(MessageKeys.PROJECT_ID));
            else if ("MR".equals(recordType)) {
                Project[] projs = JSONArrayToProjectArray((JSONArray) obj.get(MessageKeys.PROJECTS));
                record = new ManagerRecord((String) obj.get(MessageKeys.FIRST_NAME), (String) obj.get(MessageKeys.LAST_NAME),
                        Integer.parseInt((String) obj.get(MessageKeys.EMPLOYEE_ID)), (String) obj.get(MessageKeys.MAIL_ID),
                        projs, (String) obj.get(MessageKeys.LOCATION));
            }

            addRecord(recordID, record);
	    }
    }

    private Project[] JSONArrayToProjectArray(JSONArray projectArray) {
	    Project[] projects = new Project[projectArray.size()];
	    JSONObject obj;

	    for (int i = 0; i < projects.length; i++) {
	        obj = (JSONObject) projectArray.get(i);
	        projects[i] = new Project((String) obj.get(MessageKeys.PROJECT_ID), (String) obj.get(MessageKeys.PROJECT_CLIENT), (String) obj.get(MessageKeys.PROJECT_NAME));
        }

        return projects;
    }

	public synchronized int getRecordCount() {
		return recordCount;
	}
	
	public synchronized boolean isEmpty() {
		return recordCount == 0;
	}
	
	private String createRecordIDString(String recordIDCount) {
        String recordID = "";
        for (int i = 0; i < 5 - recordIDCount.length(); i++)
            recordID += "0";
        return recordID + recordIDCount;
	}

}
