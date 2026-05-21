package server.dao;

import common.dto.CaThiHistoryDTO;
import common.dto.GiamSatViewDTO;
import common.dto.PhanCongViewDTO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class KetQuaDAO {

    public List<CaThiHistoryDTO> getLichSuCa(Connection conn) throws Exception {
        List<CaThiHistoryDTO> list = new ArrayList<>();

        String sql = """
                SELECT
                    ct.thu_tu,
                    ct.ten_ca,
                    COUNT(DISTINCT pcp.id) AS so_phong,
                    COUNT(DISTINCT pcgs.id) AS so_giam_sat
                FROM ca_thi ct
                LEFT JOIN phan_cong_phong pcp ON pcp.ca_thi_id = ct.id
                LEFT JOIN phan_cong_giam_sat pcgs ON pcgs.ca_thi_id = ct.id
                GROUP BY ct.id, ct.thu_tu, ct.ten_ca
                ORDER BY ct.thu_tu DESC
                """;

        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new CaThiHistoryDTO(
                        rs.getInt("thu_tu"),
                        rs.getString("ten_ca"),
                        rs.getInt("so_phong"),
                        rs.getInt("so_giam_sat")
                ));
            }
        }

        return list;
    }

    public List<PhanCongViewDTO> getPhanCongTheoCa(Connection conn, int thuTuCa) throws Exception {
        List<PhanCongViewDTO> list = new ArrayList<>();

        String sql = """
                SELECT
                    pt.ma_phong,
                    cb1.ma_gv AS ma_giam_thi_1,
                    cb1.ho_ten AS ho_ten_giam_thi_1,
                    cb2.ma_gv AS ma_giam_thi_2,
                    cb2.ho_ten AS ho_ten_giam_thi_2
                FROM phan_cong_phong pcp
                JOIN ca_thi ct ON ct.id = pcp.ca_thi_id
                JOIN phong_thi pt ON pt.id = pcp.phong_thi_id
                JOIN can_bo cb1 ON cb1.id = pcp.giam_thi_1_id
                JOIN can_bo cb2 ON cb2.id = pcp.giam_thi_2_id
                WHERE ct.thu_tu = ?
                ORDER BY pt.id
                """;

        int stt = 1;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, thuTuCa);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new PhanCongViewDTO(
                            stt++,
                            rs.getString("ma_phong"),
                            rs.getString("ma_giam_thi_1"),
                            rs.getString("ho_ten_giam_thi_1"),
                            rs.getString("ma_giam_thi_2"),
                            rs.getString("ho_ten_giam_thi_2")
                    ));
                }
            }
        }

        return list;
    }

    public List<GiamSatViewDTO> getGiamSatTheoCa(Connection conn, int thuTuCa) throws Exception {
        List<GiamSatViewDTO> list = new ArrayList<>();

        String sql = """
                SELECT
                    cb.ma_gv,
                    cb.ho_ten,
                    COALESCE(
                        pcgs.mo_ta_pham_vi,
                        CONCAT('Từ ', p1.ma_phong, ' đến ', p2.ma_phong)
                    ) AS pham_vi_giam_sat
                FROM phan_cong_giam_sat pcgs
                JOIN ca_thi ct ON ct.id = pcgs.ca_thi_id
                JOIN can_bo cb ON cb.id = pcgs.can_bo_id
                JOIN phong_thi p1 ON p1.id = pcgs.tu_phong_id
                JOIN phong_thi p2 ON p2.id = pcgs.den_phong_id
                WHERE ct.thu_tu = ?
                ORDER BY pcgs.id
                """;

        int stt = 1;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, thuTuCa);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new GiamSatViewDTO(
                            stt++,
                            rs.getString("ma_gv"),
                            rs.getString("ho_ten"),
                            rs.getString("pham_vi_giam_sat")
                    ));
                }
            }
        }

        return list;
    }
}
