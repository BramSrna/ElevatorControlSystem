
public class Floor {
    private FloorSubsystem controller;
    
    private int floorNum;
    private int numElevatorShafts;    
    
    private enum lampState {
        OFF,
        ON
    }
    
    private lampState arrivalLamp;
    private FloorSubsystem.Direction arrivalLampDir;
    
    private lampState upButtonPressed;
    private lampState downButtonPressed;
    
    public Floor(FloorSubsystem controller, 
                 int floorNum, 
                 int numElevatorShafts) {
        this.controller = controller;
        
        this.floorNum = floorNum;
        this.numElevatorShafts = numElevatorShafts;
        
        arrivalLamp = lampState.OFF;
        arrivalLampDir = FloorSubsystem.Direction.UP;
        
        upButtonPressed = lampState.OFF;
        downButtonPressed = lampState.OFF;
    }
    
    public void elevatorArriving(int elevatorShaftNum, FloorSubsystem.Direction direction) {
        arrivalLamp = lampState.ON;
        arrivalLampDir = direction;
        
        if ((direction == FloorSubsystem.Direction.UP) && (upButtonPressed == lampState.ON)) {
            upButtonPressed = lampState.OFF;
        } else if ((direction == FloorSubsystem.Direction.DOWN) && (downButtonPressed == lampState.ON)) {
            downButtonPressed = lampState.OFF;
        }
        
        controller.sendArrivalSensorSignal(this.floorNum, elevatorShaftNum);
    }
    
    public void elevatorRequest(FloorSubsystem.Direction direction) {
        if (direction == FloorSubsystem.Direction.UP) {
            upButtonPressed = lampState.ON;
        } else if (direction == FloorSubsystem.Direction.DOWN) {
            downButtonPressed = lampState.ON;
        }
        
        controller.addRequest(0, 0, 0, 0, this.floorNum, direction, 0);
    }
}
