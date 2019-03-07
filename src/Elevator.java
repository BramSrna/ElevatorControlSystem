/**
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
 * Last Edited February 13,2019
 * 
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
                System.out.println("DONE TEST");
            }
        } ).start();
        
        System.out.println("Elevator Door Opened");
        door = doorState.OPEN;
        System.out.println("Door: " + door + "on floor: " + currentFloor);
    }

    /*
     * Method to close the elevator door.
     */
    public void closeDoor() {        
        new Thread( new Runnable() {
            public void run()  {
                try  { 
                    Thread.sleep(UtilityInformation.CLOSE_DOOR_TIME);
                } catch (InterruptedException ie)  {
                    
                }
                System.out.println("DONE TEST");
            }
        } ).start();
        
        System.out.println("Elevator Door Closed");
        door = doorState.CLOSED;
        System.out.println("Door: " + door + "on floor: " + currentFloor);
    }    
}