//
//  NPVideoCell.m
//  notPipe for iOS
//
//  Created by Claude on 30.10.2026.
//

#import "NPVideoCell.h"
#import "NPUtil.h"

static const CGFloat kSide = 12, kInfoHeight = 74;

@interface NPVideoCell ()
@property (nonatomic, strong) UIImageView *thumb;
@property (nonatomic, strong) UILabel *duration, *title, *meta;
@end

@implementation NPVideoCell

- (instancetype)initWithStyle:(UITableViewCellStyle)style reuseIdentifier:(NSString *)reuseIdentifier {
    self = [super initWithStyle:style reuseIdentifier:reuseIdentifier];
    if (self) {
        self.backgroundColor = NP_BG;
        UIView *selected = [UIView new];
        selected.backgroundColor = [UIColor colorWithWhite:0.18 alpha:1];
        self.selectedBackgroundView = selected;

        _thumb = [UIImageView new];
        _thumb.contentMode = UIViewContentModeScaleAspectFill;
        _thumb.clipsToBounds = YES;
        _thumb.backgroundColor = [UIColor colorWithWhite:0.15 alpha:1];
        [self.contentView addSubview:_thumb];

        _duration = [UILabel new];
        _duration.font = [UIFont boldSystemFontOfSize:12];
        _duration.textColor = [UIColor whiteColor];
        _duration.backgroundColor = [UIColor colorWithWhite:0 alpha:0.75];
        _duration.textAlignment = NSTextAlignmentCenter;
        [self.contentView addSubview:_duration];

        _title = [UILabel new];
        _title.font = [UIFont systemFontOfSize:15 weight:UIFontWeightMedium];
        _title.textColor = [UIColor colorWithWhite:0.95 alpha:1];
        _title.numberOfLines = 2;
        [self.contentView addSubview:_title];

        _meta = [UILabel new];
        _meta.font = [UIFont systemFontOfSize:12];
        _meta.textColor = NP_GREY;
        [self.contentView addSubview:_meta];
    }
    return self;
}

+ (CGFloat)heightForWidth:(CGFloat)width {
    return width * 9 / 16 + kInfoHeight;
}

- (void)layoutSubviews {
    [super layoutSubviews];
    CGFloat w = self.contentView.bounds.size.width, h = w * 9 / 16;
    self.thumb.frame = CGRectMake(0, 0, w, h);
    CGSize d = [self.duration sizeThatFits:CGSizeMake(100, 20)];
    self.duration.hidden = self.duration.text.length == 0;
    self.duration.frame = CGRectMake(w - d.width - 14, h - 24, d.width + 8, 18);
    CGSize t = [self.title sizeThatFits:CGSizeMake(w - kSide * 2, 40)];
    self.title.frame = CGRectMake(kSide, h + 10, w - kSide * 2, MIN(t.height, 38));
    self.meta.frame = CGRectMake(kSide, CGRectGetMaxY(self.title.frame) + 3, w - kSide * 2, 16);
}

- (void)showVideo:(NSDictionary *)video {
    NSString *videoId = video[@"videoId"];
    [NPUtil loadImage:[NPUtil thumbnailForVideo:videoId] into:self.thumb];
    self.title.text = video[@"title"];
    self.duration.text = [NPUtil duration:[video[@"lengthSeconds"] integerValue]];
    NSMutableArray *parts = [NSMutableArray new];
    if ([video[@"author"] length]) [parts addObject:video[@"author"]];
    NSString *views = [NPUtil views:[video[@"viewCount"] longLongValue]];
    if (!views.length && [video[@"viewCountText"] isKindOfClass:[NSString class]]) views = video[@"viewCountText"];
    if (views.length) [parts addObject:views];
    if ([video[@"publishedText"] isKindOfClass:[NSString class]] && [video[@"publishedText"] length]) {
        [parts addObject:video[@"publishedText"]];
    }
    self.meta.text = [parts componentsJoinedByString:@" · "];
    [self setNeedsLayout];
}

@end
