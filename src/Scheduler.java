import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Scheduler {

	// State machine
	enum State {
		WAITING, READING_MESSAGE, RESPONDING_TO_MESSAGE
	}

	// External and internal events
	enum Event {
		MESSAGE_RECIEVED, CONFIG_MESSAGE, BUTTON_PUSHED_IN_ELEVATOR, FLOOR_SENSOR_ACTIVATED, FLOOR_REQUESTED,
		MOVE_ELEVATOR, TEARDOWN, CONFIRM_CONFIG

	}

	private DatagramSocket recieveSocket, sendSocket;
	private DatagramPacket recievePacket, sendPacket;
	private List<Byte> floorsToVisit;
	private UtilityInformation.ElevatorDirection elevatorDirection;
	private byte floorElevatorIsCurrentlyOn;
	private State currentState;

	public Scheduler() {
		floorsToVisit = new ArrayList<Byte>();
		elevatorDirection = UtilityInformation.ElevatorDirection.STATIONARY;
		currentState = State.WAITING;
	}

	public static void main(String[] args) {
		Scheduler scheduler = new Scheduler();
		while (true) {
			scheduler.recieveAndSendData();
		}
	}

	/**
	 * Recieve messages and send messages according to the type of message recieved
	 */
	private void recieveAndSendData() {
		try {
			recieveSocket = new DatagramSocket(UtilityInformation.SCHEDULER_PORT_NUM);
		} catch (SocketException e) {
			e.printStackTrace();
			System.exit(1);
		}
		byte[] data = new byte[UtilityInformation.MAX_BYTE_ARRAY_SIZE];
		recievePacket = new DatagramPacket(data, data.length);
		try {
			System.out.println("Scheduler is waiting for data...");
			recieveSocket.receive(recievePacket);
			eventOccured(Event.MESSAGE_RECIEVED, recievePacket);
		} catch (IOException e) {
			System.out.println("Recieve Socket failure!");
			e.printStackTrace();
			System.exit(1);
		}

		socketTearDown();
	}

	/**
	 * Close send and recieve sockets.
	 */
	private void socketTearDown() {
		recieveSocket.close();
		sendSocket.close();
	}

	/**
	 * Based on an event that occurred, determine what action needs to be taken.
	 * Also changes the state if the scheduler.
	 * 
	 * @param event
	 * @param packet
	 */
	private void eventOccured(Event event, DatagramPacket packet) {
		switch (currentState) {
		case READING_MESSAGE:
			if (event.equals(Event.CONFIG_MESSAGE)) {
				sendConfigPacketToElevator(packet);
				currentState = State.RESPONDING_TO_MESSAGE;
				eventOccured(Event.CONFIG_MESSAGE, packet);
			} else if (event.equals(Event.BUTTON_PUSHED_IN_ELEVATOR)) {
				extractElevatorButtonFloorAndGenerateResponseMessageAndActions(packet);
				currentState = State.RESPONDING_TO_MESSAGE;
				moveToFloor(packet);
			} else if (event.equals(Event.FLOOR_SENSOR_ACTIVATED)) {
				extractFloorReachedNumberAndGenerateResponseMessageAndActions(packet);
			} else if (event.equals(Event.FLOOR_REQUESTED)) {
				extractFloorRequestedNumberAndGenerateResponseMessageAndActions(packet);
				currentState = State.RESPONDING_TO_MESSAGE;
				moveToFloor(packet);
			} else if (event.equals(Event.TEARDOWN)) {
				sendTearDownMessage(packet);
			} else if (event.equals(Event.CONFIRM_CONFIG)) {
				sendConfigConfirmMessage(packet);
				currentState = State.RESPONDING_TO_MESSAGE;
				eventOccured(Event.CONFIRM_CONFIG, packet);
			}

			break;
		case WAITING:
			if (event.equals(Event.MESSAGE_RECIEVED)) {
				currentState = State.READING_MESSAGE;
				readMessage(recievePacket);
			}
			break;
		case RESPONDING_TO_MESSAGE:
			if (event.equals(Event.MOVE_ELEVATOR) || event.equals(Event.CONFIG_MESSAGE)
					|| event.equals(Event.CONFIRM_CONFIG)) {
				currentState = State.WAITING;
			}
			break;
		default:
			System.out.println("Should never come here!\n");
			break;

		}
	}

	/**
	 * Send the confirm config message to the Floor.
	 * 
	 * @param packet
	 */
	protected void sendConfigConfirmMessage(DatagramPacket packet) {
		sendMessage(packet.getData(), packet.getData().length, packet.getAddress(), UtilityInformation.FLOOR_PORT_NUM);

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
		System.exit(1);
	}

	/**
	 * Send the initial floor schematics to the elevator for setup
	 * 
	 * @param configPacket
	 */
	protected void sendConfigPacketToElevator(DatagramPacket configPacket) {
		System.out.println("Sending config file to Elevator...\n");
		sendMessage(configPacket.getData(), configPacket.getData().length, configPacket.getAddress(),
				UtilityInformation.ELEVATOR_PORT_NUM);
	}

	/**
	 * Read the message and call the appropriate event
	 * 
	 * @param recievedPacket
	 */
	private void readMessage(DatagramPacket recievedPacket) {
		byte mode = recievedPacket.getData()[0];
		if (mode == UtilityInformation.CONFIG_MODE) {
			eventOccured(Event.CONFIG_MESSAGE, recievedPacket);
		} else if (mode == UtilityInformation.ELEVATOR_BUTTON_HIT_MODE) {
			eventOccured(Event.BUTTON_PUSHED_IN_ELEVATOR, recievedPacket);
		} else if (mode == UtilityInformation.FLOOR_SENSOR_MODE) {
			eventOccured(Event.FLOOR_SENSOR_ACTIVATED, recievedPacket);
		} else if (mode == UtilityInformation.FLOOR_REQUEST_MODE) {
			eventOccured(Event.FLOOR_REQUESTED, recievedPacket);
		} else if (mode == UtilityInformation.TEARDOWN_MODE) {
			eventOccured(Event.TEARDOWN, recievedPacket);
		} else if (mode == UtilityInformation.CONFIG_CONFIRM) {
			eventOccured(Event.CONFIRM_CONFIG, recievedPacket);
		}
	}

	/**
	 * For when someone on a Floor presses the button for an elevator request.
	 * 
	 * @param recievedData
	 */
	protected void extractFloorRequestedNumberAndGenerateResponseMessageAndActions(DatagramPacket recievedPacket) {
		System.out.println("Elevator was requested at: " + recievedPacket.getData()[1] + " in the direction "
				+ recievedPacket.getData()[2] + " with destination " + recievedPacket.getData()[3] + "\n");
		byte[] destinationFloor = { UtilityInformation.SEND_DESTINATION_TO_ELEVATOR_MODE, recievedPacket.getData()[3],
				UtilityInformation.END_OF_MESSAGE };
		sendMessage(destinationFloor, destinationFloor.length, recievedPacket.getAddress(),
				UtilityInformation.ELEVATOR_PORT_NUM);
		floorButtonPressed(recievedPacket.getData()[1]);
		floorButtonPressed(recievedPacket.getData()[3]);
	}

	/**
	 * Move the elevator, and trigger the move elevator event
	 * 
	 * @param packet
	 */
	private void moveToFloor(DatagramPacket packet) {
		if (elevatorDirection.equals(UtilityInformation.ElevatorDirection.STATIONARY) && !floorsToVisit.isEmpty()) {
			closeElevatorDoors(packet);
			if (floorsToVisit.contains(floorElevatorIsCurrentlyOn)) {
				floorsToVisit.remove(floorElevatorIsCurrentlyOn);
			}
			if (elevatorShouldGoUp()) {
				sendElevatorUp(packet);
			} else {
				sendElevatorDown(packet);
			}
		} else if (elevatorDirection.equals(UtilityInformation.ElevatorDirection.UP) && !floorsToVisit.isEmpty()) {
			closeElevatorDoors(packet);
			if (floorsToGoToAbove()) {
				sendElevatorUp(packet);
			} else {
				sendElevatorDown(packet);
			}
		} else if (elevatorDirection.equals(UtilityInformation.ElevatorDirection.DOWN) && !floorsToVisit.isEmpty()) {
			closeElevatorDoors(packet);
			if (floorsToGoToBelow()) {
				sendElevatorDown(packet);
			} else {
				sendElevatorUp(packet);
			}
		} else {
			stopElevator(packet);
			openElevatorDoors(packet);
		}
		eventOccured(Event.MOVE_ELEVATOR, packet);
	}

	/**
	 * Send stop elevator message
	 * 
	 * @param packet
	 */
	protected void stopElevator(DatagramPacket packet) {
		byte[] stopElevator = { UtilityInformation.ELEVATOR_DIRECTION_MODE, floorElevatorIsCurrentlyOn,
				UtilityInformation.ELEVATOR_STAY, UtilityInformation.END_OF_MESSAGE };
		sendMessage(stopElevator, stopElevator.length, packet.getAddress(), UtilityInformation.ELEVATOR_PORT_NUM);
		sendMessage(stopElevator, stopElevator.length, packet.getAddress(), UtilityInformation.FLOOR_PORT_NUM);
		elevatorDirection = UtilityInformation.ElevatorDirection.STATIONARY;
	}

	/**
	 * Send move elevator up message
	 * 
	 * @param packet
	 */
	protected void sendElevatorUp(DatagramPacket packet) {
		byte[] goUp = { UtilityInformation.ELEVATOR_DIRECTION_MODE, floorElevatorIsCurrentlyOn,
				UtilityInformation.ELEVATOR_UP, UtilityInformation.END_OF_MESSAGE };
		System.out.println("Sending elevator up... \n");
		sendMessage(goUp, goUp.length, packet.getAddress(), UtilityInformation.ELEVATOR_PORT_NUM);
		sendMessage(goUp, goUp.length, packet.getAddress(), UtilityInformation.FLOOR_PORT_NUM);
		elevatorDirection = UtilityInformation.ElevatorDirection.UP;
	}

	/**
	 * Send move elevator down message
	 * 
	 * @param packet
	 */
	protected void sendElevatorDown(DatagramPacket packet) {
		byte[] goDown = { UtilityInformation.ELEVATOR_DIRECTION_MODE, floorElevatorIsCurrentlyOn,
				UtilityInformation.ELEVATOR_DOWN, UtilityInformation.END_OF_MESSAGE };
		System.out.println("Sending elevator down... \n");
		sendMessage(goDown, goDown.length, packet.getAddress(), UtilityInformation.ELEVATOR_PORT_NUM);
		sendMessage(goDown, goDown.length, packet.getAddress(), UtilityInformation.FLOOR_PORT_NUM);
		elevatorDirection = UtilityInformation.ElevatorDirection.DOWN;
	}

	/**
	 * Send close elevator door message
	 * 
	 * @param packet
	 */
	protected void closeElevatorDoors(DatagramPacket packet) {
		byte[] closeDoor = { UtilityInformation.ELEVATOR_DOOR_MODE, UtilityInformation.DOOR_CLOSE,
				UtilityInformation.END_OF_MESSAGE };
		sendMessage(closeDoor, closeDoor.length, packet.getAddress(), UtilityInformation.ELEVATOR_PORT_NUM);
	}

	/**
	 * Send open elevator door message
	 * 
	 * @param packet
	 */
	protected void openElevatorDoors(DatagramPacket packet) {
		byte[] openDoor = { UtilityInformation.ELEVATOR_DOOR_MODE, UtilityInformation.DOOR_OPEN,
				UtilityInformation.END_OF_MESSAGE };
		sendMessage(openDoor, openDoor.length, packet.getAddress(), UtilityInformation.ELEVATOR_PORT_NUM);
	}

	/**
	 * Check to see if there are floors we need to go to above the current floor
	 * 
	 * @return True if we need to go up, false otherwise
	 */
	private boolean floorsToGoToAbove() {
		for (byte tempFloor : floorsToVisit) {
			if (tempFloor > floorElevatorIsCurrentlyOn) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Check to see if there are floors we need to go to below the current floor
	 * 
	 * @return True if we need to go down, false otherwise
	 */
	private boolean floorsToGoToBelow() {
		for (byte tempFloor : floorsToVisit) {
			if (tempFloor < floorElevatorIsCurrentlyOn) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Determine if the elevator should go up
	 * 
	 * @return True if the elevator should go up, false otherwise
	 */
	private boolean elevatorShouldGoUp() {
		int difference;
		int currentClosestDistance = Integer.MAX_VALUE;
		int closestFloor = 0;

		for (byte tempFloor : floorsToVisit) {
			difference = Math.abs(floorElevatorIsCurrentlyOn - tempFloor);
			if (difference < currentClosestDistance) {
				currentClosestDistance = difference;
				closestFloor = tempFloor;
			}
		}
		if (closestFloor > floorElevatorIsCurrentlyOn) {
			return true;
		}
		return false;

	}

	/**
	 * For when someone on the Elevator presses a button
	 * 
	 * @param recievedData
	 */
	private void extractElevatorButtonFloorAndGenerateResponseMessageAndActions(DatagramPacket recievedPacket) {
		System.out.println("Following floor button was hit in the elevator: " + recievedPacket.getData()[1] + "\n");
		floorButtonPressed(recievedPacket.getData()[1]);

	}

	/**
	 * For when the Floor sends message to Scheduler saying it has arrived.
	 * 
	 * @param recievedData
	 */
	private void extractFloorReachedNumberAndGenerateResponseMessageAndActions(DatagramPacket recievedPacket) {
		byte currentFloor = recievedPacket.getData()[1];
		System.out.println("Elevator has reached floor: " + currentFloor + "\n");
		floorElevatorIsCurrentlyOn = currentFloor;
		if (floorsToVisit.contains(currentFloor)) {
			stopElevator(recievedPacket);
			openElevatorDoors(recievedPacket);
		} else {
			currentState = State.RESPONDING_TO_MESSAGE;
			moveToFloor(recievedPacket);
		}
		reachedFloor(currentFloor);
	}

	/**
	 * Add a floor to the list tracking the floors to visit
	 * 
	 * @param floor
	 */
	private void floorButtonPressed(byte floor) {
		if (!floorsToVisit.contains(floor)) {
			floorsToVisit.add(floor);
			Collections.sort(floorsToVisit);
		}
	}

	/**
	 * Remove a floor from the list tracking floors to visit
	 * 
	 * @param floor
	 */
	private void reachedFloor(byte floor) {
		floorsToVisit.removeAll(Arrays.asList(floor));
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
		try {
			sendSocket = new DatagramSocket();
		} catch (SocketException e1) {
			e1.printStackTrace();
			System.exit(1);
		}
		sendPacket = new DatagramPacket(responseData, packetLength, destAddress, destPortNum);
		try {
			System.out.println("Scheduler is sending data...");
			sendSocket.send(sendPacket);
		} catch (IOException e) {
			System.out.println("Send socket failure!");
			e.printStackTrace();
			System.exit(1);
		}
	}
}