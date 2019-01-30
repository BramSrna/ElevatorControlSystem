import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;
import java.net.*;

public class FloorSubsystem {
    
    private DatagramPacket sendPacket, receivePacket;
    private DatagramSocket sendReceiveSocket;
    
    private int bottomFloor; // Lowest possible floor
    private int topFloor;    // Highest possible floor
    private int numFloors;   // Number of floors that the elevator services
    
    private int numElevators; // The current number of elevators
    
    // Size of service requests
    private final int CONFIG_SIZE = 4;
    private final int REQUEST_SIZE = 5; 
    private final int ARRIVAL_SIZE = 4;
    
    private final int TIME_PER_FLOOR = 2000;
    
    
    // Valid ranges for the number of 
    // floors and number of elevators
    private static final int MIN_NUM_FLOORS = 1;
    private static final int MAX_NUM_FLOORS = 1000;
    
    private static final int MIN_NUM_ELEVATORS = 1;
    private static final int MAX_NUM_ELEVATORS = 1000;
    
    // List of service requests parsed from the input file
    // Sorted in order of time that requests are made
    private ArrayList<byte[]> serviceRequests;
    
    // List of existing floor objects
    private ArrayList<Floor> floors;
    
    /**
     * FloorSubsystem
     * 
     * Constructor
     * 
     * Create a new FloorSubsystem object.
     * Initializes the number of floors to the given number.
     * Initialize the list of requests.
     * 
     * @param numFloors The number of floors for this system
     * 
     * @return  void
     */
    public FloorSubsystem(int numFloors, int numElevators) {
        serviceRequests = new ArrayList<byte[]>();
        
        floors = new ArrayList<Floor>();
        
        this.setNumElevators(numElevators);
        this.setNumFloors(numFloors);
        
        // Initialize the DatagramSocket
        try {
            sendReceiveSocket = new DatagramSocket();
        } catch (SocketException se) {
            se.printStackTrace();
            this.teardown();
            System.exit(1);
        }
    }
    
    /**
     * setNumFloors
     * 
     * Set the number of floors that the elevator services.
     * Adds and removes Floor objects from the list of Floors
     * as needed.
     * 
     * @param newNumFloors  Number of floors that the elevator services
     * 
     * @return  void
     */
    public void setNumFloors(int newNumFloors) {
        if ((newNumFloors < MIN_NUM_FLOORS) || 
            (newNumFloors > MAX_NUM_FLOORS)) {
            System.out.println("Error: Floor value is outside of valid range.");
            System.exit(1);
        }
        
        // Set the number of floors
        this.numFloors = newNumFloors;
        
        // Set bottom and top floors
        this.bottomFloor = 0;
        this.topFloor = this.numFloors - 1;
        
        // Check if the list of floors needs to be modified
        if (floors.size() < numFloors) {
            // Need more floors, so add amount needed
            for (int i = floors.size(); i < numFloors; i++) {
                floors.add(new Floor(this, i, numElevators));
            }   
        } else if (floors.size() > numFloors) {
            // Too many floors, so remove floors
            ArrayList<Floor> toRemove = new ArrayList<Floor>();
            
            // Get a list of floors to remove
            for (Floor currFloor : floors) {
                if (currFloor.getFloorNumber() > numFloors - 1) {
                    toRemove.add(currFloor);
                }
            }
            
            // Remove the marked floors
            for (Floor currFloor : toRemove) {
                floors.remove(currFloor);
            }
        }
    }
    
    /**
     * setNumElevators
     * 
     * Sets the number of elevators to the new amount.
     * Checks that the new number is within the valid range.
     * 
     * @param newNumElevators The new number of elevators
     * 
     * @return  void
     */
    public void setNumElevators(int newNumElevators) {
        if ((newNumElevators < MIN_NUM_ELEVATORS) ||
            (newNumElevators > MAX_NUM_ELEVATORS)) {
            System.out.println("Error: Elevator value is outside of valid range.");
            System.exit(1);
        }
        
        this.numElevators = newNumElevators;
    }
    
    /**
     * parseInputFile
     * 
     * Parses the given text file containing requests.
     * The requests are added to the system.
     * There should be one request per file.
     * The requests should be in the following format:
     *      Time         Floor FloorButton CarButton
     *      hh:mm:ss.mmm n     Up/Down     m
     *      
     *      Example:
     *      14:05:15.0 2 Up 4
     *      
     *      I.e.: String Space Int Space String Space Int
     *      
     * Where:
     *      Time = Time that request is made
     *      Floor = Floor on which the passenger is making the request
     *      FloorButton = Direction button the passenger pressed (Up or Down)
     *      CarButton = Integer representing the desired destination floor
     * 
     * @param pathToFile String containing a path to the file to parse
     * 
     * @return void
     */
    public void parseInputFile(String pathToFile) {
        // Indices of all significant data in each line of the text file
        int timeInd = 0;
        int startFloorInd = 1;
        int directionFloorInd = 2;
        int endFloorInd = 3;
        
        // Significant indices for parsing the time
        int hourInd = 0;
        int minInd = 1;
        int secInd = 2;
        
        // Setup the file for parsing
        FileReader input = null;
        
        try {
            input = new FileReader(pathToFile);
        } catch (FileNotFoundException e) {
            System.out.println("Error: File could not be found: " + pathToFile) ;
            e.printStackTrace();
            System.exit(1);
        }
        
        BufferedReader bufRead = new BufferedReader(input);
        
        // Get the first line in the file
        String currLine = "";        
        try {
            currLine = bufRead.readLine();
        } catch (IOException e) {
            System.out.println("Error while reading file: " + pathToFile);
            e.printStackTrace();
            System.exit(1);
        }
        
        // Parse the current line
        // Add the request
        // Go to the next line
        while(currLine != null) {
            // Split the line at spaces
            String[] info = currLine.split(" ");
            
            // Get all important parts of data
            String timeStr = info[timeInd];
            String startFloorStr = info[startFloorInd];
            String directionStr  = info[directionFloorInd];
            String finalFloorStr = info[endFloorInd];
            
            int hourInt = 0;
            int minInt = 0;
            int secInt = 0;
            int milliSecInt = 0;
            int startFloorInt = 0;
            int finalFloorInt = 0;
            HardwareState.ElevatorDirection directionEnum;
            
            // Convert the data to the proper format
            // Time format is hh:mm:ss.mmmm
            String[] timeParts = timeStr.split(":");
            
            // Get the hour and minute
            hourInt = Integer.parseInt(timeParts[hourInd]);
            minInt = Integer.parseInt(timeParts[minInd]);
            
            String[] secParts = timeParts[secInd].split("\\.");
            
            // Get the second and millisecond
            secInt = Integer.parseInt(secParts[0]);
            milliSecInt = Integer.parseInt(secParts[1]);
            
            try {
                startFloorInt = Integer.parseInt(startFloorStr);
            } catch (Exception e) {
                System.out.println("Error: Start floor must be an integer.");
            }
            
            try {
                finalFloorInt = Integer.parseInt(finalFloorStr);
            } catch (Exception e) {
                System.out.println("Error: Start floor must be an integer.");
            }
            
            directionEnum = HardwareState.ElevatorDirection.valueOf(directionStr.toUpperCase());
            
            // Find the floor where the request was made,
            // and make the request
            for (Floor floor : floors) {
                if (floor.getFloorNumber() == startFloorInt) {
                    floor.elevatorRequest(hourInt, 
                                          minInt, 
                                          secInt, 
                                          milliSecInt, 
                                          directionEnum, 
                                          finalFloorInt);
                }
            }
            
            // Get the next line in the file
            try {
                currLine = bufRead.readLine();
            } catch (IOException e) {
                System.out.println("Error while reading file: " + pathToFile);
                e.printStackTrace();
                System.exit(1);
            }
        }
        
        // Close the file being read
        try {
            input.close();
        } catch (IOException e) {
            System.out.println("Error: Unable to close input file.");
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    /**
     * addRequest
     * 
     * Adds a request to the list of requests with the given information.
     * 
     * The requests use the following format:
     *  1st byte = Hour request was made
     *  2nd byte = Minute request was made
     *  3rd byte = Second request was made
     *  4th byte = Millisecond request was made
     *  5th byte = Floor where request was made
     *  6th byte = Direction user wants to travel (0 = Down, 1 = Up)
     *  7th byte = Floor where user wants to travel
     * 
     * @param hrOfReq : Hour that request was made
     * @param minOfReq : Minute that request was made
     * @param secOfReq : Second that request was made
     * @param msOfReq : Millisecond that request was made
     * @param startFloor : Floor where request was made
     * @param dirPressed : Direction that used wants to travel
     * @param finalFloor : Floor that user wants to travel to
     * 
     * @return  void
     */
    public void addElevatorRequest(int hrOfReq, 
                                    int minOfReq, 
                                    int secOfReq, 
                                    int msOfReq, 
                                    int startFloor, 
                                    HardwareState.ElevatorDirection dirPressed, 
                                    int finalFloor) {
        // Check that the given floors are valid
        if ((startFloor < bottomFloor) || (startFloor > topFloor) ||
            (finalFloor < bottomFloor) || (finalFloor > topFloor)) {
            System.out.println("Error: Invalid request. One or both floors given is invalid.");
            System.exit(1);
        }
        
        // Check that the direction is valid for the floor
        if (((startFloor == bottomFloor) && (dirPressed == HardwareState.ElevatorDirection.DOWN)) ||
            ((startFloor == topFloor) && (dirPressed == HardwareState.ElevatorDirection.UP))) {
            System.out.println("Error: Invalid request. Direction invalid for given floor.");
            System.exit(1);
        }
        
        // Check that the start + end floors match the given direction
        if (((dirPressed == HardwareState.ElevatorDirection.DOWN) && (finalFloor > startFloor)) ||
            ((dirPressed == HardwareState.ElevatorDirection.UP) && (finalFloor < startFloor))) {
            System.out.println("Error: Invalid request. "
                    + "Given floor numbers do not match desired direction.");
            System.exit(1);
        }
        
        // Formulate byte array
        byte request[] = new byte[7];
        
        int hrInd = 0;
        int minInd = 1;
        int secInd = 2;
        int msInd = 3;
        int startFloorInd = 4;
        int dirInd = 5;
        int finalFloorInd = 6;
        
        // Set all of the values
        request[hrInd] = (byte) hrOfReq;
        request[minInd] = (byte) minOfReq;
        request[secInd] = (byte) secOfReq;
        request[msInd] = (byte) msOfReq;
        request[startFloorInd] = (byte) startFloor;
        request[dirInd] = (byte) dirPressed.ordinal();
        request[finalFloorInd] = (byte) finalFloor;
        
        // Add it to the list of requests
        if (serviceRequests.isEmpty()) {
            // If empty, just add the request
            serviceRequests.add(request);
        } else {
            // If not empty add it to the proper spot in the list
            byte currReq[];
            int i = 0;
            while (i < serviceRequests.size()) {
                currReq = serviceRequests.get(i);
                
                // Check if current request should be after new request
                if ((request[hrInd] < currReq[hrInd]) ||
                    ((request[hrInd] == currReq[hrInd]) && (request[minInd] < currReq[minInd])) ||
                    ((request[hrInd] == currReq[hrInd]) && (request[minInd] == currReq[minInd]) && (request[secInd] < currReq[secInd])) ||
                    ((request[hrInd] == currReq[hrInd]) && (request[minInd] == currReq[minInd]) && (request[secInd] == currReq[secInd]) && (request[msInd] < currReq[msInd]))) {
                     break;
                 }
                
                i++;
            }
            serviceRequests.add(i, request);
        } 
    }
    
    /**
     * getValidFloorValueRange
     * 
     * Static method
     * 
     * Returns the range of valid number of floor values.
     * Returns the range as an array of 2 values.
     * Format:
     *      Byte 0 - Minimum value
     *      Byte 1 - Maximum value
     * 
     * @return int[] First byte is minimum value
     *               Second byte is maximum value
     */
    public static int[] getValidFloorValueRange() {
        int validRange[] = new int[2];
        
        validRange[0] = MIN_NUM_FLOORS;
        validRange[1] = MAX_NUM_FLOORS;
        
        return(validRange);
    }
    
    /**
     * getValidElevatorValueRange
     * 
     * Static method
     * 
     * Returns the range of valid number of elevator values.
     * Returns the range as an array of 2 values.
     * Format:
     *      Byte 0 - Minimum value
     *      Byte 1 - Maximum value
     * 
     * @return int[] First byte is minimum value
     *               Second byte is maximum value
     */
    public static int[] getValidElevatorValueRange() {
        int validRange[] = new int[2];
        
        validRange[0] = MIN_NUM_ELEVATORS;
        validRange[1] = MAX_NUM_ELEVATORS;
        
        return(validRange);
    }
    
    /**
     * getRequests
     * 
     * Return the current list of requests.
     * 
     * @return  ArrayList<byte[]> : The current list of requests
     */
    public ArrayList<byte[]> getRequests(){
        return(serviceRequests);
    }
       
    /**
     * sendTeardownSignal
     * 
     * Sends a signal that the program should teardown.
     * 
     * Format:
     * 
     * @return  void
     */
    public void sendTeardownSignal() {
        
    }
    
    /**
     * teardown
     * 
     * Sends a teardown signal and then closes
     * all open sockets.
     * 
     * @return  void
     */
    public void teardown() {
        sendTeardownSignal();
    }
    
    /**
     * sendConfigurationSignal
     * 
     * Sends a configuration signal with the
     * number of elevators and elevator shaft number.
     * 
     * @param numElevators  The amount for elevators the building has
     * @param numFloors     The amount of floors the buidling has
     * 
     * @return  void
     */
    public void sendConfigurationSignal(int numElevators, int numFloors) {
        System.out.println("Sending a packet containing elevator configuration\n");        
        
        // Construct a message to send with data from given parameters        
        byte[] msg = new byte[CONFIG_SIZE];
        msg[0] = 0;
        msg[1] = (byte) numElevators;
        msg[2] = (byte) numFloors;
        msg[3] = -1;
                
        // Construct a datagram packet that is to be sent to a specified port 
        // on a specified host.
        // The arguments are:
        //  msg - the message contained in the packet (the byte array)
        //  msg.length - the length of the byte array
        //  InetAddress.getLocalHost() - the Internet address of the 
        //     destination host.
        //     InetAddress.getLocalHost() returns the Internet
        //     address of the local host.
        //  420 - the destination port number on the destination host.
        try {
            sendPacket = new DatagramPacket(msg, msg.length,
                            InetAddress.getLocalHost(), 420);
        } catch (UnknownHostException e) {
            e.printStackTrace();
            System.exit(1);
        }
                
        // Process the sent datagram.
        System.out.println("Config: Sending signal...");
                
        // Send the datagram packet to the host via the send/receive socket. 
                
        try {
            sendReceiveSocket.send(sendPacket);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
                
        System.out.println("Config: Signal sent.\n");
                
        // Construct a DatagramPacket for receiving packets up 
        // to 100 bytes long (the length of the byte array).
                
        byte data[] = new byte[100];
        receivePacket = new DatagramPacket(data, data.length);
        
        System.out.println("Config: Waiting for response...\n");
                
        try {
            // Block until a datagram is received via sendReceiveSocket.  
            sendReceiveSocket.receive(receivePacket);
        } catch(IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
                
        System.out.println("Config: Response received.");
    }
    
    
    public int getNumFloors() {
        return numFloors;
    }
    
    public int getNumElevators() {
        return numElevators;
    }
    
    
    public void sendElevatorRequest(int sourceFloor, 
						    		int destFloor, 
						    		HardwareState.ElevatorDirection diRequest) {        
        System.out.println("Sending a packet containing elevator request details\n");        
        
        // Construct a message to send with data from given parameters        
        byte[] msg = new byte[REQUEST_SIZE];
        msg[0] = 2;
        msg[1] = (byte) sourceFloor;
        msg[2] = (byte) diRequest.ordinal();
        msg[3] = (byte) destFloor;
        msg[4] = -1;
                
        // Construct a datagram packet that is to be sent to a specified port 
        // on a specified host.
        // The arguments are:
        //  msg - the message contained in the packet (the byte array)
        //  msg.length - the length of the byte array
        //  InetAddress.getLocalHost() - the Internet address of the 
        //     destination host.
        //     InetAddress.getLocalHost() returns the Internet
        //     address of the local host.
        //  420 - the destination port number on the destination host.
        try {
            sendPacket = new DatagramPacket(msg, msg.length,
                            InetAddress.getLocalHost(), 420);
        } catch (UnknownHostException e) {
            e.printStackTrace();
            System.exit(1);
        }
                
        // Process the sent datagram.
        System.out.println("Floor" + sourceFloor + ": Elevator request...");
                
        // Send the datagram packet to the host via the send/receive socket. 
                
        try {
            sendReceiveSocket.send(sendPacket);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
                
        System.out.println("Floor" + sourceFloor + ": Elevator request sent.\n");
    }
    
    public void runSubsystem() {        
        Timer timer = new Timer();
        for (int i = 0; i < serviceRequests.size(); i++) {
            byte currReq[] = serviceRequests.get(i);
            
            byte startFloor = currReq[4];
            byte endFloor = currReq[6];
            byte dir = currReq[5];
            
            sendElevatorRequest((int) startFloor, 
			            		(int) endFloor, 
			            		HardwareState.ElevatorDirection.values()[(int) dir]);
            
            int timeUntilNextRequest = 0;
            
            if (i < serviceRequests.size() - 1) {
                byte nextReq[] = serviceRequests.get(i + 1);
                
                timeUntilNextRequest = 0;
                
                timeUntilNextRequest += nextReq[3] - currReq[3];
                timeUntilNextRequest += nextReq[2] - currReq[2] * 1000;
                timeUntilNextRequest += nextReq[1] - currReq[1] * 60 * 1000;
                timeUntilNextRequest += nextReq[0] - currReq[0] * 60 * 60 * 1000;
                
            } else {
                timeUntilNextRequest = 10 * 1000;
            }
            
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    while (true) {
                        timedMovement();
                    }                   
                }
            }, timeUntilNextRequest);
        }
    }
    
    
    public void timedMovement() {
        
        int floorTiming = TIME_PER_FLOOR;
        byte data[] = new byte[100];
        receivePacket = new DatagramPacket(data, data.length);
                
        try {
            // Block until a datagram is received via sendReceiveSocket.  
            sendReceiveSocket.receive(receivePacket);
        } catch(IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
        
        byte holder = receivePacket.getData()[1];
        HardwareState.ElevatorDirection dir = 
        		HardwareState.ElevatorDirection.values()[(int) receivePacket.getData()[2]];
        if (dir == HardwareState.ElevatorDirection.UP) {
            holder++;
        } else {
            holder--;
        }
        
        for (Floor currFloor : floors) {
        	
        }
    }
    
    
    
    /**
     * main
     * 
     * Static method
     * 
     * Main method
     * 
     * Creates a new UserInterface class to get input from the user.
     * Uses the input to control a FloorSubsystem.
     * 
     * @param args
     * 
     * @return  void
     */
    public static void main(String[] args) {
        UserInterface ui = new UserInterface();
        
        // Get basic configuration information to start
        ui.getNewConfigurationInformation();
        
        // Create a FloorSubsystem with the given information
        FloorSubsystem floorController = new FloorSubsystem(ui.getNumFloors(), ui.getNumElevators());
        
        floorController.sendConfigurationSignal(floorController.getNumElevators(), floorController.getNumFloors());
            
        // While true
        // Display the valid options to the user
        // Based off of user input, run the corresponding method(s)
        while (true) {
            UserInterface.ReturnVals val = ui.displayMenu();
            
            if (val == UserInterface.ReturnVals.RECONFIG) {
                // If reconfing was received, resend the configuration method               
                floorController.setNumFloors(ui.getNumFloors());
                floorController.setNumElevators(ui.getNumElevators());
                floorController.sendConfigurationSignal(floorController.getNumElevators(), floorController.getNumFloors());
            } else if (val == UserInterface.ReturnVals.NEW_TEST_FILE) {
                // If a new test file was entered, parse the file
                floorController.parseInputFile(ui.getTestFile());
                floorController.runSubsystem();
            } else if (val == UserInterface.ReturnVals.TEARDOWN) {
                // If teardown was selected,
                // Send the teardown signal
                // Exit the program
                floorController.teardown();
                floorController = null;
                System.exit(1);
            }
        }
        
    }
}
