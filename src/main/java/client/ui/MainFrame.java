package client.ui;

import client.ClientService;
import common.dto.CaThiHistoryDTO;
import common.dto.GiamSatViewDTO;
import common.dto.PhanCongViewDTO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class MainFrame extends JFrame {

    private static final String VI_TEXT_SAMPLE = "Tiếng Việt có dấu ĐđĂăÂâÊêÔôƠơƯư";

    private static final Color BG = new Color(242, 246, 252);
    private static final Color CARD_BG = Color.WHITE;
    private static final Color BORDER = new Color(220, 228, 238);

    private static final Color TEXT_MAIN = new Color(31, 41, 55);
    private static final Color TEXT_MUTED = new Color(100, 116, 139);

    private static final Color PRIMARY = new Color(37, 99, 235);
    private static final Color SUCCESS = new Color(22, 163, 74);
    private static final Color DANGER = new Color(220, 38, 38);
    private static final Color DARK = new Color(51, 65, 85);

    private final ClientService clientService = new ClientService("localhost", 8888);

    private final JTextField txtExcelPath = new JTextField();
    private final JTextField txtOutputDir = new JTextField();
    private final JTextField txtSoCanBoCoiThi = new JTextField();
    private final JTextField txtSoPhongThi = new JTextField();

    private final JTextArea txtLog = new JTextArea();

    private final JLabel lblServer = new JLabel("Server: localhost:8888");
    private final JLabel lblStatus = new JLabel("Sẵn sàng");
    private final JLabel lblSoPhong = new JLabel("0");
    private final JLabel lblSoGiamSat = new JLabel("0");
    private final JLabel lblCaGanNhat = new JLabel("0");

    private final List<JButton> actionButtons = new ArrayList<>();

    private int lastAssignedCa = 0;

    private final DefaultTableModel phanCongTableModel = new DefaultTableModel(
            new Object[]{
                    "STT",
                    "Phòng thi",
                    "Mã GT1",
                    "Họ tên giám thị 1",
                    "Mã GT2",
                    "Họ tên giám thị 2"
            },
            0
    ) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    private final DefaultTableModel giamSatTableModel = new DefaultTableModel(
            new Object[]{
                    "STT",
                    "Mã GV",
                    "Họ và tên",
                    "Phòng thi được giám sát"
            },
            0
    ) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    private final JTable tblPhanCong = new JTable(phanCongTableModel);
    private final JTable tblGiamSat = new JTable(giamSatTableModel);

    private final JTabbedPane tabbedPane = new JTabbedPane();

    public MainFrame() {
        setTitle("Phần mềm phân công cán bộ coi thi");
        setSize(1320, 820);
        setMinimumSize(new Dimension(1180, 720));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        initUI();
    }

    private void initUI() {
        JPanel root = new JPanel(new BorderLayout(12, 12));
        root.setBackground(BG);
        root.setBorder(new EmptyBorder(12, 12, 12, 12));

        root.add(buildHeader(), BorderLayout.NORTH);
        root.add(buildMainContent(), BorderLayout.CENTER);
        root.add(buildFooter(), BorderLayout.SOUTH);

        setContentPane(root);
    }

    private JComponent buildHeader() {
        JPanel header = new HeaderPanel();
        header.setLayout(new BorderLayout(16, 8));
        header.setBorder(new EmptyBorder(18, 22, 18, 22));
        header.setPreferredSize(new Dimension(100, 92));

        JPanel titleBox = new JPanel();
        titleBox.setOpaque(false);
        titleBox.setLayout(new BoxLayout(titleBox, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("TRUNG TÂM ĐIỀU HÀNH PHÂN CÔNG COI THI");
        title.setFont(uiFont(Font.BOLD, 23));
        title.setForeground(Color.WHITE);

        JLabel subtitle = new JLabel("Kiểm tra server • Import Excel • Phân công (tự tăng ca + xuất Excel)");
        subtitle.setFont(uiFont(Font.PLAIN, 13));
        subtitle.setForeground(new Color(219, 234, 254));

        titleBox.add(title);
        titleBox.add(Box.createVerticalStrut(5));
        titleBox.add(subtitle);

        JPanel rightBox = new JPanel(new GridLayout(2, 1, 0, 4));
        rightBox.setOpaque(false);

        lblServer.setFont(uiFont(Font.BOLD, 13));
        lblServer.setForeground(Color.WHITE);

        lblStatus.setFont(uiFont(Font.PLAIN, 13));
        lblStatus.setForeground(new Color(220, 252, 231));

        rightBox.add(lblServer);
        rightBox.add(lblStatus);

        header.add(titleBox, BorderLayout.CENTER);
        header.add(rightBox, BorderLayout.EAST);

        return header;
    }

    private JComponent buildMainContent() {
        JSplitPane split = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT,
                buildLeftPanel(),
                buildRightPanel()
        );

        split.setResizeWeight(0.75);
        split.setDividerSize(10);
        split.setBorder(null);
        split.setContinuousLayout(true);

        return split;
    }

    private JComponent buildLeftPanel() {
        JPanel left = new JPanel(new BorderLayout(12, 12));
        left.setOpaque(false);

        left.add(buildInputCard(), BorderLayout.NORTH);
        left.add(buildDataCard(), BorderLayout.CENTER);

        return left;
    }

    private JComponent buildRightPanel() {
        JPanel right = new JPanel(new BorderLayout(12, 12));
        right.setOpaque(false);
        right.setPreferredSize(new Dimension(350, 100));

        right.add(buildStatsCard(), BorderLayout.NORTH);
        right.add(buildLogCard(), BorderLayout.CENTER);

        return right;
    }

    private JComponent buildInputCard() {
        JPanel card = createCard();
        card.setLayout(new BorderLayout(10, 10));

        JLabel title = sectionTitle("Thiết lập dữ liệu");
        card.add(title, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        styleField(txtExcelPath);
        styleField(txtOutputDir);
        styleField(txtSoCanBoCoiThi);
        styleField(txtSoPhongThi);

        JButton btnChooseExcel = createButton("Chọn file Excel", DARK);
        JButton btnChooseOutput = createButton("Chọn thư mục xuất", DARK);

        addFormRow(form, gbc, 0, "File Excel", txtExcelPath, btnChooseExcel);
        addFormRow(form, gbc, 1, "Thư mục xuất", txtOutputDir, btnChooseOutput);

        gbc.gridy = 2;
        gbc.gridx = 0;
        gbc.weightx = 0;
        form.add(formLabel("Số CB coi thi"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        form.add(txtSoCanBoCoiThi, gbc);

        gbc.gridy = 3;
        gbc.gridx = 0;
        gbc.weightx = 0;
        form.add(formLabel("Số phòng thi"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        form.add(txtSoPhongThi, gbc);

        JPanel hintPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        hintPanel.setOpaque(false);
        JLabel hint = new JLabel("Mỗi lần bấm \"Phân công\" sẽ tự tạo 1 ca thi mới và xuất Excel ngay.");
        hint.setFont(uiFont(Font.PLAIN, 12));
        hint.setForeground(TEXT_MUTED);
        hintPanel.add(hint);

        gbc.gridy = 4;
        gbc.gridx = 1;
        gbc.weightx = 1;
        form.add(hintPanel, gbc);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        actions.setOpaque(false);

        JButton btnPing = createButton("Kiểm tra server", DARK);
        JButton btnImport = createButton("Import Excel", PRIMARY);
        JButton btnPhanCong = createButton("Phân công", SUCCESS);

        actions.add(btnPing);
        actions.add(btnImport);
        actions.add(btnPhanCong);

        card.add(form, BorderLayout.CENTER);
        card.add(actions, BorderLayout.SOUTH);

        btnChooseExcel.addActionListener(e -> chooseExcelFile());
        btnChooseOutput.addActionListener(e -> chooseOutputDir());

        btnPing.addActionListener(e -> runInBackground(() -> {
            log("Đang kiểm tra kết nối server...");
            handleServerResponse(clientService.ping());
            setStatus("Đã kiểm tra server");
        }));

        btnImport.addActionListener(e -> runInBackground(() -> {
            String excelPath = requireExcelPath();

            log("Đang import dữ liệu từ Excel...");
            handleServerResponse(clientService.importAll(excelPath));
            setStatus("Import Excel xong");
        }));

        btnPhanCong.addActionListener(e -> runInBackground(() -> {
            String outputDir = requireOutputDir();

            int soCanBo = getRequiredPositiveInt(txtSoCanBoCoiThi.getText(), "Số cán bộ coi thi");
            int soPhong = getRequiredPositiveInt(txtSoPhongThi.getText(), "Số phòng thi");
            validateInputCombination(soCanBo, soPhong);

            int thuTuCa = resolveNextThuTuCa();
            String ngayThi = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

            log("Đang phân công ca " + thuTuCa + "...");
            handleServerResponse(clientService.phanCong(thuTuCa, soCanBo, soPhong));

            log("Đang xuất Excel ca " + thuTuCa + "...");
            handleServerResponse(clientService.xuatExcel(thuTuCa, outputDir, ngayThi, ""));

            lastAssignedCa = thuTuCa;
            SwingUtilities.invokeLater(() -> lblCaGanNhat.setText(String.valueOf(lastAssignedCa)));
            loadAllTables(thuTuCa);
            setStatus("Phân công + xuất Excel ca " + thuTuCa + " xong");
        }));

        return card;
    }

    private JComponent buildDataCard() {
        JPanel card = createCard();
        card.setLayout(new BorderLayout(8, 8));

        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setOpaque(false);

        JLabel title = sectionTitle("Kết quả phân công");

        JPanel tableAction = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        tableAction.setOpaque(false);

        JButton btnReload = createButton("Tải lại bảng", DARK);
        JButton btnClearTable = createButton("Xóa dữ liệu hiển thị", DANGER);

        tableAction.add(btnReload);
        tableAction.add(btnClearTable);

        titleRow.add(title, BorderLayout.WEST);
        titleRow.add(tableAction, BorderLayout.EAST);

        styleTable(tblPhanCong);
        styleTable(tblGiamSat);

        JScrollPane scrollPhanCong = new JScrollPane(tblPhanCong);
        JScrollPane scrollGiamSat = new JScrollPane(tblGiamSat);

        scrollPhanCong.setBorder(BorderFactory.createLineBorder(BORDER));
        scrollGiamSat.setBorder(BorderFactory.createLineBorder(BORDER));
        scrollPhanCong.getViewport().setBackground(Color.WHITE);
        scrollGiamSat.getViewport().setBackground(Color.WHITE);

        tabbedPane.setFont(uiFont(Font.BOLD, 13));
        tabbedPane.addTab("Danh sách phân công giám thị", scrollPhanCong);
        tabbedPane.addTab("Danh sách cán bộ giám sát", scrollGiamSat);

        card.add(titleRow, BorderLayout.NORTH);
        card.add(tabbedPane, BorderLayout.CENTER);

        btnReload.addActionListener(e -> runInBackground(() -> {
            if (lastAssignedCa <= 0) {
                int latestCa = getLatestThuTuCa();
                if (latestCa <= 0) {
                    log("Chưa có ca thi nào để tải.");
                    return;
                }
                lastAssignedCa = latestCa;
                SwingUtilities.invokeLater(() -> lblCaGanNhat.setText(String.valueOf(lastAssignedCa)));
            }
            loadAllTables(lastAssignedCa);
        }));

        btnClearTable.addActionListener(e -> {
            phanCongTableModel.setRowCount(0);
            giamSatTableModel.setRowCount(0);
            lblSoPhong.setText("0");
            lblSoGiamSat.setText("0");
            log("Đã xóa dữ liệu đang hiển thị trên bảng.");
        });

        return card;
    }

    private JComponent buildStatsCard() {
        JPanel card = createCard();
        card.setLayout(new GridLayout(3, 1, 8, 8));

        card.add(statBox("Ca thi gần nhất", lblCaGanNhat, DARK));
        card.add(statBox("Số phòng đã phân công", lblSoPhong, PRIMARY));
        card.add(statBox("Số cán bộ giám sát", lblSoGiamSat, SUCCESS));

        return card;
    }

    private JComponent buildLogCard() {
        JPanel card = createCard();
        card.setLayout(new BorderLayout(8, 8));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);

        JLabel title = sectionTitle("Nhật ký xử lý");
        JButton btnClearLog = createButton("Xóa log", DARK);

        top.add(title, BorderLayout.WEST);
        top.add(btnClearLog, BorderLayout.EAST);

        txtLog.setEditable(false);
        txtLog.setLineWrap(true);
        txtLog.setWrapStyleWord(true);
        txtLog.setFont(monoFont(Font.PLAIN, 13));
        txtLog.setBackground(new Color(15, 23, 42));
        txtLog.setForeground(new Color(226, 232, 240));
        txtLog.setCaretColor(Color.WHITE);
        txtLog.setBorder(new EmptyBorder(10, 10, 10, 10));

        JScrollPane scroll = new JScrollPane(txtLog);
        scroll.setBorder(BorderFactory.createLineBorder(BORDER));

        card.add(top, BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);

        btnClearLog.addActionListener(e -> txtLog.setText(""));
        return card;
    }

    private JComponent buildFooter() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);

        JLabel left = new JLabel("Java Swing Client/Server • MySQL • Apache POI");
        left.setFont(uiFont(Font.PLAIN, 12));
        left.setForeground(TEXT_MUTED);

        JLabel right = new JLabel("Trạng thái: sẵn sàng");
        right.setFont(uiFont(Font.PLAIN, 12));
        right.setForeground(TEXT_MUTED);

        footer.add(left, BorderLayout.WEST);
        footer.add(right, BorderLayout.EAST);
        return footer;
    }

    private int resolveNextThuTuCa() throws Exception {
        return getLatestThuTuCa() + 1;
    }

    private int getLatestThuTuCa() throws Exception {
        List<CaThiHistoryDTO> list = clientService.getLichSuCa();
        int max = 0;
        for (CaThiHistoryDTO item : list) {
            if (item != null && item.getThuTuCa() > max) {
                max = item.getThuTuCa();
            }
        }
        return max;
    }

    private void loadAllTables(int thuTuCa) throws Exception {
        loadPhanCongTable(thuTuCa);
        loadGiamSatTable(thuTuCa);
    }

    private void loadPhanCongTable(int thuTuCa) throws Exception {
        log("Đang tải bảng phân công ca " + thuTuCa + "...");
        List<PhanCongViewDTO> list = clientService.getPhanCong(thuTuCa);

        SwingUtilities.invokeLater(() -> {
            phanCongTableModel.setRowCount(0);
            for (PhanCongViewDTO item : list) {
                phanCongTableModel.addRow(new Object[]{
                        item.getStt(),
                        item.getPhongThi(),
                        item.getMaGiamThi1(),
                        item.getHoTenGiamThi1(),
                        item.getMaGiamThi2(),
                        item.getHoTenGiamThi2()
                });
            }
            lblSoPhong.setText(String.valueOf(list.size()));
            tabbedPane.setSelectedIndex(0);
        });

        log("Đã tải " + list.size() + " phòng thi lên bảng phân công.");
    }

    private void loadGiamSatTable(int thuTuCa) throws Exception {
        log("Đang tải bảng giám sát ca " + thuTuCa + "...");
        List<GiamSatViewDTO> list = clientService.getGiamSat(thuTuCa);

        SwingUtilities.invokeLater(() -> {
            giamSatTableModel.setRowCount(0);
            for (GiamSatViewDTO item : list) {
                giamSatTableModel.addRow(new Object[]{
                        item.getStt(),
                        item.getMaGv(),
                        item.getHoTen(),
                        item.getPhamViGiamSat()
                });
            }
            lblSoGiamSat.setText(String.valueOf(list.size()));
        });

        log("Đã tải " + list.size() + " cán bộ lên bảng giám sát.");
    }

    private void chooseExcelFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Chọn file danh sách cán bộ/phòng thi");
        chooser.setFileFilter(new FileNameExtensionFilter("Excel file (*.xlsx)", "xlsx"));

        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            txtExcelPath.setText(file.getAbsolutePath());
        }
    }

    private void chooseOutputDir() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Chọn thư mục xuất kết quả");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File dir = chooser.getSelectedFile();
            txtOutputDir.setText(dir.getAbsolutePath());
        }
    }

    private int getRequiredPositiveInt(String raw, String fieldName) {
        String value = raw == null ? "" : raw.trim();
        if (value.isEmpty()) {
            throw new RuntimeException(fieldName + " không được để trống.");
        }

        try {
            int n = Integer.parseInt(value);
            if (n <= 0) {
                throw new NumberFormatException();
            }
            return n;
        } catch (Exception e) {
            throw new RuntimeException(fieldName + " phải là số nguyên dương.");
        }
    }

    private void validateInputCombination(int soCanBo, int soPhong) {
        if (soCanBo < soPhong) {
            throw new RuntimeException(
                    "Số cán bộ coi thi (" + soCanBo + ") không được nhỏ hơn số phòng thi (" + soPhong + ")."
            );
        }

        long canBoToiThieu = (long) soPhong * 2L;
        if (soCanBo < canBoToiThieu) {
            throw new RuntimeException(
                    "Không đủ cán bộ coi thi. Tối thiểu cần " + canBoToiThieu
                            + " cán bộ cho " + soPhong + " phòng (mỗi phòng 2 giám thị)."
            );
        }

        if (soCanBo == canBoToiThieu) {
            log("Lưu ý: Số cán bộ vừa đủ 2 giám thị/phòng, có thể không còn cán bộ giám sát.");
        }
    }

    private String requireExcelPath() {
        String excelPath = txtExcelPath.getText().trim();
        if (excelPath.isEmpty()) {
            throw new RuntimeException("Vui lòng chọn file Excel.");
        }

        Path p = Path.of(excelPath);
        if (!Files.exists(p) || !Files.isRegularFile(p)) {
            throw new RuntimeException("File Excel không tồn tại hoặc không phải file hợp lệ.");
        }

        String lower = p.getFileName().toString().toLowerCase();
        if (!lower.endsWith(".xlsx")) {
            throw new RuntimeException("File Excel phải có định dạng .xlsx");
        }
        return excelPath;
    }

    private String requireOutputDir() {
        String outputDir = txtOutputDir.getText().trim();
        if (outputDir.isEmpty()) {
            throw new RuntimeException("Vui lòng chọn thư mục xuất Excel.");
        }

        Path p = Path.of(outputDir);
        if (!Files.exists(p) || !Files.isDirectory(p)) {
            throw new RuntimeException("Thư mục xuất không tồn tại hoặc không hợp lệ.");
        }
        return outputDir;
    }

    private void handleServerResponse(String response) {
        if (response == null || response.trim().isEmpty()) {
            throw new RuntimeException("Server không phản hồi.");
        }

        if (response.startsWith("ERROR|")) {
            throw new RuntimeException(response.substring("ERROR|".length()));
        }

        if (response.startsWith("OK|")) {
            log(response.substring("OK|".length()));
            return;
        }

        log(response);
    }

    private void runInBackground(Task task) {
        setBusy(true);

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                try {
                    task.run();
                } catch (Exception e) {
                    log("Lỗi: " + e.getMessage());
                    setStatus("Có lỗi xảy ra");
                    showErrorDialog(e.getMessage());
                }
                return null;
            }

            @Override
            protected void done() {
                setBusy(false);
            }
        }.execute();
    }

    private void setBusy(boolean busy) {
        SwingUtilities.invokeLater(() -> {
            for (JButton button : actionButtons) {
                button.setEnabled(!busy);
            }

            if (busy) {
                lblStatus.setText("Đang xử lý...");
                lblStatus.setForeground(new Color(254, 240, 138));
            } else {
                lblStatus.setText("Sẵn sàng");
                lblStatus.setForeground(new Color(220, 252, 231));
            }
        });
    }

    private void setStatus(String status) {
        SwingUtilities.invokeLater(() -> lblStatus.setText(status));
    }

    private void log(String message) {
        SwingUtilities.invokeLater(() -> {
            txtLog.append(message + "\n");
            txtLog.setCaretPosition(txtLog.getDocument().getLength());
        });
    }

    private void showErrorDialog(String message) {
        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(
                this,
                message == null || message.isBlank() ? "Có lỗi xảy ra." : message,
                "Thông báo lỗi",
                JOptionPane.ERROR_MESSAGE
        ));
    }

    private JPanel createCard() {
        JPanel panel = new JPanel();
        panel.setBackground(CARD_BG);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1),
                new EmptyBorder(12, 12, 12, 12)
        ));
        return panel;
    }

    private JLabel sectionTitle(String text) {
        JLabel label = new JLabel(text);
        label.setFont(uiFont(Font.BOLD, 16));
        label.setForeground(TEXT_MAIN);
        return label;
    }

    private JLabel formLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(uiFont(Font.BOLD, 13));
        label.setForeground(TEXT_MAIN);
        label.setPreferredSize(new Dimension(95, 32));
        return label;
    }

    private void addFormRow(
            JPanel panel,
            GridBagConstraints gbc,
            int row,
            String labelText,
            JTextField field,
            JButton button
    ) {
        gbc.gridy = row;

        gbc.gridx = 0;
        gbc.weightx = 0;
        panel.add(formLabel(labelText), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        panel.add(field, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0;
        panel.add(button, gbc);
    }

    private void styleField(JTextField field) {
        field.setFont(uiFont(Font.PLAIN, 13));
        field.setForeground(TEXT_MAIN);
        field.setBackground(Color.WHITE);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1),
                new EmptyBorder(7, 10, 7, 10)
        ));
    }

    private JButton createButton(String text, Color color) {
        JButton button = new JButton(text);
        button.setFont(uiFont(Font.BOLD, 12));
        button.setForeground(Color.WHITE);
        button.setBackground(color);
        button.setOpaque(true);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(Math.max(120, button.getPreferredSize().width + 24), 36));

        actionButtons.add(button);
        return button;
    }

    private JPanel statBox(String title, JLabel valueLabel, Color color) {
        JPanel box = new JPanel(new BorderLayout(8, 4));
        box.setBackground(new Color(248, 250, 252));
        box.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1),
                new EmptyBorder(12, 12, 12, 12)
        ));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(uiFont(Font.PLAIN, 12));
        titleLabel.setForeground(TEXT_MUTED);

        valueLabel.setFont(uiFont(Font.BOLD, 28));
        valueLabel.setForeground(color);

        box.add(titleLabel, BorderLayout.NORTH);
        box.add(valueLabel, BorderLayout.CENTER);
        return box;
    }

    private void styleTable(JTable table) {
        table.setRowHeight(28);
        table.setFont(uiFont(Font.PLAIN, 13));
        table.setForeground(TEXT_MAIN);
        table.setGridColor(new Color(229, 236, 246));
        table.setSelectionBackground(new Color(219, 234, 254));
        table.setSelectionForeground(TEXT_MAIN);
        table.setFillsViewportHeight(true);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);

        table.getTableHeader().setFont(uiFont(Font.BOLD, 12));
        table.getTableHeader().setBackground(new Color(237, 242, 250));
        table.getTableHeader().setForeground(TEXT_MAIN);
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setPreferredSize(new Dimension(0, 34));

        DefaultTableCellRenderer headerRenderer =
                (DefaultTableCellRenderer) table.getTableHeader().getDefaultRenderer();
        headerRenderer.setHorizontalAlignment(SwingConstants.CENTER);

        table.setDefaultRenderer(Object.class, new ZebraRenderer());
    }

    private static Font uiFont(int style, int size) {
        String[] candidates = {"Segoe UI", "Tahoma", "Arial", "SansSerif"};
        for (String name : candidates) {
            Font font = new Font(name, style, size);
            if (font.canDisplayUpTo(VI_TEXT_SAMPLE) == -1) {
                return font;
            }
        }
        return new Font(Font.SANS_SERIF, style, size);
    }

    private static Font monoFont(int style, int size) {
        String[] candidates = {"Consolas", "Cascadia Mono", "Monospaced"};
        for (String name : candidates) {
            Font font = new Font(name, style, size);
            if (font.canDisplayUpTo(VI_TEXT_SAMPLE) == -1) {
                return font;
            }
        }
        return new Font(Font.MONOSPACED, style, size);
    }

    private interface Task {
        void run() throws Exception;
    }

    private static class HeaderPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            Graphics2D g2 = (Graphics2D) g.create();
            GradientPaint gradient = new GradientPaint(
                    0, 0, new Color(30, 64, 175),
                    getWidth(), getHeight(), new Color(37, 99, 235)
            );
            g2.setPaint(gradient);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 18, 18);
            g2.dispose();
        }
    }

    private static class ZebraRenderer extends DefaultTableCellRenderer {
        private final Color even = Color.WHITE;
        private final Color odd = new Color(248, 250, 252);

        @Override
        public Component getTableCellRendererComponent(
                JTable table,
                Object value,
                boolean isSelected,
                boolean hasFocus,
                int row,
                int column
        ) {
            Component component = super.getTableCellRendererComponent(
                    table, value, isSelected, hasFocus, row, column
            );

            if (!isSelected) {
                component.setBackground(row % 2 == 0 ? even : odd);
                component.setForeground(TEXT_MAIN);
            }

            setBorder(new EmptyBorder(0, 8, 0, 8));
            if (column == 0 || column == 1 || column == 2 || column == 4) {
                setHorizontalAlignment(SwingConstants.CENTER);
            } else {
                setHorizontalAlignment(SwingConstants.LEFT);
            }
            return component;
        }
    }
}
