# ORB
start orbd -ORBInitialPort 1050

# Front End
java -cp .;lib\json-simple-1.1.1.jar FrontEnd.FrontEndServerMain -ORBInitialPort 1050 -ORBInitialHost localhost CA

# Client
java Client.ClientMain -ORBInitialPort 1050 -ORBInitialHost localhost CA1111

# Mock System
java -cp .;lib\json-simple-1.1.1.jar MockSystem
