package server;

import server.excel.ExcelExportService;

import java.nio.file.Path;

public class ExportExcelTest {

    public static void main(String[] args) {
        try {
            ExcelExportService service = new ExcelExportService();

            int thuTuCa = 1;

            String outputDir = "E:/SEM6/07_THLTM/THLTM_PHANCONGCANBO/output";

            Path filePhanCong = service.exportDanhSachPhanCong(thuTuCa, outputDir);
            Path fileGiamSat = service.exportDanhSachGiamSat(thuTuCa, outputDir);

            System.out.println("Xuất file phân công thành công:");
            System.out.println(filePhanCong);

            System.out.println("Xuất file giám sát thành công:");
            System.out.println(fileGiamSat);

        } catch (Exception e) {
            System.out.println("Xuất Excel thất bại!");
            e.printStackTrace();
        }
    }
}