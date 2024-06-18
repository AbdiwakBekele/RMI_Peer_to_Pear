import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface ParticipantInterface extends Remote {
    void notifyInfection() throws RemoteException;
    void receiveNotification(String participantName, int symptoms, int numberOfQuestions) throws RemoteException;
    void addPeer(String participantName, ParticipantInterface participant) throws RemoteException;
    void removePeer(String participantName) throws RemoteException;
    void receiveRecovery(String participantName) throws RemoteException;
    String getName() throws RemoteException;
    List<String> getActiveParticipants() throws RemoteException;
}
