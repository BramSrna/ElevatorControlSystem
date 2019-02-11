import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;

public abstract class ServerPattern {
    private ArrayList<byte[]> receivedSignals;
    
    private SignalReceiver receiver;
    
    private boolean receivedSignalsEmpty;
    
    private final int MAX_NUM_SIGNALS = 100;
    
    private String name;
    
    public ServerPattern(int portNum, String name) {
        this.name = name;
        
        receivedSignals = new ArrayList<byte[]>();
        receivedSignalsEmpty = true;
        
        receiver = new SignalReceiver(portNum, this, name);
        
        Thread receiverThread = new Thread(receiver, "receiver");
        receiverThread.start();
    }
    
    public abstract void handleRequest();
    
    public synchronized void signalReceived(byte[] newSignal, int priority) {
        while (receivedSignals.size() >= MAX_NUM_SIGNALS) {
            try {
                wait();
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        
        receivedSignals.add(newSignal);
        
        receivedSignalsEmpty = false;
        notifyAll();
    }
    
    public synchronized byte[] getNextRequest() {
        while (receivedSignalsEmpty) {
            try {
                wait();
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        
        byte[] toReturn = receivedSignals.get(0);
        receivedSignals.remove(0);
        
        if (receivedSignals.size() == 0) {
            receivedSignalsEmpty = true;
        }
        notifyAll();
        
        return(toReturn);
    }
    

}

class SignalReceiver implements Runnable {
    private DatagramPacket receivePacket;
    private DatagramSocket receiveSocket;
    
    private ServerPattern controller;
    
    private String name;
    
    public SignalReceiver(int portNum, ServerPattern controller, String name) {
        this.controller = controller;
        this.name = name;
        
        // Initialize the DatagramSocket
        try {
            receiveSocket = new DatagramSocket(portNum);
        } catch (SocketException se) {
            se.printStackTrace();
            this.teardown();
            System.exit(1);
        }
    }
    
    public void teardown() {
        receiveSocket.close();
    }
    
    /**
     * waitForSignal
     * 
     * Waits for a packet of the given size to be
     * sent by the Scheduler. When the packet is received,
     * information about the packet is printed. The message (byte[])
     * in the packet is then returned.
     * 
     * @param expectedMsgSize  The expected size of the message to receive
     * 
     * @return The byte[] send in the DatagramPacket
     */
    public byte[] waitForSignal(int expectedMsgSize) {
        // Create the receive packet
        byte data[] = new byte[expectedMsgSize];
        receivePacket = new DatagramPacket(data, data.length);

        System.out.println("FloorSubsystem: Waiting for response from host...");

        try {
            // Block until a datagram is received via sendSocket.
            receiveSocket.receive(receivePacket);
        } catch (IOException e) {
            e.printStackTrace();
            this.teardown();
            System.exit(1);
        }

        // Print out information about the response
        System.out.println(String.format("%s: Packet received:", name));
        System.out.println("From host: " + receivePacket.getAddress());
        System.out.println("Host port: " + receivePacket.getPort());
        int len = receivePacket.getLength();
        System.out.println("Length: " + len);
        System.out.print("Containing (as bytes): ");
        System.out.println(Arrays.toString(data) + "\n");

        return (receivePacket.getData());
    }

    @Override
    public void run() {
        while (true) {
            byte[] signal = waitForSignal(5);
            controller.signalReceived(signal, 0);            
        }        
    }
}