package server.dao;

import server.model.CanBoPhanCong;
import server.model.PhongThiPhanCong;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PhanCongDAO {

    public static final int DEFAULT_BATCH_SIZE = 2000;

    public record PhanCongInsert(long phongThiId, long giamThi1Id, long giamThi2Id) {
    }

    public record LichSuInsert(long canBoId, long phongThiId, long phanCongPhongId) {
    }

    public record NhiemVuInsert(long canBoId, String loaiNhiemVu, long refId) {
    }

    public record GiamSatInsert(long canBoId, long tuPhongId, long denPhongId, String moTaPhamVi) {
    }

    public long findOrCreateCaThi(Connection conn, int thuTu) throws SQLException {
        String selectSql = "SELECT id FROM ca_thi WHERE thu_tu = ?";

        try (PreparedStatement ps = conn.prepareStatement(selectSql)) {
            ps.setInt(1, thuTu);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("id");
                }
            }
        }

        String insertSql = """
                INSERT INTO ca_thi (ten_ca, thu_tu)
                VALUES (?, ?)
                """;

        try (PreparedStatement ps = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, "Ca " + thuTu);
            ps.setInt(2, thuTu);
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }

        throw new SQLException("Khong tao duoc ca thi.");
    }

    public void resetCaThi(Connection conn, long caThiId) throws SQLException {
        String deleteNhiemVu = "DELETE FROM nhiem_vu_can_bo_ca WHERE ca_thi_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(deleteNhiemVu)) {
            ps.setLong(1, caThiId);
            ps.executeUpdate();
        }

        String deleteGiamSat = "DELETE FROM phan_cong_giam_sat WHERE ca_thi_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(deleteGiamSat)) {
            ps.setLong(1, caThiId);
            ps.executeUpdate();
        }

        String deleteLichSu = """
                DELETE FROM lich_su_can_bo_phong
                WHERE phan_cong_phong_id IN (
                    SELECT id FROM phan_cong_phong WHERE ca_thi_id = ?
                )
                """;
        try (PreparedStatement ps = conn.prepareStatement(deleteLichSu)) {
            ps.setLong(1, caThiId);
            ps.executeUpdate();
        }

        String deletePhanCong = "DELETE FROM phan_cong_phong WHERE ca_thi_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(deletePhanCong)) {
            ps.setLong(1, caThiId);
            ps.executeUpdate();
        }
    }

    public List<CanBoPhanCong> getAllCanBo(Connection conn) throws SQLException {
        return getAllCanBo(conn, null);
    }

    public List<CanBoPhanCong> getAllCanBo(Connection conn, Integer limit) throws SQLException {
        List<CanBoPhanCong> list = new ArrayList<>();

        String sql = """
                SELECT id, ma_gv, ho_ten
                FROM can_bo
                ORDER BY id
                """;
        if (limit != null) {
            sql += " LIMIT ?";
        }

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            if (limit != null) {
                ps.setInt(1, limit);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new CanBoPhanCong(
                            rs.getLong("id"),
                            rs.getString("ma_gv"),
                            rs.getString("ho_ten")
                    ));
                }
            }
        }

        return list;
    }

    public List<PhongThiPhanCong> getAllPhongThi(Connection conn) throws SQLException {
        return getAllPhongThi(conn, null);
    }

    public List<PhongThiPhanCong> getAllPhongThi(Connection conn, Integer limit) throws SQLException {
        List<PhongThiPhanCong> list = new ArrayList<>();

        String sql = """
                SELECT id, ma_phong
                FROM phong_thi
                ORDER BY id
                """;
        if (limit != null) {
            sql += " LIMIT ?";
        }

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            if (limit != null) {
                ps.setInt(1, limit);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new PhongThiPhanCong(
                            rs.getLong("id"),
                            rs.getString("ma_phong")
                    ));
                }
            }
        }

        return list;
    }

    public Set<Long> getLichSuCanBoPhongKey(Connection conn) throws SQLException {
        Set<Long> set = new HashSet<>();

        String sql = """
                SELECT can_bo_id, phong_thi_id
                FROM lich_su_can_bo_phong
                """;

        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                long canBoId = rs.getLong("can_bo_id");
                long phongThiId = rs.getLong("phong_thi_id");
                set.add(keyCanBoPhongLong(canBoId, phongThiId));
            }
        }

        return set;
    }

    public Set<Long> getCapGiamThiDaDungKey(Connection conn) throws SQLException {
        Set<Long> set = new HashSet<>();

        String sql = """
                SELECT giam_thi_1_id, giam_thi_2_id
                FROM phan_cong_phong
                """;

        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                long cb1 = rs.getLong("giam_thi_1_id");
                long cb2 = rs.getLong("giam_thi_2_id");
                set.add(keyCapLong(cb1, cb2));
            }
        }

        return set;
    }

    public List<Long> insertPhanCongPhongBatch(
            Connection conn,
            long caThiId,
            List<PhanCongInsert> rows,
            int batchSize
    ) throws SQLException {
        if (rows.isEmpty()) {
            return List.of();
        }

        String insertSql = """
                INSERT INTO phan_cong_phong (
                    ca_thi_id,
                    phong_thi_id,
                    giam_thi_1_id,
                    giam_thi_2_id
                )
                VALUES (?, ?, ?, ?)
                """;

        try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
            executeBatch(rows, batchSize, ps, row -> {
                ps.setLong(1, caThiId);
                ps.setLong(2, row.phongThiId());
                ps.setLong(3, row.giamThi1Id());
                ps.setLong(4, row.giamThi2Id());
            });
        }

        List<Long> ids = getPhanCongPhongIdsByCa(conn, caThiId, rows.size());
        if (ids.size() != rows.size()) {
            throw new SQLException("So generated key phan_cong_phong khong khop so ban ghi.");
        }

        return ids;
    }

    public void insertLichSuCanBoPhongBatch(
            Connection conn,
            List<LichSuInsert> rows,
            int batchSize
    ) throws SQLException {
        if (rows.isEmpty()) {
            return;
        }

        String sql = """
                INSERT INTO lich_su_can_bo_phong (
                    can_bo_id,
                    phong_thi_id,
                    phan_cong_phong_id
                )
                VALUES (?, ?, ?)
                """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            executeBatch(rows, batchSize, ps, row -> {
                ps.setLong(1, row.canBoId());
                ps.setLong(2, row.phongThiId());
                ps.setLong(3, row.phanCongPhongId());
            });
        }
    }

    public void insertNhiemVuBatch(
            Connection conn,
            long caThiId,
            List<NhiemVuInsert> rows,
            int batchSize
    ) throws SQLException {
        if (rows.isEmpty()) {
            return;
        }

        String sql = """
                INSERT INTO nhiem_vu_can_bo_ca (
                    ca_thi_id,
                    can_bo_id,
                    loai_nhiem_vu,
                    ref_id
                )
                VALUES (?, ?, ?, ?)
                """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            executeBatch(rows, batchSize, ps, row -> {
                ps.setLong(1, caThiId);
                ps.setLong(2, row.canBoId());
                ps.setString(3, row.loaiNhiemVu());
                ps.setLong(4, row.refId());
            });
        }
    }

    public void insertGiamSatVaNhiemVuBatch(
            Connection conn,
            long caThiId,
            List<GiamSatInsert> rows,
            int batchSize
    ) throws SQLException {
        if (rows.isEmpty()) {
            return;
        }

        String insertSql = """
                INSERT INTO phan_cong_giam_sat (
                    ca_thi_id,
                    can_bo_id,
                    tu_phong_id,
                    den_phong_id,
                    mo_ta_pham_vi
                )
                VALUES (?, ?, ?, ?, ?)
                """;

        try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
            executeBatch(rows, batchSize, ps, row -> {
                ps.setLong(1, caThiId);
                ps.setLong(2, row.canBoId());
                ps.setLong(3, row.tuPhongId());
                ps.setLong(4, row.denPhongId());
                ps.setString(5, row.moTaPhamVi());
            });
        }

        List<Long> giamSatIds = getGiamSatIdsByCa(conn, caThiId, rows.size());
        if (giamSatIds.size() != rows.size()) {
            throw new SQLException("So generated key phan_cong_giam_sat khong khop so ban ghi.");
        }

        List<NhiemVuInsert> nhiemVuRows = new ArrayList<>(rows.size());
        for (int i = 0; i < rows.size(); i++) {
            nhiemVuRows.add(new NhiemVuInsert(rows.get(i).canBoId(), "GIAM_SAT", giamSatIds.get(i)));
        }
        insertNhiemVuBatch(conn, caThiId, nhiemVuRows, batchSize);
    }

    private List<Long> getPhanCongPhongIdsByCa(Connection conn, long caThiId, int expected) throws SQLException {
        String sql = """
                SELECT id
                FROM phan_cong_phong
                WHERE ca_thi_id = ?
                ORDER BY id
                """;
        return collectIds(conn, sql, caThiId, expected, "phan_cong_phong");
    }

    private List<Long> getGiamSatIdsByCa(Connection conn, long caThiId, int expected) throws SQLException {
        String sql = """
                SELECT id
                FROM phan_cong_giam_sat
                WHERE ca_thi_id = ?
                ORDER BY id
                """;
        return collectIds(conn, sql, caThiId, expected, "phan_cong_giam_sat");
    }

    private List<Long> collectIds(Connection conn, String sql, long caThiId, int expected, String tableName) throws SQLException {
        List<Long> ids = new ArrayList<>(expected);
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, caThiId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ids.add(rs.getLong(1));
                }
            }
        }
        if (ids.size() != expected) {
            throw new SQLException("So ID doc tu " + tableName + " khong khop. expected=" + expected + ", actual=" + ids.size());
        }
        return ids;
    }

    private interface BatchBinder<T> {
        void bind(T row) throws SQLException;
    }

    private <T> void executeBatch(List<T> rows, int batchSize, PreparedStatement ps, BatchBinder<T> binder) throws SQLException {
        int pending = 0;
        for (T row : rows) {
            binder.bind(row);
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

    public static long keyCanBoPhongLong(long canBoId, long phongThiId) {
        return packToLong(canBoId, phongThiId);
    }

    public static long keyCapLong(long canBo1Id, long canBo2Id) {
        long min = Math.min(canBo1Id, canBo2Id);
        long max = Math.max(canBo1Id, canBo2Id);
        return packToLong(min, max);
    }

    private static long packToLong(long left, long right) {
        if ((left & 0xFFFFFFFF00000000L) != 0 || (right & 0xFFFFFFFF00000000L) != 0) {
            throw new IllegalArgumentException("ID vuot qua gioi han 32-bit.");
        }
        return (left << 32) | (right & 0xFFFFFFFFL);
    }
}
