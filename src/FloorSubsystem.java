import java.awt.*;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;

import javax.swing.*;

public class FloorSubsystem extends ServerPattern{
	// Sockets and packets used for UDP
	private DatagramPacket sendPacket;
	private DatagramSocket sendSocket;

	// Important floor indices
	private int numFloors; // Number of floors that the elevator services

	private int numElevators; // The current number of elevators

	// Size of service requests
	private final int CONFIG_SIZE = 4;
	private final int REQUEST_SIZE = 5;
	private final int TEARDOWN_SIZE = 10;

	// List of existing floor objects
	private ArrayList<Floor> floors;

	// Address to send messages to
	private InetAddress addressToSend;
	
	private ArrayList<Thread> floorThreads;

	/**
	 * FloorSubsystem
	 * 
	 * Constructor
	 * 
	 * Create a new FloorSubsystem object. 
	 * Initializes the number of floors to the given number. 
	 * Initializes the list of floors and fills in with Floor objects.
	 * Initializes the number of elevators.
	 * Initialize the list of requests.
	 * 
	 * @param 	numFloors 		The number of floors for this system
	 * @param	numElevators	The number of elevators in the system
	 * 
	 * @return None
	 */
	public FloorSubsystem(int numFloors, int numElevators) {
	    super(UtilityInformation.FLOOR_PORT_NUM, "FloorSubsystem");

		floors = new ArrayList<Floor>();
		floorThreads = new ArrayList<Thread>();

		this.setNumElevators(numElevators);
		this.setNumFloors(numFloors);

		// Initialize the DatagramSocket
		try {
			sendSocket = new DatagramSocket();
		} catch (SocketException se) {
			se.printStackTrace();
			this.teardown();
			System.exit(1);
		}

		// Set the address to send to
		try {
			addressToSend = InetAddress.getLocalHost();
		} catch (UnknownHostException e) {
			System.out.println("Error: Unable to get local address.");
			e.printStackTrace();
			this.teardown();
			System.exit(1);
		}
	}

	/**
	 * setNumFloors
	 * 
	 * Set the number of floors that the elevator services. Adds and removes Floor
	 * objects from the list of Floors as needed.
	 * 
	 * @param newNumFloors Number of floors that the elevator services
	 * 
	 * @return void
	 */
	public void setNumFloors(int newNumFloors) {
		if ((newNumFloors < UtilityInformation.MIN_NUM_FLOORS) || 
		    (newNumFloors > UtilityInformation.MAX_NUM_FLOORS)) {
			System.out.println("Error: Floor value is outside of valid range.");
			this.teardown();
			System.exit(1);
		}

		// Set the number of floors
		this.numFloors = newNumFloors;

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
		
		floorThreads.clear();
		
		for (Floor currFloor : floors) {
		    floorThreads.add(new Thread(currFloor));
		}
	}

	/**
	 * setNumElevators
	 * 
	 * Sets the number of elevators to the new amount. Checks that the new number is
	 * within the valid range. Propagates the new information to all of the Floor
	 * objects that belong to this FloorSubsystem.
	 * 
	 * @param newNumElevators The new number of elevators
	 * 
	 * @return void
	 */
	public void setNumElevators(int newNumElevators) {
		if ((newNumElevators < UtilityInformation.MIN_NUM_ELEVATORS) || 
		    (newNumElevators > UtilityInformation.MAX_NUM_ELEVATORS)) {
			System.out.println("Error: Elevator value is outside of valid range.");
			super.teardown();
			this.teardown();
			System.exit(1);
		}

		this.numElevators = newNumElevators;

		// Update all of the Floor objects that belong to this FloorSubsystem
		for (Floor currFloor : floors) {
			currFloor.setNumElevatorShafts(newNumElevators);
		}
	}

	/**
	 * parseInputFile
	 * 
	 * Parses the given text file containing requests. The requests are added to the
	 * system. There should be one request per line. The requests should be in the
	 * following format: 
	 * 		Time Floor FloorButton CarButton 
	 * 		i.e. hh:mm:ss.mmm n Up/Down m
	 * 
	 * Example: 14:05:15.0 2 Up 4
	 * 
	 * I.e.: String Space Int Space String Space Int
	 * 
	 * Where: 
	 * 		Time = Time that request is made 
	 * 		Floor = Floor on which the passenger is making the request 
	 * 		FloorButton = Direction button the passenger pressed (Up or Down) 
	 * 		CarButton = Integer representing the desired destination floor
	 * 
	 * @param pathToFile String containing a path to the file to parse
	 * 
	 * @return void
	 */
	public void parseInputFile(String pathToFile) {
		// Setup the file for parsing
		FileReader input = null;

		try {
			input = new FileReader(pathToFile);
		} catch (FileNotFoundException e) {
			System.out.println("Error: File could not be found: " + pathToFile);
			e.printStackTrace();
			this.teardown();
			System.exit(1);
		}

		BufferedReader bufRead = new BufferedReader(input);

		System.out.println("Parsing test file...");

		// Get the first line in the file
		String currLine = "";
		try {
			currLine = bufRead.readLine();
		} catch (IOException e) {
			System.out.println("Error while reading file: " + pathToFile);
			e.printStackTrace();
			this.teardown();
			System.exit(1);
		}
		
		int timeOfFirstRequest = -1;

		// Parse the current line
		// Add the request
		// Go to the next line
		while (currLine != null) {
			int[] vals = parseInputFileLine(currLine);
			
			if (timeOfFirstRequest == -1) {
			    timeOfFirstRequest = vals[0];
			}
			
			// Find the floor where the request was made,
			// and make the request
			for (Floor floor : floors) {
				if (floor.getFloorNumber() == vals[1]) {
				    System.out.println(String.format("TIME: %d", vals[0] - timeOfFirstRequest));
				    
				    if (vals[2] == -1) {
				        floor.createErrorOccuranceRequest(vals[0] - timeOfFirstRequest, 
				        								  UtilityInformation.ErrorType.values()[vals[1]]);
				    } else {
				        floor.createElevatorRequest(vals[0] - timeOfFirstRequest, 
                                                    UtilityInformation.ElevatorDirection.values()[vals[3]], 
                                                    vals[2]);
				    }
				}
			}

			// Get the next line in the file
			try {
				currLine = bufRead.readLine();
			} catch (IOException e) {
				System.out.println("Error while reading file: " + pathToFile);
				e.printStackTrace();
				this.teardown();
				System.exit(1);
			}
		}

		// Close the file being read
		try {
			input.close();
		} catch (IOException e) {
			System.out.println("Error: Unable to close input file.");
			e.printStackTrace();
			this.teardown();
			System.exit(1);
		}

		System.out.println("Finished parsing test file.");
	}
	
	/**
	 * Parse a line in the given input file.
	 * Returns a list containing the parsed information.
	 * 
	 * Expected format for input file line:
	 *     For Elevator Request:
	 *         HH:MM:SS.SSS START_FLOOR DIRECTION END_FLOOR
	 *     For Error Request:
	 *         HH:MM:SS.SSS ERROR_TYPE
	 * 
	 * Format of return:
	 *     Elevator Request:
	 *         [Time of request, start floor, final floor, direction]
	 *     Error Request:
	 *         [Time of request, error type]
	 *         
	 * @param line A line from the input file given as a string
	 * 
	 * @return Integer[] containg the information from the line
	 */
	private int[] parseInputFileLine(String line) { 
        int[] returnVal = new int[4];
        
        // Set all values in the return array to -1
        for (int i = 0; i < returnVal.length; i++) {
            returnVal[i] = -1;
        }
        
        String[] info = line.split(" ");

        // Get all important parts of data
        String timeStr = info[0];
        String startFloorStr = info[1];
        
        // Convert the data to the proper format
        // Time format is hh:mm:ss.mmmm
        String[] timeParts = timeStr.split(":");

        // Get the hour and minute
        int hourInt = Integer.parseInt(timeParts[0]);
        int minInt = Integer.parseInt(timeParts[1]);

        String[] secParts = timeParts[2].split("\\.");

        // Get the second and millisecond
        int secInt = Integer.parseInt(secParts[0]);
        int milliSecInt = Integer.parseInt(secParts[1]);

        // Get the time of request in milliseconds
        milliSecInt += secInt * 1000;
        milliSecInt += minInt * 60 * 1000;
        milliSecInt += hourInt * 60 * 60 * 1000;
        
        // Check if the current line is an error occurrence or an elevator request
        try {
            UtilityInformation.ErrorType errorType = UtilityInformation.ErrorType.valueOf(startFloorStr.toUpperCase());
            
            returnVal[0] = milliSecInt;
            returnVal[1] = errorType.ordinal();
        } catch (Exception e1) {
            String directionStr = info[2];
            String finalFloorStr = info[3];
            
            int startFloorInt = 0;
            try {
                startFloorInt = Integer.parseInt(startFloorStr);
            } catch (Exception e2) {
                System.out.println("Error: Start floor must be an integer.");
            }

            int finalFloorInt = 0;
            try {
                finalFloorInt = Integer.parseInt(finalFloorStr);
            } catch (Exception e3) {
                System.out.println("Error: Start floor must be an integer.");
            }

            UtilityInformation.ElevatorDirection directionEnum = UtilityInformation.ElevatorDirection.valueOf(directionStr.toUpperCase());
            
            returnVal[0] = milliSecInt;
            returnVal[1] = startFloorInt;
            returnVal[2] = finalFloorInt;
            returnVal[3] = directionEnum.ordinal();
        }
        
        return(returnVal);
	}

	/**
	 * teardown
	 * 
	 * Sends a teardown signal and then closes all open sockets.
	 * 
	 * @param  None
	 * 
	 * @return void
	 */
	public void teardown() {
		sendTeardownSignal();
		super.teardown();
		sendSocket.close();
	}
	
	/**
	 * toString
	 * 
	 * Overridden
	 * 
	 * Returns a String object describing this FloorSubsystem
	 * 
	 * @param  None
	 * 
	 * @return String  String representing this FloorSubsystem
	 */
	public String toString() {
		String toReturn = "";

		// Add the information about each Floor object in the FloorSubsystem
		for (Floor currFloor : floors) {
			toReturn += currFloor.toString();
			toReturn += "\n";
		}

		return (toReturn);
	}

	/**
	 * getNumFloors
	 * 
	 * Return the current number of Floor objects in this FloorSubsystem
	 * 
	 * @param  None
	 * 
	 * @return int The number of Floor objects in this FloorSubsystem
	 */
	public int getNumFloors() {
		return numFloors;
	}

	/**
	 * getNumElevators
	 * 
	 * Return the current number of Elevators that the system is configured to use.
	 * 
	 * @param  NoneS
	 * 
	 * @return int The number of elevators that the system is configured to
	 */
	public int getNumElevators() {
		return numElevators;
	}

	/**
	 * sendTeardownSignal
	 * 
	 * Sends a signal that the program should teardown.
	 * 
	 * Format:
	 *     Byte 0: UtilityInformation.TEARDOWN_MODE
	 *     Byte 1: -1
	 *     
	 * @param  None
	 * 
	 * @return void
	 */
	public void sendTeardownSignal() {
		// Construct a message to send with data from given parameters
		byte[] msg = new byte[TEARDOWN_SIZE];
		msg[0] = UtilityInformation.TEARDOWN_MODE;
		msg[1] = UtilityInformation.END_OF_MESSAGE;

		// Send the signal
		System.out.println("Sending teardown signal...");
		sendSignal(msg, UtilityInformation.SCHEDULER_PORT_NUM, addressToSend);
		System.out.println("Teardown signal sent...");
	}

	/**
	 * sendConfigurationSignal
	 * 
	 * Sends a configuration signal with the number of elevators
     * and the number of floors in the system
	 * 
	 * Format:
	 *     Byte 0: UtilityInformation.CONFIG_MODE
	 *     Byte 1: The number of elevators in the system
	 *     Byte 2: The number of floors in the system
	 *     Byte 3: -1
	 * 
	 * @param numElevators The amount for elevators the building has
	 * @param numFloors    The amount of floors the buidling has
	 * 
	 * @return void
	 */
	public void sendConfigurationSignal(int numElevators, int numFloors) {
		// Construct a message to send with data from given parameters
		byte[] msg = new byte[CONFIG_SIZE];
		msg[0] = UtilityInformation.CONFIG_MODE;
		msg[1] = (byte) numElevators;
		msg[2] = (byte) numFloors;
		msg[3] = UtilityInformation.END_OF_MESSAGE;

		// Send the signal
		System.out.println("Sending configuration signal...");
		sendSignal(msg, UtilityInformation.SCHEDULER_PORT_NUM, addressToSend);
		System.out.println("Configuration signal sent...");

		// Wait for a confirmation from the Scheduler before commencing the program
		System.out.println("Waiting for response to configuration signal...");
		this.getNextRequest();
		System.out.println("Respone to configuration received.");
	}

	/**
	 * sendElevatorRequest
	 * 
	 * Sends a new elevator request made at a Floor to the Scheduler.
	 * 
	 * Format:
	 *     Byte 0: UtilityInformation.FLOOR_REQUEST_MODE
	 *     Byte 1: The floor number where the request was made
	 *     Byte 2: Direction that the user wants to travel
	 *     Byte 3: The floor number that the user wants to travel to
	 *     Byte 4: -1
	 *     
	 * @param sourceFloor  Floor number where request was made
	 * @param destFloor    Floor number that the user wants to travel to
	 * @param diRequest    Direction that the user wants to travel to
	 * 
	 * @return None
	 */
	public void sendElevatorRequest(int sourceFloor, int destFloor, UtilityInformation.ElevatorDirection diRequest) {
		// Construct a message to send with data from given parameters
		byte[] msg = new byte[REQUEST_SIZE];
		msg[0] = UtilityInformation.FLOOR_REQUEST_MODE;
		msg[1] = (byte) sourceFloor;
		msg[2] = (byte) diRequest.ordinal();
		msg[3] = (byte) destFloor;
		msg[4] = UtilityInformation.END_OF_MESSAGE;

		// Send the signal
		System.out.println("Sending elevator request...");
		sendSignal(msg, UtilityInformation.SCHEDULER_PORT_NUM, addressToSend);
		System.out.println("Elevator request sent...");
	}
	
	/**
	 * sendErrorOccursMessage
	 * 
	 * Sends a new error occurance request to the Scheduler.
	 * 
	 * Format:
	 *     {ERROR_MESSAGE_MODE, Type of error, Elevator number where the error occured, -1}
	 *     
	 * @param type The type of error to send
	 * @param elevatorNum  The elevator number where the error occured.
	 * 
	 * @return None
	 */
	public void sendErrorOccursMessage(UtilityInformation.ErrorType type, int elevatorNum) {
	    // Construct a message to send with data from given parameters
	    byte[] msg = new byte[REQUEST_SIZE];
	    msg[0] = UtilityInformation.ERROR_MESSAGE_MODE;
	    msg[1] = (byte) type.ordinal();
	    msg[2] = (byte) elevatorNum;
	    msg[3] = UtilityInformation.END_OF_MESSAGE;
	    
        // Send the signal
        System.out.println("Sending error occurs message...");
        sendSignal(msg, UtilityInformation.SCHEDULER_PORT_NUM, addressToSend);
        System.out.println("Error occurs message sent...");
	}

	/**
	 * runSubsystem
	 * 
	 * Runs the FloorSubsystem object. 
	 * 
	 * Loops through all elevator requests in the system and sends
	 * the requests to the Scheduler at the proper time. Request 0
	 * is sent at the beginning of execution and subsequent requests
	 * are made the proper amount of ms after the previous request.
	 * 
	 * While not sending a request, the system is waiting for elevator
	 * location updates from the Scheduler.
	 * 
	 * @param  None
	 * 
	 * @return None
	 */
	public void runSubsystem() {
	    while (true) {
	        byte data[] = this.getNextRequest().getData();

	        // Get the floor number and elevator number
	        byte floorNum = data[1];
	        
	        // Request currently does not contain the elevator number,
	        // so hardcode the value to 1 for now.
	        int elevatorNum = data[2]; 
	        
	        // Get the direction of the elevator
	        UtilityInformation.ElevatorDirection dir = UtilityInformation.ElevatorDirection.values()[data[3]];

	        // Propagate the information through all Floor
	        // objects in the FloorSubsystem
	        for (Floor currFloor : floors) {
	            currFloor.updateElevatorLocation(elevatorNum, floorNum, dir);
	        }
	        
	        System.out.println(this.toString());
	    }
	}

	/**
	 * sendSignal
	 * 
	 * Sends the given message to the port number through
	 * the given address. Information about the created
	 * packet is printed before sending.
	 * 
	 * @param msg          byte[] consisting of the message to send
	 * @param portNumber   The port to send the created packet to
	 * @param address      The address to send the packet through
	 * 
	 * @return None
	 */
	public void sendSignal(byte[] msg, int portNumber, InetAddress address) {
	    // Create the DatagramPacket
		sendPacket = new DatagramPacket(msg, msg.length, address, portNumber);

		// Print out info about the message being sent
		System.out.println("FloorSubsystem: Sending packet:");
		System.out.println("To host: " + sendPacket.getAddress());
		System.out.println("Destination host port: " + sendPacket.getPort());
		int len = sendPacket.getLength();
		System.out.println("Length: " + len);
		System.out.print("Containing (as bytes): ");
		System.out.println(Arrays.toString(sendPacket.getData()));

		// Send the packet
		try {
			sendSocket.send(sendPacket);
		} catch (IOException e) {
			e.printStackTrace();
			this.teardown();
			System.exit(1);
		}

		System.out.println("FloorSubsystem: Packet sent.\n");
	}
	
	/**
	 * getListOfFloors
	 * 
	 * Get the list of Floor objects that exist
	 * in the FloorSubsystem.
	 * 
	 * @param  None
	 * 
	 * @return ArrayList<Floor> containing all of the Floor objects in the system
	 */
	public ArrayList<Floor> getListOfFloors(){
	    return(floors);
	}
	
	/**
	 * startFloorThread
	 * 
	 * Starts all floor threads in that the controller owns
	 * 
	 * @param  None
	 * 
	 * @return None
	 */
	public void startFloorThreads() {
	    for (Thread thread : floorThreads) {
	        thread.start();
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
	 * @return void
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
				floorController.sendConfigurationSignal(floorController.getNumElevators(),
						floorController.getNumFloors());
			} else if (val == UserInterface.ReturnVals.NEW_TEST_FILE) {
				// If a new test file was entered, parse the file
				floorController.parseInputFile(ui.getTestFile());
				floorController.startFloorThreads();
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

	/**
	 * getRequests
	 * 
	 * Return the current list of requests
	 * 
	 * @param  None
	 * 
	 * @return ArrayList<Integer[]> List containg all request arrays
	 */
    public ArrayList<Integer[]> getRequests() {
        ArrayList<Integer[]> allRequests = new ArrayList<Integer[]>();
        ArrayList<Integer[]> tempRequests;
        
        for (Floor floor : floors) {
            tempRequests = floor.getServiceRequests();
            for (Integer[] req : tempRequests) {
                allRequests.add(req);
            }
        }
        
        return(allRequests);
    }
}


class GUI implements Runnable {
	private JTextArea outputArea;
	
	
	public GUI() {
		JFrame mainFrame = new JFrame("Floor Debug Window");
		mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	
		Container contentPane = mainFrame.getContentPane();
		contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));
		
		JPanel debugPanel = new JPanel();
		debugPanel.setLayout(new GridLayout(1, 1));
		
		outputArea = new JTextArea();
		outputArea.setEditable(false);
		JScrollPane scrollPane = new JScrollPane(outputArea); // May need to be a field
		scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		
		debugPanel.add(scrollPane);
		
		contentPane.add(debugPanel);
		
		mainFrame.setSize(800, 600);
		mainFrame.setResizable(false);
		mainFrame.pack();
		mainFrame.setVisible(true);
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		
	}
}

