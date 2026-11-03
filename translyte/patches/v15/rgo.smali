.class final Lrgo;
.super Lorg/chromium/net/UrlRequest$Callback;
.source "PG"


# instance fields
.field final synthetic a:Lrgp;


# direct methods
.method public constructor <init>(Lrgp;)V
    .locals 0

    iput-object p1, p0, Lrgo;->a:Lrgp;

    invoke-direct {p0}, Lorg/chromium/net/UrlRequest$Callback;-><init>()V

    return-void
.end method


# virtual methods
.method public final declared-synchronized onFailed(Lorg/chromium/net/UrlRequest;Lorg/chromium/net/UrlResponseInfo;Lorg/chromium/net/CronetException;)V
    .locals 0

    monitor-enter p0

    :try_start_0
    iget-object p2, p0, Lrgo;->a:Lrgp;

    iget-object p2, p2, Lrgp;->d:Lorg/chromium/net/UrlRequest;
    :try_end_0
    .catchall {:try_start_0 .. :try_end_0} :catchall_0

    if-eq p1, p2, :cond_0

    monitor-exit p0

    return-void

    .line 1
    :cond_0
    :try_start_1
    instance-of p1, p3, Lorg/chromium/net/NetworkException;

    if-eqz p1, :cond_1

    move-object p1, p3

    check-cast p1, Lorg/chromium/net/NetworkException;

    .line 2
    invoke-virtual {p1}, Lorg/chromium/net/NetworkException;->getErrorCode()I

    move-result p1

    const/4 p2, 0x1

    if-ne p1, p2, :cond_1

    iget-object p1, p0, Lrgo;->a:Lrgp;

    .line 3
    new-instance p2, Ljava/net/UnknownHostException;

    invoke-direct {p2}, Ljava/net/UnknownHostException;-><init>()V

    iput-object p2, p1, Lrgp;->g:Ljava/io/IOException;

    goto :goto_0

    .line 4
    :cond_1
    iget-object p1, p0, Lrgo;->a:Lrgp;

    iput-object p3, p1, Lrgp;->g:Ljava/io/IOException;

    .line 3
    :goto_0
    iget-object p1, p0, Lrgo;->a:Lrgp;

    iget-object p1, p1, Lrgp;->c:Lscj;

    .line 4
    invoke-virtual {p1}, Lscj;->a()Z
    :try_end_1
    .catchall {:try_start_1 .. :try_end_1} :catchall_0

    monitor-exit p0

    return-void

    :catchall_0
    move-exception p1

    monitor-exit p0

    throw p1
.end method

.method public final declared-synchronized onReadCompleted(Lorg/chromium/net/UrlRequest;Lorg/chromium/net/UrlResponseInfo;Ljava/nio/ByteBuffer;)V
    .locals 3

    # translyte diagnostic: see abmu.smali's identical comment. Found via a
    # broader .super sweep after the first 6 candidates never fired.
    const-string v0, "translyteDebug"

    new-instance v1, Ljava/lang/StringBuilder;

    invoke-direct {v1}, Ljava/lang/StringBuilder;-><init>()V

    const-string v2, "translyte: onReadCompleted class=Lrgo; url="

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

    monitor-enter p0

    :try_start_0
    iget-object p2, p0, Lrgo;->a:Lrgp;

    iget-object p3, p2, Lrgp;->d:Lorg/chromium/net/UrlRequest;
    :try_end_0
    .catchall {:try_start_0 .. :try_end_0} :catchall_0

    if-eq p1, p3, :cond_0

    monitor-exit p0

    return-void

    :cond_0
    :try_start_1
    iget-object p1, p2, Lrgp;->c:Lscj;

    .line 1
    invoke-virtual {p1}, Lscj;->a()Z
    :try_end_1
    .catchall {:try_start_1 .. :try_end_1} :catchall_0

    monitor-exit p0

    return-void

    :catchall_0
    move-exception p1

    monitor-exit p0

    throw p1
.end method

.method public final declared-synchronized onRedirectReceived(Lorg/chromium/net/UrlRequest;Lorg/chromium/net/UrlResponseInfo;Ljava/lang/String;)V
    .locals 3

    monitor-enter p0

    :try_start_0
    iget-object p3, p0, Lrgo;->a:Lrgp;

    iget-object p3, p3, Lrgp;->d:Lorg/chromium/net/UrlRequest;
    :try_end_0
    .catchall {:try_start_0 .. :try_end_0} :catchall_0

    if-eq p1, p3, :cond_0

    monitor-exit p0

    return-void

    .line 1
    :cond_0
    :try_start_1
    invoke-static {p3}, Lscd;->f(Ljava/lang/Object;)Ljava/lang/Object;

    iget-object p3, p0, Lrgo;->a:Lrgp;

    iget-object p3, p3, Lrgp;->e:Lsal;

    .line 2
    invoke-static {p3}, Lscd;->f(Ljava/lang/Object;)Ljava/lang/Object;

    iget v0, p3, Lsal;->c:I

    const/4 v1, 0x2

    if-ne v0, v1, :cond_2

    .line 3
    invoke-virtual {p2}, Lorg/chromium/net/UrlResponseInfo;->getHttpStatusCode()I

    move-result v0

    const/16 v1, 0x133

    const/16 v2, 0x134

    if-eq v0, v1, :cond_1

    if-ne v0, v2, :cond_2

    const/16 v0, 0x134

    :cond_1
    iget-object p1, p0, Lrgo;->a:Lrgp;

    new-instance v1, Lsbe;

    .line 4
    invoke-virtual {p2}, Lorg/chromium/net/UrlResponseInfo;->getHttpStatusText()Ljava/lang/String;

    .line 5
    invoke-virtual {p2}, Lorg/chromium/net/UrlResponseInfo;->getAllHeaders()Ljava/util/Map;

    sget p2, Lsdt;->a:I

    invoke-direct {v1, v0, p3}, Lsbe;-><init>(ILsal;)V

    iput-object v1, p1, Lrgp;->g:Ljava/io/IOException;

    iget-object p1, p0, Lrgo;->a:Lrgp;

    iget-object p1, p1, Lrgp;->c:Lscj;

    .line 6
    invoke-virtual {p1}, Lscj;->a()Z
    :try_end_1
    .catchall {:try_start_1 .. :try_end_1} :catchall_0

    monitor-exit p0

    return-void

    :cond_2
    :try_start_2
    iget-object p2, p0, Lrgo;->a:Lrgp;

    iget-boolean p3, p2, Lrgp;->b:Z

    if-eqz p3, :cond_3

    .line 7
    invoke-virtual {p2}, Lrgp;->k()V

    .line 8
    :cond_3
    invoke-virtual {p1}, Lorg/chromium/net/UrlRequest;->followRedirect()V
    :try_end_2
    .catchall {:try_start_2 .. :try_end_2} :catchall_0

    monitor-exit p0

    return-void

    :catchall_0
    move-exception p1

    monitor-exit p0

    throw p1
.end method

.method public final declared-synchronized onResponseStarted(Lorg/chromium/net/UrlRequest;Lorg/chromium/net/UrlResponseInfo;)V
    .locals 2

    monitor-enter p0

    :try_start_0
    iget-object v0, p0, Lrgo;->a:Lrgp;

    iget-object v1, v0, Lrgp;->d:Lorg/chromium/net/UrlRequest;
    :try_end_0
    .catchall {:try_start_0 .. :try_end_0} :catchall_0

    if-eq p1, v1, :cond_0

    monitor-exit p0

    return-void

    :cond_0
    :try_start_1
    iput-object p2, v0, Lrgp;->f:Lorg/chromium/net/UrlResponseInfo;

    iget-object p1, v0, Lrgp;->c:Lscj;

    .line 1
    invoke-virtual {p1}, Lscj;->a()Z
    :try_end_1
    .catchall {:try_start_1 .. :try_end_1} :catchall_0

    monitor-exit p0

    return-void

    :catchall_0
    move-exception p1

    monitor-exit p0

    throw p1
.end method

.method public final declared-synchronized onSucceeded(Lorg/chromium/net/UrlRequest;Lorg/chromium/net/UrlResponseInfo;)V
    .locals 1

    monitor-enter p0

    :try_start_0
    iget-object p2, p0, Lrgo;->a:Lrgp;

    iget-object v0, p2, Lrgp;->d:Lorg/chromium/net/UrlRequest;
    :try_end_0
    .catchall {:try_start_0 .. :try_end_0} :catchall_0

    if-eq p1, v0, :cond_0

    monitor-exit p0

    return-void

    :cond_0
    const/4 p1, 0x1

    :try_start_1
    iput-boolean p1, p2, Lrgp;->h:Z

    iget-object p1, p2, Lrgp;->c:Lscj;

    .line 1
    invoke-virtual {p1}, Lscj;->a()Z
    :try_end_1
    .catchall {:try_start_1 .. :try_end_1} :catchall_0

    monitor-exit p0

    return-void

    :catchall_0
    move-exception p1

    monitor-exit p0

    throw p1
.end method
