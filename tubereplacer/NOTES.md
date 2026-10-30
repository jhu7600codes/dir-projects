# random notes

- ML stands for medialib
- the l(@"") function handles proper underscore placement between different versions.
- to find the callstack of a function w/o using a debugger, you can use
```objc
#import <execinfo.h>
#import <mach-o/dyld.h>

intptr_t slide = _dyld_get_image_vmaddr_slide(0);
NSLog(@"ASLR Slide Offset: 0x%lx\n", (unsigned long)slide);
void *callstack[128];
int frames = backtrace(callstack, 128);
char **symbols = backtrace_symbols(callstack, frames);
NSMutableString *callstackString = [NSMutableString stringWithFormat:@"uwu >_<"];
for (int i = 0; i < frames; i++) {
  [callstackString appendFormat:@"%s\n", symbols[i]];
}
NSLog(@"%@", callstackString);
```
## fixes in this copy (dir-projects)

- comments: YTComment is built with whichever initializer the app version actually has (checked at runtime),
  nil author/text/date are replaced, one unparseable item no longer drops the page, continuation tokens are
  read from both places YouTube puts them, and comments in the newer commentViewModel format are read too
- SABR quality: bandwidth is reported to YouTube in bits/s (was bytes/s, 8x too low) with a sane estimate
  before the first measurement, and the stream asks for the best allowed format up to 720p instead of
  letting the server start at 144p
- requests no longer throw when the device locale has no region (gl falls back to US)
