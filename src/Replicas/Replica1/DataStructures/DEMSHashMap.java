package Replicas.Replica1.DataStructures;

public class DEMSHashMap {
	
	private class HashNode {
		
		private int key;
		private String recordID;
		private Record record;
		private HashNode next;
		
		HashNode(char key, String recordID, Record record, HashNode link){
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
	
	public synchronized String addRecord(Record record, String recordType){
		HashNode newNode = new HashNode(record.getLastName().toUpperCase().charAt(0), recordType + produceRecordIDString("" + ID_count), record, null);

		return addHashNode(newNode);
	}
	
	public synchronized String addRecord(String recordID, Record record){
		HashNode newNode = new HashNode(record.getLastName().toUpperCase().charAt(0), recordID, record, null);
		return addHashNode(newNode);
	}

	private synchronized String addHashNode(HashNode newNode) {
        int key = newNode.getKey();
        if (map[key] == null)
            map[key] = newNode;
        else {
            HashNode node = map[key];
            while (node.getNext() != null){
                node = node.getNext();
            }
            node.setNext(newNode);
        }

        ID_count++;
        recordCount++;
        return newNode.getRecordID();

    }
	public synchronized Record getRecord(String recordID){
            for (HashNode node : map) {
                while (node != null){
                    if (node.getRecordID().equals(recordID)) {
                        return node.getRecord();
                    }
                    node = node.getNext();
                }
            }
		return null;
	}
	
	public synchronized boolean replaceRecord(Record record){
		int id = record.getEmployeeID();
        for (HashNode node : map) {
            while (node != null){
                if (node.getRecord().getEmployeeID() == id) {
                    node.setRecord(record);
                    return true;
                }
                node = node.getNext();
            }
        }
		return false;
	}
	
    public synchronized boolean removeRecord(String recordID){
        for (int i  = 0; i < map.length; i++) 
        {
            HashNode currentNode = map[i];
            int len = 0;
            HashNode prevNode = null;
            
            //Cycles through linked list at map[i]
            while (currentNode != null) 
            {
                if (currentNode.getRecordID().equals(recordID)) 
                {
                    if (len == 0) {
                        map[i] = currentNode.getNext();
                    } else {
                        prevNode = currentNode.getNext();
                    }
                    recordCount--;
                    return true;
                }
                prevNode = currentNode; //Will only be set when len >0
                currentNode = currentNode.getNext();
                len++;
            }
        }
		return false;
	}
	
	public synchronized int getRecordCount(){
		return recordCount;
	}
	
	public synchronized boolean isEmpty(){
		return recordCount == 0;
	}
	
	private String produceRecordIDString(String recordIDCount) {
        String recordID = "";
        for (int i = 0; i < 5 - recordIDCount.length(); i++) {
            recordID += "0";
        }
        return recordID + recordIDCount;
	}

}
