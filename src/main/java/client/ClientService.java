package client;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import common.dto.CaThiHistoryDTO;
import common.dto.GiamSatViewDTO;
import common.dto.PhanCongViewDTO;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.reflect.Type;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Enumeration;
import java.util.List;

public class ClientService {

    private static final int DISCOVERY_PORT = 8889;
    private static final String DISCOVERY_REQUEST = "DISCOVER_THLTM_SERVER";
    private static final String DISCOVERY_RESPONSE_PREFIX = "THLTM_SERVER|";
    private static final int DISCOVERY_TIMEOUT_MS = 1200;

    private volatile String host;
    private int port;
    private final Gson gson = new Gson();
    private volatile long lastDiscoveryAt;

    public ClientService(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String sendCommand(String command) throws Exception {
        Exception firstError = null;
        for (int attempt = 0; attempt < 2; attempt++) {
            String targetHost = resolveHost(attempt > 0);
            try (
                    Socket socket = new Socket()
            ) {
                socket.connect(new InetSocketAddress(targetHost, port), 2000);
                try (
                        PrintWriter out = new PrintWriter(
                                new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8),
                                true
                        );
                        BufferedReader in = new BufferedReader(
                                new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8)
                        )
                ) {
                    out.println(command);

                    String response = in.readLine();
                    if (response == null) {
                        return "ERROR|Server khong phan hoi";
                    }
                    return response;
                }
            } catch (Exception e) {
                if (firstError == null) {
                    firstError = e;
                }
            }
        }

        throw firstError == null ? new RuntimeException("Khong ket noi duoc server.") : firstError;
    }

    public String ping() throws Exception {
        return sendCommand("PING");
    }

    public String importAll(String excelPath) throws Exception {
        return sendCommand("IMPORT_ALL|" + excelPath);
    }

    public String phanCong(int thuTuCa, Integer soCanBoCoiThi, Integer soPhongThi) throws Exception {
        String canBo = soCanBoCoiThi == null ? "" : soCanBoCoiThi.toString();
        String phong = soPhongThi == null ? "" : soPhongThi.toString();
        return sendCommand("PHAN_CONG|" + thuTuCa + "|" + canBo + "|" + phong);
    }

    // Backward-compatible overload for older UI calls.
    public String phanCong(int thuTuCa) throws Exception {
        return phanCong(thuTuCa, null, null);
    }

    public String xuatExcel(int thuTuCa, String outputDir, String ngayThi, String logoPath) throws Exception {
        String safeLogoPath = logoPath == null ? "" : logoPath;
        return sendCommand("XUAT_EXCEL|" + thuTuCa + "|" + outputDir + "|" + ngayThi + "|" + safeLogoPath);
    }

    // Backward-compatible overload for older UI calls.
    public String xuatExcel(int thuTuCa, String outputDir) throws Exception {
        String ngayThi = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        return xuatExcel(thuTuCa, outputDir, ngayThi, "");
    }

    public List<PhanCongViewDTO> getPhanCong(int thuTuCa) throws Exception {
        String response = sendCommand("GET_PHAN_CONG|" + thuTuCa);

        if (!response.startsWith("OK|")) {
            throw new RuntimeException(response);
        }

        String json = response.substring("OK|".length());
        Type type = new TypeToken<List<PhanCongViewDTO>>() { }.getType();
        return gson.fromJson(json, type);
    }

    public List<GiamSatViewDTO> getGiamSat(int thuTuCa) throws Exception {
        String response = sendCommand("GET_GIAM_SAT|" + thuTuCa);

        if (!response.startsWith("OK|")) {
            throw new RuntimeException(response);
        }

        String json = response.substring("OK|".length());
        Type type = new TypeToken<List<GiamSatViewDTO>>() { }.getType();
        return gson.fromJson(json, type);
    }

    public List<CaThiHistoryDTO> getLichSuCa() throws Exception {
        String response = sendCommand("GET_LICH_SU_CA");

        if (!response.startsWith("OK|")) {
            throw new RuntimeException(response);
        }

        String json = response.substring("OK|".length());
        Type type = new TypeToken<List<CaThiHistoryDTO>>() { }.getType();
        return gson.fromJson(json, type);
    }

    private String resolveHost(boolean forceRefresh) throws Exception {
        if (forceRefresh || host == null || host.trim().isEmpty() || isDiscoveryStale()) {
            String discovered = discoverServerHost();
            if (discovered != null && !discovered.isBlank()) {
                host = discovered;
                lastDiscoveryAt = System.currentTimeMillis();
            } else if (host == null || host.trim().isEmpty()) {
                host = "localhost";
            }
        }
        return host;
    }

    private boolean isDiscoveryStale() {
        return System.currentTimeMillis() - lastDiscoveryAt > 60_000;
    }

    private String discoverServerHost() {
        byte[] reqBytes = DISCOVERY_REQUEST.getBytes(StandardCharsets.UTF_8);

        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setBroadcast(true);
            socket.setSoTimeout(DISCOVERY_TIMEOUT_MS);

            // Broadcast to generic broadcast address first.
            DatagramPacket generic = new DatagramPacket(
                    reqBytes,
                    reqBytes.length,
                    InetAddress.getByName("255.255.255.255"),
                    DISCOVERY_PORT
            );
            socket.send(generic);

            // Broadcast to each network interface broadcast address.
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                if (!ni.isUp() || ni.isLoopback()) {
                    continue;
                }
                for (var addr : ni.getInterfaceAddresses()) {
                    InetAddress broadcast = addr.getBroadcast();
                    if (broadcast == null) {
                        continue;
                    }
                    DatagramPacket p = new DatagramPacket(reqBytes, reqBytes.length, broadcast, DISCOVERY_PORT);
                    socket.send(p);
                }
            }

            byte[] respBuf = new byte[256];
            DatagramPacket response = new DatagramPacket(respBuf, respBuf.length);
            socket.receive(response);

            String payload = new String(
                    response.getData(),
                    response.getOffset(),
                    response.getLength(),
                    StandardCharsets.UTF_8
            ).trim();

            if (payload.startsWith(DISCOVERY_RESPONSE_PREFIX)) {
                String serverPort = payload.substring(DISCOVERY_RESPONSE_PREFIX.length()).trim();
                try {
                    int discoveredPort = Integer.parseInt(serverPort);
                    if (discoveredPort > 0 && discoveredPort <= 65535) {
                        this.port = discoveredPort;
                    }
                } catch (Exception ignored) {
                    // Keep current port if parse fails.
                }
                return response.getAddress().getHostAddress();
            }
        } catch (SocketTimeoutException timeout) {
            // no discovery response in timeout
        } catch (Exception ignored) {
            // fall through and use fallback host
        }
        return host;
    }
}
