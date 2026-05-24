package com.example.barbearia.pdf;

import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;

/** Tabelas com cabeçalho e linhas zebradas no estilo da marca. */
public final class PdfStyledTable {

    private PdfStyledTable() {
    }

    public static Paragraph sectionTitle(String text) {
        Paragraph p = new Paragraph(text, PdfFonts.section());
        p.setSpacingBefore(10f);
        p.setSpacingAfter(8f);
        return p;
    }

    public static PdfPTable create(int columns, float widthPercent) {
        PdfPTable table = new PdfPTable(columns);
        table.setWidthPercentage(widthPercent);
        table.setSpacingAfter(12f);
        table.setHeaderRows(1);
        return table;
    }

    public static void addHeaderRow(PdfPTable table, String... labels) {
        for (String label : labels) {
            table.addCell(headerCell(label));
        }
    }

    public static void addDataRow(PdfPTable table, int rowIndex, String... values) {
        for (String value : values) {
            table.addCell(dataCell(value, rowIndex % 2 == 1));
        }
    }

    public static void addKeyValueRow(PdfPTable table, int rowIndex, String key, String value) {
        table.addCell(dataCell(key, rowIndex % 2 == 1));
        table.addCell(dataCell(value, rowIndex % 2 == 1));
    }

    private static PdfPCell headerCell(String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "", PdfFonts.tableHeader()));
        cell.setBackgroundColor(PdfBrand.ACCENT_DARK);
        cell.setPadding(8);
        cell.setBorderColor(PdfBrand.ACCENT);
        cell.setBorderWidth(0.5f);
        cell.setHorizontalAlignment(PdfPCell.ALIGN_LEFT);
        return cell;
    }

    private static PdfPCell dataCell(String text, boolean alt) {
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "", PdfFonts.body()));
        cell.setBackgroundColor(alt ? PdfBrand.ROW_ALT : PdfBrand.ROW_BASE);
        cell.setPadding(7);
        cell.setBorderColor(PdfBrand.BORDER_SOFT);
        cell.setBorderWidth(0.35f);
        cell.setHorizontalAlignment(PdfPCell.ALIGN_LEFT);
        return cell;
    }
}
