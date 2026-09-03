.class final Labmu;
.super Lorg/chromium/net/UrlRequest$Callback;
.source "PG"


# instance fields
.field public final a:Labyh;

.field public final b:Labmt;

.field public final c:Labnw;

.field public volatile d:I

.field public e:J

.field private final f:Ljava/util/concurrent/Executor;

.field private final g:Labnz;

.field private final h:Lbqm;

.field private final i:Ljava/util/ArrayDeque;

.field private j:J

.field private k:I

.field private l:Z

.field private m:Z

.field private n:J

.field private final o:Lakgm;


# direct methods
.method public constructor <init>(Labyh;Lakgm;Ljava/util/concurrent/Executor;Labnz;Lbqm;Labmt;Labnw;[B[B)V
    .locals 2

    invoke-direct {p0}, Lorg/chromium/net/UrlRequest$Callback;-><init>()V

    new-instance p8, Ljava/util/ArrayDeque;

    const/4 p9, 0x2

    .line 1
    invoke-direct {p8, p9}, Ljava/util/ArrayDeque;-><init>(I)V

    iput-object p8, p0, Labmu;->i:Ljava/util/ArrayDeque;

    const/4 p8, 0x0

    iput p8, p0, Labmu;->d:I

    const-wide/16 v0, -0x1

    iput-wide v0, p0, Labmu;->j:J

    iput p8, p0, Labmu;->k:I

    iput-boolean p8, p0, Labmu;->l:Z

    iput-boolean p8, p0, Labmu;->m:Z

    iput-wide v0, p0, Labmu;->e:J

    iput-wide v0, p0, Labmu;->n:J

    iput-object p1, p0, Labmu;->a:Labyh;

    iput-object p2, p0, Labmu;->o:Lakgm;

    iput-object p3, p0, Labmu;->f:Ljava/util/concurrent/Executor;

    iput-object p4, p0, Labmu;->g:Labnz;

    iput-object p5, p0, Labmu;->h:Lbqm;

    iput-object p6, p0, Labmu;->b:Labmt;

    iput-object p7, p0, Labmu;->c:Labnw;

    return-void
.end method

.method private final a(Lorg/chromium/net/UrlResponseInfo;Lorg/chromium/net/CronetException;)V
    .locals 12

    const/4 v0, 0x0

    if-eqz p1, :cond_3

    if-eqz p2, :cond_0

    goto/16 :goto_3

    .line 1
    :cond_0
    iget-wide v1, p0, Labmu;->n:J

    iget-wide v3, p0, Labmu;->e:J

    sub-long v10, v1, v3

    .line 2
    invoke-virtual {p1}, Lorg/chromium/net/UrlResponseInfo;->getHttpStatusCode()I

    move-result p2

    const/16 v1, 0x130

    if-ne p2, v1, :cond_2

    .line 3
    invoke-static {}, Labkb;->c()Labjz;

    move-result-object p2

    iget-object v1, p0, Labmu;->h:Lbqm;

    if-eqz v1, :cond_1

    iget-object v1, v1, Lbqm;->g:Ljava/util/Map;

    .line 4
    invoke-interface {v1}, Ljava/util/Map;->entrySet()Ljava/util/Set;

    move-result-object v1

    invoke-virtual {p2, v1}, Labjz;->c(Ljava/util/Collection;)V

    iget-object v1, p0, Labmu;->h:Lbqm;

    .line 5
    iget-object v1, v1, Lbqm;->a:[B

    move-object v7, v1

    goto :goto_0

    :cond_1
    move-object v7, v0

    .line 6
    :goto_0
    invoke-virtual {p1}, Lorg/chromium/net/UrlResponseInfo;->getAllHeadersAsList()Ljava/util/List;

    move-result-object v1

    invoke-virtual {p2, v1}, Labjz;->c(Ljava/util/Collection;)V

    new-instance v1, Lbqr;

    const/16 v6, 0x130

    .line 7
    invoke-virtual {p2}, Labjz;->b()Labkb;

    move-result-object p2

    invoke-virtual {p2}, Labkb;->b()Ljava/util/Map;

    move-result-object v8

    const/4 v9, 0x1

    move-object v5, v1

    invoke-direct/range {v5 .. v11}, Lbqr;-><init>(I[BLjava/util/Map;ZJ)V

    goto :goto_1

    .line 8
    :cond_2
    invoke-static {}, Labkb;->c()Labjz;

    move-result-object p2

    invoke-virtual {p1}, Lorg/chromium/net/UrlResponseInfo;->getAllHeadersAsList()Ljava/util/List;

    move-result-object v1

    invoke-virtual {p2, v1}, Labjz;->c(Ljava/util/Collection;)V

    invoke-virtual {p2}, Labjz;->b()Labkb;

    move-result-object p2

    :try_start_0
    iget-object v1, p0, Labmu;->i:Ljava/util/ArrayDeque;

    .line 9
    invoke-static {v1}, Labkm;->i(Ljava/util/Collection;)Labkm;

    move-result-object v1

    invoke-virtual {v1}, Labkm;->f()[B

    move-result-object v7
    :try_end_0
    .catch Ljava/io/IOException; {:try_start_0 .. :try_end_0} :catch_0
    .catchall {:try_start_0 .. :try_end_0} :catchall_0

    # translyte diagnostic: dump the real raw /browse response body to the
    # app's own private data dir (readable via root) for real analysis -
    # answers whether the server reshapes wire format by version or
    # sends one schema to everyone. Purely additive - v7 still flows
    # unchanged into the real b(...) call below.
    #
    # This whole file (abmu.smali/Labmu) was relocated from smali/
    # (classes.dex) to this dex (smali_classes4) specifically to make
    # this patch possible - classes.dex is sitting exactly at some
    # 65536-entry table limit and could not accept even one new type or
    # method reference, breaking an unrelated pre-existing method
    # (Lzqg;->values(), then Lznc;->values() once the first attempt
    # shifted table ordering) as collateral on two separate attempts.
    # Android's multidex classloading doesn't care which physical dex a
    # class lives in, so moving Labmu here is safe.
    invoke-virtual {p1}, Lorg/chromium/net/UrlResponseInfo;->getUrl()Ljava/lang/String;

    move-result-object v3

    const-string v4, "/youtubei/v1/browse"

    invoke-virtual {v3, v4}, Ljava/lang/String;->contains(Ljava/lang/CharSequence;)Z

    move-result v3

    if-eqz v3, :cond_translyte_dump_skip

    :try_start_translyte_dump
    new-instance v3, Ljava/io/FileOutputStream;

    const-string v4, "/data/data/com.google.android.youtube/translyte_dump_browse.bin"

    invoke-direct {v3, v4}, Ljava/io/FileOutputStream;-><init>(Ljava/lang/String;)V

    invoke-virtual {v3, v7}, Ljava/io/FileOutputStream;->write([B)V

    invoke-virtual {v3}, Ljava/io/FileOutputStream;->close()V

    const-string v3, "translyteDebug"

    const-string v4, "translyte: dumped /browse response to translyte_dump_browse.bin"

    invoke-static {v3, v4}, Landroid/util/Log;->w(Ljava/lang/String;Ljava/lang/String;)I

    goto :goto_translyte_dump_done
    :try_end_translyte_dump
    .catch Ljava/lang/Exception; {:try_start_translyte_dump .. :try_end_translyte_dump} :catch_translyte_dump

    :catch_translyte_dump
    move-exception v3

    const-string v4, "translyteDebug"

    invoke-virtual {v3}, Ljava/lang/Exception;->toString()Ljava/lang/String;

    move-result-object v3

    invoke-static {v4, v3}, Landroid/util/Log;->w(Ljava/lang/String;Ljava/lang/String;)I

    :goto_translyte_dump_done
    :cond_translyte_dump_skip
    iget-object v1, p0, Labmu;->i:Ljava/util/ArrayDeque;

    .line 11
    invoke-virtual {v1}, Ljava/util/ArrayDeque;->clear()V

    new-instance v1, Lbqr;

    .line 13
    invoke-virtual {p1}, Lorg/chromium/net/UrlResponseInfo;->getHttpStatusCode()I

    move-result v6

    .line 14
    invoke-virtual {p2}, Labkb;->b()Ljava/util/Map;

    move-result-object v8

    const/4 v9, 0x0

    move-object v5, v1

    invoke-direct/range {v5 .. v11}, Lbqr;-><init>(I[BLjava/util/Map;ZJ)V

    .line 15
    :goto_1
    invoke-direct {p0, v1, p1, v0}, Labmu;->b(Lbqr;Lorg/chromium/net/UrlResponseInfo;Lorg/chromium/net/CronetException;)V

    return-void

    :catchall_0
    move-exception p1

    goto :goto_2

    :catch_0
    move-exception p1

    .line 12
    :try_start_1
    new-instance p2, Ljava/lang/RuntimeException;

    .line 10
    invoke-direct {p2, p1}, Ljava/lang/RuntimeException;-><init>(Ljava/lang/Throwable;)V

    throw p2
    :try_end_1
    .catchall {:try_start_1 .. :try_end_1} :catchall_0

    .line 14
    :goto_2
    iget-object p2, p0, Labmu;->i:Ljava/util/ArrayDeque;

    .line 11
    invoke-virtual {p2}, Ljava/util/ArrayDeque;->clear()V

    .line 12
    throw p1

    .line 1
    :cond_3
    :goto_3
    invoke-direct {p0, v0, v0, p2}, Labmu;->b(Lbqr;Lorg/chromium/net/UrlResponseInfo;Lorg/chromium/net/CronetException;)V

    return-void
.end method

.method private final b(Lbqr;Lorg/chromium/net/UrlResponseInfo;Lorg/chromium/net/CronetException;)V
    .locals 0

    new-instance p2, Labms;

    .line 1
    invoke-direct {p2, p0, p1, p3}, Labms;-><init>(Labmu;Lbqr;Lorg/chromium/net/CronetException;)V

    iget-object p1, p0, Labmu;->f:Ljava/util/concurrent/Executor;

    .line 2
    invoke-interface {p1, p2}, Ljava/util/concurrent/Executor;->execute(Ljava/lang/Runnable;)V

    return-void
.end method

.method private final c(J)I
    .locals 3

    const-wide/32 v0, 0x60000

    cmp-long v2, p1, v0

    if-lez v2, :cond_0

    const/high16 p1, 0x60000

    return p1

    :cond_0
    const-wide/16 v0, 0x100

    cmp-long v2, p1, v0

    if-gtz v2, :cond_2

    iget-boolean p1, p0, Labmu;->m:Z

    if-eqz p1, :cond_1

    iget-boolean p1, p0, Labmu;->l:Z

    if-nez p1, :cond_1

    const/4 p1, 0x1

    iput-boolean p1, p0, Labmu;->l:Z

    const/16 p1, 0x100

    return p1

    :cond_1
    const/16 p1, 0x2000

    return p1

    :cond_2
    long-to-int p2, p1

    return p2
.end method


# virtual methods
.method public final onCanceled(Lorg/chromium/net/UrlRequest;Lorg/chromium/net/UrlResponseInfo;)V
    .locals 1

    iget-object p1, p0, Labmu;->c:Labnw;

    .line 1
    invoke-interface {p1}, Labnw;->f()V

    iget-object p1, p0, Labmu;->a:Labyh;

    .line 2
    invoke-interface {p1}, Labyh;->c()J

    move-result-wide p1

    iput-wide p1, p0, Labmu;->n:J

    iget-object p1, p0, Labmu;->i:Ljava/util/ArrayDeque;

    .line 3
    invoke-virtual {p1}, Ljava/util/ArrayDeque;->clear()V

    iget p1, p0, Labmu;->d:I

    const/4 p2, 0x0

    if-eqz p1, :cond_2

    iget p1, p0, Labmu;->d:I

    const/4 v0, 0x1

    if-ne p1, v0, :cond_0

    new-instance p1, Labnv;

    const/4 v0, 0x6

    .line 4
    invoke-direct {p1, v0}, Labnv;-><init>(I)V

    goto :goto_0

    :cond_0
    const/4 v0, 0x2

    if-ne p1, v0, :cond_1

    .line 7
    new-instance p1, Labnv;

    const/4 v0, 0x4

    .line 5
    invoke-direct {p1, v0}, Labnv;-><init>(I)V

    goto :goto_0

    :cond_1
    new-instance p1, Labnv;

    const/16 v0, 0xb

    .line 6
    invoke-direct {p1, v0}, Labnv;-><init>(I)V

    .line 7
    :goto_0
    invoke-direct {p0, p2, p1}, Labmu;->a(Lorg/chromium/net/UrlResponseInfo;Lorg/chromium/net/CronetException;)V

    return-void

    .line 8
    :cond_2
    invoke-direct {p0, p2, p2}, Labmu;->a(Lorg/chromium/net/UrlResponseInfo;Lorg/chromium/net/CronetException;)V

    return-void
.end method

.method public final onFailed(Lorg/chromium/net/UrlRequest;Lorg/chromium/net/UrlResponseInfo;Lorg/chromium/net/CronetException;)V
    .locals 2

    iget-object v0, p0, Labmu;->c:Labnw;

    .line 1
    invoke-interface {v0}, Labnw;->e()V

    iget-object v0, p0, Labmu;->g:Labnz;

    iget-boolean v0, v0, Labnz;->h:Z

    if-eqz v0, :cond_0

    .line 2
    invoke-virtual {p0, p1, p2}, Lorg/chromium/net/UrlRequest$Callback;->onCanceled(Lorg/chromium/net/UrlRequest;Lorg/chromium/net/UrlResponseInfo;)V

    return-void

    :cond_0
    iget-object p1, p0, Labmu;->a:Labyh;

    .line 3
    invoke-interface {p1}, Labyh;->c()J

    move-result-wide v0

    iput-wide v0, p0, Labmu;->n:J

    .line 4
    invoke-direct {p0, p2, p3}, Labmu;->a(Lorg/chromium/net/UrlResponseInfo;Lorg/chromium/net/CronetException;)V

    return-void
.end method

.method public final onReadCompleted(Lorg/chromium/net/UrlRequest;Lorg/chromium/net/UrlResponseInfo;Ljava/nio/ByteBuffer;)V
    .locals 5

    iget-object p2, p0, Labmu;->c:Labnw;

    .line 1
    invoke-interface {p2}, Labnw;->c()V

    iget-object p2, p0, Labmu;->g:Labnz;

    iget-boolean p2, p2, Labnz;->h:Z

    if-eqz p2, :cond_0

    .line 2
    invoke-virtual {p1}, Lorg/chromium/net/UrlRequest;->cancel()V

    return-void

    .line 3
    :cond_0
    invoke-virtual {p3}, Ljava/nio/ByteBuffer;->position()I

    move-result p2

    iget v0, p0, Labmu;->k:I

    iget-wide v1, p0, Labmu;->j:J

    sub-int v0, p2, v0

    int-to-long v3, v0

    sub-long/2addr v1, v3

    iput-wide v1, p0, Labmu;->j:J

    iput p2, p0, Labmu;->k:I

    .line 4
    invoke-virtual {p3}, Ljava/nio/ByteBuffer;->hasRemaining()Z

    move-result p2

    if-eqz p2, :cond_1

    .line 5
    invoke-virtual {p1, p3}, Lorg/chromium/net/UrlRequest;->read(Ljava/nio/ByteBuffer;)V

    return-void

    .line 6
    :cond_1
    invoke-virtual {p3}, Ljava/nio/ByteBuffer;->flip()Ljava/nio/Buffer;

    iget-wide p2, p0, Labmu;->j:J

    .line 7
    invoke-direct {p0, p2, p3}, Labmu;->c(J)I

    move-result p2

    invoke-static {p2}, Ljava/nio/ByteBuffer;->allocateDirect(I)Ljava/nio/ByteBuffer;

    move-result-object p2

    const/4 p3, 0x0

    iput p3, p0, Labmu;->k:I

    iget-object p3, p0, Labmu;->i:Ljava/util/ArrayDeque;

    .line 8
    invoke-virtual {p3, p2}, Ljava/util/ArrayDeque;->add(Ljava/lang/Object;)Z

    .line 9
    invoke-virtual {p1, p2}, Lorg/chromium/net/UrlRequest;->read(Ljava/nio/ByteBuffer;)V

    return-void
.end method

.method public final onRedirectReceived(Lorg/chromium/net/UrlRequest;Lorg/chromium/net/UrlResponseInfo;Ljava/lang/String;)V
    .locals 0

    iget-object p2, p0, Labmu;->c:Labnw;

    .line 1
    invoke-interface {p2}, Labnw;->a()V

    iget-object p2, p0, Labmu;->o:Lakgm;

    if-eqz p2, :cond_0

    .line 2
    invoke-static {p3}, Lakgm;->e(Ljava/lang/String;)V

    .line 3
    :cond_0
    invoke-virtual {p1}, Lorg/chromium/net/UrlRequest;->followRedirect()V

    return-void
.end method

.method public final onResponseStarted(Lorg/chromium/net/UrlRequest;Lorg/chromium/net/UrlResponseInfo;)V
    .locals 8

    iget-object v0, p0, Labmu;->c:Labnw;

    .line 1
    invoke-interface {v0}, Labnw;->b()V

    iget-object v0, p0, Labmu;->b:Labmt;

    check-cast v0, Labnt;

    iget-object v0, v0, Labnt;->a:Labnz;

    const-class v1, Labkq;

    .line 2
    invoke-virtual {v0, v1}, Labnz;->j(Ljava/lang/Class;)Ljava/lang/Object;

    move-result-object v0

    check-cast v0, Labkq;

    if-eqz v0, :cond_0

    .line 3
    invoke-interface {v0}, Labkq;->b()V

    :cond_0
    iget-object v0, p0, Labmu;->g:Labnz;

    iget-boolean v0, v0, Labnz;->h:Z

    if-nez v0, :cond_a

    .line 4
    invoke-virtual {p2}, Lorg/chromium/net/UrlResponseInfo;->getAllHeaders()Ljava/util/Map;

    move-result-object p2

    const/4 v0, 0x0

    if-eqz p2, :cond_4

    const-string v1, "Content-Length"

    .line 5
    invoke-interface {p2, v1}, Ljava/util/Map;->containsKey(Ljava/lang/Object;)Z

    move-result v2

    const/4 v3, 0x0

    if-eqz v2, :cond_1

    .line 6
    invoke-interface {p2, v1}, Ljava/util/Map;->get(Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v1

    check-cast v1, Ljava/util/List;

    invoke-interface {v1, v3}, Ljava/util/List;->get(I)Ljava/lang/Object;

    move-result-object v1

    check-cast v1, Ljava/lang/String;

    goto :goto_0

    :cond_1
    move-object v1, v0

    :goto_0
    const-string v2, "Content-Encoding"

    .line 7
    invoke-interface {p2, v2}, Ljava/util/Map;->containsKey(Ljava/lang/Object;)Z

    move-result v4

    if-eqz v4, :cond_2

    .line 8
    invoke-interface {p2, v2}, Ljava/util/Map;->get(Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v2

    check-cast v2, Ljava/util/List;

    invoke-interface {v2, v3}, Ljava/util/List;->get(I)Ljava/lang/Object;

    move-result-object v2

    check-cast v2, Ljava/lang/String;

    goto :goto_1

    :cond_2
    move-object v2, v0

    :goto_1
    const-string v4, "Content-Type"

    .line 9
    invoke-interface {p2, v4}, Ljava/util/Map;->containsKey(Ljava/lang/Object;)Z

    move-result v5

    if-eqz v5, :cond_3

    .line 10
    invoke-interface {p2, v4}, Ljava/util/Map;->get(Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object p2

    check-cast p2, Ljava/util/List;

    invoke-interface {p2, v3}, Ljava/util/List;->get(I)Ljava/lang/Object;

    move-result-object p2

    move-object v0, p2

    check-cast v0, Ljava/lang/String;

    :cond_3
    move-object p2, v0

    move-object v0, v1

    goto :goto_2

    :cond_4
    move-object p2, v0

    move-object v2, p2

    .line 11
    :goto_2
    invoke-static {v0}, Landroid/text/TextUtils;->isEmpty(Ljava/lang/CharSequence;)Z

    move-result v1

    const-wide/high16 v3, -0x8000000000000000L

    if-nez v1, :cond_5

    .line 12
    :try_start_0
    invoke-static {v0}, Ljava/lang/Long;->parseLong(Ljava/lang/String;)J

    move-result-wide v0
    :try_end_0
    .catch Ljava/lang/NumberFormatException; {:try_start_0 .. :try_end_0} :catch_0

    goto :goto_3

    :catch_0
    :cond_5
    move-wide v0, v3

    :goto_3
    const-wide/16 v5, 0x0

    cmp-long v7, v0, v5

    if-gez v7, :cond_6

    goto :goto_6

    .line 13
    :cond_6
    invoke-static {v2}, Landroid/text/TextUtils;->isEmpty(Ljava/lang/CharSequence;)Z

    move-result v3

    if-nez v3, :cond_9

    const-string v3, "identity"

    invoke-virtual {v3, v2}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v2

    if-eqz v2, :cond_7

    goto :goto_4

    :cond_7
    const-string v2, "application/x-protobuf"

    .line 14
    invoke-virtual {v2, p2}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result p2

    if-eqz p2, :cond_8

    const-wide/16 v2, 0x3

    mul-long v0, v0, v2

    goto :goto_5

    :cond_8
    long-to-double v0, v0

    const-wide/high16 v2, 0x3ff8000000000000L    # 1.5

    invoke-static {v0, v1}, Ljava/lang/Double;->isNaN(D)Z

    mul-double v0, v0, v2

    double-to-long v0, v0

    goto :goto_5

    :cond_9
    :goto_4
    const/4 p2, 0x1

    .line 13
    iput-boolean p2, p0, Labmu;->m:Z

    :goto_5
    move-wide v3, v0

    .line 12
    :goto_6
    iput-wide v3, p0, Labmu;->j:J

    .line 15
    invoke-direct {p0, v3, v4}, Labmu;->c(J)I

    move-result p2

    invoke-static {p2}, Ljava/nio/ByteBuffer;->allocateDirect(I)Ljava/nio/ByteBuffer;

    move-result-object p2

    iget-object v0, p0, Labmu;->i:Ljava/util/ArrayDeque;

    .line 16
    invoke-virtual {v0, p2}, Ljava/util/ArrayDeque;->add(Ljava/lang/Object;)Z

    .line 17
    invoke-virtual {p1, p2}, Lorg/chromium/net/UrlRequest;->read(Ljava/nio/ByteBuffer;)V

    return-void

    .line 18
    :cond_a
    invoke-virtual {p1}, Lorg/chromium/net/UrlRequest;->cancel()V

    return-void
.end method

.method public final onSucceeded(Lorg/chromium/net/UrlRequest;Lorg/chromium/net/UrlResponseInfo;)V
    .locals 2

    iget-object v0, p0, Labmu;->c:Labnw;

    .line 1
    invoke-interface {v0}, Labnw;->d()V

    iget-object v0, p0, Labmu;->g:Labnz;

    iget-boolean v0, v0, Labnz;->h:Z

    if-eqz v0, :cond_0

    .line 2
    invoke-virtual {p0, p1, p2}, Lorg/chromium/net/UrlRequest$Callback;->onCanceled(Lorg/chromium/net/UrlRequest;Lorg/chromium/net/UrlResponseInfo;)V

    return-void

    :cond_0
    iget-object p1, p0, Labmu;->a:Labyh;

    .line 3
    invoke-interface {p1}, Labyh;->c()J

    move-result-wide v0

    iput-wide v0, p0, Labmu;->n:J

    iget-object p1, p0, Labmu;->i:Ljava/util/ArrayDeque;

    .line 4
    invoke-virtual {p1}, Ljava/util/ArrayDeque;->peekLast()Ljava/lang/Object;

    move-result-object p1

    check-cast p1, Ljava/nio/ByteBuffer;

    .line 5
    invoke-virtual {p1}, Ljava/nio/ByteBuffer;->flip()Ljava/nio/Buffer;

    .line 6
    invoke-virtual {p1}, Ljava/nio/ByteBuffer;->hasRemaining()Z

    move-result p1

    if-nez p1, :cond_1

    iget-object p1, p0, Labmu;->i:Ljava/util/ArrayDeque;

    .line 7
    invoke-virtual {p1}, Ljava/util/ArrayDeque;->removeLast()Ljava/lang/Object;

    :cond_1
    const/4 p1, 0x0

    .line 8
    invoke-direct {p0, p2, p1}, Labmu;->a(Lorg/chromium/net/UrlResponseInfo;Lorg/chromium/net/CronetException;)V

    return-void
.end method
