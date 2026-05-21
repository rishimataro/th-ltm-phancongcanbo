package server;

import server.excel.ExcelImportService;

import java.io.File;

public class ImportExcelTest {

    public static void main(String[] args) {
        try {
            System.out.println("Bắt đầu import Excel...");

            String excelPath = "E:/SEM6/07_THLTM/THLTM_PHANCONGCANBO/data/Danh sach can bo coi thi.xlsx";

            System.out.println("Đường dẫn Excel: " + excelPath);

            File file = new File(excelPath);

            if (!file.exists()) {
                System.out.println("File không tồn tại!");
                return;
            }

            System.out.println("File tồn tại.");
            System.out.println("Dung lượng file: " + file.length() + " bytes");

            ExcelImportService service = new ExcelImportService();

            System.out.println("Chuẩn bị import cán bộ...");
            int soCanBo = service.importCanBo(excelPath);
            System.out.println("Import cán bộ thành công: " + soCanBo);

            System.out.println("Chuẩn bị import phòng thi...");
            int soPhongThi = service.importPhongThi(excelPath);
            System.out.println("Import phòng thi thành công: " + soPhongThi);

            System.out.println("Hoàn thành import Excel.");

        } catch (Exception e) {
            System.out.println("Import Excel thất bại!");
            e.printStackTrace();
        }
    }
}