#!/usr/bin/env bash
# Builds every module. The wagon transport + relaxed SSL flags are needed on networks
# where a corporate proxy intercepts TLS to Maven Central (PKIX errors otherwise).
set -e
cd "$(dirname "$0")"
mvn -Dmaven.resolver.transport=wagon \
    -Dmaven.wagon.http.ssl.insecure=true \
    -Dmaven.wagon.http.ssl.allowall=true \
    -Dmaven.wagon.http.ssl.ignore.validity.dates=true \
    clean install "$@"
