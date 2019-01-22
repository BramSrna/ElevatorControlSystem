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

	private DatagramSocket recieveSocket, sendSocket;
	private DatagramPacket recievePacket, sendPacket;
	private final int SCHEDULER_PORT_NUM = 420;
	private final int MAX_BYTE_ARRAY_SIZE = 100;
	private List<Integer> floorsToVisit;
	private final byte[] FLOOR_BUTTON_HIT_MODE = { 1, 2, 3 };
	private final byte[] REACHED_FLOOR_MESSAGE = { 4, 5, 6 };
	private final byte[] STOP_ELEVATOR = { 83, 84, 79, 90 }; // STOP in ASCII
	private final byte[] OPEN_DOOR = { 81, 80, 69, 78 }; // OPEN IN ASCII
	private ElevatorDirection elevatorDirection;

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

	private void readMessageAndGenerateResponseMessageAndActions(DatagramPacket dataPacket) {
		byte[] recievedData = dataPacket.getData();
		byte[] mode = { recievedData[0], recievedData[1], recievedData[2] };
		if (mode.equals(FLOOR_BUTTON_HIT_MODE)) {
			extractFloorButtonAndGenerateResponseMessageAndActions(recievedData);
		} else if (mode.equals(REACHED_FLOOR_MESSAGE)) {
			extractFloorNumberAndGenerateResponseMessageAndActions(recievedData);
		}
	}

	private void extractFloorButtonAndGenerateResponseMessageAndActions(byte[] recievedData) {
		byte[] floorButtonPressed = { recievedData[3], recievedData[4], recievedData[5], recievedData[6] };
		int buttonPressed = Integer.parseInt(new String(floorButtonPressed));
		floorButtonPressed(buttonPressed);
		// Generate and send necessary messages using sendMessage function

	}

	private void extractFloorNumberAndGenerateResponseMessageAndActions(byte[] recievedData) {
		byte[] currentFloorNumber = { recievedData[3], recievedData[4], recievedData[5], recievedData[6] };
		int currentFloor = Integer.parseInt(new String(currentFloorNumber));
		if (floorsToVisit.contains(currentFloor)) {
			// Stop the motor

			// Open the door
		}
		reachedFloor(currentFloor);
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

}
