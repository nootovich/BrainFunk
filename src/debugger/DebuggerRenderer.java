package debugger;

import BrainFunk.*;
import java.awt.*;
import nootovich.nglib.*;

import static BrainFunk.Interpreter.*;
import static debugger.Debugger.WINDOW_HEIGHT;
import static debugger.Debugger.WINDOW_WIDTH;

public class DebuggerRenderer extends NGRenderer {

    public static NGVec4i areaCode;
    public static NGVec2i areaPadding;
    public static NGVec4i areaTape;

    public static NGVec2i fontSize;
    public static int     codeOffsetY = 0;
    public static int     cachedLinesToBottom;

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

        if (finished) {
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
            for (int y = 0; y < tape.length / 8 && y < areaTape.h() / fontSize.h(); y++) {
                for (int x = 0; x < 8; x++) {
                    int cx = x * fontSize.w() * 3 + areaTape.x() + areaPadding.x() / 3;
                    int cy = (y + 1) * fontSize.h() + areaTape.y();
                    g.drawText(hex(tape[y * 8 + x]), cx, cy, colors[colorEnum.COLOR_TEXT.ordinal()]);
                    if (y * 8 + x == pointer) {
                        g.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_BEVEL));
                        g.drawRectBorder(cx, cy - fontSize.h() + 5, fontSize.w() * 2, fontSize.h(), colors[colorEnum.COLOR_HIGHLIGHT.ordinal()]);
                        g.setStroke(new BasicStroke(1));
                    }
                }
            }
        }

        NGVec4i areaText = new NGVec4i(areaCode.xy().add(areaPadding).subY(codeOffsetY), fontSize.yx());

        // Program
        {
            if (mode == MODE.NORMAL) {
                // g.setClip(areaCode.x(), areaCode.y(), areaCode.w(), areaCode.h());
                NGVec2i pos         = areaText.xy().addY(g.g2d.getFontMetrics().getAscent());
                String  currentFile = ops[finished ? ip - 1 : ip].token.file;
                for (Token t: Debugger.tokens.getOrDefault(currentFile, new Token[]{ })) {
                    g.drawText(t.repr(), pos.add(fontSize.scale(t.col, t.row)), colors[(t.visited ? colorEnum.COLOR_TEXT : colorEnum.COLOR_TEXT_FADED).ordinal()]);
                }
            } else if (mode == MODE.TOKEN_LIST) {
                // g.resetClip();
                // for (int i = 0; i < tokens.length; i++) {
                //     NGVec2i pos = areaText.xy().addY(fontSize.y() * i);
                //     g.drawText(tokens[i].toString(), pos, colors[colorEnum.COLOR_TEXT.ordinal()]);
                // }
            }
        }

        // Current token
        if (!finished && ops.length > 0) {
            NGVec2i prevCenter = new NGVec2i(Integer.MIN_VALUE);
            Op      op         = ops[ip];
            g.drawTextCentered("[%d] => %s".formatted(ip, op.toString()), areaCode.w() / 2, areaPadding.h() / 2, Color.WHITE);
            g.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_BEVEL));

            // TODO: think about this flow. would be nice to separate first(current) token from the rest of macro expansion.
            while (op != null) {
                Token   token   = op.token;
                NGVec4i outline = areaText.addXY(fontSize.scale(token.col, token.row)).scaleW(token.len());
                g.drawRectBorder(outline, colors[colorEnum.COLOR_HIGHLIGHT.ordinal()]);
                g.setStroke(new BasicStroke(1));

                NGVec2i outlineCenter = new NGVec2i(outline.x() + outline.w() / 2, outline.y() + outline.h() / 2);
                if (prevCenter.x() != Integer.MIN_VALUE && prevCenter.y() != Integer.MIN_VALUE) {
                    // TODO: maybe a pretty drawArc()?)
                    g.drawLine(outlineCenter, prevCenter, colors[colorEnum.COLOR_CONNECTION.ordinal()]);
                }
                prevCenter = outlineCenter;

                if (op.origin >= 0) op = ops[op.origin];
                else op = null;
            }
        }

        // Token under mouse outline
        if (mouseToken != null) {
            NGVec4i outline = areaText.addXY(fontSize.scale(mouseToken.col, mouseToken.row)).scaleW(mouseToken.len());
            g.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_BEVEL));
            g.drawRectBorder(outline, colors[colorEnum.COLOR_SELECT.ordinal()]);
        }
        g.resetClip();

        // g.drawRect(Debugger.BUTTON_TOKEN_LIST, Color.RED);
    }

    public void reset() { }

    public static void findMouseToken(NGVec2i pos) {
        // mouseToken = getTokenByRowCol(pos.sub(areaCode.xy().add(areaPadding).subY(codeOffsetY)).divide(fontSize.w(), fontSize.h()));
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
