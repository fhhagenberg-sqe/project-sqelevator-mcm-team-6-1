package at.fhhagenberg.sqelevator.logic;

import at.fhhagenberg.sqelevator.data.IElevatorClient;
import at.fhhagenberg.sqelevator.domain.Elevator;
import at.fhhagenberg.sqelevator.domain.ElevatorFloor;
import at.fhhagenberg.sqelevator.domain.ElevatorFloorStatus;
import at.fhhagenberg.sqelevator.domain.ElevatorStatus;

import java.rmi.RemoteException;
import java.util.LinkedList;
import java.util.List;

public class ElevatorStatusPollingService implements IElevatorStatusPollingService {

    private IElevatorClient client;
    private List<Elevator> elevators;
    private long pollingInterval;

    private Thread pollingStatusThread;

    private List<ElevatorStatusObserver> observers;

    public ElevatorStatusPollingService(IElevatorClient client, List<Elevator> elevators, long pollingInterval) {
        this.client = client;
        this.elevators = elevators;
        this.pollingInterval = pollingInterval;
        this.observers = new LinkedList<>();
    }

    public void startPollingService() {
        this.pollingStatusThread = new Thread(() -> {
            while (true) {
                for (Elevator elevator : elevators) {
                    try {
                        var elevatorStatus = pollElevatorStatus(elevator);
                        notifyAll(elevator, elevatorStatus);

                        Thread.sleep(pollingInterval);
                    } catch (InterruptedException | RemoteException e) {
                        notifyAll(elevator, ElevatorStatus.build().notConnected());
                    }
                }
            }
        });

        this.pollingStatusThread.setDaemon(true);
        this.pollingStatusThread.start();
    }

    public void stopPollingService() {
        this.pollingStatusThread.interrupt();
    }

    private ElevatorStatus pollElevatorStatus(Elevator elevator) throws RemoteException {
        var elevatorStatusBuilder = ElevatorStatus.build()
                .velocity(client.getCurrentVelocity(elevator))
                .payload(client.getCurrentWeightLoad(elevator))
                .buttonStatus(client.getElevatorFloorButtonsStatus(elevator))
                .currentFloor(client.getCurrentFloor(elevator))
                .elevatorFloorStatus(pollElevatorFloorStatuses(elevator))
                .direction(client.getDirection(elevator))
                .doorStatus(client.getElevatorDoorStatus(elevator));

        client.getTargetedFloor(elevator).ifPresent(elevatorStatusBuilder::targetedFloor);

        return elevatorStatusBuilder.get();
    }

    private ElevatorFloorStatus[] pollElevatorFloorStatuses(Elevator elevator) throws RemoteException {
        var elevatorFloorStatuses = new LinkedList<ElevatorFloorStatus>();

        for (ElevatorFloor elevatorFloor : elevator.getElevatorFloors()) {
            elevatorFloorStatuses.add(pollElevatorFloorStatus(elevator, elevatorFloor));
        }

        return elevatorFloorStatuses.toArray(ElevatorFloorStatus[]::new);
    }

    private ElevatorFloorStatus pollElevatorFloorStatus(Elevator elevator, ElevatorFloor elevatorFloor) throws RemoteException {
        var floor = elevatorFloor.getFloor();

        return ElevatorFloorStatus.build()
                .upRequested(client.hasFloorBeenRequestedUp(floor))
                .downRequested(client.hasFloorBeenRequestedDown(floor))
                .serviced(client.isServiceEnabled(elevator, floor))
                .get();
    }

    @Override
    public void addObserver(ElevatorStatusObserver observer) {
        this.observers.add(observer);
    }

    @Override
    public void notifyAll(Elevator elevator, ElevatorStatus elevatorStatus) {
        this.observers.forEach(observer -> observer.update(elevator, elevatorStatus));
    }
}