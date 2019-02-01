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
	void test() {
		host.setExpectedNumMessages(1);
		Thread thread = new Thread(host);
		thread.start();

		// scheduler.sendAMessage();

	}

}
