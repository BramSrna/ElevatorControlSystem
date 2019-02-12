import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SchedulerTest {

	private Scheduler scheduler;
	private TestHost host;
	private TestHost host2;

	/**
	 * Initialize scheduler
	 * 
	 * @throws Exception
	 */
	@BeforeEach
	void setUp() throws Exception {
		scheduler = new Scheduler();
	}

	/**
	 * Make sure the scheduler and hosts are closed/set to NULL.
	 * 
	 * @throws Exception
	 */
	@AfterEach
	void tearDown() throws Exception {
		scheduler.socketTearDown();
		scheduler = null;
		host.teardown();
		host = null;
		if (host2 != null) {
			host2.teardown();
			host2 = null;
		}
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
		host = new TestHost(1, UtilityInformation.ELEVATOR_PORT_NUM);
		Thread thread = new Thread(host);
		thread.start();
		thread.sleep(2000);
		byte[] buf = new byte[] { 1, -1 };
		DatagramPacket packet = new DatagramPacket(buf, buf.length, InetAddress.getLocalHost(),
				UtilityInformation.ELEVATOR_PORT_NUM);

		scheduler.openElevatorDoors(packet);

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
		host = new TestHost(1, UtilityInformation.FLOOR_PORT_NUM);
		Thread thread = new Thread(host);
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
		host = new TestHost(1, UtilityInformation.ELEVATOR_PORT_NUM);
		Thread thread = new Thread(host);
		thread.start();
		thread.sleep(2000);
		byte[] buf = new byte[] { 1, -1 };
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
		host = new TestHost(1, UtilityInformation.ELEVATOR_PORT_NUM);
		Thread thread = new Thread(host);
		thread.start();
		thread.sleep(2000);
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
		host = new TestHost(1, UtilityInformation.ELEVATOR_PORT_NUM);
		Thread thread = new Thread(host);
		host2 = new TestHost(1, UtilityInformation.FLOOR_PORT_NUM);
		Thread thread2 = new Thread(host2);
		thread.start();
		thread2.start();
		thread.sleep(1000);
		thread2.sleep(1000);
		byte[] buf = new byte[] { 1, -1 };
		DatagramPacket packet = new DatagramPacket(buf, buf.length, InetAddress.getLocalHost(),
				UtilityInformation.ELEVATOR_PORT_NUM);
		scheduler.stopElevator(packet, 0);

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
		host = new TestHost(1, UtilityInformation.ELEVATOR_PORT_NUM);
		Thread thread = new Thread(host);
		host2 = new TestHost(1, UtilityInformation.FLOOR_PORT_NUM);
		Thread thread2 = new Thread(host2);
		thread.start();
		thread2.start();
		thread.sleep(1000);
		thread2.sleep(1000);
		byte[] buf = new byte[] { 1, -1 };
		DatagramPacket packet = new DatagramPacket(buf, buf.length, InetAddress.getLocalHost(),
				UtilityInformation.ELEVATOR_PORT_NUM);
		scheduler.sendElevatorUp(packet, 0);

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
		host = new TestHost(1, UtilityInformation.ELEVATOR_PORT_NUM);
		Thread thread = new Thread(host);
		host2 = new TestHost(1, UtilityInformation.FLOOR_PORT_NUM);
		Thread thread2 = new Thread(host2);
		thread.start();
		thread2.start();
		thread.sleep(1000);
		thread2.sleep(1000);
		byte[] buf = new byte[] { 1, -1 };
		DatagramPacket packet = new DatagramPacket(buf, buf.length, InetAddress.getLocalHost(),
				UtilityInformation.ELEVATOR_PORT_NUM);
		scheduler.sendElevatorDown(packet, 0);

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
		host = new TestHost(1, UtilityInformation.ELEVATOR_PORT_NUM);
		Thread thread = new Thread(host);
		thread.start();
		thread.sleep(2000);
		byte[] buf = new byte[] { 1, -1 };
		DatagramPacket packet = new DatagramPacket(buf, buf.length, InetAddress.getLocalHost(),
				UtilityInformation.ELEVATOR_PORT_NUM);
		scheduler.closeElevatorDoors(packet);

	}

}
