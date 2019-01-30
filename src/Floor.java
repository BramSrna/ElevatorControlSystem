public class Floor {
    private FloorSubsystem controller;
    
    private int floorNum;
    private int numElevatorShafts;    
    
    private enum lampState {
        OFF,
        ON
    }
    
    private lampState arrivalLamp;
    private FloorSubsystem.Direction arrivalLampDir;
    
    private lampState upButtonPressed;
    private lampState downButtonPressed;
    
    /**
     * Floor
     * 
     * Constructor
     * 
     * Creates a new Floor object.
     * Initializes all of the buttons and lamps.
     * 
     * @param controller	FloorSubsystem that this Floor belongs to
     * @param floorNum		This Floor's number
     * @param numElevatorShafts	Number of elevator shafts on the Floor
     * 
     * @return	None
     */
    public Floor(FloorSubsystem controller, 
                 int floorNum, 
                 int numElevatorShafts) {
    	// Save all of the information
        this.controller = controller;
        
        this.floorNum = floorNum;
        this.setNumElevatorShafts(numElevatorShafts);
        
        // Configure lamps and buttons
        setArrivalLamp(lampState.OFF);
        setArrivalLampDir(FloorSubsystem.Direction.UP);
        
        upButtonPressed = lampState.OFF;
        downButtonPressed = lampState.OFF;
    }
    
    /**
     * elevatorArriving
     * 
     * Tells the FloorSubsystem that an elevator is arriving at this floor.
     * Turns on the corresponding lamps.
     * 
     * @param elevatorShaftNum	Elevator shaft number where the elevator is
     * @param direction	Direction that the elevator is travelling
     * 
     * @return	void
     */
    public void elevatorArriving(int elevatorShaftNum, FloorSubsystem.Direction direction) {
    	// Set the lamp values
        setArrivalLamp(lampState.ON);
        setArrivalLampDir(direction);
        
        // Turn off the buttons
        if ((direction == FloorSubsystem.Direction.UP) && (upButtonPressed == lampState.ON)) {
            upButtonPressed = lampState.OFF;
        } else if ((direction == FloorSubsystem.Direction.DOWN) && (downButtonPressed == lampState.ON)) {
            downButtonPressed = lampState.OFF;
        }
        
        // Tell the controller to send the signal
        controller.sendArrivalSensorSignal(this.floorNum, elevatorShaftNum);
    }
    
    /**
     * elevatorRequest
     * 
     * Tells the FloorSubsystem that a new request was made at this Floor.
     * Sets the corresponding lamp and button values.
     * 
     * @param hourOfCall	Hour that call was made
     * @param minOfCall		Minute that call was made
     * @param secOfCall		Second that call was made
     * @param msOfCall		Millisecond that call was made
     * @param direction		Direction that the user wants to travel
     * @param endFloor		Floor that user wants to travel to
     * 
     * @return	void
     */
    public void elevatorRequest(int hourOfCall, 
					    		int minOfCall, 
					    		int secOfCall, 
					    		int msOfCall, 
					    		FloorSubsystem.Direction direction,
					    		int endFloor) {
    	// Set the button and lamp states
        if (direction == FloorSubsystem.Direction.UP) {
            upButtonPressed = lampState.ON;
        } else if (direction == FloorSubsystem.Direction.DOWN) {
            downButtonPressed = lampState.ON;
        }
        
        // Tell the controller to send the request
        controller.addElevatorRequest(hourOfCall, 
						        	  minOfCall, 
						        	  secOfCall, 
						        	  msOfCall, 
						        	  this.floorNum, 
						        	  direction, 
						        	  endFloor);
    }
    
    /**
     * getFloorNumber
     * 
     * Returns the number of this floor.
     * 
     * @param	None
     * 
     * @return	int Floor number of this Floor
     */
    public int getFloorNumber() {
    	return(this.floorNum);
    }

	/**
	 * getNumElevatorShafts
	 * 
	 * Return the number of elevator shafts at this floor.
	 * 
	 * @param	None
	 * 
	 * @return int	Number of elevator shafts on this floor
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
	 * @return	void
	 */
	public void setNumElevatorShafts(int numElevatorShafts) {
		this.numElevatorShafts = numElevatorShafts;
	}

	/**
	 * getArrivalLamp
	 * 
	 * Returns the state of the arrival lamp
	 * 
	 * @param	None
	 * 
	 * @return lampState	The state of the arrival lamp
	 */
	public lampState getArrivalLamp() {
		return arrivalLamp;
	}

	/**
	 * setArrivalLamp
	 * 
	 * Set the state of the arrival lamp
	 * 
	 * @param arrivalLamp The new value for the arrivalLamp
	 * 
	 * @return	void
	 */
	public void setArrivalLamp(lampState arrivalLamp) {
		this.arrivalLamp = arrivalLamp;
	}

	/**
	 * getArrivalLampDir
	 * 
	 * Returns the direction set on the arrivalLamp
	 * 
	 * @param	None
	 * 
	 * @return FloorSubsystem.Direction	Direction currently being displayed on the lamp
	 */
	public FloorSubsystem.Direction getArrivalLampDir() {
		return arrivalLampDir;
	}

	/**
	 * setArrivalLampDir
	 * 
	 * Sets the current direction displayed on the lamp.
	 * 
	 * @param arrivalLampDir The direction currently displayed on the lamp
	 * 
	 * @return	void
	 */
	public void setArrivalLampDir(FloorSubsystem.Direction arrivalLampDir) {
		this.arrivalLampDir = arrivalLampDir;
	}
}
