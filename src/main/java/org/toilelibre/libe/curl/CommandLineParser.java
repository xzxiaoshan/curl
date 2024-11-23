package org.toilelibre.libe.curl;

import lombok.Data;
import lombok.Getter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 解析命令行输入参数
 * <p>
 * String line = "--option1=value1 --option2=\"value with spaces\" arg1 'arg2 with spaces' --option3='another value' \"escaped \\\" quote\"";
 * String[] args = CommandLineParser.getInstance().parseToArgs(input);
 * 正确的解析结果args.length应该是6个值
 * </p>
 *
 * @author 单红宇
 * @since 2024/11/23 8:30
 */
@Data
public class CommandLineParser {

    /**
     * 被视为引号的字符
     */
    private char[] quoteChars = {'\'', '"'};

    /**
     * 被视为转义符的字符
     */
    private char[] escapeChars = {'\\'};

    /**
     * 是否在未闭合的引号处结束解析
     */
    private boolean eofOnUnclosedQuote;

    /**
     * 是否在转义的换行符处结束解析
     */
    private boolean eofOnEscapedNewLine;

    /**
     * 开始括号字符数组，用于匹配嵌套结构
     */
    private char[] openingBrackets = null;

    /**
     * 结束括号字符数组，用于匹配嵌套结构
     */
    private char[] closingBrackets = null;

    /**
     * 单行注释分隔符数组，用于识别单行注释的开始
     */
    private String[] lineCommentDelims = null;

    /**
     * 块注释分隔符对象，用于识别块注释的开始和结束
     */
    private BlockCommentDelims blockCommentDelims = null;

    /**
     * 变量名的正则表达式，匹配以字母或下划线开头，后续可以是字母、数字、下划线、破折号，
     * 并且可以包含点、方括号等嵌套结构
     */
    private String regexVariable = "[a-zA-Z_]+[a-zA-Z0-9_-]*((\\.|\\['|\\[\"|\\[)[a-zA-Z0-9_-]*(|']|\"]|]))?";
    /**
     * 命令的正则表达式，匹配可选的前导冒号，后跟字母开头，后续可以是字母、数字、下划线、破折号
     */
    private String regexCommand = "[:]?[a-zA-Z]+[a-zA-Z0-9_-]*";
    /**
     * 命令匹配的正则表达式组索引，用于提取命令部分
     */
    private int commandGroup = 4;

    /**
     * build
     *
     * @return DefaultParser
     */
    public static CommandLineParser getInstance() {
        return new CommandLineParser();
    }

    /**
     * CommandLineParser
     */
    public CommandLineParser() {
        // default
    }

    /**
     * 解析命令行字符串，分隔为字符串数组
     *
     * @param line 要解析的命令行字符串。
     * @return 解析后分隔为字符串数组
     * @throws EOFError 如果解析过程中遇到语法错误，将抛出此异常。
     */
    public String[] parseToArgs(String line) throws EOFError {
        return parse(line).words().toArray(new String[0]);
    }

    /**
     * 解析命令行字符串
     *
     * @param line 要解析的命令行字符串。
     * @return 解析后的命令行对象 {@link ParsedLine}，包含解析结果和相关信息。
     * @throws EOFError 如果解析过程中遇到语法错误，将抛出此异常。
     */
    public ParsedLine parse(String line) throws EOFError {
        return parse(line, 0, ParseContext.UNSPECIFIED);
    }

    /**
     * 解析命令行字符串
     *
     * @param line   要解析的命令行字符串。
     * @param cursor 当前光标的位置，用于指示解析的起点。
     * @return 解析后的命令行对象 {@link ParsedLine}，包含解析结果和相关信息。
     * @throws EOFError 如果解析过程中遇到语法错误，将抛出此异常。
     */
    public ParsedLine parse(String line, int cursor) throws EOFError {
        return parse(line, cursor, ParseContext.UNSPECIFIED);
    }

    /**
     * 设置 lineCommentDelims
     * 此方法与 {@link CommandLineParser#setLineCommentDelims(String[])} 方法的功能相同
     *
     * @param lineCommentDelims lineCommentDelims
     * @return DefaultParser
     */
    public CommandLineParser lineCommentDelims(final String[] lineCommentDelims) {
        this.setLineCommentDelims(lineCommentDelims);
        return this;
    }

    /**
     * 设置 blockCommentDelims
     * 此方法与 {@link this.setBlockCommentDelims(BlockCommentDelims)} 方法的功能相同
     *
     * @param blockCommentDelims blockCommentDelims
     * @return DefaultParser
     */
    public CommandLineParser blockCommentDelims(final BlockCommentDelims blockCommentDelims) {
        this.setBlockCommentDelims(blockCommentDelims);
        return this;
    }

    /**
     * 设置 quoteChars
     *
     * @param chars chars
     * @return DefaultParser
     */
    public CommandLineParser quoteChars(final char[] chars) {
        this.setQuoteChars(chars);
        return this;
    }

    /**
     * 设置 escapeChars
     *
     * @param chars chars
     * @return DefaultParser
     */
    public CommandLineParser escapeChars(final char[] chars) {
        this.setEscapeChars(chars);
        return this;
    }

    /**
     * 设置 eofOnUnclosedQuote
     *
     * @param eofOnUnclosedQuote eofOnUnclosedQuote
     * @return DefaultParser
     */
    public CommandLineParser eofOnUnclosedQuote(boolean eofOnUnclosedQuote) {
        this.setEofOnUnclosedQuote(eofOnUnclosedQuote);
        return this;
    }

    /**
     * 设置 eofOnUnclosedBracket
     *
     * @param brackets brackets
     * @return DefaultParser
     */
    public CommandLineParser eofOnUnclosedBracket(CommandLineParser.Bracket... brackets) {
        setEofOnUnclosedBracket(brackets);
        return this;
    }

    /**
     * 设置 eofOnEscapedNewLine
     *
     * @param eofOnEscapedNewLine eofOnEscapedNewLine
     * @return DefaultParser
     */
    public CommandLineParser eofOnEscapedNewLine(boolean eofOnEscapedNewLine) {
        this.setEofOnEscapedNewLine(eofOnEscapedNewLine);
        return this;
    }

    /**
     * regexVariable
     *
     * @param regexVariable regexVariable
     * @return DefaultParser
     */
    public CommandLineParser regexVariable(String regexVariable) {
        this.setRegexVariable(regexVariable);
        return this;
    }

    /**
     * 设置 regexCommand
     *
     * @param regexCommand regexCommand
     * @return DefaultParser
     */
    public CommandLineParser regexCommand(String regexCommand) {
        this.setRegexCommand(regexCommand);
        return this;
    }

    /**
     * 设置 commandGroup
     *
     * @param commandGroup commandGroup
     * @return DefaultParser
     */
    public CommandLineParser commandGroup(int commandGroup) {
        this.setCommandGroup(commandGroup);
        return this;
    }

    /**
     * setEofOnUnclosedBracket
     *
     * @param brackets brackets
     */
    public void setEofOnUnclosedBracket(CommandLineParser.Bracket... brackets) {
        if (brackets == null) {
            openingBrackets = null;
            closingBrackets = null;
        } else {
            Set<CommandLineParser.Bracket> bs = new HashSet<>(Arrays.asList(brackets));
            openingBrackets = new char[bs.size()];
            closingBrackets = new char[bs.size()];
            int i = 0;
            for (CommandLineParser.Bracket b : bs) {
                switch (b) {
                    case ROUND:
                        openingBrackets[i] = '(';
                        closingBrackets[i] = ')';
                        break;
                    case CURLY:
                        openingBrackets[i] = '{';
                        closingBrackets[i] = '}';
                        break;
                    case SQUARE:
                        openingBrackets[i] = '[';
                        closingBrackets[i] = ']';
                        break;
                    case ANGLE:
                        openingBrackets[i] = '<';
                        closingBrackets[i] = '>';
                        break;
                }
                i++;
            }
        }
    }

    /**
     * validCommandName
     *
     * @param name name
     * @return data
     */
    public boolean validCommandName(String name) {
        return name != null && name.matches(regexCommand);
    }

    /**
     * validVariableName
     *
     * @param name name
     * @return data
     */
    public boolean validVariableName(String name) {
        return name != null && regexVariable != null && name.matches(regexVariable);
    }

    /**
     * getCommand
     *
     * @param line line
     * @return String
     */
    public String getCommand(final String line) {
        String out = "";
        boolean checkCommandOnly = regexVariable == null;
        if (!checkCommandOnly) {
            Pattern patternCommand = Pattern.compile("^\\s*" + regexVariable + "=(" + regexCommand + ")(\\s+|$)");
            Matcher matcher = patternCommand.matcher(line);
            if (matcher.find()) {
                out = matcher.group(commandGroup);
            } else {
                checkCommandOnly = true;
            }
        }
        if (checkCommandOnly) {
            out = line.trim().split("\\s+")[0];
            if (!out.matches(regexCommand)) {
                out = "";
            }
        }
        return out;
    }

    /**
     * getVariable
     *
     * @param line line
     * @return String
     */
    public String getVariable(final String line) {
        String out = null;
        if (regexVariable != null) {
            Pattern patternCommand = Pattern.compile("^\\s*(" + regexVariable + ")\\s*=[^=~].*");
            Matcher matcher = patternCommand.matcher(line);
            if (matcher.find()) {
                out = matcher.group(1);
            }
        }
        return out;
    }

    /**
     * parse
     *
     * @param line    line
     * @param cursor  cursor
     * @param context context
     * @return ParsedLine
     */
    @SuppressWarnings("all")
    public ParsedLine parse(final String line, final int cursor, ParseContext context) {
        List<String> words = new LinkedList<>();
        StringBuilder current = new StringBuilder();
        int wordCursor = -1;
        int wordIndex = -1;
        int quoteStart = -1;
        int rawWordCursor = -1;
        int rawWordLength = -1;
        int rawWordStart = 0;
        CommandLineParser.BracketChecker bracketChecker = new CommandLineParser.BracketChecker(cursor);
        boolean quotedWord = false;
        boolean lineCommented = false;
        boolean blockCommented = false;
        boolean blockCommentInRightOrder = true;
        final String blockCommentEnd = blockCommentDelims == null ? null : blockCommentDelims.getEnd();
        final String blockCommentStart = blockCommentDelims == null ? null : blockCommentDelims.getStart();

        for (int i = 0; (line != null) && (i < line.length()); i++) {
            // once we reach the cursor, set the
            // position of the selected index
            if (i == cursor) {
                wordIndex = words.size();
                // the position in the current argument is just the
                // length of the current argument
                wordCursor = current.length();
                rawWordCursor = i - rawWordStart;
            }

            if (quoteStart < 0 && isQuoteChar(line, i) && !lineCommented && !blockCommented) {
                // Start a quote block
                quoteStart = i;
                if (current.length() == 0) {
                    quotedWord = true;
                    if (context == ParseContext.SPLIT_LINE) {
                        current.append(line.charAt(i));
                    }
                } else {
                    current.append(line.charAt(i));
                }
            } else if (quoteStart >= 0 && line.charAt(quoteStart) == line.charAt(i) && notEscaped(line, i)) {
                // End quote block
                if (!quotedWord || context == ParseContext.SPLIT_LINE) {
                    current.append(line.charAt(i));
                } else if (rawWordCursor >= 0 && rawWordLength < 0) {
                    rawWordLength = i - rawWordStart + 1;
                }
                quoteStart = -1;
                quotedWord = false;
            } else if (quoteStart < 0 && isDelimiter(line, i)) {
                if (lineCommented) {
                    if (isCommentDelim(line, i, System.lineSeparator())) {
                        lineCommented = false;
                    }
                } else if (blockCommented) {
                    if (isCommentDelim(line, i, blockCommentEnd)) {
                        blockCommented = false;
                    }
                } else {
                    // Delimiter
                    rawWordLength = handleDelimiterAndGetRawWordLength(
                            current, words, rawWordStart, rawWordCursor, rawWordLength, i);
                    rawWordStart = i + 1;
                }
            } else {
                if (quoteStart < 0 && !blockCommented && (lineCommented || isLineCommentStarted(line, i))) {
                    lineCommented = true;
                } else if (quoteStart < 0
                        && !lineCommented
                        && (blockCommented || isCommentDelim(line, i, blockCommentStart))) {
                    if (blockCommented) {
                        if (blockCommentEnd != null && isCommentDelim(line, i, blockCommentEnd)) {
                            blockCommented = false;
                            i += blockCommentEnd.length() - 1;
                        }
                    } else {
                        blockCommented = true;
                        rawWordLength = handleDelimiterAndGetRawWordLength(
                                current, words, rawWordStart, rawWordCursor, rawWordLength, i);
                        i += blockCommentStart == null ? 0 : blockCommentStart.length() - 1;
                        rawWordStart = i + 1;
                    }
                } else if (quoteStart < 0 && !lineCommented && isCommentDelim(line, i, blockCommentEnd)) {
                    current.append(line.charAt(i));
                    blockCommentInRightOrder = false;
                } else if (!isEscapeChar(line, i)) {
                    current.append(line.charAt(i));
                    if (quoteStart < 0) {
                        bracketChecker.check(line, i);
                    }
                } else if (context == ParseContext.SPLIT_LINE) {
                    current.append(line.charAt(i));
                }
            }
        }

        if (current.length() > 0 || cursor == Objects.requireNonNull(line).length()) {
            words.add(current.toString());
            if (rawWordCursor >= 0 && rawWordLength < 0) {
                rawWordLength = line.length() - rawWordStart;
            }
        }

        if (cursor == Objects.requireNonNull(line).length()) {
            wordIndex = words.size() - 1;
            wordCursor = words.get(words.size() - 1).length();
            rawWordCursor = cursor - rawWordStart;
            rawWordLength = rawWordCursor;
        }

        if (context != ParseContext.COMPLETE && context != ParseContext.SPLIT_LINE) {
            if (eofOnEscapedNewLine && isEscapeChar(line, line.length() - 1)) {
                throw new EOFError(-1, -1, "Escaped new line", "newline");
            }
            if (eofOnUnclosedQuote && quoteStart >= 0) {
                throw new EOFError(
                        -1, -1, "Missing closing quote", line.charAt(quoteStart) == '\'' ? "quote" : "dquote");
            }
            if (blockCommented) {
                throw new EOFError(-1, -1, "Missing closing block comment delimiter", "add: " + blockCommentEnd);
            }
            if (!blockCommentInRightOrder) {
                throw new EOFError(-1, -1, "Missing opening block comment delimiter", "missing: " + blockCommentStart);
            }
            if (bracketChecker.isClosingBracketMissing() || bracketChecker.isOpeningBracketMissing()) {
                String message = null;
                String missing = null;
                if (bracketChecker.isClosingBracketMissing()) {
                    message = "Missing closing brackets";
                    missing = "add: " + bracketChecker.getMissingClosingBrackets();
                } else {
                    message = "Missing opening bracket";
                    missing = "missing: " + bracketChecker.getMissingOpeningBracket();
                }
                throw new EOFError(
                        -1,
                        -1,
                        message,
                        missing,
                        bracketChecker.getOpenBrackets(),
                        bracketChecker.getNextClosingBracket());
            }
        }

        String openingQuote = quotedWord ? line.substring(quoteStart, quoteStart + 1) : null;
        return new ArgumentList(this, line, words, wordIndex, wordCursor, cursor, openingQuote, rawWordCursor, rawWordLength);
    }

    /**
     * Returns true if the specified character is a whitespace parameter. Check to ensure that the character is not
     * escaped by any of {@link #getQuoteChars}, and is not escaped by any of the {@link #getEscapeChars}, and
     * returns true from {@link #isDelimiterChar}.
     *
     * @param buffer The complete command buffer
     * @param pos    The index of the character in the buffer
     * @return True if the character should be a delimiter
     */
    public boolean isDelimiter(final CharSequence buffer, final int pos) {
        return !isQuoted(buffer, pos) && notEscaped(buffer, pos) && isDelimiterChar(buffer, pos);
    }

    /**
     * handleDelimiterAndGetRawWordLength
     *
     * @param current       current
     * @param words         words
     * @param rawWordStart  rawWordStart
     * @param rawWordCursor rawWordCursor
     * @param rawWordLength rawWordLength
     * @param pos           pos
     * @return data
     */
    private int handleDelimiterAndGetRawWordLength(
            StringBuilder current,
            List<String> words,
            int rawWordStart,
            int rawWordCursor,
            int rawWordLength,
            int pos) {
        if (current.length() > 0) {
            words.add(current.toString());
            current.setLength(0); // reset the arg
            if (rawWordCursor >= 0 && rawWordLength < 0) {
                return pos - rawWordStart;
            }
        }
        return rawWordLength;
    }

    /**
     * isQuoted
     *
     * @param buffer buffer
     * @param pos    pos
     * @return data
     */
    public boolean isQuoted(final CharSequence buffer, final int pos) {
        return false;
    }

    /**
     * isQuoteChar
     *
     * @param buffer buffer
     * @param pos    pos
     * @return data
     */
    public boolean isQuoteChar(final CharSequence buffer, final int pos) {
        if (pos < 0) {
            return false;
        }
        if (quoteChars != null) {
            for (char e : quoteChars) {
                if (e == buffer.charAt(pos)) {
                    return notEscaped(buffer, pos);
                }
            }
        }
        return false;
    }

    /**
     * isCommentDelim
     *
     * @param buffer  buffer
     * @param pos     pos
     * @param pattern pattern
     * @return data
     */
    private boolean isCommentDelim(final CharSequence buffer, final int pos, final String pattern) {
        if (pos < 0) {
            return false;
        }

        if (pattern != null) {
            final int length = pattern.length();
            if (length <= buffer.length() - pos) {
                for (int i = 0; i < length; i++) {
                    if (pattern.charAt(i) != buffer.charAt(pos + i)) {
                        return false;
                    }
                }
                return true;
            }
        }
        return false;
    }

    /**
     * isLineCommentStarted
     *
     * @param buffer buffer
     * @param pos    pos
     * @return data
     */
    public boolean isLineCommentStarted(final CharSequence buffer, final int pos) {
        if (lineCommentDelims != null) {
            for (String comment : lineCommentDelims) {
                if (isCommentDelim(buffer, pos, comment)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * isEscapeChar
     *
     * @param ch ch
     * @return data
     */
    public boolean isEscapeChar(char ch) {
        if (escapeChars != null) {
            for (char e : escapeChars) {
                if (e == ch) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Check if this character is a valid escape char (i.e. one that has not been escaped)
     *
     * @param buffer the buffer to check in
     * @param pos    the position of the character to check
     * @return true if the character at the specified position in the given buffer is
     * an escape character and the character immediately preceding it is not an escape character.
     */
    public boolean isEscapeChar(final CharSequence buffer, final int pos) {
        if (pos < 0) {
            return false;
        }
        char ch = buffer.charAt(pos);
        return isEscapeChar(ch) && notEscaped(buffer, pos);
    }

    /**
     * Check if a character is escaped (i.e. if the previous character is an escape)
     *
     * @param buffer the buffer to check in
     * @param pos    the position of the character to check
     * @return true if the character at the specified position in the given buffer is
     * an escape character and the character immediately preceding it is an escape character.
     */
    public boolean isEscaped(final CharSequence buffer, final int pos) {
        if (pos <= 0) {
            return false;
        }
        return isEscapeChar(buffer, pos - 1);
    }

    /**
     * Check if a character is not escaped
     *
     * @param buffer buffer
     * @param pos    pos
     * @return data
     */
    public boolean notEscaped(final CharSequence buffer, final int pos) {
        return !isEscaped(buffer, pos);
    }

    /**
     * Returns true if the character at the specified position if a delimiter. This method will only be called if
     * the character is not enclosed in any of the {@link #getQuoteChars}, and is not escaped by any of the
     * {@link #getEscapeChars}. To perform escaping manually, override {@link #isDelimiter} instead.
     *
     * @param buffer the buffer to check in
     * @param pos    the position of the character to check
     * @return true if the character at the specified position in the given buffer is a delimiter.
     */
    public boolean isDelimiterChar(CharSequence buffer, int pos) {
        return Character.isWhitespace(buffer.charAt(pos));
    }

    /**
     * BracketChecker
     *
     * @author 单红宇
     * @since 2024-11-23 09:03:00
     */
    @Getter
    private class BracketChecker {
        /**
         * missingOpeningBracket
         */
        private int missingOpeningBracket = -1;
        /**
         * nested
         */
        private final List<Integer> nested = new ArrayList<>();
        /**
         * openBrackets
         */
        private int openBrackets = 0;
        /**
         * cursor
         */
        private final int cursor;
        /**
         * nextClosingBracket
         */
        private String nextClosingBracket;

        /**
         * BracketChecker
         *
         * @param cursor cursor
         */
        public BracketChecker(int cursor) {
            this.cursor = cursor;
        }

        /**
         * check
         *
         * @param buffer buffer
         * @param pos    pos
         */
        public void check(final CharSequence buffer, final int pos) {
            if (openingBrackets == null || pos < 0) {
                return;
            }
            int bid = bracketId(openingBrackets, buffer, pos);
            if (bid >= 0) {
                nested.add(bid);
            } else {
                bid = bracketId(closingBrackets, buffer, pos);
                if (bid >= 0) {
                    if (!nested.isEmpty() && bid == nested.get(nested.size() - 1)) {
                        nested.remove(nested.size() - 1);
                    } else {
                        missingOpeningBracket = bid;
                    }
                }
            }
            if (cursor > pos) {
                openBrackets = nested.size();
                if (!nested.isEmpty()) {
                    nextClosingBracket = String.valueOf(closingBrackets[nested.get(nested.size() - 1)]);
                }
            }
        }

        /**
         * isOpeningBracketMissing
         *
         * @return data
         */
        public boolean isOpeningBracketMissing() {
            return missingOpeningBracket != -1;
        }

        /**
         * getMissingOpeningBracket
         *
         * @return String
         */
        public String getMissingOpeningBracket() {
            if (!isOpeningBracketMissing()) {
                return null;
            }
            return Character.toString(openingBrackets[missingOpeningBracket]);
        }

        /**
         * isClosingBracketMissing
         *
         * @return data
         */
        public boolean isClosingBracketMissing() {
            return !nested.isEmpty();
        }

        /**
         * getMissingClosingBrackets
         *
         * @return String
         */
        public String getMissingClosingBrackets() {
            if (!isClosingBracketMissing()) {
                return null;
            }
            StringBuilder out = new StringBuilder();
            for (int i = nested.size() - 1; i > -1; i--) {
                out.append(closingBrackets[nested.get(i)]);
            }
            return out.toString();
        }

        /**
         * getNextClosingBracket
         *
         * @return String
         */
        public String getNextClosingBracket() {
            return nested.size() == 2 ? nextClosingBracket : null;
        }

        /**
         * bracketId
         *
         * @param brackets brackets
         * @param buffer   buffer
         * @param pos      pos
         * @return data
         */
        private int bracketId(final char[] brackets, final CharSequence buffer, final int pos) {
            for (int i = 0; i < brackets.length; i++) {
                if (buffer.charAt(pos) == brackets[i]) {
                    return i;
                }
            }
            return -1;
        }
    }

    /**
     * Bracket
     *
     * @author 单红宇
     * @since 2024-11-23 09:03:00
     */
    public enum Bracket {
        /**
         * ROUND
         */
        ROUND, // ()
        /**
         * CURLY
         */
        CURLY, // {}
        /**
         * SQUARE
         */
        SQUARE, // []
        /**
         * ANGLE
         */
        ANGLE // <>
    }

    /**
     * ParseContext
     *
     * @author 单红宇
     * @since 2024-11-23 09:03:00
     */
    public enum ParseContext {
        /**
         * UNSPECIFIED
         */
        UNSPECIFIED,

        /**
         * Try a real "final" parse.
         * May throw EOFError in which case we have incomplete input.
         */
        ACCEPT_LINE,

        /**
         * Parsed words will have all characters present in input line
         * including quotes and escape chars.
         * We should tolerate and ignore errors.
         */
        SPLIT_LINE,

        /**
         * Parse to find completions (typically after a Tab).
         * We should tolerate and ignore errors.
         */
        COMPLETE,

        /**
         * Called when we need to update the secondary prompts.
         * Specifically, when we need the 'missing' field from EOFError,
         * which is used by a "%M" in a prompt pattern.
         */
        SECONDARY_PROMPT
    }

    /**
     * BlockCommentDelims
     *
     * @author 单红宇
     * @since 2024-11-23 09:03:00
     */
    @Getter
    public static class BlockCommentDelims {
        /**
         * start
         */
        private final String start;
        /**
         * end
         */
        private final String end;

        /**
         * BlockCommentDelims
         *
         * @param start start
         * @param end   end
         */
        public BlockCommentDelims(String start, String end) {
            if (start == null || end == null || start.isEmpty() || end.isEmpty() || start.equals(end)) {
                throw new IllegalArgumentException("Bad block comment delimiter!");
            }
            this.start = start;
            this.end = end;
        }

    }

    /**
     * 参数集合
     *
     * @author 单红宇
     * @since 2024/11/23 9:37
     */
    public static class ArgumentList implements ParsedLine, CompletingParsedLine {
        /**
         * line
         */
        private final String line;

        /**
         * words
         */
        private final List<String> words;

        /**
         * wordIndex
         */
        private final int wordIndex;

        /**
         * wordCursor
         */
        private final int wordCursor;

        /**
         * cursor
         */
        private final int cursor;

        /**
         * openingQuote
         */
        private final String openingQuote;

        /**
         * rawWordCursor
         */
        private final int rawWordCursor;

        /**
         * rawWordLength
         */
        private final int rawWordLength;

        /**
         * parser
         */
        private final CommandLineParser parser;

        /**
         * ArgumentList
         *
         * @param parser        parser
         * @param line          the command line being edited
         * @param words         the list of words
         * @param wordIndex     the index of the current word in the list of words
         * @param wordCursor    the cursor position within the current word
         * @param cursor        the cursor position within the line
         * @param openingQuote  the opening quote (usually '\"' or '\'') or null
         * @param rawWordCursor the cursor position inside the raw word (i.e. including quotes and escape characters)
         * @param rawWordLength the raw word length, including quotes and escape characters
         */
        @SuppressWarnings("java:S107") // 忽略经过：参数过多
        public ArgumentList(final CommandLineParser parser,
                            final String line,
                            final List<String> words,
                            final int wordIndex,
                            final int wordCursor,
                            final int cursor,
                            final String openingQuote,
                            final int rawWordCursor,
                            final int rawWordLength) {
            this.parser = parser;
            this.line = line;
            this.words = Collections.unmodifiableList(Objects.requireNonNull(words));
            this.wordIndex = wordIndex;
            this.wordCursor = wordCursor;
            this.cursor = cursor;
            this.openingQuote = openingQuote;
            this.rawWordCursor = rawWordCursor;
            this.rawWordLength = rawWordLength;
        }

        /**
         * isRawEscapeChar
         *
         * @param key key
         * @return data
         */
        private boolean isRawEscapeChar(char key) {
            if (parser.getEscapeChars() != null) {
                for (char e : parser.getEscapeChars()) {
                    if (e == key) {
                        return true;
                    }
                }
            }
            return false;
        }

        /**
         * isRawQuoteChar
         *
         * @param key key
         * @return data
         */
        private boolean isRawQuoteChar(char key) {
            if (parser.getQuoteChars() != null) {
                for (char e : parser.getQuoteChars()) {
                    if (e == key) {
                        return true;
                    }
                }
            }
            return false;
        }

        /**
         * wordIndex
         *
         * @return int
         */
        public int wordIndex() {
            return this.wordIndex;
        }

        /**
         * word() should always be contained in words()
         *
         * @return String
         */
        public String word() {
            if ((wordIndex < 0) || (wordIndex >= words.size())) {
                return "";
            }
            return words.get(wordIndex);
        }

        /**
         * wordCursor
         *
         * @return int
         */
        public int wordCursor() {
            return this.wordCursor;
        }

        /**
         * words
         *
         * @return List
         */
        public List<String> words() {
            return this.words;
        }

        /**
         * cursor
         *
         * @return int
         */
        public int cursor() {
            return this.cursor;
        }

        /**
         * line
         *
         * @return String
         */
        public String line() {
            return line;
        }

        /**
         * escape
         *
         * @param candidate candidate
         * @param complete  complete
         * @return CharSequence
         */
        @SuppressWarnings("all")
        public CharSequence escape(CharSequence candidate, boolean complete) {
            StringBuilder sb = new StringBuilder(candidate);
            Predicate<Integer> needToBeEscaped;
            String quote = openingQuote;
            boolean middleQuotes = false;
            if (openingQuote == null) {
                for (int i = 0; i < sb.length(); i++) {
                    if (parser.isQuoteChar(sb, i)) {
                        middleQuotes = true;
                        break;
                    }
                }
            }
            if (parser.getEscapeChars() != null) {
                if (parser.getEscapeChars().length > 0) {
                    // Completion is protected by an opening quote:
                    // Delimiters (spaces) don't need to be escaped, nor do other quotes, but everything else does.
                    // Also, close the quote at the end
                    if (openingQuote != null) {
                        needToBeEscaped = i -> isRawEscapeChar(sb.charAt(i))
                                || String.valueOf(sb.charAt(i)).equals(openingQuote);
                    }
                    // Completion is protected by middle quotes:
                    // Delimiters (spaces) don't need to be escaped, nor do quotes, but everything else does.
                    else if (middleQuotes) {
                        needToBeEscaped = i -> isRawEscapeChar(sb.charAt(i));
                    }
                    // No quote protection, need to escape everything: delimiter chars (spaces), quote chars
                    // and escapes themselves
                    else {
                        needToBeEscaped = i ->
                                parser.isDelimiterChar(sb, i) || isRawEscapeChar(sb.charAt(i)) || isRawQuoteChar(sb.charAt(i));
                    }
                    for (int i = 0; i < sb.length(); i++) {
                        if (needToBeEscaped.test(i)) {
                            sb.insert(i++, parser.getEscapeChars()[0]);
                        }
                    }
                }
            } else if (openingQuote == null && !middleQuotes) {
                for (int i = 0; i < sb.length(); i++) {
                    if (parser.isDelimiterChar(sb, i)) {
                        quote = "'";
                        break;
                    }
                }
            }
            if (quote != null) {
                sb.insert(0, quote);
                if (complete) {
                    sb.append(quote);
                }
            }
            return sb;
        }

        @Override
        public int rawWordCursor() {
            return rawWordCursor;
        }

        @Override
        public int rawWordLength() {
            return rawWordLength;
        }
    }


    /**
     * <code>ParsedLine</code> objects are returned by the
     * during completion or when accepting the line.
     * <p>
     * The instances should implement the {@link CompletingParsedLine}
     * interface so that escape chars and quotes can be correctly handled.
     *
     * @author 单红宇
     * @see CompletingParsedLine
     * @since 2024/11/23 9:35
     */
    public interface ParsedLine {

        /**
         * The current word being completed.
         * If the cursor is after the last word, an empty string is returned.
         *
         * @return the word being completed or an empty string
         */
        String word();

        /**
         * The cursor position within the current word.
         *
         * @return the cursor position within the current word
         */
        int wordCursor();

        /**
         * The index of the current word in the list of words.
         *
         * @return the index of the current word in the list of words
         */
        int wordIndex();

        /**
         * The list of words.
         *
         * @return the list of words
         */
        List<String> words();

        /**
         * The unparsed line.
         *
         * @return the unparsed line
         */
        String line();

        /**
         * The cursor position within the line.
         *
         * @return the cursor position within the line
         */
        int cursor();
    }

    /**
     * An extension of {@link org.jline.reader.ParsedLine} that, being aware of the quoting and escaping rules
     * of the {@link org.jline.reader.Parser} that produced it, knows if and how a completion candidate
     * should be escaped/quoted.
     *
     * @author 单红宇
     * @since 2024/11/23 9:36
     */
    public interface CompletingParsedLine {

        /**
         * escape
         *
         * @param candidate candidate
         * @param complete  complete
         * @return CharSequence
         */
        CharSequence escape(CharSequence candidate, boolean complete);

        /**
         * rawWordCursor
         *
         * @return data
         */
        int rawWordCursor();

        /**
         * rawWordLength
         *
         * @return data
         */
        int rawWordLength();

    }

    /**
     * EOFError
     *
     * @author 单红宇
     * @since 2024-11-23 10:10:04
     */
    @ToString
    public static class EOFError extends RuntimeException {

        /**
         * line
         */
        private final int line;
        private final int column;
        /**
         * missing
         */
        private final String missing;

        /**
         * openBrackets
         */
        private final int openBrackets;
        /**
         * nextClosingBracket
         */
        private final String nextClosingBracket;

        /**
         * EOFError
         *
         * @param line    line
         * @param column  column
         * @param message message
         * @param missing missing
         */
        public EOFError(int line, int column, String message, String missing) {
            this(line, column, message, missing, -1, null);
        }

        /**
         * EOFError
         *
         * @param line               line
         * @param column             column
         * @param message            message
         * @param missing            missing
         * @param openBrackets       openBrackets
         * @param nextClosingBracket nextClosingBracket
         */
        public EOFError(int line, int column, String message, String missing, int openBrackets, String nextClosingBracket) {
            super(message);
            this.line = line;
            this.column = column;
            this.missing = missing;
            this.openBrackets = openBrackets;
            this.nextClosingBracket = nextClosingBracket;
        }

    }
}

