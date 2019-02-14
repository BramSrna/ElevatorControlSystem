import java.util.ArrayList;

public class SchedulerAlgorithm {

	private ArrayList<ArrayList<Byte>> elevatorStops; // Elevator, Destinations
	private ArrayList<Byte> currentFloor;
	private ArrayList<Boolean> stopElevator;
	private ArrayList<ArrayList<Byte>> elevatorDestinations;

	public SchedulerAlgorithm(byte numElevators) {
		elevatorStops = new ArrayList<ArrayList<Byte>>();
		currentFloor = new ArrayList<Byte>();
		stopElevator = new ArrayList<Boolean>();
		elevatorDestinations = new ArrayList<ArrayList<Byte>>();
		
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
		
		if (elevatorStops.get(elevatorNum).contains(floorNum)) {
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
        if (elevatorStops.get(currentElevator).contains(currentFloor)) {
            int indToRemove = elevatorStops.get(currentElevator).indexOf(currentFloor);
            if (indToRemove != -1) {
            	elevatorStops.get(currentElevator).remove(indToRemove);
            }            
            
            indToRemove = elevatorDestinations.get(currentElevator).indexOf(currentFloor);
            
            if (indToRemove != -1)
            {
            	elevatorDestinations.get(currentElevator).remove(indToRemove);
            }
            
            System.out.println(currentFloor + " was removed from the destinations list");
            System.out.println("New destination list: " + elevatorStops.toString() + "\n");
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
	private void addElevatorRequest(byte startFloor, byte endFloor) {	    
	    ArrayList<Integer> closestElevator = new ArrayList<Integer>();
	    int closestDiff = -1;
	    int chosenElevator = -1;
	    int currDiff;
	    
	    // Add destination to closest elevator
	    for (int i = 0; i < currentFloor.size(); i++) {
	        currDiff = Math.abs(currentFloor.get(i) - startFloor);
	        if ((closestDiff == -1) || 
	            (currDiff <= closestDiff)) { // same as below
	            closestElevator.add(i);
	            closestDiff = currDiff;
	        }
	    }
	    
	    // Break ties by prioritizing elevators above the start floor
	    if (closestElevator.size() > 1) {
	        ArrayList<Integer> indicesToRemove = new ArrayList<Integer>();
	        
	        for (int i = 0; i < closestElevator.size(); i++) {
	            if (currentFloor.get(closestElevator.get(i)) < startFloor) {
	                indicesToRemove.add(i);
	            }
	        }
	        
	        if (indicesToRemove.size() < closestElevator.size()) {
	            for (int index : indicesToRemove) {
	                closestElevator.remove(index);
	            }
	        }
	        
	        // Break ties by prioritizing shortest queues
	        int shortestQueue = -1;
	        if (closestElevator.size() > 1) {
	            for (int i = 0; i < closestElevator.size(); i++) {
	                if ((shortestQueue == -1) || 
	                    (elevatorStops.get(closestElevator.get(i)).size() < shortestQueue)) {
	                    chosenElevator = closestElevator.get(i);
	                }
	            }
	        } else {
	            chosenElevator = closestElevator.get(0);
	        }
	    } else {
	        chosenElevator = closestElevator.get(0);
	    }
	    
	    int currFloor = currentFloor.get(chosenElevator);
	    int startInd = -1;
	    int closestInd = -1;
	    closestDiff = Integer.MAX_VALUE;
	    ArrayList<Byte> currDests = elevatorStops.get(chosenElevator);
	    
	    if (!elevatorDestinations.get(chosenElevator).contains(endFloor)) {
	    	elevatorDestinations.get(chosenElevator).add(endFloor);
	    }
	    
	    if (currDests.size() == 0) {
	        currDests.add(startFloor);
	        currDests.add(endFloor);
	    } else {
    	    if (!(currDests.contains(startFloor))) {
    	        int maxInd = currDests.size();
    	        
    	        if (currDests.contains(endFloor)) {
    	            maxInd = currDests.indexOf(endFloor);
    	        }
    	        
    	        if ((currFloor < startFloor) && (startFloor < currDests.get(0))) {
    	            startInd = 0;
    	        } else {    	        
        	        for(int i = 0; i < maxInd; i++) {
        	            if (i != maxInd - 1) {
            	            if (((currDests.get(i) < startFloor) && (currDests.get(i + 1) > startFloor)) ||
            	                ((currDests.get(i) > startFloor) && (currDests.get(i + 1) < startFloor))) {
            	                startInd = i + 1;
            	            }
        	            }
        	            
        	            currDiff = Math.abs(currDests.get(i) - startFloor);
        	            if (currDiff < closestDiff) {
        	                closestDiff = currDiff;
        	                closestInd = i + 1;
        	            }
        	            
        	        }
        	        
        	        if (startInd == -1) {
        	            startInd = closestInd;
        	        }
        	    }
    	        
    	        currDests.add(startInd, startFloor);
    	    } else {
    	        startInd = currDests.indexOf(startFloor);
    	    }
    	    
    	    closestInd = -1;
            closestDiff = Integer.MAX_VALUE;
            int endInd = -1;
            
            if (!(currDests.contains(endFloor))) {
                for(int i = startInd; i < currDests.size(); i++) {
                    if (i != currDests.size() - 1) {
                        if (((currDests.get(i) < endFloor) && (currDests.get(i + 1) > endFloor)) ||
                            ((currDests.get(i) > endFloor) && (currDests.get(i + 1) < endFloor))) {
                            endInd = i + 1;
                        }
                    }
                    
                    currDiff = Math.abs(currDests.get(i) - endFloor);
                    if (currDiff < closestDiff) {
                        closestDiff = currDiff;
                        closestInd = i;
                    }
                    
                }
                
                if (endInd == -1) {
                    endInd = closestInd;
                }
                
                if (endInd != 0) {
                    endInd += 1;
                }
                
                currDests.add(endInd, endFloor);
            }
	    }
	    
	    System.out.println("New request list: " + elevatorStops.get(chosenElevator) + "\n");	    
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

		for (byte destination : elevatorStops.get(elevatorNum)) {
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
		for (byte tempFloor : elevatorStops.get(elevatorNum)) {
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
		for (byte tempFloor : elevatorStops.get(elevatorNum)) {
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
		return(!elevatorStops.get(elevatorNum).isEmpty());
	}

	public void setNumberOfElevators(byte numElevators) {	        
	    while (elevatorStops.size() > numElevators) {
	        elevatorStops.remove(elevatorStops.size() - 1);
            currentFloor.remove(currentFloor.size() - 1);
            stopElevator.remove(stopElevator.size() - 1);
            elevatorDestinations.remove(elevatorDestinations.size() - 1);
	    }
	    
	    while (elevatorStops.size() < numElevators) {
	        elevatorStops.add(new ArrayList<Byte>());
            currentFloor.add((byte) 0);
            stopElevator.add(true); 
            elevatorDestinations.add(new ArrayList<Byte>());
	    }
	}

	public ArrayList<Byte> getDestinations(byte elevatorNum) {
		return elevatorDestinations.get(elevatorNum);
	}

	public ArrayList<Byte> getCurrentelevatorStops(byte elevatorNum) {
		return elevatorStops.get(elevatorNum);
	}

	public byte getCurrentFloor(byte elevatorNum) {
		return currentFloor.get(elevatorNum);
	}

	public boolean getStopElevator(byte elevatorNum) {
		return stopElevator.get(elevatorNum);
	}
}
