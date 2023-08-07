package org.toilelibre.libe.curl;

import lombok.extern.slf4j.Slf4j;

import static org.toilelibre.libe.curl.Version.BUILD_TIME;
import static org.toilelibre.libe.curl.Version.NUMBER;

@Slf4j
final class VersionDisplay {

    static void stopAndDisplayVersionIfThe(boolean isTrue) {
        if (!isTrue) {
            return;
        }

        log.info(Curl.class.getPackage().getName() + " version " + NUMBER + ", build-time : " + BUILD_TIME);

        throw new CurlException(
                new IllegalArgumentException(
                        "You asked me to display the version. Probably not a production-ready code"));
    }

}
