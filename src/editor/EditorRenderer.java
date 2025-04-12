package editor;

import java.awt.*;
import nootovich.nglib.*;

import static editor.Editor.*;

public class EditorRenderer extends NGRenderer {

    public static final Color COLOR_BG       = new Color(0x153243);
    public static final Color COLOR_OUTLINE  = new Color(0x2A6486);
    public static final Color COLOR_CUR_LINE = new Color(0x284B63);

    public static int borderPadding = 10;

    static {
        font = new Font(Font.MONOSPACED, Font.PLAIN, 17);
    }

    @Override
    public void render(NGGraphics g) {
        FontMetrics metrics    = g.g2d.getFontMetrics();
        final int   fontHeight = metrics.getHeight();
        final int   fontWidth  = metrics.charWidth('@');

        NGVec4i screen = new NGVec4i(0, 0, h, w);
        g.drawRect(screen, COLOR_BG);

        g.drawRect(borderPadding, fontHeight * (line + 1), w - borderPadding * 2, fontHeight, COLOR_CUR_LINE);
        String[] codeLines = getLines();
        for (int i = 0; i < codeLines.length; i++) {
            g.drawText(codeLines[i], new NGVec2i(borderPadding * 2).addY(fontHeight * (i + 1)), Color.WHITE);
        }
        g.drawRect(borderPadding * 2 + fontWidth * (cursor - bol), fontHeight * (line + 1), 2, fontHeight, Color.WHITE);

        g.drawRectBorder(screen.xy().add(borderPadding), screen.wh().sub(borderPadding * 2), COLOR_OUTLINE, 3);
    }

    @Override
    public void reset() {

    }
}
