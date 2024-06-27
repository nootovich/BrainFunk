import nootovich.nglib.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseWheelEvent;
import java.io.File;

// jar cvf jar-name compiled-classes...

public class Debugger {
    public static int w = 1400;
    public static int h = 785;

    private static final int codeX       = w / 40;
    private static final int codeY       = codeX;
    private static final int codeW       = w * 75 / 100;
    private static final int codeH       = h - codeY * 2;
    private static final int codePadH    = 16;
    private static final int codePadV    = 4;
    private static       int codeOffsetY = 0;

    private static final int tapeY = codeY;
    private static final int tapeH = codeH;
    private static final int tapeW = w - codeX * 3 - codeW;
    private static final int tapeX = w - codeX - tapeW;

    private static int cachedFontW;
    private static int cachedFontH;
    private static int cachedLinesToBottom;

    private static       String[] filedata   = {""};
    private static final Font     font       = new Font(Font.MONOSPACED, Font.PLAIN, 15);
    private static       Token    mouseToken = null;

//    private static       boolean unfolding       = false;
//    private static final int     unfoldedDataPad = codeX / 2;
//    private static       int      cachedUnfoldedDataWidth;
//    private static       int      cachedUnfoldedDataHeight;
//    private static       String   unfoldedData = "";
//    private static       Token[]  unfoldedTokens;

    // TODO: move into NGColors
    public enum colorEnum {
        COLOR_BG, COLOR_DATA, COLOR_TEXT, COLOR_TEXT_FADED, COLOR_HIGHLIGHT, COLOR_SELECT, COLOR_CONNECTION
    }

    public static Color[] colors = new Color[]{
            new Color(0x153243),
            new Color(0x284B63),
            new Color(0xFFFFFF),
            new Color(0x7EA8BE),
            new Color(0x5EF38C),
            new Color(0xFFC69B),
            new Color(0xD9E985)
    };

    public static void main(String[] args) {
        if (args.length < 1) Utils.error("No file was provided. Please provide a `.bf`, `.bfn` or `.bfnx` file as a command line argument.");

        String   filepath      = args[0];
        String   filename      = new File(filepath).getName();
        String[] filenameParts = filename.split("\\.");
        String   extension     = filenameParts[filenameParts.length - 1];

        Main.ProgramType programType = switch (extension) {
            case "bf" -> Main.ProgramType.BF;
            case "bfn" -> Main.ProgramType.BFN;
            case "bfnx" -> Main.ProgramType.BFNX;
            default -> {
                Utils.error("Invalid file type `%s`. Please provide a `.bf`, `.bfn` or `.bfnx` file as a command line argument.");
                yield Main.ProgramType.ERR;
            }
        };

        String code = FileSystem.loadFile(filepath);
        filedata = code.split("\n", -1);

        Parser.debug = true;
        Token[] lexed  = Lexer.lex(code, filepath, programType);
        Token[] parsed = Parser.parse(Token.deepCopy(lexed), filepath);
        Interpreter.loadProgram(parsed, programType);

        ////////////////////////////////////////////////
        if (Interpreter.tokens.length > 0) codeOffsetY = Utils.clampi(Interpreter.tokens[0].row * cachedFontH - codeH / 2, 0, cachedLinesToBottom * cachedFontH);

        NGWindow window = new NGWindow(w, h);

        window.g.g2d.setFont(font); // TODO: placeholder
        FontMetrics metrics = window.g.g2d.getFontMetrics();
        cachedFontH         = metrics.getHeight();
        cachedFontW         = (int) metrics.getStringBounds("@", null).getWidth();
        cachedLinesToBottom = filedata.length - codeH / cachedFontH;

        {
            window.jf.addMouseMotionListener(new MouseMotionAdapter() {
                @Override
                public void mouseMoved(MouseEvent e) {
                    findMouseToken(e.getX(), e.getY());
                }
            });
            window.jf.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {

                    if (Interpreter.finished) return;
                    int b = e.getButton();

                    if (b == MouseEvent.BUTTON1/* && !unfolding*/) {

                        if (mouseToken != null) {
                            while (Interpreter.ip < Interpreter.tokens.length && !Interpreter.tokens[Interpreter.ip].eq(mouseToken)) {
                                Interpreter.execute();
                            }
                        }

                    } //else if (b == MouseEvent.BUTTON3) {
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
                }
//                }
            });
            window.jf.addMouseWheelListener(new MouseAdapter() {
                @Override
                public void mouseWheelMoved(MouseWheelEvent e) {
                    codeOffsetY = Utils.clampi(codeOffsetY + e.getWheelRotation() * cachedFontH, 0, cachedLinesToBottom * cachedFontH);
                    findMouseToken(e.getX(), e.getY());
                }
            });
        } // TODO: Make more handlers

        window.renderer = new DebugRenderer();
        window.setKeyboardHandler(new DebugKeyboardhandler());
        Timer timer = new Timer(100, _ -> window.redraw());
        timer.start();
    }

    private static final class DebugRenderer implements NGRenderer {

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
    }

    private static final class DebugKeyboardhandler extends NGKeyboardHandler {

        @Override
        public void onKeyDn(int key, char chr) {
            int savedRow = Interpreter.ip < Interpreter.tokens.length ? Interpreter.tokens[Interpreter.ip].row : 0;

            switch (key) {
                case NGKeys.ESCAPE -> {
                    Utils.info("Debugger terminated by user.");
                    System.exit(0);
                }
                case NGKeys.SPACEBAR -> {
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
                case NGKeys.ENTER -> {
                    if (!Interpreter.finished) Interpreter.execute();
                }
                default -> {
                }
            }

            if (!Interpreter.finished) {
                int newOffsetY = codeOffsetY - (savedRow - Interpreter.tokens[Interpreter.ip].row) * cachedFontH;
                codeOffsetY = Utils.clampi(newOffsetY, 0, cachedLinesToBottom * cachedFontH);
            }
        }
    }

    private static void findMouseToken(int mouseX, int mouseY) {
//        if (unfolding) {
//
//            // FIXME: wrong col pos (and most likely everything)
//            int unfoldingWindowX = codeX / 2 + codeW / 2 - cachedUnfoldedDataWidth / 2; // -8
//            int unfoldingWindowY = codeY / 2 + codeH / 2 - cachedUnfoldedDataHeight / 2;// -30
//            int row              = (mouseY - unfoldingWindowY - unfoldedDataPad - cachedFontH) / cachedFontH;
//            int col              = (mouseX - unfoldingWindowX - unfoldedDataPad) / cachedFontW;
//            mouseToken = getTokenByRowCol(unfoldedTokens, row, col);
//        } else {
        int row = (mouseY - codeY - codePadH + codeOffsetY) / cachedFontH - 1;
        int col = (mouseX - codeX - codePadH) / cachedFontW;
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
