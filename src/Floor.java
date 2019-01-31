import java.util.ArrayList;

public class Floor {
	private FloorSubsystem controller;

	private int floorNum;
	private int numElevatorShafts;

	private ArrayList<Integer> elevatorLocation;
	private ArrayList<UtilityInformation.LampState> arrivalLamp;
	private ArrayList<UtilityInformation.ElevatorDirection> arrivalLampDir;

	private UtilityInformation.ButtonState upButton;
	private UtilityInformation.ButtonState downButton;

	/**
	 * Floor
	 * 
	 * Constructor
	 * 
	 * Creates a new Floor object. Initializes all of the buttons and lamps.
	 * 
	 * @param controller        FloorSubsystem that this Floor belongs to
	 * @param floorNum          This Floor's number
	 * @param numElevatorShafts Number of elevator shafts on the Floor
	 * 
	 * @return None
	 */
	public Floor(FloorSubsystem controller, int floorNum, int numElevatorShafts) {
		elevatorLocation = new ArrayList<Integer>();
		arrivalLamp = new ArrayList<UtilityInformation.LampState>();
		arrivalLampDir = new ArrayList<UtilityInformation.ElevatorDirection>();

		// Save all of the information
		this.controller = controller;

		this.floorNum = floorNum;
		this.setNumElevatorShafts(numElevatorShafts);

		// Configure lamps and buttons
		for (int i = 0; i < numElevatorShafts; i++) {
			elevatorLocation.add(0);
			arrivalLamp.add(UtilityInformation.LampState.OFF);
			arrivalLampDir.add(UtilityInformation.ElevatorDirection.STATIONARY);
		}

		upButton = UtilityInformation.ButtonState.UNPRESSED;
		downButton = UtilityInformation.ButtonState.UNPRESSED;
	}

	/**
	 * createElevatorRequest
	 * 
	 * Tells the FloorSubsystem that a new request was made at this Floor. Sets the
	 * corresponding lamp and button values.
	 * 
	 * @param hourOfCall Hour that call was made
	 * @param minOfCall  Minute that call was made
	 * @param secOfCall  Second that call was made
	 * @param msOfCall   Millisecond that call was made
	 * @param direction  Direction that the user wants to travel
	 * @param endFloor   Floor that user wants to travel to
	 * 
	 * @return void
	 */
	public void createElevatorRequest(int timeOfReq, UtilityInformation.ElevatorDirection direction, int endFloor) {
		// Set the button and lamp states
		if (direction == UtilityInformation.ElevatorDirection.UP) {
			upButton = UtilityInformation.ButtonState.PRESSED;
		} else if (direction == UtilityInformation.ElevatorDirection.DOWN) {
			downButton = UtilityInformation.ButtonState.PRESSED;
		}

		// Tell the controller to send the request
		controller.addElevatorRequest(timeOfReq, 
									  this.floorNum, 
									  direction, 
									  endFloor);
	}
	
	/**
     * updateElevatorLocation
     * 
     * Updates the lamps and buttons depending on the given
     * elevator's direction and location.
     * 
     * @param elevatorShaftNum	Elevator that is moving
     * @param floorNum	Floor number the elevator is at
     * @param direction	Direction of the elevator
     * 
     * @return	void
     */
    public void updateElevatorLocation(int elevatorShaftNum, 
							    	   int floorNum, 
							    	   UtilityInformation.ElevatorDirection direction) {
    	// If the elevator is at this floor
    	// Set the arrival lamp and
    	// check if this is the floor that the elevator shaft is stopping at
    	if ((floorNum == this.getFloorNumber()) && 
    		(direction == UtilityInformation.ElevatorDirection.STATIONARY)) {
    		// Turn off up/down buttons if the elevator is stopping at this floor
			arrivalLamp.set(elevatorShaftNum, UtilityInformation.LampState.ON);
			downButton = UtilityInformation.ButtonState.UNPRESSED;
			upButton = UtilityInformation.ButtonState.UNPRESSED;
    	} else {
    		arrivalLamp.set(elevatorShaftNum, UtilityInformation.LampState.OFF);
    	}
    	
    	// Update the elevator location and direction
    	elevatorLocation.set(elevatorShaftNum, floorNum);
    	arrivalLampDir.set(elevatorShaftNum, direction);
    }

	/**
	 * getFloorNumber
	 * 
	 * Returns the number of this floor.
	 * 
	 * @param None
	 * 
	 * @return int Floor number of this Floor
	 */
	public int getFloorNumber() {
		return (this.floorNum);
	}

	/**
	 * getNumElevatorShafts
	 * 
	 * Return the number of elevator shafts at this floor.
	 * 
	 * @param None
	 * 
	 * @return int Number of elevator shafts on this floor
	 */
	public int getNumElevatorShafts() {
		return numElevatorShafts;
	}

	/**
	 * setNumElevatorShafts
	 * 
	 * Set the number of elevator shafts on this floor
	 * 
	 * @param numElevatorShafts The new number of elevator shafts
	 * 
	 * @return void
	 */
	public void setNumElevatorShafts(int numElevatorShafts) {
		this.numElevatorShafts = numElevatorShafts;
		
		if (elevatorLocation.size() > numElevatorShafts) {
			while (elevatorLocation.size() > numElevatorShafts) {
				elevatorLocation.remove(elevatorLocation.size() - 1);
			}
		} else {
			while (elevatorLocation.size() < numElevatorShafts) {
				elevatorLocation.add(0);
			}
		}
		
		if (arrivalLamp.size() > numElevatorShafts) {
			while (arrivalLamp.size() > numElevatorShafts) {
				arrivalLamp.remove(arrivalLamp.size() - 1);
			}
		} else {
			while (arrivalLamp.size() < numElevatorShafts) {
				arrivalLamp.add(UtilityInformation.LampState.OFF);
			}
		}
		
		if (arrivalLampDir.size() > numElevatorShafts) {
			while (arrivalLampDir.size() > numElevatorShafts) {
				arrivalLampDir.remove(arrivalLampDir.size() - 1);
			}
		} else {
			while (arrivalLampDir.size() < numElevatorShafts) {
				arrivalLampDir.add(UtilityInformation.ElevatorDirection.STATIONARY);
			}
		}
	}

	/**
	 * getArrivalLamp
	 * 
	 * Returns the state of the arrival lamp
	 * 
	 * @param None
	 * 
	 * @return lampState The state of the arrival lamp
	 */
	public UtilityInformation.LampState getArrivalLamp(int elevatorShaftNum) {
		return arrivalLamp.get(elevatorShaftNum);
	}

	/**
	 * setArrivalLamp
	 * 
	 * Set the state of the arrival lamp
	 * 
	 * @param arrivalLamp The new value for the arrivalLamp
	 * 
	 * @return void
	 */
	public void setArrivalLamp(UtilityInformation.LampState newLampState, int elevatorShaftNum) {
		arrivalLamp.set(elevatorShaftNum, newLampState);
	}

	/**
	 * getArrivalLampDir
	 * 
	 * Returns the direction set on the arrivalLamp
	 * 
	 * @param None
	 * 
	 * @return FloorSubsystem.Direction Direction currently being displayed on the
	 *         lamp
	 */
	public UtilityInformation.ElevatorDirection getArrivalLampDir(int elevatorShaftNum) {
		return arrivalLampDir.get(elevatorShaftNum);
	}

	/**
	 * setArrivalLampDir
	 * 
	 * Sets the current direction displayed on the lamp.
	 * 
	 * @param arrivalLampDir The direction currently displayed on the lamp
	 * 
	 * @return void
	 */
	public void setArrivalLampDir(UtilityInformation.ElevatorDirection newDirection, int elevatorShaftNum) {
		arrivalLampDir.set(elevatorShaftNum, newDirection);
	}
	
	/**
	 * getUpButton
	 * 
	 * Returns the state of the upButton.
	 * 
	 * @param	None
	 * 
	 * @return UtilityInformation.ButtonState	State of the upButton
	 */
	public UtilityInformation.ButtonState getUpButton() {
		return upButton;
	}

	/**
	 * setUpButton
	 * 
	 * Sets the state of the upButton to the given state.
	 * 
	 * @param downButton The new state of the upButton
	 * 
	 * @return	void
	 */
	public void setUpButton(UtilityInformation.ButtonState newState) {
		this.upButton = newState;
	}

	/**
	 * getDownButton
	 * 
	 * Returns the state of the downButton.
	 * 
	 * @param	None
	 * 
	 * @return UtilityInformation.ButtonState	State of the down button
	 */
	public UtilityInformation.ButtonState getDownButton() {
		return downButton;
	}

	/**
	 * setDownButton
	 * 
	 * Sets the state of the downButton to the given state.
	 * 
	 * @param downButton The new state of the downButton
	 * 
	 * @return	void
	 */
	public void setDownButton(UtilityInformation.ButtonState newState) {
		this.downButton = newState;
	}
	
	public String toString() {	    
	    String toReturn = "";
	    
	    toReturn += String.format("Floor Number: %d ", floorNum);
	    
	    for (int i = 0; i < numElevatorShafts; i++) {
	        toReturn += String.format("Elevator: %d", i);
	        toReturn += String.format(", Floor: %d", elevatorLocation.get(i));
            toReturn += String.format(", Direction", arrivalLampDir.get(i));
            toReturn += String.format(", ArrivalLamp: %d ", i, arrivalLamp.get(i));
	    }
	    
        toReturn += String.format("Up Button: %s ", upButton.toString());
       
        toReturn += String.format("Down Button: %s ", downButton.toString());
	    
        return(toReturn);
	    
	}
}
