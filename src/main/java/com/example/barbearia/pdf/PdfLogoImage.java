package com.example.barbearia.pdf;

import com.lowagie.text.Image;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

/** Logo da marca para cabeçalho PDF (alta resolução). */
public final class PdfLogoImage {

    private PdfLogoImage() {
    }

    public static Image create(float heightPt) throws Exception {
        int px = 200;
        BufferedImage img = new BufferedImage(px, px, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        g.setColor(new Color(8, 10, 12));
        g.fillRect(0, 0, px, px);

        g.setColor(new Color(12, 18, 16));
        g.fill(new RoundRectangle2D.Float(10, 10, px - 20, px - 20, 36, 36));

        g.setColor(new Color(0, 255, 156));
        g.setStroke(new BasicStroke(2.5f));
        g.draw(new RoundRectangle2D.Float(12, 12, px - 24, px - 24, 32, 32));

        g.setColor(new Color(57, 255, 20));
        g.setStroke(new BasicStroke(6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Line2D.Float(58, 52, 78, 108));
        g.draw(new Line2D.Float(142, 52, 122, 108));
        g.draw(new Line2D.Float(78, 128, 122, 128));

        g.setColor(new Color(8, 10, 12));
        g.fill(new Ellipse2D.Float(48, 44, 26, 26));
        g.fill(new Ellipse2D.Float(126, 44, 26, 26));
        g.setColor(new Color(57, 255, 20));
        g.setStroke(new BasicStroke(3.5f));
        g.draw(new Ellipse2D.Float(48, 44, 26, 26));
        g.draw(new Ellipse2D.Float(126, 44, 26, 26));

        g.setFont(new Font("SansSerif", Font.BOLD, 28));
        g.setColor(new Color(0, 255, 156));
        FontMetrics fm = g.getFontMetrics();
        String obs = "OBS";
        g.drawString(obs, (px - fm.stringWidth(obs)) / 2f, 168);

        g.setFont(new Font("SansSerif", Font.PLAIN, 11));
        g.setColor(new Color(122, 154, 138));
        String sub = "BARBER";
        g.drawString(sub, (px - g.getFontMetrics().stringWidth(sub)) / 2f, 184);

        g.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "png", baos);
        Image image = Image.getInstance(baos.toByteArray());
        image.scaleToFit(heightPt, heightPt);
        return image;
    }
}
