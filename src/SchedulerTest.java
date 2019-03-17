import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SchedulerTest {

	private Scheduler scheduler;
	private TestHost elevatorHost;
	private TestHost floorHost;

	/**
	 * Initialize scheduler
	 * 
	 * @throws Exception
	 */
	@BeforeEach
	void setUp() throws Exception {
		scheduler = new Scheduler();

		elevatorHost = new TestHost(0, UtilityInformation.ELEVATOR_PORT_NUM, UtilityInformation.SCHEDULER_PORT_NUM);

		floorHost = new TestHost(0, UtilityInformation.FLOOR_PORT_NUM, UtilityInformation.SCHEDULER_PORT_NUM);

		elevatorHost.disableResponse();
		floorHost.disableResponse();
	}

	/**
	 * Make sure the scheduler and hosts are closed/set to NULL.
	 * 
	 * @throws Exception
	 */
	@AfterEach
	void tearDown() throws Exception {
		scheduler.socketTearDown();
		elevatorHost.teardown();
		floorHost.teardown();

		scheduler = null;
		elevatorHost = null;
		floorHost = null;
	}

	/**
	 * Test to make sure the open elevator doors send/recieve works.
	 * 
	 * @throws UnknownHostException
	 * @throws InterruptedException
	 */
	@SuppressWarnings("static-access")
	@Test
	void testOpenElevatorDoors() throws UnknownHostException, InterruptedException {
		elevatorHost.setExpectedNumMessages(1);

		Thread thread = new Thread(elevatorHost);
		thread.start();
		thread.sleep(2000);

		byte[] buf = new byte[] { 1, 1, 1, -1 };
		DatagramPacket packet = new DatagramPacket(buf, buf.length, InetAddress.getLocalHost(),
				UtilityInformation.ELEVATOR_PORT_NUM);

		scheduler.changeDoorState(packet, UtilityInformation.DoorState.OPEN);

	}

	/**
	 * Test to make sure the send confirm config message send/recieve works.
	 * 
	 * @throws UnknownHostException
	 * @throws InterruptedException
	 */
	@SuppressWarnings("static-access")
	@Test
	void testConfigConfirmMessage() throws UnknownHostException, InterruptedException {
		floorHost.setExpectedNumMessages(1);

		Thread thread = new Thread(floorHost);
		thread.start();
		thread.sleep(2000);

		byte[] buf = new byte[] { 1, -1 };
		DatagramPacket packet = new DatagramPacket(buf, buf.length, InetAddress.getLocalHost(),
				UtilityInformation.FLOOR_PORT_NUM);

		scheduler.sendConfigConfirmMessage(packet);

	}

	/**
	 * Test to make sure the send config packet to elevator send/recieve works.
	 * 
	 * @throws UnknownHostException
	 * @throws InterruptedException
	 */
	@SuppressWarnings("static-access")
	@Test
	void testSendConfigPacketToElevator() throws UnknownHostException, InterruptedException {
		elevatorHost.setExpectedNumMessages(1);

		Thread thread = new Thread(elevatorHost);
		thread.start();
		thread.sleep(2000);

		byte[] buf = new byte[] { 1, 1, 11, -1 };
		DatagramPacket packet = new DatagramPacket(buf, buf.length, InetAddress.getLocalHost(),
				UtilityInformation.ELEVATOR_PORT_NUM);

		scheduler.sendConfigPacketToElevator(packet);

	}

	/**
	 * Test to make sure the send destination to elevator send/recieve works.
	 * 
	 * @throws UnknownHostException
	 * @throws InterruptedException
	 */
	@SuppressWarnings("static-access")
	@Test
	void testExtractFloorRequestedNumberAndGenerateResponseMessageAndActions()
			throws UnknownHostException, InterruptedException {
		elevatorHost.setExpectedNumMessages(1);

		Thread thread = new Thread(elevatorHost);
		thread.start();
		thread.sleep(2000);

		scheduler.setNumElevators((byte) 1);

		byte[] buf = new byte[] { 1, 2, 3, 4, -1 };
		DatagramPacket packet = new DatagramPacket(buf, buf.length, InetAddress.getLocalHost(),
				UtilityInformation.ELEVATOR_PORT_NUM);

		scheduler.extractFloorRequestedNumberAndGenerateResponseMessageAndActions(packet);

	}

	/**
	 * Test to make sure the stop elevator send/recieve works.
	 * 
	 * @throws UnknownHostException
	 * @throws InterruptedException
	 */
	@SuppressWarnings("static-access")
	@Test
	void testStopElevator() throws UnknownHostException, InterruptedException {
		elevatorHost.setExpectedNumMessages(1);
		floorHost.setExpectedNumMessages(1);

		Thread thread = new Thread(elevatorHost);
		Thread thread2 = new Thread(floorHost);
		thread.start();
		thread2.start();
		thread.sleep(1000);
		thread2.sleep(1000);

		scheduler.setNumElevators((byte) 1);

		byte[] buf = new byte[] { 1, 2, 3, -1 };
		DatagramPacket packet = new DatagramPacket(buf, buf.length, InetAddress.getLocalHost(),
				UtilityInformation.ELEVATOR_PORT_NUM);

		scheduler.sendElevatorInDirection(packet, UtilityInformation.ElevatorDirection.STATIONARY);

	}

	/**
	 * Test to make sure the send elevator up send/recieve works.
	 * 
	 * @throws UnknownHostException
	 * @throws InterruptedException
	 */
	@SuppressWarnings("static-access")
	@Test
	void testSendElevatorUp() throws UnknownHostException, InterruptedException {
		elevatorHost.setExpectedNumMessages(1);
		floorHost.setExpectedNumMessages(1);

		Thread thread = new Thread(elevatorHost);
		Thread thread2 = new Thread(floorHost);
		thread.start();
		thread2.start();
		thread.sleep(1000);
		thread2.sleep(1000);

		scheduler.setNumElevators((byte) 4);

		byte[] buf = new byte[] { 1, 2, 3, -1 };
		DatagramPacket packet = new DatagramPacket(buf, buf.length, InetAddress.getLocalHost(),
				UtilityInformation.ELEVATOR_PORT_NUM);

		scheduler.sendElevatorInDirection(packet, UtilityInformation.ElevatorDirection.UP);

	}

	/**
	 * Test to make sure the send elevator down send/recieve works.
	 * 
	 * @throws UnknownHostException
	 * @throws InterruptedException
	 */
	@SuppressWarnings("static-access")
	@Test
	void testSendElevatorDown() throws UnknownHostException, InterruptedException {
		elevatorHost.setExpectedNumMessages(1);
		floorHost.setExpectedNumMessages(1);

		Thread thread = new Thread(elevatorHost);
		Thread thread2 = new Thread(floorHost);
		thread.start();
		thread2.start();
		thread.sleep(1000);
		thread2.sleep(1000);

		scheduler.setNumElevators((byte) 4);

		byte[] buf = new byte[] { 1, 2, 3, -1 };
		DatagramPacket packet = new DatagramPacket(buf, buf.length, InetAddress.getLocalHost(),
				UtilityInformation.ELEVATOR_PORT_NUM);

		scheduler.sendElevatorInDirection(packet, UtilityInformation.ElevatorDirection.DOWN);
	}

	/**
	 * Test to make sure the close elevator doors send/recieve works.
	 * 
	 * @throws UnknownHostException
	 * @throws InterruptedException
	 */
	@SuppressWarnings("static-access")
	@Test
	void testCloseElevatorDoors() throws UnknownHostException, InterruptedException {
		elevatorHost.setExpectedNumMessages(1);

		Thread thread = new Thread(elevatorHost);
		thread.start();
		thread.sleep(2000);

		byte[] buf = new byte[] { 1, 1, 1, -1 };
		DatagramPacket packet = new DatagramPacket(buf, buf.length, InetAddress.getLocalHost(),
				UtilityInformation.ELEVATOR_PORT_NUM);

		scheduler.changeDoorState(packet, UtilityInformation.DoorState.CLOSE);
	}

	/**
	 * Test to make sure the algorithm will add requests as expected.
	 */
	@Test
	void testAddRequestToSchedulerAlgorithm() {
		SchedulerAlgorithm algor = new SchedulerAlgorithm((byte) 0);

		algor.setNumberOfElevators((byte) 4);

		System.out.println(String.format("Making request: %d to %d", 0, 7));
		algor.elevatorRequestMade(
				new Request(System.nanoTime(), (byte) 0, (byte) 7, UtilityInformation.ElevatorDirection.UP));
		algor.printAllInfo();

		System.out.println(String.format("Making request: %d to %d", 0, 10));
		algor.elevatorRequestMade(
				new Request(System.nanoTime(), (byte) 0, (byte) 10, UtilityInformation.ElevatorDirection.UP));
		algor.printAllInfo();

		System.out.println(String.format("Making request: %d to %d", 1, 3));
		algor.elevatorRequestMade(
				new Request(System.nanoTime(), (byte) 1, (byte) 3, UtilityInformation.ElevatorDirection.UP));
		algor.printAllInfo();

		System.out.println(String.format("Making request: %d to %d", 0, 4));
		algor.elevatorRequestMade(
				new Request(System.nanoTime(), (byte) 0, (byte) 4, UtilityInformation.ElevatorDirection.UP));
		algor.printAllInfo();

		System.out.println(String.format("Making request: %d to %d", 9, 1));
		algor.elevatorRequestMade(
				new Request(System.nanoTime(), (byte) 9, (byte) 1, UtilityInformation.ElevatorDirection.DOWN));
		algor.printAllInfo();

		System.out.println(String.format("Making request: %d to %d", 0, 4));
		algor.elevatorRequestMade(
				new Request(System.nanoTime(), (byte) 0, (byte) 4, UtilityInformation.ElevatorDirection.UP));
		algor.printAllInfo();

		System.out.println(String.format("Making request: %d to %d", 7, 10));
		algor.elevatorRequestMade(
				new Request(System.nanoTime(), (byte) 7, (byte) 10, UtilityInformation.ElevatorDirection.UP));
		algor.printAllInfo();

		System.out.println(String.format("Making request: %d to %d", 3, 3));
		algor.elevatorRequestMade(
				new Request(System.nanoTime(), (byte) 3, (byte) 3, UtilityInformation.ElevatorDirection.DOWN));
		algor.printAllInfo();

		System.out.println(String.format("Making request: %d to %d", 9, 1));
		algor.elevatorRequestMade(
				new Request(System.nanoTime(), (byte) 9, (byte) 1, UtilityInformation.ElevatorDirection.DOWN));
		algor.printAllInfo();

		System.out.println(String.format("Making request: %d to %d", 0, 9));
		algor.elevatorRequestMade(
				new Request(System.nanoTime(), (byte) 0, (byte) 9, UtilityInformation.ElevatorDirection.UP));
		algor.printAllInfo();

		System.out.println(String.format("Making request: %d to %d", 7, 1));
		algor.elevatorRequestMade(
				new Request(System.nanoTime(), (byte) 7, (byte) 1, UtilityInformation.ElevatorDirection.DOWN));
		algor.printAllInfo();
	}

	/**
	 * Test to make sure that the arrival at floors is handled properly
	 */
	@Test
	void testElevatorArriveAtFloor() {
		SchedulerAlgorithm algor = new SchedulerAlgorithm((byte) 0);

		byte numElevators = 4;

		algor.setNumberOfElevators(numElevators);

		System.out.println(String.format("Making request: %d to %d", 0, 7));
		algor.elevatorRequestMade(
				new Request(System.nanoTime(), (byte) 0, (byte) 7, UtilityInformation.ElevatorDirection.UP));

		System.out.println(String.format("Making request: %d to %d", 0, 10));
		algor.elevatorRequestMade(
				new Request(System.nanoTime(), (byte) 0, (byte) 10, UtilityInformation.ElevatorDirection.UP));

		System.out.println(String.format("Making request: %d to %d", 1, 3));
		algor.elevatorRequestMade(
				new Request(System.nanoTime(), (byte) 1, (byte) 3, UtilityInformation.ElevatorDirection.UP));

		System.out.println(String.format("Making request: %d to %d", 0, 4));
		algor.elevatorRequestMade(
				new Request(System.nanoTime(), (byte) 0, (byte) 4, UtilityInformation.ElevatorDirection.UP));
		System.out.println(String.format("Making request: %d to %d", 9, 1));
		algor.elevatorRequestMade(
				new Request(System.nanoTime(), (byte) 9, (byte) 1, UtilityInformation.ElevatorDirection.DOWN));

		algor.printAllInfo();

		for (byte elevatorNum = 0; elevatorNum < numElevators; elevatorNum++) {
			algor.elevatorHasReachedFloor((byte) 0, elevatorNum);
		}

		algor.printAllInfo();
	}

	/**
	 * Test to make sure that the algorithm will send elevators in the correct
	 * direction.
	 */
	@Test
	void testWhatDirectionShouldTravel() {
		byte numElevators = 1;

		SchedulerAlgorithm algor = new SchedulerAlgorithm(numElevators);

		System.out.println(String.format("Making request: %d to %d", 2, 7));
		algor.elevatorRequestMade(
				new Request(System.nanoTime(), (byte) 2, (byte) 7, UtilityInformation.ElevatorDirection.UP));

		algor.elevatorHasReachedFloor((byte) 0, (byte) 0);

		algor.elevatorHasReachedFloor((byte) 3, (byte) 0);
		assertEquals(algor.whatDirectionShouldTravel((byte) 0), UtilityInformation.ElevatorDirection.DOWN);

		algor.elevatorHasReachedFloor((byte) 1, (byte) 0);
		assertEquals(algor.whatDirectionShouldTravel((byte) 0), UtilityInformation.ElevatorDirection.UP);
	}

}
