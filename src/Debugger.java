import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Arrays;

public class Debugger {

    public static boolean unfolding = false;

    private static String[] filedata = {""};

    public static Token   mouseToken;
    public static Token[] unfoldedTokens = new Token[0];

    public static void main(String[] args) throws InterruptedException {
        if (args.length < 1) {
            Utils.error("No file was provided. Please provide a `.bf`, `.bfn` or `.bfnx` file as a command line argument.");
        }
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

        DebugWindow debugWindow = new DebugWindow(1400, 785);
        while (true) {
            debugWindow.repaint();
            Thread.sleep(30);
        }
    }

    public static class DebugWindow extends JFrame {

        private static int w, h;
        private static int tapeX;
        private static int tapeY;
        private static int tapeW;
        private static int tapeH;
        public static  int codeX;
        public static  int codeY;
        public static  int codeW;
        public static  int codeH;
        public static  int codeOffsetY = 0;
        public static  int cachedFontW;
        public static  int cachedFontH;
        public static  int cachedLinesToBottom;
        private static int cachedUnfoldedDataWidth;
        private static int cachedUnfoldedDataHeight;

        private static String unfoldedData = "";

        public static        Insets        insets;
        private final        Graphics2D    g2d;
        private final        BufferedImage buffer;
        private static final Font          font     = new Font("Roboto Mono", Font.PLAIN, 16);
        public static final  Point         mousePos = new Point();

        public enum COLORS {
            COLOR_BG(0x153243),
            COLOR_DATA(0x284B63),
            COLOR_TEXT(0xFFFFFF),
            COLOR_TEXT_FADED(0x7EA8BE),
            COLOR_HIGHLIGHT(0x5EF38C),
            COLOR_SELECT(0xFFC69B),
            COLOR_CONNECTION(0xD9E985);

            final Color val;

            COLORS(int i) {
                val = new Color(i);
            }
        }

        public static boolean colorMode     = false;
        public static COLORS  selectedColor = null;
        public static String  newColorVal   = "";


        DebugWindow(int width, int height) {
            w     = width;
            h     = height;
            codeX = w / 40;
            codeY = w / 40;
            codeW = w * 65 / 100;
            codeH = h - codeY * 2;
            tapeW = (int) (w - (codeX * 3.1f) - codeW);
            tapeH = codeH;
            tapeX = w - codeX - tapeW;
            tapeY = codeY;

            buffer = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            g2d    = (Graphics2D) buffer.getGraphics();
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setFont(font);
            FontMetrics metrics = g2d.getFontMetrics();
            cachedFontH         = metrics.getHeight();
            cachedFontW         = (int) metrics.getStringBounds("@", null).getWidth();
            cachedLinesToBottom = filedata.length - codeH / cachedFontH;
            codeOffsetY         = Utils.clampi(Arrays.stream(Interpreter.tokens).findFirst().get().row * cachedFontH - codeH / 2, 0, cachedLinesToBottom * cachedFontH);

            addKeyListener(IOHandler.getKeyHandler());
            addMouseListener(IOHandler.getMouseHandler());
            addMouseWheelListener(IOHandler.getMouseWheelHandler());
            addMouseMotionListener(IOHandler.getMouseMotionHandler());
            pack();

            insets = getInsets();
            setSize(w + insets.left + insets.right, h + insets.top + insets.bottom);

            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            setLocation((screenSize.width - w) / 2, (screenSize.height - h) / 2);

            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            setVisible(true);
        }

        public void paint(Graphics g) {
            g2d.setColor(COLORS.COLOR_BG.val);
            g2d.fillRect(0, 0, w, h);

            g2d.setColor(COLORS.COLOR_DATA.val);
            g2d.fillRect(codeX, codeY, codeW, codeH);
            g2d.fillRect(tapeX, tapeY, tapeW, tapeH);

            // Colors
            final int colX = codeX >> 3;
            for (int i = 0; i < COLORS.values().length; i++) {
                int x = codeX * 3 * (i + 1);
                int y = colX;
                int w = codeX * 3 - colX;
                int h = codeY - colX * 2;
                if (selectedColor != null && i == selectedColor.ordinal()) {
                    g2d.setColor(COLORS.COLOR_HIGHLIGHT.val);
                } else {
                    g2d.setColor(COLORS.COLOR_DATA.val);
                }
                g2d.fillRect(x, y, w, h);
                g2d.setColor(COLORS.COLOR_TEXT.val);
                if (selectedColor != null && i == selectedColor.ordinal()) {
                    g2d.drawString(newColorVal, x + w / 5, (int) (y + h * .8));
                } else {
                    g2d.drawString(hexColor(COLORS.values()[i].val.getRGB()), x + w / 5, (int) (y + h * .8));
                }
            }

            if (Interpreter.finished) {
                g2d.setColor(COLORS.COLOR_HIGHLIGHT.val);
                String execFinished = "Execution finished, press \"SPACE\" to restart";
                g2d.drawString(execFinished, codeX + codeW / 2 - (cachedFontW * execFinished.length() / 2), codeY + codeH + cachedFontH);
            }

            g2d.setColor(COLORS.COLOR_TEXT.val);

            // Memory values
            {
                int x    = tapeX;
                int y    = tapeY + cachedFontH;
                int valW = (int) (cachedFontW * 3.1f);
                for (int i = 0; i < Interpreter.tape.length; i++) {
                    String val = hex(Interpreter.tape[i]);
                    g2d.drawString(val, x + 8, y);
                    if (i == Interpreter.pointer) {
                        g2d.setColor(COLORS.COLOR_HIGHLIGHT.val);
                        g2d.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_BEVEL));
                        g2d.drawRect(x + 8, y - cachedFontH + 4, cachedFontW * 2, cachedFontH);
                        g2d.setColor(COLORS.COLOR_TEXT.val);
                        g2d.setStroke(new BasicStroke(1));
                    }
                    x += valW;
                    if (x >= w - codeX - valW) {
                        x = tapeX;
                        y += cachedFontH;
                        if (y > codeY + codeH) break;
                    }
                }
            }

            g2d.setClip(codeX, codeY, codeW, codeH);

            // Program
            {
                if (unfolding) g2d.setColor(COLORS.COLOR_TEXT_FADED.val);
                int y = codeY + cachedFontH - codeOffsetY;
                for (int i = 0; i < filedata.length; i++) {
                    g2d.drawString(filedata[i], codeX + 8, y);
                    y += cachedFontH;
                }
            }

            // Current token outline
            if (!Interpreter.finished && Interpreter.tokens.length > 0) {
                g2d.setColor(COLORS.COLOR_HIGHLIGHT.val);
                g2d.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_BEVEL));
                Token tk    = Interpreter.tokens[Interpreter.ip];
                int   prevX = Integer.MIN_VALUE;
                int   prevY = Integer.MIN_VALUE;
                while (tk != null) {
                    int ipX = codeX + tk.col * cachedFontW + 8;
                    int ipY = codeY + tk.row * cachedFontH + 5 - codeOffsetY;
                    int ipW = tk.len() * cachedFontW;
                    int ipH = cachedFontH;
                    g2d.drawRect(ipX, ipY, ipW, ipH);
                    g2d.setStroke(new BasicStroke(1));
                    g2d.setColor(COLORS.COLOR_CONNECTION.val);

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
                g2d.setColor(COLORS.COLOR_DATA.val);
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
                g2d.setColor(COLORS.COLOR_SELECT.val);
                g2d.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_BEVEL));
                g2d.drawRect(ipX, ipY, ipW, ipH);
            }

            g2d.setClip(null);
            g.drawImage(buffer, insets.left, insets.top, this);
        }

        public static void findMouseToken(int mouseX, int mouseY) {
            if (unfolding) {

                float unfoldingWindowX = codeX / 2.f + codeW / 2.f - cachedUnfoldedDataWidth / 2.f;
                float unfoldingWindowY = codeY / 2.f + codeH / 2.f - cachedUnfoldedDataHeight / 2.f;

                int row = (int) (((float) mouseY - unfoldingWindowY) / cachedFontH) + unfoldedTokens[0].row;
                int col = (int) (((float) mouseX - unfoldingWindowX) / cachedFontW) + unfoldedTokens[0].col;
                mouseToken = getTokenByRowCol(unfoldedTokens, row, col);

            } else {

                int row = (int) (((float) mouseY - codeY + codeOffsetY) / cachedFontH);
                int col = (int) (((float) mouseX - codeX) / cachedFontW);
                mouseToken = getTokenByRowCol(Interpreter.tokens, row, col);

            }
        }

        private static Token getTokenByRowCol(Token[] array, int row, int col) {
            for (Token t: array) if (t.row == row && t.col <= col && t.col + t.len() >= col) return t;
            return null;
        }

        public static void updateUnfoldedData() {

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
            final char[] hexLookup = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
            int          m         = (int) n & 0xFF;
            return String.valueOf(hexLookup[m >> 4]) + hexLookup[m % 16];
        }

        public static String hexColor(int color) {
            byte r = (byte) (color >> 0x10 & 0xFF);
            byte g = (byte) (color >> 0x08 & 0xFF);
            byte b = (byte) (color >> 0x00 & 0xFF);
            return "%s%s%s".formatted(hex(r), hex(g), hex(b));
        }
    }
}
