package server.model;

public class CanBoPhanCong {

    private long id;
    private String maGv;
    private String hoTen;

    public CanBoPhanCong(long id, String maGv, String hoTen) {
        this.id = id;
        this.maGv = maGv;
        this.hoTen = hoTen;
    }

    public long getId() {
        return id;
    }

    public String getMaGv() {
        return maGv;
    }

    public String getHoTen() {
        return hoTen;
    }
}