.class final Lbezv;
.super Lorg/chromium/net/UrlRequest$Callback;
.source "PG"


# instance fields
.field final synthetic a:Lbezw;


# direct methods
.method public constructor <init>(Lbezw;)V
    .locals 0

    iput-object p1, p0, Lbezv;->a:Lbezw;

    invoke-direct {p0}, Lorg/chromium/net/UrlRequest$Callback;-><init>()V

    return-void
.end method

.method private final a(Ljava/io/IOException;)V
    .locals 4

    iget-object v0, p0, Lbezv;->a:Lbezw;

    iput-object p1, v0, Lbezw;->f:Ljava/io/IOException;

    iget-object v1, v0, Lbezw;->c:Lbezy;

    const/4 v2, 0x1

    if-eqz v1, :cond_0

    iput-object p1, v1, Lbezy;->c:Ljava/io/IOException;

    iput-boolean v2, v1, Lbezy;->a:Z

    const/4 v3, 0x0

    iput-object v3, v1, Lbezy;->b:Ljava/nio/ByteBuffer;

    :cond_0
    iget-object v1, v0, Lbezw;->d:Lbezz;

    if-eqz v1, :cond_1

    iput-object p1, v1, Lbezz;->d:Ljava/io/IOException;

    iput-boolean v2, v1, Lbezz;->e:Z

    .line 1
    :cond_1
    invoke-static {v0}, Lbezw;->d(Lbezw;)V

    iget-object p1, p0, Lbezv;->a:Lbezw;

    iget-object p1, p1, Lbezw;->a:Lbfab;

    .line 2
    invoke-virtual {p1}, Lbfab;->c()V

    return-void
.end method


# virtual methods
.method public final onCanceled(Lorg/chromium/net/UrlRequest;Lorg/chromium/net/UrlResponseInfo;)V
    .locals 0

    iget-object p1, p0, Lbezv;->a:Lbezw;

    iput-object p2, p1, Lbezw;->e:Lorg/chromium/net/UrlResponseInfo;

    new-instance p1, Ljava/io/IOException;

    const-string p2, "disconnect() called"

    .line 1
    invoke-direct {p1, p2}, Ljava/io/IOException;-><init>(Ljava/lang/String;)V

    invoke-direct {p0, p1}, Lbezv;->a(Ljava/io/IOException;)V

    return-void
.end method

.method public final onFailed(Lorg/chromium/net/UrlRequest;Lorg/chromium/net/UrlResponseInfo;Lorg/chromium/net/CronetException;)V
    .locals 0

    if-eqz p3, :cond_0

    .line 1
    iget-object p1, p0, Lbezv;->a:Lbezw;

    iput-object p2, p1, Lbezw;->e:Lorg/chromium/net/UrlResponseInfo;

    .line 2
    invoke-direct {p0, p3}, Lbezv;->a(Ljava/io/IOException;)V

    return-void

    .line 0
    :cond_0
    new-instance p1, Ljava/lang/IllegalStateException;

    const-string p2, "Exception cannot be null in onFailed."

    .line 1
    invoke-direct {p1, p2}, Ljava/lang/IllegalStateException;-><init>(Ljava/lang/String;)V

    throw p1
.end method

.method public final onReadCompleted(Lorg/chromium/net/UrlRequest;Lorg/chromium/net/UrlResponseInfo;Ljava/nio/ByteBuffer;)V
    .locals 3

    # translyte diagnostic: see abmu.smali's identical comment.
    const-string v0, "translyteDebug"

    new-instance v1, Ljava/lang/StringBuilder;

    invoke-direct {v1}, Ljava/lang/StringBuilder;-><init>()V

    const-string v2, "translyte: onReadCompleted class=Lbezv; url="

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

    iget-object p1, p0, Lbezv;->a:Lbezw;

    iput-object p2, p1, Lbezw;->e:Lorg/chromium/net/UrlResponseInfo;

    iget-object p1, p1, Lbezw;->a:Lbfab;

    .line 1
    invoke-virtual {p1}, Lbfab;->c()V

    return-void
.end method

.method public final onRedirectReceived(Lorg/chromium/net/UrlRequest;Lorg/chromium/net/UrlResponseInfo;Ljava/lang/String;)V
    .locals 1

    iget-object p1, p0, Lbezv;->a:Lbezw;

    const/4 v0, 0x1

    iput-boolean v0, p1, Lbezw;->g:Z

    .line 1
    :try_start_0
    new-instance p1, Ljava/net/URL;

    invoke-direct {p1, p3}, Ljava/net/URL;-><init>(Ljava/lang/String;)V

    .line 2
    invoke-virtual {p1}, Ljava/net/URL;->getProtocol()Ljava/lang/String;

    move-result-object p3

    iget-object v0, p0, Lbezv;->a:Lbezw;

    invoke-static {v0}, Lbezw;->a(Lbezw;)Ljava/net/URL;

    move-result-object v0

    invoke-virtual {v0}, Ljava/net/URL;->getProtocol()Ljava/lang/String;

    move-result-object v0

    invoke-virtual {p3, v0}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result p3

    iget-object v0, p0, Lbezv;->a:Lbezw;

    .line 3
    invoke-static {v0}, Lbezw;->b(Lbezw;)Z

    move-result v0

    if-eqz v0, :cond_0

    iget-object v0, p0, Lbezv;->a:Lbezw;

    .line 4
    invoke-static {v0, p1}, Lbezw;->e(Lbezw;Ljava/net/URL;)V

    :cond_0
    iget-object p1, p0, Lbezv;->a:Lbezw;

    .line 5
    invoke-static {p1}, Lbezw;->c(Lbezw;)Z

    move-result p1

    if-eqz p1, :cond_1

    if-eqz p3, :cond_1

    iget-object p1, p0, Lbezv;->a:Lbezw;

    iget-object p1, p1, Lbezw;->b:Lorg/chromium/net/UrlRequest;

    .line 6
    invoke-virtual {p1}, Lorg/chromium/net/UrlRequest;->followRedirect()V
    :try_end_0
    .catch Ljava/net/MalformedURLException; {:try_start_0 .. :try_end_0} :catch_0

    return-void

    :catch_0
    :cond_1
    iget-object p1, p0, Lbezv;->a:Lbezw;

    iput-object p2, p1, Lbezw;->e:Lorg/chromium/net/UrlResponseInfo;

    iget-object p1, p1, Lbezw;->b:Lorg/chromium/net/UrlRequest;

    .line 7
    invoke-virtual {p1}, Lorg/chromium/net/UrlRequest;->cancel()V

    const/4 p1, 0x0

    .line 8
    invoke-direct {p0, p1}, Lbezv;->a(Ljava/io/IOException;)V

    return-void
.end method

.method public final onResponseStarted(Lorg/chromium/net/UrlRequest;Lorg/chromium/net/UrlResponseInfo;)V
    .locals 0

    iget-object p1, p0, Lbezv;->a:Lbezw;

    iput-object p2, p1, Lbezw;->e:Lorg/chromium/net/UrlResponseInfo;

    .line 1
    invoke-static {p1}, Lbezw;->d(Lbezw;)V

    iget-object p1, p0, Lbezv;->a:Lbezw;

    iget-object p1, p1, Lbezw;->a:Lbfab;

    .line 2
    invoke-virtual {p1}, Lbfab;->c()V

    return-void
.end method

.method public final onSucceeded(Lorg/chromium/net/UrlRequest;Lorg/chromium/net/UrlResponseInfo;)V
    .locals 0

    iget-object p1, p0, Lbezv;->a:Lbezw;

    iput-object p2, p1, Lbezw;->e:Lorg/chromium/net/UrlResponseInfo;

    const/4 p1, 0x0

    .line 1
    invoke-direct {p0, p1}, Lbezv;->a(Ljava/io/IOException;)V

    return-void
.end method
