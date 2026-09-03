.class public final Lbdfp;
.super Lorg/chromium/net/BidirectionalStream$Callback;
.source "PG"


# instance fields
.field final synthetic a:Lbdfs;

.field private b:Ljava/util/List;


# direct methods
.method public constructor <init>(Lbdfs;)V
    .locals 0

    iput-object p1, p0, Lbdfp;->a:Lbdfs;

    invoke-direct {p0}, Lorg/chromium/net/BidirectionalStream$Callback;-><init>()V

    return-void
.end method

.method private final a(Ljava/util/List;Z)V
    .locals 11

    new-instance v0, Ljava/util/ArrayList;

    .line 1
    invoke-direct {v0}, Ljava/util/ArrayList;-><init>()V

    .line 2
    invoke-interface {p1}, Ljava/util/List;->iterator()Ljava/util/Iterator;

    move-result-object p1

    :goto_0
    invoke-interface {p1}, Ljava/util/Iterator;->hasNext()Z

    move-result v1

    if-eqz v1, :cond_0

    invoke-interface {p1}, Ljava/util/Iterator;->next()Ljava/lang/Object;

    move-result-object v1

    check-cast v1, Ljava/util/Map$Entry;

    .line 3
    invoke-interface {v1}, Ljava/util/Map$Entry;->getKey()Ljava/lang/Object;

    move-result-object v2

    check-cast v2, Ljava/lang/String;

    invoke-interface {v0, v2}, Ljava/util/List;->add(Ljava/lang/Object;)Z

    .line 4
    invoke-interface {v1}, Ljava/util/Map$Entry;->getValue()Ljava/lang/Object;

    move-result-object v1

    check-cast v1, Ljava/lang/String;

    invoke-interface {v0, v1}, Ljava/util/List;->add(Ljava/lang/Object;)Z

    goto :goto_0

    .line 5
    :cond_0
    invoke-interface {v0}, Ljava/util/List;->size()I

    move-result p1

    new-array p1, p1, [[B

    const/4 v1, 0x0

    const/4 v2, 0x0

    .line 6
    :goto_1
    invoke-interface {v0}, Ljava/util/List;->size()I

    move-result v3

    if-ge v2, v3, :cond_1

    .line 7
    invoke-interface {v0, v2}, Ljava/util/List;->get(I)Ljava/lang/Object;

    move-result-object v3

    check-cast v3, Ljava/lang/String;

    const-string v4, "UTF-8"

    invoke-static {v4}, Ljava/nio/charset/Charset;->forName(Ljava/lang/String;)Ljava/nio/charset/Charset;

    move-result-object v4

    invoke-virtual {v3, v4}, Ljava/lang/String;->getBytes(Ljava/nio/charset/Charset;)[B

    move-result-object v3

    aput-object v3, p1, v2

    add-int/lit8 v3, v2, 0x1

    .line 8
    invoke-interface {v0, v3}, Ljava/util/List;->get(I)Ljava/lang/Object;

    move-result-object v4

    check-cast v4, Ljava/lang/String;

    const-string v5, "UTF-8"

    invoke-static {v5}, Ljava/nio/charset/Charset;->forName(Ljava/lang/String;)Ljava/nio/charset/Charset;

    move-result-object v5

    invoke-virtual {v4, v5}, Ljava/lang/String;->getBytes(Ljava/nio/charset/Charset;)[B

    move-result-object v4

    aput-object v4, p1, v3

    add-int/lit8 v2, v2, 0x2

    goto :goto_1

    .line 9
    :cond_1
    invoke-static {p1}, Lbdox;->b([[B)[[B

    move-result-object p1

    invoke-static {p1}, Lbddc;->b([[B)Lbdec;

    move-result-object p1

    iget-object v0, p0, Lbdfp;->a:Lbdfs;

    iget-object v0, v0, Lbdfs;->p:Lbdkf;

    .line 10
    sget-object v2, Lbdkf;->m:Lbddy;

    .line 11
    iget-object v0, v0, Lbdkf;->r:Ljava/lang/Object;

    .line 10
    monitor-enter v0

    :try_start_0
    iget-object v2, p0, Lbdfp;->a:Lbdfs;

    iget-object v2, v2, Lbdfs;->p:Lbdkf;

    const/4 v3, 0x1

    if-eqz p2, :cond_9

    iget-object p2, v2, Lbdkf;->n:Lio/grpc/Status;

    if-nez p2, :cond_2

    iget-boolean v4, v2, Lbdkf;->q:Z

    if-nez v4, :cond_2

    .line 33
    invoke-static {p1}, Lbdkf;->i(Lbdec;)Lio/grpc/Status;

    move-result-object p2

    iput-object p2, v2, Lbdkf;->n:Lio/grpc/Status;

    iget-object p2, v2, Lbdkf;->n:Lio/grpc/Status;

    if-eqz p2, :cond_2

    iput-object p1, v2, Lbdkf;->o:Lbdec;

    :cond_2
    if-eqz p2, :cond_3

    .line 45
    invoke-static {p1}, Ljava/lang/String;->valueOf(Ljava/lang/Object;)Ljava/lang/String;

    move-result-object p1

    invoke-static {p1}, Ljava/lang/String;->valueOf(Ljava/lang/Object;)Ljava/lang/String;

    move-result-object v3

    invoke-virtual {v3}, Ljava/lang/String;->length()I

    move-result v3

    add-int/lit8 v3, v3, 0xa

    new-instance v4, Ljava/lang/StringBuilder;

    invoke-direct {v4, v3}, Ljava/lang/StringBuilder;-><init>(I)V

    const-string v3, "trailers: "

    invoke-virtual {v4, v3}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v4, p1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v4}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p1

    invoke-virtual {p2, p1}, Lio/grpc/Status;->d(Ljava/lang/String;)Lio/grpc/Status;

    move-result-object p1

    iput-object p1, v2, Lbdkf;->n:Lio/grpc/Status;

    iget-object p1, v2, Lbdkf;->n:Lio/grpc/Status;

    iget-object p2, v2, Lbdkf;->o:Lbdec;

    .line 29
    invoke-virtual {v2, p1, v1, p2}, Lbdkf;->k(Lio/grpc/Status;ZLbdec;)V

    goto/16 :goto_9

    .line 34
    :cond_3
    sget-object p2, Lbddd;->b:Lbddy;

    invoke-virtual {p1, p2}, Lbdec;->f(Lbddy;)Ljava/lang/Object;

    move-result-object p2

    check-cast p2, Lio/grpc/Status;

    if-eqz p2, :cond_4

    sget-object v4, Lbddd;->a:Lbddy;

    .line 35
    invoke-virtual {p1, v4}, Lbdec;->f(Lbddy;)Ljava/lang/Object;

    move-result-object v4

    check-cast v4, Ljava/lang/String;

    invoke-virtual {p2, v4}, Lio/grpc/Status;->withDescription(Ljava/lang/String;)Lio/grpc/Status;

    move-result-object p2

    goto :goto_3

    .line 29
    :cond_4
    iget-boolean p2, v2, Lbdkf;->q:Z

    if-eqz p2, :cond_5

    .line 36
    sget-object p2, Lio/grpc/Status;->d:Lio/grpc/Status;

    const-string v4, "missing GRPC status in response"

    invoke-virtual {p2, v4}, Lio/grpc/Status;->withDescription(Ljava/lang/String;)Lio/grpc/Status;

    move-result-object p2

    goto :goto_3

    :cond_5
    sget-object p2, Lbdkf;->m:Lbddy;

    .line 37
    invoke-virtual {p1, p2}, Lbdec;->f(Lbddy;)Ljava/lang/Object;

    move-result-object p2

    check-cast p2, Ljava/lang/Integer;

    if-eqz p2, :cond_6

    .line 39
    invoke-virtual {p2}, Ljava/lang/Integer;->intValue()I

    move-result p2

    invoke-static {p2}, Lbdkc;->a(I)Lio/grpc/Status;

    move-result-object p2

    goto :goto_2

    .line 38
    :cond_6
    sget-object p2, Lio/grpc/Status;->j:Lio/grpc/Status;

    const-string v4, "missing HTTP status code"

    invoke-virtual {p2, v4}, Lio/grpc/Status;->withDescription(Ljava/lang/String;)Lio/grpc/Status;

    move-result-object p2

    :goto_2
    const-string v4, "missing GRPC status, inferred error from HTTP status code"

    .line 40
    invoke-virtual {p2, v4}, Lio/grpc/Status;->d(Ljava/lang/String;)Lio/grpc/Status;

    move-result-object p2

    .line 41
    :goto_3
    invoke-static {p1}, Lbdkf;->h(Lbdec;)V

    iget-boolean v4, v2, Lbdgd;->l:Z

    if-eqz v4, :cond_7

    sget-object v5, Lbdfz;->q:Ljava/util/logging/Logger;

    sget-object v6, Ljava/util/logging/Level;->INFO:Ljava/util/logging/Level;

    const/4 v2, 0x2

    new-array v10, v2, [Ljava/lang/Object;

    aput-object p2, v10, v1

    aput-object p1, v10, v3

    const-string v7, "io.grpc.internal.AbstractClientStream$TransportState"

    const-string v8, "inboundTrailersReceived"

    const-string v9, "Received trailers on closed stream:\n {1}\n {2}"

    .line 42
    invoke-virtual/range {v5 .. v10}, Ljava/util/logging/Logger;->logp(Ljava/util/logging/Level;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;[Ljava/lang/Object;)V

    goto/16 :goto_9

    :cond_7
    iget-object v3, v2, Lbdgd;->h:Lbdos;

    iget-object v3, v3, Lbdos;->c:[Lbdhm;

    array-length v4, v3

    const/4 v5, 0x0

    :goto_4
    if-ge v5, v4, :cond_8

    .line 43
    aget-object v6, v3, v5

    .line 44
    check-cast v6, Lbdbw;

    add-int/lit8 v5, v5, 0x1

    goto :goto_4

    .line 29
    :cond_8
    invoke-virtual {v2, p2, v1, p1}, Lbdgd;->d(Lio/grpc/Status;ZLbdec;)V

    goto/16 :goto_9

    .line 38
    :cond_9
    iget-object p2, v2, Lbdkf;->n:Lio/grpc/Status;

    if-eqz p2, :cond_a

    .line 12
    invoke-static {p1}, Ljava/lang/String;->valueOf(Ljava/lang/Object;)Ljava/lang/String;

    move-result-object p1

    invoke-static {p1}, Ljava/lang/String;->valueOf(Ljava/lang/Object;)Ljava/lang/String;

    move-result-object v1

    invoke-virtual {v1}, Ljava/lang/String;->length()I

    move-result v1

    add-int/lit8 v1, v1, 0x9

    new-instance v3, Ljava/lang/StringBuilder;

    invoke-direct {v3, v1}, Ljava/lang/StringBuilder;-><init>(I)V

    const-string v1, "headers: "

    invoke-virtual {v3, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v3, p1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v3}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p1

    invoke-virtual {p2, p1}, Lio/grpc/Status;->d(Ljava/lang/String;)Lio/grpc/Status;

    move-result-object p1

    iput-object p1, v2, Lbdkf;->n:Lio/grpc/Status;
    :try_end_0
    .catchall {:try_start_0 .. :try_end_0} :catchall_1

    goto/16 :goto_9

    :cond_a
    :try_start_1
    iget-boolean p2, v2, Lbdkf;->q:Z

    if-eqz p2, :cond_b

    .line 13
    sget-object p2, Lio/grpc/Status;->j:Lio/grpc/Status;

    const-string v1, "Received headers twice"

    invoke-virtual {p2, v1}, Lio/grpc/Status;->withDescription(Ljava/lang/String;)Lio/grpc/Status;

    move-result-object p2

    iput-object p2, v2, Lbdkf;->n:Lio/grpc/Status;
    :try_end_1
    .catchall {:try_start_1 .. :try_end_1} :catchall_0

    :try_start_2
    iget-object p2, v2, Lbdkf;->n:Lio/grpc/Status;

    if-eqz p2, :cond_12

    .line 14
    invoke-static {p1}, Ljava/lang/String;->valueOf(Ljava/lang/Object;)Ljava/lang/String;

    move-result-object v1

    invoke-static {v1}, Ljava/lang/String;->valueOf(Ljava/lang/Object;)Ljava/lang/String;

    move-result-object v3

    invoke-virtual {v3}, Ljava/lang/String;->length()I

    move-result v3

    add-int/lit8 v3, v3, 0x9

    new-instance v4, Ljava/lang/StringBuilder;

    invoke-direct {v4, v3}, Ljava/lang/StringBuilder;-><init>(I)V

    const-string v3, "headers: "

    invoke-virtual {v4, v3}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v4, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v4}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v1

    invoke-virtual {p2, v1}, Lio/grpc/Status;->d(Ljava/lang/String;)Lio/grpc/Status;

    move-result-object p2

    iput-object p2, v2, Lbdkf;->n:Lio/grpc/Status;

    iput-object p1, v2, Lbdkf;->o:Lbdec;

    .line 15
    invoke-static {p1}, Lbdkf;->g(Lbdec;)Ljava/nio/charset/Charset;

    move-result-object p1

    :goto_5
    iput-object p1, v2, Lbdkf;->p:Ljava/nio/charset/Charset;
    :try_end_2
    .catchall {:try_start_2 .. :try_end_2} :catchall_1

    goto/16 :goto_9

    :cond_b
    :try_start_3
    sget-object p2, Lbdkf;->m:Lbddy;

    .line 16
    invoke-virtual {p1, p2}, Lbdec;->f(Lbddy;)Ljava/lang/Object;

    move-result-object p2

    check-cast p2, Ljava/lang/Integer;

    if-eqz p2, :cond_c

    .line 17
    invoke-virtual {p2}, Ljava/lang/Integer;->intValue()I

    move-result v4

    const/16 v5, 0x64

    if-lt v4, v5, :cond_c

    invoke-virtual {p2}, Ljava/lang/Integer;->intValue()I

    move-result p2
    :try_end_3
    .catchall {:try_start_3 .. :try_end_3} :catchall_0

    const/16 v4, 0xc8

    if-ge p2, v4, :cond_c

    :try_start_4
    iget-object p2, v2, Lbdkf;->n:Lio/grpc/Status;

    if-eqz p2, :cond_12

    .line 14
    invoke-static {p1}, Ljava/lang/String;->valueOf(Ljava/lang/Object;)Ljava/lang/String;

    move-result-object v1

    invoke-static {v1}, Ljava/lang/String;->valueOf(Ljava/lang/Object;)Ljava/lang/String;

    move-result-object v3

    invoke-virtual {v3}, Ljava/lang/String;->length()I

    move-result v3

    add-int/lit8 v3, v3, 0x9

    new-instance v4, Ljava/lang/StringBuilder;

    invoke-direct {v4, v3}, Ljava/lang/StringBuilder;-><init>(I)V

    const-string v3, "headers: "

    invoke-virtual {v4, v3}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v4, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v4}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v1

    invoke-virtual {p2, v1}, Lio/grpc/Status;->d(Ljava/lang/String;)Lio/grpc/Status;

    move-result-object p2

    iput-object p2, v2, Lbdkf;->n:Lio/grpc/Status;

    iput-object p1, v2, Lbdkf;->o:Lbdec;

    .line 15
    invoke-static {p1}, Lbdkf;->g(Lbdec;)Ljava/nio/charset/Charset;

    move-result-object p1
    :try_end_4
    .catchall {:try_start_4 .. :try_end_4} :catchall_1

    goto :goto_5

    :cond_c
    :try_start_5
    iput-boolean v3, v2, Lbdkf;->q:Z

    .line 18
    invoke-static {p1}, Lbdkf;->i(Lbdec;)Lio/grpc/Status;

    move-result-object p2

    iput-object p2, v2, Lbdkf;->n:Lio/grpc/Status;

    iget-object p2, v2, Lbdkf;->n:Lio/grpc/Status;
    :try_end_5
    .catchall {:try_start_5 .. :try_end_5} :catchall_0

    if-eqz p2, :cond_d

    .line 14
    :try_start_6
    invoke-static {p1}, Ljava/lang/String;->valueOf(Ljava/lang/Object;)Ljava/lang/String;

    move-result-object v1

    invoke-static {v1}, Ljava/lang/String;->valueOf(Ljava/lang/Object;)Ljava/lang/String;

    move-result-object v3

    invoke-virtual {v3}, Ljava/lang/String;->length()I

    move-result v3

    add-int/lit8 v3, v3, 0x9

    new-instance v4, Ljava/lang/StringBuilder;

    invoke-direct {v4, v3}, Ljava/lang/StringBuilder;-><init>(I)V

    const-string v3, "headers: "

    invoke-virtual {v4, v3}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v4, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v4}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v1

    invoke-virtual {p2, v1}, Lio/grpc/Status;->d(Ljava/lang/String;)Lio/grpc/Status;

    move-result-object p2

    iput-object p2, v2, Lbdkf;->n:Lio/grpc/Status;

    iput-object p1, v2, Lbdkf;->o:Lbdec;

    .line 15
    invoke-static {p1}, Lbdkf;->g(Lbdec;)Ljava/nio/charset/Charset;

    move-result-object p1
    :try_end_6
    .catchall {:try_start_6 .. :try_end_6} :catchall_1

    goto/16 :goto_5

    .line 19
    :cond_d
    :try_start_7
    invoke-static {p1}, Lbdkf;->h(Lbdec;)V

    iget-boolean p2, v2, Lbdgd;->l:Z

    xor-int/2addr p2, v3

    const-string v4, "Received headers on closed stream"

    .line 20
    invoke-static {p2, v4}, Larmo;->n(ZLjava/lang/Object;)V

    iget-object p2, v2, Lbdgd;->h:Lbdos;

    iget-object p2, p2, Lbdos;->c:[Lbdhm;

    array-length v4, p2

    const/4 v5, 0x0

    :goto_6
    if-ge v5, v4, :cond_e

    .line 21
    aget-object v6, p2, v5

    .line 22
    check-cast v6, Lbdbw;

    add-int/lit8 v5, v5, 0x1

    goto :goto_6

    .line 23
    :cond_e
    sget-object p2, Lbdkc;->d:Lbddy;

    invoke-virtual {p1, p2}, Lbdec;->f(Lbddy;)Ljava/lang/Object;

    move-result-object p2

    check-cast p2, Ljava/lang/String;

    sget-object p2, Lbdkc;->b:Lbddy;

    .line 24
    invoke-virtual {p1, p2}, Lbdec;->f(Lbddy;)Ljava/lang/Object;

    move-result-object p2

    check-cast p2, Ljava/lang/String;

    if-eqz p2, :cond_11

    iget-object v4, v2, Lbdgd;->j:Lbdck;

    iget-object v4, v4, Lbdck;->c:Ljava/util/Map;

    .line 25
    invoke-interface {v4, p2}, Ljava/util/Map;->get(Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v4

    check-cast v4, Lbdcj;

    if-eqz v4, :cond_f

    iget-object v4, v4, Lbdcj;->a:Lbdci;

    goto :goto_7

    :cond_f
    const/4 v4, 0x0

    :goto_7
    if-nez v4, :cond_10

    .line 26
    sget-object v4, Lio/grpc/Status;->j:Lio/grpc/Status;

    new-array v3, v3, [Ljava/lang/Object;

    aput-object p2, v3, v1

    const-string p2, "Can\'t find decompressor for %s"

    .line 27
    invoke-static {p2, v3}, Ljava/lang/String;->format(Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String;

    move-result-object p2

    invoke-virtual {v4, p2}, Lio/grpc/Status;->withDescription(Ljava/lang/String;)Lio/grpc/Status;

    move-result-object p2

    .line 28
    invoke-virtual {p2}, Lio/grpc/Status;->f()Lbdfe;

    move-result-object p2

    .line 29
    invoke-virtual {v2, p2}, Lbdgd;->l(Ljava/lang/Throwable;)V

    goto :goto_8

    .line 15
    :cond_10
    sget-object p2, Lbdby;->a:Lbdbz;

    if-eq v4, p2, :cond_11

    iget-object p2, v2, Lbdgd;->a:Lbdhp;

    const-string v1, "Already set full stream decompressor"

    .line 30
    invoke-static {v3, v1}, Larmo;->n(ZLjava/lang/Object;)V

    check-cast p2, Lbdnc;

    iput-object v4, p2, Lbdnc;->c:Lbdci;

    :cond_11
    iget-object p2, v2, Lbdgd;->i:Lbdhd;

    .line 31
    invoke-interface {p2, p1}, Lbdhd;->b(Lbdec;)V
    :try_end_7
    .catchall {:try_start_7 .. :try_end_7} :catchall_0

    .line 29
    :goto_8
    :try_start_8
    iget-object p2, v2, Lbdkf;->n:Lio/grpc/Status;

    if-eqz p2, :cond_12

    .line 14
    invoke-static {p1}, Ljava/lang/String;->valueOf(Ljava/lang/Object;)Ljava/lang/String;

    move-result-object v1

    invoke-static {v1}, Ljava/lang/String;->valueOf(Ljava/lang/Object;)Ljava/lang/String;

    move-result-object v3

    invoke-virtual {v3}, Ljava/lang/String;->length()I

    move-result v3

    add-int/lit8 v3, v3, 0x9

    new-instance v4, Ljava/lang/StringBuilder;

    invoke-direct {v4, v3}, Ljava/lang/StringBuilder;-><init>(I)V

    const-string v3, "headers: "

    invoke-virtual {v4, v3}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v4, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v4}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v1

    invoke-virtual {p2, v1}, Lio/grpc/Status;->d(Ljava/lang/String;)Lio/grpc/Status;

    move-result-object p2

    iput-object p2, v2, Lbdkf;->n:Lio/grpc/Status;

    iput-object p1, v2, Lbdkf;->o:Lbdec;

    .line 15
    invoke-static {p1}, Lbdkf;->g(Lbdec;)Ljava/nio/charset/Charset;

    move-result-object p1

    goto/16 :goto_5

    .line 46
    :cond_12
    :goto_9
    monitor-exit v0

    return-void

    :catchall_0
    move-exception p2

    iget-object v1, v2, Lbdkf;->n:Lio/grpc/Status;

    if-eqz v1, :cond_13

    .line 14
    invoke-static {p1}, Ljava/lang/String;->valueOf(Ljava/lang/Object;)Ljava/lang/String;

    move-result-object v3

    invoke-static {v3}, Ljava/lang/String;->valueOf(Ljava/lang/Object;)Ljava/lang/String;

    move-result-object v4

    invoke-virtual {v4}, Ljava/lang/String;->length()I

    move-result v4

    add-int/lit8 v4, v4, 0x9

    .line 32
    new-instance v5, Ljava/lang/StringBuilder;

    .line 14
    invoke-direct {v5, v4}, Ljava/lang/StringBuilder;-><init>(I)V

    const-string v4, "headers: "

    invoke-virtual {v5, v4}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v5, v3}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v5}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v3

    invoke-virtual {v1, v3}, Lio/grpc/Status;->d(Ljava/lang/String;)Lio/grpc/Status;

    move-result-object v1

    iput-object v1, v2, Lbdkf;->n:Lio/grpc/Status;

    iput-object p1, v2, Lbdkf;->o:Lbdec;

    .line 15
    invoke-static {p1}, Lbdkf;->g(Lbdec;)Ljava/nio/charset/Charset;

    move-result-object p1

    iput-object p1, v2, Lbdkf;->p:Ljava/nio/charset/Charset;

    .line 32
    :cond_13
    throw p2

    :catchall_1
    move-exception p1

    .line 46
    monitor-exit v0
    :try_end_8
    .catchall {:try_start_8 .. :try_end_8} :catchall_1

    goto :goto_b

    :goto_a
    throw p1

    :goto_b
    goto :goto_a
.end method

.method private static final b(Lorg/chromium/net/UrlResponseInfo;)Lio/grpc/Status;
    .locals 0

    .line 1
    invoke-virtual {p0}, Lorg/chromium/net/UrlResponseInfo;->getHttpStatusCode()I

    move-result p0

    invoke-static {p0}, Lbdkc;->a(I)Lio/grpc/Status;

    move-result-object p0

    return-object p0
.end method


# virtual methods
.method public final onCanceled(Lorg/chromium/net/BidirectionalStream;Lorg/chromium/net/UrlResponseInfo;)V
    .locals 1

    iget-object p1, p0, Lbdfp;->a:Lbdfs;

    iget-object p1, p1, Lbdfs;->p:Lbdkf;

    .line 1
    sget-object v0, Lbdkf;->m:Lbddy;

    .line 2
    iget-object p1, p1, Lbdkf;->r:Ljava/lang/Object;

    .line 1
    monitor-enter p1

    :try_start_0
    iget-object v0, p0, Lbdfp;->a:Lbdfs;

    iget-object v0, v0, Lbdfs;->p:Lbdkf;

    .line 3
    iget-object v0, v0, Lbdkf;->w:Lio/grpc/Status;

    if-eqz v0, :cond_0

    goto :goto_0

    :cond_0
    if-eqz p2, :cond_1

    .line 5
    invoke-static {p2}, Lbdfp;->b(Lorg/chromium/net/UrlResponseInfo;)Lio/grpc/Status;

    move-result-object v0

    goto :goto_0

    .line 4
    :cond_1
    sget-object p2, Lio/grpc/Status;->c:Lio/grpc/Status;

    const-string v0, "stream cancelled without reason"

    invoke-virtual {p2, v0}, Lio/grpc/Status;->withDescription(Ljava/lang/String;)Lio/grpc/Status;

    move-result-object v0

    .line 6
    :goto_0
    monitor-exit p1
    :try_end_0
    .catchall {:try_start_0 .. :try_end_0} :catchall_0

    iget-object p1, p0, Lbdfp;->a:Lbdfs;

    .line 7
    invoke-virtual {p1, v0}, Lbdfs;->c(Lio/grpc/Status;)V

    return-void

    :catchall_0
    move-exception p2

    .line 6
    :try_start_1
    monitor-exit p1
    :try_end_1
    .catchall {:try_start_1 .. :try_end_1} :catchall_0

    throw p2
.end method

.method public final onFailed(Lorg/chromium/net/BidirectionalStream;Lorg/chromium/net/UrlResponseInfo;Lorg/chromium/net/CronetException;)V
    .locals 0

    iget-object p1, p0, Lbdfp;->a:Lbdfs;

    .line 1
    sget-object p2, Lio/grpc/Status;->k:Lio/grpc/Status;

    invoke-virtual {p2, p3}, Lio/grpc/Status;->c(Ljava/lang/Throwable;)Lio/grpc/Status;

    move-result-object p2

    .line 2
    invoke-virtual {p1, p2}, Lbdfs;->c(Lio/grpc/Status;)V

    return-void
.end method

.method public final onReadCompleted(Lorg/chromium/net/BidirectionalStream;Lorg/chromium/net/UrlResponseInfo;Ljava/nio/ByteBuffer;Z)V
    .locals 4

    # translyte diagnostic: real UrlRequest.Callback subclasses never fired
    # during 15 real minutes of Home+playback - checking the
    # BidirectionalStream.Callback path instead (HTTP/2 streaming),
    # wrongly assumed earlier to be live-chat-only without verifying.
    # Purely additive - does not touch p1/p2/p3/p4.
    const-string v0, "translyteDebug"

    new-instance v1, Ljava/lang/StringBuilder;

    invoke-direct {v1}, Ljava/lang/StringBuilder;-><init>()V

    const-string v2, "translyte: onReadCompleted class=Lbdfp; url="

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

    .line 1
    invoke-virtual {p3}, Ljava/nio/Buffer;->flip()Ljava/nio/Buffer;

    iget-object p1, p0, Lbdfp;->a:Lbdfs;

    iget-object p1, p1, Lbdfs;->p:Lbdkf;

    .line 2
    sget-object p2, Lbdkf;->m:Lbddy;

    .line 3
    iget-object p1, p1, Lbdkf;->r:Ljava/lang/Object;

    .line 2
    monitor-enter p1

    :try_start_0
    iget-object p2, p0, Lbdfp;->a:Lbdfs;

    iget-object p2, p2, Lbdfs;->p:Lbdkf;

    iput-boolean p4, p2, Lbdkf;->x:Z

    .line 4
    invoke-virtual {p3}, Ljava/nio/ByteBuffer;->remaining()I

    move-result p2

    if-eqz p2, :cond_4

    iget-object p2, p0, Lbdfp;->a:Lbdfs;

    iget-object p2, p2, Lbdfs;->p:Lbdkf;

    iget v0, p2, Lbdkf;->v:I

    .line 5
    invoke-virtual {p3}, Ljava/nio/ByteBuffer;->remaining()I

    move-result v1

    add-int/2addr v0, v1

    iput v0, p2, Lbdkf;->v:I

    .line 6
    invoke-static {p3}, Lbdnv;->a(Ljava/nio/ByteBuffer;)Lbdnr;

    move-result-object p3

    iget-object v0, p2, Lbdkf;->n:Lio/grpc/Status;

    const/4 v1, 0x0

    if-eqz v0, :cond_1

    const-string v2, "DATA-----------------------------\n"

    iget-object v3, p2, Lbdkf;->p:Ljava/nio/charset/Charset;

    .line 7
    invoke-static {p3, v3}, Lbdnv;->b(Lbdnr;Ljava/nio/charset/Charset;)Ljava/lang/String;

    move-result-object p3

    invoke-virtual {p3}, Ljava/lang/String;->length()I

    move-result v3

    if-eqz v3, :cond_0

    invoke-virtual {v2, p3}, Ljava/lang/String;->concat(Ljava/lang/String;)Ljava/lang/String;

    move-result-object p3

    goto :goto_0

    .line 19
    :cond_0
    new-instance p3, Ljava/lang/String;

    .line 7
    invoke-direct {p3, v2}, Ljava/lang/String;-><init>(Ljava/lang/String;)V

    .line 8
    :goto_0
    invoke-virtual {v0, p3}, Lio/grpc/Status;->d(Ljava/lang/String;)Lio/grpc/Status;

    move-result-object p3

    iput-object p3, p2, Lbdkf;->n:Lio/grpc/Status;

    iget-object p3, p2, Lbdkf;->n:Lio/grpc/Status;

    .line 9
    invoke-virtual {p3}, Lio/grpc/Status;->getDescription()Ljava/lang/String;

    move-result-object p3

    invoke-virtual {p3}, Ljava/lang/String;->length()I

    move-result p3

    const/16 v0, 0x3e8

    if-le p3, v0, :cond_4

    iget-object p3, p2, Lbdkf;->n:Lio/grpc/Status;

    iget-object v0, p2, Lbdkf;->o:Lbdec;

    .line 10
    :goto_1
    invoke-virtual {p2, p3, v1, v0}, Lbdkf;->k(Lio/grpc/Status;ZLbdec;)V

    goto :goto_2

    .line 7
    :cond_1
    iget-boolean v0, p2, Lbdkf;->q:Z

    if-nez v0, :cond_2

    .line 11
    sget-object p3, Lio/grpc/Status;->j:Lio/grpc/Status;

    const-string v0, "headers not received before payload"

    .line 12
    invoke-virtual {p3, v0}, Lio/grpc/Status;->withDescription(Ljava/lang/String;)Lio/grpc/Status;

    move-result-object p3

    new-instance v0, Lbdec;

    invoke-direct {v0}, Lbdec;-><init>()V
    :try_end_0
    .catchall {:try_start_0 .. :try_end_0} :catchall_3

    goto :goto_1

    .line 10
    :cond_2
    :try_start_1
    iget-boolean v0, p2, Lbdgd;->l:Z

    if-eqz v0, :cond_3

    sget-object p2, Lbdfz;->q:Ljava/util/logging/Logger;

    sget-object p3, Ljava/util/logging/Level;->INFO:Ljava/util/logging/Level;

    const-string v0, "io.grpc.internal.AbstractClientStream$TransportState"

    const-string v1, "inboundDataReceived"

    const-string v2, "Received data on closed stream"

    .line 13
    invoke-virtual {p2, p3, v0, v1, v2}, Ljava/util/logging/Logger;->logp(Ljava/util/logging/Level;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V
    :try_end_1
    .catchall {:try_start_1 .. :try_end_1} :catchall_2

    goto :goto_2

    :cond_3
    :try_start_2
    iget-object v0, p2, Lbdgd;->a:Lbdhp;
    :try_end_2
    .catchall {:try_start_2 .. :try_end_2} :catchall_1

    :try_start_3
    move-object v1, v0

    check-cast v1, Lbdnc;

    .line 14
    invoke-virtual {v1}, Lbdnc;->b()Z

    move-result v1

    if-nez v1, :cond_4

    move-object v1, v0

    check-cast v1, Lbdnc;

    iget-boolean v1, v1, Lbdnc;->e:Z

    if-nez v1, :cond_4

    move-object v1, v0

    check-cast v1, Lbdnc;

    iget-object v1, v1, Lbdnc;->d:Lbdhk;

    .line 15
    invoke-virtual {v1, p3}, Lbdhk;->b(Lbdnr;)V

    check-cast v0, Lbdnc;

    .line 14
    invoke-virtual {v0}, Lbdnc;->d()V
    :try_end_3
    .catchall {:try_start_3 .. :try_end_3} :catchall_0

    goto :goto_2

    :catchall_0
    move-exception p3

    .line 16
    :try_start_4
    throw p3
    :try_end_4
    .catchall {:try_start_4 .. :try_end_4} :catchall_1

    :catchall_1
    move-exception p3

    .line 10
    :try_start_5
    invoke-virtual {p2, p3}, Lbdgd;->l(Ljava/lang/Throwable;)V
    :try_end_5
    .catchall {:try_start_5 .. :try_end_5} :catchall_2

    goto :goto_2

    :catchall_2
    move-exception p2

    .line 17
    :try_start_6
    throw p2

    .line 18
    :cond_4
    :goto_2
    monitor-exit p1
    :try_end_6
    .catchall {:try_start_6 .. :try_end_6} :catchall_3

    if-eqz p4, :cond_5

    iget-object p1, p0, Lbdfp;->b:Ljava/util/List;

    if-eqz p1, :cond_5

    const/4 p2, 0x1

    .line 19
    invoke-direct {p0, p1, p2}, Lbdfp;->a(Ljava/util/List;Z)V

    :cond_5
    return-void

    :catchall_3
    move-exception p2

    .line 18
    :try_start_7
    monitor-exit p1
    :try_end_7
    .catchall {:try_start_7 .. :try_end_7} :catchall_3

    goto :goto_4

    :goto_3
    throw p2

    :goto_4
    goto :goto_3
.end method

.method public final onResponseHeadersReceived(Lorg/chromium/net/BidirectionalStream;Lorg/chromium/net/UrlResponseInfo;)V
    .locals 1

    .line 1
    invoke-virtual {p2}, Lorg/chromium/net/UrlResponseInfo;->getAllHeadersAsList()Ljava/util/List;

    move-result-object p2

    const/4 v0, 0x0

    invoke-direct {p0, p2, v0}, Lbdfp;->a(Ljava/util/List;Z)V

    const/16 p2, 0x1000

    .line 2
    invoke-static {p2}, Ljava/nio/ByteBuffer;->allocateDirect(I)Ljava/nio/ByteBuffer;

    move-result-object p2

    invoke-virtual {p1, p2}, Lorg/chromium/net/BidirectionalStream;->read(Ljava/nio/ByteBuffer;)V

    return-void
.end method

.method public final onResponseTrailersReceived(Lorg/chromium/net/BidirectionalStream;Lorg/chromium/net/UrlResponseInfo;Lorg/chromium/net/UrlResponseInfo$HeaderBlock;)V
    .locals 0

    .line 1
    invoke-virtual {p3}, Lorg/chromium/net/UrlResponseInfo$HeaderBlock;->getAsList()Ljava/util/List;

    move-result-object p1

    iput-object p1, p0, Lbdfp;->b:Ljava/util/List;

    iget-object p2, p0, Lbdfp;->a:Lbdfs;

    iget-object p2, p2, Lbdfs;->p:Lbdkf;

    .line 2
    sget-object p3, Lbdkf;->m:Lbddy;

    .line 3
    iget-object p2, p2, Lbdkf;->r:Ljava/lang/Object;

    .line 2
    monitor-enter p2

    :try_start_0
    iget-object p3, p0, Lbdfp;->a:Lbdfs;

    iget-object p3, p3, Lbdfs;->p:Lbdkf;

    .line 4
    iget-boolean p3, p3, Lbdkf;->x:Z

    .line 5
    monitor-exit p2
    :try_end_0
    .catchall {:try_start_0 .. :try_end_0} :catchall_0

    if-eqz p3, :cond_0

    const/4 p2, 0x1

    .line 6
    invoke-direct {p0, p1, p2}, Lbdfp;->a(Ljava/util/List;Z)V

    :cond_0
    return-void

    :catchall_0
    move-exception p1

    .line 5
    :try_start_1
    monitor-exit p2
    :try_end_1
    .catchall {:try_start_1 .. :try_end_1} :catchall_0

    throw p1
.end method

.method public final onStreamReady(Lorg/chromium/net/BidirectionalStream;)V
    .locals 6

    iget-object p1, p0, Lbdfp;->a:Lbdfs;

    iget-object p1, p1, Lbdfs;->p:Lbdkf;

    .line 1
    sget-object v0, Lbdkf;->m:Lbddy;

    .line 2
    iget-object p1, p1, Lbdkf;->r:Ljava/lang/Object;

    .line 1
    monitor-enter p1

    :try_start_0
    iget-object v0, p0, Lbdfp;->a:Lbdfs;

    iget-object v0, v0, Lbdfs;->p:Lbdkf;

    .line 3
    invoke-virtual {v0}, Lbdkf;->j()V

    iget-object v0, p0, Lbdfp;->a:Lbdfs;

    iget-object v0, v0, Lbdfs;->p:Lbdkf;

    const/4 v1, 0x1

    iput-boolean v1, v0, Lbdkf;->t:Z

    iget-object v1, v0, Lbdkf;->s:Ljava/util/Collection;

    .line 4
    invoke-interface {v1}, Ljava/util/Collection;->iterator()Ljava/util/Iterator;

    move-result-object v1

    :goto_0
    invoke-interface {v1}, Ljava/util/Iterator;->hasNext()Z

    move-result v2

    if-eqz v2, :cond_0

    invoke-interface {v1}, Ljava/util/Iterator;->next()Ljava/lang/Object;

    move-result-object v2

    check-cast v2, Lbdfq;

    iget-object v3, v0, Lbdkf;->z:Lbdfs;

    .line 5
    iget-object v4, v2, Lbdfq;->a:Ljava/nio/ByteBuffer;

    iget-boolean v5, v2, Lbdfq;->b:Z

    iget-boolean v2, v2, Lbdfq;->c:Z

    .line 6
    invoke-virtual {v3, v4, v5, v2}, Lbdfs;->b(Ljava/nio/ByteBuffer;ZZ)V

    goto :goto_0

    :cond_0
    iget-object v0, v0, Lbdkf;->s:Ljava/util/Collection;

    .line 7
    invoke-interface {v0}, Ljava/util/Collection;->clear()V

    .line 8
    monitor-exit p1

    return-void

    :catchall_0
    move-exception v0

    monitor-exit p1
    :try_end_0
    .catchall {:try_start_0 .. :try_end_0} :catchall_0

    goto :goto_2

    :goto_1
    throw v0

    :goto_2
    goto :goto_1
.end method

.method public final onSucceeded(Lorg/chromium/net/BidirectionalStream;Lorg/chromium/net/UrlResponseInfo;)V
    .locals 3

    iget-object p1, p0, Lbdfp;->a:Lbdfs;

    iget-object p1, p1, Lbdfs;->p:Lbdkf;

    .line 1
    sget-object v0, Lbdkf;->m:Lbddy;

    .line 2
    iget-object p1, p1, Lbdkf;->r:Ljava/lang/Object;

    .line 1
    monitor-enter p1

    :try_start_0
    iget-object v0, p0, Lbdfp;->b:Ljava/util/List;

    const/4 v1, 0x0

    const/4 v2, 0x1

    if-eqz v0, :cond_0

    iget-object v0, p0, Lbdfp;->a:Lbdfs;

    iget-object v0, v0, Lbdfs;->p:Lbdkf;

    .line 3
    iget-boolean v0, v0, Lbdkf;->x:Z

    if-eqz v0, :cond_0

    const/4 v1, 0x1

    .line 4
    :cond_0
    monitor-exit p1
    :try_end_0
    .catchall {:try_start_0 .. :try_end_0} :catchall_0

    if-nez v1, :cond_3

    iget-object p1, p0, Lbdfp;->b:Ljava/util/List;

    if-eqz p1, :cond_1

    .line 6
    invoke-direct {p0, p1, v2}, Lbdfp;->a(Ljava/util/List;Z)V

    goto :goto_0

    :cond_1
    if-eqz p2, :cond_2

    .line 7
    invoke-virtual {p2}, Lorg/chromium/net/UrlResponseInfo;->getAllHeadersAsList()Ljava/util/List;

    move-result-object p1

    invoke-direct {p0, p1, v2}, Lbdfp;->a(Ljava/util/List;Z)V

    goto :goto_0

    :cond_2
    new-instance p1, Ljava/lang/AssertionError;

    const-string p2, "No response header or trailer"

    .line 10
    invoke-direct {p1, p2}, Ljava/lang/AssertionError;-><init>(Ljava/lang/Object;)V

    throw p1

    .line 6
    :cond_3
    :goto_0
    iget-object p1, p0, Lbdfp;->a:Lbdfs;

    .line 8
    invoke-static {p2}, Lbdfp;->b(Lorg/chromium/net/UrlResponseInfo;)Lio/grpc/Status;

    move-result-object p2

    .line 9
    invoke-virtual {p1, p2}, Lbdfs;->c(Lio/grpc/Status;)V

    return-void

    :catchall_0
    move-exception p2

    .line 5
    :try_start_1
    monitor-exit p1
    :try_end_1
    .catchall {:try_start_1 .. :try_end_1} :catchall_0

    throw p2
.end method

.method public final onWriteCompleted(Lorg/chromium/net/BidirectionalStream;Lorg/chromium/net/UrlResponseInfo;Ljava/nio/ByteBuffer;Z)V
    .locals 4

    iget-object p1, p0, Lbdfp;->a:Lbdfs;

    iget-object p1, p1, Lbdfs;->p:Lbdkf;

    .line 1
    sget-object p2, Lbdkf;->m:Lbddy;

    .line 2
    iget-object p1, p1, Lbdkf;->r:Ljava/lang/Object;

    .line 1
    monitor-enter p1

    :try_start_0
    iget-object p2, p0, Lbdfp;->a:Lbdfs;

    iget-object p4, p2, Lbdfs;->p:Lbdkf;

    .line 3
    iget-boolean v0, p4, Lbdkf;->y:Z

    const/4 v1, 0x0

    const/4 v2, 0x1

    if-nez v0, :cond_0

    iput-boolean v2, p4, Lbdkf;->y:Z

    iget-object p2, p2, Lbdfs;->f:Lbdos;

    iget-object p2, p2, Lbdos;->c:[Lbdhm;

    array-length p4, p2

    const/4 v0, 0x0

    :goto_0
    if-ge v0, p4, :cond_0

    .line 4
    aget-object v3, p2, v0

    .line 5
    check-cast v3, Lbdbw;

    add-int/lit8 v0, v0, 0x1

    goto :goto_0

    :cond_0
    iget-object p2, p0, Lbdfp;->a:Lbdfs;

    iget-object p2, p2, Lbdfs;->p:Lbdkf;

    .line 6
    invoke-virtual {p3}, Ljava/nio/ByteBuffer;->position()I

    move-result p3

    iget-object p4, p2, Lbdgd;->b:Ljava/lang/Object;

    monitor-enter p4
    :try_end_0
    .catchall {:try_start_0 .. :try_end_0} :catchall_1

    :try_start_1
    iget-boolean v0, p2, Lbdgd;->f:Z

    const-string v3, "onStreamAllocated was not called, but it seems the stream is active"

    .line 7
    invoke-static {v0, v3}, Larmo;->n(ZLjava/lang/Object;)V

    iget v0, p2, Lbdgd;->e:I

    sub-int p3, v0, p3

    iput p3, p2, Lbdgd;->e:I

    const v3, 0x8000

    if-lt v0, v3, :cond_1

    if-ge p3, v3, :cond_1

    const/4 v1, 0x1

    .line 8
    :cond_1
    monitor-exit p4
    :try_end_1
    .catchall {:try_start_1 .. :try_end_1} :catchall_0

    if-eqz v1, :cond_2

    .line 6
    :try_start_2
    invoke-virtual {p2}, Lbdgd;->a()V

    .line 9
    :cond_2
    monitor-exit p1
    :try_end_2
    .catchall {:try_start_2 .. :try_end_2} :catchall_1

    return-void

    :catchall_0
    move-exception p2

    .line 8
    :try_start_3
    monitor-exit p4
    :try_end_3
    .catchall {:try_start_3 .. :try_end_3} :catchall_0

    :try_start_4
    throw p2

    :catchall_1
    move-exception p2

    .line 9
    monitor-exit p1
    :try_end_4
    .catchall {:try_start_4 .. :try_end_4} :catchall_1

    goto :goto_2

    :goto_1
    throw p2

    :goto_2
    goto :goto_1
.end method
