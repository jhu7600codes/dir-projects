// TRCommentTranslator.m
// TubeReplacer
//
// Comment translator implementation

#import "TRCommentTranslator.h"
#import "TRJSONUtils.h"
#import "../appheaders.h"
#import "../general.h"

@interface YTComment : NSObject
- (id)initWithTitle:(id)title content:(id)content authorDisplayName:(id)displayName publishedDate:(id)date;
// 1.2.1+
- (id)initWithTitle:(id)title content:(id)content authorDisplayName:(id)displayName publishedDate:(id)date isSpam:(BOOL)isSpam;
@end

@implementation TRCommentTranslator

#pragma mark - TRJSONTranslatorProtocol

+ (TREndpointType)supportedEndpoint {
    return TREndpointTypeComment;
}

- (BOOL)canTranslateJSON:(NSDictionary *)json {
    // Feed comment format (wrapped in "i" key from page parser)
    if ([json objectForKey:@"i"]) {
        NSDictionary *inner = [json objectForKey:@"i"];
        if ([inner isKindOfClass:[NSDictionary class]] && [inner objectForKey:@"commentThreadRenderer"]) {
            return YES;
        }
    }
    // Created comment response
    if ([TRJSONUtils dictFromJSON:json keyPath:@"actions[1].createCommentAction"]) {
        return YES;
    }
    return NO;
}

- (id)translateJSON:(NSDictionary *)json error:(NSError **)error {
    if (!json || ![json isKindOfClass:[NSDictionary class]]) {
        if (error) {
            *error = [NSError errorWithDomain:@"TRCommentTranslator" code:1 
                                     userInfo:@{NSLocalizedDescriptionKey: @"Invalid input"}];
        }
        return nil;
    }
    
    if ([json objectForKey:@"i"]) {
        return [self translateFeedComment:json error:error];
    }
    
    return [self translateCreatedComment:json error:error];
}

#pragma mark - Building YTComment

/**
 * Makes a YTComment with whichever initializer this app version has (checked at runtime rather than by
 * version string, since several versions share them) and never passes nil, which crashes the comment cells.
 */
+ (id)commentWithAuthor:(NSString *)author content:(NSString *)content date:(NSDate *)date {
    Class commentClass = NSClassFromString(@"YTComment");
    if (!commentClass) return nil;
    author = [author isKindOfClass:[NSString class]] && [author length] ? author : @"";
    content = [content isKindOfClass:[NSString class]] ? content : @"";
    date = date ?: [NSDate date];
    SEL withSpam = @selector(initWithTitle:content:authorDisplayName:publishedDate:isSpam:);
    SEL plain = @selector(initWithTitle:content:authorDisplayName:publishedDate:);
    if ([commentClass instancesRespondToSelector:withSpam]) {
        return [[[commentClass alloc] initWithTitle:author content:content authorDisplayName:author
                                      publishedDate:date isSpam:NO] autorelease];
    }
    if ([commentClass instancesRespondToSelector:plain]) {
        return [[[commentClass alloc] initWithTitle:author content:content authorDisplayName:author
                                      publishedDate:date] autorelease];
    }
    return nil;
}

#pragma mark - Feed Comment (from page parser)

- (id)translateFeedComment:(NSDictionary *)json error:(NSError **)error {
    NSDictionary *commentData = [TRJSONUtils dictFromJSON:json 
        keyPath:@"i.commentThreadRenderer.comment.commentRenderer"];
    
    if (!commentData) {
        // Newer responses: the thread only holds a key, the text lives in frameworkUpdates
        id viewModelComment = [self translateViewModelComment:json];
        if (viewModelComment) return viewModelComment;
        if (error) {
            *error = [NSError errorWithDomain:@"TRCommentTranslator" code:2 
                                     userInfo:@{NSLocalizedDescriptionKey: @"Missing comment data"}];
        }
        return nil;
    }
    
    NSMutableString *commentText = [NSMutableString string];
    NSArray *runs = [TRJSONUtils arrayFromJSON:commentData keyPath:@"contentText.runs"];
    for (NSDictionary *run in runs) {
        NSString *text = [run objectForKey:@"text"];
        if (text) {
            [commentText appendString:text];
        }
    }
    
    NSString *username = [TRJSONUtils stringFromJSON:commentData keyPath:@"authorText.runs[0].text"];
    NSString *timeAgo = [TRJSONUtils stringFromJSON:commentData keyPath:@"publishedTimeText.runs[0].text"];
    NSDate *publishedDate = [TRJSONUtils dateFromTimeAgo:timeAgo];
    
    id comment = [TRCommentTranslator commentWithAuthor:username content:commentText date:publishedDate];

    return comment;
}

/** commentThreadRenderer.commentViewModel + frameworkUpdates.entityBatchUpdate.mutations (commentEntityPayload) */
- (id)translateViewModelComment:(NSDictionary *)json {
    NSString *commentKey = [TRJSONUtils stringFromJSON:json keyPath:@"i.commentThreadRenderer.commentViewModel.commentViewModel.commentKey"];
    NSArray *mutations = [TRJSONUtils arrayFromJSON:json keyPath:@"all.frameworkUpdates.entityBatchUpdate.mutations"];
    if (![commentKey isKindOfClass:[NSString class]] || ![mutations isKindOfClass:[NSArray class]]) return nil;
    for (NSDictionary *mutation in mutations) {
        if (![mutation isKindOfClass:[NSDictionary class]]) continue;
        if (![[mutation objectForKey:@"entityKey"] isEqual:commentKey]) continue;
        NSDictionary *payload = [TRJSONUtils dictFromJSON:mutation keyPath:@"payload.commentEntityPayload"];
        if (!payload) continue;
        NSString *author = [TRJSONUtils stringFromJSON:payload keyPath:@"author.displayName"];
        NSString *content = [TRJSONUtils stringFromJSON:payload keyPath:@"properties.content.content"];
        NSString *timeAgo = [TRJSONUtils stringFromJSON:payload keyPath:@"properties.publishedTime"];
        return [TRCommentTranslator commentWithAuthor:author content:content date:[TRJSONUtils dateFromTimeAgo:timeAgo]];
    }
    return nil;
}

#pragma mark - Created Comment (just posted)

- (id)translateCreatedComment:(NSDictionary *)json error:(NSError **)error {
    // Path: actions[1].createCommentAction.contents.commentThreadRenderer.comment.commentRenderer
    NSDictionary *commentData = [TRJSONUtils dictFromJSON:json 
        keyPath:@"actions[1].createCommentAction.contents.commentThreadRenderer.comment.commentRenderer"];
    
    if (!commentData) {
        if (error) {
            *error = [NSError errorWithDomain:@"TRCommentTranslator" code:3 
                                     userInfo:@{NSLocalizedDescriptionKey: @"Missing created comment data"}];
        }
        return nil;
    }
    
    // Author from accessibility label
    NSString *username = [TRJSONUtils stringFromJSON:commentData 
        keyPath:@"authorThumbnail.accessibility.accessibilityData.label"];
    
    // Comment text
    NSString *content = [TRJSONUtils stringFromJSON:commentData keyPath:@"contentText.runs[0].text"];
    
    // Just created, so use current date
    NSDate *publishedDate = [NSDate date];
    
    id comment = [TRCommentTranslator commentWithAuthor:username content:content date:publishedDate];
    
    return comment;
}

@end
