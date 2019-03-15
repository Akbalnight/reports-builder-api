package com.dias.services.reports.utils;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import org.apache.commons.lang3.time.FastDateFormat;

import java.math.BigDecimal;
import java.text.DecimalFormat;

/**
 * @author Kotelnikova Polina
 */
public class PdfExportUtils {
    private static final float PAGE_PADDING_LEFT = 20;
    private static final float PAGE_PADDING_RIGHT = 20;
    private static final Rectangle PAGE_FORMAT = PageSize.A4.rotate();
    private static final float PAGE_WIDTH = PAGE_FORMAT.getWidth();
    public static final float PAGE_VISIBLE_WIDTH = PAGE_WIDTH - PAGE_PADDING_LEFT - PAGE_PADDING_RIGHT;

    private static final FastDateFormat DAY_FORMAT = FastDateFormat.getInstance("dd.MM.yyyy");
    private static final FastDateFormat DATE_TIME_FORMAT = FastDateFormat.getInstance("dd.MM.yyyy HH:mm");

    public static final Font NORMAL_FONT = getFont(false, false, 8);
    public static final Font BOLD_FONT = getFont(true, false, 8);

    public static void writeHeaderSpacing(Document document) throws DocumentException {
        Chunk spacer = new Chunk("", getFont(false, false, 5));
        document.add(spacer);
    }

    private static Font getFont(boolean bold, boolean italic, int size) {
        String font;
        if (bold && italic) {
            font = "fonts/FreeSansBoldOblique.ttf";
        } else if (bold) {
            font = "fonts/FreeSansBold.ttf";
        } else if (italic) {
            font = "fonts/FreeSansOblique.ttf";
        } else {
            font = "fonts/FreeSans.ttf";
        }
        return FontFactory.getFont(font, BaseFont.IDENTITY_H, size);
    }

    public static void writeHeader(Document document, String text) throws DocumentException {
        Paragraph p = new Paragraph(text, getFont(true, false, 12));
        p.setAlignment(Paragraph.ALIGN_LEFT);
        document.add(p);
    }

    public static PdfPCell cell(String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, NORMAL_FONT));
        cell.setPadding(1);
        return cell;
    }

    public static PdfPCell cell(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font != null ? font : NORMAL_FONT));
        cell.setPadding(1);
        return cell;
    }

    public static PdfPCell cell(String text, int rowspan) {
        PdfPCell cell = new PdfPCell(new Phrase(text, NORMAL_FONT));
        cell.setPadding(1);
        cell.setRowspan(rowspan);
        return cell;
    }

    private static PdfPCell cell(String text, int padding, int colspan, int rowspan, boolean withoutBorder, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        if (withoutBorder) {
            cell.setBorder(0);
        }
        cell.setPadding(padding);
        cell.setColspan(colspan);
        cell.setRowspan(rowspan);
        return cell;
    }

    public static void writeHeaderTable(Document document, float[] relativeWidths, String[][] tableData) throws DocumentException {
        PdfPTable table = new PdfPTable(relativeWidths);
        table.setTotalWidth(PAGE_VISIBLE_WIDTH);
        table.setLockedWidth(true);
        //rowdata - строка
        for (String[] rowData : tableData) {
            //celldata - ячейка
            for (String cellData : rowData) {
                table.addCell(cell(cellData, 5, 1, 1, true, NORMAL_FONT));
            }
        }
        document.add(table);
    }

    public static void addCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = cell(text, font);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPaddingBottom(3);
        table.addCell(cell);
    }

    public static void addEmptyCellWithNoBorder(PdfPTable table, Font font) {
        PdfPCell cell = cell("", font);
        cell.setBorder(0);
        table.addCell(cell);
    }


    public static void addBigDecimalCell(PdfPTable table, Object cellValue, Font font, String nullSymbol) {
        PdfPCell cell;
        if (cellValue != null && !cellValue.toString().isEmpty()) {
            cell = cell(bigDecimalAsText(BigDecimal.valueOf(Double.valueOf(cellValue.toString()))), font);
        } else {
            cell = cell(nullSymbol, font);
        }
        cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPaddingBottom(3);
        table.addCell(cell);
    }

    public static void addIntegerCell(PdfPTable table, Integer number, Font font) {
        PdfPCell cell = cell(Integer.toString(number), font);
        cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPaddingBottom(3);
        table.addCell(cell);
    }

    private static String bigDecimalAsText(BigDecimal value) {
        DecimalFormat decimalFormat = new DecimalFormat();
        decimalFormat.setMaximumFractionDigits(2);
        decimalFormat.setMinimumFractionDigits(2);
        decimalFormat.setGroupingUsed(false);

        BigDecimal scaled = value.setScale(2, BigDecimal.ROUND_HALF_EVEN);
        return decimalFormat.format(scaled);
    }
}
