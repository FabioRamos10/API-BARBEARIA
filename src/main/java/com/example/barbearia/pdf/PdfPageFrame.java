package com.example.barbearia.pdf;

import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfWriter;

/** Faixas laterais pretas, bordas verdes e área central do papel. */
public final class PdfPageFrame {

    private PdfPageFrame() {
    }

    public static void draw(PdfWriter writer, float headerH, float footerH) {
        Rectangle page = writer.getPageSize();
        float w = page.getWidth();
        float h = page.getHeight();
        float side = PdfBrand.SIDE_BAR_W;

        PdfContentByte under = writer.getDirectContentUnder();

        under.saveState();

        under.setColorFill(PdfBrand.BLACK_DEEP);
        under.rectangle(0, 0, w, h);
        under.fill();

        under.setColorFill(PdfBrand.PAPER);
        under.rectangle(side, footerH, w - 2 * side, h - headerH - footerH);
        under.fill();

        under.setColorFill(PdfBrand.PAPER_INNER);
        under.rectangle(side + 6, footerH + 6, w - 2 * (side + 6), h - headerH - footerH - 12);
        under.fill();

        under.setColorFill(PdfBrand.BLACK_BAR);
        under.rectangle(0, 0, side, h);
        under.fill();
        under.rectangle(w - side, 0, side, h);
        under.fill();

        under.setColorFill(PdfBrand.ACCENT);
        under.rectangle(side - 3, footerH, 3, h - headerH - footerH);
        under.fill();
        under.rectangle(w - side, footerH, 3, h - headerH - footerH);
        under.fill();

        under.setColorStroke(PdfBrand.ACCENT);
        under.setLineWidth(PdfBrand.FRAME_BORDER);
        under.rectangle(side + 1, footerH + 1, w - 2 * side - 2, h - headerH - footerH - 2);
        under.stroke();

        under.setColorStroke(PdfBrand.ACCENT_BRIGHT);
        under.setLineWidth(0.8f);
        under.rectangle(side + 5, footerH + 5, w - 2 * side - 10, h - headerH - footerH - 10);
        under.stroke();

        under.restoreState();
    }
}
