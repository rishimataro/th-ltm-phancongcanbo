package server;

import server.service.PhanCongService;

public class PhanCongTest {

    public static void main(String[] args) {
        try {
            PhanCongService service = new PhanCongService();

            service.phanCongCa(1);

        } catch (Exception e) {
            System.out.println("Phân công thất bại!");
            e.printStackTrace();
        }
    }
}