import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.InetAddress;
import java.net.UnknownHostException;
import org.junit.jupiter.api.*;


public class ElevatorSubsystemTests {
	 private TestHost host;
	 private Elevator elevator;
	    
	    @BeforeEach
	    void setUp() throws Exception {
	        host = new TestHost(1, 
                                UtilityInformation.SCHEDULER_PORT_NUM, 
                                UtilityInformation.ELEVATOR_PORT_NUM);
	        elevator = new Elevator(0);
	    }
	    
	    @AfterEach
	    void tearDown() throws Exception {
	        host.teardown();
	        host = null;

			// Clean up elevator
//	        elevator.sendPacket = null;
//			elevator.receivePacket = null;
//			elevator.sendSocket.close();
//			elevator.receiveSocket.close();
	        elevator = null;
	    }
	    
	    
	    /**
	     * testSendData
	     * 
	     * Tests to see if the Elevator can successfully send packets
	     * 
	     * @input   None
	     * 
	     * @return  None
	     */
	@Test
	public void testSendData() throws UnknownHostException
	{
		//Setup the echo server to reply back and run it on its own thread
		host.setExpectedNumMessages(1);
        Thread t = new Thread(host);
        t.start();
        
        //Send the data to the host
//        elevator.sendData(new byte[] { 0x20, 0x20, 0x20 },
//        				  InetAddress.getLocalHost(),
//        				  UtilityInformation.SCHEDULER_PORT_NUM
//        				  );
        
        /*TODO*/
        //Receive a packet and decode it to see if it holds the same data
	}
	
    /**
     * testReceiveData (WIP)
     * 
     * Tests to see if the Elevator can successfully receive packets.
     * 
     * @input   None
     * 
     * @return  None
     */
	/* TODO
	@Test
	public void testReceiveData() throws IOException
	{
		Elevator elevator = new Elevator();
		
		host.setExpectedNumMessages(1);
        Thread t = new Thread(host);
        t.start();
        
        DatagramSocket _sendsocket = new DatagramSocket();
        byte[] _data = new byte[0x01];
        _sendsocket.send(new DatagramPacket(_data,
        									_data.length,
        									InetAddress.getLocalHost(),
        									UtilityInformation.SCHEDULER_PORT_NUM)
        				);
        _sendsocket.close();
	}
	*/
	
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