import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.*;


public class ElevatorTests {
	private Elevator elevator;
	private Elevator_Subsystem controller;
	    
    @BeforeEach
    void setUp() throws Exception {
        int numFloors = 10;
        
        controller = new Elevator_Subsystem();
        controller.configSubsystem(numFloors, 1);
        
        elevator = new Elevator(controller, 0, numFloors); //Elevator #0
    }
    
    @AfterEach
    void tearDown() throws Exception {
        elevator = null;
        controller.teardown();
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
		elevator.move(UtilityInformation.ElevatorDirection.UP);
		
		assertEquals(previousFloor + 1, elevator.getCurrentFloor());
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
		
		// Move up first so that the move is not invalid
		elevator.move(UtilityInformation.ElevatorDirection.UP);
		assertEquals(previousFloor + 1, elevator.getCurrentFloor());
		
		elevator.move(UtilityInformation.ElevatorDirection.DOWN);
		
		assertEquals(previousFloor, elevator.getCurrentFloor());
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
		elevator.changeDoorState(UtilityInformation.DoorState.CLOSE);
		
		//The doorState before opening (i.e. closed)
		UtilityInformation.DoorState previousDoorState = elevator.getDoorState();
		elevator.changeDoorState(UtilityInformation.DoorState.OPEN);
		
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
		elevator.changeDoorState(UtilityInformation.DoorState.OPEN);
		
		//The doorState before closing (i.e. opened)
		UtilityInformation.DoorState previousDoorState = elevator.getDoorState();
		elevator.changeDoorState(UtilityInformation.DoorState.CLOSE);
		
		assertNotEquals(previousDoorState,elevator.getDoorState());
	}
}
