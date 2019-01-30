import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SchedulerTest {

	private Scheduler scheduler;

	@BeforeEach
	void setUp() throws Exception {
		scheduler = new Scheduler();
	}

	@AfterEach
	void tearDown() throws Exception {
		scheduler = null;
	}

	@Test
	void test() {
		assertTrue(true);
	}

}
