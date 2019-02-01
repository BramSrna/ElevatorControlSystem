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
		host = new TestHost(1);
	}

	@AfterEach
	void tearDown() throws Exception {
		scheduler = null;
		host.teardown();
	}

	@Test
	void test() throws UnknownHostException {
		host.setExpectedNumMessages(1);
		Thread thread = new Thread(host);
		thread.start();

		byte[] buf = new byte[] { 1, -1 };
		DatagramPacket packet = new DatagramPacket(buf, buf.length, InetAddress.getLocalHost(),
				UtilityInformation.ELEVATOR_PORT_NUM);

		scheduler.openElevatorDoors(packet);

	}

}
