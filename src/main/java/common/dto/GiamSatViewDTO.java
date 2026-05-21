package common.dto;

public class GiamSatViewDTO {

    private int stt;
    private String maGv;
    private String hoTen;
    private String phamViGiamSat;

    public GiamSatViewDTO() {
    }

    public GiamSatViewDTO(int stt, String maGv, String hoTen, String phamViGiamSat) {
        this.stt = stt;
        this.maGv = maGv;
        this.hoTen = hoTen;
        this.phamViGiamSat = phamViGiamSat;
    }

    public int getStt() {
        return stt;
    }

    public String getMaGv() {
        return maGv;
    }

    public String getHoTen() {
        return hoTen;
    }

    public String getPhamViGiamSat() {
        return phamViGiamSat;
    }
}