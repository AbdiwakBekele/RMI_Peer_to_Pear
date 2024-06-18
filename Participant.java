import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;
import java.util.HashSet;
import java.util.stream.Collectors;

public class Participant extends UnicastRemoteObject implements ParticipantInterface {
    private String name;
    private BootstrapNodeInterface bootstrapNode;
    private Map<String, ParticipantInterface> peers;
    private Set<String> infectedParticipants;
    private boolean isInfected;

    protected Participant(String name, BootstrapNodeInterface bootstrapNode, boolean isBootstrapNode)
            throws RemoteException {
        super();
        this.name = name;
        this.bootstrapNode = bootstrapNode;
        this.peers = new HashMap<>();
        this.infectedParticipants = new HashSet<>();
        this.isInfected = false;
        bootstrapNode.registerParticipant(name, this);
        discoverPeers();
    }

    @Override
    public void notifyInfection() throws RemoteException {

        int symptoms = 0;
        String[] questions = {
                "Have you experienced a fever in the last 48 hours?",
                "Have you had any respiratory issues or coughing?",
                "Have you been in contact with a confirmed COVID-19 case?"
        };

        int numberOfQuestions = questions.length;

        Random random = new Random();
        for (String question : questions) {
            // Generate random yes or no response
            String answer = random.nextBoolean() ? "yes" : "no";
            System.out.println(question + " (yes/no) - Randomly answered: " + answer);
            if ("yes".equals(answer)) {
                symptoms++;
            }
        }

        if (symptoms >= 2) {
            System.out.println("You are infected with covid: " + symptoms + "/" + numberOfQuestions + " Symptoms");
            this.isInfected = true;
            for (ParticipantInterface peer : peers.values()) {
                peer.receiveNotification(name, symptoms, numberOfQuestions);
            }
        } else {
            System.out.println("You are NOT infected with covid: " + symptoms + "/" + numberOfQuestions + " Symptoms");
        }
    }

    @Override
    public void receiveNotification(String participantName, int symptoms, int numberOfQuestions)
            throws RemoteException {
        infectedParticipants.add(participantName);
        System.out.println(
                participantName + " is infected with covid: " + symptoms + "/" + numberOfQuestions + " Symptoms");
    }

    @Override
    public void addPeer(String participantName, ParticipantInterface participant) throws RemoteException {
        peers.put(participantName, participant);
    }

    @Override
    public void removePeer(String participantName) throws RemoteException {
        peers.remove(participantName);
    }

    @Override
    public String getName() throws RemoteException {
        return name;
    }

    @Override
    public List<String> getActiveParticipants() throws RemoteException {
        return peers.keySet().stream().collect(Collectors.toList());
    }

    public List<String> getInfectedParticipants() throws RemoteException {
        return infectedParticipants.stream().collect(Collectors.toList());
    }

    private void recover() throws RemoteException {
        if (isInfected) {
            this.isInfected = false;
            System.out.println("You have recovered.");
            for (ParticipantInterface peer : peers.values()) {
                peer.receiveRecovery(name);
            }
        } else {
            System.out.println("You are not infected.");
        }
    }

    @Override
    public void receiveRecovery(String participantName) throws RemoteException {
        infectedParticipants.remove(participantName);
        System.out.println(participantName + " has recovered.");
    }

    private void discoverPeers() throws RemoteException {
        Map<String, ParticipantInterface> participants = bootstrapNode.getParticipants();
        for (Map.Entry<String, ParticipantInterface> entry : participants.entrySet()) {
            if (!entry.getKey().equals(name)) {
                peers.put(entry.getKey(), entry.getValue());
                entry.getValue().addPeer(name, this);
            }
        }
    }

    private void leaveNetwork() throws RemoteException {
        bootstrapNode.deregisterParticipant(name);
        for (ParticipantInterface peer : peers.values()) {
            peer.removePeer(name);
        }
        System.out.println(name + " has left the network.");
    }

    private static String getLocalIPAddress() throws SocketException {
        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
        while (interfaces.hasMoreElements()) {
            NetworkInterface networkInterface = interfaces.nextElement();
            if (networkInterface.isLoopback() || !networkInterface.isUp()) {
                continue;
            }

            Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
            while (addresses.hasMoreElements()) {
                InetAddress address = addresses.nextElement();
                if (address.isSiteLocalAddress()) {
                    return address.getHostAddress();
                }
            }
        }
        return null;
    }

    public static void main(String[] args) {
        try {
            // Start RMI registry
            try {
                LocateRegistry.createRegistry(Registry.REGISTRY_PORT);
                System.out.println("RMI registry started.");
            } catch (RemoteException e) {
                System.out.println("RMI registry already running.");
            }

            String name = "Participant" + new Random().nextInt(100);

            // BootstrapNodeInterface bootstrapNode;
            boolean isBootstrapNode = true;

            List<String> bootstrapNodeIP = NetworkUtils.findBootstrapNode(Registry.REGISTRY_PORT, 2000);
            String localhostIP = getLocalIPAddress();

            System.out.println("Node IP: " + bootstrapNodeIP);
            System.out.println("Localhost IP: " + localhostIP);

            // Node is available
            if (bootstrapNodeIP == null) {
                isBootstrapNode = false;
            }

            BootstrapNodeInterface bootstrapNode = null;

            if (!isBootstrapNode) {

                bootstrapNode = new BootstrapNode();

                // String localhostIP_ = getLocalIPAddress();
                System.out.println(localhostIP);
                if (localhostIP != null) {
                    Naming.rebind("//" + localhostIP + "/BootstrapNode", bootstrapNode);
                    System.out.println("Bootstrap Node is running at " + localhostIP);
                } else {
                    System.err.println("Failed to find a suitable IP address.");
                }

                System.out.println("Bootstrap Node is running.");

            } else {
                // Node is available
                System.out.println("testing Bind");

                for (String ip : bootstrapNodeIP) {
                    try {
                        bootstrapNode = (BootstrapNodeInterface) Naming.lookup("//" + ip + "/BootstrapNode");
                        System.out.println("Successfully Bind at: " + ip);
                        // If lookup is successful, break the loop
                        break;
                    } catch (Exception e) {
                        System.err.println("Failed to lookup BootstrapNode at IP: " + ip);
                        // e.printStackTrace();
                    }
                }

                // bootstrapNode = (BootstrapNodeInterface) Naming.lookup("//" + bootstrapNodeIP
                // + "/BootstrapNode");
            }

            Participant participant = new Participant(name, bootstrapNode, isBootstrapNode);
            System.out.println(name + " joined the network.");

            Scanner scanner = new Scanner(System.in);
            boolean running = true;

            while (running) {
                System.out.println("Options: ");
                System.out.println("1. Notify infection");
                System.out.println("2. Leave network");
                System.out.println("3. List active participants");
                System.out.println("4. List infected participants");
                System.out.println("5. Recover from infection");
                System.out.println("6. Exit");

                int choice = scanner.nextInt();
                scanner.nextLine();

                switch (choice) {
                    case 1:
                        participant.notifyInfection();
                        break;
                    case 2:
                        if (isBootstrapNode) {
                            System.out.println("The first participant (bootstrap node) cannot leave the network.");
                        } else {
                            participant.leaveNetwork();
                            running = false;
                        }
                        break;
                    case 3:
                        List<String> activeParticipants = participant.getActiveParticipants();
                        System.out.println("Active participants: " + activeParticipants);
                        break;
                    case 4:
                        List<String> infectedParticipants = participant.getInfectedParticipants();
                        System.out.println("Infected participants: " + infectedParticipants);
                        break;
                    case 5:
                        participant.recover();
                        break;
                    case 6:
                        running = false;
                        break;
                    default:
                        System.out.println("Invalid choice. Please try again.");
                }
            }

            scanner.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
