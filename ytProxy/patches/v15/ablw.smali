.class final Lablw;
.super Lorg/chromium/net/UrlRequest$Callback;
.source "PG"


# instance fields
.field public final a:Labmh;

.field public b:Z

.field public c:Ljava/lang/Object;

.field public d:Z

.field public e:Z

.field private final f:Z

.field private final g:Lablz;

.field private h:Labmk;

.field private i:Ljava/io/IOException;


# direct methods
.method protected constructor <init>(Labmh;ZLablz;)V
    .locals 0

    invoke-direct {p0}, Lorg/chromium/net/UrlRequest$Callback;-><init>()V

    iput-object p1, p0, Lablw;->a:Labmh;

    iput-boolean p2, p0, Lablw;->f:Z

    iput-object p3, p0, Lablw;->g:Lablz;

    return-void
.end method

.method protected static bridge synthetic b(Lorg/chromium/net/UrlResponseInfo;Ljava/io/InputStream;)Ljava/lang/Object;
    .locals 6

    invoke-static {}, Labkb;->c()Labjz;

    move-result-object v0

    invoke-virtual {p0}, Lorg/chromium/net/UrlResponseInfo;->getAllHeadersAsList()Ljava/util/List;

    move-result-object v1

    invoke-virtual {v0, v1}, Labjz;->c(Ljava/util/Collection;)V

    invoke-virtual {v0}, Labjz;->b()Labkb;

    move-result-object v0

    new-instance v1, Labkn;

    invoke-direct {v1}, Labkn;-><init>()V

    invoke-virtual {p0}, Lorg/chromium/net/UrlResponseInfo;->getHttpStatusCode()I

    move-result v2

    invoke-static {v2}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    move-result-object v2

    iput-object v2, v1, Labkn;->b:Ljava/lang/Integer;

    invoke-virtual {p0}, Lorg/chromium/net/UrlResponseInfo;->getHttpStatusText()Ljava/lang/String;

    move-result-object v2

    if-eqz v2, :cond_b

    iput-object v2, v1, Labkn;->c:Ljava/lang/String;

    invoke-static {}, Labkb;->c()Labjz;

    move-result-object v2

    invoke-virtual {p0}, Lorg/chromium/net/UrlResponseInfo;->getAllHeadersAsList()Ljava/util/List;

    move-result-object v3

    invoke-virtual {v2, v3}, Labjz;->c(Ljava/util/Collection;)V

    invoke-virtual {v2}, Labjz;->b()Labkb;

    move-result-object v2

    iput-object v2, v1, Labkn;->d:Labkb;

    invoke-virtual {p0}, Lorg/chromium/net/UrlResponseInfo;->getNegotiatedProtocol()Ljava/lang/String;

    move-result-object p0

    if-eqz p0, :cond_0

    invoke-virtual {p0}, Ljava/lang/String;->isEmpty()Z

    move-result v2

    if-eqz v2, :cond_1

    :cond_0
    const-string p0, "HTTP/1.1"

    :cond_1
    iput-object p0, v1, Labkn;->a:Ljava/lang/String;

    if-eqz p1, :cond_4

    const-string p0, "Content-Type"

    invoke-virtual {v0, p0}, Labkb;->a(Ljava/lang/String;)Ljava/lang/String;

    move-result-object p0

    const-string v2, "content-encoding"

    invoke-virtual {v0, v2}, Labkb;->a(Ljava/lang/String;)Ljava/lang/String;

    move-result-object v2

    const-string v3, "-1"

    if-eqz v2, :cond_2

    const-string v4, "identity"

    invoke-virtual {v4, v2}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v2

    if-nez v2, :cond_2

    goto :goto_0

    :cond_2
    const-string v2, "transfer-encoding"

    invoke-virtual {v0, v2}, Labkb;->a(Ljava/lang/String;)Ljava/lang/String;

    move-result-object v2

    if-eqz v2, :cond_3

    goto :goto_0

    :cond_3
    const-string v2, "content-length"

    invoke-virtual {v0, v2}, Labkb;->a(Ljava/lang/String;)Ljava/lang/String;

    move-result-object v3

    :goto_0
    new-instance v0, Labkm;

    invoke-direct {v0, p0, v3}, Labkm;-><init>(Ljava/lang/String;Ljava/lang/String;)V

    iput-object p1, v0, Labkm;->b:Ljava/io/InputStream;

    iput-object v0, v1, Labkn;->e:Labkm;

    goto :goto_1

    :cond_4
    sget-object p0, Labkm;->a:Labkm;

    iput-object p0, v1, Labkn;->e:Labkm;

    :goto_1
    iget-object p0, v1, Labkn;->a:Ljava/lang/String;

    if-nez p0, :cond_5

    const-string p0, " protocol"

    goto :goto_2

    :cond_5
    const-string p0, ""

    :goto_2
    iget-object p1, v1, Labkn;->b:Ljava/lang/Integer;

    if-nez p1, :cond_6

    const-string p1, " statusCode"

    invoke-virtual {p0, p1}, Ljava/lang/String;->concat(Ljava/lang/String;)Ljava/lang/String;

    move-result-object p0

    :cond_6
    iget-object p1, v1, Labkn;->c:Ljava/lang/String;

    if-nez p1, :cond_7

    invoke-static {p0}, Ljava/lang/String;->valueOf(Ljava/lang/Object;)Ljava/lang/String;

    move-result-object p0

    const-string p1, " reasonPhrase"

    invoke-virtual {p0, p1}, Ljava/lang/String;->concat(Ljava/lang/String;)Ljava/lang/String;

    move-result-object p0

    :cond_7
    iget-object p1, v1, Labkn;->d:Labkb;

    if-nez p1, :cond_8

    invoke-static {p0}, Ljava/lang/String;->valueOf(Ljava/lang/Object;)Ljava/lang/String;

    move-result-object p0

    const-string p1, " headers"

    invoke-virtual {p0, p1}, Ljava/lang/String;->concat(Ljava/lang/String;)Ljava/lang/String;

    move-result-object p0

    :cond_8
    invoke-virtual {p0}, Ljava/lang/String;->isEmpty()Z

    move-result p1

    if-nez p1, :cond_a

    new-instance p1, Ljava/lang/IllegalStateException;

    invoke-static {p0}, Ljava/lang/String;->valueOf(Ljava/lang/Object;)Ljava/lang/String;

    move-result-object p0

    const-string v0, "Missing required properties:"

    invoke-virtual {p0}, Ljava/lang/String;->length()I

    move-result v1

    if-eqz v1, :cond_9

    invoke-virtual {v0, p0}, Ljava/lang/String;->concat(Ljava/lang/String;)Ljava/lang/String;

    move-result-object p0

    goto :goto_3

    :cond_9
    new-instance p0, Ljava/lang/String;

    invoke-direct {p0, v0}, Ljava/lang/String;-><init>(Ljava/lang/String;)V

    :goto_3
    invoke-direct {p1, p0}, Ljava/lang/IllegalStateException;-><init>(Ljava/lang/String;)V

    throw p1

    :cond_a
    new-instance p0, Labko;

    iget-object p1, v1, Labkn;->a:Ljava/lang/String;

    iget-object v0, v1, Labkn;->b:Ljava/lang/Integer;

    invoke-virtual {v0}, Ljava/lang/Integer;->intValue()I

    move-result v2

    iget-object v3, v1, Labkn;->c:Ljava/lang/String;

    iget-object v4, v1, Labkn;->d:Labkb;

    iget-object v5, v1, Labkn;->e:Labkm;

    move-object v0, p0

    move-object v1, p1

    invoke-direct/range {v0 .. v5}, Labko;-><init>(Ljava/lang/String;ILjava/lang/String;Labkb;Labkm;)V

    return-object p0

    :cond_b
    new-instance p0, Ljava/lang/NullPointerException;

    const-string p1, "Null reasonPhrase"

    invoke-direct {p0, p1}, Ljava/lang/NullPointerException;-><init>(Ljava/lang/String;)V

    throw p0
.end method

.method private final c(Ljava/io/IOException;)V
    .locals 1

    iget-object v0, p0, Lablw;->a:Labmh;

    .line 1
    invoke-virtual {v0}, Labmh;->c()V

    const/4 v0, 0x1

    iput-boolean v0, p0, Lablw;->d:Z

    iput-object p1, p0, Lablw;->i:Ljava/io/IOException;

    iget-object p1, p0, Lablw;->h:Labmk;

    if-eqz p1, :cond_0

    const/4 v0, 0x0

    iput-object v0, p1, Labmk;->a:Ljava/nio/ByteBuffer;

    :cond_0
    return-void
.end method


# virtual methods
.method public final a()V
    .locals 1

    iget-object v0, p0, Lablw;->i:Ljava/io/IOException;

    if-nez v0, :cond_0

    return-void

    .line 1
    :cond_0
    throw v0
.end method

.method public final onCanceled(Lorg/chromium/net/UrlRequest;Lorg/chromium/net/UrlResponseInfo;)V
    .locals 0

    iget-object p1, p0, Lablw;->a:Labmh;

    .line 1
    invoke-virtual {p1}, Labmh;->c()V

    iget-boolean p1, p0, Lablw;->e:Z

    if-eqz p1, :cond_0

    const/4 p1, 0x0

    goto :goto_0

    .line 2
    :cond_0
    sget-object p1, Labmi;->a:Labmj;

    .line 3
    :goto_0
    invoke-direct {p0, p1}, Lablw;->c(Ljava/io/IOException;)V

    return-void
.end method

.method public final onFailed(Lorg/chromium/net/UrlRequest;Lorg/chromium/net/UrlResponseInfo;Lorg/chromium/net/CronetException;)V
    .locals 0

    iget-object p1, p0, Lablw;->a:Labmh;

    .line 1
    invoke-virtual {p1}, Labmh;->c()V

    .line 2
    invoke-virtual {p3}, Lorg/chromium/net/CronetException;->getCause()Ljava/lang/Throwable;

    move-result-object p1

    .line 3
    instance-of p2, p1, Ljava/io/IOException;

    if-eqz p2, :cond_0

    move-object p3, p1

    check-cast p3, Ljava/io/IOException;

    :cond_0
    invoke-direct {p0, p3}, Lablw;->c(Ljava/io/IOException;)V

    return-void
.end method

.method public final onReadCompleted(Lorg/chromium/net/UrlRequest;Lorg/chromium/net/UrlResponseInfo;Ljava/nio/ByteBuffer;)V
    .locals 3

    # ytProxy diagnostic: see abmu.smali's identical comment.
    const-string v0, "ytProxyDebug"

    new-instance v1, Ljava/lang/StringBuilder;

    invoke-direct {v1}, Ljava/lang/StringBuilder;-><init>()V

    const-string v2, "ytProxy: onReadCompleted class=Lablw; url="

    invoke-virtual {v1, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {p2}, Lorg/chromium/net/UrlResponseInfo;->getUrl()Ljava/lang/String;

    move-result-object v2

    invoke-virtual {v1, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    const-string v2, " bufPos="

    invoke-virtual {v1, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {p3}, Ljava/nio/ByteBuffer;->position()I

    move-result v2

    invoke-virtual {v1, v2}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    invoke-virtual {v1}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v1

    invoke-static {v0, v1}, Landroid/util/Log;->w(Ljava/lang/String;Ljava/lang/String;)I

    iget-object p1, p0, Lablw;->a:Labmh;

    .line 1
    invoke-virtual {p1}, Labmh;->c()V

    iget-object p1, p0, Lablw;->h:Labmk;

    if-nez p1, :cond_0

    new-instance p1, Ljava/io/IOException;

    .line 2
    invoke-direct {p1}, Ljava/io/IOException;-><init>()V

    invoke-direct {p0, p1}, Lablw;->c(Ljava/io/IOException;)V

    :cond_0
    return-void
.end method

.method public final onRedirectReceived(Lorg/chromium/net/UrlRequest;Lorg/chromium/net/UrlResponseInfo;Ljava/lang/String;)V
    .locals 1

    :try_start_0
    iget-boolean v0, p0, Lablw;->f:Z

    if-eqz v0, :cond_1

    iget-object p2, p0, Lablw;->g:Lablz;

    iget-object p2, p2, Lablz;->d:Lakgm;

    if-eqz p2, :cond_0

    .line 1
    invoke-static {p3}, Lakgm;->e(Ljava/lang/String;)V

    .line 2
    :cond_0
    invoke-virtual {p1}, Lorg/chromium/net/UrlRequest;->followRedirect()V

    return-void

    :cond_1
    iget-object p3, p0, Lablw;->a:Labmh;

    .line 3
    invoke-virtual {p3}, Labmh;->c()V

    const/4 p3, 0x0

    .line 4
    invoke-static {p2, p3}, Lablw;->b(Lorg/chromium/net/UrlResponseInfo;Ljava/io/InputStream;)Ljava/lang/Object;

    move-result-object p2

    iput-object p2, p0, Lablw;->c:Ljava/lang/Object;

    const/4 p2, 0x1

    iput-boolean p2, p0, Lablw;->b:Z

    iput-boolean p2, p0, Lablw;->e:Z

    .line 5
    invoke-virtual {p1}, Lorg/chromium/net/UrlRequest;->cancel()V
    :try_end_0
    .catch Ljava/io/IOException; {:try_start_0 .. :try_end_0} :catch_0

    return-void

    :catch_0
    move-exception p1

    .line 6
    invoke-direct {p0, p1}, Lablw;->c(Ljava/io/IOException;)V

    return-void
.end method

.method public final onResponseStarted(Lorg/chromium/net/UrlRequest;Lorg/chromium/net/UrlResponseInfo;)V
    .locals 1

    iget-object v0, p0, Lablw;->a:Labmh;

    .line 1
    invoke-virtual {v0}, Labmh;->c()V

    new-instance v0, Labmk;

    .line 2
    invoke-direct {v0, p1, p0}, Labmk;-><init>(Lorg/chromium/net/UrlRequest;Lablw;)V

    iput-object v0, p0, Lablw;->h:Labmk;

    .line 3
    invoke-static {p2, v0}, Lablw;->b(Lorg/chromium/net/UrlResponseInfo;Ljava/io/InputStream;)Ljava/lang/Object;

    move-result-object p1

    iput-object p1, p0, Lablw;->c:Ljava/lang/Object;

    const/4 p1, 0x1

    iput-boolean p1, p0, Lablw;->b:Z

    return-void
.end method

.method public final onSucceeded(Lorg/chromium/net/UrlRequest;Lorg/chromium/net/UrlResponseInfo;)V
    .locals 0

    iget-object p1, p0, Lablw;->a:Labmh;

    .line 1
    invoke-virtual {p1}, Labmh;->c()V

    const/4 p1, 0x0

    .line 2
    invoke-direct {p0, p1}, Lablw;->c(Ljava/io/IOException;)V

    return-void
.end method
