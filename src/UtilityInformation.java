public class UtilityInformation {

	public final static int MAX_BYTE_ARRAY_SIZE = 100;

	// Lamp states for lamps in Elevators and on Floors
	public enum LampState {
		OFF, ON
	}

	// Button states for buttons on Elevator and Floors
	public enum ButtonState {
		UNPRESSED, PRESSED
	}

	// ElevatorDirection for display on Floors
	public enum ElevatorDirection {
		STATIONARY, UP, DOWN
	}

	public enum ErrorType {
		DOOR_STUCK_ERROR, ELEVATOR_STUCK_ERROR
	}
	
	public enum DoorState
	{
	    CLOSE, OPEN
	}
	
	// Byte in messages used to indicate the type of message
	public final static int MODE_BYTE_IND = 0;

	// Modes
	public final static byte CONFIG_MODE = 0;
	public final static byte FLOOR_SENSOR_MODE = 1;
	public final static byte FLOOR_REQUEST_MODE = 2;
	public final static byte ELEVATOR_BUTTON_HIT_MODE = 3;
	public final static byte ELEVATOR_DIRECTION_MODE = 4;
	public final static byte ELEVATOR_DOOR_MODE = 5;
	public final static byte SEND_DESTINATION_TO_ELEVATOR_MODE = 6;
	public final static byte TEARDOWN_MODE = 7;
	public final static byte CONFIG_CONFIRM_MODE = 8;
	public final static byte ELEVATOR_STOPPED_MODE = 9;
	public final static byte ERROR_MESSAGE_MODE = 10;
	public final static byte FIX_ERROR_MODE = 11;
	public final static byte FIX_DOOR_MODE = 12;
	public final static byte ALL_REQUESTS_FINISHED_MODE = 13;

	// Messages (End of Message Indicator)
	public final static byte END_OF_MESSAGE = -1;

	// Ports
	public final static int SCHEDULER_PORT_NUM = 420;
	public final static int ELEVATOR_PORT_NUM = 69;
	public final static int FLOOR_PORT_NUM = 666;

	// Information to configure times for important actions (in milliseconds)
	public final static int OPEN_DOOR_TIME = 1500;
	public final static int CLOSE_DOOR_TIME = 1500;
	public final static int TIME_UP_ONE_FLOOR = 5000;
	public final static int TIME_DOWN_ONE_FLOOR = 5000;

	// Valid ranges for the number of
	// floors and number of elevators
	public final static int MIN_NUM_FLOORS = 1;
	public final static int MAX_NUM_FLOORS = 22;

	public final static int MIN_NUM_ELEVATORS = 1;
	public final static int MAX_NUM_ELEVATORS = 4;
}
