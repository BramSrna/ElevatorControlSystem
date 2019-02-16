import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import org.junit.jupiter.api.*;


public class ElevatorSubsystemTests {
	 private TestHost host;
	 private Elevator_Subsystem elevatorSubsystem;
	    
	    @BeforeEach
	    void setUp() throws Exception {
	        host = new TestHost(1,
				    UtilityInformation.SCHEDULER_PORT_NUM,
				    UtilityInformation.ELEVATOR_PORT_NUM);
	        
	        elevatorSubsystem = new Elevator_Subsystem();
	    }
	    
	    @AfterEach
	    void tearDown() throws Exception {
	        host.teardown();
	        host = null;
	        
	        elevatorSubsystem.sendPacket = null;
	        elevatorSubsystem.receivePacket = null;
		elevatorSubsystem.sendSocket.close();
		elevatorSubsystem.receiveSocket.close();
	        elevatorSubsystem = null;
	    }
	    
	    
    /**
     * testSendData
     * 
     * Tests to see if the ElevatorSubsystem can successfully send packets
     * 
     * @input   None
     * 
     * @return  None
     */
	@Test
	public void testSendData() throws UnknownHostException
	{
	//Setup the echo server to reply back and run it on its own thread
        //Thread t = new Thread(host);
        //t.start();
        
        //Send the data to the host
        elevatorSubsystem.sendData(new byte[] { 0x20, 0x20, 0x20 },
       				   InetAddress.getLocalHost(),
        			   UtilityInformation.SCHEDULER_PORT_NUM);
        
        //* TODO Check to see if the packet received is the same as the sent one
        
	}
	
    /**
     * testReceiveData
     * 
     * Tests to see if the ElevatorSubsystem can successfully receive packets.
     * 
     * @input   None
     * 
     * @return  None
     */
	
	@Test
	public void testReceiveData() throws IOException
	{	
	//Setup the echo server to reply back and run it on its own thread
        Thread t = new Thread(host);
        t.start();
        
        host.sendPacket(new byte[] { 0x0, 0x20, 0x20 }, //Send a dummy "config" message (must be a valid message or System.exit is called)
        		InetAddress.getLocalHost(), 
        		UtilityInformation.ELEVATOR_PORT_NUM);
        
        
        elevatorSubsystem.receiveData();
	}
}
