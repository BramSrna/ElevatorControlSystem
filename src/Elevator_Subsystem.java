import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.*;

/*
 * SYSC 3303 Elevator Group Project
 * Elevator_Subsystem.java
 * @ author Samy Ibrahim 
 * @ student# 101037927
 * @ version 1
 * 
 * The elevator subsystem consists of the buttons and lamps inside of the elevator used to select floors and indicate the
 * floors selected, and to indicate the location of the elevator itself. The elevator subsystem is also used to operate the
 * motor and to open and close the doors. Each elevator has its own elevator subsystem. 
 * 
 * For the purpose of this project, the elevator subsystem listens for packets from the scheduler to control the motor
 * and to open the doors. The elevator subsystem also has to monitor the floor subsystem for destination requests
 * (button presses inside of the elevator car, rather than button presses at the floors) from the input file. Button presses
 * are to be rerouted to the scheduler system. Lamp (floor indications) from button pushes do not have to originate
 * from the scheduler. Rather, when the elevator subsystem detects a button request, it can then light the
 * corresponding lamp. When the elevator reaches a floor, the scheduling subsystem signals the elevator subsystem to
 * turn the lamp of
 * 
 * Last Edited March 7, 2019.
 */
public class Elevator_Subsystem extends ServerPattern {
	// ArrayList containing all the elevators being used by an instance of the system.
	public ArrayList<Elevator> allElevators = new ArrayList<>();
	
	// The number of elevators and floors, initialized to 0
	// These are set during the initial config
	private int numberOfElevators = 0;
	private int numberOfFloors = 0;
	
	// The destination floor
	private int destinationFloor;

	// The current elevator number being accessed
	private static byte currentElevatorToWork = 0;

	// Datagram Packets and Sockets for sending and receiving data
	private DatagramPacket sendPacket, receivePacket;
	private DatagramSocket sendSocket;

	// Information for System
	private InetAddress schedulerIP;
	private int schedulerPort = 420;
	
	private ArrayList<LinkedList<Elevator.Action>> nextActions;
	
    private ArrayList<ArrayList<Long>> frequencyTimes;
    private ArrayList<ArrayList<Long>> executionDurationTimes;

	// USED ENUMS:
	// State machine states
	public enum State {
		ANALYZING_MESSAGE, CURRENTLY_MOVING, ARRIVE_AT_FLOOR
	}

	// All events taking place in the elevator
	public enum Event {
		CONFIG_RECIEVED, BUTTON_PUSHED_IN_ELEVATOR, ELEVATOR_MOVING, STOP_ELEVATOR, UPDATE_DEST, OPEN_DOOR, 
		CLOSE_DOOR, ISSUE_OCCURING, ISSUE_FIXED, DOOR_ISSUE
	}

	// Start off stationary
	public static State currentState = State.ANALYZING_MESSAGE;

	// General Constructor for Elevator Subsystem class.
	public Elevator_Subsystem() {
	    super(UtilityInformation.ELEVATOR_PORT_NUM, "Elevator_Subsystem");
	    
        frequencyTimes = new ArrayList<ArrayList<Long>>();      
        for (int i = 0; i < 14; i++) {
            frequencyTimes.add(new ArrayList<Long>());
        }
        
        executionDurationTimes = new ArrayList<ArrayList<Long>>();
        for (int i = 0; i < 14; i++) {
            executionDurationTimes.add(new ArrayList<Long>());
        }
	    
	    nextActions = new ArrayList<LinkedList<Elevator.Action>>();
	    
		try {
			schedulerIP = InetAddress.getLocalHost();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		try {
			// Construct a Datagram socket and bind it to any available port on the local
			// host machine. This socket will be used to
			// send UDP Datagram packets.
			sendSocket = new DatagramSocket();
		} catch (SocketException se) {
			se.printStackTrace();
			System.exit(1);
		}
	}
	

	/*
	 * This method sends an array of bytes to a specific Ip address and port number.
	 * 
	 * @param data the array of bytes being sent
	 * 
	 * @param IP the target IP address for the destination of the data
	 * 
	 * @param port the port number on the destination computer
	 */
	public void sendData(byte[] data, InetAddress IP, int port) {
		sendPacket = new DatagramPacket(data, data.length, IP, schedulerPort);
		System.out.println("Elevator: Sending packet:");
		System.out.println("To host: " + sendPacket.getAddress());
		System.out.println("Destination host port: " + sendPacket.getPort());
		int len = sendPacket.getLength();
		System.out.println("Length: " + len);
		System.out.print("Containing: ");
		System.out.println(Arrays.toString(data)); // or could print "s"
		try {
			sendSocket.send(sendPacket);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		System.out.println("Elevator: Packet sent.\n");
	}
	/*
	 * Returns true if the System is in an error state and
	 */
	public boolean checkERROR(byte[] data) {
		// Check if elevator in error state, elevator subsystem in error state, and if the message is not the fixer.
		if(allElevators.get(data[2]).isInErrorState() == true && data[0]!=11) {
			return true;
		}else {
			return false;
		}
	}
	

	/*
	 * The way the receiving aspect of the elevator was designed is to have the
	 * system constantly receive messages, nothing else, and then decode the
	 * messages. First, the elevator system receives a configuration messages
	 * informing it how many elevators and floors to consider. After that, it is
	 * still waiting to receive data from the scheduler. If the first byte of the
	 * messages matches with one of the valid modes (0,3,4,5) then an action is
	 * performed, if not, it is considered invalid.
	 */
	public DatagramPacket receiveData() {
		receivePacket = this.getNextRequest();
		byte data[] = receivePacket.getData();
		
		// ALL messages except config have the current elevator in data[2]
		if(data[0]!= UtilityInformation.CONFIG_MODE) {
			currentElevatorToWork = data[2];
			
			// CHECK IF THE ELEVATOR CORRESPONDING TO THE REQUEST IS IN AN ERROR STATE
			if(this.checkERROR(data)) {
				return(receivePacket); // leave method if it is an error state	
			} // If the message received while in error state is not the fixing message, then simply
				// return and receive the next message (doing nothing)
			
		}
		
		String check = this.validPacket(data);
		if (check.equals("invalid")) {
			System.out.println("Invalid packet received");
			System.exit(1);
		}

		// Depending on the type of message, a certain event will be raised
		if (check.equals("config")) {
			currentState = State.ANALYZING_MESSAGE;
			eventOccured(Event.CONFIG_RECIEVED, receivePacket, check, data);
		}
		if (check.equals("button clicked")) {
			currentState = State.ANALYZING_MESSAGE;
			eventOccured(Event.BUTTON_PUSHED_IN_ELEVATOR, receivePacket, check, data);
		}
		if (check.equals("destination")) {
			currentState = State.ANALYZING_MESSAGE;
			eventOccured(Event.UPDATE_DEST, receivePacket, check, data);
		}
		if (check.equals("go up") | check.equals("go down") | check.equals("stay")) {
			currentState = State.CURRENTLY_MOVING;
			
			if (check.equals("stay")) {
				eventOccured(Event.STOP_ELEVATOR, receivePacket, check, data);
			}
			
			if (check.equals("go up") | check.equals("go down")) {
				eventOccured(Event.ELEVATOR_MOVING, receivePacket, check, data);
			}
		}
		if (check.equals("open door") | check.equals("close door")) {
			// if not, the elevator is not at the floor yet
			if (currentState.equals(State.ARRIVE_AT_FLOOR)) {
				eventOccured(Event.ELEVATOR_MOVING, receivePacket, check, data);
			}
		}
		
		if(check.equals("door stuck")) {
			currentState = State.ANALYZING_MESSAGE;
			eventOccured(Event.DOOR_ISSUE, receivePacket, check, data);
		}
		if(check.equals("elevator stuck")){
			currentState = State.ANALYZING_MESSAGE;
			eventOccured(Event.ISSUE_OCCURING, receivePacket, check, data);
		}
		if(check.equals("issue fixed")) {
			currentState = State.ANALYZING_MESSAGE;
			eventOccured(Event.ISSUE_FIXED, receivePacket, check, data);
		}
		
		return(receivePacket);
	}

	/*
	 * Separately analyzing the different events that occur in the elevator subsystem.
	 * 
	 * @param event the event that occurred
	 * 
	 * @param packet DatagramPacket received and analyzed
	 * 
	 * @param valid the type of message received (valid messages only)
	 * 
	 * @param data array of bytes received and analyzed
	 */
	public void eventOccured(Event event, DatagramPacket packet, String valid, byte[] data) {
		switch (currentState) {
		case ANALYZING_MESSAGE:
			if (event.equals(Event.CONFIG_RECIEVED)) {
				this.performAction("config", data);
			}
			if (event.equals(Event.BUTTON_PUSHED_IN_ELEVATOR)) {
				this.performAction("button clicked", data);
			}
			if (event.equals(Event.UPDATE_DEST)) {
				performAction("destination", data);
			}
			if(event.equals(Event.ISSUE_OCCURING)) {
				performAction("error", data);
			}
			if(event.equals(Event.DOOR_ISSUE)) {
				performAction("door issue", data);
			}
			if(event.equals(Event.ISSUE_FIXED)) {
				performAction("issue fixed", data);
			}
			break;

		case CURRENTLY_MOVING:
			if (event.equals(Event.ELEVATOR_MOVING)) {
				if (valid.equals("go up")) {
					this.performAction("go up", data);
				}
				if (valid.equals("go down")) {
					this.performAction("go down", data);
				}
			}
			if (event.equals(Event.STOP_ELEVATOR)) {
				this.performAction("stop", data);
			}
			break;

		case ARRIVE_AT_FLOOR: // represents the sensor in the elevator
			if (event.equals(Event.OPEN_DOOR)) {
				performAction("open door", data);
			}
			if (event.equals(Event.CLOSE_DOOR)) {
				performAction("close door", data);
			}
			currentState = State.ANALYZING_MESSAGE;
			break;
			
		}
	}

	/*
	 * Takes the validated String values from valid packet and performs the actions
	 * necessary
	 * 
	 * @param str the string indicating what type of message was received
	 * 
	 * @param data the array of bytes received to be decoded
	 */
	public void performAction(String str, byte[] data) {
		// Setting up our "Building" with configurable number of elevators and floors
		if (str.equals("config")) {
			numberOfElevators = data[1];
			numberOfFloors = data[2];
			
			configSubsystem(numberOfFloors, numberOfElevators);
		}

		if (str.equals("open door")) {
			addActionToQueue(currentElevatorToWork, Elevator.Action.OPEN_DOOR);
		}
		if (str.equals("close door")) {
			addActionToQueue(currentElevatorToWork, Elevator.Action.CLOSE_DOOR);
		}
		if (str.equals("go up")) {
			addActionToQueue(currentElevatorToWork, Elevator.Action.MOVE_UP);
		}
		if (str.equals("go down")) {
			addActionToQueue(currentElevatorToWork, Elevator.Action.MOVE_DOWN);
		}
		if (str.equals("stop")) {
			allElevators.get(currentElevatorToWork).turnOffDestButton(destinationFloor);
			addActionToQueue(currentElevatorToWork, Elevator.Action.STOP);
		}
		// getting destination from scheduler for each input
		if (str.equals("destination")) {
			destinationFloor = data[1];
			currentElevatorToWork = data[2];
			allElevators.get(currentElevatorToWork).turnOnDestButton(destinationFloor);
		}
		if(str.equals("error")) {
			System.out.print(currentElevatorToWork + " ");
			addActionToQueue(currentElevatorToWork, Elevator.Action.BROKEN);
		}
		if(str.equals("door issue")) {
			if (data[1] == UtilityInformation.ErrorType.DOOR_STUCK_ERROR.ordinal()) {
			    addActionToQueue(currentElevatorToWork, Elevator.Action.DAMAGED);	
			}
		}
		if(str.equals("issue fixed")) {
			System.out.print(currentElevatorToWork + " ");
			addActionToQueue(currentElevatorToWork, Elevator.Action.FIXED);
		}

	}
	
	/**
	 * sendElevatorDoorFixedMessage
	 * 
	 * Send a message to the scheduler stating that the door on the elevator
	 * has been fixed.
	 * 
	 * Format:
	 *     {FIX_DOOR MODE, elevator number, -1}
	 *     
	 * @param elevatorNum  The number of the elevator that has been fixed
	 * 
	 * @return None
	 */
	public void sendElevatorDoorFixedMessage(int elevatorNum) {
	    byte[] issConf = {UtilityInformation.FIX_DOOR_MODE, (byte) elevatorNum, -1};
        this.sendData(issConf, schedulerIP, schedulerPort);
	}
	
	/**
	 * sendFloorSensorMessage
	 * 
	 * Send a message to the the scheduler that a floor sensor has been activated.
	 * 
	 * Format:
	 *     {FLOOR_SENSOR_MODE, floor number, elevator number, -1}
	 *     
	 * @param elevatorNum  The elevator shaft number when the sensor has occured
	 * 
	 * @return None
	 */
    public void sendFloorSensorMessage(int elevatorNum) {
        byte[] returnMessage = { UtilityInformation.FLOOR_SENSOR_MODE,
                (byte) allElevators.get(elevatorNum).getCurrentFloor(),
                (byte) allElevators.get(elevatorNum).getElevatorNumber(), -1 };
        this.sendData(returnMessage, schedulerIP, schedulerPort);
    }

	/*
	 * Method to validate the form of the array of bytes received from the Scheduler
	 * 
	 * @param data array of bytes received and analyzed
	 */
	public String validPacket(byte[] data) {
		if (data[0] == UtilityInformation.CONFIG_MODE) {
			return "config";
		} else if (data[0] == UtilityInformation.ELEVATOR_BUTTON_HIT_MODE) {// send the number of floor clicked
			return "button clicked";
		} else if (data[0] == UtilityInformation.ELEVATOR_DIRECTION_MODE) {
			currentElevatorToWork = data[2];
			
			byte checkFloor = data[1];
			
			if (checkFloor != allElevators.get(currentElevatorToWork).getCurrentFloor()) {				
				return "ignore";
			}
			
			byte moveDirection = data[3];
			
			if (moveDirection == 0) {
				return "stay";
			} // stop
			if (moveDirection == 1) {
				return "go up";
			}
			if (moveDirection == 2) {
				return "go down";
			}
			
			return "invalid";
		} else if (data[0] == UtilityInformation.ELEVATOR_DOOR_MODE) {
			byte doorState = data[1];
			currentElevatorToWork = data[2];
			if (doorState == 1) {
				return "open door";
			}
			if (doorState == 0) {
				return "close door";
			}
		} else if (data[0] == UtilityInformation.SEND_DESTINATION_TO_ELEVATOR_MODE) {
			return "destination";
		} else if (data[0] == UtilityInformation.TEARDOWN_MODE) {
			System.out.println("Tear-Down Mode");
			teardown();
			
			printTimingInformation();
			printFrequencyInformation();
			
			System.exit(0);
		} 
		
		
		// ITERATION 3 ASK THEM WHAT BYTE IS THE TYPE OF ERROR
		else if (data[0] == UtilityInformation.ERROR_MESSAGE_MODE) {
			if (data[1] == UtilityInformation.ErrorType.DOOR_STUCK_ERROR.ordinal()) {	
				System.out.println("Message from Elevator " + currentElevatorToWork + ": DOOR STUCK");
				return "door stuck";
			} else if(data[1] == UtilityInformation.ErrorType.ELEVATOR_STUCK_ERROR.ordinal()) {
				System.out.println("Message from Elevator " + currentElevatorToWork + ": I AM STUCK");
				return "elevator stuck";
			}
		}
		
		else if( data[0] == UtilityInformation.FIX_ERROR_MODE) {
			return "issue fixed";
		}
		
		
		return "invalid"; // anything else is an invalid request.
	}

	/*
	 * Main method for starting the elevator. 
	 */
	public static void main(String[] args) {
		Elevator_Subsystem elvSub = new Elevator_Subsystem();
		elvSub.runElevatorSubsystem();
		
	}
	
	/**
	 * runElevatorSubsystem
	 * 
	 * Runs the subsystem
	 * Repeats the following loop indefinitely:
	 *     Start Timer
	 *     Receive packet
	 *     Handle Packet
	 *     Stop Timer
	 *     Save Time
	 *     Display current information
	 *     
	 * @input  None
	 * 
	 * @return void
	 */
	public void runElevatorSubsystem() {
	 // receive the config message
        for(;;) {
            long startTime = System.nanoTime();
            
            DatagramPacket nextReq = receiveData();
            
            long finishTime = System.nanoTime();
            saveTimes(startTime, finishTime, nextReq.getData()[0]);
            
            allElevators.get(currentElevatorToWork).display();
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
            writer = new PrintWriter("timing information/timing_elevator.txt", "UTF-8");
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
            writer = new PrintWriter("timing information/frequency_elevator.txt", "UTF-8");
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
     * addActionToQueue
     * 
     * synchronized
     * 
     * Adds the given action type to the queue for the given elevator
     * 
     * @param elevatorNumber    Number of elevator to add the action for
     * @param stateToAdd        Next action to add to queue
     * 
     * @return  void
     */
	public synchronized void addActionToQueue(int elevatorNumber, Elevator.Action actionToAdd) {
		nextActions.get(elevatorNumber).add(actionToAdd);
		
		notifyAll();
	}

	/**
	 * getNextActionForElevator
	 * 
	 * synchronized
	 * 
	 * Returns the next action in the queue for the given elevator.
	 * Waits until an action is available,
	 * 
	 * @param elevatorNumber   Elevator number to get the next action for
	 * 
	 * @return Action  The next action for the elevator
	 */
	public synchronized Elevator.Action getNextActionForElevator(int elevatorNumber) {
		while (nextActions.get(elevatorNumber).isEmpty()) {
			try {
				wait();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		Elevator.Action nextAction = nextActions.get(elevatorNumber).remove();
		
		notifyAll();
		
		return(nextAction);
	}

	/**
	 * getNumFloors
	 * 
	 * Returns the number of floors that the subsystem is configured to handle.
	 * 
	 * @param  None
	 * 
	 * @return int Number of floors
	 */
    public int getNumFloors() {
        return(numberOfFloors);
    }
    
    /**
     * teardown
     * 
     * Tears down the Elevator subsystem object
     * 
     * @param   None
     * 
     * @return  void
     */
    public void teardown() {
        sendPacket = null;
        receivePacket = null;
        sendSocket.close();
        super.teardown();
    }
    
    /**
     * configSubsystem
     * 
     * Configures the subsystem to use the given number of floors and elevators.
     * 
     * @param numFloors     New number of floors for the system
     * @param numElevators  New number of elevators for the system
     * 
     * @return  void
     */
    public void configSubsystem(int numFloors, int numElevators) {
        // Based on the config message, set up the elevators and their lights.
        for (int i = 0; i < numElevators; i++) {
            Elevator hold = new Elevator(this, i, numFloors);
            // add to elevator subsystem ArrayList of elevators
            allElevators.add(hold);
            nextActions.add(new LinkedList<Elevator.Action>());
        }
        // allButtons = new lampState[numberOfFloors];
        byte[] response = { UtilityInformation.CONFIG_CONFIRM_MODE, 1, -1 };
        this.sendData(response, schedulerIP, schedulerPort);
        
        for (Elevator ele : allElevators) {
            Thread t = new Thread(ele);
            t.start();
        }
    }
}