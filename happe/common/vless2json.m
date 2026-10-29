// vless2json: stage 2 test tool. reads a vless:// link from argv[1] or stdin
// and prints the xray config the app would write.
//   read -r L; printf '%s' "$L" | vless2json > /tmp/config.json

#import <Foundation/Foundation.h>
#import "VLESSConfig.h"

int main(int argc, char **argv) {
    @autoreleasepool {
        NSString *link = nil;
        if (argc > 1) {
            link = [NSString stringWithUTF8String:argv[1]];
        } else {
            NSData *in = [[NSFileHandle fileHandleWithStandardInput] readDataToEndOfFile];
            link = [[NSString alloc] initWithData:in encoding:NSUTF8StringEncoding];
        }
        NSString *err = nil;
        NSData *json = [VLESSConfig configJSONFromLink:link error:&err];
        if (!json) {
            fprintf(stderr, "vless2json: %s\n", err.UTF8String);
            return 1;
        }
        fwrite(json.bytes, 1, json.length, stdout);
        fputc('\n', stdout);
        fprintf(stderr, "ok: %s\n", [VLESSConfig summaryForLink:link].UTF8String);
        return 0;
    }
}
