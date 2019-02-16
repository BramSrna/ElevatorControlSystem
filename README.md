"# ElevatorControlSystem" 

Description: The team designed and implemented an elevator control system and simulator. The system consists of an elevator
controller (the Scheduler), a simulator for the elevator cars (which includes, the lights, buttons, doors and motors)
and a simulator for the floors (which includes, buttons, lights).

Files involved in the system:
	Elevator.java
		Class ised to simulate an elevator. Simulates the lights, buttons, door, and movement.
    ElevatorSubsystem.java
        Class used to control the Elevator objects in the simulation. Communicates with the Scheduler to control the movement of the elevators as well as other elevator behaviour, sich as opening the door.
    ElevatorSubsystemTests.java
        Contains JUnit test cases for the ElevatorSubsystem calss
    Floor.java
        Class used to simulate a floor. Simulates the lights and buttons.
    FloorSubsystem.java
        Class used to control the floors in the simulation. Updates floors with elevator location and controls communication with the scheduler.
    FloorSubsytemTests.java
        Contains JUnit test cases for the FloorSubsystem class.
    Scheduler.java
        Simulates the Scheduler for the system. Controls both the FloorSubsystem and the Elevator classes. In charge of telling the classes what to do. This includes moving the elevator, turning on and off ligths, etc.
    SchedulerAlgorithm.java
		Controls the logic for taking requests and divvying the requests to the proper Elevator to minimize wait time.
	SchedulerTest.java
        Contains JUnit test cases for the Scheduelr class.
	ServerPattern.java
		An abstract class that runs a Thread used to constantly receive data and add the received packets to a shared buffer. Implementation classes then wait on this shared buffer to get messages.
    TestHost.java
        Helper class used in JUnit test cases by acting as an EchoServer.
    UserInterface.java
        Contains the code needed for communicating with the user and obtaining needed information.
    UtilityInformation.java
        Header file containg important information shared between the three systems.
        
Setup instructions: 
	1. Extract the zip file, this should contain the code for the assignment and all UML diagrams
	3. In Eclipse select File > Import 
	4. Select General > Existing Project into Workspace and press Next
	5. Choose Select root directory and press Browse...
	6. Browse to the location where the assignment was unzipped to, select it, and press ok
	7. Under Projects select the project 
	8. Click finish

To test the program using JUnit test cases:
  Each system contains corresponding JUnit test cases. To run the JUnit test, perform the following:
	1. Select the Junit test file to run in Eclipse. The JUnit test files are: ElevatorSubsystemTests.java, FloorSubsystemTests.java, and SchedulerTest.java.
	2. Press the "Run" button at the top of the Eclipse interface.
	3. Repeat for the other JUnit test files
    
To test the program using a file: 
	1. Run ElevatorSubsystem.java, Scheduler.java, and FloorSubsystem.java concurrently.
	      To run them, right-click the java file and select Run As > Java Application.
	2. Repeat this for all three java files.
	      Note: The order of the run files is unimportant. On some systems, it takes Eclipse a while to finish starting a file, in which case, errors will occur when the other files are run. If this happens, wait a few moments and then run the next file.
	3. Once all three files are running, open the FloorSubsystem console.
	4. Follow the UI prompts, entering numbers for the number of elevators and number of floors.
	5. When the configuraiton is finished and the menu is displayed, enter "3" and then browse and select the file you want to use. Test file is in the directory ElevatorControlSystem -> src -> Test Files -> 5s tests -> OneRequest.txt.

Running the project: 
	- Once the configuration information (# of floors, # of elevators) is given, the FloorSubsystem will send this information to the Scheduler, which will forward it to the Elevator. A confirmation will then be sent back.
	- When this is complete, the user will select a test file.
	- The test file will then be parsed.
	- The FloorSubsystem will then send these requests to the Scheduler with the appropriate spacing between requests.
	- The Scheduler will receive the requests and then use this information to control the elevator(s).
	- The Scheduler will send instructions to the Elevator system and FloorSubsystem containing movement requests, button and lamp states, door operations, and other important information.
	- Once the final request is complete, the user can then exit the program, causing a teardown signal to be sent through the system, exiting all running files.

Breakdown of responsibilities for Iteration #1:
  
  Group 1: 

    Samy Ibrahim (101037927): Elevator.java, elevatorSubsystem.java, testHost.java, UtilityInformation.java, Elevator UML Class diagram, Sequence diagram and State diagram
    
    Tri Nhan (101023872) : Floor.java, FloorSubsystem.java, FloorSubsystemTests.java, TestHost.java, UserInterface.java, UtilityInformation.java, ServerPattern.java, GUI, Floor UML diagram

    Abraham Srna (100997482) : Floor.java, FloorSubsystem.java, FloorSubsystemTests.java, TestHost.java, UserInterface.java, UtilityInformation.java, ServerPattern.java, Floor UML diagram

    Haseeb Khan(101009713) : Scheduler.java, SchedulerTest.java, TestHost.java, UtilityInformation.java, ServerPattern.java, Scheduler UML diagram, Scheduler State diagram
    
    Hashim Hussen (100996269) : Elevator.java, elevatorSubsystemTest.java, testHost.java, Elevator UML and State
