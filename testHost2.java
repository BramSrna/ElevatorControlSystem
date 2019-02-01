package groupProject;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;

public class testHost2 {
	private DatagramSocket receiveSocket, sendSocket; // sockets to send and receive
	private DatagramPacket sendPacket,receivePacket; // packets to send and receive
	
	/*
	 * This is the general constructor for the class
	 */
	public testHost2() {
		try {
			receiveSocket = new DatagramSocket(112); // Datagram-Socket used to receive data from the Client on Port 23.
			sendSocket = new DatagramSocket(); // Datagram-Socket used to send data to the Server (on Port 69, initialized when the Packet is formed).
		}catch(SocketException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	/*
	 * The method below is used to exchange data (send and receive) between an Intermediate Host, a Client, and a Server.
	 */
	public void exchangeData() {
		try {
			for(;;) {
				
				// The Host waits to receive data from Client then it print that data (string and array of bytes).
				byte data[] = new byte[7]; // sufficient size array of bytes to receive data.
                receivePacket = new DatagramPacket(data, data.length); // new packet to receive data.
                System.out.println("Host: Waiting for Packet from Client.\n");
                receiveSocket.receive(receivePacket);
                int returnPort = receivePacket.getPort();
                System.out.println("Host: Packet received");
                System.out.println("From client: " + receivePacket.getAddress());
                System.out.println("Client port: " + returnPort);
                int len = receivePacket.getLength();
                System.out.println("Length: " + len);
                System.out.println("Containing: ");
                
        	    System.out.println(new String(data,2,len-2)); // turn data received into a string but don't print the 01/02...
        	    System.out.println(Arrays.toString(data));
        	    System.out.println("\n");

                // The Host forms a packet containing the response received from the Server and sends it to the Client.
                sendPacket = new DatagramPacket(data, receivePacket.getLength(), receivePacket.getAddress(), returnPort);
                System.out.println("Host: Sending packet");
                System.out.println("To client: " + sendPacket.getAddress());
                System.out.println("Destination client port: " + sendPacket.getPort());
                len = sendPacket.getLength();
                System.out.println("Length: " + len);
                System.out.println("Containing: ");
                
                //System.out.println(new String(sendPacket.getData(),0,len)); //print info in packet, String
                System.out.println(Arrays.toString(sendPacket.getData()));
                sendSocket.send(sendPacket);
                System.out.println("Host: Packet sent to Client.\n");
            }
		} catch (IOException e){
                e.printStackTrace();
		}
	}
	
	/*
	 * The main method used to run the Intermediate Host. The Host receives data from a Client, then forwards it to a Server, it waits
	 * on a response from the Server and sends that response back to the Client.
	 */
	public static void main(String args[]) {
		testHost2 test = new testHost2();
		test.exchangeData();
	}
}
