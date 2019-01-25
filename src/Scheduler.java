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

	/**
	 * Workflow
	 * 
	 * <pre>
	 * User at floor and wants elevator
	 * Floor Subsystem -> Scheduler: I am on X floor and I want to go up/down
	 * Scheduler -> Elevator: Make sure doors closed, and move up/down
	 * Floor Subsystem -> Scheduler: You are on floor X (stop if requested)
	 * Scheduler -> Elevator: Stop, open door
	 * 
	 * User inside elevator:
	 * Elevator -> Scheduler: User wants to go to floor X
	 * Scheduler -> Elevator: Close door and move up/down
	 * Floor Subsystem -> Scheduler: You are on floor X (stop if requested)
	 * Scheduler -> Elevator: Stop, open door
	 * </pre>
	 */

	/**
	 * HOW TO DECODE MESSAGES:
	 * 
	 * <pre>
	 * [MODE, MODE, MODE, MESSAGE, MESSAGE, MESSAGE, MESSAGE]
	 * </pre>
	 */

	/**
	 * <pre>
	 * 1. How will the Scheduler know the elevator port number?
	 * </pre>
	 */

	private DatagramSocket recieveSocket, sendSocket;
	private DatagramPacket recievePacket, sendPacket;
	private final int SCHEDULER_PORT_NUM = 420;
	private final int ELEVATOR_PORT_NUM = 69;
	private final int FLOOR_PORT_NUM = 666;
	private final int MAX_BYTE_ARRAY_SIZE = 100;
	private List<Integer> floorsToVisit;
	private final byte[] FLOOR_BUTTON_HIT_MODE = { 0, 0, 0 };
	private final byte[] REACHED_FLOOR_MESSAGE_MODE = { 0, 0, 1 };
	private final byte[] ELEVATOR_REQUESTED_MODE = { 0, 0, 2 };
	private final byte[] STOP_ELEVATOR = { 83, 84, 79, 90 }; // STOP in ASCII
	private final byte[] OPEN_DOOR = { 81, 80, 69, 78 }; // OPEN in ASCII
	private final byte[] CLOSE_DOOR = { 67, 76, 79, 83, 69 }; // CLOSE in ASCII
	private final byte[] GO_UP = { 0, 0, 85, 80 }; // UP in ASCII
	private final byte[] GO_DOWN = { 68, 79, 87, 78 }; // DOWN in ASCII
	private ElevatorDirection elevatorDirection;
	private int floorElevatorIsCurrentlyOn;

	enum ElevatorDirection {
		UP, DOWN, STATIONARY
	}

	private Scheduler() {
		floorsToVisit = new ArrayList<Integer>();
		elevatorDirection = ElevatorDirection.STATIONARY;
	}

	public static void main(String[] args) {
		Scheduler scheduler = new Scheduler();
		while (true) {
			scheduler.recieveAndSendData();
		}
	}

	private void recieveAndSendData() {
		try {
			recieveSocket = new DatagramSocket(SCHEDULER_PORT_NUM);
		} catch (SocketException e) {
			e.printStackTrace();
			System.exit(1);
		}
		byte[] data = new byte[MAX_BYTE_ARRAY_SIZE];
		recievePacket = new DatagramPacket(data, data.length);
		try {
			System.out.println("Scheduler is waiting for data...");
			recieveSocket.receive(recievePacket);
		} catch (IOException e) {
			System.out.println("Recieve Socket failure!");
			e.printStackTrace();
			System.exit(1);
		}
		readMessageAndGenerateResponseMessageAndActions(recievePacket);
		recieveSocket.close();
		sendSocket.close();
	}

	// TODO
	private void readMessageAndGenerateResponseMessageAndActions(DatagramPacket recievedPacket) {
		byte[] recievedData = recievedPacket.getData();
		byte[] mode = { recievedData[0], recievedData[1], recievedData[2] };
		if (mode.equals(FLOOR_BUTTON_HIT_MODE)) {
			extractElevatorButtonFloorAndGenerateResponseMessageAndActions(recievedPacket);
		} else if (mode.equals(REACHED_FLOOR_MESSAGE_MODE)) {
			extractFloorReachedNumberAndGenerateResponseMessageAndActions(recievedPacket);
		} else if (mode.equals(ELEVATOR_REQUESTED_MODE)) {
			extractFloorRequestedNumberAndGenerateResponseMessageAndActions(recievedPacket);
		}
	}

	/**
	 * For when someone on a Floor presses the button for an elevator request.
	 * 
	 * @param recievedData
	 */
	private void extractFloorRequestedNumberAndGenerateResponseMessageAndActions(DatagramPacket recievedPacket) {
		floorButtonPressed(Character.getNumericValue(getCharFromByteArray(recievedPacket.getData()).get(0)));
	}

	private void moveToFloor(DatagramPacket packet) {
		if (elevatorDirection.equals(ElevatorDirection.STATIONARY)) {
			if (!floorsToVisit.isEmpty()) {
				sendMessage(CLOSE_DOOR, CLOSE_DOOR.length, packet.getAddress(), ELEVATOR_PORT_NUM);
				if (elevatorShouldGoUp()) {
					sendMessage(GO_UP, CLOSE_DOOR.length, packet.getAddress(), ELEVATOR_PORT_NUM);
				} else {
					sendMessage(GO_DOWN, CLOSE_DOOR.length, packet.getAddress(), ELEVATOR_PORT_NUM);
				}
			}

		} else if (elevatorDirection.equals(ElevatorDirection.UP)) {
			sendMessage(CLOSE_DOOR, CLOSE_DOOR.length, packet.getAddress(), ELEVATOR_PORT_NUM);
			sendMessage(GO_UP, CLOSE_DOOR.length, packet.getAddress(), ELEVATOR_PORT_NUM);
		} else if (elevatorDirection.equals(ElevatorDirection.DOWN)) {
			sendMessage(CLOSE_DOOR, CLOSE_DOOR.length, packet.getAddress(), ELEVATOR_PORT_NUM);
			sendMessage(GO_DOWN, CLOSE_DOOR.length, packet.getAddress(), ELEVATOR_PORT_NUM);
		}
	}

	private boolean elevatorShouldGoUp() {
		int difference;
		int currentClosestDistance = Integer.MAX_VALUE;
		int closestFloor = 0;

		for (int tempFloor : floorsToVisit) {
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
		floorButtonPressed(Character.getNumericValue(getCharFromByteArray(recievedPacket.getData()).get(0)));

	}

	/**
	 * For when the Floor sends message to Scheduler saying it has arrived.
	 * 
	 * @param recievedData
	 */
	private void extractFloorReachedNumberAndGenerateResponseMessageAndActions(DatagramPacket recievedPacket) {
		int currentFloor = Character.getNumericValue(getCharFromByteArray(recievedPacket.getData()).get(0));
		floorElevatorIsCurrentlyOn = currentFloor;
		if (floorsToVisit.contains(currentFloor)) {
			sendMessage(STOP_ELEVATOR, CLOSE_DOOR.length, recievedPacket.getAddress(), ELEVATOR_PORT_NUM);
			sendMessage(OPEN_DOOR, CLOSE_DOOR.length, recievedPacket.getAddress(), ELEVATOR_PORT_NUM);
		}
		reachedFloor(currentFloor);
		moveToFloor(recievedPacket);
	}

	private void floorButtonPressed(int floor) {
		if (!floorsToVisit.contains(floor)) {
			floorsToVisit.add(floor);
			Collections.sort(floorsToVisit);
		}
	}

	private void reachedFloor(int floor) {
		floorsToVisit.removeAll(Arrays.asList(floor));
	}

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

	private List<Character> getCharFromByteArray(byte[] array) {
		List<Character> charValues = new ArrayList<Character>();
		for (int a = 0; a < array.length; a++) {
			if (array[a] == 48) { // 0 in ASCII
				break;
			}
			charValues.add((char) array[a]);
		}
		return charValues;
	}

}
