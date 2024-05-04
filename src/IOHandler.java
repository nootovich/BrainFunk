import java.awt.*;
import java.awt.event.*;

public class IOHandler {

    public static KeyAdapter getKeyHandler() {
        return new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {

                if (Interpreter.tokens.length == 0) return;

                // if (colorMode) {
                //     switch (e.getKeyCode()) {
                //         case KeyEvent.VK_BACK_SPACE -> {
                //             if (newColorVal.length() == 0) return;
                //             newColorVal = newColorVal.substring(0, newColorVal.length() - 1);
                //         }
                //         case KeyEvent.VK_ENTER -> {
                //             if (newColorVal.length() < 6) return;
                //             COLORS[selectedColor.ordinal()] = Color.decode("#" + newColorVal);
                //             colorMode                       = false;
                //             selectedColor                   = null;
                //             newColorVal                     = "";
                //         }
                //         default -> {
                //             char c = e.getKeyChar();
                //             if (!Character.isLetterOrDigit(c)) return;
                //             try {
                //                 newColorVal += c;
                //             } catch (Exception ignored) {
                //             }
                //         }
                //     }
                //     return;
                // }

                int savedRow = Interpreter.ip < Interpreter.tokens.length ? Interpreter.tokens[Interpreter.ip].row : 0;

                switch (e.getKeyCode()) {

                    case KeyEvent.VK_ESCAPE -> {
                        Utils.info("Debugger terminated by user.");
                        System.exit(0);
                    }

                    case KeyEvent.VK_SPACE -> {
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

                    case KeyEvent.VK_ENTER -> {
                        if (!Interpreter.finished) Interpreter.execute();
                    }

                    default -> {}
                }

                if (!Interpreter.finished) {
                    int newOffsetY = Debugger.DebugWindow.codeOffsetY - (savedRow - Interpreter.tokens[Interpreter.ip].row) * Debugger.DebugWindow.cachedFontH;
                    Debugger.DebugWindow.codeOffsetY = Utils.clampi(newOffsetY, 0, Debugger.DebugWindow.cachedLinesToBottom * Debugger.DebugWindow.cachedFontH);
                }

                Debugger.DebugWindow.cachedLinesToBottom = Debugger.filedata.get(Interpreter.tokens[Interpreter.ip].file).length - Debugger.DebugWindow.codeH / Debugger.DebugWindow.cachedFontH;
            }
        };
    }

    public static MouseMotionAdapter getMouseMotionHandler() {
        return new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                Insets insets = Debugger.DebugWindow.insets;
                int    x      = e.getX() - insets.left;
                int    y      = e.getY() - insets.top;
                Debugger.DebugWindow.mousePos.move(x, y);
                Debugger.DebugWindow.findMouseToken(x, y);
            }
        };
    }

    public static MouseAdapter getMouseHandler() {
        return new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                Point mousePos = Debugger.DebugWindow.mousePos;
                int   codeX    = Debugger.DebugWindow.codeX;
                int   codeY    = Debugger.DebugWindow.codeY;
                int   colX     = codeX >> 3;
                for (int i = 0; i < Debugger.DebugWindow.COLORS.values().length; i++) {
                    int x = codeX * 3 * (i + 1);
                    int y = colX;
                    int w = codeX * 3 - colX;
                    int h = codeY - colX * 2;
                    if (x < mousePos.x && mousePos.x < x + w && y < mousePos.y && mousePos.y < y + h) {
                        Debugger.DebugWindow.selectedColor = Debugger.DebugWindow.COLORS.values()[i];
                        Debugger.DebugWindow.colorMode     = true;
                        System.out.println(Debugger.DebugWindow.selectedColor + ":" + Debugger.DebugWindow.newColorVal + ":");
                        return;
                    }
                }
                Debugger.DebugWindow.colorMode     = false;
                Debugger.DebugWindow.selectedColor = null;
                Debugger.DebugWindow.newColorVal   = "";

                if (Interpreter.finished) return;
                int b = e.getButton();

                Token mouseToken = Debugger.mouseToken;
                if (b == MouseEvent.BUTTON1 && !Debugger.unfolding) {

                    if (mouseToken != null) {
                        while (Interpreter.ip < Interpreter.tokens.length && !Interpreter.tokens[Interpreter.ip].eq(mouseToken)) {
                            Interpreter.execute();
                        }
                    }

                } else if (b == MouseEvent.BUTTON3) {
                    if (mouseToken == null) {
                        Debugger.unfolding  = false;
                        Debugger.mouseToken = null;
                    } else if (mouseToken.type == Token.Type.WRD) {

                        if (Debugger.unfolding) {

                            int mouseTokenLoc = 0;
                            for (; mouseTokenLoc < Debugger.unfoldedTokens.length; mouseTokenLoc++) {
                                if (Debugger.unfoldedTokens[mouseTokenLoc].eq(mouseToken)) break;
                            }

                            Token[] macroTokens = Parser.macros.get(mouseToken.str);

                            int startRow = macroTokens[0].row;
                            int startCol = macroTokens[0].col;

                            for (int i = 0; i < macroTokens.length; i++) {
                                macroTokens[i].row -= startRow;
                                macroTokens[i].row += mouseToken.row;
                            }

                            startRow = macroTokens[0].row;
                            int endRow = macroTokens[macroTokens.length - 1].row;

                            for (int i = 0; i < macroTokens.length; i++) {
                                if (macroTokens[i].row != startRow) break;
                                macroTokens[i].col -= startCol;
                                macroTokens[i].col += mouseToken.col;
                            }

                            int endCol = macroTokens[macroTokens.length - 1].col;

                            if (macroTokens[macroTokens.length - 1].type == Token.Type.WRD) {
                                endCol += macroTokens[macroTokens.length - 1].str.length() - 1;
                            }

                            int mtkColDiff = endCol - Debugger.unfoldedTokens[mouseTokenLoc].col + 1 - mouseToken.str.length();
                            int mtkRowDiff = endRow - Debugger.unfoldedTokens[mouseTokenLoc].row;

                            for (int i = mouseTokenLoc; i < Debugger.unfoldedTokens.length; i++) {
                                Debugger.unfoldedTokens[i].row += mtkRowDiff;
                                if (Debugger.unfoldedTokens[i].row == mouseToken.row) {
                                    Debugger.unfoldedTokens[i].col += mtkColDiff;
                                }
                            }

                            Token[] temp = Debugger.unfoldedTokens;
                            Debugger.unfoldedTokens = new Token[Debugger.unfoldedTokens.length + macroTokens.length - 1];

                            Token[] macroTokensCopy = Token.deepCopy(macroTokens);
                            System.arraycopy(temp, 0, Debugger.unfoldedTokens, 0, mouseTokenLoc);
                            System.arraycopy(macroTokensCopy, 0, Debugger.unfoldedTokens, mouseTokenLoc, macroTokens.length);
                            System.arraycopy(temp, mouseTokenLoc + 1, Debugger.unfoldedTokens, mouseTokenLoc + macroTokens.length, temp.length - mouseTokenLoc - 1);

                            Debugger.DebugWindow.updateUnfoldedData();

                        } else {

                            Debugger.unfoldedTokens = Parser.macros.get(mouseToken.str);

                            int startRow = Debugger.unfoldedTokens[0].row;
                            int startCol = Debugger.unfoldedTokens[0].col;

                            for (int i = 0; i < Debugger.unfoldedTokens.length; i++) {
                                Debugger.unfoldedTokens[i].row -= startRow;
                            }

                            for (int i = 0; i < Debugger.unfoldedTokens.length; i++) {
                                if (Debugger.unfoldedTokens[i].row != startRow) break;
                                Debugger.unfoldedTokens[i].col -= startCol;
                            }

                            Debugger.DebugWindow.updateUnfoldedData();
                            Debugger.unfolding = true;
                        }

                        Debugger.mouseToken = null;
                    }
                }

                Debugger.DebugWindow.cachedLinesToBottom = Debugger.filedata.get(Interpreter.tokens[Interpreter.ip].file).length - Debugger.DebugWindow.codeH / Debugger.DebugWindow.cachedFontH;
            }
        };
    }

    public static MouseWheelListener getMouseWheelHandler() {
        return new MouseAdapter() {
            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                Debugger.DebugWindow.codeOffsetY = Utils.clampi(Debugger.DebugWindow.codeOffsetY + e.getWheelRotation() * Debugger.DebugWindow.cachedFontH, 0, Debugger.DebugWindow.cachedLinesToBottom * Debugger.DebugWindow.cachedFontH);
                Debugger.DebugWindow.findMouseToken(e.getX(), e.getY());
            }
        };
    }
}
