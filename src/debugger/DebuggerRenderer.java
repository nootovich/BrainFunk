package debugger;

import BrainFunk.Interpreter;
import BrainFunk.Token;
import java.awt.*;
import nootovich.nglib.*;

import static nootovich.nglib.NGMain.h;
import static nootovich.nglib.NGMain.w;

public class DebuggerRenderer extends NGRenderer {

    public static NGVec4i CODE;
    public static NGVec2i CODE_PAD;
    public static NGVec4i TAPE;

    protected static int codeOffsetY = 0;
    protected static int cachedFontW;
    protected static int cachedFontH;
    protected static int cachedLinesToBottom;

    protected static Token mouseToken = null;

    protected static String[] filedata = {""};

    public DebuggerRenderer(Container c) {
        super(c);
        font     = new Font(Font.MONOSPACED, Font.PLAIN, 17);
        CODE     = new NGVec4i(w / 40, w / 40, (int) (w * 0.75f), h - w / 20);
        CODE_PAD = new NGVec2i(16, 4);
        TAPE     = new NGVec4i(w - CODE.x() - (w - CODE.x() * 3 - CODE.width()), CODE.y(), w - CODE.x() * 3 - CODE.width(), CODE.height());
    }

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
    public void render() {
        drawRect(0, 0, w, h, colors[colorEnum.COLOR_BG.ordinal()]);

        drawRect(CODE.x(), CODE.y(), CODE.width(), CODE.height(), colors[colorEnum.COLOR_DATA.ordinal()]);
        drawRect(TAPE.x(), TAPE.y(), TAPE.width(), TAPE.height(), colors[colorEnum.COLOR_DATA.ordinal()]);

        if (Interpreter.finished) {
            String execFinished = "Execution finished, press \"SPACE\" to restart";
            drawText(execFinished, CODE.x() + CODE.width() / 2 - (cachedFontW * execFinished.length() / 2), CODE.y() + CODE.height() + cachedFontH, colors[colorEnum.COLOR_HIGHLIGHT.ordinal()]);
        }

        // Memory values
        {
            setClip(TAPE.x(), TAPE.y(), TAPE.width(), TAPE.height());
            int x    = TAPE.x();
            int y    = TAPE.y() + cachedFontH;
            int valW = cachedFontW * 3;
            for (int i = 0; i < Interpreter.tape.length && y < TAPE.y() + TAPE.height(); i++) {
                String val = hex(Interpreter.tape[i]);
                drawText(val, x + 8, y, colors[colorEnum.COLOR_TEXT.ordinal()]);
                if (i == Interpreter.pointer) {
                    setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_BEVEL));
                    drawRectBorder(x + 8, y - cachedFontH + 4, cachedFontW * 2, cachedFontH, colors[colorEnum.COLOR_HIGHLIGHT.ordinal()]);
                    setStroke(new BasicStroke(1));
                }
                x += valW;
                if (x >= w - CODE.x() - valW) {
                    x = TAPE.x();
                    y += cachedFontH;
                }
            }
        }

        // Program
        {
            setClip(CODE.x(), CODE.y(), CODE.width(), CODE.height());
            int y = CODE.y() + cachedFontH - codeOffsetY;
            for (int i = 0; i < filedata.length; i++) {
                drawText(filedata[i], CODE.x() + CODE_PAD.width(), y + CODE_PAD.height(), /*unfolding ? colors[colorEnum.COLOR_TEXT_FADED.ordinal()] :*/ colors[colorEnum.COLOR_TEXT.ordinal()]);
                y += cachedFontH;
            }
        }

        // Current token outline
        if (!Interpreter.finished && Interpreter.tokens.length > 0) {
            setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_BEVEL));
            Token tk    = Interpreter.tokens[Interpreter.ip];
            int   prevX = Integer.MIN_VALUE;
            int   prevY = Integer.MIN_VALUE;
            while (tk != null) {
                int ipX = CODE.x() + CODE_PAD.width() + tk.col * cachedFontW;
                int ipY = CODE.y() + CODE_PAD.height() + tk.row * cachedFontH - codeOffsetY + 5;
                int ipW = tk.len() * cachedFontW;
                int ipH = cachedFontH;
                drawRectBorder(ipX, ipY, ipW, ipH, colors[colorEnum.COLOR_HIGHLIGHT.ordinal()]);
                setStroke(new BasicStroke(1));

                // TODO: maybe a pretty drawArc()?)

                if (prevX != Integer.MIN_VALUE && prevY != Integer.MIN_VALUE) {
                    drawLine(ipX + ipW / 2, ipY + ipH / 2, prevX, prevY, colors[colorEnum.COLOR_CONNECTION.ordinal()]);
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
//                int x = CODE.x() / 2 + CODE.width() / 2 - w / 2;
//                int y = CODE.y() / 2 + CODE.height() / 2 - h / 2;
//            drawRoundRect(x, y, w + CODE.x(), h + CODE.y(), 5, 5, colors[colorEnum.COLOR_DATA.ordinal()]);
//            drawRoundRectBorder(x, y, w + CODE.x(), h + CODE.y(), 5, 5, Color.WHITE);
//                for (int i = 0; i < uData.length; i++) {
//                drawText(uData[i], x + unfoldedDataPad, y + unfoldedDataPad + cachedFontH, Color.WHITE);
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
//                    ipX = (int) (CODE.x() + CODE.width() / 2.f - ufw / 2.f + mouseToken.col * cachedFontW) - 2;
//                    ipY = (int) (CODE.x() + CODE.height() / 2.f + (-uData.length / 2.f + mouseToken.row) * cachedFontH);
//                } else {
            ipX = CODE.x() + CODE_PAD.width() + mouseToken.col * cachedFontW;
            ipY = CODE.y() + CODE_PAD.height() + mouseToken.row * cachedFontH - codeOffsetY + 5;
//                }
            ipW = cachedFontW * mouseToken.len();
            ipH = cachedFontH;
            setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_BEVEL));
            drawRectBorder(ipX, ipY, ipW, ipH, colors[colorEnum.COLOR_SELECT.ordinal()]);
        }
        resetClip();
    }

    @Override
    public void reset() { }

    protected static void findMouseToken(NGVec2i pos) {
//        if (unfolding) {
//            int unfoldingWindowX = CODE.x() / 2 + CODE.width() / 2 - cachedUnfoldedDataWidth / 2; // -8
//            int unfoldingWindowY = CODE.y() / 2 + CODE.height() / 2 - cachedUnfoldedDataHeight / 2;// -30
//            int row              = (mouseY - unfoldingWindowY - unfoldedDataPad - cachedFontH) / cachedFontH;
//            int col              = (mouseX - unfoldingWindowX - unfoldedDataPad) / cachedFontW;
//            mouseToken = getTokenByRowCol(unfoldedTokens, row, col);
//        } else {
        mouseToken = getTokenByRowCol(Interpreter.tokens, pos.sub(CODE.x(), CODE.y()).sub(CODE_PAD).add(0, codeOffsetY).divide(cachedFontW, cachedFontH));
//        }
    }

    private static Token getTokenByRowCol(Token[] array, NGVec2i pos) {
        for (Token t: array) if (t.row == pos.y() && t.col <= pos.x() && t.col + t.len() > pos.x()) return t;
        return null;
    }

    private static String hex(byte n) {
        return "%02X".formatted(n);
    }
}
