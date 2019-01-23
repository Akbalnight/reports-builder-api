package com.dias.services.reports.utils;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * @author Kotelnikova Polina
 */
public class ExcelExportUtils {

    public static XSSFFont createFont(XSSFWorkbook workbook, short height, String name) {
        XSSFFont font = workbook.createFont();
        font.setFontHeightInPoints(height);
        font.setFontName(name);
        return font;
    }

    public static XSSFFont createFont(XSSFWorkbook workbook, short height, String name, boolean bold) {
        XSSFFont font = createFont(workbook, height, name);
        font.setBold(bold);
        return font;
    }

    public static XSSFCellStyle createCellStyle(XSSFWorkbook workbook, Font font) {
        XSSFCellStyle style = workbook.createCellStyle();
        style.setFont(font);
        return style;
    }

    public static XSSFCellStyle createCellStyle(XSSFWorkbook workbook, Font font, short format) {
        XSSFCellStyle style = createCellStyle(workbook, font);
        style.setDataFormat(format);
        return style;
    }

    public static Cell createCell(XSSFRow row, int cellNum, CellStyle style) {
        Cell cell = row.createCell(cellNum);
        cell.setCellStyle(style);
        return cell;
    }

    public static Cell createCell(XSSFRow row, int cellNum, CellStyle style, String value) {
        Cell cell = createCell(row, cellNum, style);
        cell.setCellValue(value);
        return cell;
    }
}