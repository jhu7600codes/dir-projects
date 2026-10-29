#import "HPScanner.h"
#import <AVFoundation/AVFoundation.h>
#import <CoreImage/CoreImage.h>

NSString *HPQRTextInImage(UIImage *image) {
    CIImage *ci = image.CIImage ?: [CIImage imageWithCGImage:image.CGImage];
    if (!ci) return nil;
    CIDetector *d = [CIDetector detectorOfType:CIDetectorTypeQRCode context:nil
                                       options:@{ CIDetectorAccuracy: CIDetectorAccuracyHigh }];
    NSMutableArray *texts = [NSMutableArray array];
    for (CIFeature *f in [d featuresInImage:ci]) {
        if ([f isKindOfClass:[CIQRCodeFeature class]] && ((CIQRCodeFeature *)f).messageString.length)
            [texts addObject:((CIQRCodeFeature *)f).messageString];
    }
    return texts.count ? [texts componentsJoinedByString:@"\n"] : nil;
}

@interface HPScanViewController () <AVCaptureMetadataOutputObjectsDelegate>
@property (nonatomic, copy) void (^completion)(NSString *);
@property (nonatomic, strong) AVCaptureSession *session;
@property (nonatomic, strong) AVCaptureVideoPreviewLayer *preview;
@property (nonatomic, strong) UILabel *hint;
@property (nonatomic, assign) BOOL done;
@end

@implementation HPScanViewController

- (instancetype)initWithCompletion:(void (^)(NSString *))completion {
    if ((self = [super initWithNibName:nil bundle:nil])) {
        _completion = [completion copy];
    }
    return self;
}

- (UIStatusBarStyle)preferredStatusBarStyle {
    return UIStatusBarStyleLightContent;
}

- (void)viewDidLoad {
    [super viewDidLoad];
    self.view.backgroundColor = [UIColor blackColor];

    self.hint = [[UILabel alloc] init];
    self.hint.textColor = [UIColor whiteColor];
    self.hint.font = [UIFont systemFontOfSize:15];
    self.hint.textAlignment = NSTextAlignmentCenter;
    self.hint.numberOfLines = 0;
    self.hint.text = @"Point the camera at a subscription, vless:// or Happ QR code.";

    UIButton *cancel = [UIButton buttonWithType:UIButtonTypeSystem];
    [cancel setTitle:@"Cancel" forState:UIControlStateNormal];
    cancel.titleLabel.font = [UIFont systemFontOfSize:17 weight:UIFontWeightSemibold];
    [cancel setTitleColor:[UIColor whiteColor] forState:UIControlStateNormal];
    [cancel addTarget:self action:@selector(cancel) forControlEvents:UIControlEventTouchUpInside];

    UIView *frame = [[UIView alloc] init];
    frame.layer.borderColor = [UIColor colorWithWhite:1 alpha:0.9].CGColor;
    frame.layer.borderWidth = 2;
    frame.layer.cornerRadius = 16;
    frame.userInteractionEnabled = NO;

    for (UIView *v in @[ frame, self.hint, cancel ]) {
        v.translatesAutoresizingMaskIntoConstraints = NO;
        [self.view addSubview:v];
    }
    [NSLayoutConstraint activateConstraints:@[
        [frame.centerXAnchor constraintEqualToAnchor:self.view.centerXAnchor],
        [frame.centerYAnchor constraintEqualToAnchor:self.view.centerYAnchor constant:-20],
        [frame.widthAnchor constraintEqualToConstant:230],
        [frame.heightAnchor constraintEqualToConstant:230],
        [self.hint.topAnchor constraintEqualToAnchor:frame.bottomAnchor constant:24],
        [self.hint.leadingAnchor constraintEqualToAnchor:self.view.leadingAnchor constant:32],
        [self.hint.trailingAnchor constraintEqualToAnchor:self.view.trailingAnchor constant:-32],
        [cancel.centerXAnchor constraintEqualToAnchor:self.view.centerXAnchor],
        [cancel.bottomAnchor constraintEqualToAnchor:self.bottomLayoutGuide.topAnchor constant:-24],
        [cancel.heightAnchor constraintEqualToConstant:44],
    ]];

    [AVCaptureDevice requestAccessForMediaType:AVMediaTypeVideo completionHandler:^(BOOL granted) {
        dispatch_async(dispatch_get_main_queue(), ^{
            if (granted) [self startCamera];
            else self.hint.text = @"Camera access is off. Turn it on in Settings > Privacy > Camera > Happe.";
        });
    }];
}

- (void)startCamera {
    AVCaptureDevice *cam = [AVCaptureDevice defaultDeviceWithMediaType:AVMediaTypeVideo];
    NSError *err = nil;
    AVCaptureDeviceInput *input = cam ? [AVCaptureDeviceInput deviceInputWithDevice:cam error:&err] : nil;
    if (!input) {
        self.hint.text = [NSString stringWithFormat:@"Camera unavailable: %@", err.localizedDescription ?: @"no camera"];
        return;
    }
    self.session = [[AVCaptureSession alloc] init];
    [self.session addInput:input];
    AVCaptureMetadataOutput *out = [[AVCaptureMetadataOutput alloc] init];
    [self.session addOutput:out];
    [out setMetadataObjectsDelegate:self queue:dispatch_get_main_queue()];
    if ([out.availableMetadataObjectTypes containsObject:AVMetadataObjectTypeQRCode])
        out.metadataObjectTypes = @[ AVMetadataObjectTypeQRCode ];

    self.preview = [AVCaptureVideoPreviewLayer layerWithSession:self.session];
    self.preview.videoGravity = AVLayerVideoGravityResizeAspectFill;
    self.preview.frame = self.view.bounds;
    [self.view.layer insertSublayer:self.preview atIndex:0];
    dispatch_async(dispatch_get_global_queue(QOS_CLASS_USER_INITIATED, 0), ^{
        [self.session startRunning];
    });
}

- (void)viewDidLayoutSubviews {
    [super viewDidLayoutSubviews];
    self.preview.frame = self.view.bounds;
}

- (void)finish:(NSString *)text {
    if (self.done) return;
    self.done = YES;
    AVCaptureSession *s = self.session;
    dispatch_async(dispatch_get_global_queue(QOS_CLASS_USER_INITIATED, 0), ^{
        [s stopRunning];
    });
    if (self.completion) self.completion(text);
}

- (void)cancel {
    [self finish:nil];
}

- (void)captureOutput:(AVCaptureOutput *)output didOutputMetadataObjects:(NSArray *)objects fromConnection:(AVCaptureConnection *)connection {
    for (AVMetadataObject *o in objects) {
        if (![o isKindOfClass:[AVMetadataMachineReadableCodeObject class]]) continue;
        NSString *text = ((AVMetadataMachineReadableCodeObject *)o).stringValue;
        if (text.length) {
            [self finish:text];
            return;
        }
    }
}

@end
