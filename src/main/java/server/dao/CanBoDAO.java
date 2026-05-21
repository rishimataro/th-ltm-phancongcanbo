package server.dao;

import server.model.CanBo;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

public class CanBoDAO {

    private static final String UPSERT_SQL = """
            INSERT INTO can_bo (
                stt_excel,
                ma_gv,
                ho_ten,
                ngay_sinh,
                don_vi_cong_tac
            )
            VALUES (?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
                stt_excel = VALUES(stt_excel),
                ho_ten = VALUES(ho_ten),
                ngay_sinh = VALUES(ngay_sinh),
                don_vi_cong_tac = VALUES(don_vi_cong_tac)
            """;

    public void insertOrUpdate(Connection conn, CanBo canBo) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(UPSERT_SQL)) {
            bind(ps, canBo);
            ps.executeUpdate();
        }
    }

    public void insertOrUpdateBatch(Connection conn, List<CanBo> canBos, int batchSize) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(UPSERT_SQL)) {
            int pending = 0;
            for (CanBo canBo : canBos) {
                bind(ps, canBo);
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

    private void bind(PreparedStatement ps, CanBo canBo) throws SQLException {
        ps.setObject(1, canBo.getSttExcel());
        ps.setString(2, canBo.getMaGv());
        ps.setString(3, canBo.getHoTen());

        if (canBo.getNgaySinh() != null) {
            ps.setDate(4, Date.valueOf(canBo.getNgaySinh()));
        } else {
            ps.setNull(4, java.sql.Types.DATE);
        }

        ps.setString(5, canBo.getDonViCongTac());
    }

    public int countAll(Connection conn) throws SQLException {
        String sql = "SELECT COUNT(*) FROM can_bo";

        try (PreparedStatement ps = conn.prepareStatement(sql);
             var rs = ps.executeQuery()) {

            if (rs.next()) {
                return rs.getInt(1);
            }

            return 0;
        }
    }
}
