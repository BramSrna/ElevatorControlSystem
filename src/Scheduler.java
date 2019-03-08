import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;

public class Scheduler extends ServerPattern {

	// State machine
	enum State {
		START, WAITING, READING_MESSAGE, RESPONDING_TO_MESSAGE, END
	}

	// External and internal events
	enum Event {
		MESSAGE_RECIEVED, CONFIG_MESSAGE, BUTTON_PUSHED_IN_ELEVATOR, FLOOR_SENSOR_ACTIVATED, FLOOR_REQUESTED,
		MOVE_ELEVATOR, TEARDOWN, CONFIRM_CONFIG, ELEVATOR_STOPPED, ELEVATOR_ERROR, SEND_ELEVATOR_ERROR,
		FIX_ELEVATOR_ERROR
	}

	private DatagramSocket sendSocket = null;
	private DatagramPacket sendPacket;
	private ArrayList<UtilityInformation.ElevatorDirection> elevatorDirection;
	private State currentState;
	private byte numElevators;

	private SchedulerAlgorithm algor;

	public Scheduler() {
		super(UtilityInformation.SCHEDULER_PORT_NUM, "Scheduler");

		algor = new SchedulerAlgorithm((byte) 0);

		elevatorDirection = new ArrayList<UtilityInformation.ElevatorDirection>();

		currentState = State.START;

		try {
			sendSocket = new DatagramSocket();
		} catch (SocketException e1) {
			e1.printStackTrace();
			System.exit(1);
		}
	}

	/**
	 * Close send and reciever sockets
	 */
	protected void socketTearDown() {
		if (sendSocket != null) {
			sendSocket.close();
		}

		super.teardown();
	}

	/**
	 * Based on an event that occurred in a given state, determine what action needs
	 * to be taken. Also changes the state of the scheduler.
	 * 
	 * @param event
	 * @param packet
	 */
	private void eventOccured(Event event, DatagramPacket packet) {
		switch (currentState) {
		case READING_MESSAGE:
			if (event.equals(Event.CONFIG_MESSAGE)) {
				currentState = State.RESPONDING_TO_MESSAGE;
				sendConfigPacketToElevator(packet);
				eventOccured(Event.CONFIG_MESSAGE, packet);
			} else if (event.equals(Event.FLOOR_SENSOR_ACTIVATED)) {
				currentState = State.RESPONDING_TO_MESSAGE;
				extractFloorReachedNumberAndGenerateResponseMessageAndActions(packet);
			} else if (event.equals(Event.FLOOR_REQUESTED)) {
				System.out.println("START CURR STATE:");
				algor.printAllInfo();
				currentState = State.RESPONDING_TO_MESSAGE;
				extractFloorRequestedNumberAndGenerateResponseMessageAndActions(packet);

				// If the elevator should stop, change the packet data to the information
				// required and trigger the floor sensor event
				for (byte elevatorNum = 0; elevatorNum < numElevators; elevatorNum++) {
					currentState = State.READING_MESSAGE;
					if (algor.getStopElevator(elevatorNum)) {
						byte[] newData = { UtilityInformation.FLOOR_SENSOR_MODE, algor.getCurrentFloor(elevatorNum),
								elevatorNum, -1 };
						packet.setData(newData);
						eventOccured(Event.FLOOR_SENSOR_ACTIVATED, packet);
					}
				}
				System.out.println("END CURR STATE:");
				algor.printAllInfo();

				currentState = State.RESPONDING_TO_MESSAGE;
			} else if (event.equals(Event.TEARDOWN)) {
				currentState = State.END;

				sendTearDownMessage(packet);
			} else if (event.equals(Event.CONFIRM_CONFIG)) {
				currentState = State.RESPONDING_TO_MESSAGE;

				sendConfigConfirmMessage(packet);
				eventOccured(Event.CONFIRM_CONFIG, packet);
			} else if (event.equals(Event.ELEVATOR_STOPPED)) {
				currentState = State.RESPONDING_TO_MESSAGE;
			} else if (event.equals(Event.ELEVATOR_ERROR)) {
				currentState = State.RESPONDING_TO_MESSAGE;
				byte errorType = packet.getData()[1];
				if (errorType == UtilityInformation.DOOR_WONT_CLOSE_ERROR
						|| errorType == UtilityInformation.DOOR_WONT_OPEN_ERROR) {
					handleDoorStuckError(packet);
				} else if (errorType == UtilityInformation.ELEVATOR_STUCK_ERROR) {
					handleElevatorStuckError(packet);
				} else {
					System.out.println("\n\nThat is an invalid error type!\n\n");
				}
				eventOccured(Event.SEND_ELEVATOR_ERROR, packet);

			} else if (event.equals(Event.FIX_ELEVATOR_ERROR)) {
				handleElevatorFixed(packet);
			}
			currentState = State.WAITING;
			break;
		case WAITING:
			if (event.equals(Event.MESSAGE_RECIEVED)) {
				currentState = State.READING_MESSAGE;
				readMessage(packet);
			}
			break;
		case RESPONDING_TO_MESSAGE:
			if (event.equals(Event.MOVE_ELEVATOR) || event.equals(Event.CONFIG_MESSAGE)
					|| event.equals(Event.CONFIRM_CONFIG) || event.equals(Event.SEND_ELEVATOR_ERROR)) {
				currentState = State.WAITING;
			}
			break;
		case START:
			currentState = State.WAITING;
			eventOccured(event, packet);
			break;
		default:
			System.out.println("Should never come here!\n");
			System.exit(1);
			break;

		}
	}

	/**
	 * Send the confimration from the config message to the Floor
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
		System.exit(0);
	}

	/**
	 * Setup elevator and floor schematics and also send this information to the
	 * Elevator
	 * 
	 * @param configPacket
	 */
	protected void sendConfigPacketToElevator(DatagramPacket configPacket) {
		System.out.println("Sending config file to Elevator...\n");
		setNumElevators(configPacket.getData()[1]);
		sendMessage(configPacket.getData(), configPacket.getData().length, configPacket.getAddress(),
				UtilityInformation.ELEVATOR_PORT_NUM);
	}

	/**
	 * Set the number of elevators and all the lists that need to be initialized
	 * with the correct number of elevators
	 * 
	 * @param newNumElevators
	 */
	public void setNumElevators(byte newNumElevators) {
		this.numElevators = newNumElevators;
		while (elevatorDirection.size() > numElevators) {
			elevatorDirection.remove(elevatorDirection.size() - 1);
		}
		while (elevatorDirection.size() < numElevators) {
			elevatorDirection.add(UtilityInformation.ElevatorDirection.STATIONARY);
		}
		algor.setNumberOfElevators(numElevators);
	}

	/**
	 * Read the message recieved and call the appropriate event
	 * 
	 * @param recievedPacket
	 */
	private void readMessage(DatagramPacket recievedPacket) {
		byte mode = recievedPacket.getData()[UtilityInformation.MODE_BYTE_IND];

		if (mode == UtilityInformation.CONFIG_MODE) { // 0
			eventOccured(Event.CONFIG_MESSAGE, recievedPacket);
		} else if (mode == UtilityInformation.FLOOR_SENSOR_MODE) { // 1
			eventOccured(Event.FLOOR_SENSOR_ACTIVATED, recievedPacket);
		} else if (mode == UtilityInformation.FLOOR_REQUEST_MODE) { // 2
			eventOccured(Event.FLOOR_REQUESTED, recievedPacket);
		} else if (mode == UtilityInformation.ELEVATOR_BUTTON_HIT_MODE) { // 3
			eventOccured(Event.BUTTON_PUSHED_IN_ELEVATOR, recievedPacket);
		} else if (mode == UtilityInformation.TEARDOWN_MODE) { // 7
			eventOccured(Event.TEARDOWN, recievedPacket);
		} else if (mode == UtilityInformation.CONFIG_CONFIRM) { // 8
			eventOccured(Event.CONFIRM_CONFIG, recievedPacket);
		} else if (mode == UtilityInformation.ELEVATOR_STOPPED_MODE) { // 9
			eventOccured(Event.ELEVATOR_STOPPED, recievedPacket);
		} else if (mode == UtilityInformation.ERROR_MESSAGE_MODE) {
			eventOccured(Event.ELEVATOR_ERROR, recievedPacket);
		} else if (mode == UtilityInformation.FIX_ERROR_MODE) {
			eventOccured(Event.FIX_ELEVATOR_ERROR, recievedPacket);
		} else {
			System.out.println(String.format("Error in readMessage: Undefined mode: %d", mode));
		}
	}

	/**
	 * For when someone on a Floor presses the button for an elevator request.
	 * 
	 * @param recievedData
	 */
	protected void extractFloorRequestedNumberAndGenerateResponseMessageAndActions(DatagramPacket recievedPacket) {
		UtilityInformation.ElevatorDirection upOrDown = null;
		if (recievedPacket.getData()[2] == 0) {
			upOrDown = UtilityInformation.ElevatorDirection.DOWN;
		} else {
			upOrDown = UtilityInformation.ElevatorDirection.UP;
		}

		byte elevatorNum = algor.elevatorRequestMade(recievedPacket.getData()[1], recievedPacket.getData()[3],
				upOrDown);

		// Update elevator destinations
		ArrayList<Byte> elevatorDestinations = algor.getDestinations(elevatorNum);
		if (elevatorDestinations.size() > 0) {
			byte[] destinationFloor = { UtilityInformation.SEND_DESTINATION_TO_ELEVATOR_MODE,
					elevatorDestinations.get(0), elevatorNum, UtilityInformation.END_OF_MESSAGE };
			for (int a = 0; a < 10000; a++) {
				// Delay
			}
			sendMessage(destinationFloor, destinationFloor.length, recievedPacket.getAddress(),
					UtilityInformation.ELEVATOR_PORT_NUM);
		}
	}

	/**
	 * Move the elevator, and trigger the move elevator event
	 * 
	 * @param packet
	 */
	private void moveToFloor(DatagramPacket packet) {
		byte elevatorNum = packet.getData()[2];

		if (algor.somewhereToGo(elevatorNum)) {
			closeElevatorDoors(packet);

			if (algor.whatDirectionShouldTravel(elevatorNum).equals(UtilityInformation.ElevatorDirection.DOWN)) {
				sendElevatorDown(packet);
			} else if (algor.whatDirectionShouldTravel(elevatorNum).equals(UtilityInformation.ElevatorDirection.UP)) {
				sendElevatorUp(packet);
			} else {
				if (packet.getData()[0] != UtilityInformation.ELEVATOR_STOPPED_MODE) {
					stopElevator(packet, elevatorNum);
					openElevatorDoors(packet);
				}
			}
		} else {
			if (packet.getData()[0] != UtilityInformation.ELEVATOR_STOPPED_MODE) {
				stopElevator(packet, elevatorNum);
				openElevatorDoors(packet);
			}
		}

		eventOccured(Event.MOVE_ELEVATOR, packet);
	}

	/**
	 * Send stop elevator message
	 * 
	 * @param packet
	 * @param elevatorNum
	 */
	protected void stopElevator(DatagramPacket packet, byte elevatorNum) {
		byte[] stopElevator = { UtilityInformation.ELEVATOR_DIRECTION_MODE, algor.getCurrentFloor(elevatorNum),
				elevatorNum, UtilityInformation.ELEVATOR_STAY, UtilityInformation.END_OF_MESSAGE };
		sendMessage(stopElevator, stopElevator.length, packet.getAddress(), UtilityInformation.ELEVATOR_PORT_NUM);
		sendMessage(stopElevator, stopElevator.length, packet.getAddress(), UtilityInformation.FLOOR_PORT_NUM);
		elevatorDirection.set(elevatorNum, UtilityInformation.ElevatorDirection.STATIONARY);
		algor.setStopElevator(elevatorNum, true);
	}

	/**
	 * Send move elevator up message
	 * 
	 * @param packet
	 */
	protected void sendElevatorUp(DatagramPacket packet) {
		byte elevatorNum = packet.getData()[2];
		byte[] goUp = { UtilityInformation.ELEVATOR_DIRECTION_MODE, algor.getCurrentFloor(elevatorNum), elevatorNum,
				UtilityInformation.ELEVATOR_UP, UtilityInformation.END_OF_MESSAGE };
		System.out.println("Sending elevator up... \n");
		sendMessage(goUp, goUp.length, packet.getAddress(), UtilityInformation.ELEVATOR_PORT_NUM);
		sendMessage(goUp, goUp.length, packet.getAddress(), UtilityInformation.FLOOR_PORT_NUM);
		elevatorDirection.set(elevatorNum, UtilityInformation.ElevatorDirection.UP);
	}

	/**
	 * Send move elevator down message
	 * 
	 * @param packet
	 */
	protected void sendElevatorDown(DatagramPacket packet) {
		byte elevatorNum = packet.getData()[2];
		byte[] goDown = { UtilityInformation.ELEVATOR_DIRECTION_MODE, algor.getCurrentFloor(elevatorNum), elevatorNum,
				UtilityInformation.ELEVATOR_DOWN, UtilityInformation.END_OF_MESSAGE };
		System.out.println("Sending elevator down... \n");
		sendMessage(goDown, goDown.length, packet.getAddress(), UtilityInformation.ELEVATOR_PORT_NUM);
		sendMessage(goDown, goDown.length, packet.getAddress(), UtilityInformation.FLOOR_PORT_NUM);
		elevatorDirection.set(elevatorNum, UtilityInformation.ElevatorDirection.DOWN);
	}

	/**
	 * Send close elevator door message
	 * 
	 * @param packet
	 */
	protected void closeElevatorDoors(DatagramPacket packet) {
		byte elevatorNum = packet.getData()[2];
		byte[] closeDoor = { UtilityInformation.ELEVATOR_DOOR_MODE, UtilityInformation.DOOR_CLOSE, elevatorNum,
				UtilityInformation.END_OF_MESSAGE };
		sendMessage(closeDoor, closeDoor.length, packet.getAddress(), UtilityInformation.ELEVATOR_PORT_NUM);
	}

	/**
	 * Send open elevator door message
	 * 
	 * @param packet
	 */
	protected void openElevatorDoors(DatagramPacket packet) {
		byte elevatorNum = packet.getData()[2];
		byte[] openDoor = { UtilityInformation.ELEVATOR_DOOR_MODE, UtilityInformation.DOOR_OPEN, elevatorNum,
				UtilityInformation.END_OF_MESSAGE };
		sendMessage(openDoor, openDoor.length, packet.getAddress(), UtilityInformation.ELEVATOR_PORT_NUM);
	}

	/**
	 * For when the Floor sends message to Scheduler saying it has arrived.
	 * 
	 * @param recievedPacket
	 */
	private void extractFloorReachedNumberAndGenerateResponseMessageAndActions(DatagramPacket recievedPacket) {
		byte floorNum = recievedPacket.getData()[1];
		byte elevatorNum = recievedPacket.getData()[2];
		algor.elevatorHasReachedFloor(floorNum, elevatorNum);

		// Stop elevator if necessary
		if (algor.getStopElevator(elevatorNum)) {
			stopElevator(recievedPacket, elevatorNum);
			openElevatorDoors(recievedPacket);
		}

		// Continue moving elevator
		moveToFloor(recievedPacket);
	}

	private void handleDoorStuckError(DatagramPacket receivedPacket) {
		byte stuckDoorState = receivedPacket.getData()[1];

		if (stuckDoorState == UtilityInformation.DOOR_CLOSE) {
			closeElevatorDoors(receivedPacket);
		} else if (stuckDoorState == UtilityInformation.DOOR_OPEN) {
			openElevatorDoors(receivedPacket);
		} else {
			System.out.println("Error: Invalid Door State.");
			socketTearDown();
			System.exit(1);
		}
	}

	private void handleElevatorStuckError(DatagramPacket receivedPacket) {
		byte elevatorNum = receivedPacket.getData()[2];
		algor.stopUsingElevator(elevatorNum);
	}

	private void handleElevatorFixed(DatagramPacket receivedPacket) {
		byte elevatorNum = receivedPacket.getData()[2];
		algor.resumeUsingElevator(elevatorNum);
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
		sendPacket = new DatagramPacket(responseData, packetLength, destAddress, destPortNum);

		// Print out info about the message being sent
		System.out.println("Scheduler: Sending packet:");
		System.out.println("To host: " + sendPacket.getAddress());
		System.out.println("Destination host port: " + sendPacket.getPort());
		int len = sendPacket.getLength();
		System.out.println("Length: " + len);
		System.out.print("Containing (as bytes): ");
		System.out.println(Arrays.toString(sendPacket.getData()));

		try {
			System.out.println("Scheduler is sending data...");
			sendSocket.send(sendPacket);
		} catch (IOException e) {
			System.out.println("Send socket failure!");
			e.printStackTrace();
			System.exit(1);
		}

		System.out.println("Scheduler: Packet sent.\n");
	}

	public void runSheduler() {
		while (true) {
			DatagramPacket nextReq = this.getNextRequest();
			eventOccured(Event.MESSAGE_RECIEVED, nextReq);
		}
	}

	public static void main(String[] args) {
		Scheduler scheduler = new Scheduler();
		scheduler.runSheduler();
	}
}