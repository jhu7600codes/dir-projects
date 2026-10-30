//
//  NPListViewController.m
//  notPipe for iOS
//
//  Created by Claude on 30.10.2026.
//

#import "NPListViewController.h"
#import "NPNet.h"
#import "NPPlayerViewController.h"
#import "NPUtil.h"
#import "NPVideoCell.h"

/** The same rotating "hype" playlists the Android app uses for its Home feed */
static NSString *NPHomePlaylist(void) {
    NSArray *lists = @[@"OLXuPDPDe3URMWBdmS4_jxdqVF08DAlAXWQ", @"OLd7LoUR1ndxIsyTJ_5pK4tvrWBcvkewBLg",
                       @"OLNUaVf-BhE20xpEEdOmnxF6oyKQwpN1tBQ", @"OLUZ2nAWnPVwwoXomaTvbFi1Cje8td4Z0zg",
                       @"OLewrgOzMLeOADYzQ1ewNeqtPlJP20RE_Zg", @"OLbXum44nJ19cZmR7GxDm5rF_Nj_coRoO5g",
                       @"OLPPB3977IfFUtPW8213fFOsf2lmAWfahDg", @"OLZyZj8vWFMkIOGlFzyKuFlLA2PY42VhRuA"];
    return lists[arc4random_uniform((uint32_t)lists.count)];
}

@interface NPListViewController () <UISearchBarDelegate>
@property (nonatomic) NPListMode mode;
@property (nonatomic, strong) NSArray *videos;
@property (nonatomic, strong) UISearchBar *searchBar;
@property (nonatomic, strong) UILabel *message;
@property (nonatomic, strong) UIActivityIndicatorView *spinner;
@end

@implementation NPListViewController

- (instancetype)initWithMode:(NPListMode)mode {
    self = [super initWithStyle:UITableViewStylePlain];
    if (self) {
        _mode = mode;
        _videos = [NSArray array];
        if (mode == NPListModeHome) {
            self.title = @"notPipe";
            self.tabBarItem = [[UITabBarItem alloc] initWithTitle:@"Home" image:[UIImage imageNamed:@"tab_home"] tag:0];
        } else if (mode == NPListModeSearch) {
            self.title = @"Search";
            self.tabBarItem = [[UITabBarItem alloc] initWithTabBarSystemItem:UITabBarSystemItemSearch tag:1];
        } else {
            self.title = @"Library";
            self.tabBarItem = [[UITabBarItem alloc] initWithTitle:@"Library" image:[UIImage imageNamed:@"tab_library"] tag:2];
        }
    }
    return self;
}

- (void)viewDidLoad {
    [super viewDidLoad];
    self.tableView.backgroundColor = NP_BG;
    self.tableView.separatorStyle = UITableViewCellSeparatorStyleNone;
    [self.tableView registerClass:[NPVideoCell class] forCellReuseIdentifier:@"video"];

    self.spinner = [[UIActivityIndicatorView alloc] initWithActivityIndicatorStyle:UIActivityIndicatorViewStyleWhiteLarge];
    self.spinner.hidesWhenStopped = YES;
    self.message = [UILabel new];
    self.message.textColor = NP_GREY;
    self.message.textAlignment = NSTextAlignmentCenter;
    self.message.numberOfLines = 0;

    if (self.mode == NPListModeSearch) {
        self.searchBar = [UISearchBar new];
        self.searchBar.barStyle = UIBarStyleBlack;
        self.searchBar.placeholder = @"Search YouTube";
        self.searchBar.delegate = self;
        [self.searchBar sizeToFit];
        self.tableView.tableHeaderView = self.searchBar;
        self.tableView.keyboardDismissMode = UIScrollViewKeyboardDismissModeOnDrag;
    } else if (self.mode == NPListModeHome) {
        self.refreshControl = [UIRefreshControl new];
        self.refreshControl.tintColor = [UIColor whiteColor];
        [self.refreshControl addTarget:self action:@selector(loadHome) forControlEvents:UIControlEventValueChanged];
        [self loadHome];
    } else {
        self.navigationItem.rightBarButtonItem = [[UIBarButtonItem alloc] initWithTitle:@"Clear" style:UIBarButtonItemStylePlain
                                                                                 target:self action:@selector(clearHistory)];
    }
    [self.tableView addSubview:self.spinner];
    [self.tableView addSubview:self.message];
}

- (void)viewWillAppear:(BOOL)animated {
    [super viewWillAppear:animated];
    if (self.mode == NPListModeHistory) {
        self.videos = [NPUtil history];
        [self showMessage:self.videos.count ? nil : @"Videos you watch show up here"];
        [self.tableView reloadData];
    }
}

- (void)viewDidLayoutSubviews {
    [super viewDidLayoutSubviews];
    CGFloat w = self.tableView.bounds.size.width;
    CGFloat top = self.tableView.tableHeaderView ? CGRectGetMaxY(self.tableView.tableHeaderView.frame) : 0;
    self.spinner.center = CGPointMake(w / 2, top + 60);
    self.message.frame = CGRectMake(24, top + 30, w - 48, 60);
}

- (void)showMessage:(NSString *)text {
    self.message.text = text;
    self.message.hidden = text == nil;
}

#pragma mark - Loading

- (void)loadHome {
    if (!self.refreshControl.isRefreshing) [self.spinner startAnimating];
    [self showMessage:nil];
    NSString *path = [NSString stringWithFormat:@"/api/v1/playlists/%@", NPHomePlaylist()];
    [NPNet invidious:path completion:^(id json, NSString *base, NSError *error) {
        NSArray *videos = [json isKindOfClass:[NSDictionary class]] ? json[@"videos"] : nil;
        if ([videos isKindOfClass:[NSArray class]] && videos.count) {
            [self finishLoading:videos error:nil];
            return;
        }
        [NPNet invidious:@"/api/v1/trending" completion:^(id json2, NSString *base2, NSError *error2) {
            [self finishLoading:[json2 isKindOfClass:[NSArray class]] ? json2 : nil error:error2 ?: error];
        }];
    }];
}

- (void)search:(NSString *)query {
    [self.spinner startAnimating];
    [self showMessage:nil];
    self.videos = [NSArray array];
    [self.tableView reloadData];
    NSString *encoded = [query stringByAddingPercentEncodingWithAllowedCharacters:[NSCharacterSet URLQueryAllowedCharacterSet]];
    encoded = [encoded stringByReplacingOccurrencesOfString:@"&" withString:@"%26"];
    NSString *path = [NSString stringWithFormat:@"/api/v1/search?type=video&q=%@", encoded];
    [NPNet invidious:path completion:^(id json, NSString *base, NSError *error) {
        [self finishLoading:[json isKindOfClass:[NSArray class]] ? json : nil error:error];
    }];
}

- (void)finishLoading:(NSArray *)videos error:(NSError *)error {
    [self.spinner stopAnimating];
    [self.refreshControl endRefreshing];
    NSMutableArray *clean = [NSMutableArray new];
    for (id v in videos) {
        if ([v isKindOfClass:[NSDictionary class]] && [v[@"videoId"] isKindOfClass:[NSString class]]) [clean addObject:v];
    }
    self.videos = clean;
    [self showMessage:clean.count ? nil : (error ? @"Couldn't load, pull down to try again" : @"Nothing found")];
    [self.tableView reloadData];
}

- (void)clearHistory {
    [NPUtil clearHistory];
    self.videos = [NSArray array];
    [self showMessage:@"Videos you watch show up here"];
    [self.tableView reloadData];
}

#pragma mark - Search bar

- (void)searchBarSearchButtonClicked:(UISearchBar *)searchBar {
    [searchBar resignFirstResponder];
    NSString *query = [searchBar.text stringByTrimmingCharactersInSet:[NSCharacterSet whitespaceCharacterSet]];
    if (query.length) [self search:query];
}

#pragma mark - Table

- (NSInteger)tableView:(UITableView *)tableView numberOfRowsInSection:(NSInteger)section {
    return self.videos.count;
}

- (CGFloat)tableView:(UITableView *)tableView heightForRowAtIndexPath:(NSIndexPath *)indexPath {
    return [NPVideoCell heightForWidth:tableView.bounds.size.width];
}

- (UITableViewCell *)tableView:(UITableView *)tableView cellForRowAtIndexPath:(NSIndexPath *)indexPath {
    NPVideoCell *cell = [tableView dequeueReusableCellWithIdentifier:@"video" forIndexPath:indexPath];
    [cell showVideo:self.videos[indexPath.row]];
    return cell;
}

- (void)tableView:(UITableView *)tableView didSelectRowAtIndexPath:(NSIndexPath *)indexPath {
    [tableView deselectRowAtIndexPath:indexPath animated:YES];
    NPPlayerViewController *player = [[NPPlayerViewController alloc] initWithVideo:self.videos[indexPath.row]];
    [self.navigationController pushViewController:player animated:YES];
}

@end
