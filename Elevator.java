package groupProject;

import java.io.*;
import java.net.*;
import java.util.*;

/*
 * SYSC 3303 Elevator Group Project
 * Elevator.java
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
 * Last Edited January 30,2019
 * 
 */
public class Elevator {
	//Datagram Packets and Sockets for sending and receiving data
	DatagramPacket sendPacket, receivePacket;
	DatagramSocket sendSocket, receiveSocket;
	
	// Information for System
	private InetAddress schedulerIP;
	private int schedulerPort = 420;
	
	// The elevator car number
	private int elevatorNumber = 1;
	
	// The current floor the elevator is on
	private int currentFloor = 0;
	
	// The number of elevators and floors, initialized to 0
	// These are set during the initial config
	private int numberOfElevators = 0;
	private int numberOfFloors = 0;
	
	// The destination floor
	private int destinationFloor;
	
	

	// USED ENUMS:
	//Enum for Door
	enum doorState {OPEN, CLOSED}
	private doorState door = doorState.CLOSED;
	
	// Enum for all lights
	enum lampState {OFF, ON}
	// State machine states
	enum State {ANALYZING_MESSAGE, CURRENTLY_MOVING, ARRIVE_AT_FLOOR}
	// All events taking place in the elevator
	enum Event {CONFIG_RECIEVED, BUTTON_PUSHED_IN_ELEVATOR, ELEVATOR_MOVING, STOP_ELEVATOR, UPDATE_DEST, OPEN_DOOR, CLOSE_DOOR}
	
	//Start off stationary
	private State currentState = State.ANALYZING_MESSAGE;
	
	// Final values for all modes (for messages) being exchanged
	private final int CONFIG_MODE = 0;
	private final int CURRFLOOR_MODE = 1;
	private final int BUTTON_CLICKED_MODE = 3;
	private final int MOVE_ELEVATOR_MODE = 4;
	private final int DOOR_MOTOR_MODE = 5;
	private final int DESTINATION_MODE = 6;
	private final int TEARDOWN = 7;
	private final int CONFIG_RESPONSE = 8;
	
	// The lamps indicate the floor(s) which will be visited by the elevator
	private lampState[] allButtons;
	
	public Elevator() {
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
	
	public int getElevatorNumber() {return this.elevatorNumber;}
	public int getCurrentFloor() {return this.currentFloor;}
	public int getNumberOfElevators() {return this.numberOfElevators;}
	public int getNumberOfFloors() {return this.numberOfFloors;}
	
	public void display() {
		// Simply display 
		System.out.println("Elevator " + this.getElevatorNumber());
		System.out.println("Floor # " + this.getCurrentFloor());
		for(int i=0; i<allButtons.length; i++) {
			System.out.println("Floor Number " + i + ": " +allButtons[i]);
		}
	}
	/*
	 * This method sends an array of bytes to a specific Ip address and port number.
	 * 
	 * @param data the array of bytes being sent 
	 * @param IP the target IP address for the destination of the data
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
	 * The way the receiving aspect of the elevator was designed is to have the system constantly receive messages, nothing else, 
	 * and then decode the messages. First, the elevator system receives a configuration messages informing it how many elevators and 
	 * floors to consider. After that, it is still waiting to receive data from the scheduler. If the first byte of
	 * the messages matches with one of the valid modes (0,3,4,5) then an action is
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
	 * Separately analyzing the different events that occur in the system
	 * 
	 * @param event the event that occurred
	 * @param packet DatagramPacket received and analyzed
	 * @param valid the type of message received (valid messages only)
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
	 * Takes the validated String values from valid packet and performs the actions necessary 
	 * 
	 * @param str the string indicating what type of message was received
	 * @param data the array of bytes received to be decoded
	 */
	public void performAction(String str, byte[] data) {
		// Setting up our "Building" with configurable number of elevators and floors
		if (str.equals("config")) {
			numberOfElevators = data[1];
			numberOfFloors = data[2];

			allButtons = new lampState[numberOfFloors];
			byte[] response = { CONFIG_RESPONSE, 1, -1 };
			this.sendData(response, schedulerIP, schedulerPort);
			// adding required buttons to the list of buttons
			for (int i = 0; i < numberOfFloors; i++) {
				allButtons[i] = lampState.OFF; // currently making everything OFF
			}
		}

		if (str.equals("open door")) {
			this.openDoor();
		}
		if (str.equals("close door")) {
			this.closeDoor();
		}
		if (str.equals("go up")) {
			this.goUp();
		}
		if (str.equals("go down")) {
			this.goDown();
		}
		if (str.equals("stop")) {
			this.Stop();
		}

		// Do we need this? We're not sending any messages
		if (str.equals("button clicked")) {

			// button clicked by user (in the elevator), send that to scheduler
			byte clickedFloor = 5; // ex. 5, not sure how we will do this
			byte[] clickedButton = { 3, clickedFloor, -1 };
			this.sendData(clickedButton, schedulerIP, schedulerPort);
		}

		// getting destination from scheduler for each input
		if (str.equals("destination")) {
			destinationFloor = data[1];
			allButtons[destinationFloor] = lampState.ON;
		}
	}
	
	/*
	 * Method to validate the form of the array of bytes received from the Scheduler
	 * 
	 * @param data array of bytes received and analyzed
	 */
	public String validPacket(byte[] data) {
		if (data[0] == CONFIG_MODE) {
			return "config";
		} else if (data[0] == BUTTON_CLICKED_MODE) {// send the number of floor clicked
			return "button clicked";
		} else if (data[0] == MOVE_ELEVATOR_MODE) {
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
		} else if (data[0] == DOOR_MOTOR_MODE) {
			byte doorState = data[1];
			if (doorState == 1) {
				return "open door";
			}
			if (doorState == 0) {
				return "close door";
			}
		} else if (data[0] == DESTINATION_MODE) {
			return "destination";
		} else if (data[0] == TEARDOWN) {
			System.out.println("Tear-Down Mode");
			sendSocket.close();
			receiveSocket.close();
			System.exit(1);
		}
		return "invalid"; // anything else is an invalid request.
	}

	/*
	 * Method to make the elevator move up one floor.
	 */
	public void goUp() {
		System.out.println("Elevator Moving Up One Floor");
		try {
			Thread.sleep(5000); // it takes approximately 5 seconds to go up one floor
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
		}
		currentFloor++;
		byte[] data = { CURRFLOOR_MODE, (byte) currentFloor, -1 };
		currentState = State.ARRIVE_AT_FLOOR;
		System.out.println("Elevator arrives on floor");
		this.sendData(data, schedulerIP, schedulerPort);
	}

	/*
	 * Method to make the elevator move down one floor.
	 */
	public void goDown() {
		System.out.println("Elevator Moving Down One Floor");
		try {
			Thread.sleep(5000); // it takes approximately 5 seconds to go up one floor
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
		}
		currentFloor--;
		byte[] data = { CURRFLOOR_MODE, (byte) currentFloor, -1 };
		System.out.println("Elevator arrives on floor");
		this.sendData(data, schedulerIP, schedulerPort);
	}
	
	/*
	 * Method to make the elevator stop moving.
	 */
	public void Stop() {
		System.out.println("The elevator has stopped moving");
	}

	/*
	 * Method to open the elevator door.
	 */
	public void openDoor() {
		try {
			Thread.sleep(1500); // it takes approximately 1 second for the door to open
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
		}
		System.out.println("Elevator Door Opened");
		door = doorState.OPEN;
		System.out.println("Door: " + door + "on floor: " + currentFloor);
	}

	/*
	 * Method to close the elevator door.
	 */
	public void closeDoor() {
		try {
			Thread.sleep(1500); // it takes approximately 1.5 second for the door to close
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
		}
		System.out.println("Elevator Door Closed");
		door = doorState.CLOSED;
		System.out.println("Door: " + door + "on floor: " + currentFloor);
	}
	
	public static void main(String[] args) {
		// for now we only have one elevator
		Elevator elevator1 = new Elevator();
		for (;;) {
			// always wait for a message from the scheduler
			elevator1.receiveData();
			elevator1.display();
		}
	}
}
