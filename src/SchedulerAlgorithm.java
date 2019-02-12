import java.util.ArrayList;

public class SchedulerAlgorithm {

	private ArrayList<ArrayList<Byte>> elevatorDestinations; // Elevator, Destinations
	private ArrayList<Byte> currentFloor;
	private ArrayList<Boolean> stopElevator;

	public SchedulerAlgorithm(byte numElevators) {
		elevatorDestinations = new ArrayList<ArrayList<Byte>>();
		currentFloor = new ArrayList<Byte>();
		stopElevator = new ArrayList<Boolean>();
		
		setNumberOfElevators((byte) numElevators);
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
		
		addElevatorRequest(source, destination);

	}

	/**
	 * Called when the sensor informs the scheduler where the elevator is.
	 * 
	 * @param Byte currentFloor
	 */
	public void elevatorHasReachedFloor(Byte floorNum, Byte elevatorNum) {		
		System.out.println("Elevator " + elevatorNum + " has reached floor: " + floorNum);
		
		if (elevatorDestinations.get(elevatorNum).contains(floorNum)) {
			System.out.println("Current floor is a destination.");
			stopElevator.set(elevatorNum, true);
		} else {
		    stopElevator.set(elevatorNum, false);
		}
		
		currentFloor.set(elevatorNum, floorNum);
		
		System.out.println();
		
		removeFloorFromDestinations(floorNum, elevatorNum);
	}
	
   /**
     * Remove a floor from the destinations list.
     * 
     * @param currentFloor
     */
    private void removeFloorFromDestinations(byte currentFloor, byte currentElevator) {
        if (elevatorDestinations.get(currentElevator).contains(currentFloor)) {
            int indToRemove = elevatorDestinations.get(currentElevator).indexOf(currentFloor);
            elevatorDestinations.get(currentElevator).remove(indToRemove);
            System.out.println(currentFloor + " was removed from the destinations list");
            System.out.println("New destination list: " + elevatorDestinations.toString() + "\n");
        }
    }

	// TODO NOT USED YET
	public void floorButtonPressed(Byte pressedButton, byte elevatorNum) {
		elevatorDestinations.get(elevatorNum).add(pressedButton);
	}

	/**
	 * Add a elevator request to the list
	 * 
	 * @param request
	 */
	private boolean addElevatorRequest(byte startFloor, byte endFloor) {
	    for (ArrayList<Byte> destinations : elevatorDestinations) {
	        if (destinations.contains(startFloor)) {
	            if (destinations.contains(endFloor)) {
	                System.out.println("New request list: " + destinations.toString() + "\n");
	                return(true);
	            } else {
	                destinations.add(endFloor);
	                System.out.println("New request list: " + destinations.toString() + "\n");
	                return(true);
	            }
	            
	        }
	    }
	    
	    int closestElevator = 0;
	    int closestDiff = -1;
	    
	    for (int i = 0; i < currentFloor.size(); i++) {
	        if (closestDiff == -1) {
	            closestElevator = i;
	            closestDiff = Math.abs(currentFloor.get(i) - startFloor);
	        } else if (Math.abs(currentFloor.get(i) - startFloor) < closestDiff) {
	            closestElevator = i;
	            closestDiff = Math.abs(currentFloor.get(i) - startFloor);
	        }
	    }
	    
	    elevatorDestinations.get(closestElevator).add(startFloor);
	    elevatorDestinations.get(closestElevator).add(endFloor);
	    
	    System.out.println("New request list: " + elevatorDestinations.get(closestElevator) + "\n");
	    
        return(true);
	    
	}

	/**
	 * Determine if the elevator should go up
	 * 
	 * @return True if the elevator should go up, false otherwise
	 */
	public UtilityInformation.ElevatorDirection whatDirectionShouldTravel(byte elevatorNum) {
		int difference;
		int currentClosestDistance = Integer.MAX_VALUE;
		int closestFloor = 0;

		for (byte destination : elevatorDestinations.get(elevatorNum)) {
			difference = Math.abs(currentFloor.get(elevatorNum) - destination);
			if (difference < currentClosestDistance) {
				currentClosestDistance = difference;
				closestFloor = destination;
			}
		}
		
		if (closestFloor > currentFloor.get(elevatorNum)) {
		    return(UtilityInformation.ElevatorDirection.UP);
		} else if (closestFloor < currentFloor.get(elevatorNum)) {
		    return(UtilityInformation.ElevatorDirection.DOWN);
		} else {
		    return(UtilityInformation.ElevatorDirection.STATIONARY);
		}
	}

	/**
	 * Check to see if there are floors we need to go to above the current floor
	 * 
	 * @return True if we need to go up, false otherwise
	 */
	public boolean floorsToGoToAbove(byte elevatorNum) {
		for (byte tempFloor : elevatorDestinations.get(elevatorNum)) {
			if (tempFloor > currentFloor.get(elevatorNum)) {
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
	public boolean floorsToGoToBelow(byte elevatorNum) {
		for (byte tempFloor : elevatorDestinations.get(elevatorNum)) {
			if (tempFloor < currentFloor.get(elevatorNum)) {
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
	public boolean somewhereToGo(byte elevatorNum) {
		return(!elevatorDestinations.get(elevatorNum).isEmpty());
	}

	public void setNumberOfElevators(byte numElevators) {	        
	    while (elevatorDestinations.size() > numElevators) {
	        elevatorDestinations.remove(elevatorDestinations.size() - 1);
            currentFloor.remove(currentFloor.size() - 1);
            stopElevator.remove(stopElevator.size() - 1); 
	    }
	    
	    while (elevatorDestinations.size() < numElevators) {
	        elevatorDestinations.add(new ArrayList<Byte>());
            currentFloor.add((byte) 0);
            stopElevator.add(true); 
	    }
	}

	public ArrayList<ArrayList<Byte>> getDestinations() {
		return elevatorDestinations;
	}

	public ArrayList<Byte> getCurrentElevatorDestinations(byte elevatorNum) {
		return elevatorDestinations.get(elevatorNum);
	}

	public byte getCurrentFloor(byte elevatorNum) {
		return currentFloor.get(elevatorNum);
	}

	public boolean getStopElevator(byte elevatorNum) {
		return stopElevator.get(elevatorNum);
	}
}
