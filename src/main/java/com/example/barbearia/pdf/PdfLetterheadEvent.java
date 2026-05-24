package com.example.barbearia.pdf;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Cabeçalho, rodapé e moldura timbrada em todas as páginas. */
public class PdfLetterheadEvent extends PdfPageEventHelper {

    private static final float HEADER_H = 86f;
    private static final float FOOTER_H = 46f;
    private static final DateTimeFormatter GERADO_EM =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final String reportLabel;
    private Image logo;
    private PdfTemplate totalTemplate;
    private BaseFont baseFont;

    public PdfLetterheadEvent(String reportLabel) {
        this.reportLabel = reportLabel != null ? reportLabel : PdfBrand.NAME;
    }

    @Override
    public void onOpenDocument(PdfWriter writer, Document document) {
        try {
            logo = PdfLogoImage.create(54f);
            baseFont = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
            totalTemplate = writer.getDirectContent().createTemplate(28, 14);
        } catch (Exception e) {
            throw new RuntimeException("Falha ao preparar timbrado PDF", e);
        }
    }

    @Override
    public void onStartPage(PdfWriter writer, Document document) {
        PdfPageFrame.draw(writer, HEADER_H, FOOTER_H);
    }

    @Override
    public void onEndPage(PdfWriter writer, Document document) {
        Rectangle page = document.getPageSize();
        float w = page.getWidth();
        float h = page.getHeight();
        float side = PdfBrand.SIDE_BAR_W;
        PdfContentByte cb = writer.getDirectContent();

        cb.saveState();

        cb.setColorFill(PdfBrand.BG_HEADER);
        cb.rectangle(side, h - HEADER_H, w - 2 * side, HEADER_H);
        cb.fill();

        cb.setColorFill(PdfBrand.ACCENT);
        cb.rectangle(side, h - HEADER_H, w - 2 * side, 3);
        cb.fill();

        cb.setColorStroke(PdfBrand.ACCENT_BRIGHT);
        cb.setLineWidth(1f);
        cb.moveTo(side, h - HEADER_H);
        cb.lineTo(w - side, h - HEADER_H);
        cb.stroke();

        try {
            logo.setAbsolutePosition(side + 14, h - HEADER_H + 16);
            cb.addImage(logo);
        } catch (DocumentException ignored) {
            /* opcional */
        }

        float textX = side + 78;
        ColumnText.showTextAligned(cb, Element.ALIGN_LEFT, new Phrase(PdfBrand.NAME, brandTitleFont()), textX, h - 36, 0);
        ColumnText.showTextAligned(cb, Element.ALIGN_LEFT, new Phrase(reportLabel, reportFont()), textX, h - 52, 0);
        ColumnText.showTextAligned(
                cb,
                Element.ALIGN_RIGHT,
                new Phrase(PdfBrand.TAGLINE, taglineFont()),
                w - side - 12,
                h - 44,
                0
        );

        cb.setColorFill(PdfBrand.BG_FOOTER);
        cb.rectangle(side, 0, w - 2 * side, FOOTER_H);
        cb.fill();

        cb.setColorFill(PdfBrand.ACCENT);
        cb.rectangle(side, FOOTER_H - 2, w - 2 * side, 2);
        cb.fill();

        String gerado = "Gerado em " + GERADO_EM.format(LocalDateTime.now());
        ColumnText.showTextAligned(cb, Element.ALIGN_LEFT, new Phrase(gerado, footerFont()), side + 14, 16, 0);
        ColumnText.showTextAligned(
                cb,
                Element.ALIGN_CENTER,
                new Phrase(PdfBrand.NAME + " · confidencial", footerFont()),
                w / 2,
                16,
                0
        );

        String pageText = "Página " + writer.getPageNumber() + " / ";
        float textBase = baseFont.getWidthPoint(pageText, 8);
        cb.beginText();
        cb.setFontAndSize(baseFont, 8);
        cb.setColorFill(PdfBrand.TEXT_ON_DARK);
        cb.setTextMatrix(w - side - 14 - textBase, 16);
        cb.showText(pageText);
        cb.endText();
        cb.addTemplate(totalTemplate, w - side - 14, 16);

        cb.restoreState();
    }

    @Override
    public void onCloseDocument(PdfWriter writer, Document document) {
        totalTemplate.beginText();
        totalTemplate.setFontAndSize(baseFont, 8);
        totalTemplate.setColorFill(PdfBrand.TEXT_ON_DARK);
        totalTemplate.showText(String.valueOf(writer.getPageNumber() - 1));
        totalTemplate.endText();
    }

    public static Document newDocument() {
        float side = PdfBrand.SIDE_BAR_W;
        float pad = 18f;
        return new Document(
                PageSize.A4,
                side + pad,
                side + pad,
                HEADER_H + pad + 8,
                FOOTER_H + pad
        );
    }

    public static void addCoverBlock(Document doc, String title, String... metaLines) throws DocumentException {
        Paragraph titleP = new Paragraph(title, PdfFonts.title());
        titleP.setSpacingAfter(6f);
        doc.add(titleP);

        for (String line : metaLines) {
            if (line != null && !line.isBlank()) {
                Paragraph meta = new Paragraph(line, PdfFonts.meta());
                meta.setSpacingAfter(4f);
                doc.add(meta);
            }
        }

        Paragraph rule = new Paragraph(" ");
        rule.setSpacingAfter(14f);
        doc.add(rule);

        PdfPTable line = new PdfPTable(1);
        line.setWidthPercentage(100);
        PdfPCell c = new PdfPCell();
        c.setFixedHeight(3f);
        c.setBackgroundColor(PdfBrand.ACCENT);
        c.setBorder(Rectangle.NO_BORDER);
        line.addCell(c);
        line.setSpacingAfter(16f);
        doc.add(line);
    }

    private static Font brandTitleFont() {
        return PdfFonts.brandHeader();
    }

    private static Font reportFont() {
        return PdfFonts.reportSubtitle();
    }

    private static Font taglineFont() {
        return PdfFonts.tagline();
    }

    private static Font footerFont() {
        return PdfFonts.footer();
    }
}
