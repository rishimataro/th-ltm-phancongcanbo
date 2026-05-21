package server.ui;

import server.ServerApp;
import server.config.DatabaseConfig;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ServerControlFrame extends JFrame {

    private static final String VI_TEXT_SAMPLE = "Tiếng Việt có dấu ĐđĂăÂâÊêÔôƠơƯư";

    private static final Color BG = new Color(242, 246, 252);
    private static final Color CARD_BG = Color.WHITE;
    private static final Color BORDER = new Color(220, 228, 238);

    private static final Color TEXT_MAIN = new Color(31, 41, 55);
    private static final Color TEXT_MUTED = new Color(100, 116, 139);

    private static final Color PRIMARY = new Color(37, 99, 235);
    private static final Color SUCCESS = new Color(22, 163, 74);
    private static final Color DANGER = new Color(220, 38, 38);
    private static final Color WARNING = new Color(217, 119, 6);
    private static final Color DARK = new Color(51, 65, 85);

    private final JTextField txtPort = new JTextField("8888");
    private final JTextField txtDbUrl = new JTextField(DatabaseConfig.getDbUrl());
    private final JTextField txtDbUser = new JTextField(DatabaseConfig.getDbUser());
    private final JPasswordField txtDbPassword = new JPasswordField(DatabaseConfig.getDbPassword());

    private final JTextArea txtLog = new JTextArea();

    private final JButton btnStart = createButton("Khởi động Server", SUCCESS);
    private final JButton btnStop = createButton("Dừng Server", DANGER);
    private final JButton btnTestDb = createButton("Thử kết nối DB", PRIMARY);
    private final JButton btnClearLog = createButton("Xóa log", DARK);
    private final JButton btnCopyLog = createButton("Copy log", WARNING);
    private final JButton btnTogglePassword = createButton("Hiện mật khẩu", DARK);

    private final JLabel lblServerStatus = new JLabel("Đang tắt");
    private final JLabel lblDbStatus = new JLabel("Chưa kiểm tra");
    private final JLabel lblCurrentPort = new JLabel("-");
    private final JLabel lblStartedAt = new JLabel("-");
    private final JLabel lblUptime = new JLabel("00:00:00");

    private final ServerApp serverApp = new ServerApp(this::log);

    private LocalDateTime startedAt;
    private char passwordEchoChar;

    private final Timer uptimeTimer = new Timer(1000, e -> updateUptime());

    public ServerControlFrame() {
        setTitle("Server - Quản lý phân công cán bộ coi thi");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1180, 720));
        setSize(1280, 780);
        setLocationRelativeTo(null);

        passwordEchoChar = txtDbPassword.getEchoChar();

        initUI();
        bindActions();
        updateButtonState();

        log("Giao diện server đã sẵn sàng.");
    }

    private void initUI() {
        JPanel root = new JPanel(new BorderLayout(12, 12));
        root.setBackground(BG);
        root.setBorder(new EmptyBorder(12, 12, 12, 12));

        root.add(buildHeader(), BorderLayout.NORTH);
        root.add(buildContent(), BorderLayout.CENTER);
        root.add(buildFooter(), BorderLayout.SOUTH);

        setContentPane(root);
    }

    private JComponent buildHeader() {
        JPanel header = new HeaderPanel();
        header.setLayout(new BorderLayout(16, 8));
        header.setBorder(new EmptyBorder(18, 22, 18, 22));
        header.setPreferredSize(new Dimension(100, 94));

        JPanel titleBox = new JPanel();
        titleBox.setOpaque(false);
        titleBox.setLayout(new BoxLayout(titleBox, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("SERVER ĐIỀU PHỐI PHÂN CÔNG COI THI");
        title.setFont(uiFont(Font.BOLD, 23));
        title.setForeground(Color.WHITE);

        JLabel subtitle = new JLabel("Quản lý Socket Server • Cấu hình MySQL • Theo dõi nhật ký hệ thống");
        subtitle.setFont(uiFont(Font.PLAIN, 13));
        subtitle.setForeground(new Color(219, 234, 254));

        titleBox.add(title);
        titleBox.add(Box.createVerticalStrut(5));
        titleBox.add(subtitle);

        JPanel statusBox = new JPanel(new GridLayout(2, 1, 0, 4));
        statusBox.setOpaque(false);

        JLabel serverLabel = new JLabel("Trạng thái server");
        serverLabel.setFont(uiFont(Font.PLAIN, 12));
        serverLabel.setForeground(new Color(219, 234, 254));

        lblServerStatus.setFont(uiFont(Font.BOLD, 15));
        lblServerStatus.setForeground(new Color(254, 240, 138));

        statusBox.add(serverLabel);
        statusBox.add(lblServerStatus);

        header.add(titleBox, BorderLayout.CENTER);
        header.add(statusBox, BorderLayout.EAST);

        return header;
    }

    private JComponent buildContent() {
        JSplitPane splitPane = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT,
                buildLeftPanel(),
                buildRightPanel()
        );

        splitPane.setResizeWeight(0.36);
        splitPane.setDividerSize(10);
        splitPane.setContinuousLayout(true);
        splitPane.setBorder(null);

        return splitPane;
    }

    private JComponent buildLeftPanel() {
        JPanel left = new JPanel(new BorderLayout(12, 12));
        left.setOpaque(false);
        left.setPreferredSize(new Dimension(440, 100));

        left.add(buildConfigCard(), BorderLayout.NORTH);
        left.add(buildActionCard(), BorderLayout.SOUTH);

        return left;
    }

    private JComponent buildRightPanel() {
        JPanel right = new JPanel(new BorderLayout(12, 12));
        right.setOpaque(false);

        right.add(buildLogCard(), BorderLayout.CENTER);

        return right;
    }

    private JComponent buildConfigCard() {
        JPanel card = createCard();
        card.setLayout(new BorderLayout(8, 12));

        JLabel title = sectionTitle("Cấu hình kết nối");
        card.add(title, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);

        styleField(txtPort);
        styleField(txtDbUrl);
        styleField(txtDbUser);
        styleField(txtDbPassword);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(7, 4, 7, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        addFormRow(form, gbc, 0, "Cổng server", txtPort);
        addFormRow(form, gbc, 1, "DB URL", txtDbUrl);
        addFormRow(form, gbc, 2, "DB User", txtDbUser);

        gbc.gridy = 3;
        gbc.gridx = 0;
        gbc.weightx = 0;
        form.add(formLabel("DB Password"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        form.add(txtDbPassword, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0;
        form.add(btnTogglePassword, gbc);

        JLabel note = new JLabel("<html>Server phải chạy trước khi mở Client. Client mặc định kết nối đến localhost:8888.</html>");
        note.setFont(uiFont(Font.PLAIN, 12));
        note.setForeground(TEXT_MUTED);
        note.setBorder(new EmptyBorder(4, 4, 0, 4));

        card.add(form, BorderLayout.CENTER);
        card.add(note, BorderLayout.SOUTH);

        return card;
    }

    private JComponent buildActionCard() {
        JPanel card = createCard();
        card.setLayout(new BorderLayout(8, 10));

        JLabel title = sectionTitle("Điều khiển server");
        card.add(title, BorderLayout.NORTH);

        JPanel actions = new JPanel(new GridLayout(2, 2, 10, 10));
        actions.setOpaque(false);

        actions.add(btnStart);
        actions.add(btnStop);
        actions.add(btnTestDb);
        actions.add(btnClearLog);

        card.add(actions, BorderLayout.CENTER);

        return card;
    }

    private JComponent buildLogCard() {
        JPanel card = createCard();
        card.setLayout(new BorderLayout(8, 10));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);

        JLabel title = sectionTitle("Nhật ký hệ thống");

        JPanel logActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        logActions.setOpaque(false);
        logActions.add(btnCopyLog);

        top.add(title, BorderLayout.WEST);
        top.add(logActions, BorderLayout.EAST);

        txtLog.setEditable(false);
        txtLog.setLineWrap(true);
        txtLog.setWrapStyleWord(true);
        txtLog.setFont(monoFont(Font.PLAIN, 13));
        txtLog.setBackground(new Color(15, 23, 42));
        txtLog.setForeground(new Color(226, 232, 240));
        txtLog.setCaretColor(Color.WHITE);
        txtLog.setBorder(new EmptyBorder(12, 12, 12, 12));

        JScrollPane scroll = new JScrollPane(txtLog);
        scroll.setBorder(BorderFactory.createLineBorder(BORDER));
        scroll.getViewport().setBackground(new Color(15, 23, 42));

        card.add(top, BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);

        return card;
    }

    private JComponent buildFooter() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);

        JLabel left = new JLabel("Java Socket Server • MySQL JDBC • Swing Control Panel");
        left.setFont(uiFont(Font.PLAIN, 12));
        left.setForeground(TEXT_MUTED);

        JLabel right = new JLabel("Luồng xử lý: Client → Server → MySQL → Excel");
        right.setFont(uiFont(Font.PLAIN, 12));
        right.setForeground(TEXT_MUTED);

        footer.add(left, BorderLayout.WEST);
        footer.add(right, BorderLayout.EAST);

        return footer;
    }

    private void bindActions() {
        btnStart.addActionListener(e -> runInBackground(() -> {
            int port = parsePort();
            String url = requireText(txtDbUrl, "DB URL");
            String user = requireText(txtDbUser, "DB User");
            String password = new String(txtDbPassword.getPassword());

            log("Đang cấu hình database...");
            DatabaseConfig.configure(url, user, password);

            log("Đang khởi động server tại port " + port + "...");
            serverApp.start(port);

            startedAt = LocalDateTime.now();
            uptimeTimer.start();

            log("Server đã khởi động thành công.");
            log("DB URL: " + url);

            SwingUtilities.invokeLater(() -> {
                lblServerStatus.setText("Đang chạy");
                lblServerStatus.setForeground(SUCCESS);
                lblCurrentPort.setText(String.valueOf(port));
                lblStartedAt.setText(startedAt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")));
                updateButtonState();
            });
        }));

        btnStop.addActionListener(e -> {
            serverApp.stop();

            startedAt = null;
            uptimeTimer.stop();

            log("Server đã dừng.");

            lblServerStatus.setText("Đang tắt");
            lblServerStatus.setForeground(DANGER);
            lblCurrentPort.setText("-");
            lblStartedAt.setText("-");
            lblUptime.setText("00:00:00");

            updateButtonState();
        });

        btnTestDb.addActionListener(e -> runInBackground(() -> {
            String url = requireText(txtDbUrl, "DB URL");
            String user = requireText(txtDbUser, "DB User");
            String password = new String(txtDbPassword.getPassword());

            log("Đang thử kết nối database...");
            DatabaseConfig.configure(url, user, password);

            try (var conn = DatabaseConfig.getConnection()) {
                String dbUrl = conn.getMetaData().getURL();
                log("Kết nối DB thành công: " + dbUrl);

                SwingUtilities.invokeLater(() -> {
                    lblDbStatus.setText("Kết nối thành công");
                    lblDbStatus.setForeground(SUCCESS);
                });
            }
        }));

        btnClearLog.addActionListener(e -> txtLog.setText(""));

        btnCopyLog.addActionListener(e -> {
            Toolkit.getDefaultToolkit()
                    .getSystemClipboard()
                    .setContents(new StringSelection(txtLog.getText()), null);

            log("Đã copy nhật ký hệ thống vào clipboard.");
        });

        btnTogglePassword.addActionListener(e -> {
            if (txtDbPassword.getEchoChar() == 0) {
                txtDbPassword.setEchoChar(passwordEchoChar);
                btnTogglePassword.setText("Hiện mật khẩu");
            } else {
                txtDbPassword.setEchoChar((char) 0);
                btnTogglePassword.setText("Ẩn mật khẩu");
            }
        });
    }

    private void runInBackground(Task task) {
        setBusy(true);

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                try {
                    task.run();
                } catch (Exception ex) {
                    log("Lỗi: " + ex.getMessage());

                    SwingUtilities.invokeLater(() -> {
                        lblDbStatus.setText("Có lỗi");
                        lblDbStatus.setForeground(DANGER);
                    });
                }

                return null;
            }

            @Override
            protected void done() {
                setBusy(false);
                updateButtonState();
            }
        }.execute();
    }

    private void setBusy(boolean busy) {
        SwingUtilities.invokeLater(() -> {
            btnStart.setEnabled(!busy && !serverApp.isRunning());
            btnStop.setEnabled(!busy && serverApp.isRunning());
            btnTestDb.setEnabled(!busy);
            btnTogglePassword.setEnabled(!busy);
            txtPort.setEnabled(!busy && !serverApp.isRunning());
            txtDbUrl.setEnabled(!busy && !serverApp.isRunning());
            txtDbUser.setEnabled(!busy && !serverApp.isRunning());
            txtDbPassword.setEnabled(!busy && !serverApp.isRunning());

            if (busy) {
                lblServerStatus.setText(serverApp.isRunning() ? "Đang chạy" : "Đang xử lý...");
                lblServerStatus.setForeground(WARNING);
            }
        });
    }

    private void updateButtonState() {
        boolean running = serverApp.isRunning();

        btnStart.setEnabled(!running);
        btnStop.setEnabled(running);
        btnTestDb.setEnabled(!running);

        txtPort.setEnabled(!running);
        txtDbUrl.setEnabled(!running);
        txtDbUser.setEnabled(!running);
        txtDbPassword.setEnabled(!running);
        btnTogglePassword.setEnabled(!running);

        if (running) {
            lblServerStatus.setText("Đang chạy");
            lblServerStatus.setForeground(SUCCESS);
        } else {
            lblServerStatus.setText("Đang tắt");
            lblServerStatus.setForeground(DANGER);
        }
    }

    private void updateUptime() {
        if (startedAt == null) {
            lblUptime.setText("00:00:00");
            return;
        }

        Duration duration = Duration.between(startedAt, LocalDateTime.now());

        long hours = duration.toHours();
        long minutes = duration.toMinutesPart();
        long seconds = duration.toSecondsPart();

        lblUptime.setText(String.format("%02d:%02d:%02d", hours, minutes, seconds));
    }

    private int parsePort() {
        String value = txtPort.getText().trim();

        try {
            int port = Integer.parseInt(value);

            if (port < 1 || port > 65535) {
                throw new NumberFormatException();
            }

            return port;
        } catch (Exception e) {
            throw new IllegalArgumentException("Port phải là số trong khoảng 1..65535.");
        }
    }

    private String requireText(JTextField field, String name) {
        String value = field.getText().trim();

        if (value.isEmpty()) {
            throw new IllegalArgumentException(name + " không được để trống.");
        }

        return value;
    }

    private void log(String message) {
        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        SwingUtilities.invokeLater(() -> {
            txtLog.append("[" + ts + "] " + message + "\n");
            txtLog.setCaretPosition(txtLog.getDocument().getLength());
        });
    }

    private JPanel createCard() {
        JPanel panel = new RoundedPanel();
        panel.setBackground(CARD_BG);
        panel.setBorder(new EmptyBorder(14, 14, 14, 14));
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
        label.setPreferredSize(new Dimension(105, 34));
        return label;
    }

    private void addFormRow(
            JPanel panel,
            GridBagConstraints gbc,
            int row,
            String labelText,
            JComponent field
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
        panel.add(Box.createHorizontalStrut(1), gbc);
    }

    private JPanel statusRow(String title, JLabel valueLabel, Color color) {
        JPanel box = new JPanel(new BorderLayout(8, 4));
        box.setBackground(new Color(248, 250, 252));
        box.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1),
                new EmptyBorder(10, 12, 10, 12)
        ));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(uiFont(Font.PLAIN, 12));
        titleLabel.setForeground(TEXT_MUTED);

        valueLabel.setFont(uiFont(Font.BOLD, 15));
        valueLabel.setForeground(color);

        box.add(titleLabel, BorderLayout.NORTH);
        box.add(valueLabel, BorderLayout.CENTER);

        return box;
    }

    private void styleField(JComponent field) {
        field.setFont(uiFont(Font.PLAIN, 13));
        field.setBackground(Color.WHITE);
        field.setForeground(TEXT_MAIN);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1),
                new EmptyBorder(8, 10, 8, 10)
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
        button.setPreferredSize(new Dimension(Math.max(130, button.getPreferredSize().width + 24), 38));
        return button;
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
                    0,
                    0,
                    new Color(30, 64, 175),
                    getWidth(),
                    getHeight(),
                    new Color(37, 99, 235)
            );

            g2.setPaint(gradient);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 18, 18);
            g2.dispose();
        }
    }

    private static class RoundedPanel extends JPanel {
        public RoundedPanel() {
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();

            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2.setColor(new Color(226, 232, 240, 140));
            g2.fillRoundRect(2, 3, getWidth() - 4, getHeight() - 4, 18, 18);

            g2.setColor(CARD_BG);
            g2.fillRoundRect(0, 0, getWidth() - 4, getHeight() - 5, 18, 18);

            g2.setColor(BORDER);
            g2.drawRoundRect(0, 0, getWidth() - 5, getHeight() - 6, 18, 18);

            g2.dispose();

            super.paintComponent(g);
        }
    }
}