import java.util.ArrayList;
import java.util.Arrays;

public class SchedulerAlgorithm {

	private ArrayList<ArrayList<Byte>> elevatorDestinations; // Elevator, Destinations
	private ArrayList<ArrayList<Byte>> elevatorRequests; // Source, Destination
	private byte currentFloor;
	private byte currentElevator;
	private boolean stopElevator;

	public SchedulerAlgorithm() {
		elevatorRequests = new ArrayList<ArrayList<Byte>>();
		elevatorDestinations = new ArrayList<ArrayList<Byte>>();
		currentFloor = 0;
		stopElevator = true;
	}

	/**
	 * Called when someone on the Floor has requested the elevator.
	 * 
	 * @param source
	 * @param destination
	 * @param upOrDown
	 */
	public void elevatorRequestMade(Byte source, Byte destination, UtilityInformation.ElevatorDirection upOrDown) {
		System.out.println("Elevator was requested at: " + source + " in the direction " + upOrDown
				+ " with destination " + destination);
		ArrayList<Byte> request = new ArrayList<Byte>();
		request.add(source);
		request.add(destination);
		addElevatorRequest(request);

	}

	/**
	 * Called when the sensor informs the scheduler where the elevator is.
	 * 
	 * @param Byte currentFloor
	 */
	public void elevatorHasReachedFloor(Byte floor, Byte elevator) {
		currentFloor = floor;
		currentElevator = elevator;
		System.out.println("Elevator " + currentElevator + " has reached floor: " + currentFloor);
		if (elevatorDestinations.get(currentElevator).contains(currentFloor)) {
			System.out.println("Current floor is a destination.");
			stopElevator = true;

		}
		ArrayList<ArrayList<Byte>> reqToRemove = new ArrayList<ArrayList<Byte>>();
		for (ArrayList<Byte> request : elevatorRequests) {
			if (request.get(0) == currentFloor) {
				System.out.println("Current floor is a request source.");
				addDestination(request.get(1));
				reqToRemove.add(request);
				stopElevator = true;
			}
		}
		removeRequest(reqToRemove);
		stopElevator = false;
		System.out.println();
		removeFloorFromDestinations(currentFloor, currentElevator);
	}

	// TODO NOT USED YET
	public void floorButtonPressed(Byte pressedButton) {
		elevatorDestinations.get(currentElevator).add(pressedButton);
	}

	/**
	 * Remove a floor from the destinations list.
	 * 
	 * @param currentFloor
	 */
	private void removeFloorFromDestinations(byte currentFloor, byte currentElevator) {
		if (elevatorDestinations.get(currentElevator).removeAll(Arrays.asList(currentFloor))) {
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
		elevatorDestinations.get(currentElevator).add(destination);
		System.out.println(destination + "was added to the destination list");
		System.out.println("New destination list: " + elevatorDestinations.toString() + "\n");
	}

	/**
	 * Remove the elevator request from the list
	 * 
	 * @param request
	 */
	private void removeRequest(ArrayList<ArrayList<Byte>> requests) {
		elevatorRequests.removeAll(Arrays.asList(requests));
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

		for (byte destination : elevatorDestinations.get(currentElevator)) {
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
		for (byte tempFloor : elevatorDestinations.get(currentElevator)) {
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
		for (byte tempFloor : elevatorDestinations.get(currentElevator)) {
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

	public void setNumberOfElevators(byte numOfElevators) {
		for (byte a = 0; a < numOfElevators; a++) {
			elevatorDestinations.add(new ArrayList<Byte>());
		}
	}

	public ArrayList<ArrayList<Byte>> getDestinations() {
		return elevatorDestinations;
	}

	public ArrayList<Byte> getCurrentElevatorDestinations() {
		return elevatorDestinations.get(currentElevator);
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
