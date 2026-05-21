package server.excel;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.util.IOUtils;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import server.config.DatabaseConfig;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

public class ExcelExportService {

    private static final int MAX_ROWS_PER_SHEET = 24;
    private static final int MAX_PHONG_PER_PHAN_CONG_SHEET = MAX_ROWS_PER_SHEET;
    private static final int MAX_GIAM_SAT_PER_SHEET = MAX_ROWS_PER_SHEET;
    private static final int PAGE_INFO_ROW_INDEX = 6;      // Excel row 7
    private static final int TABLE_HEADER_ROW_INDEX = 7;   // Excel row 8
    private static final int FIRST_DATA_ROW_INDEX = 8;     // Excel row 9

    private static final float ROW_HEIGHT_HEADER = 26f;
    private static final float ROW_HEIGHT_DATA = 24f;

    private static final DateTimeFormatter INPUT_DATE_FORMAT = DateTimeFormatter.ofPattern("d/M/yyyy");
    private static final DateTimeFormatter DISPLAY_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FOLDER_DATE_FORMAT = DateTimeFormatter.ofPattern("ddMMyyyy");

    private record PhanCongRow(int stt, String phongThi, String maGt1, String hoTenGt1, String maGt2, String hoTenGt2) {
    }

    private record GiamSatRow(int stt, String maGv, String hoTen, String phamVi) {
    }

    public Path exportBaoCaoCaThi(int thuTuCa, String outputDir, String ngayThiInput, String logoPath) throws Exception {
        LocalDate ngayThi = parseNgayThi(ngayThiInput);

        String folderName = buildCommonFolderName(ngayThi, thuTuCa);
        String fileName = "DANH_SACH_PHAN_CONG_CAN_BO_COI_THI.xlsx";

        Path outFolder = Path.of(outputDir, folderName);
        Path outputPath = outFolder.resolve(fileName);

        try (Connection conn = DatabaseConfig.getConnection(); Workbook workbook = new XSSFWorkbook()) {
            List<PhanCongRow> phanCongRows = loadPhanCongRows(conn, thuTuCa);
            List<GiamSatRow> giamSatRows = loadGiamSatRows(conn, thuTuCa);

            createPhanCongSheets(workbook, thuTuCa, ngayThi, phanCongRows, logoPath);

            Files.createDirectories(outFolder);
            try (OutputStream os = Files.newOutputStream(outputPath)) {
                workbook.write(os);
            }
        }

        return outputPath;
    }

    public Path exportBaoCaoGiamSat(int thuTuCa, String outputDir, String ngayThiInput, String logoPath) throws Exception {
        LocalDate ngayThi = parseNgayThi(ngayThiInput);

        String folderName = buildCommonFolderName(ngayThi, thuTuCa);
        String fileName = "DANH_SACH_PHAN_CONG_GIAM_SAT.xlsx";

        Path outFolder = Path.of(outputDir, folderName);
        Path outputPath = outFolder.resolve(fileName);

        try (Connection conn = DatabaseConfig.getConnection(); Workbook workbook = new XSSFWorkbook()) {
            List<GiamSatRow> giamSatRows = loadGiamSatRows(conn, thuTuCa);
            createGiamSatSheets(workbook, thuTuCa, ngayThi, giamSatRows, logoPath);

            Files.createDirectories(outFolder);
            try (OutputStream os = Files.newOutputStream(outputPath)) {
                workbook.write(os);
            }
        }

        return outputPath;
    }

    public Path exportDanhSachPhanCong(int thuTuCa, String outputDir) throws Exception {
        return exportBaoCaoCaThi(thuTuCa, outputDir, LocalDate.now().format(DISPLAY_DATE_FORMAT), "");
    }

    public Path exportDanhSachGiamSat(int thuTuCa, String outputDir) throws Exception {
        return exportBaoCaoGiamSat(thuTuCa, outputDir, LocalDate.now().format(DISPLAY_DATE_FORMAT), "");
    }

    private void createPhanCongSheets(
            Workbook workbook,
            int thuTuCa,
            LocalDate ngayThi,
            List<PhanCongRow> rows,
            String logoPath
    ) {
        if (rows.isEmpty()) {
            Sheet sheet = workbook.createSheet("Phân công 1");
            setPrintLayout(sheet);
            Styles styles = new Styles(workbook);
            drawFormHeader(sheet, styles, thuTuCa, ngayThi, logoPath, "DANH SÁCH PHÂN CÔNG CÁN BỘ COI THI");
            drawPageRow(sheet, styles, 1, 1);
            drawPhanCongTableHeader(sheet, styles, TABLE_HEADER_ROW_INDEX);
            drawSignatureBlock(sheet, styles, FIRST_DATA_ROW_INDEX + 3, ngayThi);
            setColumnWidthDefault(sheet);
            sheet.createFreezePane(0, FIRST_DATA_ROW_INDEX);
            return;
        }

        int pageCount = (int) Math.ceil(rows.size() * 1.0 / MAX_PHONG_PER_PHAN_CONG_SHEET);
        int index = 0;

        for (int page = 1; page <= pageCount; page++) {
            Sheet sheet = workbook.createSheet("Phân công " + page);
            setPrintLayout(sheet);
            Styles styles = new Styles(workbook);

            int rowIndex = drawFormHeader(
                    sheet,
                    styles,
                    thuTuCa,
                    ngayThi,
                    logoPath,
                    "DANH SÁCH PHÂN CÔNG CÁN BỘ COI THI"
            );

            drawPageRow(sheet, styles, page, pageCount);

            drawPhanCongTableHeader(sheet, styles, TABLE_HEADER_ROW_INDEX);
            rowIndex = FIRST_DATA_ROW_INDEX;

            for (int i = 0; i < MAX_PHONG_PER_PHAN_CONG_SHEET && index < rows.size(); i++, index++) {
                PhanCongRow data = rows.get(index);
                Row row = sheet.createRow(rowIndex++);
                row.setHeightInPoints(ROW_HEIGHT_DATA);
                createCell(row, 0, data.stt(), styles.normalCenter);
                createCell(row, 1, data.phongThi(), styles.normalCenter);
                createCell(row, 2, data.maGt1(), styles.normalCenter);
                createCell(row, 3, data.hoTenGt1(), styles.normal);
                createCell(row, 4, data.maGt2(), styles.normalCenter);
                createCell(row, 5, data.hoTenGt2(), styles.normal);
                createCell(row, 6, "", styles.normal);
                createCell(row, 7, "", styles.normal);
            }

            drawSignatureBlock(sheet, styles, rowIndex + 2, ngayThi);
            setColumnWidthDefault(sheet);
            sheet.createFreezePane(0, FIRST_DATA_ROW_INDEX);
        }
    }

    private void createGiamSatSheets(
            Workbook workbook,
            int thuTuCa,
            LocalDate ngayThi,
            List<GiamSatRow> rows,
            String logoPath
    ) {
        if (rows.isEmpty()) {
            Sheet sheet = workbook.createSheet("Giám sát 1");
            setPrintLayout(sheet);
            Styles styles = new Styles(workbook);
            drawFormHeader(sheet, styles, thuTuCa, ngayThi, logoPath, "DANH SÁCH PHÂN CÔNG GIÁM SÁT");
            drawPageRow(sheet, styles, 1, 1);
            drawGiamSatTableHeader(sheet, styles, TABLE_HEADER_ROW_INDEX);
            drawSignatureBlock(sheet, styles, FIRST_DATA_ROW_INDEX + 3, ngayThi);
            setColumnWidthDefault1(sheet);
            sheet.createFreezePane(0, FIRST_DATA_ROW_INDEX);
            return;
        }

        int pageCount = (int) Math.ceil(rows.size() * 1.0 / MAX_GIAM_SAT_PER_SHEET);
        int index = 0;

        for (int page = 1; page <= pageCount; page++) {
            Sheet sheet = workbook.createSheet("Giám sát " + page);
            setPrintLayout(sheet);
            Styles styles = new Styles(workbook);

            int rowIndex = drawFormHeader(sheet, styles, thuTuCa, ngayThi, logoPath, "DANH SÁCH PHÂN CÔNG GIÁM SÁT");

            drawPageRow(sheet, styles, page, pageCount);

            drawGiamSatTableHeader(sheet, styles, TABLE_HEADER_ROW_INDEX);
            rowIndex = FIRST_DATA_ROW_INDEX;

            for (int i = 0; i < MAX_GIAM_SAT_PER_SHEET && index < rows.size(); i++, index++) {
                GiamSatRow data = rows.get(index);
                Row row = sheet.createRow(rowIndex++);
                row.setHeightInPoints(ROW_HEIGHT_DATA);
                createCell(row, 0, data.stt(), styles.normalCenter);
                createCell(row, 1, data.maGv(), styles.normalCenter);
                createCell(row, 2, data.hoTen(), styles.normal);
                createCell(row, 3, data.phamVi(), styles.normal);
                createCell(row, 4, "", styles.normal);
                createCell(row, 5, "", styles.normal);
                createCell(row, 6, "", styles.normal);
                createCell(row, 7, "", styles.normal);
                // Chỉ merge phạm vi giám sát từ cột 3..6, giữ cột 7 cho "Ghi chú"
                sheet.addMergedRegion(new CellRangeAddress(row.getRowNum(), row.getRowNum(), 3, 6));
            }

            drawSignatureBlock(sheet, styles, rowIndex + 2, ngayThi);
            setColumnWidthDefault1(sheet);
            sheet.createFreezePane(0, FIRST_DATA_ROW_INDEX);
        }
    }

    private int drawFormHeader(Sheet sheet, Styles styles, int thuTuCa, LocalDate ngayThi, String logoPath, String title) {
        for (int i = 0; i < 3; i++) {
            sheet.createRow(i).setHeightInPoints(20);
        }

        Row row0 = sheet.getRow(0);
        Row row1 = sheet.getRow(1);
        Row row2 = sheet.getRow(2);

        createCell(row0, 0, "TRƯỜNG ĐẠI HỌC BÁCH KHOA - ĐẠI HỌC ĐÀ NẴNG", styles.leftHeader);
        createCell(row1, 0, "KHOA CÔNG NGHỆ THÔNG TIN", styles.leftHeader);
        createCell(row2, 0, "***", styles.leftHeader);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 3));
        sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 3));
        sheet.addMergedRegion(new CellRangeAddress(2, 2, 0, 3));

        createCell(row0, 4, "CỘNG HÒA XÃ HỘI CHỦ NGHĨA VIỆT NAM", styles.rightHeader);
        createCell(row1, 4, "Độc lập - Tự do - Hạnh phúc", styles.rightSubHeader);
        createCell(row2, 4, "------------------------------", styles.rightSubHeader);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 4, 7));
        sheet.addMergedRegion(new CellRangeAddress(1, 1, 4, 7));
        sheet.addMergedRegion(new CellRangeAddress(2, 2, 4, 7));

        int rowIndex = 4;
        Row titleRow = sheet.createRow(rowIndex++);
        titleRow.setHeightInPoints(24);
        createCell(titleRow, 0, title, styles.title);
        sheet.addMergedRegion(new CellRangeAddress(titleRow.getRowNum(), titleRow.getRowNum(), 0, 7));

        Row subTitleRow = sheet.createRow(rowIndex++);
        createCell(subTitleRow, 0, "Ca thi: " + thuTuCa + "    Ngày thi: " + ngayThi.format(DISPLAY_DATE_FORMAT), styles.subTitle);
        sheet.addMergedRegion(new CellRangeAddress(subTitleRow.getRowNum(), subTitleRow.getRowNum(), 0, 7));

        return rowIndex;
    }

    private void drawPhanCongTableHeader(Sheet sheet, Styles styles, int rowIndex) {
        Row header = sheet.createRow(rowIndex);
        header.setHeightInPoints(ROW_HEIGHT_HEADER);
        createCell(header, 0, "STT", styles.header);
        createCell(header, 1, "Phòng thi", styles.header);
        createCell(header, 2, "Mã GT1", styles.header);
        createCell(header, 3, "Họ tên giám thị 1", styles.header);
        createCell(header, 4, "Mã GT2", styles.header);
        createCell(header, 5, "Họ tên giám thị 2", styles.header);
        createCell(header, 6, "Ký nhận", styles.header);
        createCell(header, 7, "Ghi chú", styles.header);
    }

    private void drawGiamSatTableHeader(Sheet sheet, Styles styles, int rowIndex) {
        Row header = sheet.createRow(rowIndex);
        header.setHeightInPoints(ROW_HEIGHT_HEADER);
        createCell(header, 0, "STT", styles.header);
        createCell(header, 1, "Mã GV", styles.header);
        createCell(header, 2, "Họ và tên", styles.header);
        createCell(header, 3, "Phạm vi giám sát", styles.header);
        createCell(header, 4, "", styles.header);
        createCell(header, 5, "", styles.header);
        createCell(header, 6, "", styles.header);
        createCell(header, 7, "Ghi chú", styles.header);
        sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, 3, 6));
    }

    private void drawPageRow(Sheet sheet, Styles styles, int page, int pageCount) {
        Row pageRow = sheet.createRow(PAGE_INFO_ROW_INDEX);
        pageRow.setHeightInPoints(ROW_HEIGHT_DATA);
        createCell(pageRow, 0, "Trang " + page + "/" + pageCount, styles.italicCenter);
        sheet.addMergedRegion(new CellRangeAddress(PAGE_INFO_ROW_INDEX, PAGE_INFO_ROW_INDEX, 0, 7));
    }

    private void drawSignatureBlock(Sheet sheet, Styles styles, int startRow, LocalDate ngayThi) {
        String dateLine = "..........., ngày " + String.format("%02d", ngayThi.getDayOfMonth())
                + " tháng " + String.format("%02d", ngayThi.getMonthValue())
                + " năm " + ngayThi.getYear();

        Row r1 = sheet.createRow(startRow);
        createCell(r1, 5, dateLine, styles.normalCenter);
        sheet.addMergedRegion(new CellRangeAddress(startRow, startRow, 5, 7));

        Row r2 = sheet.createRow(startRow + 1);
        createCell(r2, 5, "NGƯỜI LẬP BIỂU", styles.boldCenter);
        sheet.addMergedRegion(new CellRangeAddress(startRow + 1, startRow + 1, 5, 7));

        Row r3 = sheet.createRow(startRow + 4);
        createCell(r3, 5, "(Ký, ghi rõ họ tên)", styles.italicCenter);
        sheet.addMergedRegion(new CellRangeAddress(startRow + 4, startRow + 4, 5, 7));
    }

    private void setPrintLayout(Sheet sheet) {
        sheet.setMargin(Sheet.LeftMargin, 0.3);
        sheet.setMargin(Sheet.RightMargin, 0.3);
        sheet.setMargin(Sheet.TopMargin, 0.3);
        sheet.setMargin(Sheet.BottomMargin, 0.3);
        sheet.setHorizontallyCenter(true);
        sheet.setFitToPage(true);
        PrintSetup printSetup = sheet.getPrintSetup();
        printSetup.setLandscape(false);
        printSetup.setPaperSize(PrintSetup.A4_PAPERSIZE);
    }

    private void setColumnWidthDefault(Sheet sheet) {
        sheet.setColumnWidth(0, 8 * 256);
        sheet.setColumnWidth(1, 14 * 256);
        sheet.setColumnWidth(2, 12 * 256);
        sheet.setColumnWidth(3, 24 * 256);
        sheet.setColumnWidth(4, 12 * 256);
        sheet.setColumnWidth(5, 24 * 256);
        sheet.setColumnWidth(6, 14 * 256);
        sheet.setColumnWidth(7, 20 * 256);
    }

    private void setColumnWidthDefault1(Sheet sheet) {
        sheet.setColumnWidth(0, 6 * 256);
        sheet.setColumnWidth(1, 14 * 256);
        sheet.setColumnWidth(2, 30 * 256);
        sheet.setColumnWidth(3, 12 * 256);
        sheet.setColumnWidth(4, 8 * 256);
        sheet.setColumnWidth(5, 8 * 256);
        sheet.setColumnWidth(6, 8 * 256);
        sheet.setColumnWidth(7, 25 * 256);
    }

    private LocalDate parseNgayThi(String ngayThiInput) {
        if (ngayThiInput == null || ngayThiInput.trim().isEmpty()) {
            throw new IllegalArgumentException("Ngày thi không được để trống.");
        }

        try {
            return LocalDate.parse(ngayThiInput.trim(), INPUT_DATE_FORMAT);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("Ngày thi phải đúng định dạng dd/MM/yyyy.");
        }
    }

    private String buildCommonFolderName(LocalDate ngayThi, int thuTuCa) {
        return ngayThi.format(FOLDER_DATE_FORMAT) + "_" + thuTuCa;
    }

    private List<PhanCongRow> loadPhanCongRows(Connection conn, int thuTuCa) throws Exception {
        List<PhanCongRow> rows = new ArrayList<>();

        String sql = """
                SELECT
                    pt.ma_phong,
                    cb1.ma_gv AS ma_gt1,
                    cb1.ho_ten AS ho_ten_gt1,
                    cb2.ma_gv AS ma_gt2,
                    cb2.ho_ten AS ho_ten_gt2
                FROM phan_cong_phong pcp
                JOIN ca_thi ct ON ct.id = pcp.ca_thi_id
                JOIN phong_thi pt ON pt.id = pcp.phong_thi_id
                JOIN can_bo cb1 ON cb1.id = pcp.giam_thi_1_id
                JOIN can_bo cb2 ON cb2.id = pcp.giam_thi_2_id
                WHERE ct.thu_tu = ?
                ORDER BY pt.id
                """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, thuTuCa);
            try (ResultSet rs = ps.executeQuery()) {
                int stt = 1;
                while (rs.next()) {
                    rows.add(new PhanCongRow(
                            stt++,
                            rs.getString("ma_phong"),
                            rs.getString("ma_gt1"),
                            rs.getString("ho_ten_gt1"),
                            rs.getString("ma_gt2"),
                            rs.getString("ho_ten_gt2")
                    ));
                }
            }
        }

        return rows;
    }

    private List<GiamSatRow> loadGiamSatRows(Connection conn, int thuTuCa) throws Exception {
        List<GiamSatRow> rows = new ArrayList<>();

        String sql = """
                SELECT
                    cb.ma_gv,
                    cb.ho_ten,
                    COALESCE(pcgs.mo_ta_pham_vi, CONCAT('Từ ', p1.ma_phong, ' đến ', p2.ma_phong)) AS pham_vi
                FROM phan_cong_giam_sat pcgs
                JOIN ca_thi ct ON ct.id = pcgs.ca_thi_id
                JOIN can_bo cb ON cb.id = pcgs.can_bo_id
                JOIN phong_thi p1 ON p1.id = pcgs.tu_phong_id
                JOIN phong_thi p2 ON p2.id = pcgs.den_phong_id
                WHERE ct.thu_tu = ?
                ORDER BY pcgs.id
                """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, thuTuCa);
            try (ResultSet rs = ps.executeQuery()) {
                int stt = 1;
                while (rs.next()) {
                    rows.add(new GiamSatRow(
                            stt++,
                            rs.getString("ma_gv"),
                            rs.getString("ho_ten"),
                            normalizePhamViGiamSat(rs.getString("pham_vi"))
                    ));
                }
            }
        }

        return rows;
    }

    private String normalizePhamViGiamSat(String value) {
        if (value == null) {
            return "";
        }

        String normalized = value.trim().replaceAll("\\s+", " ");
        normalized = normalized.replaceAll("(?i)\\btu\\b", "Từ phòng");
        normalized = normalized.replaceAll("(?i)\\bden\\b", "đến phòng");
        normalized = normalized.replaceAll("(?i)\\bphong\\b", "Phòng");
        return normalized;
    }

    private void createCell(Row row, int columnIndex, String value, CellStyle style) {
        Cell cell = row.createCell(columnIndex);
        cell.setCellValue(value == null ? "" : value);
        cell.setCellStyle(style);
    }

    private void createCell(Row row, int columnIndex, int value, CellStyle style) {
        Cell cell = row.createCell(columnIndex);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private static class Styles {
        private final CellStyle leftHeader;
        private final CellStyle rightHeader;
        private final CellStyle rightSubHeader;
        private final CellStyle title;
        private final CellStyle subTitle;
        private final CellStyle header;
        private final CellStyle normal;
        private final CellStyle normalCenter;
        private final CellStyle boldCenter;
        private final CellStyle italicCenter;

        private Styles(Workbook workbook) {
            Font fontNormal = workbook.createFont();
            fontNormal.setFontName("Times New Roman");
            fontNormal.setFontHeightInPoints((short) 12);

            Font fontBold = workbook.createFont();
            fontBold.setFontName("Times New Roman");
            fontBold.setBold(true);
            fontBold.setFontHeightInPoints((short) 12);

            Font fontTitle = workbook.createFont();
            fontTitle.setFontName("Times New Roman");
            fontTitle.setBold(true);
            fontTitle.setFontHeightInPoints((short) 15);

            Font fontItalic = workbook.createFont();
            fontItalic.setFontName("Times New Roman");
            fontItalic.setItalic(true);
            fontItalic.setFontHeightInPoints((short) 12);

            leftHeader = workbook.createCellStyle();
            leftHeader.setFont(fontBold);
            leftHeader.setAlignment(HorizontalAlignment.CENTER);
            leftHeader.setVerticalAlignment(VerticalAlignment.CENTER);
            leftHeader.setWrapText(true);

            rightHeader = workbook.createCellStyle();
            rightHeader.setFont(fontBold);
            rightHeader.setAlignment(HorizontalAlignment.CENTER);
            rightHeader.setVerticalAlignment(VerticalAlignment.CENTER);

            rightSubHeader = workbook.createCellStyle();
            rightSubHeader.setFont(fontBold);
            rightSubHeader.setAlignment(HorizontalAlignment.CENTER);
            rightSubHeader.setVerticalAlignment(VerticalAlignment.CENTER);

            title = workbook.createCellStyle();
            title.setFont(fontTitle);
            title.setAlignment(HorizontalAlignment.CENTER);
            title.setVerticalAlignment(VerticalAlignment.CENTER);

            subTitle = workbook.createCellStyle();
            subTitle.setFont(fontBold);
            subTitle.setAlignment(HorizontalAlignment.CENTER);
            subTitle.setVerticalAlignment(VerticalAlignment.CENTER);

            header = workbook.createCellStyle();
            header.setFont(fontBold);
            header.setAlignment(HorizontalAlignment.CENTER);
            header.setVerticalAlignment(VerticalAlignment.CENTER);
            header.setWrapText(true);
            header.setBorderTop(BorderStyle.THIN);
            header.setBorderBottom(BorderStyle.THIN);
            header.setBorderLeft(BorderStyle.THIN);
            header.setBorderRight(BorderStyle.THIN);

            normal = workbook.createCellStyle();
            normal.setFont(fontNormal);
            normal.setAlignment(HorizontalAlignment.LEFT);
            normal.setVerticalAlignment(VerticalAlignment.CENTER);
            normal.setBorderTop(BorderStyle.THIN);
            normal.setBorderBottom(BorderStyle.THIN);
            normal.setBorderLeft(BorderStyle.THIN);
            normal.setBorderRight(BorderStyle.THIN);
            normal.setWrapText(true);

            normalCenter = workbook.createCellStyle();
            normalCenter.cloneStyleFrom(normal);
            normalCenter.setAlignment(HorizontalAlignment.CENTER);

            boldCenter = workbook.createCellStyle();
            boldCenter.setFont(fontBold);
            boldCenter.setAlignment(HorizontalAlignment.CENTER);
            boldCenter.setVerticalAlignment(VerticalAlignment.CENTER);

            italicCenter = workbook.createCellStyle();
            italicCenter.setFont(fontItalic);
            italicCenter.setAlignment(HorizontalAlignment.CENTER);
            italicCenter.setVerticalAlignment(VerticalAlignment.CENTER);
        }
    }
}
