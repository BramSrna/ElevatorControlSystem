import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.*;


public class ElevatorTests {
	private Elevator elevator;
	    
    @BeforeEach
    void setUp() throws Exception {
        Elevator_Subsystem controller = new Elevator_Subsystem();
        
        elevator = new Elevator(controller, 0); //Elevator #0
    }
    
    @AfterEach
    void tearDown() throws Exception {
        elevator = null;
    }

    /**
     * testGoUp
     * 
     * Tests the Elevators ability to move up a floor
     * 
     * @input   None
     * 
     * @return  None
     */
	@Test
	public void testGoUp()
	{
		//The elevator's floor number before moving
		int previousFloor = elevator.getCurrentFloor();
		elevator.goUp();
		
		assertEquals(previousFloor + 1,elevator.getCurrentFloor());
	}
	
    /**
     * testGoUp
     * 
     * Tests the Elevators ability to move down a floor
     * 
     * @input   None
     * 
     * @return  None
     */
	@Test
	public void testGoDown()
	{
		//The elevator's floor number before moving
		int previousFloor = elevator.getCurrentFloor();
		elevator.goDown();
		
		assertEquals(previousFloor - 1,elevator.getCurrentFloor());
	}

    /**
     * testOpenDoor
     * 
     * Tests the Elevators ability to open its doors
     * 
     * @input   None
     * 
     * @return  None
     */
	@Test
	public void testOpenDoor()
	{
		//Confirm that the elevator doors are closed beforehand
		elevator.closeDoor();
		
		//The doorState before opening (i.e. closed)
		Elevator.doorState previousDoorState = elevator.getDoorState();
		elevator.openDoor();
		
		assertNotEquals(previousDoorState,elevator.getDoorState());
	}

    /**
     * testCloseDoor
     * 
     * Tests the Elevators ability to close its doors
     * 
     * @input   None
     * 
     * @return  None
     */
	@Test
	public void testCloseDoor()
	{
		//Confirm that the elevator doors are open beforehand
		elevator.openDoor();
		
		//The doorState before closing (i.e. opened)
		Elevator.doorState previousDoorState = elevator.getDoorState();
		elevator.closeDoor();
		
		assertNotEquals(previousDoorState,elevator.getDoorState());
	}
}
