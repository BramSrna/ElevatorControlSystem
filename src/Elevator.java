import java.util.Random;

/*
 * SYSC 3303 Elevator Group Project
 * Elevator.java
 * @ author Samy Ibrahim 
 * @ student# 101037927
 * @ version 1
 * 
 * The elevator class consists of the buttons and lamps inside of the elevator used to select floors and indicate the
 * floors selected, and to indicate the location of the elevator itself. The elevator subsystem is also used to operate the
 * motor and to open and close the doors. Each elevator has its own elevator subsystem. 
 * 
 * For the purpose of this project, the elevator subsystem listens for packets from the scheduler to control the motor
 * and to open the doors. The elevator subsystem also has to monitor the floor subsystem for destination requests
 * (button presses inside of the elevator car, rather than button presses at the floors) from the input file. Button presses
 * are to be rerouted to the scheduler system. Lamp (floor indications) from button pushes do not have to originate
 * from the scheduler. Rather, when the elevator subsystem detects a button request, it can then light the
 * corresponding lamp. When the elevator reaches a floor, the scheduling subsystem signals the elevator subsystem to
 * turn the lamp of
 * 
 * Last Edited March 7, 2019.
 */
public class Elevator implements Runnable {
	// The elevator car number
	int elevatorNumber;
	// The current floor the elevator is on
	int currentFloor = 0;
	
	private UtilityInformation.DoorState door = UtilityInformation.DoorState.CLOSE;
	
	// Enum for all lights
	enum lampState {OFF, ON}
	
	// The lamps indicate the floor(s) which will be visited by the elevator
	lampState[] allButtons;
	
	Elevator_Subsystem controller;
	
	/*
	 * General Constructor for Elevator Class
	 */	
	// USED ENUMS:
	// State machine states
	enum State {
		MOVE_UP,
		MOVE_DOWN,
		OPEN_DOOR,
		CLOSE_DOOR,
		BROKEN,
		DAMAGED_OPEN,
		DAMAGED_CLOSED,
		WAITING,
		FIXED
	}
	
	private State currState;
	
	public Elevator(Elevator_Subsystem controller, int number) {
		elevatorNumber = number;
		this.controller = controller;
		currState = State.WAITING;
	}
	
	public int getElevatorNumber() {return this.elevatorNumber;}
	public int getCurrentFloor() {return this.currentFloor;}
	public UtilityInformation.DoorState getDoorState() {return this.door;}
	
	/*
	 * Method to print out each elevators data.
	 */
	public void display() {
		// Simply display 
		System.out.println("Elevator Number: " + this.getElevatorNumber());
		System.out.println("Floor Number: " + this.getCurrentFloor() + "\n");
		for(int i=0; i<allButtons.length; i++) {
			System.out.println("Floor Number " + i + ": " +allButtons[i]);
		}
		System.out.println("\n");
	}
	
	/*
	 * Method to make the elevator move up one floor.
	 */
	public void move(UtilityInformation.ElevatorDirection dir) {		
    	System.out.println(String.format("Elevator Moving %s One Floor", dir.toString()));
    	
        try  { 
            Thread.sleep(UtilityInformation.TIME_UP_ONE_FLOOR);
        } catch (InterruptedException ie)  {

        }                
        
        currentFloor++;
        
        controller.sendFloorSensorMessage(elevatorNumber);
        
        System.out.println("Elevator arrives on floor");
	}
	
	/*
	 * Method to make the elevator stop moving.
	 */
	public void Stop() {
        System.out.println("The elevator has stopped moving");
	}

	/*
	 * Method to open the elevator door.
	 */
	public void changeDoorState(UtilityInformation.DoorState newState) {
        try  { 
            Thread.sleep(UtilityInformation.OPEN_DOOR_TIME);
        } catch (InterruptedException ie)  {

        }
        
        System.out.println(String.format("Elevator Door %s", newState.toString()));
        
		door = newState;
		System.out.println("Door: " + door + " on floor: " + currentFloor);
	}
	
	/**
	 * brokenElevator
	 * 
	 * Set this elevator to the broken state.
	 * 
	 * @param  None
	 * 
	 * @return None
	 */
	public void brokenElevator() {
		System.out.println("Elevator is Broken");
	}
	
	/**
	 * elevatorFixed
	 * 
	 * Set this elevator to the fixed state
	 * 
	 * @param  None
	 * 
	 * @return None
	 */
	public void elevatorFixed() {
		System.out.println("Elevator is Fixed");
	}
	
	/**
	 * fixDoorStuckError
	 * 
	 * Fix this elevator door. Runs a wait loop until the
	 * door is fixed. Current probability of the door being fixed
	 * is 40 percent.
	 * 
	 * @param doorState    The door state that the elevator is broken in
	 * 
	 * @return None
	 */
	public void fixDoorStuckError(byte doorState) {
	    // Run the loop until the door is fixed.
        Random r = new Random();
        boolean broken = true;
        float chance;
        
        // The percent change that the elevator will be fixed
        float percentChanceFixDoor = 0.4f;
        
        int sleepTimeBetweenAttempts = 1000;
        
        while(broken) {
            try {
                Thread.sleep(sleepTimeBetweenAttempts);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            System.out.println("Attempting to fix door...");
            chance = r.nextFloat();
            if(chance <= percentChanceFixDoor){
                broken = false;
            }
        }
    
	    // Set the door to the fixed state
	    if (doorState == UtilityInformation.ErrorType.DOOR_WONT_CLOSE_ERROR.ordinal()) {
	        changeDoorState(UtilityInformation.DoorState.CLOSE);
	    } else if (doorState == UtilityInformation.ErrorType.DOOR_WONT_OPEN_ERROR.ordinal()) {
	    	changeDoorState(UtilityInformation.DoorState.OPEN);
	    } else {
	        System.out.println("Error: Unknown error type in Elevator fixDoorStuckError.");
	        System.exit(1);
	    }
	    
	    // Tell the controller that the door is fixed
	    controller.sendElevatorDoorFixedMessage(elevatorNumber);	    
	}

	public State getCurrState() {
		return(currState);
	}

	public boolean isInErrorState() {
		return(currState.equals(State.BROKEN));
	}
	
	public void changeState(State newState) {
		currState = newState;
		
		switch (currState) {
		case MOVE_UP:
			move(UtilityInformation.ElevatorDirection.UP);
			break;
		case MOVE_DOWN:
			move(UtilityInformation.ElevatorDirection.DOWN);
			break;
		case OPEN_DOOR:
			changeDoorState(UtilityInformation.DoorState.OPEN);
			break;
		case CLOSE_DOOR:
			changeDoorState(UtilityInformation.DoorState.CLOSE);
			break;
		case BROKEN:
			brokenElevator();
			break;
		case DAMAGED_OPEN:
			fixDoorStuckError((byte) UtilityInformation.ErrorType.DOOR_WONT_OPEN_ERROR.ordinal());
			break;
		case DAMAGED_CLOSED:
			fixDoorStuckError((byte) UtilityInformation.ErrorType.DOOR_WONT_CLOSE_ERROR.ordinal());
			break;
		case WAITING:
			break;
		default:
			System.out.println("Error: Unknown State.");
			System.exit(1);
		}
		
		currState = State.WAITING;
	}

	@Override
	public void run() {		
		while (true) {
			State nextState = controller.getNextStateForElevator(elevatorNumber);
			changeState(nextState);
		}
		
	}
	
}