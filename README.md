# SOEN423Project

SOEN 423 Group Project - DEMS

[Link to Google Drive Folder](https://drive.google.com/drive/folders/1a_pQnYdVTQ2WMDOOZDDUdrWAXBgGCZk6?usp=sharing)

## Instructions

```shell
# start sequencer
java -cp 'bin:lib/*' DEMS.Sequencer

# start frontend
java -cp 'bin:lib/*' FrontEnd.FrontEndServerMain <orbArguments>

# start replica manager including specified replica
java -cp 'bin:lib/*' DEMS.ReplicaManager <replicaNumber>

# start client
java -cp 'bin:lib/*' Client.ClientMain
```

## Distribution of work

TEAM :
- design server system
- modify assignment 2 to work as a non-CORBA server replica
- replica receives client requests with sequence numbers and FE information from the sequencer
- replica executes client requests in total order according to the sequence number

VIKRAM: To design and implement FE
- receives CORBA request
- forwards request to sequencer
- receives results from replicas
- sends one result to client
- FE detects failure and informs RM

FRANCIS: Design and implement RM
- creates and initializes the actively replicated server system
- RM implements failure detection and recovery for the required type of failure

SHELBY: Design and implement sequencer
- receives client request from FE
- assigns a unique sequence number to request
- reliably multicast the request and with sequence number and FE info to all 3 replicas
