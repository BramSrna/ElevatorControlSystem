import java.net.DatagramPacket;
import java.util.ArrayList;
import java.util.Arrays;

public class SchedulerAlgorithm {

	// TODO
	/**
	 * Note: I think destinations should be elevator independent between elevators,
	 * and elevatorRequests should be shared between elevators
	 */

	private ArrayList<Byte> elevatorDestinations; // Destinations
	private ArrayList<ArrayList<Byte>> elevatorRequests; // Source, Destination
	private byte currentFloor;
	private boolean stopElevator = true;

	/**
	 * Called when someone on the Floor has requested the elevator.
	 * 
	 * @param packet
	 */
	public void elevatorRequestMade(DatagramPacket packet) {
		System.out.println("Elevator was requested at: " + packet.getData()[1] + " in the direction "
				+ packet.getData()[2] + " with destination " + packet.getData()[3]);
		ArrayList<Byte> request = new ArrayList<Byte>();
		request.add(packet.getData()[1]);
		request.add(packet.getData()[3]);
		addElevatorRequest(request);

	}

	/**
	 * Called when the sensor informs the scheduler where the elevator is.
	 * 
	 * @param packet
	 */
	public void elevatorHasReachedFloor(DatagramPacket packet) {
		currentFloor = packet.getData()[1];
		System.out.println("Elevator has reached floor: " + currentFloor);
		if (elevatorDestinations.contains(currentFloor)) {
			System.out.println("Current floor is a destination.");
			stopElevator = true;

		}
		for (ArrayList<Byte> request : elevatorRequests) {
			if (request.get(0) == currentFloor) {
				System.out.println("Current floor is a request source.");
				addDestination(request.get(1));
				removeRequest(request);
				stopElevator = true;
			}
		}
		stopElevator = false;
		System.out.println();
		removeFloorFromDestinations(currentFloor);
	}

	public void floorButtonPressed(DatagramPacket packet) {
		elevatorDestinations.add(packet.getData()[1]);
	}

	/**
	 * Remove a floor from the destinations list.
	 * 
	 * @param currentFloor
	 */
	private void removeFloorFromDestinations(byte currentFloor) {
		if (elevatorDestinations.removeAll(Arrays.asList(currentFloor))) {
			System.out.println(currentFloor + " was removed from the destinations list");
			System.out.println("New destination list: " + elevatorDestinations.toString() + "\n");
		}
	}

	/**
	 * Add a elevator request to the list
	 * 
	 * @param request
	 */
	private void addElevatorRequest(ArrayList<Byte> request) {
		elevatorRequests.add(request);
		System.out.println("New request list: " + elevatorRequests.toString() + "\n");
	}

	/**
	 * Add a destination to the list
	 * 
	 * @param destination
	 */
	private void addDestination(byte destination) {
		// Send message to elevator so they can light up lamp
		elevatorDestinations.add(destination);
		System.out.println(destination + "was added to the destination list");
		System.out.println("New destination list: " + elevatorDestinations.toString() + "\n");
	}

	/**
	 * Remove the elevator request from the list
	 * 
	 * @param request
	 */
	private void removeRequest(ArrayList<Byte> request) {
		// TODO Is this correct?
		elevatorRequests.removeAll(Arrays.asList(request));
		System.out.println("New requests: " + elevatorRequests.toString());

	}

	/**
	 * Determine if the elevator should go up
	 * 
	 * @return True if the elevator should go up, false otherwise
	 */
	public boolean elevatorShouldGoUp() {
		int difference;
		int currentClosestDistance = Integer.MAX_VALUE;
		int closestFloor = 0;

		for (byte destination : elevatorDestinations) {
			difference = Math.abs(currentFloor - destination);
			if (difference < currentClosestDistance) {
				currentClosestDistance = difference;
				closestFloor = destination;
			}
		}
		for (ArrayList<Byte> request : elevatorRequests) {
			difference = Math.abs(currentFloor - request.get(0));
			if (difference < currentClosestDistance) {
				currentClosestDistance = difference;
				closestFloor = request.get(0);
			}
		}
		if (closestFloor > currentFloor) {
			return true;
		}
		return false;
	}

	/**
	 * Check to see if there are floors we need to go to above the current floor
	 * 
	 * @return True if we need to go up, false otherwise
	 */
	public boolean floorsToGoToAbove() {
		for (byte tempFloor : elevatorDestinations) {
			if (tempFloor > currentFloor) {
				return true;
			}
		}
		for (ArrayList<Byte> tempRequests : elevatorRequests) {
			if (tempRequests.get(0) > currentFloor) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Check to see if there are floors we need to go to below the current floor
	 * 
	 * @return True if we need to go down, false otherwise
	 */
	public boolean floorsToGoToBelow() {
		for (byte tempFloor : elevatorDestinations) {
			if (tempFloor < currentFloor) {
				return true;
			}
		}
		for (ArrayList<Byte> tempRequests : elevatorRequests) {
			if (tempRequests.get(0) < currentFloor) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Determines if the elevator has somewhere to go
	 * 
	 * @return True if has somewhere to go, False otherwise
	 */
	public boolean somewhereToGo() {
		if (elevatorDestinations.isEmpty() && elevatorRequests.isEmpty()) {
			return false;
		}
		return true;
	}

	public ArrayList<Byte> getDestinations() {
		return elevatorDestinations;
	}

	public ArrayList<ArrayList<Byte>> getRequests() {
		return elevatorRequests;
	}

	public byte getCurrentFloor() {
		return currentFloor;
	}

	public boolean getStopElevator() {
		return stopElevator;
	}
}
