//
//  NPPlayerViewController.m
//  notPipe for iOS
//
//  Created by Claude on 30.10.2026.
//

#import "NPPlayerViewController.h"
#import <AVFoundation/AVFoundation.h>
#import <AVKit/AVKit.h>
#import "NPNet.h"
#import "NPUtil.h"
#import "NPVideoCell.h"

#pragma mark - Comment cell

@interface NPCommentCell : UITableViewCell
@property (nonatomic, strong) UIImageView *avatar;
@property (nonatomic, strong) UILabel *header, *body;
+ (CGFloat)heightFor:(NSDictionary *)comment width:(CGFloat)width;
@end

@implementation NPCommentCell

- (instancetype)initWithStyle:(UITableViewCellStyle)style reuseIdentifier:(NSString *)reuseIdentifier {
    self = [super initWithStyle:style reuseIdentifier:reuseIdentifier];
    if (self) {
        self.backgroundColor = NP_BG;
        self.selectionStyle = UITableViewCellSelectionStyleNone;
        _avatar = [UIImageView new];
        _avatar.backgroundColor = [UIColor colorWithWhite:0.2 alpha:1];
        _avatar.contentMode = UIViewContentModeScaleAspectFill;
        _avatar.clipsToBounds = YES;
        [self.contentView addSubview:_avatar];
        _header = [UILabel new];
        _header.font = [UIFont boldSystemFontOfSize:12];
        _header.textColor = NP_GREY;
        [self.contentView addSubview:_header];
        _body = [UILabel new];
        _body.font = [UIFont systemFontOfSize:14];
        _body.textColor = [UIColor colorWithWhite:0.92 alpha:1];
        _body.numberOfLines = 0;
        [self.contentView addSubview:_body];
    }
    return self;
}

+ (CGFloat)heightFor:(NSDictionary *)comment width:(CGFloat)width {
    NSString *text = [comment[@"content"] isKindOfClass:[NSString class]] ? comment[@"content"] : @"";
    CGRect r = [text boundingRectWithSize:CGSizeMake(width - 68, CGFLOAT_MAX) options:NSStringDrawingUsesLineFragmentOrigin
                               attributes:@{NSFontAttributeName: [UIFont systemFontOfSize:14]} context:nil];
    return MAX(60, ceil(r.size.height) + 40);
}

- (void)layoutSubviews {
    [super layoutSubviews];
    CGFloat w = self.contentView.bounds.size.width;
    self.avatar.frame = CGRectMake(12, 12, 40, 40);
    self.header.frame = CGRectMake(62, 10, w - 74, 16);
    CGSize s = [self.body sizeThatFits:CGSizeMake(w - 68, CGFLOAT_MAX)];
    self.body.frame = CGRectMake(62, 28, w - 68, s.height);
}

@end

#pragma mark - Player

@interface NPPlayerViewController () <UITableViewDataSource, UITableViewDelegate>
@property (nonatomic, strong) NSDictionary *video;
@property (nonatomic, strong) NSDictionary *details;
@property (nonatomic, copy) NSString *base;
@property (nonatomic, strong) AVPlayerViewController *playerController;
@property (nonatomic, strong) UIView *playerBox;
@property (nonatomic, strong) UILabel *status;
@property (nonatomic, strong) UIActivityIndicatorView *spinner;
@property (nonatomic, strong) UITableView *table;
@property (nonatomic, strong) UIView *header;
@property (nonatomic, strong) UILabel *titleLabel, *metaLabel, *channelLabel;
@property (nonatomic, strong) UIImageView *channelAvatar;
@property (nonatomic, strong) UISegmentedControl *tabs;
@property (nonatomic, strong) NSArray *related, *comments;
@property (nonatomic) BOOL commentsLoading;
@end

@implementation NPPlayerViewController

- (instancetype)initWithVideo:(NSDictionary *)video {
    self = [super init];
    if (self) {
        _video = video;
        _related = [NSArray array];
        _comments = [NSArray array];
        self.hidesBottomBarWhenPushed = YES;
    }
    return self;
}

- (void)viewDidLoad {
    [super viewDidLoad];
    self.view.backgroundColor = NP_BG;
    self.edgesForExtendedLayout = UIRectEdgeNone;
    self.title = @"";
    self.navigationItem.rightBarButtonItem = [[UIBarButtonItem alloc]
            initWithTitle:[[NPUtil quality] stringByAppendingString:@"p"] style:UIBarButtonItemStylePlain
                   target:self action:@selector(chooseQuality)];

    self.playerBox = [UIView new];
    self.playerBox.backgroundColor = [UIColor blackColor];
    [self.view addSubview:self.playerBox];

    self.playerController = [AVPlayerViewController new];
    [self addChildViewController:self.playerController];
    [self.playerBox addSubview:self.playerController.view];
    [self.playerController didMoveToParentViewController:self];

    self.spinner = [[UIActivityIndicatorView alloc] initWithActivityIndicatorStyle:UIActivityIndicatorViewStyleWhiteLarge];
    self.spinner.hidesWhenStopped = YES;
    [self.playerBox addSubview:self.spinner];
    self.status = [UILabel new];
    self.status.textColor = [UIColor whiteColor];
    self.status.font = [UIFont systemFontOfSize:13];
    self.status.textAlignment = NSTextAlignmentCenter;
    self.status.numberOfLines = 2;
    [self.playerBox addSubview:self.status];

    self.table = [[UITableView alloc] initWithFrame:CGRectZero style:UITableViewStylePlain];
    self.table.backgroundColor = NP_BG;
    self.table.separatorStyle = UITableViewCellSeparatorStyleNone;
    self.table.dataSource = self;
    self.table.delegate = self;
    [self.table registerClass:[NPVideoCell class] forCellReuseIdentifier:@"video"];
    [self.table registerClass:[NPCommentCell class] forCellReuseIdentifier:@"comment"];
    [self.view addSubview:self.table];
    [self buildHeader];

    [self showVideoInfo:self.video];
    [self loadDetails];
}

- (void)viewDidLayoutSubviews {
    [super viewDidLayoutSubviews];
    CGFloat w = self.view.bounds.size.width, h = round(w * 9 / 16);
    self.playerBox.frame = CGRectMake(0, 0, w, h);
    self.playerController.view.frame = self.playerBox.bounds;
    self.spinner.center = CGPointMake(w / 2, h / 2 - 12);
    self.status.frame = CGRectMake(16, h / 2 + 16, w - 32, 36);
    self.table.frame = CGRectMake(0, h, w, self.view.bounds.size.height - h);
    [self layoutHeader];
}

- (void)viewWillDisappear:(BOOL)animated {
    [super viewWillDisappear:animated];
    if (self.isMovingFromParentViewController || self.navigationController.topViewController != self) {
        [self.playerController.player pause];
    }
}

#pragma mark - Header

- (void)buildHeader {
    self.header = [UIView new];
    self.titleLabel = [UILabel new];
    self.titleLabel.font = [UIFont boldSystemFontOfSize:17];
    self.titleLabel.textColor = [UIColor whiteColor];
    self.titleLabel.numberOfLines = 0;
    [self.header addSubview:self.titleLabel];
    self.metaLabel = [UILabel new];
    self.metaLabel.font = [UIFont systemFontOfSize:13];
    self.metaLabel.textColor = NP_GREY;
    [self.header addSubview:self.metaLabel];
    self.channelAvatar = [UIImageView new];
    self.channelAvatar.backgroundColor = [UIColor colorWithWhite:0.2 alpha:1];
    self.channelAvatar.contentMode = UIViewContentModeScaleAspectFill;
    self.channelAvatar.clipsToBounds = YES;
    [self.header addSubview:self.channelAvatar];
    self.channelLabel = [UILabel new];
    self.channelLabel.font = [UIFont systemFontOfSize:15 weight:UIFontWeightMedium];
    self.channelLabel.textColor = [UIColor whiteColor];
    [self.header addSubview:self.channelLabel];
    self.tabs = [[UISegmentedControl alloc] initWithItems:@[@"Related", @"Comments"]];
    self.tabs.selectedSegmentIndex = 0;
    self.tabs.tintColor = NP_RED;
    [self.tabs addTarget:self action:@selector(tabChanged) forControlEvents:UIControlEventValueChanged];
    [self.header addSubview:self.tabs];
}

- (void)layoutHeader {
    CGFloat w = self.view.bounds.size.width;
    CGSize t = [self.titleLabel sizeThatFits:CGSizeMake(w - 24, CGFLOAT_MAX)];
    self.titleLabel.frame = CGRectMake(12, 12, w - 24, t.height);
    self.metaLabel.frame = CGRectMake(12, CGRectGetMaxY(self.titleLabel.frame) + 4, w - 24, 18);
    CGFloat y = CGRectGetMaxY(self.metaLabel.frame) + 12;
    self.channelAvatar.frame = CGRectMake(12, y, 40, 40);
    self.channelLabel.frame = CGRectMake(62, y + 10, w - 74, 20);
    self.tabs.frame = CGRectMake(12, y + 54, w - 24, 30);
    CGRect frame = CGRectMake(0, 0, w, y + 96);
    if (!CGRectEqualToRect(self.header.frame, frame)) {
        self.header.frame = frame;
        self.table.tableHeaderView = self.header;
    }
}

- (void)showVideoInfo:(NSDictionary *)info {
    self.titleLabel.text = info[@"title"];
    NSMutableArray *parts = [NSMutableArray new];
    NSString *views = [NPUtil views:[info[@"viewCount"] longLongValue]];
    if (views.length) [parts addObject:views];
    if ([info[@"publishedText"] isKindOfClass:[NSString class]] && [info[@"publishedText"] length]) [parts addObject:info[@"publishedText"]];
    self.metaLabel.text = [parts componentsJoinedByString:@" · "];
    self.channelLabel.text = info[@"author"];
    [self.view setNeedsLayout];
}

#pragma mark - Loading

- (void)loadDetails {
    [self.spinner startAnimating];
    self.status.text = @"";
    NSString *path = [NSString stringWithFormat:@"/api/v1/videos/%@?local=true", self.video[@"videoId"]];
    [NPNet invidious:path completion:^(id json, NSString *base, NSError *error) {
        if (![json isKindOfClass:[NSDictionary class]]) {
            [self.spinner stopAnimating];
            self.status.text = @"Couldn't load this video";
            return;
        }
        self.details = json;
        self.base = base;
        [NPUtil addToHistory:json];
        [self showVideoInfo:json];
        [NPUtil loadImage:[NPUtil avatarFrom:json[@"authorThumbnails"] base:base] into:self.channelAvatar];
        NSArray *related = json[@"recommendedVideos"];
        self.related = [related isKindOfClass:[NSArray class]] ? related : [NSArray array];
        if (self.tabs.selectedSegmentIndex == 0) [self.table reloadData];
        [self startPlayback];
    }];
}

- (NSString *)standardStreamURL {
    NSArray *streams = self.details[@"formatStreams"];
    if (![streams isKindOfClass:[NSArray class]] || !streams.count) return nil;
    NSDictionary *best = streams.firstObject;
    for (NSDictionary *s in streams) {
        if ([[s[@"itag"] description] isEqualToString:@"18"]) best = s;
    }
    return [NPUtil absolute:best[@"url"] base:self.base];
}

- (void)startPlayback {
    NSString *quality = [NPUtil quality];
    if ([quality isEqualToString:@"360"]) {
        [self playURL:[self standardStreamURL] message:nil];
        return;
    }
    [self.spinner startAnimating];
    self.status.text = [NSString stringWithFormat:@"Processing %@p may take a moment. Please wait.", quality];
    NSString *videoId = self.video[@"videoId"];
    [NPNet resolveHD:videoId quality:quality completion:^(NSString *url, NSError *error) {
        if (![videoId isEqualToString:self.video[@"videoId"]]) return;
        if (url) {
            [self playURL:url message:nil];
        } else {
            [self playURL:[self standardStreamURL]
                  message:[NSString stringWithFormat:@"%@p isn't available, playing 360p", quality]];
        }
    }];
}

- (void)playURL:(NSString *)url message:(NSString *)message {
    [self.spinner stopAnimating];
    self.status.text = message ?: @"";
    if (!url.length) {
        self.status.text = @"No playable stream found";
        return;
    }
    AVPlayer *player = [AVPlayer playerWithURL:[NSURL URLWithString:url]];
    self.playerController.player = player;
    [player play];
    if (message) {
        dispatch_after(dispatch_time(DISPATCH_TIME_NOW, (int64_t) (4 * NSEC_PER_SEC)), dispatch_get_main_queue(), ^{
            self.status.text = @"";
        });
    }
}

- (void)loadComments {
    if (self.commentsLoading || self.comments.count) return;
    self.commentsLoading = YES;
    NSString *path = [NSString stringWithFormat:@"/api/v1/comments/%@", self.video[@"videoId"]];
    [NPNet invidious:path completion:^(id json, NSString *base, NSError *error) {
        self.commentsLoading = NO;
        NSArray *list = [json isKindOfClass:[NSDictionary class]] ? json[@"comments"] : nil;
        NSMutableArray *clean = [NSMutableArray new];
        for (NSDictionary *c in list) {
            if (![c isKindOfClass:[NSDictionary class]]) continue;
            NSMutableDictionary *m = [c mutableCopy];
            m[@"avatar"] = [NPUtil avatarFrom:c[@"authorThumbnails"] base:base];
            [clean addObject:m];
        }
        self.comments = clean;
        if (self.tabs.selectedSegmentIndex == 1) [self.table reloadData];
    }];
}

#pragma mark - Actions

- (void)tabChanged {
    if (self.tabs.selectedSegmentIndex == 1) [self loadComments];
    [self.table reloadData];
}

- (void)chooseQuality {
    UIAlertController *sheet = [UIAlertController alertControllerWithTitle:@"Quality" message:nil
                                                            preferredStyle:UIAlertControllerStyleActionSheet];
    for (NSString *q in @[@"360", @"720", @"1080"]) {
        [sheet addAction:[UIAlertAction actionWithTitle:[q stringByAppendingString:@"p"] style:UIAlertActionStyleDefault
                                                handler:^(UIAlertAction *action) {
            if ([q isEqualToString:[NPUtil quality]]) return;
            [NPUtil setQuality:q];
            self.navigationItem.rightBarButtonItem.title = [q stringByAppendingString:@"p"];
            if (self.details) {
                [self.playerController.player pause];
                self.playerController.player = nil;
                [self startPlayback];
            }
        }]];
    }
    [sheet addAction:[UIAlertAction actionWithTitle:@"Cancel" style:UIAlertActionStyleCancel handler:nil]];
    sheet.popoverPresentationController.barButtonItem = self.navigationItem.rightBarButtonItem;
    [self presentViewController:sheet animated:YES completion:nil];
}

#pragma mark - Table

- (NSInteger)tableView:(UITableView *)tableView numberOfRowsInSection:(NSInteger)section {
    if (self.tabs.selectedSegmentIndex == 1) return MAX(1, (NSInteger) self.comments.count);
    return self.related.count;
}

- (CGFloat)tableView:(UITableView *)tableView heightForRowAtIndexPath:(NSIndexPath *)indexPath {
    if (self.tabs.selectedSegmentIndex == 1) {
        if (!self.comments.count) return 60;
        return [NPCommentCell heightFor:self.comments[indexPath.row] width:tableView.bounds.size.width];
    }
    return [NPVideoCell heightForWidth:tableView.bounds.size.width];
}

- (UITableViewCell *)tableView:(UITableView *)tableView cellForRowAtIndexPath:(NSIndexPath *)indexPath {
    if (self.tabs.selectedSegmentIndex == 1) {
        NPCommentCell *cell = [tableView dequeueReusableCellWithIdentifier:@"comment" forIndexPath:indexPath];
        if (!self.comments.count) {
            cell.header.text = @"";
            cell.body.text = self.commentsLoading ? @"Loading comments…" : @"No comments found";
            cell.avatar.hidden = YES;
        } else {
            NSDictionary *c = self.comments[indexPath.row];
            cell.avatar.hidden = NO;
            NSString *when = [c[@"publishedText"] isKindOfClass:[NSString class]] ? c[@"publishedText"] : @"";
            cell.header.text = [NSString stringWithFormat:@"%@  %@", c[@"author"] ?: @"", when];
            cell.body.text = [c[@"content"] isKindOfClass:[NSString class]] ? c[@"content"] : @"";
            [NPUtil loadImage:c[@"avatar"] into:cell.avatar];
        }
        [cell setNeedsLayout];
        return cell;
    }
    NPVideoCell *cell = [tableView dequeueReusableCellWithIdentifier:@"video" forIndexPath:indexPath];
    [cell showVideo:self.related[indexPath.row]];
    return cell;
}

- (void)tableView:(UITableView *)tableView didSelectRowAtIndexPath:(NSIndexPath *)indexPath {
    [tableView deselectRowAtIndexPath:indexPath animated:YES];
    if (self.tabs.selectedSegmentIndex != 0) return;
    [self.playerController.player pause];
    NPPlayerViewController *next = [[NPPlayerViewController alloc] initWithVideo:self.related[indexPath.row]];
    [self.navigationController pushViewController:next animated:YES];
}

@end
