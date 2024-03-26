import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;

public class Debugger {

    private static boolean unfolding = false;

    private static String[] filedata = {""};

    private static Token   mouseToken;
    private static Token[] unfoldedTokens = new Token[0];

    public static void main(String[] args) throws InterruptedException {

        if (args.length < 1) Utils.error("Please provide a .bf or .bfn file as a command line argument.");
        String filepath = args[0];

        String filename = new File(filepath).getName();
        if (!filename.endsWith(".bfn") && !filename.endsWith(".bf")) {
            Utils.error("Invalid file format. Please provide a .bf or .bfn file.");
        }

        String code = FileSystem.loadFile(filepath);
        filedata = code.split("\n", -1);

        Parser.debug = true;
        Token[] lexed = Lexer.lex(code, filepath);
        Interpreter.tokens = Parser.parse(Token.deepCopy(lexed));

        DebugWindow debugWindow = new DebugWindow(1400, 785);
        while (true) {
            debugWindow.repaint();
            Thread.sleep(30);
        }
    }

    private static class DebugWindow extends JFrame {

        private final int w, h;
        private final int tapeX, tapeY, tapeW, tapeH;
        private final int codeX, codeY, codeW, codeH;
        private int codeOffsetY;

        private final Graphics2D    g2d;
        private final BufferedImage buffer;
        private final Font          font = new Font("Roboto Mono", Font.PLAIN, 16);

        private final Color COLOR_BG   = new Color(0x3D4154);
        private final Color COLOR_DATA = new Color(0x4C5470);

        private final int cachedFontW, cachedFontH;
        private final int cachedLinesToBottom;
        private       int cachedUnfoldedDataWidth, cachedUnfoldedDataHeight;

        private String unfoldedData = "";

        private static final char[] hexLookup = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

        DebugWindow(int width, int height) {

            buffer = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            w = width;
            h = height;
            int x = (screenSize.width - w) / 2;
            int y = (screenSize.height - h) / 2;

            codeX = w / 40;
            codeY = codeX;
            codeW = w * 75 / 100;
            codeH = h - codeY * 2;

            tapeY = codeY;
            tapeH = codeH;
            tapeW = w - codeX * 3 - codeW;
            tapeX = w - codeX - tapeW;

            g2d = (Graphics2D) buffer.getGraphics();
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setBackground(COLOR_BG);
            g2d.setFont(font);

            FontMetrics metrics = g2d.getFontMetrics();

            cachedFontH         = metrics.getHeight();
            cachedFontW         = (int) metrics.getStringBounds("@", null).getWidth();
            cachedLinesToBottom = filedata.length - codeH / cachedFontH;
            codeOffsetY         = Utils.clampi(Interpreter.tokens[0].row * cachedFontH - codeH / 2, 0, cachedLinesToBottom * cachedFontH);

            addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    int savedRow = Interpreter.ip < Interpreter.tokens.length ? Interpreter.tokens[Interpreter.ip].row : 0;

                    switch(e.getKeyCode()) {
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

                            } else if (Interpreter.tokens[Interpreter.ip].type == Token.Type.JEZ || Interpreter.tokens[Interpreter.ip].type == Token.Type.UNSAFEJEZ) {

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
                        int newOffsetY = codeOffsetY - (savedRow - Interpreter.tokens[Interpreter.ip].row) * cachedFontH;
                        codeOffsetY = Utils.clampi(newOffsetY, 0, cachedLinesToBottom * cachedFontH);
                    }
                }
            });

            addMouseMotionListener(new MouseMotionAdapter() {
                @Override
                public void mouseMoved(MouseEvent e) {
                    findMouseToken(e.getX(), e.getY());
                }
            });

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {

                    if (Interpreter.finished) return;
                    int b = e.getButton();

                    if (b == MouseEvent.BUTTON1 && !unfolding) {

                        if (mouseToken != null) {
                            while (Interpreter.ip < Interpreter.tokens.length && !Interpreter.tokens[Interpreter.ip].eq(mouseToken)) {
                                Interpreter.execute();
                            }
                        }

                    } else if (b == MouseEvent.BUTTON3 && mouseToken != null && mouseToken.type == Token.Type.WRD) {

                        if (unfolding) {

                            int mouseTokenLoc = 0;
                            for (; mouseTokenLoc < unfoldedTokens.length; mouseTokenLoc++) {
                                if (unfoldedTokens[mouseTokenLoc].eq(mouseToken)) break;
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

                            int mtkColDiff = endCol - unfoldedTokens[mouseTokenLoc].col + 1 - mouseToken.str.length();
                            int mtkRowDiff = endRow - unfoldedTokens[mouseTokenLoc].row;

                            for (int i = mouseTokenLoc; i < unfoldedTokens.length; i++) {
                                unfoldedTokens[i].row += mtkRowDiff;
                                if (unfoldedTokens[i].row == mouseToken.row) {
                                    unfoldedTokens[i].col += mtkColDiff;
                                }
                            }

                            Token[] temp = unfoldedTokens;
                            unfoldedTokens = new Token[unfoldedTokens.length + macroTokens.length - 1];

                            Token[] macroTokensCopy = Token.deepCopy(macroTokens);
                            System.arraycopy(temp, 0, unfoldedTokens, 0, mouseTokenLoc);
                            System.arraycopy(macroTokensCopy, 0, unfoldedTokens, mouseTokenLoc, macroTokens.length);
                            System.arraycopy(temp, mouseTokenLoc + 1, unfoldedTokens, mouseTokenLoc + macroTokens.length, temp.length - mouseTokenLoc - 1);

                            updateUnfoldedData();

                            mouseToken = null;

                        } else {

                            unfoldedTokens = Parser.macros.get(mouseToken.str);

                            int startRow = unfoldedTokens[0].row;
                            int startCol = unfoldedTokens[0].col;

                            for (int i = 0; i < unfoldedTokens.length; i++) {
                                unfoldedTokens[i].row -= startRow;
                            }

                            for (int i = 0; i < unfoldedTokens.length; i++) {
                                if (unfoldedTokens[i].row != startRow) break;
                                unfoldedTokens[i].col -= startCol;
                            }

                            updateUnfoldedData();
                            unfolding  = true;
                            mouseToken = null;
                        }
                    }
                }
            });

            addMouseWheelListener(new MouseAdapter() {
                @Override
                public void mouseWheelMoved(MouseWheelEvent e) {
                    codeOffsetY = Utils.clampi(codeOffsetY + e.getWheelRotation() * cachedFontH, 0, cachedLinesToBottom * cachedFontH);
                    findMouseToken(e.getX(), e.getY());
                }
            });

            pack();
            Insets insets = getInsets();
            setSize(width + insets.left + insets.right, height + insets.top + insets.bottom);
            setLocation(x, y);
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            setVisible(true);
        }

        public void paint(Graphics g) {
            g2d.clearRect(0, 0, w, h);

            g2d.setColor(COLOR_DATA);
            g2d.fillRect(codeX, codeY, codeW, codeH);
            g2d.fillRect(tapeX, tapeY, tapeW, tapeH);

            if (Interpreter.finished) {
                g2d.setColor(Color.ORANGE);
                String execFinished = "Execution finished, press \"SPACE\" to restart";
                g2d.drawString(execFinished, codeX + codeW / 2 - (cachedFontW * execFinished.length() / 2), codeY + codeH + cachedFontH);
            }

            g2d.setColor(Color.WHITE);

            // Memory values
            {
                int x    = tapeX;
                int y    = tapeY + cachedFontH;
                int valW = cachedFontW * 3;
                for (int i = 0; i < Interpreter.tape.length; i++) {
                    String val = hex(Interpreter.tape[i]);
                    g2d.drawString(val, x + 8, y);
                    if (i == Interpreter.pointer) {
                        g2d.setColor(Color.ORANGE);
                        g2d.drawRect(x + 8, y - cachedFontH + 4, cachedFontW * 2, cachedFontH);
                        g2d.setColor(Color.WHITE);
                    }
                    x += valW;
                    if (x >= w - codeX - valW) {
                        x = tapeX;
                        y += cachedFontH;
                    }
                }
            }

            g2d.setClip(codeX, codeY, codeW, codeH);

            // Program
            {
                if (unfolding) g2d.setColor(Color.LIGHT_GRAY);
                int y = codeY + cachedFontH - codeOffsetY;
                for (int i = 0; i < filedata.length; i++) {
                    g2d.drawString(filedata[i], codeX + 8, y);
                    y += cachedFontH;
                }
            }

            // Current token outline
            if (!Interpreter.finished) {
                g2d.setColor(Color.ORANGE);
                Token tk    = Interpreter.tokens[Interpreter.ip];
                int   prevX = Integer.MIN_VALUE;
                int   prevY = Integer.MIN_VALUE;
                while (tk != null) {
                    int ipX = codeX + tk.col * cachedFontW + 8;
                    int ipY = codeY + tk.row * cachedFontH + 5 - codeOffsetY;
                    int ipW = tk.len() * cachedFontW;
                    int ipH = cachedFontH;
                    g2d.drawRect(ipX, ipY, ipW, ipH);
                    g2d.setColor(Color.GRAY);

                    // TODO: maybe a pretty drawArc()?)

                    if (prevX != Integer.MIN_VALUE && prevY != Integer.MIN_VALUE) {
                        g2d.drawLine(ipX + ipW / 2, ipY + ipH / 2, prevX, prevY);
                    }

                    prevX = ipX + ipW / 2;
                    prevY = ipY + ipH / 2;
                    tk    = tk.origin;
                }
            }

            // Unfolding window
            if (unfolding) {
                g2d.setColor(COLOR_DATA);
                String[] uData = unfoldedData.split("\n", -1);
                int      w     = 0;
                int      h     = uData.length * cachedFontH;
                for (String s: uData) {
                    int len = s.length() * cachedFontW;
                    if (len > w) w = len;
                }
                int x = codeX / 2 + codeW / 2 - w / 2;
                int y = codeY / 2 + codeH / 2 - h / 2;
                g2d.fillRoundRect(x, y, w + codeX, h + codeY, 5, 5);
                g2d.setColor(Color.WHITE);
                g2d.drawRoundRect(x, y, w + codeX, h + codeY, 5, 5);
                for (int i = 0; i < uData.length; i++) {
                    g2d.drawString(uData[i], x + codeX / 2, y + codeY);
                    y += cachedFontH;
                }
            }

            // Token under mouse outline
            if (mouseToken != null) {
                int ipX, ipY, ipW, ipH;
                if (unfolding) {
                    int      ufw   = 0;
                    String[] uData = unfoldedData.split("\n", -1);
                    for (String s: uData) {
                        int len = s.length() * cachedFontW;
                        if (len > ufw) ufw = len;
                    }
                    ipX = (int) (codeX + codeW / 2.f - ufw / 2.f + mouseToken.col * cachedFontW) - 2;
                    ipY = (int) (codeX + codeH / 2.f + (-uData.length / 2.f + mouseToken.row) * cachedFontH);
                } else {
                    ipX = codeX + mouseToken.col * cachedFontW + 8;
                    ipY = codeY + mouseToken.row * cachedFontH + 5 - codeOffsetY;
                }
                ipW = mouseToken.len() * cachedFontW;
                ipH = cachedFontH;
                g2d.setColor(Color.CYAN);
                g2d.drawRect(ipX, ipY, ipW, ipH);
            }

            g2d.setClip(null);
            Insets insets = getInsets();
            g.drawImage(buffer, insets.left, insets.top, this);
        }

        private void findMouseToken(int mouseX, int mouseY) {
            if (unfolding) {

                float unfoldingWindowX = codeX / 2.f + codeW / 2.f - cachedUnfoldedDataWidth / 2.f;
                float unfoldingWindowY = codeY / 2.f + codeH / 2.f - cachedUnfoldedDataHeight / 2.f;

                int row = (int) (((float) mouseY - unfoldingWindowY) / cachedFontH - 2.5f) + unfoldedTokens[0].row;
                int col = (int) (((float) mouseX - unfoldingWindowX) / cachedFontW - 2.5f) + unfoldedTokens[0].col;
                mouseToken = getTokenByRowCol(unfoldedTokens, row, col);

            } else {

                int row = (int) (((float) mouseY - codeY + codeOffsetY) / cachedFontH - 1.5f);
                int col = (int) (((float) mouseX - codeX) / cachedFontW - 1.5f);
                mouseToken = getTokenByRowCol(Interpreter.tokens, row, col);

            }
        }

        private static Token getTokenByRowCol(Token[] array, int row, int col) {
            for (Token t: array) if (t.row == row && t.col <= col && t.col + t.len() >= col) return t;
            return null;
        }

        private void updateUnfoldedData() {

            int unfoldedDataLines = 1;

            StringBuilder sb = new StringBuilder(unfoldedTokens[0].repr());
            for (int i = 1; i < unfoldedTokens.length; i++) {
                if (unfoldedTokens[i].row > unfoldedTokens[i - 1].row) {
                    sb.append("\n").append(" ".repeat(unfoldedTokens[i].col));
                    unfoldedDataLines++;
                } else {
                    sb.append(" ".repeat(unfoldedTokens[i].col - unfoldedTokens[i - 1].col - unfoldedTokens[i - 1].len()));
                }
                sb.append(unfoldedTokens[i].repr());
            }
            unfoldedData = sb.toString();

            cachedUnfoldedDataHeight = unfoldedDataLines * cachedFontH;
            cachedUnfoldedDataWidth  = 0;
            for (String s: unfoldedData.split("\n", -1)) {
                int currentWidth = s.length() * cachedFontW;
                if (currentWidth > cachedUnfoldedDataWidth) cachedUnfoldedDataWidth = currentWidth;
            }
        }

        private static String hex(byte n) {
            int m = (int) n & 0xFF;
            return String.valueOf(hexLookup[m >> 4]) + hexLookup[m % 16];
        }
    }
}
