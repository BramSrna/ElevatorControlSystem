import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;

public class Scheduler extends ServerPattern {

	// State machine
	enum State {
		START, WAITING, READING_MESSAGE, RESPONDING_TO_MESSAGE, END
	}

	// External and internal events
	enum Event {
		MESSAGE_RECIEVED, CONFIG_MESSAGE, BUTTON_PUSHED_IN_ELEVATOR, FLOOR_SENSOR_ACTIVATED, FLOOR_REQUESTED,
		MOVE_ELEVATOR, TEARDOWN, CONFIRM_CONFIG, ELEVATOR_STOPPED, ELEVATOR_ERROR, SEND_ELEVATOR_ERROR,
		FIX_ELEVATOR_ERROR, FIX_DOOR_ERROR
	}

	private DatagramSocket sendSocket = null;
	private DatagramPacket sendPacket;
	private ArrayList<UtilityInformation.ElevatorDirection> elevatorDirection;
	private State currentState;
	private byte numElevators;
	private long messageRecieveTime;

	private SchedulerAlgorithm algor;
	
	private long totalArrivalSensorTime;
	private int numArrivalSensorTimeSamples;

	/**
	 * Scheduler
	 * 
	 * Constructor
	 * 
	 * Create a new Scheduler object
	 */
	public Scheduler() {
		super(UtilityInformation.SCHEDULER_PORT_NUM, "Scheduler");

		algor = new SchedulerAlgorithm((byte) 0);

		elevatorDirection = new ArrayList<UtilityInformation.ElevatorDirection>();

		currentState = State.START;

		try {
			sendSocket = new DatagramSocket();
		} catch (SocketException e1) {
			e1.printStackTrace();
			System.exit(1);
		}
	}
	
	/**
     * runScheduler
     * 
     * Runs the scheduler object. Receives and handle packets.
     * 
     * @param None
     * 
     * @return None
     */
    public void runSheduler() {
        while (true) {
            DatagramPacket nextReq = this.getNextRequest();
            messageRecieveTime = System.nanoTime();
            eventOccured(Event.MESSAGE_RECIEVED, nextReq);
        }
    }

	/**
	 * Based on an event that occurred in a given state, determine what action needs
	 * to be taken. Also changes the state of the scheduler.
	 * 
	 * @param event
	 * @param packet
	 */
	private void eventOccured(Event event, DatagramPacket packet) {
		switch (currentState) {
		case READING_MESSAGE:
		    currentState = State.RESPONDING_TO_MESSAGE;
		    
		    switch(event) {
		    case CONFIG_MESSAGE:
		        sendConfigPacketToElevator(packet);
                eventOccured(Event.CONFIG_MESSAGE, packet);
                break;
		    case FLOOR_SENSOR_ACTIVATED:
		        extractFloorReachedNumberAndGenerateResponseMessageAndActions(packet);
		        
		        totalArrivalSensorTime += System.nanoTime() - messageRecieveTime;
		        numArrivalSensorTimeSamples += 1;
		        
		        break;
		    case FLOOR_REQUESTED:
		        byte elevatorNum = extractFloorRequestedNumberAndGenerateResponseMessageAndActions(packet);
                currentState = State.READING_MESSAGE;
                kickStartElevator(packet, elevatorNum);                
                break;
		    case TEARDOWN:
		        currentState = State.END;
                sendTearDownMessage(packet);
                break;
		    case CONFIRM_CONFIG:
		        sendConfigConfirmMessage(packet);
                eventOccured(Event.CONFIRM_CONFIG, packet);
                break;
		    case ELEVATOR_STOPPED:
		    	if(checkForFinish() == true) {
		    		sendAllRequestsFinishedMessage(packet);
		    	}
		        break;
		    case ELEVATOR_ERROR:
		        handleError(packet);
                eventOccured(Event.SEND_ELEVATOR_ERROR, packet);
                break;
		    case FIX_ELEVATOR_ERROR:
		        handleElevatorFixMessage(packet);
		        break;
		    case FIX_DOOR_ERROR:
		        handleDoorFixMessage(packet);
		        break;
		    default:
		        System.out.println("Unknown event.");
		        System.exit(1);
		        break;
		    }
			
			currentState = State.WAITING;
			
			break;
		case WAITING:
			if (event.equals(Event.MESSAGE_RECIEVED)) {
				currentState = State.READING_MESSAGE;
				readMessage(packet);
			}
			break;
		case RESPONDING_TO_MESSAGE:
			if (event.equals(Event.MOVE_ELEVATOR) || 
		        event.equals(Event.CONFIG_MESSAGE) || 
		        event.equals(Event.CONFIRM_CONFIG) || 
		        event.equals(Event.SEND_ELEVATOR_ERROR)) {
				currentState = State.WAITING;
			}
			break;
		case START:
			currentState = State.WAITING;
			eventOccured(event, packet);
			break;
		default:
			System.out.println("Should never come here!\n");
			System.exit(1);
			break;

		}
	}

	private void sendAllRequestsFinishedMessage(DatagramPacket packet) {
		byte[] message = {UtilityInformation.ALL_REQUESTS_FINISHED_MODE,
						  UtilityInformation.END_OF_MESSAGE};
		
		sendMessage(message, message.length, packet.getAddress(), UtilityInformation.FLOOR_PORT_NUM);
	}

	private boolean checkForFinish() {
		for (byte i = 0; i < numElevators; i++) {
			for (Request req : algor.getRequests(i)) {
				if ((req.getElevatorPickupTimeFlag() == false) ||
					(req.getElevatorArrivedDestinationTimeFlag() == false)) {
					return(false);
				}
			}
		}
		
		return(true);
		
	}

	/**
	 * Read the message recieved and call the appropriate event
	 * 
	 * @param recievedPacket
	 */
	private void readMessage(DatagramPacket recievedPacket) {
		byte mode = recievedPacket.getData()[UtilityInformation.MODE_BYTE_IND];

		if (mode == UtilityInformation.CONFIG_MODE) { // 0
			eventOccured(Event.CONFIG_MESSAGE, recievedPacket);
		} else if (mode == UtilityInformation.FLOOR_SENSOR_MODE) { // 1
			eventOccured(Event.FLOOR_SENSOR_ACTIVATED, recievedPacket);
		} else if (mode == UtilityInformation.FLOOR_REQUEST_MODE) { // 2
			eventOccured(Event.FLOOR_REQUESTED, recievedPacket);
		} else if (mode == UtilityInformation.ELEVATOR_BUTTON_HIT_MODE) { // 3
			eventOccured(Event.BUTTON_PUSHED_IN_ELEVATOR, recievedPacket);
		} else if (mode == UtilityInformation.TEARDOWN_MODE) { // 7
			eventOccured(Event.TEARDOWN, recievedPacket);
		} else if (mode == UtilityInformation.CONFIG_CONFIRM_MODE) { // 8
			eventOccured(Event.CONFIRM_CONFIG, recievedPacket);
		} else if (mode == UtilityInformation.ELEVATOR_STOPPED_MODE) { // 9
			eventOccured(Event.ELEVATOR_STOPPED, recievedPacket);
		} else if (mode == UtilityInformation.ERROR_MESSAGE_MODE) {
			eventOccured(Event.ELEVATOR_ERROR, recievedPacket);
		} else if (mode == UtilityInformation.FIX_ERROR_MODE) {
			eventOccured(Event.FIX_ELEVATOR_ERROR, recievedPacket);
		} else if (mode == UtilityInformation.FIX_DOOR_MODE) {
			eventOccured(Event.FIX_DOOR_ERROR, recievedPacket);
		} else {
			System.out.println(String.format("Error in readMessage: Undefined mode: %d", mode));
		}
	}
	
	/**
     * Send the confimration from the config message to the Floor
     * 
     * @param packet
     */
    protected void sendConfigConfirmMessage(DatagramPacket packet) {
        sendMessage(packet.getData(), packet.getData().length, packet.getAddress(), UtilityInformation.FLOOR_PORT_NUM);

    }

    /**
     * Setup elevator and floor schematics and also send this information to the
     * Elevator
     * 
     * @param configPacket
     */
    protected void sendConfigPacketToElevator(DatagramPacket configPacket) {
        System.out.println("Sending config file to Elevator...\n");
        setNumElevators(configPacket.getData()[1]);
        sendMessage(configPacket.getData(), configPacket.getData().length, configPacket.getAddress(),
                UtilityInformation.ELEVATOR_PORT_NUM);
    }

    /**
     * Set the number of elevators and all the lists that need to be initialized
     * with the correct number of elevators
     * 
     * @param newNumElevators
     */
    public void setNumElevators(byte newNumElevators) {
        this.numElevators = newNumElevators;
        while (elevatorDirection.size() > numElevators) {
            elevatorDirection.remove(elevatorDirection.size() - 1);
        }
        while (elevatorDirection.size() < numElevators) {
            elevatorDirection.add(UtilityInformation.ElevatorDirection.STATIONARY);
        }
        algor.setNumberOfElevators(numElevators);
    }

	/**
	 * For when someone on a Floor presses the button for an elevator request.
	 * 
	 * @param recievedData
	 */
	protected byte extractFloorRequestedNumberAndGenerateResponseMessageAndActions(DatagramPacket recievedPacket) {
		UtilityInformation.ElevatorDirection upOrDown = UtilityInformation.ElevatorDirection.values()[recievedPacket.getData()[2]];

		Request tempRequest = new Request(messageRecieveTime, recievedPacket.getData()[1], recievedPacket.getData()[3],
				upOrDown);

		byte elevatorNum = algor.elevatorRequestMade(tempRequest);

		// Update elevator destinations
		LinkedHashSet<Byte> elevatorDestinations = algor.getDestinations(elevatorNum);
		if (elevatorDestinations.size() > 0) {
			byte[] destinationFloor = {UtilityInformation.SEND_DESTINATION_TO_ELEVATOR_MODE,
                    				   elevatorDestinations.iterator().next(), 
                    				   elevatorNum, 
                    				   UtilityInformation.END_OF_MESSAGE };
			sendMessage(destinationFloor, destinationFloor.length, recievedPacket.getAddress(),
					UtilityInformation.ELEVATOR_PORT_NUM);
		}

		return (elevatorNum);
	}
	
	protected void kickStartElevator(DatagramPacket packet, byte elevatorNum) {
	    if (algor.getStopElevator(elevatorNum)) {
            currentState = State.READING_MESSAGE;
            byte[] newData = {UtilityInformation.FLOOR_SENSOR_MODE, 
                              algor.getCurrentFloor(elevatorNum),
                              elevatorNum,
                              -1 };
            packet.setData(newData);
            eventOccured(Event.FLOOR_SENSOR_ACTIVATED, packet);
        }
	}

	/**
	 * Move the elevator, and trigger the move elevator event
	 * 
	 * @param packet
	 */
	private void moveToFloor(DatagramPacket packet) {
		byte elevatorNum = packet.getData()[2];

		if (algor.somewhereToGo(elevatorNum)) {
		    changeDoorState(packet, UtilityInformation.DoorState.CLOSE);

			if (algor.whatDirectionShouldTravel(elevatorNum).equals(UtilityInformation.ElevatorDirection.DOWN)) {
				sendElevatorInDirection(packet, UtilityInformation.ElevatorDirection.DOWN);
			} else if (algor.whatDirectionShouldTravel(elevatorNum).equals(UtilityInformation.ElevatorDirection.UP)) {
			    sendElevatorInDirection(packet, UtilityInformation.ElevatorDirection.UP);
			} else {
				if (packet.getData()[0] != UtilityInformation.ELEVATOR_STOPPED_MODE) {
				    sendElevatorInDirection(packet, UtilityInformation.ElevatorDirection.STATIONARY);
				    changeDoorState(packet, UtilityInformation.DoorState.OPEN);
				}
			}
		} else {
			if (packet.getData()[0] != UtilityInformation.ELEVATOR_STOPPED_MODE) {
			    sendElevatorInDirection(packet, UtilityInformation.ElevatorDirection.STATIONARY);
			    changeDoorState(packet, UtilityInformation.DoorState.OPEN);
			}
		}

		eventOccured(Event.MOVE_ELEVATOR, packet);
	}
	
	protected void sendElevatorInDirection(DatagramPacket packet, UtilityInformation.ElevatorDirection direction) {
	    byte elevatorNum = packet.getData()[2];
        byte[] message = {UtilityInformation.ELEVATOR_DIRECTION_MODE, 
                          algor.getCurrentFloor(elevatorNum), 
                          elevatorNum,
                          (byte) direction.ordinal(), 
                          UtilityInformation.END_OF_MESSAGE};
        
        System.out.println(String.format("Sending elevator %s... \n", direction.toString()));
        sendMessage(message, message.length, packet.getAddress(), UtilityInformation.ELEVATOR_PORT_NUM);
        sendMessage(message, message.length, packet.getAddress(), UtilityInformation.FLOOR_PORT_NUM);
        
        elevatorDirection.set(elevatorNum, direction);
        
        if (direction.equals(UtilityInformation.ElevatorDirection.STATIONARY)) {
            algor.setStopElevator(elevatorNum, true);
        }
	}
	
	protected void changeDoorState(DatagramPacket packet, UtilityInformation.DoorState state) {
	    byte elevatorNum = packet.getData()[2];
        byte[] closeDoor = {UtilityInformation.ELEVATOR_DOOR_MODE, 
                            (byte) state.ordinal(), 
                            elevatorNum,
                            UtilityInformation.END_OF_MESSAGE};
        sendMessage(closeDoor, closeDoor.length, packet.getAddress(), UtilityInformation.ELEVATOR_PORT_NUM);
	}

	/**
	 * For when the Floor sends message to Scheduler saying it has arrived.
	 * 
	 * @param recievedPacket
	 */
	private void extractFloorReachedNumberAndGenerateResponseMessageAndActions(DatagramPacket recievedPacket) {
		byte floorNum = recievedPacket.getData()[1];
		byte elevatorNum = recievedPacket.getData()[2];
		algor.elevatorHasReachedFloor(floorNum, elevatorNum);

		// Stop elevator if necessary
		if (algor.getStopElevator(elevatorNum)) {
		    sendElevatorInDirection(recievedPacket, UtilityInformation.ElevatorDirection.STATIONARY);
			changeDoorState(recievedPacket, UtilityInformation.DoorState.OPEN);
			
			// Set the time in the requests
			long updatedTime = System.nanoTime();
			updateRequestTimes(algor.getRequests(elevatorNum), updatedTime);
		}

		// Continue moving elevator
		moveToFloor(recievedPacket);
	}
	
	private void handleError(DatagramPacket packet) {
	    byte errorType = packet.getData()[1];
	    byte elevatorNum = packet.getData()[2];
        sendMessage(packet.getData(), 
                    packet.getData().length, 
                    packet.getAddress(),
                    UtilityInformation.ELEVATOR_PORT_NUM);
        
        if ((errorType == UtilityInformation.ErrorType.DOOR_WONT_OPEN_ERROR.ordinal()) || 
            (errorType == UtilityInformation.ErrorType.DOOR_WONT_CLOSE_ERROR.ordinal())){
            algor.pauseElevator(elevatorNum);
        } else if (errorType == UtilityInformation.ErrorType.ELEVATOR_STUCK_ERROR.ordinal()) {
            algor.stopUsingElevator(elevatorNum);
        } else {
            System.out.println("Error in Shceduler: Unknown error type.");
        }
	}

	/**
	 * handleDoorFixMessage
	 * 
	 * Handle a message stating that the door was fixed. Tells the algorithm to
	 * start using that elevator again.
	 * 
	 * @param recievedPacket The received packet containing the message
	 * 
	 * @return None
	 */
	private void handleDoorFixMessage(DatagramPacket recievedPacket) {
		algor.resumeUsingElevator(recievedPacket.getData()[1]);
	}

	/**
	 * This is from the Floor to the Elevator for the fatal error.
	 * 
	 * @param receivedPacket
	 */
	private void handleElevatorFixMessage(DatagramPacket receivedPacket) {
		byte elevatorNum = receivedPacket.getData()[2];
		sendMessage(receivedPacket.getData(), receivedPacket.getData().length, receivedPacket.getAddress(),
				UtilityInformation.ELEVATOR_PORT_NUM);
		algor.resumeUsingElevator(elevatorNum);
	}

	private void updateRequestTimes(ArrayList<Request> request, long updatedTime) {
		for (Request temp : request) {
			if (temp.getElevatorPickupTimeFlag() && 
			   (temp.getElevatorPickupTime() == -1)) {
				temp.setElevatorPickupTime(updatedTime);
			}
			
			if (temp.getElevatorArrivedDestinationTimeFlag() && 
			   (temp.getElevatorArrivedDestinationTime() == -1)) {
				temp.setElevatorArrivedDestinationTime(updatedTime);
			}
		}
	}

	/**
	 * Send a message
	 * 
	 * @param responseData
	 * @param packetLength
	 * @param destAddress
	 * @param destPortNum
	 */
	private void sendMessage(byte[] responseData, int packetLength, InetAddress destAddress, int destPortNum) {
		sendPacket = new DatagramPacket(responseData, packetLength, destAddress, destPortNum);

		// Print out info about the message being sent
		System.out.println("Scheduler: Sending packet:");
		System.out.println("To host: " + sendPacket.getAddress());
		System.out.println("Destination host port: " + sendPacket.getPort());
		int len = sendPacket.getLength();
		System.out.println("Length: " + len);
		System.out.print("Containing (as bytes): ");
		System.out.println(Arrays.toString(sendPacket.getData()));

		try {
			System.out.println("Scheduler is sending data...");
			sendSocket.send(sendPacket);
		} catch (IOException e) {
			System.out.println("Send socket failure!");
			e.printStackTrace();
			System.exit(1);
		}

		System.out.println("Scheduler: Packet sent.\n");
	}
	
    /**
     * If the tear down message was sent from Floor, relay the message to Elevator
     * and shut everything down.
     * 
     * @param packet
     */
    private void sendTearDownMessage(DatagramPacket packet) {
        byte[] tearDown = { UtilityInformation.TEARDOWN_MODE, UtilityInformation.END_OF_MESSAGE };
        sendMessage(tearDown, tearDown.length, packet.getAddress(), UtilityInformation.ELEVATOR_PORT_NUM);
        System.out.println("\n\nTEARING DOWN!\n\n");
        socketTearDown();
        printTimingInformation();
        System.exit(0);
    }
	
	private void printTimingInformation() {
		long totalElevatorBtnTime = 0;
		int numElevatorBtnTimeSamples = 0;
		
		long totalFloorBtnTime = 0;
		int numFloorBtnTimeSamples = 0;
		
		long time = -1;
		long timeOfReq;

		for (byte i = 0; i < numElevators; i++) {
			for (Request req : algor.getRequests(i)) {
				timeOfReq = req.getElevatorRequestTime();
				
				time = req.getElevatorPickupTime();				
				if (time != -1) {
					totalElevatorBtnTime += (time - timeOfReq);
					numElevatorBtnTimeSamples += 1;
				}
				
				time = req.getElevatorArrivedDestinationTime();
				if (time != -1) {
					totalFloorBtnTime += (time - timeOfReq);
					numFloorBtnTimeSamples += 1;
				}
			}
		}
		
		System.out.println(String.format("Average Arrival Sensors Interface Time (ns): %d", 
				totalArrivalSensorTime / numArrivalSensorTimeSamples));
		
		if (numElevatorBtnTimeSamples != 0) {
			System.out.println(String.format("Average Elevators Buttons Interface Time (ns): %d", 
					totalElevatorBtnTime / numElevatorBtnTimeSamples));
		} else {
			System.out.println("Not enough samples to determine Elevator Buttons Interface time.");
		}
		
		if (numFloorBtnTimeSamples != 0) {
			System.out.println(String.format("Average Floor Buttons Interface Time (ns): %d", 
					totalFloorBtnTime / numFloorBtnTimeSamples));
		} else {
			System.out.println("Not enough samples to determine Floor Buttons Interface time.");
		}
		
	}

	/**
     * Close send and reciever sockets
     */
    protected void socketTearDown() {
        if (sendSocket != null) {
            sendSocket.close();
        }

        super.teardown();
    }

	/**
	 * main
	 * 
	 * Main method
	 * 
	 * Creates and runs a new scheduler
	 * 
	 * @param args
	 * 
	 * @return None
	 */
	public static void main(String[] args) {
		Scheduler scheduler = new Scheduler();
		scheduler.runSheduler();
	}
}