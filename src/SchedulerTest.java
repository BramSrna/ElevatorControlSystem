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

	@BeforeEach
	void setUp() throws Exception {
		scheduler = new Scheduler();
	}

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

//	@Test
//	void testOpenElevatorDoors() throws UnknownHostException {
//		host = new TestHost(1, UtilityInformation.ELEVATOR_PORT_NUM);
//		Thread thread = new Thread(host);
//		thread.start();
//
//		byte[] buf = new byte[] { 1, -1 };
//		DatagramPacket packet = new DatagramPacket(buf, buf.length, InetAddress.getLocalHost(),
//				UtilityInformation.ELEVATOR_PORT_NUM);
//
//		scheduler.openElevatorDoors(packet);
//
//	}

//	@Test
//	void testConfigConfirmMessage() throws UnknownHostException {
//		host = new TestHost(1, UtilityInformation.FLOOR_PORT_NUM);
//		Thread thread = new Thread(host);
//		thread.start();
//
//		byte[] buf = new byte[] { 1, -1 };
//		DatagramPacket packet = new DatagramPacket(buf, buf.length, InetAddress.getLocalHost(),
//				UtilityInformation.FLOOR_PORT_NUM);
//
//		scheduler.sendConfigConfirmMessage(packet);
//
//	}

	@Test
	void testSendConfigPacketToElevator() throws UnknownHostException {
		host = new TestHost(1, UtilityInformation.ELEVATOR_PORT_NUM);
		Thread thread = new Thread(host);
		thread.start();

		byte[] buf = new byte[] { 1, -1 };
		DatagramPacket packet = new DatagramPacket(buf, buf.length, InetAddress.getLocalHost(),
				UtilityInformation.ELEVATOR_PORT_NUM);

		scheduler.sendConfigPacketToElevator(packet);

	}

//	@Test
//	void testExtractFloorRequestedNumberAndGenerateResponseMessageAndActions() throws UnknownHostException {
//		host = new TestHost(1, UtilityInformation.ELEVATOR_PORT_NUM);
//		Thread thread = new Thread(host);
//		thread.start();
//
//		byte[] buf = new byte[] { 1, 2, 3, 4, -1 };
//		DatagramPacket packet = new DatagramPacket(buf, buf.length, InetAddress.getLocalHost(),
//				UtilityInformation.ELEVATOR_PORT_NUM);
//
//		scheduler.extractFloorRequestedNumberAndGenerateResponseMessageAndActions(packet);
//
//	}

//	@Test
//	void testStopElevator() throws UnknownHostException {
//		host = new TestHost(1, UtilityInformation.ELEVATOR_PORT_NUM);
//		Thread thread = new Thread(host);
//		thread.start();
//		host2 = new TestHost(1, UtilityInformation.FLOOR_PORT_NUM);
//		Thread thread2 = new Thread(host2);
//		thread2.start();
//		byte[] buf = new byte[] { 1, -1 };
//		DatagramPacket packet = new DatagramPacket(buf, buf.length, InetAddress.getLocalHost(),
//				UtilityInformation.ELEVATOR_PORT_NUM);
//
//		scheduler.stopElevator(packet);
//
//	}

//	@Test
//	void testSendElevatorUp() throws UnknownHostException {
//		host = new TestHost(1, UtilityInformation.ELEVATOR_PORT_NUM);
//		Thread thread = new Thread(host);
//		thread.start();
//		host2 = new TestHost(1, UtilityInformation.FLOOR_PORT_NUM);
//		Thread thread2 = new Thread(host2);
//		thread2.start();
//		byte[] buf = new byte[] { 1, -1 };
//		DatagramPacket packet = new DatagramPacket(buf, buf.length, InetAddress.getLocalHost(),
//				UtilityInformation.ELEVATOR_PORT_NUM);
//
//		scheduler.sendElevatorUp(packet);
//
//	}

//	@Test
//	void testSendElevatorDown() throws UnknownHostException {
//		host = new TestHost(1, UtilityInformation.ELEVATOR_PORT_NUM);
//		Thread thread = new Thread(host);
//		thread.start();
//		host2 = new TestHost(1, UtilityInformation.FLOOR_PORT_NUM);
//		Thread thread2 = new Thread(host2);
//		thread2.start();
//		byte[] buf = new byte[] { 1, -1 };
//
//		DatagramPacket packet = new DatagramPacket(buf, buf.length, InetAddress.getLocalHost(),
//				UtilityInformation.ELEVATOR_PORT_NUM);
//
//		scheduler.sendElevatorDown(packet);
//
//	}

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
