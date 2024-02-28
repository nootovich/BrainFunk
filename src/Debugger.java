import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import java.util.*;

public class Debugger {

    public static String filename;

    public static void main(String[] args) throws InterruptedException {
        if (args.length < 1) error("Please provide a .bf or .bfn file as a command line argument.");
        String filepath = args[0];
        filename = new File(filepath).getName();
        if (!filename.endsWith(".bfn") && !filename.endsWith(".bf"))
            error("Invalid file format. Please provide a .bf or .bfn file.");
        info("Debugging %s file.".formatted(filename));

        DebugWindow debugWindow = new DebugWindow(1400, 785, filepath);
        while (true) {
            debugWindow.repaint();
            Thread.sleep(30);
        }
    }

    private static void info(String message) {
        StackTraceElement src = Thread.currentThread().getStackTrace()[2];
        System.out.printf("%s:%d [INFO]: %s%n", src.getFileName(), src.getLineNumber(), message);
    }

    private static void error(String message) {
        StackTraceElement errSrc = Thread.currentThread().getStackTrace()[2];
        System.out.printf("%s:%d [ERROR]: %s%n", errSrc.getFileName(), errSrc.getLineNumber(), message);
        System.exit(1);
    }

    private static class DebugWindow extends JFrame {

        private boolean finished  = false;
        private boolean unfolding = false;

        public int x, y, w, h;
        public int codeX, codeY, codeW, codeH;
        public int tapeX, tapeY, tapeW, tapeH;
        public int codeOffsetY;

        private static Token         mouseToken;
        private final  BufferedImage buffer;
        private final  Graphics2D    g2d;
        private final  Font          font             = new Font("Roboto Mono", Font.PLAIN, 16);
        private        String[]      filedata         = {""};
        private        String        unfoldedData     = "";
        private        int           cachedFontH      = 0;
        private        int           cachedFontW      = 0;
        private        int           cachedLineAmount = 0;

        private final Color COLOR_BG   = new Color(0x3D4154);
        private final Color COLOR_DATA = new Color(0x4C5470);

        private static final char[] hexLookup = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

        private static int ip = 0;

        private static Token[] tokens         = new Token[0];
        private static Token[] lexedTokens    = new Token[0];
        private static Token[] unfoldedTokens = new Token[0];


        DebugWindow(int w, int h, String filepath) {
            this.filedata = FileSystem.loadFile(filepath).split("\n", -1);

            lexedTokens = Lexer.lexFile(filepath);
            info("Lexer OK.");

            tokens = filename.endsWith(".bfn") ? DebugParser.parseTokens(lexedTokens) : lexedTokens;
            info("Parser OK.");

            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            this.w = w;
            this.h = h;
            this.x = (screenSize.width - w) / 2;
            this.y = (screenSize.height - h) / 2;

            this.codeX = this.w / 40;
            this.codeY = codeX;
            this.codeW = this.w * 75 / 100;
            this.codeH = this.h - codeY * 2;

            this.tapeY = codeY;
            this.tapeH = codeH;
            this.tapeW = this.w - codeX * 3 - codeW;
            this.tapeX = this.w - codeX - tapeW;

            buffer = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            g2d    = (Graphics2D) buffer.getGraphics();
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setBackground(COLOR_BG);
            g2d.setFont(font);
            FontMetrics metrics = g2d.getFontMetrics();
            cachedFontH      = metrics.getHeight();
            cachedFontW      = (int) metrics.getStringBounds("@", null).getWidth();
            cachedLineAmount = codeH / cachedFontH;
            this.codeOffsetY = tokens[0].row - cachedLineAmount / 2;
            if (codeOffsetY < 0) codeOffsetY = 0;
            else if (codeOffsetY >= filedata.length - cachedLineAmount) codeOffsetY = filedata.length - cachedLineAmount - 1;

            addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    int key = e.getKeyCode();
                    if (key == KeyEvent.VK_ESCAPE) {
                        info("Debugger terminated by user.");
                        System.exit(0);
                    } else if (key == KeyEvent.VK_SPACE) {
                        if (finished) {
                            finished = false;
                            DebugInterpreter.reset();
                            ip = 0;
                        } else {
                            int prevRow = tokens[ip].row;
                            if (tokens[ip].type == Token.Type.MACRO) {
                                String macroName = tokens[ip].strValue;
                                int    macroLvl  = getLevel(tokens[ip]);
                                ip++;
                                while (ip < tokens.length && getOriginOfLevel(tokens[ip], macroLvl).equals(macroName)) {
                                    DebugInterpreter.debugExecuteBrainFunk(tokens[ip]);
                                    ip++;
                                }
                            } else if (tokens[ip].type == Token.Type.WHILE) {
                                Token start      = tokens[ip];
                                int   startDepth = DebugInterpreter.whileDepth;
                                do {
                                    DebugInterpreter.debugExecuteBrainFunk(tokens[ip]);
                                    ip++;
                                }
                                while (ip < tokens.length &&
                                       (DebugInterpreter.whileDepth !=
                                        startDepth ||
                                        tokens[ip].eq(start)));
                            } else {
                                DebugInterpreter.debugExecuteBrainFunk(tokens[ip]);
                                ip++;
                            }
                            if (ip < tokens.length) {
                                codeOffsetY -= prevRow - tokens[ip].row;
                                if (codeOffsetY < 0) codeOffsetY = 0;
                                else if (codeOffsetY >= filedata.length - cachedLineAmount) codeOffsetY = filedata.length - cachedLineAmount - 1;
                            }
                        }
                    } else if (!finished && key == KeyEvent.VK_ENTER) {
                        int prevRow = tokens[ip].row;
                        DebugInterpreter.debugExecuteBrainFunk(tokens[ip]);
                        ip++;
                        if (ip < tokens.length) {
                            codeOffsetY -= prevRow - tokens[ip].row;
                            if (codeOffsetY < 0) codeOffsetY = 0;
                            else if (codeOffsetY >= filedata.length - cachedLineAmount) codeOffsetY = filedata.length - cachedLineAmount - 1;

                        }
                    }
                    if (ip >= tokens.length) finished = true;
                }

                private int getLevel(Token tk) {
                    int result = 0;
                    while (tk.origin != null) {
                        tk = tk.origin;
                        result++;
                    }
                    return result;
                }

                private String getOriginOfLevel(Token tk, int lvl) {
                    int tkLvl = getLevel(tk);
                    while (tk.origin != null && tkLvl > lvl) {
                        tk = tk.origin;
                        tkLvl--;
                    }
                    if (tkLvl == lvl && tk.type == Token.Type.MACRO) return tk.strValue;
                    return "";
                }
            });

            addMouseMotionListener(new MouseMotionAdapter() {
                @Override
                public void mouseMoved(MouseEvent e) {
                    if (unfolding) {
                        String[] uData = unfoldedData.split("\n", -1);
                        int      ufw   = 0;
                        int      ufh   = uData.length * cachedFontH;
                        for (String s: uData) {
                            int len = s.length() * cachedFontW;
                            if (len > ufw) ufw = len;
                        }
                        int   ufx = codeX / 2 + codeW / 2 - ufw / 2;
                        int   ufy = codeY / 2 + codeH / 2 - ufh / 2;
                        float row = (float) (e.getY() - ufy) / cachedFontH - 2.5f;
                        float col = (float) (e.getX() - ufx) / cachedFontW - 2.5f;
                        row += unfoldedTokens[0].row;
                        col += unfoldedTokens[0].col;
                        for (Token tk: unfoldedTokens) {
                            if (tk.row < row && tk.row + 1 > row && tk.col <= col && col < tk.col + tokenLen(tk)) {
                                mouseToken = tk;
                                return;
                            }
                        }
                    } else {
                        float row = (float) (e.getY() - codeY + codeOffsetY * cachedFontH) / cachedFontH - 1.5f;
                        float col = (float) (e.getX() - codeX) / cachedFontW - 1.5f;
                        for (Token tk: tokens) {
                            if (tk.row < row && tk.row + 1 > row && tk.col <= col && col < tk.col + tokenLen(tk)) {
                                mouseToken = tk;
                                return;
                            }
                        }
                    }
                    mouseToken = null;
                }
            });

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (finished) return;
                    int b = e.getButton();
                    if (b == MouseEvent.BUTTON1 && !unfolding) {
                        if (mouseToken != null) {
                            while (ip < tokens.length && !tokens[ip].eq(mouseToken)) {
                                DebugInterpreter.debugExecuteBrainFunk(tokens[ip]);
                                ip++;
                            }
                        } else {
                            int row = (e.getY() - codeY - 4) / cachedFontH - 1;
                            while (ip < tokens.length && tokens[ip].row <= row) {
                                DebugInterpreter.debugExecuteBrainFunk(tokens[ip]);
                                ip++;
                            }
                        }
                        if (ip >= tokens.length) finished = true;
                    } else if (b == MouseEvent.BUTTON3 && mouseToken != null && mouseToken.type == Token.Type.MACRO) {
                        if (unfolding) {
                            Token[] mtks = findLexedMacroDef(mouseToken.strValue).macroTokens;
                            int     brow = mtks[0].row;
                            int     bcol = mtks[0].col;
                            for (int i = 0; i < mtks.length; i++) {
                                mtks[i].row -= brow;
                                mtks[i].row += mouseToken.row;
                            }
                            brow = mtks[0].row;
                            int lrow = mtks[mtks.length - 1].row;

                            int i = 0;
                            for (; i < unfoldedTokens.length; i++) {
                                if (unfoldedTokens[i].eq(mouseToken)) break;
                            }
                            for (int j = 0; j < mtks.length; j++) {
                                if (mtks[j].row != brow) break;
                                mtks[j].col -= bcol;
                                mtks[j].col += mouseToken.col;
                            }
                            int lcol = mtks[mtks.length - 1].col;
                            if (mtks[mtks.length - 1].type == Token.Type.MACRO) {
                                lcol += mtks[mtks.length - 1].strValue.length() - 1;
                            }
                            int mtkColDiff = lcol - unfoldedTokens[i].col + 1 - mouseToken.strValue.length();
                            int mtkRowDiff = lrow - unfoldedTokens[i].row;
                            for (int j = i; j < unfoldedTokens.length; j++) {
                                unfoldedTokens[j].row += mtkRowDiff;
                                if (unfoldedTokens[j].row == mouseToken.row) {
                                    unfoldedTokens[j].col += mtkColDiff;
                                }
                            }


                            Token[] temp = unfoldedTokens.clone();
                            unfoldedTokens = new Token[unfoldedTokens.length + mtks.length - 1];

                            Token[] mtksCopy = new Token[mtks.length];
                            for (int j = 0; j < mtks.length; j++) {
                                Token savedToken = mtks[j];
                                mtksCopy[j]          = new Token(savedToken.type, savedToken.file, savedToken.row, savedToken.col);
                                mtksCopy[j].numValue = savedToken.numValue;
                                mtksCopy[j].strValue = savedToken.strValue;
                            }

                            System.arraycopy(temp, 0, unfoldedTokens, 0, i);
                            System.arraycopy(mtksCopy, 0, unfoldedTokens, i, mtks.length);
                            System.arraycopy(temp, i + 1, unfoldedTokens, i + mtks.length, temp.length - i - 1);

                            StringBuilder sb = new StringBuilder(unfoldedTokens[0].repr());
                            for (i = 1; i < unfoldedTokens.length; i++) {
                                Token cuft = unfoldedTokens[i];
                                Token puft = unfoldedTokens[i - 1];
                                if (cuft.row > puft.row) {
                                    sb.append("\n");
                                    sb.append(" ".repeat(cuft.col));
                                } else {
                                    int colDiff = cuft.col - puft.col - puft.repr().length();
                                    sb.append(" ".repeat(colDiff));
                                }
                                sb.append(cuft.repr());
                            }
                            unfoldedData = sb.toString();
                            mouseToken   = null;
                        } else if (mouseToken != null) {
                            Token lexed = findLexedMacroDef(mouseToken.strValue);
                            if (lexed == null || lexed.type != Token.Type.MACRODEF) return;
                            unfoldedTokens = lexed.macroTokens;
                            int brow = unfoldedTokens[0].row;
                            int bcol = unfoldedTokens[0].col;
                            for (int i = 0; i < unfoldedTokens.length; i++) {
                                if (unfoldedTokens[i].row != brow) break;
                                unfoldedTokens[i].col -= bcol;
                            }
                            for (int i = 0; i < unfoldedTokens.length; i++) {
                                unfoldedTokens[i].row -= brow;
                            }

                            StringBuilder sb = new StringBuilder(unfoldedTokens[0].repr());
                            for (int i = 1; i < unfoldedTokens.length; i++) {
                                Token cuft = unfoldedTokens[i];
                                Token puft = unfoldedTokens[i - 1];
                                if (cuft.row > puft.row) {
                                    sb.append("\n");
                                } else {
                                    int colDiff = cuft.col - puft.col - puft.repr().length();
                                    sb.append(" ".repeat(colDiff));
                                }
                                sb.append(cuft.repr());
                            }
                            unfoldedData = sb.toString();
                            unfolding    = true;
                            mouseToken   = null;
                        }
                    }
                }

                private Token findLexedMacroDef(String strValue) {
                    for (Token t: lexedTokens) {
                        if (t.type == Token.Type.MACRODEF && t.strValue.equals(strValue)) return t;
                    }
                    return null;
                }
            });

            addMouseWheelListener(new MouseAdapter() {
                @Override
                public void mouseWheelMoved(MouseWheelEvent e) {
                    codeOffsetY += e.getWheelRotation();
                    if (codeOffsetY < 0) codeOffsetY = 0;
                    else if (codeOffsetY >= filedata.length - cachedLineAmount) codeOffsetY = filedata.length - cachedLineAmount - 1;
                }
            });

            pack();
            Insets insets = getInsets();
            setSize(w + insets.left + insets.right, h + insets.top + insets.bottom);
            setLocation(x, y);
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            setVisible(true);
        }

        public void paint(Graphics g) {
            g2d.clearRect(0, 0, w, h);

            g2d.setColor(COLOR_DATA);
            g2d.fillRect(codeX, codeY, codeW, codeH);
            g2d.fillRect(tapeX, tapeY, tapeW, tapeH);

            if (finished) {
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
                for (int i = 0; i < DebugInterpreter.tape.length; i++) {
                    String val = hex(DebugInterpreter.tape[i]);
                    g2d.drawString(val, x + 8, y);
                    if (i == DebugInterpreter.pointer) {
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
                int y = codeY + cachedFontH - codeOffsetY * cachedFontH;
                for (int i = 0; i < filedata.length; i++) {
                    g2d.drawString(filedata[i], codeX + 8, y);
                    y += cachedFontH;
                }
            }

            // Current token outline
            if (!finished) {
                g2d.setColor(Color.ORANGE);
                Token tk    = tokens[ip];
                int   prevX = Integer.MIN_VALUE;
                int   prevY = Integer.MIN_VALUE;
                while (tk != null) {
                    int ipX = codeX + tk.col * cachedFontW + 8;
                    int ipY = codeY + (-codeOffsetY + tk.row) * cachedFontH + 5;
                    int ipW = tokenLen(tk) * cachedFontW;
                    int ipH = cachedFontH;
                    g2d.drawRect(ipX, ipY, ipW, ipH);
                    g2d.setColor(Color.GRAY);

                    // TODO: maybe a pretty drawArc()?)

                    if (prevX != Integer.MIN_VALUE && prevY != Integer.MIN_VALUE) g2d.drawLine(ipX + ipW / 2, ipY + ipH / 2, prevX, prevY);

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
                    ipY = codeY + (-codeOffsetY + mouseToken.row) * cachedFontH + 5;
                }
                ipW = tokenLen(mouseToken) * cachedFontW;
                ipH = cachedFontH;
                g2d.setColor(Color.CYAN);
                g2d.drawRect(ipX, ipY, ipW, ipH);
            }

            g2d.setClip(null);
            Insets insets = getInsets();
            g.drawImage(buffer, insets.left, insets.top, this);
        }

        private static int tokenLen(Token tk) {
            return switch (tk.type) {
                case NUMBER -> (int) Math.floor(Math.log10(tk.numValue)) + 1;
                case STRING -> (tk.strValue.length() + 2);
                case MACRO -> tk.strValue.length();
                default -> 1;
            };
        }

        private static String hex(byte n) {
            int m = (int) n & 0xFF;
            return String.valueOf(hexLookup[m >> 4]) + hexLookup[m % 16];
        }
    }

    private static class DebugParser {

        private static final int RECURSION_LIMIT = 1000;
        private static       int recursionCount  = 0;

        private static final HashMap<String, Token[]> macros = new HashMap<>();

        private static Token[] parseTokens(Token[] tokens) {
            tokens = parseMacros(tokens, null);
            tokens = parsePointers(tokens);
            return tokens;
        }

        private static Token[] parseMacros(Token[] tokens, Token origin) {
            if (++recursionCount >= RECURSION_LIMIT)
                error("The recursion limit of %d was exceeded by: %s".formatted(RECURSION_LIMIT, origin));
            Stack<Token> parsed = new Stack<>();
            for (Token tk: tokens) {
                if (tk.origin == null) tk.origin = origin;
                if (tk.type == Token.Type.MACRODEF) {
                    if (macros.containsKey(tk.strValue)) error("Redefinition of a macro %s.".formatted(tk));
                    macros.put(tk.strValue, tk.macroTokens);
                } else if (tk.type == Token.Type.MACRO) {
                    int amount     = 1;
                    int parsedSize = parsed.size();
                    if (parsedSize > 0 && parsed.peek().type == Token.Type.NUMBER
                        && (parsedSize < 2 || parsed.get(parsedSize - 2).type != Token.Type.POINTER)) {
                        amount = parsed.pop().numValue;
                    }
                    if (!macros.containsKey(tk.strValue)) error("Undefined macro %s.".formatted(tk));

                    // manually deep copying current macro tokens to avoid making a copy of references
                    Token[] savedMacroTokens = macros.get(tk.strValue);
                    Token[] tokensToPass     = new Token[savedMacroTokens.length];
                    for (int i = 0; i < savedMacroTokens.length; i++) {
                        Token savedToken = savedMacroTokens[i];
                        tokensToPass[i]          = new Token(savedToken.type, savedToken.file, savedToken.row, savedToken.col);
                        tokensToPass[i].numValue = savedToken.numValue;
                        tokensToPass[i].strValue = savedToken.strValue;
                    }

                    List<Token> macroTokens = List.of(parseMacros(tokensToPass, tk));
                    for (int j = 0; j < amount; j++) {
                        parsed.push(tk);
                        parsed.addAll(macroTokens);
                    }
                } else parsed.push(tk);
            }
            recursionCount--;
            return parsed.toArray(new Token[0]);
        }

        private static Token[] parsePointers(Token[] tokens) {
            Stack<Token> parsed = new Stack<>();
            for (int i = 0; i < tokens.length; i++) {
                Token tk = tokens[i];
                if (tk.type == Token.Type.POINTER) {
                    if (i == tokens.length - 1 || tokens[i + 1].type != Token.Type.NUMBER)
                        error("Invalid argument for a pointer! Expected a number after: " + tk);
                    tk.numValue = tokens[++i].numValue;
                }
                parsed.push(tk);
            }
            return parsed.toArray(new Token[0]);
        }
    }

    private static class DebugInterpreter {

        private static final int TAPE_LEN       = 256;
        private static final int REPETITION_CAP = 1000;

        private static final byte[] tape    = new byte[TAPE_LEN];
        private static       int    pointer = 0;

        private static int savedVal   = -1;
        private static int whileDepth = 0;

        private static final Scanner         input       = new Scanner(System.in);
        private static final ArrayList<Byte> inputBuffer = new ArrayList<>();
        private static final Stack<Integer>  ptrHistory  = new Stack<>();

        private static void reset() {
            for (int i = 0; i < tape.length; i++) tape[i] = 0;
            pointer    = 0;
            whileDepth = 0;
            getVal();
            inputBuffer.clear();
            ptrHistory.clear();
        }

        private static void debugExecuteBrainFunk(Token tk) {
            switch (tk.type) {
                case ADD -> tape[pointer] += (byte) getVal();
                case SUB -> tape[pointer] -= (byte) getVal();
                case PTRADD -> ptradd(getVal());
                case PTRSUB -> ptradd(-getVal());
                case WHILE -> {
                    if (DebugWindow.ip == DebugWindow.tokens.length - 1)
                        error("Unmatched brackets at: " + DebugWindow.tokens[DebugWindow.ip]);
                    if (tape[pointer] == 0) {
                        int startIp    = DebugWindow.ip;
                        int startDepth = whileDepth;
                        whileDepth += getVal();
                        while (whileDepth > startDepth) {
                            tk = DebugWindow.tokens[++DebugWindow.ip];
                            if (tk.type == Token.Type.WHILE) whileDepth += getVal();
                            else if (tk.type == Token.Type.ENDWHILE) whileDepth -= getVal();
                            if (whileDepth != 0 && DebugWindow.ip == DebugWindow.tokens.length - 1)
                                error("Unmatched brackets at: " + DebugWindow.tokens[startIp]);
                        }
                    } else whileDepth += getVal();
                }
                case ENDWHILE -> {
                    // TODO: This is wrong. Mostly in the places of `getVal()`
                    int val = getVal();
                    for (int i = 0; i < val; i++) {
                        if (((int) tape[pointer] & 0xFF) > 0) {
                            int startIp    = DebugWindow.ip;
                            int startDepth = whileDepth - 1;
                            while (whileDepth > startDepth) {
                                tk = DebugWindow.tokens[--DebugWindow.ip];
                                if (tk.type == Token.Type.ENDWHILE) whileDepth += getVal();
                                else if (tk.type == Token.Type.WHILE) whileDepth -= getVal();
                                if (whileDepth != 0 && DebugWindow.ip == 0)
                                    error("Unmatched brackets at: " + DebugWindow.tokens[startIp]);
                            }
                            DebugWindow.ip--;
                        } else whileDepth--;
                    }
                }
                case WRITE -> write(getVal());
                case READ -> read(getVal());
                case NUMBER -> saveVal(tk);
                case STRING -> {
                    int val = getVal();
                    for (int j = 0; j < val; j++) {
                        for (int k = 0; k < tk.strValue.length(); k++) {
                            tape[pointer] = (byte) tk.strValue.charAt(k);
                            ptradd(1);
                        }
                    }
                }
                case POINTER -> {
                    int val = getVal();
                    for (int j = 0; j < val; j++) {
                        ptrHistory.push(pointer);
                        pointer = tk.numValue;
                    }
                }
                case RETURN -> {
                    int val = getVal();
                    for (int j = 0; j < val; j++) {
                        if (ptrHistory.isEmpty()) error("Not enough pointer history for: " + tk);
                        pointer = ptrHistory.pop();
                    }
                }
                case MACRO -> {}
                default -> error("Unknown token type `" + tk.type + "`");
            }
        }

        private static void ptradd(int n) {
            pointer = ((pointer + n) % TAPE_LEN + TAPE_LEN) % TAPE_LEN;
        }

        private static void saveVal(Token tk) {
            if (savedVal >= 0) error("Two consecutive numbers after one another are not supported: " + tk);
            savedVal = tk.numValue;
        }

        private static int getVal() {
            if (savedVal < 0) return 1;
            int temp = savedVal;
            savedVal = -1;
            return temp;
        }

        private static void read(int amount) {
            for (int repetitions = 0; inputBuffer.size() < amount && repetitions < REPETITION_CAP; repetitions++) {
                System.out.print("Awaiting input: ");
                char[] in = input.nextLine().toCharArray();
                for (char inChar: in) inputBuffer.add((byte) inChar);
            }
            if (inputBuffer.size() < amount)
                error("Not enough data for `READ` token. This should be unreachable unless there is a bug in processing user input.");
            for (int i = 0; i < amount; i++) {
                tape[pointer] = inputBuffer.get(0);
                inputBuffer.remove(0);
            }
        }

        private static void write(int amount) {
            for (int i = 0; i < amount; i++) System.out.print((char) tape[pointer]);
        }
    }
}
