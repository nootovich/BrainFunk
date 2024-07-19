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
        renderer.defaultFont      = new Font(Font.MONOSPACED, Font.PLAIN, 17);

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

        window.setKeyHandler(new DebuggerKeyHandler());
        window.setMouseHandler(new DebuggerMouseHandler());
        new Timer(100, _ -> window.redraw()).start();
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
