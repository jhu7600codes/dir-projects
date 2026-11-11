#!/bin/sh
# Host-side unit tests for the pure, network-independent logic modules
# (sabr.c's wire-format/clock parsing, scrape.c's HTML extraction,
# fetch.c's loop-termination decision). Run with plain host gcc -- no
# VitaSDK needed for this part. See the README's "What's verified".
set -e
cd "$(dirname "$0")/.."

gcc -Wall -Wextra -std=c11 -o /tmp/maytube_vita_sabr_test src/sabr.c test/http_stub.c test/sabr_test.c
/tmp/maytube_vita_sabr_test

gcc -Wall -Wextra -std=c11 -o /tmp/maytube_vita_scrape_test src/scrape.c test/scrape_test.c
/tmp/maytube_vita_scrape_test

gcc -Wall -Wextra -std=c11 -o /tmp/maytube_vita_fetch_test src/fetch.c src/sabr.c test/http_stub.c test/fetch_test.c
/tmp/maytube_vita_fetch_test

echo "all maytube-vita host tests passed"
