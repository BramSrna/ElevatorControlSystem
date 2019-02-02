

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
    private TestHost host;
    private String filePath;
    private PrintWriter writer;
    private FloorSubsystem testController;
    private int numFloors;
    private int numElevators;
    private int requestCount;
    
    private ArrayList<Integer[]> reqs;
    
    @BeforeEach
    void setUp() throws Exception {
    	System.out.println("------------------------- SETTING UP NEW TEST... -------------------------");
    	numFloors = 11;
    	numElevators = 1;
        host = new TestHost(0);
        
        filePath = "test.txt";
        writer = null;
        
        // Create the file
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
     * @return  None
     */
    @Test
    void testSampleInput() {
    	host.setExpectedNumMessages(0);
    	
    	assertEquals(reqs.size(), requestCount);
        
        // Print the requests
        for (Integer[] req : reqs) {
            System.out.println(String.format("CHECK OUTPUT: %s", Arrays.toString(req) + "\n"));
        }
    }
    
    @Test
    void testConfigMessage() {        
        host.setExpectedNumMessages(1);
        // Create a thread for the test host to run off
        Thread t = new Thread(host);
        t.start();
        
        // Send Config signal
        testController.sendConfigurationSignal(numFloors, numElevators);
        
        
    }
    
    @Test
    void testElevatorRequestMessage() {        
        host.setExpectedNumMessages(1);
        // Create a thread for the test host to run off
        Thread t = new Thread(host);
        t.start();
        
        int sourceFloor = 0;
        int endFloor = 15;
        UtilityInformation.ElevatorDirection dir = UtilityInformation.ElevatorDirection.UP;
        
        // Send Config signal
        testController.sendElevatorRequest(sourceFloor, endFloor, dir);
        
        
    }
    
    @Test
    void testTeardownMessage() {
        host.setExpectedNumMessages(1);
        // Create a thread for the test host to run off
        Thread t = new Thread(host);
        t.start();
        
        // Send teardown signal
        testController.teardown();
        
        testController = new FloorSubsystem(numFloors, numElevators);
    }
    
    @Test
    void testElevatorRequestTiming() {
    	// Select the two closer timed requests for comparison
    	Integer[] req1 = reqs.get(1);
    	Integer[] req2 = reqs.get(2);
    	
    	// Grab the timed requests respective values in ms
    	int msReq1 = req1[0];
    	int msReq2 = req2[0];
    	
    	// caluclate the calculated value and set what it should be
    	int expectedVal = 1203300;
    	int calcVal = msReq2 - msReq1;
    	
    	// Check if the timed values are within a acceptable range of each other
    	assertTrue(calcVal > expectedVal - 1000 && calcVal < expectedVal + 1000
    			, "Calculated time should be wthin 1000 ms of expected time");
    }
    
    @Test
    void testFloorRangeCheck() {
    	int[] toCheck = FloorSubsystem.getValidFloorValueRange();
    	
    	assertEquals(toCheck[0], 1, "Minimum floor configuration should be 1");
    	assertEquals(toCheck[1], 1000, "Maximum floor configuration should be 1000");
    }
    
    @Test
    void testElevatorRangeCheck() {
    	int[] toCheck = FloorSubsystem.getValidElevatorValueRange();
    	
    	assertEquals(toCheck[0], 1, "Minimum # of elevators configuration should be 1");
    	assertEquals(toCheck[1], 1, "Maximum # of elevators configuration should be 1");
    }
    
    @Test
    void testSetNumElevators() {
        int newNumElevators = 1;
        
        testController.setNumElevators(newNumElevators);
        
        assertEquals(testController.getNumElevators(), newNumElevators);
        
        ArrayList<Floor> check = testController.getListOfFloors();
        
        for (Floor currFloor : check) {
            assertEquals(currFloor.getNumElevatorShafts(), newNumElevators);
        }
    }
    
    @Test
    void testSetNumFloors() {
        int newNumFloors = 20;
        
        testController.setNumFloors(newNumFloors);
        
        ArrayList<Floor> check = testController.getListOfFloors();
        
        assertEquals(check.size(), newNumFloors);
        
        int checkFloorNums[] = new int[newNumFloors];
        
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
        
        newNumFloors = 5;
        
        testController.setNumFloors(newNumFloors);
        
        check = testController.getListOfFloors();
        
        assertEquals(check.size(), newNumFloors);
        
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
