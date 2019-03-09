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

	/**
	 * ServerPattern
	 * 
	 * Constructor
	 * 
	 * Create a new ServerPattern object. Also creates a new SignalReceiver object
	 * and runs it.
	 * 
	 * @param portNum  Port number to receive requests on
	 * @param name Name of the ServerPattern
	 * 
	 * @return None
	 */
	public ServerPattern(int portNum, String name) {
		receivedSignals = new ArrayList<DatagramPacket>();
		receivedSignalsEmpty = true;

		receiver = new SignalReceiver(portNum, this, name);

		Thread receiverThread = new Thread(receiver, "receiver");
		receiverThread.start();
	}

	/**
	 * signalReceived
	 * 
	 * Synchronized
	 * 
	 * Add a new signal to the list of received signals. Waits until
	 * the list of received signals is not full before adding the received
	 * signal.
	 * 
	 * @param newSignal    DatagramPacket containing the received signal
	 * @param priority     Priority of the received signal
	 * 
	 * @return None
	 */
	public synchronized void signalReceived(DatagramPacket newSignal, int priority) {
		// Wait while queue is not full
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

	/**
	 * getNextRequest
	 * 
	 * Returns the next request in the list of requests and removes
	 * it from the list.
	 * Waits until the list of requests is not empty.
	 * 
	 * @param  None
	 * 
	 * @return DatagramPacket containing the next received signal
	 */
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

	/**
	 * teardown
	 * 
	 * Tears down this ServerPattern object
	 */
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

	/**
	 * signalReceiver
	 * 
	 * Creates a new SignalReceiver object
	 * 
	 * @param portNum  The port number to receive messages on
	 * @param controller   The ServerPattern that controls this SignalReceiver object
	 * @param name The name of this ServerPattern object
	 * 
	 * @return None
	 */
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

	/**
	 * teardown
	 * 
	 * Tears down this SignalReceiver object
	 */
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

	/**
	 * run
	 * 
	 * Overridden
	 * 
	 * Runs this SignalReceiver object.
	 * Waits for signals and adds them to the list of received signals.
	 * 
	 * @param  None
	 * 
	 * @return None
	 */
	@Override
	public void run() {
		while (run) {
			DatagramPacket signal = waitForSignal();
			controller.signalReceived(signal, 0);
		}
	}
}