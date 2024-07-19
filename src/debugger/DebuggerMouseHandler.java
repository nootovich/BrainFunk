package debugger;

import BrainFunk.Interpreter;
import nootovich.nglib.NGMouseHandler;
import nootovich.nglib.NGUtils;
import nootovich.nglib.NGVec2i;

import static debugger.DebuggerRenderer.*;

public class DebuggerMouseHandler extends NGMouseHandler {

    @Override
    public void onMoved(NGVec2i pos) {
        findMouseToken(pos);
    }

    @Override
    public void onLMBPressed(NGVec2i pos) {
        if (Interpreter.finished || mouseToken == null /* || unfolding*/) return;
        while (Interpreter.ip < Interpreter.tokens.length && !Interpreter.tokens[Interpreter.ip].eq(mouseToken)) {
            Interpreter.execute();
        }
    }

    @Override
    public void onRMBPressed(NGVec2i pos) {
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
    public void onWheel(NGVec2i pos, int direction) {
        codeOffsetY = NGUtils.clamp(codeOffsetY + direction * cachedFontH, 0, cachedLinesToBottom * cachedFontH);
        findMouseToken(pos);
    }
}
