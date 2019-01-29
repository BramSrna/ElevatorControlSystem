package groupProject;

import java.io.*;
import java.net.*;
import java.util.*;
/*
 * SYSC 3303 Elevator Group Project
 * Client.java
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
 * Message 1 from Scheduler
 * 
 * PORT Numbers:
 * Elevator port 69
 * 	Receiving from Scheduler on Port 420
 * 
 * Last Edited January 25,2019
 * 
 */
public class elevatorSubsystem {
	DatagramPacket sendPacket, receivePacket;
	DatagramSocket sendSocket, receiveSocket;

	// The elevator car number
	private int elevatorNumber = 1;
	// The current floor the elevator is on
	int currentFloor = 1;
	// The number of elevators and floors, initialized to 0
	// These are set during the intial config
	private int numberOfElevators = 0;
	private int numberOfFloors = 0;
	
	// The destination floor 
	int destination; 
	// The lamps indicate the floor(s) which will be visited by the elevator
	private lampState[] allButtons;
	
	private enum lampState {
		OFF, ON
	}

	// Motor only says go up or go down

	public elevatorSubsystem() {
		try {
			// Construct a datagram socket and bind it to any available
			// port on the local host machine. This socket will be used to
			// send UDP Datagram packets.
			sendSocket = new DatagramSocket();

			// Construct a datagram socket and bind it to port 420 (the Scheduler)
			// on the local host machine. This socket will be used to
			// receive UDP Datagram packets.
			receiveSocket = new DatagramSocket(420);

			// to test socket timeout (2 seconds)
			// receiveSocket.setSoTimeout(2000);
		} catch (SocketException se) {
			se.printStackTrace();
			System.exit(1);
		}
	}

	public int getElevatorNumber() {return this.elevatorNumber;}
	public int getCurrentFloor() {return this.currentFloor;}
	public int getNumberOfElevatorNumber() {return this.numberOfElevators;}
	public int getNumberOfFloor() {return this.numberOfFloors;}

	// Interior Elevator Display
	public void display() {
		System.out.println("Elevator " + this.getElevatorNumber());
		System.out.println("Floor # " + this.getCurrentFloor());
		// ideally we want lights lighting up by whatever floor it is, not sure how we
		// wil implement that yet
		
		// Get message from scheduler containing destinationFloor (e.g. floor 5)
		// then set the lampState of the corresponsing button (#5 on the elevator) to 'ON'?
		
		//Same with turning if off 
		// Do we just do:
		// allbuttons[currentFloor] = lampState.OFF, in openDoor()?, or in stopMotor()?, or...
	}
	
	// Make the elevator move (up)
	public void goUp() {
		System.out.println("Elevator Moving Up");
	}
	// Make the elevator move (down)
	public void goDown() {
		System.out.println("Elevator Moving Down");
	}

	// Stop the elevator
	public void stopMotor() {
		// Sleep for 2 seconds to simulate elevator stopping, before door opens
		// Do we miss messages if we sleep?
		// TODO Remove
		/*
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			e.printStackTrace();
			System.exit(1);
		}
		*/
		
		//TURN OFF LIGHTS AT CURRENT FLOOR
		
		System.out.println("Elevator Stopped Moving");
	}
	
	// Open the elevator door
	public void openDoor() {
		// Sleep for 1.5 seconds to simulate the door opening
		// Do we miss messages if we sleep?
		// TODO Remove
		/*
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			e.printStackTrace();
			System.exit(1);
		}
		*/
		System.out.println("Elevator Door Opened");
	}
	
	// Close the elevator door
	public void closeDoor() {
		// Sleep for 1.5 seconds to simulate the door closing
		// Do we miss messages if we sleep?
		// TODO Remove
		/*
		try {
			Thread.sleep(1500);
		} catch (InterruptedException e) {
			e.printStackTrace();
			System.exit(1);
		}
		*/
		System.out.println("Elevator Door Closed");
	}
	
	// called within exchangeData() method to perform required actions
	public void performAction(String str, byte[] data) {
		this.display();
		if (str.equals("config")) {
			numberOfElevators = data[1];
			numberOfFloors = data[2];

			// for now this array is from 0 to num of Floors and encoded (on/off
			// respectively), you can do it however u prefer
			for (int i = 0; i < numberOfFloors; i++) {
				allButtons[i] = lampState.OFF; // currently making everything OFF
			}
		}
		if (str.equals("start/stop")) {		
			currentFloor = data[1];
			byte moveDirection = data[2]; // 0-stay, 1-up, 2-down
			if(moveDirection == 1) {
				this.goUp();
			}else if(moveDirection == 2) {
				this.goDown();
			}else {
				//stay on the same floor
			}
		}
		if (str.equals("open/close")) {
			if (data[1] == 0) {
				this.closeDoor();// Close the elevator door
			} else if (data[1] == 1) {
				this.openDoor();// Open the elevator doorv
			}
		}
		// Do we need this? We're not sending any messages
		if (str.equals("button clicked")) {
			// button clicked by user (in the elevator), send that to scheduler
			byte clickedFloor = 5; // ex. 5, not sure how we will do this
			byte[] clickedButton = {3, clickedFloor, -1};
			try {
				sendPacket = new DatagramPacket(clickedButton, clickedButton.length, InetAddress.getLocalHost(), 420);
			} catch (UnknownHostException e) {
				e.printStackTrace();
				System.exit(1);
			}

			System.out.println("Elevator: Sending packet:");
			System.out.println("To host: " + sendPacket.getAddress());
			System.out.println("Destination host port: " + sendPacket.getPort());
			int len = sendPacket.getLength();
			System.out.println("Length: " + len);
			System.out.print("Containing: ");
			System.out.println(Arrays.toString(clickedButton)); // or could print "s"

			try {
				sendSocket.send(sendPacket);
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}

			System.out.println("Elevator: Packet sent.\n");
		} 
		else {
			// Invalid case
			System.out.println("Invalid Packet Format");
			System.exit(1); // invalid
		}
		if (str.equals("destination")) {
			destination = data[1];

			//HERE TURN ON LAMPS
		}
	}

	// Method to validate the form of the array of bytes received from the Scheduler
	public String validPacket(byte[] data) {
		if (data[0] == 0) {
			return "config";
		// Do we need this?
		} else if (data[0] == 3) {
			// send the number of floor clicked
			return "button clicked";
		} else if (data[0] == 4) {
			return "start/stop";
		} else if (data[0] == 5) {
			return "close/open";
		}else if(data[0] == 6) {
			return "destination";
		}
		return "invalid"; // anything else is an invalid request.
	}

	public void exchangeData() {
		/*
		 * The way this was designed is to constantly receive messages, nothing else, and then decoding the messages and using what
		 * is needed. First (not verified yet just relying on Floor), the elevator system receives a configuration messages 
		 * informing it how many elevators and floors to consider. After that, it is still waiting to receive data from the 
		 * scheduler. If the first byte of the messages matches with one of the valid modes (0,3,4,5) then an action is performed, 
		 * if not, it is considered invalid.
		 * 
		 * Expected Communication:
		 * 	1) Receive configuration once
		 * 	Do Forever:
		 * 		2) Receive what floor to go to from the scheduler (and go there)
		 * 		3) Receive when to open/close the door from the scheduler
		 */

		// Receiving Data from the Scheduler to Control the Motor and Open the doors
		this.display(); // automatically show display

		byte data[] = new byte[100];
		receivePacket = new DatagramPacket(data, data.length);
		System.out.println("Elevator: Waiting for Packet.\n");

		// Block until a datagram packet is received from receiveSocket.
		try {
			System.out.println("Waiting..."); // so we know we're waiting
			receiveSocket.receive(receivePacket);
		} catch (IOException e) {
			System.out.print("IO Exception: likely:");
			System.out.println("Receive Socket Timed Out.\n" + e);
			e.printStackTrace();
			System.exit(1);
		}

		// Process the received datagram.
		System.out.println("Elevator: Packet received:");
		System.out.println("From host: " + receivePacket.getAddress());
		System.out.println("Host port: " + receivePacket.getPort());
		int len = receivePacket.getLength();
		System.out.println("Length: " + len);
		System.out.print("Containing: ");
		System.out.println(Arrays.toString(data) + "\n");

		// Slow things down (wait 5 seconds)
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			e.printStackTrace();
			System.exit(1);
		}

		String request = this.validPacket(data); // returns the status of the packet information
		// Is it possible we miss the first config message?
		this.performAction(request, data); // if the packet is valid, an action will be performed
	}

	public static void main(String args[]) {
		//for now we only have one elevator
		elevatorSubsystem elevator1 = new elevatorSubsystem();
		for (;;) {
			// always wait for a message from the scheduler
			elevator1.exchangeData();
		}

	}
}
