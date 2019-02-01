import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SchedulerTest {

	private Scheduler scheduler;
	private TestHost host;

	@BeforeEach
	void setUp() throws Exception {
		scheduler = new Scheduler();
		host = new TestHost(0);
	}

	@AfterEach
	void tearDown() throws Exception {
		scheduler = null;
//		host.teardown();
	}

	@Test
	void testOpenElevatorDoors() throws UnknownHostException {
		host.setExpectedNumMessages(1);
		Thread thread = new Thread(host);
		thread.start();

		byte[] buf = new byte[] { 1, -1 };
		DatagramPacket packet = new DatagramPacket(buf, buf.length, InetAddress.getLocalHost(),
				UtilityInformation.ELEVATOR_PORT_NUM);

		scheduler.openElevatorDoors(packet);

	}

	@Test
	void testConfigConfirmMessage() throws UnknownHostException {
		host.setExpectedNumMessages(1);
		Thread thread = new Thread(host);
		thread.start();

		byte[] buf = new byte[] { 1, -1 };
		DatagramPacket packet = new DatagramPacket(buf, buf.length, InetAddress.getLocalHost(),
				UtilityInformation.ELEVATOR_PORT_NUM);

		scheduler.sendConfigConfirmMessage(packet);

	}

	@Test
	void testSendConfigPacketToElevator() throws UnknownHostException {
		host.setExpectedNumMessages(1);
		Thread thread = new Thread(host);
		thread.start();

		byte[] buf = new byte[] { 1, -1 };
		DatagramPacket packet = new DatagramPacket(buf, buf.length, InetAddress.getLocalHost(),
				UtilityInformation.ELEVATOR_PORT_NUM);

		scheduler.sendConfigPacketToElevator(packet);

	}

	@Test
	void testExtractFloorRequestedNumberAndGenerateResponseMessageAndActions() throws UnknownHostException {
		host.setExpectedNumMessages(1);
		Thread thread = new Thread(host);
		thread.start();

		byte[] buf = new byte[] { 1, -1 };
		DatagramPacket packet = new DatagramPacket(buf, buf.length, InetAddress.getLocalHost(),
				UtilityInformation.ELEVATOR_PORT_NUM);

		scheduler.extractFloorRequestedNumberAndGenerateResponseMessageAndActions(packet);

	}

	@Test
	void testStopElevator() throws UnknownHostException {
		host.setExpectedNumMessages(2);
		Thread thread = new Thread(host);
		thread.start();

		byte[] buf = new byte[] { 1, -1 };
		DatagramPacket packet = new DatagramPacket(buf, buf.length, InetAddress.getLocalHost(),
				UtilityInformation.ELEVATOR_PORT_NUM);

		scheduler.stopElevator(packet);

	}

	@Test
	void testSendElevatorUp() throws UnknownHostException {
		host.setExpectedNumMessages(2);
		Thread thread = new Thread(host);
		thread.start();

		byte[] buf = new byte[] { 1, -1 };
		DatagramPacket packet = new DatagramPacket(buf, buf.length, InetAddress.getLocalHost(),
				UtilityInformation.ELEVATOR_PORT_NUM);

		scheduler.sendElevatorUp(packet);

	}

	@Test
	void testSendElevatorDown() throws UnknownHostException {
		host.setExpectedNumMessages(2);
		Thread thread = new Thread(host);
		thread.start();

		byte[] buf = new byte[] { 1, -1 };
		DatagramPacket packet = new DatagramPacket(buf, buf.length, InetAddress.getLocalHost(),
				UtilityInformation.ELEVATOR_PORT_NUM);

		scheduler.sendElevatorDown(packet);

	}

	@Test
	void testCloseElevatorDoors() throws UnknownHostException {
		host.setExpectedNumMessages(1);
		Thread thread = new Thread(host);
		thread.start();

		byte[] buf = new byte[] { 1, -1 };
		DatagramPacket packet = new DatagramPacket(buf, buf.length, InetAddress.getLocalHost(),
				UtilityInformation.ELEVATOR_PORT_NUM);

		scheduler.closeElevatorDoors(packet);

	}

}
