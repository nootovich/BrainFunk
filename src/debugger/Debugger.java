package debugger;

import BrainFunk.*;
import nootovich.nglib.NGFileSystem;
import nootovich.nglib.NGUtils;
import nootovich.nglib.NGWindow;

import javax.swing.*;
import java.awt.*;
import java.io.File;

import static debugger.DebuggerRenderer.*;

public class Debugger {

    public static NGWindow window;

//    private static       boolean unfolding       = false;
//    private static final int     unfoldedDataPad = codeX / 2;
//    private static       int      cachedUnfoldedDataWidth;
//    private static       int      cachedUnfoldedDataHeight;
//    private static       String   unfoldedData = "";
//    private static       Token[]  unfoldedTokens;

    public static void main(String[] args) {
        if (args.length < 1) NGUtils.error("No file was provided. Please provide a `.bf`, `.bfn` or `.bfnx` file as a command line argument.");

        String   filepath      = args[0];
        String   filename      = new File(filepath).getName();
        String[] filenameParts = filename.split("\\.");
        String   extension     = filenameParts[filenameParts.length - 1];

        Main.ProgramType programType = switch (extension) {
            case "bf" -> Main.ProgramType.BF;
            case "bfn" -> Main.ProgramType.BFN;
            case "bfnx" -> Main.ProgramType.BFNX;
            default -> {
                NGUtils.error("Invalid file type `%s`. Please provide a `.bf`, `.bfn` or `.bfnx` file as a command line argument.");
                yield Main.ProgramType.ERR;
            }
        };

        String code = NGFileSystem.loadFile(filepath);

        DebuggerRenderer renderer = new DebuggerRenderer();
        DebuggerRenderer.filedata = code.split("\n", -1);
        renderer.defaultFont      = new Font(Font.MONOSPACED, Font.PLAIN, 15);

        window = new NGWindow(w, h, renderer);

        FontMetrics metrics = window.jf.getFontMetrics(renderer.defaultFont);
        cachedFontH         = metrics.getHeight();
        cachedFontW         = metrics.charWidth('@');
        cachedLinesToBottom = filedata.length - codeH / cachedFontH;

        Parser.debug = true;
        Token[] lexed  = Lexer.lex(code, filepath, programType);
        Token[] parsed = Parser.parse(Token.deepCopy(lexed), filepath);
        Interpreter.loadProgram(parsed, programType);
        if (Interpreter.tokens.length > 0) codeOffsetY = NGUtils.clamp(Interpreter.tokens[0].row * cachedFontH - codeH / 2, 0, cachedLinesToBottom * cachedFontH);

        {
//            window.jf.addMouseMotionListener(new MouseMotionAdapter() {
//                @Override
//                public void mouseMoved(MouseEvent e) {
//                    findMouseToken(e.getX(), e.getY());
//                }
//            });
//            window.jf.addMouseListener(new MouseAdapter() {
//                @Override
//                public void mouseClicked(MouseEvent e) {
//
//                    if (Interpreter.finished) return;
//                    int b = e.getButton();
//
//                    if (b == MouseEvent.BUTTON1/* && !unfolding*/) {
//
//                        if (mouseToken != null) {
//                            while (Interpreter.ip < Interpreter.tokens.length && !Interpreter.tokens[Interpreter.ip].eq(mouseToken)) {
//                                Interpreter.execute();
//                            }
//                        }
//
//                    } //else if (b == MouseEvent.BUTTON3) {
//                        if (mouseToken == null) {
//                            unfolding = false;
//                        } else if (mouseToken.type == Token.Type.WRD) {
//                            if (unfolding) {
//
//                                int mouseTokenLoc = 0;
//                                for (; mouseTokenLoc < unfoldedTokens.length; mouseTokenLoc++) {
//                                    if (unfoldedTokens[mouseTokenLoc].eq(mouseToken)) break;
//                                }
//
//                                Token[] macroTokens = Parser.macros.get(mouseToken.str);
//
//                                int startRow = macroTokens[0].row;
//                                int startCol = macroTokens[0].col;
//
//                                for (int i = 0; i < macroTokens.length; i++) {
//                                    macroTokens[i].row -= startRow;
//                                    macroTokens[i].row += mouseToken.row;
//                                }
//
//                                int endRow = macroTokens[macroTokens.length - 1].row;
//
//                                for (int i = 0; i < macroTokens.length; i++) {
//                                    if (macroTokens[i].row != startRow) break;
//                                    macroTokens[i].col -= startCol;
//                                    macroTokens[i].col += mouseToken.col;
//                                }
//
//                                int endCol = macroTokens[macroTokens.length - 1].col;
//
//                                if (macroTokens[macroTokens.length - 1].type == Token.Type.WRD) {
//                                    endCol += macroTokens[macroTokens.length - 1].str.length() - 1;
//                                }
//
//                                int mtkColDiff = endCol - unfoldedTokens[mouseTokenLoc].col + 1 - mouseToken.str.length();
//                                int mtkRowDiff = endRow - unfoldedTokens[mouseTokenLoc].row;
//
//                                for (int i = mouseTokenLoc; i < unfoldedTokens.length; i++) {
//                                    unfoldedTokens[i].row += mtkRowDiff;
//                                    if (unfoldedTokens[i].row == mouseToken.row) {
//                                        unfoldedTokens[i].col += mtkColDiff;
//                                    }
//                                }
//
//                                Token[] temp = unfoldedTokens;
//                                unfoldedTokens = new Token[unfoldedTokens.length + macroTokens.length - 1];
//
//                                Token[] macroTokensCopy = Token.deepCopy(macroTokens);
//                                System.arraycopy(temp, 0, unfoldedTokens, 0, mouseTokenLoc);
//                                System.arraycopy(macroTokensCopy, 0, unfoldedTokens, mouseTokenLoc, macroTokens.length);
//                                System.arraycopy(temp, mouseTokenLoc + 1, unfoldedTokens, mouseTokenLoc + macroTokens.length, temp.length - mouseTokenLoc - 1);
//
//                                updateUnfoldedData();
//
//                            } else {
//
//                                unfoldedTokens = Parser.macros.get(mouseToken.str);
//
//                                int startRow = unfoldedTokens[0].row;
//                                int startCol = unfoldedTokens[0].col;
//
//                                for (int i = 0; i < unfoldedTokens.length; i++) {
//                                    unfoldedTokens[i].row -= startRow;
//                                }
//
//                                for (int i = 0; i < unfoldedTokens.length; i++) {
//                                    if (unfoldedTokens[i].row != startRow) break;
//                                    unfoldedTokens[i].col -= startCol;
//                                }
//
//                                updateUnfoldedData();
//                                unfolding = true;
//                            }
//                            mouseToken = null;
//                        }
//                }
//                }
//            });
//            window.jf.addMouseWheelListener(new MouseAdapter() {
//                @Override
//                public void mouseWheelMoved(MouseWheelEvent e) {
//                    codeOffsetY = NGUtils.clamp(codeOffsetY + e.getWheelRotation() * cachedFontH, 0, cachedLinesToBottom * cachedFontH);
//                    findMouseToken(e.getX(), e.getY());
//                }
//            });
        } // TODO: Make more handlers

        window.setKeyHandler(new DebuggerKeyHandler());
        Timer timer = new Timer(100, _ -> window.redraw());
        timer.start();
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
