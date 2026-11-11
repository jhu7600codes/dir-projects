/* Host test for fetch_should_continue -- the loop-termination logic
   pulled out of fetch_video specifically so it's checkable without a
   network. Mirrors StreamingPlayer.kt's fetchAllFragments reachedEnd
   computation. */

#include "../src/fetch.h"
#include <stdio.h>

static int failures = 0;
#define CHECK(cond, msg) do { \
    if (!(cond)) { fprintf(stderr, "FAIL: %s (%s:%d)\n", msg, __FILE__, __LINE__); failures++; } \
} while (0)

int main(void) {
    /* known duration: stop once next_offset reaches it */
    CHECK(fetch_should_continue(10000, 30000, 1) == 1, "known duration: keep going before the end");
    CHECK(fetch_should_continue(30000, 30000, 1) == 0, "known duration: stop exactly at the end");
    CHECK(fetch_should_continue(35000, 30000, 1) == 0, "known duration: stop past the end");
    /* even with data still arriving, a known duration caps it -- data
       alone never overrides a known total */
    CHECK(fetch_should_continue(30000, 30000, 1) == 0, "known duration wins over had_data=1");

    /* unknown duration (-1): stop only once a step returns no data */
    CHECK(fetch_should_continue(10000, -1, 1) == 1, "unknown duration: keep going while data arrives");
    CHECK(fetch_should_continue(10000, -1, 0) == 0, "unknown duration: stop once a step is empty");

    /* hard cap always wins regardless of duration/data */
    long hard_cap = 6L * 60 * 60 * 1000;
    CHECK(fetch_should_continue(hard_cap, -1, 1) == 0, "hard cap wins even with data still arriving");
    CHECK(fetch_should_continue(hard_cap, 999999999, 1) == 0, "hard cap wins even under a huge known duration");

    if (failures == 0) {
        printf("all fetch tests passed\n");
        return 0;
    }
    fprintf(stderr, "%d fetch test(s) failed\n", failures);
    return 1;
}
