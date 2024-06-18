import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class NetworkUtils {
    public static boolean isPortOpen(String ip, int port, int timeout) {
        try (Socket socket = new Socket()) {
            socket.connect(new java.net.InetSocketAddress(ip, port), timeout);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public static List<String> findBootstrapNode(int port, int timeout) throws SocketException {
        String localIp = getLocalIp();
        if (localIp == null) {
            return null;
        }

        String subnet = localIp.substring(0, localIp.lastIndexOf("."));
        List<String> availableIPs = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {

            String ip = subnet + "." + i;
            // Skip the current IP
            if (ip.equals(localIp)) {
                continue;
            }
            if (isPortOpen(ip, port, timeout)) {
                availableIPs.add(ip);
            }
        }

        if (availableIPs.size() != 0) {
            return availableIPs;
        } else {
            return null;
        }
    }

    // private static String getLocalIp() {
    // try {

    // return InetAddress.getLocalHost().getHostAddress();
    // } catch (IOException e) {
    // e.printStackTrace();
    // return null;
    // }
    // }

    private static String getLocalIp() throws SocketException {
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

}
