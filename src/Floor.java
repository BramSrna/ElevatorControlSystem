import java.util.ArrayList;

public class Floor {
    private FloorSubsystem controller;
    
    private int floorNum;
    private int numElevatorShafts;
    
    private ArrayList<HardwareState.LampState> arrivalLamp;
    private ArrayList<HardwareState.ElevatorDirection> arrivalLampDir;
    
    private HardwareState.ButtonState upButton;
    private HardwareState.ButtonState downButton;
    
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
        
        arrivalLamp = new ArrayList<HardwareState.LampState>();
        arrivalLampDir = new ArrayList<HardwareState.ElevatorDirection>();
                
        // Configure lamps and buttons
        for (int i = 0; i < arrivalLamp.size(); i++) {
        	arrivalLamp.add(HardwareState.LampState.OFF);
        	arrivalLampDir.add(HardwareState.ElevatorDirection.STATIONARY);
        }
        
        upButton = HardwareState.ButtonState.UNPRESSED;
        downButton = HardwareState.ButtonState.UNPRESSED;
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
					    		HardwareState.ElevatorDirection direction,
					    		int endFloor) {
    	// Set the button and lamp states
        if (direction == HardwareState.ElevatorDirection.UP) {
            upButton = HardwareState.ButtonState.PRESSED;
        } else if (direction == HardwareState.ElevatorDirection.DOWN) {
            downButton = HardwareState.ButtonState.PRESSED;
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
	public HardwareState.LampState getArrivalLamp(int elevatorShaftNum) {
		return arrivalLamp.get(elevatorShaftNum);
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
	public void setArrivalLamp(HardwareState.LampState newLampState, int elevatorShaftNum) {
		arrivalLamp.set(elevatorShaftNum, newLampState);
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
	public HardwareState.ElevatorDirection getArrivalLampDir(int elevatorShaftNum) {
		return arrivalLampDir.get(elevatorShaftNum);
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
	public void setArrivalLampDir(HardwareState.ElevatorDirection newDirection, int elevatorShaftNum) {
		arrivalLampDir.set(elevatorShaftNum, newDirection);
	}
}
