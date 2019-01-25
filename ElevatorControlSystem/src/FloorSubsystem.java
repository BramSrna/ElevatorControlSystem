import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.net.*;


public class FloorSubsystem {
	
	private DatagramPacket sendPacket, receivePacket;
	private DatagramSocket sendReceiveSocket;
    private int bottomFloor; // Lowest possible floor
    private int topFloor;    // Highest possible floor
    private int numFloors;   // Number of floors that the elevator services
    
    // Size of the service requests
    private final int CONFIG_SIZE = 4;
    private final int REQUEST_SIZE = 5; 
    private final int ARRIVAL_SIZE = 4;
    // List of service requests parsed from the input file
    // Sorted in order of time that requests are made
    ArrayList<byte[]> serviceRequests;
    
    ArrayList<Floor> floors;
    
    // Possible directions for the requests
    public enum Direction {
        DOWN,
        UP
    }
    
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
        this.setNumFloors(numFloors);
        
        serviceRequests = new ArrayList<byte[]>();
        
        floors = new ArrayList<Floor>();
        
        try {
			// Construct a datagram socket and bind it to any available 
			// port on the local host machine. This socket will be used to
			// send and receive UDP Datagram packets.
			sendReceiveSocket = new DatagramSocket();
		} catch (SocketException se) {   // Can't create the socket.
			se.printStackTrace();
			System.exit(1);
		}
        
        
        for (int i = 0; i < numFloors; i++) {
            floors.add(new Floor(this, i, numElevators));
        }
    }
    
    /**
     * setNumFloors
     * 
     * Set the number of floors that the elevator services.
     * 
     * @param newNumFloors  Number of floors that the elevator services
     * 
     * @return  void
     */
    public void setNumFloors(int newNumFloors) {
        this.numFloors = newNumFloors;
        
        this.bottomFloor = 0;
        this.topFloor = this.numFloors - 1;
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
            Direction directionEnum;
            
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
            
            directionEnum = Direction.valueOf(directionStr.toUpperCase());
            
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
    public void sendElevatorRequest(int hrOfReq, 
		                            int minOfReq, 
		                            int secOfReq, 
		                            int msOfReq, 
		                            int startFloor, 
		                            Direction dirPressed, 
		                            int finalFloor) {
        // Check that the given floors are valid
        if ((startFloor < bottomFloor) || (startFloor > topFloor) ||
            (finalFloor < bottomFloor) || (finalFloor > topFloor)) {
            System.out.println("Error: Invalid request. One or both floors given is invalid.");
            System.exit(1);
        }
        
        // Check that the direction is valid for the floor
        if (((startFloor == bottomFloor) && (dirPressed == Direction.DOWN)) ||
            ((startFloor == topFloor) && (dirPressed == Direction.UP))) {
            System.out.println("Error: Invalid request. Direction invalid for given floor.");
            System.exit(1);
        }
        
        // Check that the start + end floors match the given direction
        if (((dirPressed == Direction.DOWN) && (finalFloor > startFloor)) ||
            ((dirPressed == Direction.UP) && (finalFloor < startFloor))) {
            System.out.println("Error: Invalid request. "
                    + "Given floor numbers do not match desired direction.");
            System.exit(1);
        }
        
        // Formulate byte array
        byte request[] = new byte[REQUEST_SIZE];
        
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
     * Send a message to indicate where the elevator is
     * 
     * @param elevatorShaftNum : Which elevator this message is for
     * @param floorNum : The floor the elevator is at
     */
    public void sendArrivalSensorSignal(int elevatorShaftNum, int floorNum) {
    	

    	System.out.println("Floor " + floorNum + ": sending a packet containing elevator location\n");
    			
    	// Java stores characters as 16-bit Unicode values, but 
    	// DatagramPackets store their messages as byte arrays.
    	// Convert the String into bytes according to the platform's 
    	// default character encoding, storing the result into a new 
    	// byte array.
    	
    	byte[] msg = new byte[4];
    	msg[0] = 0;
    	msg[1] = (byte) floorNum;
    	msg[2] = (byte) elevatorShaftNum;
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
    	System.out.println("Floor" + floorNum + ": Sending packet:");
    	System.out.println("To host: " + sendPacket.getAddress());
    	System.out.println("Destination host port: " + sendPacket.getPort());
    	int len = sendPacket.getLength();
    	System.out.println("Length: " + len);
    			
    			
    	// Print the byte array
    	System.out.print("Containing (Bytes): ");
    	System.out.println(Arrays.toString(msg));
    			
    	// Send the datagram packet to the host via the send/receive socket. 
    			
    	try {
    		sendReceiveSocket.send(sendPacket);
    	} catch (IOException e) {
    		e.printStackTrace();
    		System.exit(1);
    	}
    			
    	System.out.println("Floor" + floorNum + ": Packet sent.\n");
    			
    	// Construct a DatagramPacket for receiving packets up 
    	// to 100 bytes long (the length of the byte array).
    			
    	byte data[] = new byte[100];
    	receivePacket = new DatagramPacket(data, data.length);
    			
    	try {
    		// Block until a datagram is received via sendReceiveSocket.  
    		sendReceiveSocket.receive(receivePacket);
    	} catch(IOException e) {
    		e.printStackTrace();
    		System.exit(1);
    	}
    			
    	// Process the received datagram.
    	System.out.println("Floor" + floorNum + ": Packet received:");
    	System.out.println("From host: " + receivePacket.getAddress());
    	System.out.println("Host port: " + receivePacket.getPort());
    	len = receivePacket.getLength();
    	System.out.println("Length: " + len);
    	System.out.print("Containing: ");
    			
    	// Form a String from the byte array.
    	String received = new String(data, 2,len);   
    	System.out.println(received);
    	System.out.println(Arrays.toString(receivePacket.getData()));
    	// We're finished, so close the socket.
    }
}
