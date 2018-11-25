# ORB
start orbd -ORBInitialPort 1050 -ORBInitialHost localhost

# Front End
java -cp .;lib\json-simple-1.1.1.jar FrontEnd.FrontEndServerMain -ORBInitialPort 1050 -ORBInitialHost localhost CA

# Client
java Client.ClientMain -ORBInitialPort 1050 -ORBInitialHost localhost CA1111

# Mock System
java -cp .;lib\json-simple-1.1.1.jar MockSystem

# Sequencer
java -cp 'bin:lib/*' DEMS.Sequencer

# Replicas and Replica Managers
java -cp 'bin:lib/*' DEMS.ReplicaManager 1 <errorType>
java -cp 'bin:lib/*' DEMS.ReplicaManager 2 <errorType>
java -cp 'bin:lib/*' DEMS.ReplicaManager 3 <errorType>
