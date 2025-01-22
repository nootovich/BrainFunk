package debugger;

import BrainFunk.Interpreter;
import BrainFunk.Token;
import java.awt.*;
import nootovich.nglib.*;

import static BrainFunk.Interpreter.tokens;
import static debugger.Debugger.WINDOW_HEIGHT;
import static debugger.Debugger.WINDOW_WIDTH;

public class DebuggerRenderer extends NGRenderer {

    public static NGVec4i areaCode;
    public static NGVec2i areaPadding;
    public static NGVec4i areaTape;

    public static int codeOffsetY = 0;
    public static int cachedFontW;
    public static int cachedFontH;
    public static int cachedLinesToBottom;

    public static Token mouseToken = null;

    public static String[] filedata = {""};

    public static MODE mode = MODE.NORMAL;

    public enum MODE {NORMAL, TOKEN_LIST}

    static {
        font        = new Font(Font.MONOSPACED, Font.PLAIN, 17);
        areaPadding = new NGVec2i(WINDOW_WIDTH >> 6, WINDOW_HEIGHT >> 5);
        areaCode    = new NGVec4i(
            areaPadding.x(),
            areaPadding.y(),
            WINDOW_HEIGHT - areaPadding.y() * 2,
            WINDOW_WIDTH * 0.8f
        );
        areaTape    = new NGVec4i(
            areaCode.x() + areaCode.w() + areaPadding.x(),
            areaCode.y(),
            areaCode.h(),
            WINDOW_WIDTH - (areaCode.x() + areaCode.w() + areaPadding.x() * 2)
        );
    }

    public enum colorEnum { // TODO: move into NGColors
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

        // BG
        {
            g.drawRect(0, 0, WINDOW_WIDTH, WINDOW_HEIGHT, colors[colorEnum.COLOR_BG.ordinal()]);
            g.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_BEVEL));
            g.drawRectBorder(areaCode.x(), areaCode.y(), areaCode.w(), areaCode.h(), colors[colorEnum.COLOR_DATA.ordinal()]);
            g.drawRectBorder(areaTape.x(), areaTape.y(), areaTape.w(), areaTape.h(), colors[colorEnum.COLOR_SELECT.ordinal()]);
            g.setStroke(new BasicStroke(1));
        }

        if (Interpreter.finished) {
            String execFinished = "Execution finished, press \"SPACE\" to restart";
            g.drawTextCentered(
                execFinished,
                areaCode.x() + areaCode.w() / 2,
                areaCode.x() + areaCode.h() + areaPadding.y() / 2,
                colors[colorEnum.COLOR_HIGHLIGHT.ordinal()]
            );
        }

        // Memory values
        {
            for (int y = 0; y < Interpreter.tape.length / 8 && y < areaTape.h() / cachedFontH; y++) {
                for (int x = 0; x < 8; x++) {
                    int cx = x * cachedFontW * 3 + areaTape.x() + areaPadding.x() / 3;
                    int cy = (y + 1) * cachedFontH + areaTape.y();
                    g.drawText(hex(Interpreter.tape[y * 8 + x]), cx, cy, colors[colorEnum.COLOR_TEXT.ordinal()]);
                    if (y * 8 + x == Interpreter.pointer) {
                        g.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_BEVEL));
                        g.drawRectBorder(cx, cy - cachedFontH + 5, cachedFontW * 2, cachedFontH, colors[colorEnum.COLOR_HIGHLIGHT.ordinal()]);
                        g.setStroke(new BasicStroke(1));
                    }
                }
            }
        }

        // Program
        {
            if (mode == MODE.NORMAL) {
                g.setClip(areaCode.x(), areaCode.y(), areaCode.w(), areaCode.h());
                int y = areaCode.y() + cachedFontH - codeOffsetY;
                for (int i = 0; i < filedata.length; i++) {
                    g.drawText(filedata[i], areaCode.x() + areaPadding.w(), y + areaPadding.h(), colors[colorEnum.COLOR_TEXT.ordinal()]);
                    y += cachedFontH;
                }
            } else if (mode == MODE.TOKEN_LIST) {
                g.resetClip();
                for (int i = 0; i < tokens.length; i++) {
                    Token t = tokens[i];
                    int   x = areaCode.x() + areaPadding.x();
                    int   y = areaCode.y() + areaPadding.y() + cachedFontH * (i + 1) - codeOffsetY;
                    g.drawText(t.toString(), x, y, colors[colorEnum.COLOR_TEXT.ordinal()]);
                }
            } else NGUtils.error("Not implemented");
        }

        // Current token outline
        if (!Interpreter.finished && tokens.length > 0) {
            g.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_BEVEL));
            Token tk    = tokens[Interpreter.ip];
            int   prevX = Integer.MIN_VALUE;
            int   prevY = Integer.MIN_VALUE;
            while (tk != null) {
                int ipX = areaCode.x() + areaPadding.w() + tk.col * cachedFontW;
                int ipY = areaCode.y() + areaPadding.h() + tk.row * cachedFontH - codeOffsetY + 5;
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

        // Token under mouse outline
        if (mouseToken != null) {
            int ipX, ipY, ipW, ipH;
            ipX = areaCode.x() + areaPadding.w() + mouseToken.col * cachedFontW;
            ipY = areaCode.y() + areaPadding.h() + mouseToken.row * cachedFontH - codeOffsetY + 5;
            ipW = cachedFontW * mouseToken.len();
            ipH = cachedFontH;
            g.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_BEVEL));
            g.drawRectBorder(ipX, ipY, ipW, ipH, colors[colorEnum.COLOR_SELECT.ordinal()]);
        }
        g.resetClip();

        g.drawRect(Debugger.BUTTON_TOKEN_LIST, Color.RED);
    }

    public void reset() { }

    public static void findMouseToken(NGVec2i pos) {
        mouseToken = getTokenByRowCol(
            tokens,
            pos.sub(areaCode.x(), areaCode.y())
               .sub(areaPadding)
               .add(0, codeOffsetY)
               .divide(cachedFontW, cachedFontH)
        );
    }

    private static Token getTokenByRowCol(Token[] array, NGVec2i pos) {
        for (Token t: array) {
            if (t.row == pos.y() && t.col <= pos.x() && t.col + t.len() > pos.x()) {
                return t;
            }
        }
        return null;
    }

    private static String hex(byte n) {
        return "%02X".formatted(n);
    }
}
