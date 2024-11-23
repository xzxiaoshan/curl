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
     * @param line 输入值
     * @return 匹配的正则字符串集合
     */
    private static List<String> asMatches (String line) {
        return CommandLineParser.getInstance().parse(line).words();
    }

    private static String[] getArgsFromCommand (final String requestCommandWithoutBasename,
                                                final List<String> placeholderValues,
                                                final Map<String, List<String>> argMatches) {
        final String requestCommandInput = requestCommandWithoutBasename.replaceAll ("\\s+-([a-zA-Z0-9])\\s+", " -$1 ");
        final List<String> matches;
        if (argMatches.containsKey (requestCommandInput)) {
            matches = argMatches.get (requestCommandInput);
        }else{
            matches = asMatches (requestCommandInput);
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
