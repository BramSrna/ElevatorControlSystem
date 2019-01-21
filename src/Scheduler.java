import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
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
	 * [MODE, MODE, MODE, MESSAGE, MESSAGE, MESSAGE]
	 * </pre>
	 */

	private DatagramSocket recieveSocket, sendSocket;
	private DatagramPacket recievePacket, sendPacket;
	private final int SCHEDULER_PORT_NUM = 420;
	private final int MAX_BYTE_ARRAY_SIZE = 100;
	private int sendPortNum;
	private List<Integer> floorsToVisit;
	private final byte[] FLOOR_BUTTON_HIT_MODE = { 1, 2, 3 };
	private final byte[] REACHED_FLOOR_MESSAGE = { 4, 5, 6 };
	private final byte[] ERROR_DECODING_MESSAGE = { 83, 79, 83 };

	private Scheduler() {
		floorsToVisit = new ArrayList<Integer>();
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
			sendSocket = new DatagramSocket();
		} catch (SocketException e) {
			e.printStackTrace();
			System.exit(1);
		}
		// Recieve data
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
		// Decode message and create response
		byte[] responseData = new byte[MAX_BYTE_ARRAY_SIZE];
		responseData = decodeMessageAndGetResponse(recievePacket);

		// Send appropriate response
		// TODO
		sendPacket = new DatagramPacket(responseData, recievePacket.getLength(), recievePacket.getAddress(),
				sendPortNum);
		try {
			System.out.println("Scheduler is sending data...");
			sendSocket.send(sendPacket);
		} catch (IOException e) {
			System.out.println("Send socket failure!");
			e.printStackTrace();
			System.exit(1);
		}

		recieveSocket.close();
		sendSocket.close();
	}

	/**
	 * Decode the message sent in order to create the proper response message.
	 * 
	 * @param dataPacket Data send to the Scheduler.
	 * @return
	 */
	private byte[] decodeMessageAndGetResponse(DatagramPacket dataPacket) {
		byte[] recievedData = dataPacket.getData();
		byte[] mode = { recievedData[0], recievedData[1], recievedData[2] };
		// TODO
		if (mode.equals(FLOOR_BUTTON_HIT_MODE)) {
			return extractDataForFloorButtonHitAndGenerateResponse(recievedData);
		} else if (mode.equals(REACHED_FLOOR_MESSAGE)) {
			return extractDataForReachingFloorAndGenerateResponse(recievedData);
		}

		return ERROR_DECODING_MESSAGE;
	}

	private byte[] extractDataForFloorButtonHitAndGenerateResponse(byte[] recievedData) {
		// TODO
		int buttonPressed = 0;
		floorButtonPressed(buttonPressed);
		return recievedData;
	}

	/**
	 * This method gets the details of the message in the case that the mode of the
	 * message was that it reached a certain floor.
	 * 
	 * @param recievedData Data containing the details of the message
	 * @return Response message
	 */
	private byte[] extractDataForReachingFloorAndGenerateResponse(byte[] recievedData) {
		// TODO
		int currentFloor = 0;
		reachedFloor(currentFloor);
		return recievedData;
	}

	/**
	 * If a floor button in the elevator is hit and it is not already in the
	 * floorsToVisit list, then add it to the list and organize the list
	 * numerically.
	 * 
	 * @param floor The floor button pressed in the elevator.
	 */
	private void floorButtonPressed(int floor) {
		if (!floorsToVisit.contains(floor)) {
			floorsToVisit.add(floor);
			Collections.sort(floorsToVisit);
		}
	}

	/**
	 * If the elevator has arrived at a floor, remove it from the floors to visit.
	 * 
	 * @param floor The floor we arrived on.
	 */
	private void reachedFloor(int floor) {
		floorsToVisit.removeAll(Arrays.asList(floor));
	}

}
