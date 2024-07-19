package debugger;

import BrainFunk.Interpreter;
import BrainFunk.Token;
import nootovich.nglib.NGGraphics;
import nootovich.nglib.NGRenderer;
import nootovich.nglib.NGVec2i;

import java.awt.*;

public class DebuggerRenderer extends NGRenderer {

    protected static int w = 1400;
    protected static int h = 785;

    protected static final int codeX    = w / 40;
    protected static final int codeY    = codeX;
    protected static final int codeW    = w * 75 / 100;
    protected static final int codeH    = h - codeY * 2;
    protected static final int codePadH = 16;
    protected static final int codePadV = 4;
    protected static final int tapeY    = codeY;
    protected static final int tapeH    = codeH;
    protected static final int tapeW    = w - codeX * 3 - codeW;
    protected static final int tapeX    = w - codeX - tapeW;

    protected static int codeOffsetY = 0;
    protected static int cachedFontW;
    protected static int cachedFontH;
    protected static int cachedLinesToBottom;

    protected static Token mouseToken = null;

    protected static String[] filedata = {""};

    private enum colorEnum { // TODO: move into NGColors
        COLOR_BG, COLOR_DATA, COLOR_TEXT, COLOR_TEXT_FADED, COLOR_HIGHLIGHT, COLOR_SELECT, COLOR_CONNECTION
    }

    private static final Color[] colors = new Color[]{
            new Color(0x153243),
            new Color(0x284B63),
            new Color(0xFFFFFF),
            new Color(0x7EA8BE),
            new Color(0x5EF38C),
            new Color(0xFFC69B),
            new Color(0xD9E985)
    };

    @Override
    public void render(NGGraphics g) {
        g.drawRect(0, 0, w, h, colors[colorEnum.COLOR_BG.ordinal()]);

        g.drawRect(codeX, codeY, codeW, codeH, colors[colorEnum.COLOR_DATA.ordinal()]);
        g.drawRect(tapeX, tapeY, tapeW, tapeH, colors[colorEnum.COLOR_DATA.ordinal()]);

        if (Interpreter.finished) {
            String execFinished = "Execution finished, press \"SPACE\" to restart";
            g.drawText(execFinished, codeX + codeW / 2 - (cachedFontW * execFinished.length() / 2), codeY + codeH + cachedFontH, colors[colorEnum.COLOR_HIGHLIGHT.ordinal()]);
        }

        // Memory values
        {
            g.setClip(tapeX, tapeY, tapeW, tapeH);
            int x    = tapeX;
            int y    = tapeY + cachedFontH;
            int valW = cachedFontW * 3;
            for (int i = 0; i < Interpreter.tape.length && y < tapeY + tapeH; i++) {
                String val = hex(Interpreter.tape[i]);
                g.drawText(val, x + 8, y, colors[colorEnum.COLOR_TEXT.ordinal()]);
                if (i == Interpreter.pointer) {
                    g.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_BEVEL));
                    g.drawRectBorder(x + 8, y - cachedFontH + 4, cachedFontW * 2, cachedFontH, colors[colorEnum.COLOR_HIGHLIGHT.ordinal()]);
                    g.setStroke(new BasicStroke(1));
                }
                x += valW;
                if (x >= w - codeX - valW) {
                    x = tapeX;
                    y += cachedFontH;
                }
            }
        }

        // Program
        {
            g.setClip(codeX, codeY, codeW, codeH);
            int y = codeY + cachedFontH - codeOffsetY;
            for (int i = 0; i < filedata.length; i++) {
                g.drawText(filedata[i], codeX + codePadH, y + codePadV, /*unfolding ? colors[colorEnum.COLOR_TEXT_FADED.ordinal()] :*/ colors[colorEnum.COLOR_TEXT.ordinal()]);
                y += cachedFontH;
            }
        }

        // Current token outline
        if (!Interpreter.finished && Interpreter.tokens.length > 0) {
            g.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_BEVEL));
            Token tk    = Interpreter.tokens[Interpreter.ip];
            int   prevX = Integer.MIN_VALUE;
            int   prevY = Integer.MIN_VALUE;
            while (tk != null) {
                int ipX = codeX + codePadH + tk.col * cachedFontW;
                int ipY = codeY + codePadV + tk.row * cachedFontH - codeOffsetY + 5;
                int ipW = tk.len() * cachedFontW;
                int ipH = cachedFontH;
                g.drawRectBorder(ipX, ipY, ipW, ipH, colors[colorEnum.COLOR_HIGHLIGHT.ordinal()]);
                g.setStroke(new BasicStroke(1));

                // TODO: maybe a pretty drawArc()?)

                if (prevX != Integer.MIN_VALUE && prevY != Integer.MIN_VALUE) {
                    g.drawLine(ipX + ipW / 2, ipY + ipH / 2, prevX, prevY, colors[colorEnum.COLOR_CONNECTION.ordinal()]);
                }

                prevX = ipX + ipW / 2;
                prevY = ipY + ipH / 2;
                tk    = tk.origin;
            }
        }

//            // Unfolding window
//            if (unfolding) {
//                String[] uData = unfoldedData.split("\n", -1);
//                int      w     = 0;
//                int      h     = uData.length * cachedFontH;
//                for (String s : uData) {
//                    int len = s.length() * cachedFontW;
//                    if (len > w) w = len;
//                }
//                int x = codeX / 2 + codeW / 2 - w / 2;
//                int y = codeY / 2 + codeH / 2 - h / 2;
//                g.drawRoundRect(x, y, w + codeX, h + codeY, 5, 5, colors[colorEnum.COLOR_DATA.ordinal()]);
//                g.drawRoundRectBorder(x, y, w + codeX, h + codeY, 5, 5, Color.WHITE);
//                for (int i = 0; i < uData.length; i++) {
//                    g.drawText(uData[i], x + unfoldedDataPad, y + unfoldedDataPad + cachedFontH, Color.WHITE);
//                    y += cachedFontH;
//                }
//            }

        // Token under mouse outline
        if (mouseToken != null) {
            int ipX, ipY, ipW, ipH;
//                if (unfolding) {
//                    int      ufw   = 0;
//                    String[] uData = unfoldedData.split("\n", -1);
//                    for (String s : uData) {
//                        int len = s.length() * cachedFontW;
//                        if (len > ufw) ufw = len;
//                    }
//                    ipX = (int) (codeX + codeW / 2.f - ufw / 2.f + mouseToken.col * cachedFontW) - 2;
//                    ipY = (int) (codeX + codeH / 2.f + (-uData.length / 2.f + mouseToken.row) * cachedFontH);
//                } else {
            ipX = codeX + codePadH + mouseToken.col * cachedFontW;
            ipY = codeY + codePadV + mouseToken.row * cachedFontH - codeOffsetY + 5;
//                }
            ipW = cachedFontW * mouseToken.len();
            ipH = cachedFontH;
            g.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_BEVEL));
            g.drawRectBorder(ipX, ipY, ipW, ipH, colors[colorEnum.COLOR_SELECT.ordinal()]);
        }
        g.resetClip();
    }

    protected static void findMouseToken(NGVec2i pos) {
//        if (unfolding) {
//
//            // FIXME: wrong col pos (and most likely everything)
//            int unfoldingWindowX = codeX / 2 + codeW / 2 - cachedUnfoldedDataWidth / 2; // -8
//            int unfoldingWindowY = codeY / 2 + codeH / 2 - cachedUnfoldedDataHeight / 2;// -30
//            int row              = (mouseY - unfoldingWindowY - unfoldedDataPad - cachedFontH) / cachedFontH;
//            int col              = (mouseX - unfoldingWindowX - unfoldedDataPad) / cachedFontW;
//            mouseToken = getTokenByRowCol(unfoldedTokens, row, col);
//        } else {
        int row = (pos.y - codeY - codePadH + codeOffsetY) / cachedFontH - 1;
        int col = (pos.x - codeX - codePadH) / cachedFontW;
        mouseToken = getTokenByRowCol(Interpreter.tokens, row, col);
//        }
    }

    private static Token getTokenByRowCol(Token[] array, int row, int col) {
        for (Token t : array) if (t.row == row && t.col <= col && t.col + t.len() >= col) return t;
        return null;
    }

    private static String hex(byte n) {
        return "%02X".formatted(n);
    }

}
