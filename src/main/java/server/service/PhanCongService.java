package server.service;

import server.config.DatabaseConfig;
import server.dao.PhanCongDAO;
import server.model.CanBoPhanCong;
import server.model.PhongThiPhanCong;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PhanCongService {

    private final PhanCongDAO dao = new PhanCongDAO();

    public void phanCongCa(int thuTuCa) throws Exception {
        phanCongCa(thuTuCa, null, null);
    }

    public void phanCongCa(int thuTuCa, Integer gioiHanCanBo, Integer gioiHanPhong) throws Exception {
        long startAll = System.nanoTime();

        try (Connection conn = DatabaseConfig.getConnection()) {
            conn.setAutoCommit(false);
            int oldForeignKeyChecks = getSessionFlag(conn, "foreign_key_checks");
            int oldUniqueChecks = getSessionFlag(conn, "unique_checks");

            try {
                setSessionFlag(conn, "foreign_key_checks", 0);
                setSessionFlag(conn, "unique_checks", 0);

                long t0 = System.nanoTime();
                long caThiId = dao.findOrCreateCaThi(conn, thuTuCa);
                dao.resetCaThi(conn, caThiId);
                logTime("Chuan bi ca thi + reset", t0);

                t0 = System.nanoTime();
                List<CanBoPhanCong> danhSachCanBo = dao.getAllCanBo(conn, gioiHanCanBo);
                List<PhongThiPhanCong> danhSachPhong = dao.getAllPhongThi(conn, gioiHanPhong);
                logTime("Tai du lieu can bo/phong", t0);

                if (gioiHanCanBo != null && gioiHanCanBo > 0 && danhSachCanBo.size() < gioiHanCanBo) {
                    throw new RuntimeException(
                            "So can bo coi thi yeu cau la " + gioiHanCanBo
                                    + " nhung du lieu hien co chi " + danhSachCanBo.size()
                    );
                }
                if (gioiHanPhong != null && gioiHanPhong > 0 && danhSachPhong.size() < gioiHanPhong) {
                    throw new RuntimeException(
                            "So phong thi yeu cau la " + gioiHanPhong
                                    + " nhung du lieu hien co chi " + danhSachPhong.size()
                    );
                }

                if (danhSachCanBo.isEmpty()) {
                    throw new RuntimeException("Khong co du lieu can bo de phan cong.");
                }
                if (danhSachPhong.isEmpty()) {
                    throw new RuntimeException("Khong co du lieu phong thi de phan cong.");
                }
                if (danhSachCanBo.size() < danhSachPhong.size() * 2) {
                    throw new RuntimeException(
                            "Khong du can bo. Can toi thieu " + (danhSachPhong.size() * 2)
                                    + " can bo nhung chi co " + danhSachCanBo.size()
                    );
                }

                System.out.println("Bat dau phan cong Ca " + thuTuCa);
                System.out.println("So can bo duoc dung: " + danhSachCanBo.size());
                System.out.println("So phong thi duoc dung: " + danhSachPhong.size());

                t0 = System.nanoTime();
                Set<Long> lichSuCanBoPhong = dao.getLichSuCanBoPhongKey(conn);
                Set<Long> capGiamThiDaDung = dao.getCapGiamThiDaDungKey(conn);
                Set<Long> canBoDaDungTrongCa = new HashSet<>(danhSachPhong.size() * 2);
                logTime("Tai rang buoc lich su/cap", t0);

                t0 = System.nanoTime();
                List<PhanCongDAO.PhanCongInsert> phanCongRows = new ArrayList<>(danhSachPhong.size());

                int pointer = 0;
                int n = danhSachCanBo.size();

                for (int i = 0; i < danhSachPhong.size(); i++) {
                    PhongThiPhanCong phong = danhSachPhong.get(i);

                    int idx1 = timChiSoGiamThi1(
                            danhSachCanBo,
                            phong,
                            canBoDaDungTrongCa,
                            lichSuCanBoPhong,
                            pointer
                    );

                    if (idx1 < 0) {
                        throw new RuntimeException("Khong tim duoc giam thi 1 cho phong " + phong.getMaPhong());
                    }

                    CanBoPhanCong giamThi1 = danhSachCanBo.get(idx1);

                    int idx2 = timChiSoGiamThi2(
                            danhSachCanBo,
                            phong,
                            giamThi1,
                            canBoDaDungTrongCa,
                            lichSuCanBoPhong,
                            capGiamThiDaDung,
                            idx1 + 1
                    );

                    if (idx2 < 0) {
                        throw new RuntimeException("Khong tim duoc giam thi 2 cho phong " + phong.getMaPhong());
                    }

                    CanBoPhanCong giamThi2 = danhSachCanBo.get(idx2);

                    phanCongRows.add(new PhanCongDAO.PhanCongInsert(
                            phong.getId(),
                            giamThi1.getId(),
                            giamThi2.getId()
                    ));

                    canBoDaDungTrongCa.add(giamThi1.getId());
                    canBoDaDungTrongCa.add(giamThi2.getId());

                    lichSuCanBoPhong.add(PhanCongDAO.keyCanBoPhongLong(giamThi1.getId(), phong.getId()));
                    lichSuCanBoPhong.add(PhanCongDAO.keyCanBoPhongLong(giamThi2.getId(), phong.getId()));
                    capGiamThiDaDung.add(PhanCongDAO.keyCapLong(giamThi1.getId(), giamThi2.getId()));

                    pointer = (idx2 + 1) % n;

                    if ((i + 1) % 500 == 0) {
                        System.out.println("Da xay dung " + (i + 1) + " ban ghi phan cong (in-memory)...");
                    }
                }
                logTime("Tinh toan phan cong (in-memory)", t0);

                t0 = System.nanoTime();
                List<Long> phanCongIds = dao.insertPhanCongPhongBatch(
                        conn,
                        caThiId,
                        phanCongRows,
                        PhanCongDAO.DEFAULT_BATCH_SIZE
                );

                List<PhanCongDAO.LichSuInsert> lichSuRows = new ArrayList<>(phanCongRows.size() * 2);
                List<PhanCongDAO.NhiemVuInsert> nhiemVuRows = new ArrayList<>(phanCongRows.size() * 2);

                for (int i = 0; i < phanCongRows.size(); i++) {
                    PhanCongDAO.PhanCongInsert row = phanCongRows.get(i);
                    long phanCongId = phanCongIds.get(i);

                    lichSuRows.add(new PhanCongDAO.LichSuInsert(row.giamThi1Id(), row.phongThiId(), phanCongId));
                    lichSuRows.add(new PhanCongDAO.LichSuInsert(row.giamThi2Id(), row.phongThiId(), phanCongId));

                    nhiemVuRows.add(new PhanCongDAO.NhiemVuInsert(row.giamThi1Id(), "GIAM_THI_1", phanCongId));
                    nhiemVuRows.add(new PhanCongDAO.NhiemVuInsert(row.giamThi2Id(), "GIAM_THI_2", phanCongId));
                }

                dao.insertLichSuCanBoPhongBatch(conn, lichSuRows, PhanCongDAO.DEFAULT_BATCH_SIZE);
                dao.insertNhiemVuBatch(conn, caThiId, nhiemVuRows, PhanCongDAO.DEFAULT_BATCH_SIZE);
                logTime("Batch insert phan_cong + lich_su + nhiem_vu", t0);

                t0 = System.nanoTime();
                phanCongGiamSat(conn, caThiId, danhSachCanBo, danhSachPhong, canBoDaDungTrongCa);
                logTime("Phan cong giam sat", t0);

                t0 = System.nanoTime();
                conn.commit();
                logTime("Commit transaction", t0);

                System.out.println("Phan cong Ca " + thuTuCa + " thanh cong.");
                System.out.println("Tong thoi gian phan cong: " + formatDurationMs(System.nanoTime() - startAll));

            } catch (Exception e) {
                conn.rollback();
                System.out.println("Co loi, da rollback du lieu phan cong.");
                throw e;
            } finally {
                setSessionFlag(conn, "foreign_key_checks", oldForeignKeyChecks);
                setSessionFlag(conn, "unique_checks", oldUniqueChecks);
            }
        }
    }

    private int timChiSoGiamThi1(
            List<CanBoPhanCong> danhSachCanBo,
            PhongThiPhanCong phong,
            Set<Long> canBoDaDungTrongCa,
            Set<Long> lichSuCanBoPhong,
            int startIndex
    ) {
        int n = danhSachCanBo.size();

        for (int i = 0; i < n; i++) {
            int idx = (startIndex + i) % n;
            CanBoPhanCong canBo = danhSachCanBo.get(idx);

            if (canBoDaDungTrongCa.contains(canBo.getId())) {
                continue;
            }

            long key = PhanCongDAO.keyCanBoPhongLong(canBo.getId(), phong.getId());
            if (lichSuCanBoPhong.contains(key)) {
                continue;
            }

            return idx;
        }

        return -1;
    }

    private int timChiSoGiamThi2(
            List<CanBoPhanCong> danhSachCanBo,
            PhongThiPhanCong phong,
            CanBoPhanCong giamThi1,
            Set<Long> canBoDaDungTrongCa,
            Set<Long> lichSuCanBoPhong,
            Set<Long> capGiamThiDaDung,
            int startIndex
    ) {
        int n = danhSachCanBo.size();

        for (int i = 0; i < n; i++) {
            int idx = (startIndex + i) % n;
            CanBoPhanCong canBo = danhSachCanBo.get(idx);

            if (canBo.getId() == giamThi1.getId()) {
                continue;
            }
            if (canBoDaDungTrongCa.contains(canBo.getId())) {
                continue;
            }

            long keyCanBoPhong = PhanCongDAO.keyCanBoPhongLong(canBo.getId(), phong.getId());
            if (lichSuCanBoPhong.contains(keyCanBoPhong)) {
                continue;
            }

            long keyCap = PhanCongDAO.keyCapLong(giamThi1.getId(), canBo.getId());
            if (capGiamThiDaDung.contains(keyCap)) {
                continue;
            }

            return idx;
        }

        return -1;
    }

    private void phanCongGiamSat(
            Connection conn,
            long caThiId,
            List<CanBoPhanCong> danhSachCanBo,
            List<PhongThiPhanCong> danhSachPhong,
            Set<Long> canBoDaDungTrongCa
    ) throws Exception {

        List<CanBoPhanCong> danhSachGiamSat = new ArrayList<>();

        for (CanBoPhanCong canBo : danhSachCanBo) {
            if (!canBoDaDungTrongCa.contains(canBo.getId())) {
                danhSachGiamSat.add(canBo);
            }
        }

        int soGiamSat = danhSachGiamSat.size();
        int soPhong = danhSachPhong.size();

        if (soGiamSat == 0) {
            System.out.println("Khong con can bo de phan cong giam sat.");
            return;
        }

        List<PhanCongDAO.GiamSatInsert> rows = new ArrayList<>(soGiamSat);

        for (int i = 0; i < soGiamSat; i++) {
            CanBoPhanCong giamSat = danhSachGiamSat.get(i);

            int startIndex;
            int endIndex;

            if (soGiamSat <= soPhong) {
                startIndex = i * soPhong / soGiamSat;
                endIndex = ((i + 1) * soPhong / soGiamSat) - 1;
                if (endIndex < startIndex) {
                    endIndex = startIndex;
                }
            } else {
                startIndex = i % soPhong;
                endIndex = startIndex;
            }

            PhongThiPhanCong tuPhong = danhSachPhong.get(startIndex);
            PhongThiPhanCong denPhong = danhSachPhong.get(endIndex);

            String moTa = tuPhong.getId() == denPhong.getId()
                    ? "Phòng " + tuPhong.getMaPhong()
                    : "Từ phòng " + tuPhong.getMaPhong() + " đến phòng " + denPhong.getMaPhong();

            rows.add(new PhanCongDAO.GiamSatInsert(
                    giamSat.getId(),
                    tuPhong.getId(),
                    denPhong.getId(),
                    moTa
            ));
        }

        dao.insertGiamSatVaNhiemVuBatch(conn, caThiId, rows, PhanCongDAO.DEFAULT_BATCH_SIZE);
    }

    private void logTime(String step, long startNano) {
        long elapsed = System.nanoTime() - startNano;
        System.out.println("[TIME] " + step + ": " + formatDurationMs(elapsed));
    }

    private int getSessionFlag(Connection conn, String name) throws Exception {
        String sql = "SELECT @@" + name;
        try (var ps = conn.prepareStatement(sql);
             var rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
            return 1;
        }
    }

    private void setSessionFlag(Connection conn, String name, int value) throws Exception {
        String sql = "SET SESSION " + name + " = " + value;
        try (var ps = conn.prepareStatement(sql)) {
            ps.execute();
        }
    }

    private String formatDurationMs(long nanos) {
        double ms = nanos / 1_000_000.0;
        if (ms < 1000) {
            return String.format("%.2f ms", ms);
        }
        return String.format("%.2f s", ms / 1000.0);
    }
}
