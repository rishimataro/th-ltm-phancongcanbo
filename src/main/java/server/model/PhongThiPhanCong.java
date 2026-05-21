package server.model;

public class PhongThiPhanCong {

    private long id;
    private String maPhong;

    public PhongThiPhanCong(long id, String maPhong) {
        this.id = id;
        this.maPhong = maPhong;
    }

    public long getId() {
        return id;
    }

    public String getMaPhong() {
        return maPhong;
    }
}