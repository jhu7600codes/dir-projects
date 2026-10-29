// on-device list of subscriptions, manually added links and the selected
// server. lives in /var/mobile/Library/Happe/state.plist (0600), never in git.

#import <Foundation/Foundation.h>

@interface HPServer : NSObject
@property (nonatomic, copy) NSString *link;
@property (nonatomic, copy) NSString *name;
@property (nonatomic, copy) NSString *detail;
+ (instancetype)serverWithLink:(NSString *)link;
@end

@interface HPSub : NSObject
@property (nonatomic, copy) NSString *subID;
@property (nonatomic, copy) NSString *url;
@property (nonatomic, copy) NSString *title;          // profile-title header, may be nil
@property (nonatomic, copy) NSArray<HPServer *> *servers;
@property (nonatomic, assign) NSUInteger skipped;    // non-vless entries
@property (nonatomic, copy) NSDictionary *userinfo;  // upload/download/total/expire
@property (nonatomic, strong) NSDate *updated;
@property (nonatomic, copy) NSString *error;         // last refresh error
@property (nonatomic, assign) BOOL refreshing;
- (NSString *)displayTitle;
- (void)setLinks:(NSArray<NSString *> *)links; // keeps vless entries, counts the rest
@end

@interface HPStore : NSObject
@property (nonatomic, readonly) NSMutableArray<HPSub *> *subs;
@property (nonatomic, readonly) NSMutableArray<HPServer *> *manual;
@property (nonatomic, copy) NSString *selectedLink;

+ (instancetype)shared;
- (void)save;
- (HPServer *)selectedServer;
- (NSArray<HPServer *> *)allServers;

// splits pasted text into https subscriptions and vless:// links.
// returns the number of things added; *message explains anything skipped.
- (NSUInteger)addFromText:(NSString *)text newSubs:(NSArray<HPSub *> **)newSubs message:(NSString **)message;
- (void)removeSub:(HPSub *)sub;
- (void)removeManual:(HPServer *)server;

// refetches one subscription; done runs on the main queue.
- (void)refresh:(HPSub *)sub done:(void (^)(void))done;
@end
