
public class Scheduler {

}
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.ArrayList;
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
	 * []
	 * </pre>
	 */

	private DatagramSocket recieveSocket, sendSocket;
	private DatagramPacket recievePacket, sendPacket;
	private final int SCHEDULER_PORT_NUM = 420;
	private final int MAX_BYTE_ARRAY_SIZE = 100;
	private int sendPortNum;
	private List<Integer> floorsToVisit;

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
		try {SCHEDULER_PORT_NUM
			recieveSocket = new DatagramSocket();
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

	private byte[] decodeMessageAndGetResponse(DatagramPacket dataPacket) {
		// TODO
		byte[] responseByte = new byte[MAX_BYTE_ARRAY_SIZE];
		return responseByte;
	}

}
