import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Map;

public class BootstrapNode extends UnicastRemoteObject implements BootstrapNodeInterface {
    private Map<String, ParticipantInterface> participants;

    protected BootstrapNode() throws RemoteException {
        super();
        participants = new HashMap<>();
    }

    @Override
    public void registerParticipant(String name, ParticipantInterface participant) throws RemoteException {
        participants.put(name, participant);
        System.out.println(name + " has registered.");
    }

    @Override
    public void deregisterParticipant(String name) throws RemoteException {
        participants.remove(name);
        System.out.println(name + " has deregistered.");
    }

    @Override
    public Map<String, ParticipantInterface> getParticipants() throws RemoteException {
        return participants;
    }
}
