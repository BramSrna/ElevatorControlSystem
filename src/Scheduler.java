import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;

public class Scheduler extends ServerPattern {

	// State machine
	public enum State {
		START, WAITING, READING_MESSAGE, RESPONDING_TO_MESSAGE, END
	}

	// External and internal events
	public enum Event {
		MESSAGE_RECIEVED, CONFIG_MESSAGE, BUTTON_PUSHED_IN_ELEVATOR, FLOOR_SENSOR_ACTIVATED, FLOOR_REQUESTED,
		MOVE_ELEVATOR, TEARDOWN, CONFIRM_CONFIG, ELEVATOR_ERROR, SEND_ELEVATOR_ERROR,
		FIX_ELEVATOR_ERROR, FIX_DOOR_ERROR
	}

	private DatagramSocket sendSocket = null;
	private DatagramPacket sendPacket;
	private ArrayList<UtilityInformation.ElevatorDirection> elevatorDirection;
	private State currentState;
	private byte numElevators;
	private long messageRecieveTime;

	private SchedulerAlgorithm algor;
	
	private ArrayList<ArrayList<Long>> frequencyTimes;
	private ArrayList<ArrayList<Long>> executionDurationTimes;

	/**
	 * Scheduler
	 * 
	 * Constructor
	 * 
	 * Create a new Scheduler object
	 */
	public Scheduler() {
		super(UtilityInformation.SCHEDULER_PORT_NUM, "Scheduler");
		
		frequencyTimes = new ArrayList<ArrayList<Long>>();		
		for (int i = 0; i < 14; i++) {
		    frequencyTimes.add(new ArrayList<Long>());
		}
		
		executionDurationTimes = new ArrayList<ArrayList<Long>>();
		for (int i = 0; i < 14; i++) {
		    executionDurationTimes.add(new ArrayList<Long>());
		}

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
            
            byte mode = nextReq.getData()[0];            
            messageRecieveTime = System.nanoTime();
            
            eventOccured(Event.MESSAGE_RECIEVED, nextReq);
            
            long finishTime = System.nanoTime();
            saveTimes(messageRecieveTime, finishTime, mode);
            
            printInfo();
        }
    }
    
    /**
     * printInfo
     * 
     * Prints the requests currently assigned to each elevator
     * 
     * @param   None
     * 
     * @return  void
     */
    public void printInfo() {
    	for (byte i = 0; i < numElevators; i++) {
    		String toPrint = "";
    		
    		toPrint += String.format("Elevator %d: ", i);
    		
    		for (Request req : algor.getRequests(i)) {
    			toPrint += String.format("Request: %d %d ", req.getSourceFloor(), req.getDestinationFloor());
    		}
    		
    		System.out.println(toPrint);
    	}
    }
    
    /**
     * saveTimes
     * 
     * Save the given timing information for the given message type.
     * 
     * @param startTime    Start time of request handler
     * @param finishTime   End Time of request handler
     * @param mode The mode of the message being handled
     * 
     * @return void
     */
    public void saveTimes(long startTime, long finishTime, byte mode) {
	    frequencyTimes.get(mode).add(startTime);
	    executionDurationTimes.get(mode).add(finishTime - startTime);
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
		        
		        if(checkForFinish() == true) {
                    sendAllRequestsFinishedMessage(packet);
                }
		        
		        break;
		    case FLOOR_REQUESTED:
		        byte elevatorNum = extractFloorRequestedNumberAndGenerateResponseMessageAndActions(packet);
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

	/**
	 * sendAllRequestsFinishedMessage
	 * 
	 * Send a message to the FloorSubsystem signifying that
	 * all current requests have been completed.
	 * 
	 * @param packet   The received DatagramPacket that triggered this method call
	 * 
	 * @return void
	 */
	private void sendAllRequestsFinishedMessage(DatagramPacket packet) {
		byte[] message = {UtilityInformation.ALL_REQUESTS_FINISHED_MODE,
						  UtilityInformation.END_OF_MESSAGE};
		
		sendMessage(message, message.length, packet.getAddress(), UtilityInformation.FLOOR_PORT_NUM);
	}

	/**
	 * checkForFinish
	 * 
	 * Check if all current requests have been completed.
	 * Return whether this is true or not.
	 * 
	 * @param  None
	 * 
	 * @return boolean True if all requests are completed, false otherwise
	 */
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
	
	/**
	 * kickStartElevator
	 * 
	 * Checks if the given elevator is stopped.
	 * If the elevator is stopped, then a floor sensor message is triggered.
	 * 
	 * @param packet   The DatagramPacket that caused this method to be called
	 * @param elevatorNum  The number of the elevator to kick start
	 * 
	 * @return void
	 */
	protected void kickStartElevator(DatagramPacket packet, byte elevatorNum) {
	    if (algor.getStopSignalSent(elevatorNum)) {
            byte[] newData = {UtilityInformation.FLOOR_SENSOR_MODE, 
                              algor.getCurrentFloor(elevatorNum),
                              elevatorNum,
                              -1 };
            packet.setData(newData);
            moveToFloor(packet);
        }
	    
	    algor.setStopSignalSent(elevatorNum, false);
	}

	/**
	 * Move the elevator, and trigger the move elevator event
	 * 
	 * @param packet
	 */
	private void moveToFloor(DatagramPacket packet) {
		byte elevatorNum = packet.getData()[2];

		if (algor.somewhereToGo(elevatorNum)) {
		    UtilityInformation.ElevatorDirection dir = algor.whatDirectionShouldTravel(elevatorNum);
		    
		    if (dir.equals(UtilityInformation.ElevatorDirection.STATIONARY)) {
		        changeDoorState(packet, UtilityInformation.DoorState.OPEN);
		    } else {
		        changeDoorState(packet, UtilityInformation.DoorState.CLOSE);
		    }
		    
		    sendElevatorInDirection(packet, dir);
		    
		    if (dir.equals(UtilityInformation.ElevatorDirection.STATIONARY)) {
		        // Set the time in the requests
                long updatedTime = System.nanoTime();
                updateRequestTimes(algor.getRequests(elevatorNum), updatedTime);
            }
		} else {
		    if (!algor.getStopSignalSent(elevatorNum)) {
    		    changeDoorState(packet, UtilityInformation.DoorState.OPEN);
    		    sendElevatorInDirection(packet, UtilityInformation.ElevatorDirection.STATIONARY);
    		    algor.setStopSignalSent(elevatorNum, true);
    		    
    		    // Set the time in the requests
                long updatedTime = System.nanoTime();
                updateRequestTimes(algor.getRequests(elevatorNum), updatedTime);
		    }
		}

		eventOccured(Event.MOVE_ELEVATOR, packet);
	}
	
	/**
	 * sendElevatorInDirection
	 * 
	 * Send the elevator designated by the given DatagramPacket in
	 * the given direction.
	 * 
	 * @param packet   The DatagramPacket containing the number of the elevator to move
	 * @param direction    The direction to move the elevator in
	 * 
	 * @return void
	 */
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
	
	/**
	 * changeDoorState
	 * 
	 * Change the door state of the elevator designated
	 * by the given DatagramPacket to the given state.
	 * 
	 * @param packet   The DatagramPacket that triggered this method to be called
	 * @param state    The new state of the Elevator's door
	 * 
	 * @return void
	 */
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

		// Continue moving elevator
		moveToFloor(recievedPacket);
	}
	
	/**
	 * handleError
	 * 
	 * Handle the error contained in the given packet.
	 * 
	 * @param packet   The DatagramPacket containing the error
	 * 
	 * @return void
	 */
	private void handleError(DatagramPacket packet) {
	    byte errorType = packet.getData()[1];
	    byte elevatorNum = packet.getData()[2];
        sendMessage(packet.getData(), 
                    packet.getData().length, 
                    packet.getAddress(),
                    UtilityInformation.ELEVATOR_PORT_NUM);
        
        if (errorType == UtilityInformation.ErrorType.DOOR_STUCK_ERROR.ordinal()) {
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

	/**
	 * updateRequestTimes
	 * 
	 * Updates the times of all requests based on their flag values
	 * and whether or not their times have already been set.
	 * 
	 * @param request  ArrayList of all requests to update
	 * @param updatedTime  The time to update the requests to
	 * 
	 * @return void
	 */
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
        printFrequencyInformation();
        System.exit(0);
    }
	
    /**
     * printTimingInformation
     * 
     * Prints all measured timing information.
     * This includes the execution times of handlers for all message types
     *  
     * @param   None
     * 
     * @return  void
     */
	private void printTimingInformation() {
		PrintWriter writer = null;
        
        try {
            writer = new PrintWriter("timing information/timing_scheduler.txt", "UTF-8");
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        for (int i = 0; i < executionDurationTimes.size(); i++) {
            if (i == 0) {
                writer.println("CONFIG_MODE");
            } else if (i == 1) {
                writer.println("FLOOR_SENSOR_MODE");
            } else if (i == 2) {
                writer.println("FLOOR_REQUEST_MODE");
            } else if (i == 3) {
                writer.println("ELEVATOR_BUTTON_HIT_MODE");
            } else if (i == 4) {
                writer.println("ELEVATOR_DIRECTION_MODE");
            } else if (i == 5) {
                writer.println("ELEVATOR_DOOR_MODE");
            } else if (i == 6) {
                writer.println("SEND_DESTINATION_TO_ELEVATOR_MODE");
            } else if (i == 7) {
                writer.println("TEARDOWN_MODE");
            } else if (i == 8) {
                writer.println("CONFIG_CONFIRM_MODE");
            } else if (i == 9) {
                writer.println("ERROR_MESSAGE_MODE");
            } else if (i == 10) {
                writer.println("FIX_ERROR_MODE");
            } else if (i == 11) {
                writer.println("FIX_DOOR_MODE");
            } else if (i == 12) {
                writer.println("ALL_REQUESTS_FINISHED_MODE");
            }
            
            for (Long time : executionDurationTimes.get(i)) {
                writer.println(time);
            }
            
            writer.println("");
        }
        
        writer.println("Finish Request Times: ");
        
        for (byte i = 0; i < numElevators; i++) {
        	for (Request req : algor.getRequests(i)) {
        		writer.println(req.getElevatorArrivedDestinationTime());
        	}
        }
        
        writer.println("");
        
        writer.close();   	
	}
	
    /**
     * printFrequencyInformation
     * 
     * Prints all measured timing information.
     * This includes the frequency times of handlers for all message types
     *  
     * @param   None
     * 
     * @return  void
     */
    private void printFrequencyInformation() {
        PrintWriter writer = null;
        
        try {
            writer = new PrintWriter("timing information/frequency_scheduler.txt", "UTF-8");
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        for (int i = 0; i < frequencyTimes.size(); i++) {
            if (i == 0) {
                writer.println("CONFIG_MODE");
            } else if (i == 1) {
                writer.println("FLOOR_SENSOR_MODE");
            } else if (i == 2) {
                writer.println("FLOOR_REQUEST_MODE");
            } else if (i == 3) {
                writer.println("ELEVATOR_BUTTON_HIT_MODE");
            } else if (i == 4) {
                writer.println("ELEVATOR_DIRECTION_MODE");
            } else if (i == 5) {
                writer.println("ELEVATOR_DOOR_MODE");
            } else if (i == 6) {
                writer.println("SEND_DESTINATION_TO_ELEVATOR_MODE");
            } else if (i == 7) {
                writer.println("TEARDOWN_MODE");
            } else if (i == 8) {
                writer.println("CONFIG_CONFIRM_MODE");
            } else if (i == 9) {
                writer.println("ERROR_MESSAGE_MODE");
            } else if (i == 10) {
                writer.println("FIX_ERROR_MODE");
            } else if (i == 11) {
                writer.println("FIX_DOOR_MODE");
            } else if (i == 12) {
                writer.println("ALL_REQUESTS_FINISHED_MODE");
            }
            
            for (Long time : frequencyTimes.get(i)) {
                writer.println(time);
            }
            
            writer.println("");
        }
        
        writer.close();     
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