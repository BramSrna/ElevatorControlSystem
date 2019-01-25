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

	// Modes
	private final byte CONFIG_MODE = 0;
	private final byte FLOOR_SENSOR_MODE = 1;
	private final byte FLOOR_REQUEST_MODE = 2;
	private final byte ELEVATOR_BUTTON_HIT_MODE = 3;
	private final byte ELEVATOR_DIRECTION_MODE = 4;
	private final byte ELEVATOR_DOOR_MODE = 5;

	// Messages
	private final byte ELEVATOR_STAY = 0;
	private final byte ELEVATOR_UP = 1;
	private final byte ELEVATOR_DOWN = 2;
	private final byte END_OF_MESSAGE = -1;
	private final byte DOOR_CLOSE = 0;
	private final byte DOOR_OPEN = 1;

	private DatagramSocket recieveSocket, sendSocket;
	private DatagramPacket recievePacket, sendPacket;
	private final int SCHEDULER_PORT_NUM = 420;
	private final int ELEVATOR_PORT_NUM = 69;
	private final int FLOOR_PORT_NUM = 666;
	private final int MAX_BYTE_ARRAY_SIZE = 100;
	private List<Byte> floorsToVisit;
	private ElevatorDirection elevatorDirection;
	private byte floorElevatorIsCurrentlyOn;

	enum ElevatorDirection {
		UP, DOWN, STATIONARY
	}

	private Scheduler() {
		floorsToVisit = new ArrayList<Byte>();
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
		byte mode = recievedPacket.getData()[0];
		if (mode == CONFIG_MODE) {
			System.out.println("Sending config file to Elevator...");
			sendMessage(recievedPacket.getData(), recievedPacket.getData().length, recievedPacket.getAddress(),
					ELEVATOR_PORT_NUM);
		} else if (mode == ELEVATOR_BUTTON_HIT_MODE) {
			extractElevatorButtonFloorAndGenerateResponseMessageAndActions(recievedPacket);
		} else if (mode == FLOOR_SENSOR_MODE) {
			extractFloorReachedNumberAndGenerateResponseMessageAndActions(recievedPacket);
		} else if (mode == FLOOR_REQUEST_MODE) {
			extractFloorRequestedNumberAndGenerateResponseMessageAndActions(recievedPacket);
		}
	}

	/**
	 * For when someone on a Floor presses the button for an elevator request.
	 * 
	 * @param recievedData
	 */
	private void extractFloorRequestedNumberAndGenerateResponseMessageAndActions(DatagramPacket recievedPacket) {
		System.out.println("Elevator was requested at: "
				+ Character.getNumericValue(getCharFromByteArray(recievedPacket.getData()).get(0)) + "\n");
		floorButtonPressed(Character.getNumericValue(getCharFromByteArray(recievedPacket.getData()).get(0)));
	}

	private void moveToFloor(DatagramPacket packet) {
		if (elevatorDirection.equals(ElevatorDirection.STATIONARY)) {
			if (!floorsToVisit.isEmpty()) {
				sendMessage(DOOR_CLOSE, DOOR_CLOSE.length, packet.getAddress(), ELEVATOR_PORT_NUM);
				if (elevatorShouldGoUp()) {
					sendMessage(GO_UP, DOOR_CLOSE.length, packet.getAddress(), ELEVATOR_PORT_NUM);
				} else {
					sendMessage(GO_DOWN, DOOR_CLOSE.length, packet.getAddress(), ELEVATOR_PORT_NUM);
				}
			}

		} else if (elevatorDirection.equals(ElevatorDirection.UP)) {
			sendMessage(DOOR_CLOSE, DOOR_CLOSE.length, packet.getAddress(), ELEVATOR_PORT_NUM);
			sendMessage(GO_UP, DOOR_CLOSE.length, packet.getAddress(), ELEVATOR_PORT_NUM);
		} else if (elevatorDirection.equals(ElevatorDirection.DOWN)) {
			sendMessage(DOOR_CLOSE, DOOR_CLOSE.length, packet.getAddress(), ELEVATOR_PORT_NUM);
			sendMessage(GO_DOWN, DOOR_CLOSE.length, packet.getAddress(), ELEVATOR_PORT_NUM);
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
		byte elevatorShaft = recievedPacket.getData()[2];
		System.out.println("Elevator " + elevatorShaft + " has reached floor: " + currentFloor + "\n");
		floorElevatorIsCurrentlyOn = currentFloor;
		if (floorsToVisit.contains(currentFloor)) {
			// TODO I do not understand what you mean by destination floor, and added
			// elevator shaft
			byte[] STOP_ELEVATOR = { ELEVATOR_DIRECTION_MODE, elevatorShaft, currentFloor, currentFloor, ELEVATOR_STAY,
					END_OF_MESSAGE };
			sendMessage(STOP_ELEVATOR, STOP_ELEVATOR.length, recievedPacket.getAddress(), ELEVATOR_PORT_NUM);
			byte[] OPEN_DOOR = { ELEVATOR_DOOR_MODE, elevatorShaft, DOOR_OPEN };
			sendMessage(OPEN_DOOR, OPEN_DOOR.length, recievedPacket.getAddress(), ELEVATOR_PORT_NUM);
		}
		reachedFloor(currentFloor);
		moveToFloor(recievedPacket);
	}

	private void floorButtonPressed(byte floor) {
		if (!floorsToVisit.contains(floor)) {
			floorsToVisit.add(floor);
			Collections.sort(floorsToVisit);
		}
	}

	private void reachedFloor(byte floor) {
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
