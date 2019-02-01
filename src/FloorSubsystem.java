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
import java.util.Timer;
import java.util.TimerTask;

public class FloorSubsystem {
	// Sockets and packets used for UDP
	private DatagramPacket sendPacket, receivePacket;
	private DatagramSocket sendReceiveSocket;

	private int bottomFloor; // Lowest possible floor
	private int topFloor; // Highest possible floor
	private int numFloors; // Number of floors that the elevator services

	private int numElevators; // The current number of elevators

	// Size of service requests
	private final int CONFIG_SIZE = 4;
	private final int CONFIG_REC_SIZE = 100;

	private final int REQUEST_SIZE = 5;

	private final int ELEVATOR_UPDATE_SIZE = 100;

	private final int TEARDOWN_SIZE = 10;

	// Valid ranges for the number of
	// floors and number of elevators
	private static final int MIN_NUM_FLOORS = 1;
	private static final int MAX_NUM_FLOORS = 1000;

	private static final int MIN_NUM_ELEVATORS = 1;
	private static final int MAX_NUM_ELEVATORS = 1;

	// List of service requests parsed from the input file
	// Sorted in order of time that requests are made
	private ArrayList<Integer[]> serviceRequests;

	// List of existing floor objects
	private ArrayList<Floor> floors;

	// Address to send messages to
	private InetAddress addressToSend;

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
		serviceRequests = new ArrayList<Integer[]>();

		floors = new ArrayList<Floor>();

		this.setNumElevators(numElevators);
		this.setNumFloors(numFloors);

		// Initialize the DatagramSocket
		try {
			sendReceiveSocket = new DatagramSocket(UtilityInformation.FLOOR_PORT_NUM);
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
		if ((newNumFloors < MIN_NUM_FLOORS) || (newNumFloors > MAX_NUM_FLOORS)) {
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
	 * Sets the number of elevators to the new amount. Checks that the new number is
	 * within the valid range. Propagets the new information to all of the Floor
	 * objects
	 * 
	 * @param newNumElevators The new number of elevators
	 * 
	 * @return void
	 */
	public void setNumElevators(int newNumElevators) {
		if ((newNumElevators < MIN_NUM_ELEVATORS) || (newNumElevators > MAX_NUM_ELEVATORS)) {
			System.out.println("Error: Elevator value is outside of valid range.");
			System.exit(1);
		}

		this.numElevators = newNumElevators;

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
			System.out.println("Error: File could not be found: " + pathToFile);
			e.printStackTrace();
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
			System.exit(1);
		}

		// Parse the current line
		// Add the request
		// Go to the next line
		while (currLine != null) {
			// Split the line at spaces
			String[] info = currLine.split(" ");

			// Get all important parts of data
			String timeStr = info[timeInd];
			String startFloorStr = info[startFloorInd];
			String directionStr = info[directionFloorInd];
			String finalFloorStr = info[endFloorInd];

			int hourInt = 0;
			int minInt = 0;
			int secInt = 0;
			int milliSecInt = 0;
			int startFloorInt = 0;
			int finalFloorInt = 0;
			UtilityInformation.ElevatorDirection directionEnum;

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

			// Get the time of request in milliseconds
			milliSecInt += secInt * 1000;
			milliSecInt += minInt * 60 * 1000;
			milliSecInt += hourInt * 60 * 60 * 1000;

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

			directionEnum = UtilityInformation.ElevatorDirection.valueOf(directionStr.toUpperCase());

			// Find the floor where the request was made,
			// and make the request
			for (Floor floor : floors) {
				if (floor.getFloorNumber() == startFloorInt) {
					floor.createElevatorRequest(milliSecInt, directionEnum, finalFloorInt);
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

		System.out.println("Finished parsing test file.");
	}

	/**
	 * addRequest
	 * 
	 * Adds a request to the list of requests with the given information.
	 * 
	 * The requests use the following format: 1st byte = Hour request was made 2nd
	 * byte = Minute request was made 3rd byte = Second request was made 4th byte =
	 * Millisecond request was made 5th byte = Floor where request was made 6th byte
	 * = Direction user wants to travel (0 = Down, 1 = Up) 7th byte = Floor where
	 * user wants to travel
	 * 
	 * @param hrOfReq    : Hour that request was made
	 * @param minOfReq   : Minute that request was made
	 * @param secOfReq   : Second that request was made
	 * @param msOfReq    : Millisecond that request was made
	 * @param startFloor : Floor where request was made
	 * @param dirPressed : Direction that used wants to travel
	 * @param finalFloor : Floor that user wants to travel to
	 * 
	 * @return void
	 */
	public void addElevatorRequest(int timeOfReq, int startFloor, UtilityInformation.ElevatorDirection dirPressed,
			int finalFloor) {
		// Check that the given floors are valid
		if ((startFloor < bottomFloor) || (startFloor > topFloor) || (finalFloor < bottomFloor)
				|| (finalFloor > topFloor)) {
			System.out.println("Error: Invalid request. One or both floors given is invalid.");
			System.exit(1);
		}

		// Check that the direction is valid for the floor
		if (((startFloor == bottomFloor) && (dirPressed == UtilityInformation.ElevatorDirection.DOWN))
				|| ((startFloor == topFloor) && (dirPressed == UtilityInformation.ElevatorDirection.UP))) {
			System.out.println("Error: Invalid request. Direction invalid for given floor.");
			System.exit(1);
		}

		// Check that the start + end floors match the given direction
		if (((dirPressed == UtilityInformation.ElevatorDirection.DOWN) && (finalFloor > startFloor))
				|| ((dirPressed == UtilityInformation.ElevatorDirection.UP) && (finalFloor < startFloor))) {
			System.out.println("Error: Invalid request. " + "Given floor numbers do not match desired direction.");
			System.exit(1);
		}

		// Formulate byte array
		Integer request[] = new Integer[REQUEST_SIZE];

		int timeInd = 0;
		int startFloorInd = 1;
		int dirInd = 2;
		int finalFloorInd = 3;

		// Set all of the values
		request[timeInd] = timeOfReq;
		request[startFloorInd] = startFloor;
		request[dirInd] = dirPressed.ordinal();
		request[finalFloorInd] = finalFloor;

		// Add it to the list of requests
		if (serviceRequests.isEmpty()) {
			// If empty, just add the request
			serviceRequests.add(request);
		} else {
			// If not empty add it to the proper spot in the list
			Integer currReq[];
			int i = 0;
			while (i < serviceRequests.size()) {
				currReq = serviceRequests.get(i);

				// Check if current request should be after new request
				if (request[timeInd] < currReq[timeInd]) {
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
	 * Returns the range of valid number of floor values. Returns the range as an
	 * array of 2 values. Format: Byte 0 - Minimum value Byte 1 - Maximum value
	 * 
	 * @return int[] First byte is minimum value Second byte is maximum value
	 */
	public static int[] getValidFloorValueRange() {
		int validRange[] = new int[2];

		validRange[0] = MIN_NUM_FLOORS;
		validRange[1] = MAX_NUM_FLOORS;

		return (validRange);
	}

	/**
	 * getValidElevatorValueRange
	 * 
	 * Static method
	 * 
	 * Returns the range of valid number of elevator values. Returns the range as an
	 * array of 2 values. Format: Byte 0 - Minimum value Byte 1 - Maximum value
	 * 
	 * @return int[] First byte is minimum value Second byte is maximum value
	 */
	public static int[] getValidElevatorValueRange() {
		int validRange[] = new int[2];

		validRange[0] = MIN_NUM_ELEVATORS;
		validRange[1] = MAX_NUM_ELEVATORS;

		return (validRange);
	}

	/**
	 * getRequests
	 * 
	 * Return the current list of requests.
	 * 
	 * @return ArrayList<Integer[]> : The current list of requests
	 */
	public ArrayList<Integer[]> getRequests() {
		return (serviceRequests);
	}

	/**
	 * teardown
	 * 
	 * Sends a teardown signal and then closes all open sockets.
	 * 
	 * @return void
	 */
	public void teardown() {
		sendTeardownSignal();
		sendReceiveSocket.close();
	}
	
	public String toString() {
		System.out.println("-------------------------HERE---------------");
		String toReturn = "";

		for (Floor currFloor : floors) {
			toReturn += currFloor.toString();
			toReturn += "\n";
		}

		return (toReturn);
	}

	public int getNumFloors() {
		return numFloors;
	}

	public int getNumElevators() {
		return numElevators;
	}

	/**
	 * sendTeardownSignal
	 * 
	 * Sends a signal that the program should teardown.
	 * 
	 * Format:
	 * 
	 * @return void
	 */
	public void sendTeardownSignal() {
		// Construct a message to send with data from given parameters
		byte[] msg = new byte[TEARDOWN_SIZE];
		msg[0] = UtilityInformation.TEARDOWN_MODE;
		msg[1] = -1;

		System.out.println("Sending teardown signal...");
		sendSignal(msg, UtilityInformation.SCHEDULER_PORT_NUM, addressToSend);
		System.out.println("Teardown signal sent...");
	}

	/**
	 * sendConfigurationSignal
	 * 
	 * Sends a configuration signal with the number of elevators and elevator shaft
	 * number.
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
		msg[3] = -1;

		System.out.println("Sending configuration signal...");
		sendSignal(msg, UtilityInformation.SCHEDULER_PORT_NUM, addressToSend);
		System.out.println("Configuration signal sent...");

		System.out.println("Waiting for response to configuration signal...");
		waitForSignal(CONFIG_REC_SIZE);
		System.out.println("Respone to configuration received.");
	}

	public void sendElevatorRequest(int sourceFloor, int destFloor, UtilityInformation.ElevatorDirection diRequest) {
		// Construct a message to send with data from given parameters
		byte[] msg = new byte[REQUEST_SIZE];
		msg[0] = UtilityInformation.FLOOR_REQUEST_MODE;
		msg[1] = (byte) sourceFloor;
		msg[2] = (byte) diRequest.ordinal();
		msg[3] = (byte) destFloor;
		msg[4] = -1;

		System.out.println("Sending elevator request...");
		sendSignal(msg, UtilityInformation.SCHEDULER_PORT_NUM, addressToSend);
		System.out.println("Elevator request sent...");
	}

	public void runSubsystem() {
		Timer timer = new Timer();
		for (int i = 0; i < serviceRequests.size(); i++) {
			System.out.println(this.toString());

			Integer currReq[] = serviceRequests.get(i);

			int startFloor = currReq[1];
			int endFloor = currReq[3];
			int dir = currReq[2];

			sendElevatorRequest(startFloor, endFloor, UtilityInformation.ElevatorDirection.values()[dir]);

			int timeUntilNextRequest = 0;

			if (i < serviceRequests.size() - 1) {
				Integer nextReq[] = serviceRequests.get(i + 1);

				timeUntilNextRequest += nextReq[0] - currReq[0];

			} else {
				timeUntilNextRequest = 10 * 1000;
			}

			timer.schedule(new TimerTask() {
				@Override
				public void run() {
					while (true) {
						waitForElevatorUpdate();
					}
				}
			}, timeUntilNextRequest);
		}
	}

	public void waitForElevatorUpdate() {
		byte data[] = waitForSignal(ELEVATOR_UPDATE_SIZE);

		byte floorNum = data[1];

		UtilityInformation.ElevatorDirection dir = UtilityInformation.ElevatorDirection.values()[data[2]];

		if (dir == UtilityInformation.ElevatorDirection.UP) {
			floorNum++;
		} else {
			floorNum--;
		}

		for (Floor currFloor : floors) {
			currFloor.updateElevatorLocation(1, floorNum, dir);
		}
		
		System.out.println(this.toString());
	}

	public void sendSignal(byte[] msg, int portNumber, InetAddress address) {
		sendPacket = new DatagramPacket(msg, msg.length, address, portNumber);

		// Print out info about the message being sent
		System.out.println("FloorSubsystem: Sending packet:");
		System.out.println("To host: " + sendPacket.getAddress());
		System.out.println("Destination host port: " + sendPacket.getPort());
		int len = sendPacket.getLength();
		System.out.println("Length: " + len);
		System.out.print("Containing (as bytes): ");
		System.out.println(Arrays.toString(sendPacket.getData()));

		try {
			sendReceiveSocket.send(sendPacket);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}

		System.out.println("FloorSubsystem: Packet sent.\n");
	}

	public byte[] waitForSignal(int expectedMsgSize) {
		byte data[] = new byte[expectedMsgSize];
		receivePacket = new DatagramPacket(data, data.length);

		System.out.println("FloorSubsystem: Waiting for response from host...");

		try {
			// Block until a datagram is received via sendReceiveSocket.
			sendReceiveSocket.receive(receivePacket);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}

		// Print out information about the response
		System.out.println("FloorSubsystem: Packet received:");
		System.out.println("From host: " + receivePacket.getAddress());
		System.out.println("Host port: " + receivePacket.getPort());
		int len = receivePacket.getLength();
		System.out.println("Length: " + len);
		System.out.print("Containing (as bytes): ");
		System.out.println(Arrays.toString(data) + "\n");

		return (receivePacket.getData());
	}

	/**
	 * main
	 * 
	 * Static method
	 * 
	 * Main method
	 * 
	 * Creates a new UserInterface class to get input from the user. Uses the input
	 * to control a FloorSubsystem.
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

		System.out.println(floorController.toString());

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
				System.out.println(floorController.toString());
			} else if (val == UserInterface.ReturnVals.NEW_TEST_FILE) {
				// If a new test file was entered, parse the file
				floorController.parseInputFile(ui.getTestFile());
				floorController.runSubsystem();
				System.out.println(floorController.toString());
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
