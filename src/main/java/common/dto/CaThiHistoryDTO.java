package common.dto;

public class CaThiHistoryDTO {

    private int thuTuCa;
    private String tenCa;
    private int soPhongDaPhanCong;
    private int soCanBoGiamSat;

    public CaThiHistoryDTO() {
    }

    public CaThiHistoryDTO(int thuTuCa, String tenCa, int soPhongDaPhanCong, int soCanBoGiamSat) {
        this.thuTuCa = thuTuCa;
        this.tenCa = tenCa;
        this.soPhongDaPhanCong = soPhongDaPhanCong;
        this.soCanBoGiamSat = soCanBoGiamSat;
    }

    public int getThuTuCa() {
        return thuTuCa;
    }

    public String getTenCa() {
        return tenCa;
    }

    public int getSoPhongDaPhanCong() {
        return soPhongDaPhanCong;
    }

    public int getSoCanBoGiamSat() {
        return soCanBoGiamSat;
    }
}
