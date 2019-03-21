import java.util.ArrayList;
import java.util.LinkedHashSet;

public class SchedulerAlgorithm {	
	private ArrayList<AlgorithmElevator> elevatorInfo;

	/**
	 * SchedulerAlgorithm
	 * 
	 * Constructor
	 * 
	 * Creates a new scheduler algorithm class with the given number of elevators.
	 * 
	 * @param numElevators Number of elevators that the algorithm should control
	 */
	public SchedulerAlgorithm(byte numElevators) {
	    elevatorInfo = new ArrayList<AlgorithmElevator>();
	    
		setNumberOfElevators(numElevators);
	}

	/**
	 * Called when someone on the floor has requested an elevator
	 * 
	 * @param request
	 * @return
	 */
	public byte elevatorRequestMade(Request request) {
		Byte source = request.getSourceFloor();
		Byte destination = request.getDestinationFloor();
		UtilityInformation.ElevatorDirection upOrDown = request.getRequestDirection();
		System.out.println("Elevator was requested at: " + source + " in the direction " + upOrDown
				+ " with destination " + destination);

		byte elevatorNum = determineElevatorToGiveRequest(request);
		addRequestToElevator(elevatorNum, request);

		return (elevatorNum);
	}

	/**
	 * determineElevatorToGiveRequest
	 * 
	 * Determines which elevator in the system should be given the request with the
	 * given start floor.
	 * 
	 * @param startFloor Floor number where request was made
	 * 
	 * @return byte containg the elevator number that was given teh request
	 */
	private byte determineElevatorToGiveRequest(Request request) {
		int chosenElevator = -1;
		int minQueueSize = -1;
		int currQueueSize;

		// Add destination to closest elevator
		for (int i = 0; i < elevatorInfo.size(); i++) {
			currQueueSize = elevatorInfo.get(i).getRequests().size();
			if (elevatorInfo.get(i).isUsable() && 
			        ((minQueueSize == -1) || (currQueueSize <= minQueueSize))) { // same as below
			    minQueueSize = currQueueSize;
				chosenElevator = i;
			}
		}

		return ((byte) chosenElevator);
	}

	/**
	 * addStopToElevator
	 * 
	 * Adds the given floor to the list of elevator stops for the given elevator
	 * number. A minimum index is given that controls how early the stop can be
	 * placed in the list.
	 * 
	 * @param elevatorNum Elevator number that will get the new request
	 * @param destFloor   Destination floor that should be added to the list of
	 *                    stops
	 * @param minInd      Minimum index of the new stop in the list
	 * 
	 * @return Byte The index in the list where the request was placed
	 */
	private void addRequestToElevator(byte elevatorNum, Request request) {
		elevatorInfo.get(elevatorNum).addRequest(request);
	}

	/**
	 * Scheduler has been informed where the elevator is. Update the stopElevator
	 * and currentFloor ArrayList. Remove the current floor from destinations.
	 * 
	 * @param floorNum
	 * @param elevatorNum
	 */
	public void elevatorHasReachedFloor(Byte floorNum, Byte elevatorNum) {
		System.out.println("Elevator " + elevatorNum + " has reached floor: " + floorNum);
		
		boolean stopElevator = false;
		
		for (Request req : elevatorInfo.get(elevatorNum).getRequests()) {
		    if ((req.getElevatorPickupTimeFlag() == false) && 
		        (req.getSourceFloor() == floorNum)) {
		        req.setElevatorPickupTimeFlag();
		        elevatorInfo.get(elevatorNum).setDir(UtilityInformation.ElevatorDirection.STATIONARY);
		        stopElevator = true;
		    } else if ((req.getElevatorPickupTimeFlag() == true) && 
    		           (req.getElevatorArrivedDestinationTimeFlag() == false) && 
    		           (req.getDestinationFloor() == floorNum)) {
		        req.setElevatorArrivedDestinationTimeFlag();
		        elevatorInfo.get(elevatorNum).setDir(UtilityInformation.ElevatorDirection.STATIONARY);
		        stopElevator = true;
		    }
		}

		elevatorInfo.get(elevatorNum).setStopElevator(stopElevator);
		elevatorInfo.get(elevatorNum).setCurrFloor(floorNum);
	}

	/**
	 * Determine the direction the given elevator should travel
	 * 
	 * @param elevatorNum
	 * @return
	 */
	public UtilityInformation.ElevatorDirection whatDirectionShouldTravel(byte elevatorNum) {
		if ((elevatorInfo.get(elevatorNum).getRequests().size() != 0) && (elevatorInfo.get(elevatorNum).isUsable())) {
			int currFloor = elevatorInfo.get(elevatorNum).getCurrFloor();
			int nextFloor = determineNextFloor(elevatorNum);
			
			if (nextFloor < currFloor) {
			    elevatorInfo.get(elevatorNum).setDir(UtilityInformation.ElevatorDirection.DOWN);
			} else if (nextFloor == currFloor) {
			    elevatorInfo.get(elevatorNum).setDir(UtilityInformation.ElevatorDirection.STATIONARY);
			} else if (nextFloor > currFloor) {
			    elevatorInfo.get(elevatorNum).setDir(UtilityInformation.ElevatorDirection.UP);
			}
		} else {
		    elevatorInfo.get(elevatorNum).setDir(UtilityInformation.ElevatorDirection.STATIONARY);
		}
		
		return(elevatorInfo.get(elevatorNum).getDir());
	}
	
	private int determineNextFloor(byte elevatorNum) {
        int nextFloor = -1;
        
	    if ((elevatorInfo.get(elevatorNum).getRequests().size() != 0) && (elevatorInfo.get(elevatorNum).isUsable())) {
            UtilityInformation.ElevatorDirection currDir = elevatorInfo.get(elevatorNum).getDir();            
            
            if (currDir == UtilityInformation.ElevatorDirection.UP) {
                nextFloor = getNextClosestFloorInDirection(elevatorNum, UtilityInformation.ElevatorDirection.UP);
                
                if (nextFloor == -1) {
                    nextFloor = getNextClosestFloorInDirection(elevatorNum, UtilityInformation.ElevatorDirection.DOWN);
                }
            } else if (currDir == UtilityInformation.ElevatorDirection.DOWN) {
                nextFloor = getNextClosestFloorInDirection(elevatorNum, UtilityInformation.ElevatorDirection.DOWN);
                
                if (nextFloor == -1) {
                    nextFloor = getNextClosestFloorInDirection(elevatorNum, UtilityInformation.ElevatorDirection.UP);
                }
            } else {                
                nextFloor = getNextClosestFloorInDirection(elevatorNum, UtilityInformation.ElevatorDirection.STATIONARY);
            }
        }
	    
	    if (nextFloor == -1) {
	        nextFloor = elevatorInfo.get(elevatorNum).getCurrFloor();
	    }
	    
	    return(nextFloor);
	}
	
	private int getNextClosestFloorInDirection(byte elevatorNum, UtilityInformation.ElevatorDirection dir) {
	    int currFloor = -1;
        int nextFloor = currFloor;
        int currDiff;
        int closestDiff = -1;        
        int currFloorToCompare;
        
        for (Request req : elevatorInfo.get(elevatorNum).getRequests()) {
            currFloorToCompare = -1;
            
            if (req.getElevatorPickupTimeFlag() == false) {
                currFloorToCompare = req.getSourceFloor();
            } else if ((req.getElevatorPickupTimeFlag() == true) && (req.getElevatorArrivedDestinationTimeFlag() == false)) {
                currFloorToCompare = req.getDestinationFloor();
            }
            
            if (((dir.equals(UtilityInformation.ElevatorDirection.UP)) && (currFloorToCompare > currFloor)) || 
                ((dir.equals(UtilityInformation.ElevatorDirection.DOWN)) && (currFloorToCompare < currFloor)) || 
                (dir.equals(UtilityInformation.ElevatorDirection.STATIONARY))) {
                currDiff = Math.abs(currFloor - currFloorToCompare);
                if ((closestDiff == -1) || (currDiff < closestDiff)) {
                    closestDiff = currDiff;
                    nextFloor = currFloorToCompare;
                }
                
            }
        }
        
        return(nextFloor);
	}

	/**
	 * Determine if a given elevator has anywhere to go
	 * 
	 * @param elevatorNum
	 * @return
	 */
	public boolean somewhereToGo(byte elevatorNum) {
		for (Request req : elevatorInfo.get(elevatorNum).getRequests()) {
		    if ((req.getElevatorPickupTimeFlag() == false) ||
		        (req.getElevatorArrivedDestinationTimeFlag() == false)) {
		        return(true);
		    }
		}
		
		return(false);
	}

	/**
	 * Set the number of elevators from the given schematics. Updates all the
	 * ArrayLists that need to be given the correct number of elevators to be
	 * initialized
	 * 
	 * @param numElevators
	 */
	public void setNumberOfElevators(byte numElevators) {
		while (elevatorInfo.size() > numElevators) {
			elevatorInfo.remove(elevatorInfo.size() - 1);
		}

		while (elevatorInfo.size() < numElevators) {
			elevatorInfo.add(new AlgorithmElevator((byte) elevatorInfo.size()));
		}
	}

	/**
	 * Get an elevators destinations
	 * 
	 * @param elevatorNum
	 * @return
	 */
	public LinkedHashSet<Byte> getDestinations(byte elevatorNum) {
	    LinkedHashSet<Byte> dests = new LinkedHashSet<Byte>();
	    
	    for (Request req : elevatorInfo.get(elevatorNum).getRequests()) {
	        dests.add(req.getDestinationFloor());
	    }
	    
		return(dests);
	}

	/**
	 * Get the current floor of an elevator
	 * 
	 * @param elevatorNum
	 * @return
	 */
	public byte getCurrentFloor(byte elevatorNum) {
		return elevatorInfo.get(elevatorNum).getCurrFloor();
	}

	/**
	 * Determine if the current elevator should stop
	 * 
	 * @param elevatorNum
	 * @return
	 */
	public boolean getStopElevator(byte elevatorNum) {
		return elevatorInfo.get(elevatorNum).getStopElevator();
	}

	/**
	 * Set the current elevator to stop
	 * 
	 * @param elevatorNum
	 * @param newVal
	 */
	public void setStopElevator(byte elevatorNum, boolean newVal) {
		elevatorInfo.get(elevatorNum).setStopElevator(newVal);
	}

	/**
	 * Print all the information in the lists (for testing purposes)
	 */
	public void printAllInfo() {
		for (AlgorithmElevator elevator : elevatorInfo) {
		    elevator.toString();
		}
	}

	/**
	 * stopUsingElevator
	 * 
	 * Tells the algorithm to stop using the given elevator and to remove all stops
	 * from that elevator and add them to a different elevator.
	 * 
	 * @param elevatorNum The number of the elevator to stop using
	 * 
	 * @return None
	 */
	public void stopUsingElevator(byte elevatorNum) {

		// Move all stops from the broken elevator to the elevator with the shortest
		// queuse
		ArrayList<Request> currReqs = elevatorInfo.get(elevatorNum).getRequests();
		byte currFloor = elevatorInfo.get(elevatorNum).getCurrFloor();
		Request tempReq;
		byte startFloor;
		byte destFloor;
		UtilityInformation.ElevatorDirection dir;
		
		for (Request req : currReqs) {
		    if (req.getElevatorPickupTimeFlag()) {
		        startFloor = currFloor;
		        destFloor = req.getDestinationFloor();
		        
		        if (startFloor < destFloor) {
		            dir = UtilityInformation.ElevatorDirection.UP;
		        } else {
		            dir = UtilityInformation.ElevatorDirection.DOWN;
		        }
		        
		        tempReq = new Request(System.nanoTime(), startFloor, destFloor, dir);
		    } else {
		        tempReq = req;
		    }
		    
		    elevatorRequestMade(tempReq);
		}

		elevatorInfo.get(elevatorNum).clearRequests();
		pauseElevator(elevatorNum);
	}

	/**
	 * pauseElevator
	 * 
	 * Temporarily stop using the given elevator. Requests are NOT removed from the
	 * elevator, it is just paused.
	 * 
	 * @param elevatorNum Number of elevator to stop using
	 * 
	 * @return None
	 */
	public void pauseElevator(byte elevatorNum) {
		elevatorInfo.get(elevatorNum).setUsable(false);

		elevatorInfo.get(elevatorNum).setStopElevator(true);
	}

	/**
	 * resumeUsingElevator
	 * 
	 * Unpause the given elevator
	 * 
	 * @param elevatorNum Number of elevator to unpause
	 * 
	 * @return None
	 */
	public void resumeUsingElevator(byte elevatorNum) {
		elevatorInfo.get(elevatorNum).setUsable(true);
	}
	
    public ArrayList<Request> getRequests(byte elevatorNum) {
        return(elevatorInfo.get(elevatorNum).getRequests());
    }
	    
	public class AlgorithmElevator {
	    public byte elevatorNum;
        public byte currFloor;
        
        public ArrayList<Request> elevatorRequests;
        
	    public boolean stopElevator;
	    public boolean elevatorUsable;
	    
	    public UtilityInformation.ElevatorDirection dir;
	    
	    public AlgorithmElevator(byte elevatorNum) {
	        this.elevatorNum = elevatorNum;
	        currFloor = 0;
	        
            elevatorRequests = new ArrayList<Request>();
	        
	        stopElevator = true;
	        elevatorUsable = true;
	        
	        dir = UtilityInformation.ElevatorDirection.STATIONARY;

	    }
	    
	    public void setDir(UtilityInformation.ElevatorDirection newDir) {
            dir = newDir;
            
        }

        public UtilityInformation.ElevatorDirection getDir() {
            return(dir);
        }

        public void addRequest(Request request) {
            elevatorRequests.add(request);            
        }

        public ArrayList<Request> getRequests() {
            return(elevatorRequests);
        }

        public void clearRequests() {
	        elevatorRequests.clear();
	    }

        public boolean getStopElevator() {
            return(stopElevator);
        }

        public void setUsable(boolean newVal) {
            elevatorUsable = newVal;            
        }

        public void setCurrFloor(Byte floorNum) {
            currFloor = floorNum;            
        }

        public void setStopElevator(boolean newVal) {
            stopElevator = newVal;            
        }

        public boolean isUsable() {
            return(elevatorUsable);
        }

        public byte getCurrFloor() {
            return(currFloor);
        }
        
        public String toString() {
            String toReturn = "";
            
            toReturn += String.format("Elevator number: %d", elevatorNum);
            toReturn += String.format(" Current Floor: %d", currFloor);
            toReturn += String.format(" Elevator Usable: %d", elevatorUsable);
            toReturn += String.format(" Elevator Stopped: %d", stopElevator);
            
            return(toReturn);
            
        }
	}


}


