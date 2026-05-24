package com.example.barbearia.pdf;

import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;

import java.awt.Color;

/** Fontes PDF com cores explícitas (legíveis no OpenPDF 2.x). */
public final class PdfFonts {

    private PdfFonts() {
    }

    public static Font title() {
        return FontFactory.getFont(FontFactory.HELVETICA_BOLD, 17, PdfBrand.TEXT_PRIMARY);
    }

    public static Font meta() {
        return FontFactory.getFont(FontFactory.HELVETICA, 10, PdfBrand.TEXT_MUTED);
    }

    public static Font body() {
        return FontFactory.getFont(FontFactory.HELVETICA, 9, PdfBrand.TEXT_PRIMARY);
    }

    public static Font tableHeader() {
        return FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, PdfBrand.TEXT_ON_DARK);
    }

    public static Font section() {
        return FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, PdfBrand.ACCENT_DARK);
    }

    public static Font brandHeader() {
        return FontFactory.getFont(FontFactory.HELVETICA_BOLD, 15, PdfBrand.ACCENT);
    }

    public static Font reportSubtitle() {
        return FontFactory.getFont(FontFactory.HELVETICA, 9, PdfBrand.TEXT_ON_DARK);
    }

    public static Font tagline() {
        return FontFactory.getFont(FontFactory.HELVETICA, 7, new Color(140, 175, 155));
    }

    public static Font footer() {
        return FontFactory.getFont(FontFactory.HELVETICA, 8, PdfBrand.TEXT_ON_DARK);
    }
}
