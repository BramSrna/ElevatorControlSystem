import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Random;

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
	    byte chosenElevator = -1;
	    int shortestTime = -1;
	    int time;

		// Add destination to closest elevator
		for (byte i = 0; i < elevatorInfo.size(); i++) {
			time = howLongUntilRequestWouldBeServed(i, request);
			
			if (((shortestTime == -1) || (time < shortestTime)) && elevatorInfo.get(i).isUsable()) {
				chosenElevator = i;
				shortestTime = time;
			}
		}

		return (chosenElevator);
	}
	
	/**
	 * howLongUntilRequestWouldBeServed
	 * 
	 * Determines how long it would take for the current elevator to start
	 * the given request (i.e. to pick the person up)
	 * 
	 * @param elevatorNum  The number of the elevator
	 * @param req  The Request to service
	 * 
	 * @return int Time in milliseconds it would take the current elevator to service the request
	 */
	private int howLongUntilRequestWouldBeServed(byte elevatorNum, Request req) {
		int currFloor = elevatorInfo.get(elevatorNum).getCurrFloor();
		UtilityInformation.ElevatorDirection dir = elevatorInfo.get(elevatorNum).getDir();
		int nextFloor = determineNextFloor(elevatorNum);
		
		int sourceFloor = req.getSourceFloor();
		
		int diff = 0;
		
		if (dir.equals(UtilityInformation.ElevatorDirection.UP)) {
			if (sourceFloor > currFloor) {
				diff = sourceFloor - currFloor;
			} else if (sourceFloor < currFloor) {
				diff = (nextFloor - currFloor) + (nextFloor - sourceFloor);
			}
		} else if (dir.equals(UtilityInformation.ElevatorDirection.DOWN)) {
			if (sourceFloor < currFloor) {
				diff = currFloor - sourceFloor;
			} else if (sourceFloor > currFloor) {
				diff = (currFloor - nextFloor) + (sourceFloor - nextFloor);
			}
		} else {
			if (sourceFloor > currFloor) {
				diff = sourceFloor - currFloor;
			} else if (sourceFloor < currFloor) {
				diff = currFloor - sourceFloor;
			}
		}
		
		return(diff * UtilityInformation.TIME_UP_ONE_FLOOR);
	}
	
	/**
	 * addRequestToElevator
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
	
	/**
	 * determineNextFloor
	 * 
	 * Determine the next floor that the given elevator should head to.
	 * 
	 * @param elevatorNum  The number of the elevator to check
	 * 
	 * @return int The next floor number
	 */
	private int determineNextFloor(byte elevatorNum) {
        int nextFloor = -1;
        
	    if (!elevatorInfo.get(elevatorNum).getStopElevator() && 
	       (elevatorInfo.get(elevatorNum).getRequests().size() != 0) && 
	       (elevatorInfo.get(elevatorNum).isUsable())) {
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
                nextFloor = getNextClosestFloorInDirection(elevatorNum, elevatorInfo.get(elevatorNum).getPreviousDir());
                
                if (nextFloor == -1) {
                	nextFloor = getNextClosestFloorInDirection(elevatorNum, UtilityInformation.ElevatorDirection.STATIONARY);
                }
            }
        }
	    
	    if (nextFloor == -1) {
	        nextFloor = elevatorInfo.get(elevatorNum).getCurrFloor();
	    }
	    
	    return(nextFloor);
	}
	
	/**
	 * getNextClosestFloorInDirection
	 * 
	 * Get the next closest floor to the given elevator's current floor
	 * in the given direction. Returns the current floor if there are no
	 * other floors in that direction to visit.
	 * 
	 * @param elevatorNum  The number of the elevator to check
	 * @param dir  The direction to check relative to the current floor
	 * 
	 * @return int The closest floor in the given direction.
	 *             Returns the current floor if nothing else available
	 */
	private int getNextClosestFloorInDirection(byte elevatorNum, UtilityInformation.ElevatorDirection dir) {
	    int currFloor = elevatorInfo.get(elevatorNum).getCurrFloor();
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
            
            if (currFloorToCompare != -1) {
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
        }
        
        if (nextFloor == currFloor) {
            nextFloor = -1;
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
		
		pauseElevator(elevatorNum);
		
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
	
	/**
	 * getRequests
	 * 
	 * Returns the list of requests that the given elevator has
	 * 
	 * @param elevatorNum  The number of the elevator to check
	 * 
	 * @return ArrayList<Request>  Containing all of the elevator's past and current requests
	 */
    public ArrayList<Request> getRequests(byte elevatorNum) {
        return(elevatorInfo.get(elevatorNum).getRequests());
    }
    
    public boolean getStopSignalSent(byte elevatorNum) {
        return(elevatorInfo.get(elevatorNum).getStopSignalSent());
    }
    
    public void setStopSignalSent(byte elevatorNum, boolean newVal) {
        elevatorInfo.get(elevatorNum).setStopSignalSent(newVal);
    }
	    
    /**
     * AlgorithmElevator
     * 
     * Class used as a data structure to track the information
     * about each elevator.
     *
     */
	public class AlgorithmElevator {
	    // Info about elevator
	    public byte elevatorNum;
        public byte currFloor;
        
        // Requests given to the eelvator
        public ArrayList<Request> elevatorRequests;
        
        // Condition of elevator
	    public boolean stopElevator;
	    public boolean elevatorUsable;
	    
	    // Current direction of the elevator
	    public UtilityInformation.ElevatorDirection dir;
	    public UtilityInformation.ElevatorDirection previousDir;
	    
	    public boolean stopSignalSent;
	    
	    /**
	     * AlgorithmElevator
	     * 
	     * Creates a new AlgorithmElevator object
	     * 
	     * @param elevatorNum  The number of teh created elevator
	     * 
	     * @return None
	     */
	    public AlgorithmElevator(byte elevatorNum) {
	        this.elevatorNum = elevatorNum;
	        currFloor = 0;
	        
            elevatorRequests = new ArrayList<Request>();
	        
	        stopElevator = true;
	        elevatorUsable = true;
	        
	        dir = UtilityInformation.ElevatorDirection.STATIONARY;
	        previousDir = dir;
	        
	        stopSignalSent = true;

	    }
	    
	    public void setStopSignalSent(boolean newVal) {
            stopSignalSent = newVal;            
        }

        public boolean getStopSignalSent() {
            return(stopSignalSent);
        }

        public UtilityInformation.ElevatorDirection getPreviousDir() {
			return(previousDir);
		}

		/**
	     * setDir
	     * 
	     * Sets the current direction of the elevator to the given value
	     * 
	     * @param newDir   The new direction for the elevator
	     * 
	     * @return void
	     */
	    public void setDir(UtilityInformation.ElevatorDirection newDir) {
	    	previousDir = dir;
            dir = newDir;            
        }

	    /**
	     * getDir
	     * 
	     * Returns the current direction of the elevator
	     * 
	     * @param  None
	     * 
	     * @return ElevatorDirection   The current direction of the eelvator
	     */
        public UtilityInformation.ElevatorDirection getDir() {
            return(dir);
        }

        /**
         * addRequest
         * 
         * Adds the given Request to the list of Requests
         * 
         * @param request   The Request to add to the list
         * 
         * @return  void
         */
        public void addRequest(Request request) {
            elevatorRequests.add(request);            
        }

        /**
         * getRequests
         * 
         * Returns the current list of requests
         * 
         * @param   None
         * 
         * @return  ArrayList<Request> List of past and current requests
         */
        public ArrayList<Request> getRequests() {
            return(elevatorRequests);
        }

        /**
         * clearRequests
         * 
         * Empties the list of requests
         * 
         * @param   None
         * 
         * @return  void
         */
        public void clearRequests() {
	        elevatorRequests.clear();
	    }

        /**
         * getStopElevator
         * 
         * Gets the current value of stopElevator
         * 
         * @param   None
         * 
         * @return  boolean Current value of stopElevator
         */
        public boolean getStopElevator() {
            return(stopElevator);
        }

        /**
         * setUsable
         * 
         * Sets the elevatorUsable boolean to the given value
         * 
         * @param newVal    The new value for elevatorUsable
         * 
         * @return  void
         */
        public void setUsable(boolean newVal) {
            elevatorUsable = newVal;            
        }

        /**
         * setCurrFloor
         * 
         * Sets the current floor of the elevator to the given value
         * 
         * @param floorNum  The new floor number for the elevator
         * 
         * @return  void
         */
        public void setCurrFloor(Byte floorNum) {
            currFloor = floorNum;            
        }

        /**
         * setStopElevator
         * 
         * Sets stopElevator to the given value
         * 
         * @param newVal    The new value for stopElevator
         * 
         * @return  void
         */
        public void setStopElevator(boolean newVal) {
            stopElevator = newVal;            
        }

        /**
         * isUsable
         * 
         * Returns the current value of elevatorUsable
         * 
         * @param   None
         * 
         * @return  boolean The current value of elevatorUsable
         */
        public boolean isUsable() {
            return(elevatorUsable);
        }

        /**
         * getCurrFloor
         * 
         * Returns the current floor of the elevator
         * 
         * @param   None
         * 
         * @return  byte    The current floor of the elevator
         */
        public byte getCurrFloor() {
            return(currFloor);
        }
        
        /**
         * howManyMoreActiveRequests
         * 
         * Returns the number of active requests that this elevator has
         * i.e. the number of requests that have not been completed yet
         * 
         * @param   None
         * 
         * @return  int The number of active requests
         */
        public int howManyMoreActiveRequests() {
            int count = 0;
            
            for (Request req : elevatorRequests) {
                if (!req.getElevatorPickupTimeFlag() || !req.getElevatorArrivedDestinationTimeFlag()) {
                    count += 1;
                }
            }
            
            return(count);
        }
        
        /**
         * toString
         * 
         * Returns a string describing this elevator
         * 
         * @param   None
         * 
         * @return  String describing this object
         */
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


