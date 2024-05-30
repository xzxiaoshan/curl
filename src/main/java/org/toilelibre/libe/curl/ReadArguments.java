package org.toilelibre.libe.curl;

import org.apache.commons.cli.*;

import java.util.*;
import java.util.regex.*;
import java.util.stream.*;

import static java.lang.Integer.*;
import static java.util.Optional.*;

/**
 * 参数读取
 */
public final class ReadArguments {
    private static final Pattern PLACEHOLDER_REGEX = Pattern.compile ("^\\$curl_placeholder_[0-9]+$");
    private static final Map<String, List<String>> CACHED_ARGS_MATCHES = new HashMap<> ();

    static CommandLine getCommandLineFromRequest (final String requestCommand, final List<String> placeholderValues) {
        return getCommandLineFromRequest (requestCommand, placeholderValues, CACHED_ARGS_MATCHES);
    }

    static CommandLine getCommandLineFromRequest (final String requestCommand, final List<String> placeholderValues,
                                                  final Map<String, List<String>> argMatches) {

        // configure a parser
        final DefaultParser parser = new DefaultParser ();

        final String requestCommandWithoutBasename = requestCommand.replaceAll ("^[ ]*curl[ ]*", " ") + " ";
        final String[] args = ReadArguments.getArgsFromCommand (requestCommandWithoutBasename, placeholderValues, argMatches);
        final CommandLine commandLine;
        try {
            commandLine = parser.parse (Arguments.ALL_OPTIONS, args);
        } catch (final ParseException e) {
            new HelpFormatter ().printHelp ("curl [options] url", Arguments.ALL_OPTIONS);
            throw new CurlException (e);
        }
        return commandLine;
    }

    /**
     * 正则匹配返回匹配的字符串集合
     *
     * @param regex 正则对象
     * @param input 输入值
     * @return 匹配的正则字符串集合
     */
    private static List<String> asMatches (Pattern regex, String input) {
        try{
            Matcher matcher = regex.matcher (input);
            List<String> result = new ArrayList<> ();
            while (matcher.find ()){
                result.add (matcher.group (1));
            }
            return result;
        }catch(StackOverflowError ex){
            return asMatches(input);
        }
    }

    /**
     * 通过特定规则拆解返回字符串集合
     *
     * @param input 输入值
     * @return 拆解后的字符串集合
     */
    private static List<String> asMatches(String input){
        List<String> list = new ArrayList<>();
        StringBuilder singleArg = new StringBuilder();
        boolean inQuotation  = false;
        char[] characters = input.toCharArray();
        String start = "";
        for(int i = 0; i<characters.length; i++) {
            char c = characters[i];
            // 不在引号内且当前的是空格
            if(!inQuotation  && Character.isWhitespace(c)){
                if (singleArg.length() > 0) {
                    list.add(singleArg.toString());
                    singleArg.setLength(0);
                }
                // 当前是 ' 下一个字符是 { ，是开头且不在引号内（以 ' 为开头的参数）
            }else if(c == '\'' && !inQuotation  && characters[i+1] == '{'){
                start = "'{";
                inQuotation  = true;
                singleArg.append(c);
                // 当前是 ' 下一个字符不是 { ，是开头且不在引号内（以 '{ 为开头的参数）
            }else if(c == '\'' && !inQuotation  && characters[i+1] != '{'){
                start = "'";
                inQuotation  = true;
                singleArg.append(c);
                // 当前是 \ 下一个字符是 " ，是开头且不在引号内（以 " 为开头的参数）
            }else if(c == '\\' && characters[i+1] == '\"' && !inQuotation ){
                start = "\\";
                inQuotation  = true;
                singleArg.append(c + '\"');
                i++;
                // 起始是 ' ，当前是 ' ，前一个字符不是 } ，在引号内，且是最终字符或下一个是空格（以 ' 为开头的参数，以 ' 结尾）
            }else if(start == "'" && c == '\'' && inQuotation  && characters[i-1] != '}' && (i == characters.length-1 || Character.isWhitespace(characters[i+1]))){
                inQuotation  = false;
                singleArg.append(c);
                // 起始是 '{ ，当前是 ' ，前一个字符是 } ，在引号内，且是最终字符或下一个是空格（以 '{ 为开头的参数，以 }' 结尾）
            }else if(start == "'{" && c == '\'' && inQuotation  && characters[i-1] == '}' && (i == characters.length-1 || Character.isWhitespace(characters[i+1]))){
                inQuotation  = false;
                singleArg.append(c);
                // 起始是 \ ，当前是 \ ，后一个字符是 " ，在引号内，且下一个是空格（以 " 为开头的参数，以 " 结尾）
            }else if(start == "\\" && c == '\\' && characters[i+1] == '\"' && inQuotation  && Character.isWhitespace(characters[i+1])){
                inQuotation  = false;
                singleArg.append(c + '\"');
                i++;
            }else {
                singleArg.append(c);
            }
        }
        if (singleArg.length() > 0) {
            list.add(singleArg.toString());
        }
        return list;
    }


    private static String[] getArgsFromCommand (final String requestCommandWithoutBasename,
                                                final List<String> placeholderValues,
                                                final Map<String, List<String>> argMatches) {
        final String requestCommandInput = requestCommandWithoutBasename.replaceAll ("\\s+-([a-zA-Z0-9])\\s+", " -$1 ");
        final List<String> matches;
        if (argMatches.containsKey (requestCommandInput)) {
            matches = argMatches.get (requestCommandInput);
        }else{
            matches = asMatches (Arguments.ARGS_SPLIT_REGEX, requestCommandInput);
            argMatches.put (requestCommandInput, matches);
        }

        return ofNullable (matches).map (List :: stream).orElse (Stream.empty ()).map (match -> {
            String argument = ReadArguments.removeSlashes (match.trim ());
            if (PLACEHOLDER_REGEX.matcher (argument).matches ())
                return placeholderValues.get (parseInt (argument.substring ("$curl_placeholder_".length ())));
            else return argument;

        }).toArray (String[] ::new);
    }

    private static String removeSlashes (final String arg) {
        if (arg.length () == 0) {
            return arg;
        }
        if (arg.charAt (0) == '\"') {
            return arg.substring (1, arg.length () - 1).replaceAll ("\\\"", "\"");
        }
        if (arg.charAt (0) == '\'') {
            return arg.substring (1, arg.length () - 1).replaceAll ("\\\'", "\'");
        }
        return arg;
    }
}
