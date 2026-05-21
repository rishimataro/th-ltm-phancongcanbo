package server.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConfig {

    private static volatile String dbUrl =
            "jdbc:mysql://vpnphake.ddns.net:3636/phan_cong_coi_thi"
                    + "?useUnicode=true"
                    + "&characterEncoding=UTF-8"
                    + "&serverTimezone=Asia/Ho_Chi_Minh"
                    + "&rewriteBatchedStatements=true"
                    + "&useServerPrepStmts=false"
                    + "&cachePrepStmts=true"
                    + "&prepStmtCacheSize=256"
                    + "&prepStmtCacheSqlLimit=2048"
                    + "&maintainTimeStats=false";
    private static volatile String dbUser = "pbl5";
    private static volatile String dbPassword = "Khonhatthegioi@36";

    public static synchronized void configure(String url, String user, String password) {
        if (url == null || url.trim().isEmpty()) {
            throw new IllegalArgumentException("DB URL khong duoc de trong.");
        }
        if (user == null || user.trim().isEmpty()) {
            throw new IllegalArgumentException("DB user khong duoc de trong.");
        }

        dbUrl = withPerformanceOptions(url.trim());
        dbUser = user.trim();
        dbPassword = password == null ? "" : password;
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(dbUrl, dbUser, dbPassword);
    }

    public static String getDbUrl() {
        return dbUrl;
    }

    public static String getDbUser() {
        return dbUser;
    }

    public static String getDbPassword() {
        return dbPassword;
    }

    private static String withPerformanceOptions(String url) {
        String result = url;
        result = ensureParam(result, "rewriteBatchedStatements", "true");
        result = ensureParam(result, "useServerPrepStmts", "false");
        result = ensureParam(result, "cachePrepStmts", "true");
        result = ensureParam(result, "prepStmtCacheSize", "256");
        result = ensureParam(result, "prepStmtCacheSqlLimit", "2048");
        result = ensureParam(result, "maintainTimeStats", "false");
        return result;
    }

    private static String ensureParam(String url, String key, String value) {
        String marker = key + "=";
        if (url.contains(marker)) {
            return url;
        }
        String separator = url.contains("?") ? "&" : "?";
        return url + separator + key + "=" + value;
    }
}
