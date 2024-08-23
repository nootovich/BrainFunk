package debugger;

import BrainFunk.*;
import java.awt.FontMetrics;
import nootovich.nglib.*;

import static debugger.DebuggerRenderer.*;

public class Debugger extends NGMain {

//    private static       boolean unfolding       = false;
//    private static final int     unfoldedDataPad = codeX / 2;
//    private static       int      cachedUnfoldedDataWidth;
//    private static       int      cachedUnfoldedDataHeight;
//    private static       String   unfoldedData = "";
//    private static       Token[]  unfoldedTokens;

    private static int savedRow = 0;

    public void main() {
        setTickRate(0);
        setFrameRate(60);
        createWindow(1400, 785, DebuggerRenderer.class);
        FontMetrics metrics = window.jf.getFontMetrics(window.renderer.font);
        cachedFontH         = metrics.getHeight();
        cachedFontW         = metrics.charWidth('@');
        cachedLinesToBottom = filedata.length - CODE.height() / cachedFontH;
        if (Interpreter.tokens.length > 0) codeOffsetY = NGUtils.clamp(Interpreter.tokens[0].row * cachedFontH - (Debugger.h - Debugger.w / 20) / 2, 0, cachedLinesToBottom * cachedFontH);
        start();
    }

    @Override
    public void onAnyKeyPress() {
        savedRow = Interpreter.ip < Interpreter.tokens.length ? Interpreter.tokens[Interpreter.ip].row : 0;
    }

    @Override
    public void afterAnyKeyPress() {
        if (!Interpreter.finished) {
            int newOffsetY = codeOffsetY - (savedRow - Interpreter.tokens[Interpreter.ip].row) * cachedFontH;
            codeOffsetY = NGUtils.clamp(newOffsetY, 0, cachedLinesToBottom * cachedFontH);
        }
    }

    @Override
    public void onEscapePress() {
        exit();
    }

    @Override
    public void onEnterPress() {
        if (!Interpreter.finished) Interpreter.execute();
    }

    @Override
    public void onSpacePress() {
        if (Interpreter.finished) {
            Interpreter.restart();
            return;
        }
        if (Interpreter.tokens[Interpreter.ip].type == Token.Type.WRD) {

            // TODO: cache parsed macros
            Token[] macro = new Token[]{Interpreter.tokens[Interpreter.ip]};
            int     n     = Interpreter.ip + Parser.parseMacroCall(macro, null).length;
            while (Interpreter.ip < n) Interpreter.execute();
        } else if (Interpreter.tokens[Interpreter.ip].type == Token.Type.JEZ || Interpreter.tokens[Interpreter.ip].type == Token.Type.URS) {

            int target = Interpreter.tokens[Interpreter.ip].num + 1;
            while (Interpreter.ip < Interpreter.tokens.length - 1 && Interpreter.ip != target) Interpreter.execute();
        } else Interpreter.execute();
    }

    @Override
    public void onMouseMoved(NGVec2i pos) {
        findMouseToken(pos);
    }

    @Override
    public void onLMBPress(NGVec2i pos) {
        if (Interpreter.finished || mouseToken == null /* || unfolding*/) return;
        while (Interpreter.ip < Interpreter.tokens.length && !Interpreter.tokens[Interpreter.ip].eq(mouseToken)) {
            Interpreter.execute();
        }
    }

    @Override
    public void onRMBPress(NGVec2i pos) {
//    if (mouseToken == null) {
//            unfolding = false;
//        } else if (mouseToken.type == Token.Type.WRD) {
//            if (unfolding) {
//
//                int mouseTokenLoc = 0;
//                for (; mouseTokenLoc < unfoldedTokens.length; mouseTokenLoc++) {
//                    if (unfoldedTokens[mouseTokenLoc].eq(mouseToken)) break;
//                }
//
//                Token[] macroTokens = Parser.macros.get(mouseToken.str);
//
//                int startRow = macroTokens[0].row;
//                int startCol = macroTokens[0].col;
//
//                for (int i = 0; i < macroTokens.length; i++) {
//                    macroTokens[i].row -= startRow;
//                    macroTokens[i].row += mouseToken.row;
//                }
//
//                int endRow = macroTokens[macroTokens.length - 1].row;
//
//                for (int i = 0; i < macroTokens.length; i++) {
//                    if (macroTokens[i].row != startRow) break;
//                    macroTokens[i].col -= startCol;
//                    macroTokens[i].col += mouseToken.col;
//                }
//
//                int endCol = macroTokens[macroTokens.length - 1].col;
//
//                if (macroTokens[macroTokens.length - 1].type == Token.Type.WRD) {
//                    endCol += macroTokens[macroTokens.length - 1].str.length() - 1;
//                }
//
//                int mtkColDiff = endCol - unfoldedTokens[mouseTokenLoc].col + 1 - mouseToken.str.length();
//                int mtkRowDiff = endRow - unfoldedTokens[mouseTokenLoc].row;
//
//                for (int i = mouseTokenLoc; i < unfoldedTokens.length; i++) {
//                    unfoldedTokens[i].row += mtkRowDiff;
//                    if (unfoldedTokens[i].row == mouseToken.row) {
//                        unfoldedTokens[i].col += mtkColDiff;
//                    }
//                }
//
//                Token[] temp = unfoldedTokens;
//                unfoldedTokens = new Token[unfoldedTokens.length + macroTokens.length - 1];
//
//                Token[] macroTokensCopy = Token.deepCopy(macroTokens);
//                System.arraycopy(temp, 0, unfoldedTokens, 0, mouseTokenLoc);
//                System.arraycopy(macroTokensCopy, 0, unfoldedTokens, mouseTokenLoc, macroTokens.length);
//                System.arraycopy(temp, mouseTokenLoc + 1, unfoldedTokens, mouseTokenLoc + macroTokens.length, temp.length - mouseTokenLoc - 1);
//
//                updateUnfoldedData();
//
//            } else {
//
//                unfoldedTokens = Parser.macros.get(mouseToken.str);
//
//                int startRow = unfoldedTokens[0].row;
//                int startCol = unfoldedTokens[0].col;
//
//                for (int i = 0; i < unfoldedTokens.length; i++) {
//                    unfoldedTokens[i].row -= startRow;
//                }
//
//                for (int i = 0; i < unfoldedTokens.length; i++) {
//                    if (unfoldedTokens[i].row != startRow) break;
//                    unfoldedTokens[i].col -= startCol;
//                }
//
//                updateUnfoldedData();
//                unfolding = true;
//            }
//            mouseToken = null;
//        }
    }

    @Override
    public void onMouseWheel(NGVec2i pos, int direction) {
        codeOffsetY = NGUtils.clamp(codeOffsetY + direction * cachedFontH, 0, cachedLinesToBottom * cachedFontH);
        findMouseToken(pos);
    }

    //    private static void updateUnfoldedData() {
//
//        int unfoldedDataLines = 1;
//
//        StringBuilder sb = new StringBuilder(unfoldedTokens[0].repr());
//        for (int i = 1; i < unfoldedTokens.length; i++) {
//            if (unfoldedTokens[i].row > unfoldedTokens[i - 1].row) {
//                sb.append("\n").append(" ".repeat(unfoldedTokens[i].col));
//                unfoldedDataLines++;
//            } else {
//                sb.append(" ".repeat(unfoldedTokens[i].col - unfoldedTokens[i - 1].col - unfoldedTokens[i - 1].len()));
//            }
//            sb.append(unfoldedTokens[i].repr());
//        }
//        unfoldedData = sb.toString();
//
//        cachedUnfoldedDataHeight = unfoldedDataLines * cachedFontH;
//        cachedUnfoldedDataWidth  = 0;
//        for (String s : unfoldedData.split("\n", -1)) {
//            int currentWidth = s.length() * cachedFontW;
//            if (currentWidth > cachedUnfoldedDataWidth) cachedUnfoldedDataWidth = currentWidth;
//        }
//    }
}
