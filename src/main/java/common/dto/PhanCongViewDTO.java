package common.dto;

public class PhanCongViewDTO {

    private int stt;
    private String phongThi;
    private String maGiamThi1;
    private String hoTenGiamThi1;
    private String maGiamThi2;
    private String hoTenGiamThi2;

    public PhanCongViewDTO() {
    }

    public PhanCongViewDTO(
            int stt,
            String phongThi,
            String maGiamThi1,
            String hoTenGiamThi1,
            String maGiamThi2,
            String hoTenGiamThi2
    ) {
        this.stt = stt;
        this.phongThi = phongThi;
        this.maGiamThi1 = maGiamThi1;
        this.hoTenGiamThi1 = hoTenGiamThi1;
        this.maGiamThi2 = maGiamThi2;
        this.hoTenGiamThi2 = hoTenGiamThi2;
    }

    public int getStt() {
        return stt;
    }

    public String getPhongThi() {
        return phongThi;
    }

    public String getMaGiamThi1() {
        return maGiamThi1;
    }

    public String getHoTenGiamThi1() {
        return hoTenGiamThi1;
    }

    public String getMaGiamThi2() {
        return maGiamThi2;
    }

    public String getHoTenGiamThi2() {
        return hoTenGiamThi2;
    }
}