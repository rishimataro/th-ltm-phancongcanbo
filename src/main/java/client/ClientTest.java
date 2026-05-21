package client;

public class ClientTest {

    public static void main(String[] args) {
        try {
            ClientService client = new ClientService("localhost", 8888);

            System.out.println(client.ping());

            String excelPath = "E:/SEM6/07_THLTM/THLTM_PHANCONGCANBO/data/Danh sach can bo coi thi.xlsx";
            String outputDir = "E:/SEM6/07_THLTM/THLTM_PHANCONGCANBO/output";

            System.out.println(client.importAll(excelPath));
            System.out.println(client.phanCong(1, 2000, 700));
            System.out.println(client.xuatExcel(1, outputDir, "20/05/2026", ""));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
