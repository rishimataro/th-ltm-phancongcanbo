package server.excel;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import server.config.DatabaseConfig;
import server.dao.CanBoDAO;
import server.dao.PhongThiDAO;
import server.model.CanBo;
import server.model.PhongThi;

import java.io.FileInputStream;
import java.sql.Connection;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ExcelImportService {

    private static final int BATCH_SIZE = 500;

    private final CanBoDAO canBoDAO = new CanBoDAO();
    private final PhongThiDAO phongThiDAO = new PhongThiDAO();
    private final DataFormatter formatter = new DataFormatter();

    public int importCanBo(String excelPath) throws Exception {
        long start = System.nanoTime();
        int count = 0;
        List<CanBo> batch = new ArrayList<>(BATCH_SIZE);

        System.out.println("[importCanBo] Dang mo file Excel...");

        try (
                FileInputStream fis = new FileInputStream(excelPath);
                Workbook workbook = new XSSFWorkbook(fis);
                Connection conn = DatabaseConfig.getConnection()
        ) {
            conn.setAutoCommit(false);

            try {
                Sheet sheet = workbook.getSheet("Danh sach can bo");
                if (sheet == null) {
                    sheet = workbook.getSheet("Danh sách cán bộ");
                }

                if (sheet == null) {
                    printSheetNames(workbook);
                    throw new RuntimeException("Khong tim thay sheet Danh sach can bo");
                }

                for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                    Row row = sheet.getRow(rowIndex);
                    if (row == null) {
                        continue;
                    }

                    Integer stt = getInteger(row.getCell(0));
                    String maGv = getString(row.getCell(1));
                    String hoTen = getString(row.getCell(2));
                    LocalDate ngaySinh = getDate(row.getCell(3));
                    String donVi = getString(row.getCell(4));

                    if (isBlank(maGv) || isBlank(hoTen)) {
                        continue;
                    }

                    batch.add(new CanBo(stt, maGv, hoTen, ngaySinh, donVi));

                    if (batch.size() >= BATCH_SIZE) {
                        canBoDAO.insertOrUpdateBatch(conn, batch, BATCH_SIZE);
                        count += batch.size();
                        batch.clear();
                        if (count % 1000 == 0) {
                            System.out.println("[importCanBo] Da import " + count + " can bo...");
                        }
                    }
                }

                if (!batch.isEmpty()) {
                    canBoDAO.insertOrUpdateBatch(conn, batch, BATCH_SIZE);
                    count += batch.size();
                }

                long tCommit = System.nanoTime();
                conn.commit();
                System.out.println("[importCanBo] Commit thanh cong. Tong: " + count);
                System.out.println("[TIME] importCanBo.commit: " + formatDuration(tCommit));

            } catch (Exception e) {
                conn.rollback();
                System.out.println("[importCanBo] Co loi, da rollback.");
                throw e;
            }
        }

        System.out.println("[TIME] importCanBo.total: " + formatDuration(start));
        return count;
    }

    public int importPhongThi(String excelPath) throws Exception {
        long start = System.nanoTime();
        int count = 0;
        List<PhongThi> batch = new ArrayList<>(BATCH_SIZE);

        System.out.println("[importPhongThi] Dang mo file Excel...");

        try (
                FileInputStream fis = new FileInputStream(excelPath);
                Workbook workbook = new XSSFWorkbook(fis);
                Connection conn = DatabaseConfig.getConnection()
        ) {
            conn.setAutoCommit(false);

            try {
                Sheet sheet = workbook.getSheet("DS phong thi");
                if (sheet == null) {
                    sheet = workbook.getSheet("DS phòng thi");
                }

                if (sheet == null) {
                    printSheetNames(workbook);
                    throw new RuntimeException("Khong tim thay sheet DS phong thi");
                }

                for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                    Row row = sheet.getRow(rowIndex);
                    if (row == null) {
                        continue;
                    }

                    Integer stt = getInteger(row.getCell(0));
                    String maPhong = getString(row.getCell(1));
                    String ghiChu = getString(row.getCell(2));

                    if (isBlank(maPhong)) {
                        continue;
                    }

                    batch.add(new PhongThi(stt, maPhong, ghiChu));

                    if (batch.size() >= BATCH_SIZE) {
                        phongThiDAO.insertOrUpdateBatch(conn, batch, BATCH_SIZE);
                        count += batch.size();
                        batch.clear();
                        if (count % 1000 == 0) {
                            System.out.println("[importPhongThi] Da import " + count + " phong thi...");
                        }
                    }
                }

                if (!batch.isEmpty()) {
                    phongThiDAO.insertOrUpdateBatch(conn, batch, BATCH_SIZE);
                    count += batch.size();
                }

                long tCommit = System.nanoTime();
                conn.commit();
                System.out.println("[importPhongThi] Commit thanh cong. Tong: " + count);
                System.out.println("[TIME] importPhongThi.commit: " + formatDuration(tCommit));

            } catch (Exception e) {
                conn.rollback();
                System.out.println("[importPhongThi] Co loi, da rollback.");
                throw e;
            }
        }

        System.out.println("[TIME] importPhongThi.total: " + formatDuration(start));
        return count;
    }

    private String getString(Cell cell) {
        if (cell == null) {
            return "";
        }
        return formatter.formatCellValue(cell).trim();
    }

    private Integer getInteger(Cell cell) {
        String value = getString(cell);
        if (isBlank(value)) {
            return null;
        }
        try {
            value = value.replace(".0", "");
            return Integer.parseInt(value);
        } catch (Exception e) {
            return null;
        }
    }

    private LocalDate getDate(Cell cell) {
        if (cell == null) {
            return null;
        }

        try {
            if (cell.getCellType() == CellType.NUMERIC) {
                return DateUtil.getJavaDate(cell.getNumericCellValue())
                        .toInstant()
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate();
            }

            String value = getString(cell);
            if (isBlank(value)) {
                return null;
            }

            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("d/M/yyyy");
            return LocalDate.parse(value, dateFormatter);
        } catch (Exception e) {
            return null;
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private void printSheetNames(Workbook workbook) {
        System.out.println("Cac sheet hien co trong file Excel:");
        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            System.out.println("- " + workbook.getSheetName(i));
        }
    }

    private String formatDuration(long startNano) {
        long elapsed = System.nanoTime() - startNano;
        double ms = elapsed / 1_000_000.0;
        if (ms < 1000) {
            return String.format("%.2f ms", ms);
        }
        return String.format("%.2f s", ms / 1000.0);
    }
}
