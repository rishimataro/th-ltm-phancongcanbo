package server.dao;

import server.model.PhongThi;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

public class PhongThiDAO {

    private static final String UPSERT_SQL = """
            INSERT INTO phong_thi (
                stt_excel,
                ma_phong,
                dia_diem,
                ghi_chu
            )
            VALUES (?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
                stt_excel = VALUES(stt_excel),
                dia_diem = VALUES(dia_diem),
                ghi_chu = VALUES(ghi_chu)
            """;

    public void insertOrUpdate(Connection conn, PhongThi phongThi) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(UPSERT_SQL)) {
            bind(ps, phongThi);
            ps.executeUpdate();
        }
    }

    public void insertOrUpdateBatch(Connection conn, List<PhongThi> phongThis, int batchSize) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(UPSERT_SQL)) {
            int pending = 0;
            for (PhongThi phongThi : phongThis) {
                bind(ps, phongThi);
                ps.addBatch();
                pending++;
                if (pending >= batchSize) {
                    ps.executeBatch();
                    pending = 0;
                }
            }
            if (pending > 0) {
                ps.executeBatch();
            }
        }
    }

    private void bind(PreparedStatement ps, PhongThi phongThi) throws SQLException {
        ps.setObject(1, phongThi.getSttExcel());
        ps.setString(2, phongThi.getMaPhong());
        ps.setString(3, phongThi.getGhiChu());
        ps.setString(4, phongThi.getGhiChu());
    }

    public int countAll(Connection conn) throws SQLException {
        String sql = "SELECT COUNT(*) FROM phong_thi";

        try (PreparedStatement ps = conn.prepareStatement(sql);
             var rs = ps.executeQuery()) {

            if (rs.next()) {
                return rs.getInt(1);
            }

            return 0;
        }
    }
}
