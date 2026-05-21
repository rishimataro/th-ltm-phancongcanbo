package server.model;

public class PhongThi {

    private Integer sttExcel;
    private String maPhong;
    private String ghiChu;

    public PhongThi() {
    }

    public PhongThi(Integer sttExcel, String maPhong, String ghiChu) {
        this.sttExcel = sttExcel;
        this.maPhong = maPhong;
        this.ghiChu = ghiChu;
    }

    public Integer getSttExcel() {
        return sttExcel;
    }

    public void setSttExcel(Integer sttExcel) {
        this.sttExcel = sttExcel;
    }

    public String getMaPhong() {
        return maPhong;
    }

    public void setMaPhong(String maPhong) {
        this.maPhong = maPhong;
    }

    public String getGhiChu() {
        return ghiChu;
    }

    public void setGhiChu(String ghiChu) {
        this.ghiChu = ghiChu;
    }
}