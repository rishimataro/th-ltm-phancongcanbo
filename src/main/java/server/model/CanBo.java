package server.model;

import java.time.LocalDate;

public class CanBo {

    private Integer sttExcel;
    private String maGv;
    private String hoTen;
    private LocalDate ngaySinh;
    private String donViCongTac;

    public CanBo() {
    }

    public CanBo(Integer sttExcel, String maGv, String hoTen,
                 LocalDate ngaySinh, String donViCongTac) {
        this.sttExcel = sttExcel;
        this.maGv = maGv;
        this.hoTen = hoTen;
        this.ngaySinh = ngaySinh;
        this.donViCongTac = donViCongTac;
    }

    public Integer getSttExcel() {
        return sttExcel;
    }

    public void setSttExcel(Integer sttExcel) {
        this.sttExcel = sttExcel;
    }

    public String getMaGv() {
        return maGv;
    }

    public void setMaGv(String maGv) {
        this.maGv = maGv;
    }

    public String getHoTen() {
        return hoTen;
    }

    public void setHoTen(String hoTen) {
        this.hoTen = hoTen;
    }

    public LocalDate getNgaySinh() {
        return ngaySinh;
    }

    public void setNgaySinh(LocalDate ngaySinh) {
        this.ngaySinh = ngaySinh;
    }

    public String getDonViCongTac() {
        return donViCongTac;
    }

    public void setDonViCongTac(String donViCongTac) {
        this.donViCongTac = donViCongTac;
    }
}