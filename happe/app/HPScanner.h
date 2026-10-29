// qr codes: live camera scanner and decoding a picked photo/screenshot.

#import <UIKit/UIKit.h>

@interface HPScanViewController : UIViewController
// called once on the main queue with the qr text, or nil if cancelled.
// the scanner does not dismiss itself.
- (instancetype)initWithCompletion:(void (^)(NSString *text))completion;
@end

// text of every qr code in the image, joined by newlines; nil if none.
NSString *HPQRTextInImage(UIImage *image);
