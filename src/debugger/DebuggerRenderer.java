package debugger;

import BrainFunk.Op;
import BrainFunk.Token;
import java.awt.*;
import nootovich.nglib.*;

import static BrainFunk.Interpreter.*;
import static debugger.Debugger.*;

public class DebuggerRenderer extends NGRenderer {

    public static final Color COLOR_BG         = new Color(0x153243);
    public static final Color COLOR_DATA       = new Color(0x284B63);
    public static final Color COLOR_TEXT       = new Color(0xFFFFFF);
    public static final Color COLOR_TEXT_FADED = new Color(0x7EA8BE);
    public static final Color COLOR_HIGHLIGHT  = new Color(0x5EF38C);
    public static final Color COLOR_SELECT     = new Color(0xFFC69B);
    public static final Color COLOR_CONNECTION = new Color(0xD9E985);

    public static NGVec4i areaCode;
    public static NGVec2i areaPadding;
    public static NGVec4i areaTape;
    public static NGVec2i fontSize;

    public static int codeOffsetY = 0;
    public static int cachedLinesToBottom;

    public static Token mouseToken = null;

    public static String[] filedata = {""};

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

    @Override
    public void render(NGGraphics g) {

        // BG
        {
            g.drawRect(0, 0, WINDOW_WIDTH, WINDOW_HEIGHT, COLOR_BG);
            g.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_BEVEL));
            g.drawRectBorder(areaCode.x(), areaCode.y(), areaCode.w(), areaCode.h(), COLOR_DATA);
            g.drawRectBorder(areaTape.x(), areaTape.y(), areaTape.w(), areaTape.h(), COLOR_SELECT);
            g.setStroke(new BasicStroke(1));
            g.drawText("Mode: " + mode, 2, h - 2, Color.WHITE);
            if (finished) g.drawTextCentered(
                "Execution finished, press \"R\" to restart",
                areaCode.x() + areaCode.w() / 2,
                areaCode.y() + areaCode.h() + areaPadding.y() / 2,
                COLOR_HIGHLIGHT
            );
        }

        // Memory values
        {
            if (mode == MODES.EXECUTION) {
                for (int y = 0; y < tape.length / 8 && y < areaTape.h() / fontSize.h(); y++) {
                    for (int x = 0; x < 8; x++) {
                        int cx = x * fontSize.w() * 3 + areaTape.x() + areaPadding.x() / 3;
                        int cy = (y + 1) * fontSize.h() + areaTape.y();
                        g.drawText(hex(tape[y * 8 + x]), cx, cy, COLOR_TEXT);
                        if (y * 8 + x == pointer) {
                            g.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_BEVEL));
                            g.drawRectBorder(cx, cy - fontSize.h() + 5, fontSize.w() * 2, fontSize.h(), COLOR_HIGHLIGHT);
                            g.setStroke(new BasicStroke(1));
                        }
                    }
                }
            } else if (mode == MODES.FLOW_ANALYSIS) {
                for (int y = 0; y < cellStates.length / 8 && y < areaTape.h() / fontSize.h(); y++) {
                    for (int x = 0; x < 8; x++) {
                        int       cx = x * fontSize.w() * 3 + areaTape.x() + areaPadding.x() / 3;
                        int       cy = (y + 1) * fontSize.h() + areaTape.y();
                        CellState cs = cellStates[y * 8 + x];
                        for (int hNibble = 0; hNibble < 16; hNibble++) {
                            for (int lNibble = 0; lNibble < 16; lNibble++) {
                                Color c = (cs != null && cs.values[hNibble * 16 + lNibble]) ? Color.WHITE : Color.BLACK;
                                g.drawPixel(cx + lNibble, cy + hNibble, c);
                            }
                        }
                        if (y * 8 + x == pointer) {
                            g.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_BEVEL));
                            g.drawRectBorder(cx, cy - fontSize.h() + 5, fontSize.w() * 2, fontSize.h(), COLOR_HIGHLIGHT);
                            g.setStroke(new BasicStroke(1));
                        }
                    }
                }
            }
        }

        NGVec4i areaText = new NGVec4i(areaCode.xy().add(areaPadding).subY(codeOffsetY), fontSize.yx());

        // Program
        {
            g.setClip(areaCode.x(), areaCode.y(), areaCode.w(), areaCode.h());
            NGVec2i pos         = areaText.xy().addY(g.g2d.getFontMetrics().getAscent());
            String  currentFile = ops[finished ? ip - 1 : ip].token.file;
            for (Token t: Debugger.tokens.getOrDefault(currentFile, new Token[]{ })) {
                g.drawText(t.repr(), pos.add(fontSize.scale(t.col, t.row)), (t.visited ? COLOR_TEXT : COLOR_TEXT_FADED));
            }
            g.resetClip();
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
                g.drawRectBorder(outline, COLOR_HIGHLIGHT);
                g.setStroke(new BasicStroke(1));

                NGVec2i outlineCenter = new NGVec2i(outline.x() + outline.w() / 2, outline.y() + outline.h() / 2);
                if (prevCenter.x() != Integer.MIN_VALUE && prevCenter.y() != Integer.MIN_VALUE) {
                    // TODO: maybe a pretty drawArc()?)
                    g.drawLine(outlineCenter, prevCenter, COLOR_CONNECTION);
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
            g.drawRectBorder(outline, COLOR_SELECT);
        }
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

    public static String hex(byte n) {
        return "%02X".formatted(n);
    }
}
