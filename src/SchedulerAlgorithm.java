import java.util.ArrayList;

public class SchedulerAlgorithm {

	private ArrayList<ArrayList<Byte>> elevatorStops; // Index: Elevators, ArrayList: Elevator stops
	private ArrayList<Byte> currentFloor; // Index: Elevators, Byte: Current floor
	private ArrayList<Boolean> stopElevator; // Index: Elevators, Boolean: Should stop elevator
	private ArrayList<ArrayList<Byte>> elevatorDestinations; // Index: Elevators, ArrayList: Elevator destinations
	private ArrayList<Boolean> elevatorUsable;

	public SchedulerAlgorithm(byte numElevators) {
		elevatorStops = new ArrayList<ArrayList<Byte>>();
		currentFloor = new ArrayList<Byte>();
		stopElevator = new ArrayList<Boolean>();
		elevatorDestinations = new ArrayList<ArrayList<Byte>>();
		elevatorUsable = new ArrayList<Boolean>();

		setNumberOfElevators(numElevators);
	}

	/**
	 * Called when someone on the floor has requested an elevator
	 * 
	 * @param source
	 * @param destination
	 * @param upOrDown
	 * @return
	 */
	public byte elevatorRequestMade(Byte source, Byte destination, UtilityInformation.ElevatorDirection upOrDown) {
		System.out.println("Elevator was requested at: " + source + " in the direction " + upOrDown
				+ " with destination " + destination);

		byte elevatorNum = determineElevatorToGiveRequest(source);

		if (!elevatorDestinations.get(elevatorNum).contains(destination)) {
			elevatorDestinations.get(elevatorNum).add(destination);
		}

		byte index = addStopToElevator(elevatorNum, source, (byte) 0);
		addStopToElevator(elevatorNum, destination, index);

		return (elevatorNum);
	}

	private byte determineElevatorToGiveRequest(byte startFloor) {
		ArrayList<Integer> closestElevator = new ArrayList<Integer>();
		int closestDiff = -1;
		int chosenElevator = -1;
		int currDiff;

		// Add destination to closest elevator
		for (int i = 0; i < currentFloor.size(); i++) {
			currDiff = Math.abs(currentFloor.get(i) - startFloor);
			if (elevatorUsable.get(i) && ((closestDiff == -1) || (currDiff <= closestDiff))) { // same as below
				closestElevator.add(i);
				closestDiff = currDiff;
			}
		}

		// Break ties by prioritizing elevators above the start floor
		if (closestElevator.size() > 1) {
			ArrayList<Integer> indicesToRemove = new ArrayList<Integer>();

			for (int i = 0; i < closestElevator.size(); i++) {
				if (currentFloor.get(closestElevator.get(i)) < startFloor) {
					indicesToRemove.add(i);
				}
			}

			if (indicesToRemove.size() < closestElevator.size()) {
				for (int index : indicesToRemove) {
					closestElevator.set(index, null);
				}
			}

			while (closestElevator.remove(null)) {
			}
			;

			// Break ties by prioritizing shortest queues
			int shortestQueue = -1;
			int currQueueSize;

			if (closestElevator.size() > 1) {
				for (int i = 0; i < closestElevator.size(); i++) {
					currQueueSize = elevatorStops.get(closestElevator.get(i)).size();
					if ((shortestQueue == -1) || (currQueueSize < shortestQueue)) {
						chosenElevator = closestElevator.get(i);
						shortestQueue = currQueueSize;

					}
				}
			} else {
				chosenElevator = closestElevator.get(0);
			}
		} else {
			chosenElevator = closestElevator.get(0);
		}

		return ((byte) chosenElevator);
	}

	private byte addStopToElevator(byte elevatorNum, byte destFloor, byte minInd) {
		byte endInd = 0;
		int closestDiff = Integer.MAX_VALUE;
		int currDiff;
		byte closestInd = 0;

		ArrayList<Byte> currDests = elevatorStops.get(elevatorNum);

		if (currDests.size() == 0) {
			currDests.add(destFloor);
		} else {
			if (!(currDests.contains(destFloor))) {
				endInd = -1;
				for (byte i = minInd; i < currDests.size(); i++) {
					if (i != currDests.size() - 1) {
						if (((currDests.get(i) < destFloor) && (currDests.get(i + 1) > destFloor))
								|| ((currDests.get(i) > destFloor) && (currDests.get(i + 1) < destFloor))) {
							endInd = (byte) (i + 1);
						}
					}

					currDiff = Math.abs(currDests.get(i) - destFloor);
					if (currDiff < closestDiff) {
						closestDiff = currDiff;
						closestInd = i;
					}

				}

				if (endInd == -1) {
					endInd = (byte) (closestInd + 1);
				}

				currDests.add(endInd, destFloor);
			}
		}

		System.out.println("New request list: " + elevatorStops + "\n");

		return (endInd);
	}

	/**
	 * Scheduler has been informed where the elevator is. Update the stopElevator
	 * and currentFloor ArrayList. Remove the current floor from destinations.
	 * 
	 * @param floorNum
	 * @param elevatorNum
	 */
	public void elevatorHasReachedFloor(Byte floorNum, Byte elevatorNum) {
		System.out.println("Elevator " + elevatorNum + " has reached floor: " + floorNum);

		if ((elevatorStops.get(elevatorNum).size() > 0) && (elevatorStops.get(elevatorNum).get(0) == floorNum)) {
			System.out.println("Current floor is a destination.");
			stopElevator.set(elevatorNum, true);
			removeFloorFromDestinations(floorNum, elevatorNum);
		} else {
			stopElevator.set(elevatorNum, false);
		}

		currentFloor.set(elevatorNum, floorNum);
	}

	/**
	 * Remove a floor from an elevator's destinations
	 * 
	 * @param currentFloor
	 * @param currentElevator
	 */
	private void removeFloorFromDestinations(byte currentFloor, byte currentElevator) {
		if (elevatorStops.get(currentElevator).contains(currentFloor)) {
			int indToRemove = elevatorStops.get(currentElevator).indexOf(currentFloor);
			if (indToRemove != -1) {
				elevatorStops.get(currentElevator).remove(indToRemove);
			}

			indToRemove = elevatorDestinations.get(currentElevator).indexOf(currentFloor);

			if (indToRemove != -1) {
				elevatorDestinations.get(currentElevator).remove(indToRemove);
			}

			System.out.println(currentFloor + " was removed from the destinations list");
			System.out.println("New destination list: " + elevatorStops.toString() + "\n");
		}
	}

	/**
	 * Determine the direction the given elevator should travel
	 * 
	 * @param elevatorNum
	 * @return
	 */
	public UtilityInformation.ElevatorDirection whatDirectionShouldTravel(byte elevatorNum) {
		if (elevatorStops.get(elevatorNum).size() != 0) {
			int nextFloor = elevatorStops.get(elevatorNum).get(0);

			if (nextFloor > currentFloor.get(elevatorNum)) {
				return (UtilityInformation.ElevatorDirection.UP);
			} else if (nextFloor < currentFloor.get(elevatorNum)) {
				return (UtilityInformation.ElevatorDirection.DOWN);
			} else {
				return (UtilityInformation.ElevatorDirection.STATIONARY);
			}
		} else {
			return (UtilityInformation.ElevatorDirection.STATIONARY);
		}
	}

	/**
	 * Determine if a given elevator has anywhere to go
	 * 
	 * @param elevatorNum
	 * @return
	 */
	public boolean somewhereToGo(byte elevatorNum) {
		return (!elevatorStops.get(elevatorNum).isEmpty());
	}

	/**
	 * Set the number of elevators from the given schematics. Updates all the
	 * ArrayLists that need to be given the correct number of elevators to be
	 * initialized
	 * 
	 * @param numElevators
	 */
	public void setNumberOfElevators(byte numElevators) {
		while (stopElevator.size() > numElevators) {
			elevatorStops.remove(elevatorStops.size() - 1);
			currentFloor.remove(currentFloor.size() - 1);
			stopElevator.remove(stopElevator.size() - 1);
			elevatorDestinations.remove(elevatorDestinations.size() - 1);
			elevatorUsable.remove(elevatorUsable.size() - 1);
		}

		while (stopElevator.size() < numElevators) {
			currentFloor.add((byte) 0);
			stopElevator.add(true);
			elevatorUsable.add(true);

			ArrayList<Byte> temp = new ArrayList<Byte>();
			temp.add((byte) 0);

			elevatorStops.add(temp);

			temp = new ArrayList<Byte>();
			temp.add((byte) 0);

			elevatorDestinations.add(temp);
		}
	}

	/**
	 * Get an elevators destinations
	 * 
	 * @param elevatorNum
	 * @return
	 */
	public ArrayList<Byte> getDestinations(byte elevatorNum) {
		return elevatorDestinations.get(elevatorNum);
	}

	/**
	 * Get the current floor of an elevator
	 * 
	 * @param elevatorNum
	 * @return
	 */
	public byte getCurrentFloor(byte elevatorNum) {
		return currentFloor.get(elevatorNum);
	}

	/**
	 * Determine if the current elevator should stop
	 * 
	 * @param elevatorNum
	 * @return
	 */
	public boolean getStopElevator(byte elevatorNum) {
		return stopElevator.get(elevatorNum);
	}

	/**
	 * Set the current elevator to stop
	 * 
	 * @param elevatorNum
	 * @param newVal
	 */
	public void setStopElevator(byte elevatorNum, boolean newVal) {
		stopElevator.set(elevatorNum, newVal);
	}

	/**
	 * Print all the information in the lists (for testing purposes)
	 */
	public void printAllInfo() {
		System.out.println(elevatorStops);
		System.out.println(currentFloor);
		System.out.println(stopElevator);
		System.out.println(elevatorDestinations);
	}

	public void stopUsingElevator(byte elevatorNum) {
		int ind = 0;
		int shortestQueue = -1;
		int shortestQueueSize = -1;

		while (ind < elevatorStops.size()) {
			if ((ind != elevatorNum)
					&& ((shortestQueueSize == -1) || (shortestQueueSize < elevatorStops.get(ind).size()))) {
				shortestQueueSize = elevatorStops.get(ind).size();
				shortestQueue = ind;
			}
			ind++;
		}

		ArrayList<Byte> currStops = elevatorStops.get(elevatorNum);
		elevatorStops.get(shortestQueue).addAll(currStops);

		byte currFloor = currentFloor.get(elevatorNum);

		elevatorStops.set(elevatorNum, new ArrayList<Byte>());
		elevatorStops.get(elevatorNum).add(currFloor);

		elevatorDestinations.set(elevatorNum, new ArrayList<Byte>());
		elevatorDestinations.get(elevatorNum).add(currFloor);

		pauseElevator(elevatorNum);
	}

	public void pauseElevator(byte elevatorNum) {
		elevatorUsable.set(elevatorNum, false);

		stopElevator.set(elevatorNum, true);
	}

	public void resumeUsingElevator(byte elevatorNum) {
		elevatorUsable.set(elevatorNum, true);
	}
}
