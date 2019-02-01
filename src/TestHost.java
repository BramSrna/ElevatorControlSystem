import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;

public class TestHost implements Runnable {
	private int RECEIVE_PORT = UtilityInformation.SCHEDULER_PORT_NUM;

	private InetAddress address;

	private DatagramSocket sendSocket, receiveSocket;
	private DatagramPacket sendPacket, receivePacket;

	private int expectedNumMessages;

	public TestHost(int expectedNumMessages) {
		try {
			sendSocket = new DatagramSocket();

			receiveSocket = new DatagramSocket(RECEIVE_PORT);
		} catch (SocketException se) {
			se.printStackTrace();
			System.exit(1);
		}

		try {
			address = InetAddress.getLocalHost();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		this.expectedNumMessages = expectedNumMessages;

	}

	public TestHost(int expectedNumMessages, int portNumber) {
		RECEIVE_PORT = portNumber;
		try {
			sendSocket = new DatagramSocket();

			receiveSocket = new DatagramSocket(RECEIVE_PORT);
		} catch (SocketException se) {
			se.printStackTrace();
			System.exit(1);
		}

		try {
			address = InetAddress.getLocalHost();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		this.expectedNumMessages = expectedNumMessages;

	}

	public void teardown() {
		sendPacket = null;
		receivePacket = null;

		// Close the socket
		sendSocket.close();
		receiveSocket.close();
	}

	public void sendPacket(byte[] msg, InetAddress address, int portNum) {
		// Create the DatagramPacket to return
		// Send the created response to the port that the message was received on
		sendPacket = new DatagramPacket(msg, msg.length, address, portNum);

		// Print out information about the packet being sent
		System.out.println("Test: Sending packet:");
		System.out.println("Test: To address: " + sendPacket.getAddress());
		System.out.println("Test: Destination port: " + sendPacket.getPort());
		int len = sendPacket.getLength();
		System.out.println("Test: Length: " + len);
		System.out.print("Test: Containing (as bytes): ");
		System.out.println(Arrays.toString(sendPacket.getData()));

		// Send the response packet
		try {
			sendSocket.send(sendPacket);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}

		System.out.println("Test: packet sent");
	}

	public void receivePacket(int expectedLen) {
		// Initialize the DatagramPacket used to receive requests
		byte data[] = new byte[expectedLen];
		receivePacket = new DatagramPacket(data, data.length);

		System.out.println("Test: Waiting for Packet.\n");

		// Wait on the DatagramSocket to receive a request
		try {
			System.out.println("Test: Waiting...");
			receiveSocket.receive(receivePacket);
		} catch (IOException e) {
			System.out.print("Test: IO Exception: likely:");
			System.out.println("Test: Receive Socket Timed Out.\n" + e);
			e.printStackTrace();
			System.exit(1);
		}

		// Print out information about the received packet
		System.out.println("Test: Packet received:");
		System.out.println("Test: To address: " + receivePacket.getAddress());
		System.out.println("Test: To port: " + receivePacket.getPort());
		int len = receivePacket.getLength();
		System.out.println("Test: Length: " + len);
		System.out.print("Test: Containing (as bytes): ");
		System.out.println(Arrays.toString(data) + "\n");
	}

	public void checkStop(byte[] arrToCheck) {
		if (arrToCheck[0] == (byte) -1) {
			teardown();
			System.exit(1);
		}
	}

	public int getPortNum() {
		return (RECEIVE_PORT);
	}

	public InetAddress getAddress() {
		return (address);
	}

	public void setExpectedNumMessages(int expectedNum) {
		expectedNumMessages = expectedNum;
	}

	@Override
	public void run() {
		int expectedLen = 100;

		for (int i = 0; i < expectedNumMessages; i++) {
			receivePacket(expectedLen);
			sendPacket(receivePacket.getData(), receivePacket.getAddress(), receivePacket.getPort());
		}

	}
}
