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
public class Elevator {
	// The elevator car number
	int elevatorNumber;
	// The current floor the elevator is on
	int currentFloor = 0;
	
	// USED ENUMS:
	//Enum for Door
	enum doorState {OPEN, CLOSED}
	private doorState door = doorState.CLOSED;
	
	// Enum for all lights
	enum lampState {OFF, ON}
	
	// The lamps indicate the floor(s) which will be visited by the elevator
	lampState[] allButtons;
	
	Elevator_Subsystem controller;
	
	/*
	 * General Constructor for Elevator Class
	 */
	
	boolean inError = false;
	
	public boolean isDamaged = false;
	
	public Elevator(Elevator_Subsystem controller, int number) {
		elevatorNumber = number;
		this.controller = controller;
	}
	
	public int getElevatorNumber() {return this.elevatorNumber;}
	public int getCurrentFloor() {return this.currentFloor;}
	public doorState getDoorState() {return this.door;}
	
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
	public void goUp() {
		System.out.println("Elevator Moving Up One Floor");
        new Thread( new Runnable() {
            public void run()  {
                try  { 
                    Thread.sleep(UtilityInformation.TIME_UP_ONE_FLOOR);
                } catch (InterruptedException ie)  {

                }
                controller.sendFloorSensorMessage(elevatorNumber);
            }
        } ).start();
		currentFloor++;
		//byte[] data = { UtilityInformation.FLOOR_SENSOR_MODE , (byte) currentFloor, (byte) elevatorNumber, -1 };
		Elevator_Subsystem.currentState = Elevator_Subsystem.State.ARRIVE_AT_FLOOR;
		System.out.println("Elevator arrives on floor");
		//this.sendData(data, schedulerIP, schedulerPort);
	}

	/*
	 * Method to make the elevator move down one floor.
	 */
	public void goDown() {
		System.out.println("Elevator Moving Down One Floor");
        new Thread( new Runnable() {
            public void run()  {
                try  { 
                    Thread.sleep(UtilityInformation.TIME_DOWN_ONE_FLOOR);
                } catch (InterruptedException ie)  {

                }
                controller.sendFloorSensorMessage(elevatorNumber);
            }
        } ).start();
		currentFloor--;
		//byte[] data = { UtilityInformation.FLOOR_SENSOR_MODE, (byte) currentFloor, (byte) elevatorNumber, -1 };
		System.out.println("Elevator arrives on floor");
		//this.sendData(data, schedulerIP, schedulerPort);
	}
	
	/*
	 * Method to make the elevator stop moving.
	 */
	public void Stop() {
        //byte[] data = { UtilityInformation.ELEVATOR_STOPPED_MODE, (byte) currentFloor, (byte) elevatorNumber, -1 };
        System.out.println("The elevator has stopped moving");
        //this.sendData(data, schedulerIP, schedulerPort);
	}

	/*
	 * Method to open the elevator door.
	 */
	public void openDoor() {
        new Thread( new Runnable() {
            public void run()  {
                try  { 
                    Thread.sleep(UtilityInformation.OPEN_DOOR_TIME);
                } catch (InterruptedException ie)  {

                }
            }
        } ).start();
		
		System.out.println("Elevator Door Opened");
		door = doorState.OPEN;
		System.out.println("Door: " + door + " on floor: " + currentFloor);
	}

	/*
	 * Method to close the elevator door.
	 */
	public void closeDoor() {       
        new Thread(new Runnable() {
            public void run()  {
                try  { 
                    Thread.sleep(UtilityInformation.CLOSE_DOOR_TIME);
                } catch (InterruptedException ie)  {

                }
            }
        }).start();
		
		System.out.println("Elevator Door Closed");
		door = doorState.CLOSED;
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
		inError = true;
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
		inError = false;
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
	    // Set the elevator to damaged
	    this.isDamaged = true;
	    
	    // Run the loop until the door is fixed.
	    new Thread(new Runnable() {
            public void run()  {
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
                closeDoor();
            } else if (doorState == UtilityInformation.ErrorType.DOOR_WONT_OPEN_ERROR.ordinal()) {
                openDoor();
            } else {
                System.out.println("Error: Unknown error type in Elevator fixDoorStuckError.");
                System.exit(1);
            }
            
            // Set the elevator to undamaged
            isDamaged = false;
            
            // Tell the controller that the door is fixed
            controller.sendElevatorDoorFixedMessage(elevatorNumber);
            }
        }).start();
	    
	}
	
}