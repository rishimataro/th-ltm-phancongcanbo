package server;

import com.google.gson.Gson;
import server.config.DatabaseConfig;
import server.dao.KetQuaDAO;
import server.excel.ExcelExportService;
import server.excel.ExcelImportService;
import server.service.PhanCongService;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.SocketException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.function.Consumer;

public class ServerApp {
    private static final DateTimeFormatter INPUT_DATE_FORMAT = DateTimeFormatter.ofPattern("d/M/yyyy");
    private static final int DISCOVERY_PORT = 8889;
    private static final String DISCOVERY_REQUEST = "DISCOVER_THLTM_SERVER";
    private static final String DISCOVERY_RESPONSE_PREFIX = "THLTM_SERVER|";

    private volatile boolean running;
    private ServerSocket serverSocket;
    private DatagramSocket discoverySocket;
    private Thread discoveryThread;
    private volatile int tcpPort;
    private final Consumer<String> logger;

    public ServerApp(Consumer<String> logger) {
        this.logger = logger;
    }

    public synchronized void start(int port) throws Exception {
        if (running) {
            throw new IllegalStateException("Server dang chay.");
        }

        serverSocket = new ServerSocket(port);
        running = true;
        tcpPort = port;
        log("Server dang chay o cong " + port);
        startDiscoveryResponder();

        Thread acceptThread = new Thread(this::acceptLoop, "server-accept-thread");
        acceptThread.setDaemon(true);
        acceptThread.start();
    }

    public synchronized void stop() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            stopDiscoveryResponder();
            log("Server da dung.");
        } catch (Exception e) {
            log("Loi khi dung server: " + e.getMessage());
        }
    }

    public boolean isRunning() {
        return running;
    }

    private void acceptLoop() {
        while (running) {
            try {
                Socket socket = serverSocket.accept();
                Thread t = new Thread(() -> handleClient(socket), "server-client-thread");
                t.setDaemon(true);
                t.start();
            } catch (Exception e) {
                if (running) {
                    log("Loi accept client: " + e.getMessage());
                }
            }
        }
    }

    private void handleClient(Socket socket) {
        try (
                Socket s = socket;
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8)
                );
                PrintWriter out = new PrintWriter(
                        new OutputStreamWriter(s.getOutputStream(), StandardCharsets.UTF_8),
                        true
                )
        ) {
            String command = in.readLine();

            if (command == null || command.trim().isEmpty()) {
                out.println("ERROR|Khong nhan duoc lenh tu client");
                return;
            }

            log("Client gui lenh: " + command);

            try {
                long t0 = System.nanoTime();
                String response = processCommand(command);
                long elapsed = System.nanoTime() - t0;
                out.println(response);
                log("Phan hoi: " + response);
                log("[TIME] command=" + firstToken(command) + " took " + formatDuration(elapsed));
            } catch (Exception e) {
                e.printStackTrace();
                out.println("ERROR|" + cleanMessage(e.getMessage()));
                log("Loi xu ly lenh: " + e.getMessage());
            }

        } catch (Exception e) {
            log("Loi client socket: " + e.getMessage());
        }
    }

    private String processCommand(String command) throws Exception {
        if (command.equals("PING")) {
            return "OK|Server dang hoat dong";
        }

        if (command.startsWith("IMPORT_ALL|")) {
            String excelPath = command.substring("IMPORT_ALL|".length());
            validateExcelPath(excelPath);
            ExcelImportService service = new ExcelImportService();
            int soCanBo = service.importCanBo(excelPath);
            int soPhongThi = service.importPhongThi(excelPath);
            return "OK|Import thanh cong: " + soCanBo + " can bo, " + soPhongThi + " phong thi";
        }

        if (command.startsWith("IMPORT_CANBO|")) {
            String excelPath = command.substring("IMPORT_CANBO|".length());
            validateExcelPath(excelPath);
            ExcelImportService service = new ExcelImportService();
            int soCanBo = service.importCanBo(excelPath);
            return "OK|Import can bo thanh cong: " + soCanBo;
        }

        if (command.startsWith("IMPORT_PHONG|")) {
            String excelPath = command.substring("IMPORT_PHONG|".length());
            validateExcelPath(excelPath);
            ExcelImportService service = new ExcelImportService();
            int soPhongThi = service.importPhongThi(excelPath);
            return "OK|Import phong thi thanh cong: " + soPhongThi;
        }

        if (command.startsWith("PHAN_CONG|")) {
            String[] parts = command.split("\\|");
            if (parts.length < 4) {
                return "ERROR|Cu phap dung: PHAN_CONG|thuTuCa|soCanBoCoiThi|soPhongThi";
            }

            int thuTuCa = parseInt(parts[1], "Thu tu ca thi phai la so nguyen duong.");
            int soCanBo = parseRequiredPositiveInt(parts[2], "So can bo coi thi");
            int soPhong = parseRequiredPositiveInt(parts[3], "So phong thi");
            validatePhanCongInput(soCanBo, soPhong);

            PhanCongService service = new PhanCongService();
            service.phanCongCa(thuTuCa, soCanBo, soPhong);
            return "OK|Phan cong ca " + thuTuCa + " thanh cong";
        }

        if (command.startsWith("XUAT_EXCEL|")) {
            String[] parts = command.split("\\|", 5);
            if (parts.length < 4) {
                return "ERROR|Cu phap dung: XUAT_EXCEL|thuTuCa|outputDir|ngayThi|logoPath(tuy_chon)";
            }

            int thuTuCa = parseInt(parts[1], "Thu tu ca thi phai la so nguyen duong.");
            String outputDir = parts[2];
            String ngayThi = parts[3];
            String logoPath = parts.length >= 5 ? parts[4] : "";
            validateOutputDir(outputDir);
            validateNgayThi(ngayThi);

            ExcelExportService service = new ExcelExportService();
            Path fileTongHop = service.exportBaoCaoCaThi(thuTuCa, outputDir, ngayThi, logoPath);
            Path fileGiamSat = service.exportBaoCaoGiamSat(thuTuCa, outputDir, ngayThi, logoPath);

            return "OK|Xuat Excel thanh cong: " + fileTongHop + " ; " + fileGiamSat;
        }

        if (command.startsWith("GET_PHAN_CONG|")) {
            String[] parts = command.split("\\|");
            if (parts.length < 2) {
                return "ERROR|Thieu thu tu ca thi";
            }

            int thuTuCa = parseInt(parts[1], "Thu tu ca thi phai la so nguyen duong.");

            try (Connection conn = DatabaseConfig.getConnection()) {
                KetQuaDAO dao = new KetQuaDAO();
                Gson gson = new Gson();
                return "OK|" + gson.toJson(dao.getPhanCongTheoCa(conn, thuTuCa));
            }
        }

        if (command.startsWith("GET_GIAM_SAT|")) {
            String[] parts = command.split("\\|");
            if (parts.length < 2) {
                return "ERROR|Thieu thu tu ca thi";
            }

            int thuTuCa = parseInt(parts[1], "Thu tu ca thi phai la so nguyen duong.");

            try (Connection conn = DatabaseConfig.getConnection()) {
                KetQuaDAO dao = new KetQuaDAO();
                Gson gson = new Gson();
                return "OK|" + gson.toJson(dao.getGiamSatTheoCa(conn, thuTuCa));
            }
        }

        if (command.equals("GET_LICH_SU_CA")) {
            try (Connection conn = DatabaseConfig.getConnection()) {
                KetQuaDAO dao = new KetQuaDAO();
                Gson gson = new Gson();
                return "OK|" + gson.toJson(dao.getLichSuCa(conn));
            }
        }

        return "ERROR|Lenh khong hop le: " + command;
    }

    private int parseInt(String value, String err) {
        try {
            int parsed = Integer.parseInt(value.trim());
            if (parsed <= 0) {
                throw new NumberFormatException();
            }
            return parsed;
        } catch (Exception e) {
            throw new IllegalArgumentException(err);
        }
    }

    private int parseRequiredPositiveInt(String value, String fieldName) {
        String v = value == null ? "" : value.trim();
        if (v.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " khong duoc de trong.");
        }
        try {
            int parsed = Integer.parseInt(v);
            if (parsed <= 0) {
                throw new NumberFormatException();
            }
            return parsed;
        } catch (Exception e) {
            throw new IllegalArgumentException(fieldName + " phai la so nguyen duong.");
        }
    }

    private void validatePhanCongInput(int soCanBo, int soPhong) {
        if (soCanBo < soPhong) {
            throw new IllegalArgumentException(
                    "So can bo coi thi (" + soCanBo + ") khong duoc nho hon so phong thi (" + soPhong + ")."
            );
        }

        int canBoToiThieu = soPhong * 2;
        if (soCanBo < canBoToiThieu) {
            throw new IllegalArgumentException(
                    "Khong du can bo. Toi thieu can " + canBoToiThieu
                            + " can bo cho " + soPhong + " phong (moi phong 2 giam thi)."
            );
        }
    }

    private void validateExcelPath(String excelPath) {
        String p = excelPath == null ? "" : excelPath.trim();
        if (p.isEmpty()) {
            throw new IllegalArgumentException("Duong dan file Excel khong duoc de trong.");
        }

        Path path = Path.of(p);
        if (!Files.exists(path) || !Files.isRegularFile(path)) {
            throw new IllegalArgumentException("File Excel khong ton tai hoac khong hop le.");
        }
        if (!p.toLowerCase().endsWith(".xlsx")) {
            throw new IllegalArgumentException("File Excel phai co dinh dang .xlsx");
        }
    }

    private void validateOutputDir(String outputDir) {
        String p = outputDir == null ? "" : outputDir.trim();
        if (p.isEmpty()) {
            throw new IllegalArgumentException("Thu muc xuat khong duoc de trong.");
        }

        Path path = Path.of(p);
        if (!Files.exists(path) || !Files.isDirectory(path)) {
            throw new IllegalArgumentException("Thu muc xuat khong ton tai hoac khong hop le.");
        }
    }

    private void validateNgayThi(String ngayThi) {
        String value = ngayThi == null ? "" : ngayThi.trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("Ngay thi khong duoc de trong.");
        }
        try {
            LocalDate.parse(value, INPUT_DATE_FORMAT);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Ngay thi phai dung dinh dang dd/MM/yyyy.");
        }
    }

    private String cleanMessage(String message) {
        if (message == null || message.trim().isEmpty()) {
            return "Co loi xay ra o server";
        }

        return message
                .replace("\n", " ")
                .replace("\r", " ")
                .replace("|", "/");
    }

    private String firstToken(String command) {
        int idx = command.indexOf('|');
        if (idx < 0) {
            return command;
        }
        return command.substring(0, idx);
    }

    private String formatDuration(long nanos) {
        double ms = nanos / 1_000_000.0;
        if (ms < 1000) {
            return String.format("%.2f ms", ms);
        }
        return String.format("%.2f s", ms / 1000.0);
    }

    private void log(String message) {
        if (logger != null) {
            logger.accept(message);
        }
    }

    private void startDiscoveryResponder() throws Exception {
        discoverySocket = new DatagramSocket(null);
        discoverySocket.setReuseAddress(true);
        discoverySocket.bind(new InetSocketAddress("0.0.0.0", DISCOVERY_PORT));
        discoverySocket.setBroadcast(true);

        discoveryThread = new Thread(this::discoveryLoop, "server-discovery-thread");
        discoveryThread.setDaemon(true);
        discoveryThread.start();
        log("Discovery UDP dang lang nghe o cong " + DISCOVERY_PORT);
    }

    private void stopDiscoveryResponder() {
        try {
            if (discoverySocket != null && !discoverySocket.isClosed()) {
                discoverySocket.close();
            }
        } catch (Exception ignored) {
            // ignore
        }
    }

    private void discoveryLoop() {
        byte[] buffer = new byte[256];
        while (running && discoverySocket != null && !discoverySocket.isClosed()) {
            try {
                DatagramPacket request = new DatagramPacket(buffer, buffer.length);
                discoverySocket.receive(request);

                String payload = new String(
                        request.getData(),
                        request.getOffset(),
                        request.getLength(),
                        StandardCharsets.UTF_8
                ).trim();

                if (!DISCOVERY_REQUEST.equals(payload)) {
                    continue;
                }

                String responseText = DISCOVERY_RESPONSE_PREFIX + tcpPort;
                byte[] out = responseText.getBytes(StandardCharsets.UTF_8);
                DatagramPacket response = new DatagramPacket(
                        out,
                        out.length,
                        request.getAddress(),
                        request.getPort()
                );
                discoverySocket.send(response);
            } catch (SocketException se) {
                if (running) {
                    log("Loi discovery UDP: " + se.getMessage());
                }
                break;
            } catch (Exception e) {
                if (running) {
                    log("Loi xu ly discovery UDP: " + e.getMessage());
                }
            }
        }
    }
}
