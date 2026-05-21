package server;

import server.config.DatabaseConfig;

import java.sql.Connection;

public class DbTest {

    public static void main(String[] args) {
        try (Connection conn = DatabaseConfig.getConnection()) {
            System.out.println("Kết nối MySQL thành công!");
            System.out.println("Database hiện tại: " + conn.getCatalog());
        } catch (Exception e) {
            System.out.println("Kết nối MySQL thất bại!");
            e.printStackTrace();
        }
    }
}