import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * 
 * This class is used to create a queue for the messages being sent to whatever
 * class extends this class
 *
 */
public abstract class ServerPattern {
	private ArrayList<DatagramPacket> receivedSignals;

	private SignalReceiver receiver;

	private boolean receivedSignalsEmpty;

	private final int MAX_NUM_SIGNALS = 100;

	private String name;

	public ServerPattern(int portNum, String name) {
		this.name = name;

		receivedSignals = new ArrayList<DatagramPacket>();
		receivedSignalsEmpty = true;

		receiver = new SignalReceiver(portNum, this, name);

		Thread receiverThread = new Thread(receiver, "receiver");
		receiverThread.start();
	}

	public synchronized void signalReceived(DatagramPacket newSignal, int priority) {

		// Wait while queue is full
		while (receivedSignals.size() >= MAX_NUM_SIGNALS) {
			try {
				wait();
			} catch (InterruptedException e) {
				System.out.println("Error waiting in synchronized signalRecieved method in ServerPattern.");
				e.printStackTrace();
			}
		}

		receivedSignals.add(newSignal);

		receivedSignalsEmpty = false;
		notifyAll();
	}

	public synchronized DatagramPacket getNextRequest() {

		// Wait while queue is empty
		while (receivedSignalsEmpty) {
			try {
				wait();
			} catch (InterruptedException e) {
				System.out.println("Error waiting in synchronized getNextRequest method in ServerPattern.");
				e.printStackTrace();
			}
		}

		DatagramPacket toReturn = receivedSignals.get(0);
		receivedSignals.remove(0);

		if (receivedSignals.size() == 0) {
			receivedSignalsEmpty = true;
		}
		notifyAll();

		return (toReturn);
	}

	public void teardown() {
		receiver.teardown();

	}

}

/**
 * 
 * This class is used to recieve messages using DatagramSockets
 *
 */

class SignalReceiver implements Runnable {
	private DatagramPacket receivePacket;
	private DatagramSocket receiveSocket;

	private ServerPattern controller;
	private boolean run;

	private String name;

	public SignalReceiver(int portNum, ServerPattern controller, String name) {
		run = true;

		this.controller = controller;
		this.name = name;

		// Initialize the DatagramSocket
		try {
			receiveSocket = new DatagramSocket(portNum);
		} catch (SocketException se) {
			se.printStackTrace();
			this.teardown();
			System.exit(1);
		}
	}

	public void teardown() {
		run = false;
		receiveSocket.close();
		receiveSocket = null;

	}

	/**
	 * waitForSignal
	 * 
	 * Waits for a packet of the given size to be sent by the Scheduler. When the
	 * packet is received, information about the packet is printed. The message
	 * (byte[]) in the packet is then returned.
	 * 
	 * @param expectedMsgSize The expected size of the message to receive
	 * 
	 * @return The byte[] send in the DatagramPacket
	 */
	public DatagramPacket waitForSignal() {
		// Create the receive packet
		int expectedMsgSize = 100;

		byte data[] = new byte[expectedMsgSize];
		receivePacket = new DatagramPacket(data, data.length);

		System.out.println(String.format("%s: Waiting for message...", name));

		try {
			// Block until a datagram is received via sendSocket.
			receiveSocket.receive(receivePacket);
		} catch (IOException e) {
			if (run) {
				e.printStackTrace();
				this.teardown();
				System.exit(1);
			}
		}

		// Print out information about the response
		System.out.println(String.format("%s: Packet received:", name));
		System.out.println("From host: " + receivePacket.getAddress());
		System.out.println("Host port: " + receivePacket.getPort());
		int len = receivePacket.getLength();
		System.out.println("Length: " + len);
		System.out.print("Containing (as bytes): ");
		System.out.println(Arrays.toString(receivePacket.getData()) + "\n");

		return (receivePacket);
	}

	@Override
	public void run() {
		while (run) {
			DatagramPacket signal = waitForSignal();
			controller.signalReceived(signal, 0);
		}
	}
}