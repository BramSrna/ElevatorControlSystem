

import java.io.IOException;
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
 * Last Edited February 13, 2019
 * 
 */
public class Elevator_Subsystem  {
	// ArrayList containing all the elevators being used by an instance of the system.
	ArrayList<Elevator> allElevators = new ArrayList<>();
	
	// The number of elevators and floors, initialized to 0
	// These are set during the initial config
	private int numberOfElevators = 0;
	private int numberOfFloors = 0;
	
	// The destination floor
	private int destinationFloor;

	// The current elevator number being accesed
	private static byte currentElevatorToWork = 0;

	// Datagram Packets and Sockets for sending and receiving data
	DatagramPacket sendPacket, receivePacket;
	DatagramSocket sendSocket, receiveSocket;

	// Information for System
	private InetAddress schedulerIP;
	private int schedulerPort = 420;

	// USED ENUMS:
	// State machine states
	enum State {
		ANALYZING_MESSAGE, CURRENTLY_MOVING, ARRIVE_AT_FLOOR
	}

	// All events taking place in the elevator
	enum Event {
		CONFIG_RECIEVED, BUTTON_PUSHED_IN_ELEVATOR, ELEVATOR_MOVING, STOP_ELEVATOR, UPDATE_DEST, OPEN_DOOR, CLOSE_DOOR
	}

	// Start off stationary
	static State currentState = State.ANALYZING_MESSAGE;

	// General Constructor for Elevator Subsystem class.
	public Elevator_Subsystem() {
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

			// Construct a Datagram socket and bind it to port 420 (the Scheduler) on the
			// local host machine. This socket will be used to
			// receive UDP Datagram packets.
			receiveSocket = new DatagramSocket(UtilityInformation.ELEVATOR_PORT_NUM);
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
	 * The way the receiving aspect of the elevator was designed is to have the
	 * system constantly receive messages, nothing else, and then decode the
	 * messages. First, the elevator system receives a configuration messages
	 * informing it how many elevators and floors to consider. After that, it is
	 * still waiting to receive data from the scheduler. If the first byte of the
	 * messages matches with one of the valid modes (0,3,4,5) then an action is
	 * performed, if not, it is considered invalid.
	 */
	public void receiveData() {
		byte data[] = new byte[100];
		receivePacket = new DatagramPacket(data, data.length);
		System.out.println("Elevator: Waiting for Packet.\n");

		// Block until a datagram packet is received from receiveSocket.
		try {
			receiveSocket.receive(receivePacket);
		} catch (IOException e) {
			System.out.print("IO Exception: likely:");
			System.out.println("Receive Socket Timed Out.\n" + e);
			e.printStackTrace();
			System.exit(1);
		}
		// Process the received Datagram.
		System.out.println("Elevator: Packet received:");
		System.out.println("From host: " + receivePacket.getAddress());
		System.out.println("Host port: " + receivePacket.getPort());
		int len = receivePacket.getLength();
		System.out.println("Length: " + len);
		System.out.print("Containing: ");
		System.out.println(Arrays.toString(data) + "\n");

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

			// Based on the config message, set up the elevators and their lights.
			for (int i = 0; i < numberOfElevators; i++) {
				Elevator hold = new Elevator(this, i);
				hold.allButtons = new Elevator.lampState[numberOfFloors];
				for (int k = 0; k < numberOfFloors; k++) {
					hold.allButtons[k] = Elevator.lampState.OFF; // currently making everything OFF
				}
				// add to elevator subsystem ArrayList of elevators
				allElevators.add(hold);
			}
			
			// allButtons = new lampState[numberOfFloors];
			byte[] response = { UtilityInformation.CONFIG_CONFIRM, 1, -1 };
			this.sendData(response, schedulerIP, schedulerPort);
		}

		if (str.equals("open door")) {
			allElevators.get(currentElevatorToWork).openDoor();
		}
		if (str.equals("close door")) {
			allElevators.get(currentElevatorToWork).closeDoor();
		}
		if (str.equals("go up")) {
			allElevators.get(currentElevatorToWork).goUp();
		}
		if (str.equals("go down")) {
			allElevators.get(currentElevatorToWork).goDown();
		}
		if (str.equals("stop")) {
			allElevators.get(currentElevatorToWork).Stop();
			allElevators.get(currentElevatorToWork).allButtons[destinationFloor] = Elevator.lampState.OFF;
			byte[] returnMessage = { UtilityInformation.ELEVATOR_STOPPED_MODE,
					(byte) allElevators.get(currentElevatorToWork).currentFloor,
					(byte) allElevators.get(currentElevatorToWork).elevatorNumber, -1 };
			this.sendData(returnMessage, schedulerIP, schedulerPort);
		}

		/*
		 * // Do we need this? We're not sending any messages if
		 * (str.equals("button clicked")) {
		 * 
		 * // button clicked by user (in the elevator), send that to scheduler byte
		 * clickedFloor = 5; // ex. 5, not sure how we will do this byte[] clickedButton
		 * = { 3, clickedFloor, -1 }; this.sendData(clickedButton, schedulerIP,
		 * schedulerPort); }
		 * 
		 * 
		 */
		// getting destination from scheduler for each input
		if (str.equals("destination")) {
			destinationFloor = data[1];
			currentElevatorToWork = data[2];
			allElevators.get(currentElevatorToWork).allButtons[destinationFloor] = Elevator.lampState.ON;
		}

	}
	
	public void sendFloorSensorMessage(int elevatorNum) {
        byte[] returnMessage = { UtilityInformation.FLOOR_SENSOR_MODE,
                (byte) allElevators.get(elevatorNum).currentFloor,
                (byte) allElevators.get(elevatorNum).elevatorNumber, -1 };
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
            currentElevatorToWork = data[3];
            
            int schedCurrFloor = data[1];
            int eleCurrFloor = allElevators.get(currentElevatorToWork).getCurrentFloor();
            
            if (schedCurrFloor != eleCurrFloor) {
                return "ignore";
            } else {
                byte moveDirection = data[2];         
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
            }   
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
			sendSocket.close();
			receiveSocket.close();
			System.exit(1);
		}
		return "invalid"; // anything else is an invalid request.
	}

	/*
	 * Main method for starting the elevator. 
	 */
	public static void main(String[] args) {
		Elevator_Subsystem elvSub = new Elevator_Subsystem();
		// receive the config message
		for(;;) {
			elvSub.receiveData();
			elvSub.allElevators.get(currentElevatorToWork).display();
		}
		
	}
}