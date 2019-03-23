public class Request {
    // Info about service times of request
	private long elevatorRequestTime;
	private long elevatorPickupTime;
	private long elevatorArrivedDestinationTime;
	
	// Info about service status of request
	private boolean elevatorPickupTimeFlag;
	private boolean elevatorArrivedDestinationTimeFlag;
	
	// Info about request
	private byte source;
	private byte destination;
	private UtilityInformation.ElevatorDirection requestDirection;

	/**
	 * Request
	 * 
	 * Constructor
	 * 
	 * Creates a new Request
	 * 
	 * @param requestArrived   Time that request was received
	 * @param source   Source floor of request
	 * @param destination  Destination floor of request
	 * @param requestDirection Direction of request
	 * 
	 * @return None
	 */
	public Request(long requestArrived, byte source, byte destination,
			UtilityInformation.ElevatorDirection requestDirection) {
		elevatorRequestTime = requestArrived;
		this.source = source;
		this.destination = destination;
		this.requestDirection = requestDirection;
		elevatorPickupTime = -1;
		elevatorArrivedDestinationTime = -1;
		elevatorPickupTimeFlag = false;
		elevatorArrivedDestinationTimeFlag = false;
		// printRequestDetails();
	}

	/**
	 * setElevatorPickupTimeFlag
	 * 
	 * Set the elevatorPickupTimeFlag to true
	 * 
	 * @param  None
	 * 
	 * @return void
	 */
	public void setElevatorPickupTimeFlag() {
		elevatorPickupTimeFlag = true;
	}

	/**
	 * setElevatorArrivedDestinationTimeFlag
	 * 
	 * Set the elevatorArrivedDestinationTimeFlag to true
	 * 
	 * @param  None
	 * 
	 * @return void
	 */
	public void setElevatorArrivedDestinationTimeFlag() {
		elevatorArrivedDestinationTimeFlag = true;
	}

	/**
	 * setElevatorPickupTime
	 * 
	 * Set the elevatorPickupTime to the given value
	 * 
	 * @param time The new elevatorPickupTime value
	 * 
	 * @return void
	 */
	public void setElevatorPickupTime(long time) {
		elevatorPickupTime = time;
	}

	/**
	 * setElevatorArrivedDestinationTime
	 * 
	 * Set the elevatorArrivedDestinationTime to the given value
	 * 
	 * @param time The new elevatorArrivedDestinationTime value
	 * 
	 * @return void
	 */
	public void setElevatorArrivedDestinationTime(long time) {
		elevatorArrivedDestinationTime = time;
		printRequestDetails();
	}

	/**
	 * printRequestDetails
	 * 
	 * Print the information detailing this Request
	 * 
	 * @param  None
	 * 
	 * @return void
	 */
	public void printRequestDetails() {
		System.out.println("\nELEVATOR REQUEST: ");
		System.out.println("Source: " + source + ", Destination: " + destination + ", Direction: " + requestDirection);
		System.out.println("Elevator was requested at: " + elevatorRequestTime + "ns.");
		
		if (elevatorPickupTime != 0) {
			System.out.println("It took " + (elevatorPickupTime - elevatorRequestTime)
					+ "ns for an elevator to pickup the request.");
		}
		
		if (elevatorArrivedDestinationTime != 0) {
			System.out.println("It took " + (elevatorArrivedDestinationTime - elevatorRequestTime)
					+ "ns for the passenger to reach their destination from time of request.");
		}
		
		System.out.println();
	}

	/**
	 * getSourceFloor
	 * 
	 * Return the source floor value
	 * 
	 * @param  None
	 * 
	 * @return byte The source floor of the request
	 */
	public byte getSourceFloor() {
		return source;
	}

	/**
	 * getDestinationFloor
	 * 
	 * Return the destination floor value
	 * 
	 * @param  None
	 * 
	 * @return byte    The destination floor of the request
	 */
	public byte getDestinationFloor() {
		return destination;
	}

	/**
	 * getRequestDirection
	 * 
	 * Return the direction of the request
	 * 
	 * @param  None
	 * 
	 * @return ElevatorDirection   The direction of the request
	 */
	public UtilityInformation.ElevatorDirection getRequestDirection() {
		return requestDirection;
	}

	/**
	 * getElevatorPickupTimeFlag
	 * 
	 * Return the current value of the elevatorPickupTimeFlag value
	 * 
	 * @param  None
	 * 
	 * @return boolean The elevatorPickupTimeFlag value
	 */
	public boolean getElevatorPickupTimeFlag() {
		return elevatorPickupTimeFlag;
	}

	/**
	 * getElevatorArrivedDestinationTimeFlag
	 * 
	 * Return the current value of the elevatorArrivedDestinationTimeFlag
	 * 
	 * @param  None
	 * 
	 * @return boolean The elevatorArrivedDestinationTimeFlag value
	 */
	public boolean getElevatorArrivedDestinationTimeFlag() {
		return elevatorArrivedDestinationTimeFlag;
	}

	/**
	 * getElevatorPickupTime
	 * 
	 * Return the elevatorPickupTime value
	 * 
	 * @param  None
	 * 
	 * @return long    The elevatorPickupTime value
	 */
	public long getElevatorPickupTime() {
		return(elevatorPickupTime);
	}

	/**
	 * getElevatorArrivedDestinationTime
	 * 
	 * Return the getElevatorArrivedDestinationTime value
	 * 
	 * @param  None
	 * 
	 * @return long    The elevatorArrivedDestinationTime value
	 */
	public long getElevatorArrivedDestinationTime() {
		return(elevatorArrivedDestinationTime);
	}

	/**
	 * getElevatorRequestTime
	 * 
	 * Return the getElevatorRequestTime value
	 * 
	 * @param  None
	 * 
	 * @return long    The elevatorRequestTime value
	 */
	public long getElevatorRequestTime() {
		return(elevatorRequestTime);
	}
}
