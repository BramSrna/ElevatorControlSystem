import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

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

	private DatagramSocket recieveSocket = null;
	private DatagramSocket sendSocket = null;
	private DatagramPacket recievePacket, sendPacket;
	private UtilityInformation.ElevatorDirection elevatorDirection;
	private State currentState;

	private final int MODE_BYTE_IND = 0;

	private SchedulerAlgorithm algor;

	public Scheduler() {
		algor = new SchedulerAlgorithm();
		elevatorDirection = UtilityInformation.ElevatorDirection.STATIONARY;
		currentState = State.WAITING;

		try {
			sendSocket = new DatagramSocket();
		} catch (SocketException e1) {
			e1.printStackTrace();
			System.exit(1);
		}

		try {
			recieveSocket = new DatagramSocket(UtilityInformation.SCHEDULER_PORT_NUM);
		} catch (SocketException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	public static void main(String[] args) {
		Scheduler scheduler = new Scheduler();
		while (true) {
			scheduler.receiveMessage();
		}
	}

	/**
	 * Close send and recieve sockets.
	 */
	protected void socketTearDown() {
		if (recieveSocket != null) {
			recieveSocket.close();
		}
		if (sendSocket != null) {
			sendSocket.close();
		}
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
				sendConfigPacketToElevator(packet);
				currentState = State.RESPONDING_TO_MESSAGE;
				eventOccured(Event.CONFIG_MESSAGE, packet);
			} else if (event.equals(Event.BUTTON_PUSHED_IN_ELEVATOR)) {
				algor.floorButtonPressed(packet);
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
	 * Read the message recieved and call the appropriate event
	 * 
	 * @param recievedPacket
	 */
	private void readMessage(DatagramPacket recievedPacket) {
		byte mode = recievedPacket.getData()[MODE_BYTE_IND];

		if (mode == UtilityInformation.CONFIG_MODE) { // 0
			eventOccured(Event.CONFIG_MESSAGE, recievedPacket);
		} else if (mode == UtilityInformation.FLOOR_SENSOR_MODE) { // 1
			eventOccured(Event.FLOOR_SENSOR_ACTIVATED, recievedPacket);
		} else if (mode == UtilityInformation.FLOOR_REQUEST_MODE) { // 2
			eventOccured(Event.FLOOR_REQUESTED, recievedPacket);
		} else if (mode == UtilityInformation.ELEVATOR_BUTTON_HIT_MODE) { // 3
			eventOccured(Event.BUTTON_PUSHED_IN_ELEVATOR, recievedPacket);
		} else if (mode == UtilityInformation.ELEVATOR_DIRECTION_MODE) { // 4

		} else if (mode == UtilityInformation.ELEVATOR_DOOR_MODE) { // 5

		} else if (mode == UtilityInformation.SEND_DESTINATION_TO_ELEVATOR_MODE) { // 6

		} else if (mode == UtilityInformation.TEARDOWN_MODE) { // 7
			eventOccured(Event.TEARDOWN, recievedPacket);
		} else if (mode == UtilityInformation.CONFIG_CONFIRM) { // 8
			eventOccured(Event.CONFIRM_CONFIG, recievedPacket);
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
//		System.out.println("Elevator was requested at: " + recievedPacket.getData()[1] + " in the direction "
//				+ recievedPacket.getData()[2] + " with destination " + recievedPacket.getData()[3] + "\n");
		byte[] destinationFloor = { UtilityInformation.SEND_DESTINATION_TO_ELEVATOR_MODE, recievedPacket.getData()[3],
				UtilityInformation.END_OF_MESSAGE };
		sendMessage(destinationFloor, destinationFloor.length, recievedPacket.getAddress(),
				UtilityInformation.ELEVATOR_PORT_NUM);
		algor.elevatorRequestMade(recievedPacket);
	}

	/**
	 * Move the elevator, and trigger the move elevator event
	 * 
	 * @param packet
	 */
	private void moveToFloor(DatagramPacket packet) {
		if (elevatorDirection.equals(UtilityInformation.ElevatorDirection.STATIONARY) && algor.somewhereToGo()) {
			closeElevatorDoors(packet);
			if (algor.elevatorShouldGoUp()) {
				sendElevatorUp(packet);
			} else {
				sendElevatorDown(packet);
			}
		} else if (elevatorDirection.equals(UtilityInformation.ElevatorDirection.UP) && algor.somewhereToGo()) {
			closeElevatorDoors(packet);
			if (algor.floorsToGoToAbove()) {
				sendElevatorUp(packet);
			} else {
				sendElevatorDown(packet);
			}
		} else if (elevatorDirection.equals(UtilityInformation.ElevatorDirection.DOWN) && algor.somewhereToGo()) {
			closeElevatorDoors(packet);
			if (algor.floorsToGoToBelow()) {
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
		byte[] stopElevator = { UtilityInformation.ELEVATOR_DIRECTION_MODE, algor.getCurrentFloor(),
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
		byte[] goUp = { UtilityInformation.ELEVATOR_DIRECTION_MODE, algor.getCurrentFloor(),
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
		byte[] goDown = { UtilityInformation.ELEVATOR_DIRECTION_MODE, algor.getCurrentFloor(),
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
		System.out.println("Closing elevator doors... \n");
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
		System.out.println("Opening elevator doors... \n");
		sendMessage(openDoor, openDoor.length, packet.getAddress(), UtilityInformation.ELEVATOR_PORT_NUM);
	}

	/**
	 * For when someone on the Elevator presses a button NOTE: This is not used yet,
	 * as nobody can press a button while in the elevator in real-time at the
	 * moment.
	 * 
	 * @param recievedData
	 */
	private void extractElevatorButtonFloorAndGenerateResponseMessageAndActions(DatagramPacket recievedPacket) {
		System.out.println("Following floor button was hit in the elevator: " + recievedPacket.getData()[1] + "\n");
		algor.floorButtonPressed(recievedPacket);

	}

	/**
	 * For when the Floor sends message to Scheduler saying it has arrived.
	 * 
	 * @param recievedData
	 */
	private void extractFloorReachedNumberAndGenerateResponseMessageAndActions(DatagramPacket recievedPacket) {
		algor.elevatorHasReachedFloor(recievedPacket);
		if (algor.getStopElevator()) {
			stopElevator(recievedPacket);
			openElevatorDoors(recievedPacket);
		}
		currentState = State.RESPONDING_TO_MESSAGE;
		moveToFloor(recievedPacket);
	}

	private void transitionState(State startState, Event occuredEvent) {
		disableStateActivity(startState);

		runExitAction(startState);

		State newState = null;

		runTransitionAction(startState, newState, occuredEvent);
		runEntryAction(newState);

		enableStateActivity(newState);
	}

	private void runEntryAction(State entryState) {

	}

	private void runTransitionAction(State exitState, State entryState, Event occuredEvent) {

	}

	private void runExitAction(State exitState) {

	}

	private void enableStateActivity(State currState) {

	}

	private void disableStateActivity(State currState) {

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

		try {
			System.out.println("Scheduler is sending data...");
			sendSocket.send(sendPacket);
		} catch (IOException e) {
			System.out.println("Send socket failure!");
			e.printStackTrace();
			System.exit(1);
		}
	}

	/**
	 * Recieve a message
	 */
	private void receiveMessage() {
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
	}

}