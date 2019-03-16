
public class Request {

	private long elevatorRequestTime;
	private long elevatorPickupTime;
	private long elevatorArrivedDestinationTime;
	private boolean elevatorPickupTimeFlag;
	private boolean elevatorArrivedDestinationTimeFlag;
	private byte source;
	private byte destination;
	private UtilityInformation.ElevatorDirection requestDirection;

	public Request(long requestArrived, byte source, byte destination,
			UtilityInformation.ElevatorDirection requestDirection) {
		elevatorRequestTime = requestArrived;
		this.source = source;
		this.destination = destination;
		this.requestDirection = requestDirection;
		elevatorPickupTime = 0;
		elevatorArrivedDestinationTime = 0;
		elevatorPickupTimeFlag = false;
		elevatorArrivedDestinationTimeFlag = false;
		// printRequestDetails();
	}

	public void setElevatorPickupTimeFlag() {
		elevatorPickupTimeFlag = true;
	}

	public void setElevatorArrivedDestinationTimeFlag() {
		elevatorArrivedDestinationTimeFlag = true;
	}

	public void setElevatorPickupTime(long time) {
		elevatorPickupTime = time;
		elevatorPickupTimeFlag = false;
		printRequestDetails();
	}

	public void setElevatorArrivedDestinationTime(long time) {
		elevatorArrivedDestinationTime = time;
		elevatorArrivedDestinationTimeFlag = false;
		printRequestDetails();
	}

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

	public byte getSourceFloor() {
		return source;
	}

	public byte getDestinationFloor() {
		return destination;
	}

	public UtilityInformation.ElevatorDirection getRequestDirection() {
		return requestDirection;
	}

	public boolean getElevatorPickupTimeFlag() {
		return elevatorPickupTimeFlag;
	}

	public boolean getElevatorArrivedDestinationTimeFlag() {
		return elevatorArrivedDestinationTimeFlag;
	}

}
