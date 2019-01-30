public class UtilityInformation {

	public final static int MAX_BYTE_ARRAY_SIZE = 100;

	public enum LampState {
		OFF, ON
	}

	public enum ButtonState {
		UNPRESSED, PRESSED
	}

	public enum ElevatorDirection {
		DOWN, UP, STATIONARY
	}

	// Modes
	public final static byte CONFIG_MODE = 0;
	public final static byte FLOOR_SENSOR_MODE = 1;
	public final static byte FLOOR_REQUEST_MODE = 2;
	public final static byte ELEVATOR_BUTTON_HIT_MODE = 3;
	public final static byte ELEVATOR_DIRECTION_MODE = 4;
	public final static byte ELEVATOR_DOOR_MODE = 5;
	public final static byte SEND_DESTINATION_TO_ELEVATOR_MODE = 6;
	public final static byte TEARDOWN_MODE = 7;
	public final static byte CONFIG_CONFIRM = 8;

	// Messages
	public final static byte ELEVATOR_STAY = 0;
	public final static byte ELEVATOR_UP = 1;
	public final static byte ELEVATOR_DOWN = 2;
	public final static byte END_OF_MESSAGE = -1;
	public final static byte DOOR_CLOSE = 0;
	public final static byte DOOR_OPEN = 1;

	// Ports
	public final static int SCHEDULER_PORT_NUM = 420;
	public final static int ELEVATOR_PORT_NUM = 69;
	public final static int FLOOR_PORT_NUM = 666;
}
