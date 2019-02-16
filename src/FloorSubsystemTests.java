import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

class FloorSubsystemTests {
    // TestHost used to receive and echo signals
    private TestHost host;
    
    // Variables used for writing the sample test file
    private String filePath;
    private PrintWriter writer;
    
    // Number of requests in the sample file
    private int requestCount;
    
    // FloorSubsystem used for testing
    private FloorSubsystem testController;
    
    // Configuration values for creating the test FloorSubsystem
    private int numFloors;
    private int numElevators;
    
    // The parsed requests from the sample test file
    private ArrayList<Integer[]> reqs;
    
    /**
     * setUp
     * 
     * Sets up the test environment for each test.
     * Initializes the needed objects and writes a
     * sample test file.
     * 
     * @throws Exception    Throws an exception if an error occurs
     * 
     * @param   None
     * 
     * @return  void
     */
    @BeforeEach
    void setUp() throws Exception {
    	System.out.println("------------------------- SETTING UP NEW TEST... -------------------------");
    	// Set the number of floors and elevators
    	numFloors = 11;
    	numElevators = 1;
    	
    	// Initialize the TestHost for receiving signalsS
        host = new TestHost(0, 
                            UtilityInformation.SCHEDULER_PORT_NUM,
                            UtilityInformation.FLOOR_PORT_NUM);
        
        host.disableResponse();
        
        filePath = "test.txt";
        writer = null;
        
        // Create the sample test file
        try {
            writer = new PrintWriter(filePath, StandardCharsets.UTF_8);
        } catch (IOException e1) {
            System.out.println("Error: Unable to write to test text file.");
            e1.printStackTrace();
            System.exit(1);
        }
        
        requestCount = 5;
        
        // Write the requests to the text file
        writer.println("14:05:15.0 2 Up 4");
        writer.println("03:14:15.9 7 down 0");
        writer.println("22:00:59.9 3 uP 8");
        writer.println("00:56:42.7 8 UP 9");
        writer.println("03:34:19.2 6 down 1");
        
        writer.close();
        
        // Parse the created file
        testController = new FloorSubsystem(numFloors, numElevators);
        testController.parseInputFile(filePath);
        
        // Grab the arrayList of requests for use later
        reqs = testController.getRequests();
        
        System.out.println("------------------------- FINISHED SETUP -------------------------");
        System.out.println("------------------------- STARTING TEST -------------------------");
        
    }
    
    /**
     * tearDown
     * 
     * Tear down the test environment after each test.
     * Deletes the test file. Tears down the created
     * objects and set the variables to null.
     * 
     * @throws Exception    Throws an exception when an error occurs
     * 
     * @param   None
     * 
     * @return  None
     */
    @AfterEach
    void tearDown() throws Exception {
    	System.out.println("------------------------- FINISHED TEST -------------------------");
    	System.out.println("------------------------- TEARING DOWN NEW TEST... -------------------------");
    	
        // Delete the test text file
        try {
            Files.deleteIfExists(Paths.get(filePath));
        } catch (Exception e) {
            System.out.println("Error: Unable to delete test text file.");
            e.printStackTrace();
            System.exit(1);
        }
        
        // Tear down the various datagram sockets in the Floor subsystem and test host
    	testController.teardown();
    	host.teardown();
    	
        host = null;
        testController = null;
        
        System.out.println("------------------------- FINISHED TEARDOWN -------------------------");
    }

    /**
     * testSampleInput
     * 
     * Tests the FloorSubsystem with a created input file.
     * Output needs to be checked to ensure requests are created properly.
     * 
     * @input   None
     * 
     * @return  void
     */
    @Test
    void testSampleInput() {
    	// Check that the proper amount of requests were read in
    	assertEquals(reqs.size(), requestCount);
        
        // Print the requests
        for (Integer[] req : reqs) {
            System.out.println(String.format("CHECK OUTPUT: %s", Arrays.toString(req) + "\n"));
        }
    }
    
    /**
     * testConfigMessage
     * 
     * Tests that the configuration message is send and
     * that a response is received. Output should be checked
     * to ensure that the test completes successfully.
     * 
     * @param   None
     * 
     * @return  void
     */
    @Test
    void testConfigMessage() {        
        host.setExpectedNumMessages(1);
        
        host.enableResponse();
        
        // Create a thread for the test host to run off
        Thread t = new Thread(host);
        t.start();
        
        // Send Config signal
        testController.sendConfigurationSignal(numFloors, numElevators);
    }
    
    /**
     * testElevatorRequestMessage
     * 
     * Tests that an elevator request is send out
     * properly. Output should be checked to ensure
     * that the test completes successfully.
     * 
     * @param   None
     * 
     * @return  void
     */
    @Test
    void testElevatorRequestMessage() { 
        host.setExpectedNumMessages(1);
        
        // Create a thread for the test host to run off
        Thread t = new Thread(host);
        t.start();
        
        // Create the information to send in the request
        int sourceFloor = 0;
        int endFloor = 15;
        UtilityInformation.ElevatorDirection dir = UtilityInformation.ElevatorDirection.UP;
        
        // Send Config signal
        testController.sendElevatorRequest(sourceFloor, endFloor, dir);
        
        
    }
    
    /**
     * testTeardownMessage
     * 
     * Test that a teardown message can be sent properly
     * through the system. Output needs to be checked to
     * endure the test is successful.
     * 
     * @param   None
     * 
     * @return  void
     */
    @Test
    void testTeardownMessage() {
        host.setExpectedNumMessages(1);
        
        // Create a thread for the test host to run off
        Thread t = new Thread(host);
        t.start();
        
        // Send teardown signal
        testController.teardown();
        
        // Remake the FloorSubsystem so that
        // the teardown method does not crash
        testController = new FloorSubsystem(numFloors, numElevators);
    }
    
    /**
     * testElevatorRequestTiming
     * 
     * Test that the conversion of the parsed times to
     * milliseconds is done properly and that the
     * requests are stored in ascending order.
     * 
     * @param   None
     * 
     * @return  void
     */
    @Test
    void testElevatorRequestTiming() {
    	// Select the two closer timed requests for comparison
    	Integer[] req1 = reqs.get(1);
    	Integer[] req2 = reqs.get(2);
    	
    	// Grab the timed requests respective values in ms
    	int msReq1 = req1[0];
    	int msReq2 = req2[0];
    	
    	// Calculate the calculated value and set what it should be
    	int expectedVal = -66400007;
    	int calcVal = msReq2 - msReq1;
    	
    	// Check if the timed values are within a acceptable range of each other
    	assertTrue(calcVal > expectedVal - 1000 && calcVal < expectedVal + 1000
    			, "Calculated time should be wthin 1000 ms of expected time");
    }
    
    /**
     * testFloorRangeCheck
     * 
     * Test that the range of valid floors works properly.
     * 
     * @param   None
     * 
     * @return  void
     */
    @Test
    void testFloorRangeCheck() {
    	int[] toCheck = FloorSubsystem.getValidFloorValueRange();
    	
    	assertEquals(toCheck[0], 1, "Minimum floor configuration should be 1");
    	assertEquals(toCheck[1], 1000, "Maximum floor configuration should be 1000");
    }
    
    /**
     * testElevatorRangeCheck
     * 
     * Test that the range of valid elevator amounts works properly.
     * 
     * @param   None
     * 
     * @return  void
     */
    @Test
    void testElevatorRangeCheck() {
    	int[] toCheck = FloorSubsystem.getValidElevatorValueRange();
    	
    	assertEquals(toCheck[0], 1, "Minimum # of elevators configuration should be 1");
    	assertEquals(toCheck[1], 2, "Maximum # of elevators configuration should be 2");
    }
    
    /**
     * testSetNumElevators
     * 
     * Test that the number of elevators can be set
     * properly and check that the updated value is
     * propagated down to the Floor objects.
     * 
     * @param   None
     * 
     * @return  void
     */
    @Test
    void testSetNumElevators() {
        int newNumElevators = 1;
        
        // Set the new number of elevators
        testController.setNumElevators(newNumElevators);
        
        assertEquals(testController.getNumElevators(), newNumElevators);
        
        // Check that the Floor objects also have the new value
        ArrayList<Floor> check = testController.getListOfFloors();
        
        for (Floor currFloor : check) {
            assertEquals(currFloor.getNumElevatorShafts(), newNumElevators);
        }
    }
    
    /**
     * testSetNummFloors
     * 
     * Test that the number of floors in the FloorSubsystem
     * can be set properly. Check that the list of Floors
     * is properly maintained at that the proper Floors are
     * removed when the number is dropped.
     * 
     * @param   None
     * 
     * @return  void
     */
    @Test
    void testSetNumFloors() {
        int newNumFloors = 20;
        
        // Set the new number of floors
        testController.setNumFloors(newNumFloors);
        
        // Check that the new number of floors was set
        ArrayList<Floor> check = testController.getListOfFloors();
        
        assertEquals(check.size(), newNumFloors);
        
        // Create an array used to check that the proper floor numbers were kept
        int checkFloorNums[] = new int[newNumFloors];
        
        // Set the array to all -1
        for (int i = 0; i < newNumFloors; i++) {
            checkFloorNums[i] = -1;
        }
        
        // Update each index to the corresponding floor value
        for (Floor currFloor : check) {
            checkFloorNums[currFloor.getFloorNumber()] = currFloor.getFloorNumber();
        }
        
        // Check that the proper floors were kept
        assertEquals(checkFloorNums[0], 0);        
        for (int i = 1; i < newNumFloors - 1; i++) {
            checkFloorNums[i] = checkFloorNums[i - 1];
        }
        assertEquals(checkFloorNums[newNumFloors - 1], newNumFloors - 1);  
        
        // Repeat the above process, this time lowering the number
        // of floors, instead of increasing it
        newNumFloors = 5;
        
        // Set the number of floors
        testController.setNumFloors(newNumFloors);
        
        check = testController.getListOfFloors();
        
        assertEquals(check.size(), newNumFloors);
        
        // Repeat the array check for ensuring that
        // the proper Floor objects were deleted.
        checkFloorNums = new int[newNumFloors];
        
        for (int i = 0; i < newNumFloors; i++) {
            checkFloorNums[i] = -1;
        }
        
        for (Floor currFloor : check) {
            checkFloorNums[currFloor.getFloorNumber()] = currFloor.getFloorNumber();
        }
        
        assertEquals(checkFloorNums[0], 0);        
        for (int i = 1; i < newNumFloors - 1; i++) {
            checkFloorNums[i] = checkFloorNums[i - 1];
        }
        assertEquals(checkFloorNums[newNumFloors - 1], newNumFloors - 1);  
    }
}
