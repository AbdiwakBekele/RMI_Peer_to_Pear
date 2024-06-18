import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Map;

public interface BootstrapNodeInterface extends Remote {
    void registerParticipant(String name, ParticipantInterface participant) throws RemoteException;
    void deregisterParticipant(String name) throws RemoteException;
    Map<String, ParticipantInterface> getParticipants() throws RemoteException;
}
