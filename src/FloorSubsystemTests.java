import java.io.IOException;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FloorSubsystemTests {
    private TestHost host;
    
    @BeforeEach
    void setUp() throws Exception {
        host = new TestHost(1);
    }
    
    @AfterEach
    void tearDown() throws Exception {
        host.teardown();
        host = null;
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
        String filePath = "test.txt";
        PrintWriter writer = null;
        
        // Create the file
        try {
            writer = new PrintWriter(filePath, StandardCharsets.UTF_8);
        } catch (IOException e1) {
            System.out.println("Error: Unable to write to test text file.");
            e1.printStackTrace();
            System.exit(1);
        }
        
        // Write the requests to the text file
        writer.println("14:05:15.0 2 Up 4");
        writer.println("03:14:15.9 7 down 0");
        writer.println("22:00:59.9 3 uP 8");
        writer.println("00:56:42.7 8 UP 9");
        writer.println("03:34:19.2 6 down 1");
        
        writer.close();
        
        // Parse the created file
        FloorSubsystem testFloors = new FloorSubsystem(10, 1);
        
        testFloors.parseInputFile(filePath);
        
        ArrayList<Integer[]> reqs = testFloors.getRequests();
        
        // Print the requests
        for (Integer[] req : reqs) {
            System.out.println(Arrays.toString(req) + "\n");
        }
        
        // Delete the test text file
        try {
            Files.deleteIfExists(Paths.get(filePath));
        } catch (IOException e) {
            System.out.println("Error: Unable to delete test text file.");
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    @Test
    void testConfigMessage() {
        int numFloors = 11;
        int numElevators = 1;
        
        host.setExpectedNumMessages(1);
        Thread t = new Thread(host);
        t.start();
        
        FloorSubsystem testController = new FloorSubsystem(numFloors, numElevators);
        
        testController.sendConfigurationSignal(numElevators, numFloors);
        
        testController.teardown();
        testController = null;
    }

}
