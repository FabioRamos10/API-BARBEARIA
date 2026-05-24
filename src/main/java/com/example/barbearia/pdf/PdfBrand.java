package com.example.barbearia.pdf;

import java.awt.Color;

/** Identidade visual Old Barber Street para exportações PDF. */
public final class PdfBrand {

    public static final String NAME = "Old Barber Street";
    public static final String TAGLINE = "Agendamentos · Comissões · Relatórios";

    public static final Color BLACK_DEEP = new Color(5, 5, 8);
    public static final Color BLACK_BAR = new Color(12, 15, 18);
    public static final Color BG_HEADER = BLACK_BAR;
    public static final Color BG_FOOTER = BLACK_BAR;
    public static final Color ACCENT = new Color(0, 255, 156);
    public static final Color ACCENT_BRIGHT = new Color(57, 255, 20);
    public static final Color ACCENT_DARK = new Color(20, 48, 38);
    public static final Color PAPER = new Color(252, 255, 253);
    public static final Color PAPER_INNER = new Color(241, 248, 244);
    public static final Color TEXT_PRIMARY = new Color(22, 32, 28);
    public static final Color TEXT_MUTED = new Color(72, 98, 86);
    public static final Color TEXT_ON_DARK = new Color(232, 255, 244);
    public static final Color ROW_ALT = new Color(236, 248, 242);
    public static final Color ROW_BASE = Color.WHITE;
    public static final Color BORDER_SOFT = new Color(180, 210, 195);

    public static final float SIDE_BAR_W = 22f;
    public static final float FRAME_BORDER = 2.5f;

    private PdfBrand() {
    }
}
