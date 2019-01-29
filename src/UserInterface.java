import java.awt.FileDialog;
import java.awt.Frame;
import java.util.Scanner;

public class UserInterface {
	// Current number of floors and elevatos
	private int numFloors;
	private int numElevators;
	
	// Minimum an maximum values for the number of floors
	// and the number of elevators
	private int minFloorVal;
	private int maxFloorVal;
	
	private int minElevatorVal;
	private int maxElevatorVal;
	
	// Path to the current test file being run
	private String pathToTestFile;
	
	private Scanner input;
	
	// Return values for options the user selected
	public enum ReturnVals {
		RECONFIG,
		NEW_TEST_FILE,
		TEARDOWN
	}
	
	/**
	 * UserInterface
	 * 
	 * Constructor
	 * 
	 * Get the valid ranges for the number of floors and number
	 * of elevators from the user.
	 * Initialize the Scanner object.
	 * 
	 * @param	Nonve
	 * 
	 * @return	None
	 */
	public UserInterface() {
		int validRange[];
		
		// Get valid range for the number of floors
		validRange = FloorSubsystem.getValidFloorValueRange();
		
		minFloorVal = validRange[0];
		maxFloorVal = validRange[1];
		
		// Get valid range for the number of elevators
		validRange = FloorSubsystem.getValidElevatorValueRange();
		
		minElevatorVal = validRange[0];
		maxElevatorVal = validRange[1];
		
		validRange = null;
		
		// Initialize the Scanner
		input = new Scanner(System.in);
	}
	
	/**
	 * getNewConfigurationInformation
	 * 
	 * Get the needed configuraiton information.
	 * Get the number of elevators.
	 * Get the number of floors.
	 * 
	 * @param	None
	 * 
	 * @return	void
	 */
	public void getNewConfigurationInformation() {    	
		getNewNumElevators();
		getNewNumFloors();
	}
	
	/**
	 * getNewNumElevators
	 * 
	 * Get a new number of elevators from the user.
	 * Ensures that the entered value is valid.
	 * 
	 * @param	Nonw
	 * 
	 * @return	void
	 */
	public void getNewNumElevators() {
    	boolean valid = false;
    	
    	// Get input from the user until a valid range is entered
    	while (!valid) {
	    	System.out.print(String.format("Enter the number of elevators (%d to %d): ", 
			    			 minElevatorVal, 
			    			 maxElevatorVal));
	    	
	    	numElevators = input.nextInt();
	    	
	    	// Check that the entered value is valid
	    	if ((numElevators >= minElevatorVal) && (numElevators <= maxElevatorVal)) {
	    		valid = true;
	    	} else {
	    		System.out.println("Invalid number of elevators. "
	    				+ "Please enter a new value within the valid range.");
	    	}
    	}
	}
	
	/**
	 * getNewNumFloors
	 * 
	 * Get a new number of floors from the user.
	 * Gets input until a valid value is entered.
	 * 
	 * @param	None
	 * 
	 * @return	void
	 */
	public void getNewNumFloors() {
    	boolean valid = false;
    	
    	// Get input from the user until a valid value is entered
    	while (!valid) {
	    	System.out.print(String.format("Enter the number of floors (%d to %d): ", 
			    			 minFloorVal, 
			    			 maxFloorVal));
	    	
	    	numFloors = input.nextInt();
	    	
	    	// Check that the entered value is valid
	    	if ((numFloors >= minFloorVal) && (numFloors <= maxFloorVal)) {
	    		valid = true;
	    	} else {
	    		System.out.println("Invalid number of floors. "
	    				+ "Please enter a new value within the valid range.");
	    	}
    	}
	}
	
	/**
	 * getNewTestFile
	 * 
	 * Uses a dialog box to get the user to choose a new test file.
	 * 
	 * @param	None
	 * 
	 * @return	void
	 */
	public void getNewTestFile() {
		FileDialog dialog = new FileDialog((Frame)null, "Select File to Open");
	    dialog.setMode(FileDialog.LOAD);
	    dialog.setVisible(true);
	    pathToTestFile = dialog.getDirectory() + dialog.getFile();
	    System.out.println("Selected file: " + pathToTestFile);
	}
	
	/**
	 * displayMenu
	 * 
	 * Displays the valid options to the user.
	 * Gets input until a valid choice is selected.
	 * Runs the corresponding method(s) depending on the choice.
	 * 
	 * @return ReturnVals Enum describing the option chosen
	 */
	public ReturnVals displayMenu() {
		// Corresponding input values for each choice
		int newNumElevatorsChoice = 1;
		int newNumFloorsChoice = 2;
		int newTestFileChoice = 3;
		int exitChoice = 4;
		
		boolean valid = false;
		
		// Loop until a valid value is chosen
		while (!valid) {
			// Display the menu
			System.out.println("Please choose one of the following options:");
			System.out.println(String.format("\t%d. Enter a new number of elevators.", newNumElevatorsChoice));
			System.out.println(String.format("\t%d. Enter a new number of floors.", newNumFloorsChoice));
			System.out.println(String.format("\t%d. Choose a new test file.", newTestFileChoice));
			System.out.println(String.format("\t%d. Exit program.", exitChoice));
			
			// Get the choice from the user
			int choice = input.nextInt();
			
			valid = true;
			
			// Check that the choice was valid and
			// run the corresponding method(s)
			if (choice == newNumElevatorsChoice) {
				// Get new number of elevators from user
				this.getNewNumElevators();				
				return(ReturnVals.RECONFIG);
			} else if (choice == newNumFloorsChoice) {
				// Get new number of floors from the user
				this.getNewNumFloors();				
				return(ReturnVals.RECONFIG);
			} else if (choice == newTestFileChoice) {
				// Get new test file from the user
				this.getNewTestFile();
				return(ReturnVals.NEW_TEST_FILE);
			} else if (choice == exitChoice) {
				// Exit program
				input.close();
				System.out.println("Exiting. Thank you for using the program!");
				return(ReturnVals.TEARDOWN);
			} else {
				System.out.println("Error: Invalid choice. Please pick a valid option.");
				valid = false;
			}
		}
		return null;
		
	}
	
	/**
	 * getNumFloors
	 * 
	 * Return the current number of floors.
	 * 
	 * @return	int	Number of floors
	 */
	public int getNumFloors() {
		return(numFloors);
	}
	
	/**
	 * getNumElevators
	 * 
	 * Return the current number of elevators.
	 * 
	 * @return	int	Number of elevators
	 */
	public int getNumElevators() {
		return(numElevators);
	}
	
	/**
	 * getTestFile
	 * 
	 * Return the path to the current test file.
	 * 
	 * @return String	Path to the current test file.
	 */
	public String getTestFile() {
		return(pathToTestFile);
	}
}
