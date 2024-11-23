package org.toilelibre.libe.curl;

import org.apache.commons.cli.*;

import java.util.regex.*;

/**
 * Curl参数
 */
public final class Arguments {

    public final static Options ALL_OPTIONS      = new Options ();

    public final static Option USER = Arguments.add (Option.builder ("u").longOpt ("user").desc ("credentials").required (false).hasArg (true).desc ("user:password").build ());

    public final static Option  CA_CERT          = Arguments.add (Option.builder ("cacert").longOpt ("cacert").desc ("CA certificate").required (false).hasArg (true).desc ("CA_CERT").build ());

    public final static Option  CERT             = Arguments.add (Option.builder ("E").longOpt ("cert").desc ("client certificate").required (false).hasArg (true).desc ("CERT[:password]").build ());

    public final static Option  CERT_TYPE        = Arguments.add (Option.builder ("ct").longOpt ("cert-type").desc ("certificate type").required (false).hasArg (true).desc ("PEM|P12|JKS|DER|ENG").build ());

    public final static Option  COMPRESSED       = Arguments.add (Option.builder ("compressed").longOpt ("compressed").desc ("Request compressed response").required (false).hasArg (false).build ());

    public final static Option  CONNECT_TIMEOUT  = Arguments.add (Option.builder ("cti").longOpt ("connect-timeout").desc ("Maximum time allowed for connection").required (false).hasArg (true).argName ("seconds").build ());

    public final static Option  DATA             = Arguments.add (Option.builder ("d").longOpt ("data").desc ("Data").required (false).hasArg ().argName ("payload").build ());

    public final static Option  DATA_RAW         = Arguments.add (Option.builder ("dataraw").longOpt ("data-raw").desc ("Data").required (false).hasArg ().argName ("payload").build ());

    public final static Option  DATA_BINARY      = Arguments.add (Option.builder ("databinary").longOpt ("data-binary").desc ("http post binary data").required (false).hasArg ().argName ("payload").build ());

    public final static Option  DATA_URLENCODED   = Arguments.add (Option.builder ("dataurlencode").longOpt ("data-urlencode").desc ("Data to URLEncode").required (false).hasArg ().argName ("payload").build ());

    public final static Option  FOLLOW_REDIRECTS = Arguments.add (Option.builder ("L").longOpt ("location").desc ("follow redirects").required (false).hasArg (false).build ());

    public final static Option  FORM             = Arguments.add (Option.builder ("F").longOpt ("form").desc ("http multipart post data").required (false).hasArg (true).build ());

    public final static Option  HEADER           = Arguments.add (Option.builder ("H").longOpt ("header").desc ("Header").required (false).hasArg ().argName ("headerValue").build ());

    public final static Option  HTTP_METHOD      = Arguments.add (Option.builder ("X").longOpt ("request").desc ("Http Method").required (false).hasArg ().argName ("method").build ());

    public final static Option  KEY              = Arguments.add (Option.builder ("key").longOpt ("key").desc ("key").required (false).hasArg (true).desc ("KEY").build ());

    public final static Option  KEY_TYPE         = Arguments.add (Option.builder ("kt").longOpt ("key-type").desc ("key type").required (false).hasArg (true).desc ("PEM|P12|JKS|DER|ENG").build ());

    public final static Option  MAX_TIME         = Arguments.add (Option.builder ("m").longOpt ("max-time").desc ("Maximum time allowed for the transfer").required (false).hasArg (true).argName ("seconds").build ());

    public final static Option  NO_KEEPALIVE     = Arguments.add (Option.builder ("nokeepalive").longOpt ("no-keepalive").desc ("Disable TCP keepalive on the connection").required (false).hasArg (false).build ());

    public final static Option  NTLM             = Arguments.add (Option.builder ("ntlm").longOpt ("ntlm").desc ("NTLM auth").required (false).hasArg (false).build ());

    public final static Option  OUTPUT           = Arguments.add (Option.builder ("o").longOpt ("output").desc ("write to file").required (false).hasArg (true).argName ("FILE").build ());

    public final static Option  PROXY            = Arguments.add (Option.builder ("x").longOpt ("proxy").desc ("use the specified HTTP proxy").required (false).hasArg (true).argName ("<[protocol://][user:password@]proxyhost[:port]>").build ());

    public final static Option  PROXY_USER       = Arguments.add (Option.builder ("U").longOpt ("proxy-user").desc ("authentication for proxy").required (false).hasArg (true).argName ("user[:password]").build ());

    public final static Option  TLS_V1           = Arguments.add (Option.builder ("1").longOpt ("tlsv1").desc ("use >= TLSv1 (SSL)").required (false).hasArg (false).build ());

    public final static Option  TLS_V10          = Arguments.add (Option.builder ("tlsv10").longOpt ("tlsv1.0").desc ("use TLSv1.0 (SSL)").required (false).hasArg (false).build ());

    public final static Option  TLS_V11          = Arguments.add (Option.builder ("tlsv11").longOpt ("tlsv1.1").desc ("use TLSv1.1 (SSL)").required (false).hasArg (false).build ());

    public final static Option  TLS_V12          = Arguments.add (Option.builder ("tlsv12").longOpt ("tlsv1.2").desc ("use TLSv1.2 (SSL)").required (false).hasArg (false).build ());

    public final static Option  SSL_V2           = Arguments.add (Option.builder ("2").longOpt ("sslv2").desc ("use SSLv2 (SSL)").required (false).hasArg (false).build ());

    public final static Option  SSL_V3           = Arguments.add (Option.builder ("3").longOpt ("sslv3").desc ("use SSLv3 (SSL)").required (false).hasArg (false).build ());

    public final static Option  TRUST_INSECURE   = Arguments.add (Option.builder ("k").longOpt ("insecure").desc ("trust insecure").required (false).hasArg (false).build ());

    public final static Option  USER_AGENT       = Arguments.add (Option.builder ("A").longOpt ("user-agent").desc ("user agent").required (false).hasArg (true).build ());

    public final static Option  VERSION          = Arguments.add (Option.builder ("V").longOpt ("version").desc ("get the version of this library").required (false).hasArg (false).build ());

    public final static Option  INTERCEPTOR      = Arguments.add (Option.builder ("interceptor").longOpt ("interceptor").desc ("interceptor field or method (syntax is classname::fieldname). Must be a BiFunction<HttpRequest, Supplier< HttpResponse>, HttpResponse> or will be discarded").required (false).hasArg (true).build ());

    private static Option add (final Option option) {
        Arguments.ALL_OPTIONS.addOption (option);
        return option;
    }
}
