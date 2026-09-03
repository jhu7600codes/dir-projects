.class public final Lagke;
.super Ljava/lang/Object;
.source "PG"

# interfaces
.implements Lagki;


# instance fields
.field public final a:Lajuj;

.field public final b:Lagkj;

.field private final c:Lajwg;

.field private final d:Labyh;

.field private final e:Labuo;

.field private final f:Ljava/util/concurrent/Executor;


# direct methods
.method public constructor <init>(Lagkj;Lajuj;Lajwg;Labyh;Labuo;Ljava/util/concurrent/Executor;)V
    .locals 0

    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    iput-object p2, p0, Lagke;->a:Lajuj;

    iput-object p1, p0, Lagke;->b:Lagkj;

    iput-object p3, p0, Lagke;->c:Lajwg;

    iput-object p4, p0, Lagke;->d:Labyh;

    iput-object p5, p0, Lagke;->e:Labuo;

    iput-object p6, p0, Lagke;->f:Ljava/util/concurrent/Executor;

    return-void
.end method

.method public static g(Lawjf;JJLjava/lang/String;Ljava/lang/String;Z)Latdr;
    .locals 2

    .line 1
    invoke-virtual {p0}, Latdy;->toBuilder()Latdr;

    move-result-object v0

    check-cast v0, Lawjd;

    .line 2
    invoke-virtual {v0}, Latdr;->copyOnWrite()V

    iget-object v1, v0, Lawjd;->instance:Latdy;

    .line 3
    check-cast v1, Lawjf;

    invoke-static {v1, p1, p2}, Lawjf;->e(Lawjf;J)V

    .line 4
    invoke-virtual {p0}, Lawjf;->b()Lawjg;

    move-result-object p0

    invoke-virtual {p0}, Latdy;->toBuilder()Latdr;

    move-result-object p0

    invoke-virtual {p0}, Latdr;->copyOnWrite()V

    iget-object p1, p0, Latdr;->instance:Latdy;

    .line 5
    check-cast p1, Lawjg;

    iget p2, p1, Lawjg;->a:I

    or-int/lit8 p2, p2, 0x1

    iput p2, p1, Lawjg;->a:I

    iput-wide p3, p1, Lawjg;->b:J

    .line 6
    invoke-virtual {v0}, Latdr;->copyOnWrite()V

    iget-object p1, v0, Lawjd;->instance:Latdy;

    .line 7
    check-cast p1, Lawjf;

    invoke-virtual {p0}, Latdr;->build()Latdy;

    move-result-object p0

    check-cast p0, Lawjg;

    invoke-static {p1, p0}, Lawjf;->f(Lawjf;Lawjg;)V

    .line 8
    sget-object p0, Lqgj;->l:Lqgj;

    .line 9
    invoke-virtual {p0}, Latdy;->createBuilder()Latdr;

    move-result-object p0

    .line 10
    invoke-virtual {v0}, Latdr;->build()Latdy;

    move-result-object p1

    check-cast p1, Lawjf;

    invoke-virtual {p1}, Latbv;->toByteString()Latcs;

    move-result-object p1

    invoke-virtual {p0}, Latdr;->copyOnWrite()V

    iget-object p2, p0, Latdr;->instance:Latdy;

    .line 11
    check-cast p2, Lqgj;

    .line 12
    invoke-virtual {p1}, Ljava/lang/Object;->getClass()Ljava/lang/Class;

    iget p3, p2, Lqgj;->a:I

    or-int/lit8 p3, p3, 0x4

    iput p3, p2, Lqgj;->a:I

    iput-object p1, p2, Lqgj;->d:Latcs;

    .line 13
    invoke-virtual {p0}, Latdr;->copyOnWrite()V

    iget-object p1, p0, Latdr;->instance:Latdy;

    .line 14
    check-cast p1, Lqgj;

    .line 15
    iget p2, p1, Lqgj;->a:I

    or-int/lit8 p2, p2, 0x2

    iput p2, p1, Lqgj;->a:I

    const-string p2, "event_logging"

    iput-object p2, p1, Lqgj;->c:Ljava/lang/String;

    .line 16
    invoke-virtual {p0}, Latdr;->copyOnWrite()V

    iget-object p1, p0, Latdr;->instance:Latdy;

    .line 17
    check-cast p1, Lqgj;

    .line 18
    invoke-virtual {p5}, Ljava/lang/Object;->getClass()Ljava/lang/Class;

    iget p2, p1, Lqgj;->a:I

    or-int/lit8 p2, p2, 0x10

    iput p2, p1, Lqgj;->a:I

    iput-object p5, p1, Lqgj;->f:Ljava/lang/String;

    .line 19
    invoke-static {p6}, Landroid/text/TextUtils;->isEmpty(Ljava/lang/CharSequence;)Z

    move-result p1

    if-nez p1, :cond_0

    .line 20
    invoke-virtual {p0}, Latdr;->copyOnWrite()V

    iget-object p1, p0, Latdr;->instance:Latdy;

    .line 21
    check-cast p1, Lqgj;

    .line 22
    invoke-virtual {p6}, Ljava/lang/Object;->getClass()Ljava/lang/Class;

    iget p2, p1, Lqgj;->a:I

    or-int/lit16 p2, p2, 0x80

    iput p2, p1, Lqgj;->a:I

    iput-object p6, p1, Lqgj;->i:Ljava/lang/String;

    .line 23
    :cond_0
    invoke-virtual {p0}, Latdr;->copyOnWrite()V

    iget-object p1, p0, Latdr;->instance:Latdy;

    .line 24
    check-cast p1, Lqgj;

    iget p2, p1, Lqgj;->a:I

    or-int/lit16 p2, p2, 0x100

    iput p2, p1, Lqgj;->a:I

    iput-boolean p7, p1, Lqgj;->j:Z

    return-object p0
.end method

.method private final h(Lawjf;ZJLajwe;Lajus;)Z
    .locals 13

    move-object v11, p0

    move-object/from16 v0, p6

    iget-object v1, v11, Lagke;->b:Lagkj;

    iget-object v1, v1, Lagkj;->a:Lavni;

    iget-boolean v1, v1, Lavni;->b:Z

    const/4 v2, 0x0

    if-nez v1, :cond_0

    return v2

    :cond_0
    if-nez p1, :cond_1

    const-string v0, "Unspecified ClientEvent"

    .line 1
    invoke-direct {p0, v0}, Lagke;->i(Ljava/lang/String;)V

    return v2

    .line 2
    :cond_1
    invoke-virtual {p1}, Lawjf;->a()Lawje;

    move-result-object v3

    .line 3
    sget-object v1, Lawje;->fK:Lawje;

    if-ne v3, v1, :cond_2

    const-string v0, "ClientEvent does not have one and only one payload set."

    .line 4
    invoke-direct {p0, v0}, Lagke;->i(Ljava/lang/String;)V

    return v2

    :cond_2
    iget-object v1, v11, Lagke;->d:Labyh;

    .line 5
    invoke-interface {v1}, Labyh;->b()J

    move-result-wide v4

    iget-object v1, v11, Lagke;->b:Lagkj;

    iget-object v6, v1, Lagkj;->g:Ljava/util/Map;

    # translyte patch: Lagkj;->g (a GEL/analytics event-throttle timestamp
    # map) is null in this environment - real device confirmed NPE on
    # this exact Map.get() call, from both a background network thread
    # and NewVersionAvailableActivity.onCreate (both go through this one
    # shared method). Guard it the same way the code already handles the
    # lookup's *result* being null a few lines down - treat a null map
    # as "no prior-sent timestamp recorded" rather than crashing.
    .line 6
    if-eqz v6, :cond_translyte_null_gel_map

    invoke-interface {v6, v3}, Ljava/util/Map;->get(Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v6

    goto :cond_translyte_gel_map_done

    :cond_translyte_null_gel_map
    const/4 v6, 0x0

    :cond_translyte_gel_map_done
    check-cast v6, Ljava/lang/Long;

    iget-object v1, v1, Lagkj;->c:Ljava/util/Set;

    .line 7
    invoke-interface {v1, v3}, Ljava/util/Set;->contains(Ljava/lang/Object;)Z

    move-result v1

    if-nez v1, :cond_9

    if-eqz v6, :cond_3

    invoke-virtual {v6}, Ljava/lang/Long;->longValue()J

    move-result-wide v6

    cmp-long v1, v4, v6

    if-ltz v1, :cond_9

    :cond_3
    const-wide/16 v1, 0x0

    cmp-long v6, p3, v1

    if-gez v6, :cond_4

    goto :goto_0

    :cond_4
    move-wide/from16 v4, p3

    :goto_0
    iget-object v1, v11, Lagke;->e:Labuo;

    .line 8
    invoke-virtual {v1}, Labuo;->b()J

    move-result-wide v6

    if-nez p5, :cond_5

    iget-object v1, v11, Lagke;->c:Lajwg;

    .line 9
    invoke-interface {v1}, Lajwg;->d()Lajwe;

    move-result-object v1

    invoke-interface {v1}, Lajwe;->i()Ljava/lang/String;

    move-result-object v1

    goto :goto_1

    .line 18
    :cond_5
    invoke-interface/range {p5 .. p5}, Lajwe;->i()Ljava/lang/String;

    move-result-object v1

    :goto_1
    move-object v8, v1

    if-nez v0, :cond_6

    .line 9
    iget-object v1, v11, Lagke;->c:Lajwg;

    .line 10
    invoke-interface {v1}, Lajwg;->p()Ljava/lang/String;

    move-result-object v1

    goto :goto_2

    .line 18
    :cond_6
    iget-object v1, v0, Lajus;->a:Ljava/lang/String;

    :goto_2
    move-object v9, v1

    if-nez v0, :cond_7

    .line 10
    iget-object v0, v11, Lagke;->c:Lajwg;

    .line 11
    invoke-interface {v0}, Lajwg;->o()Z

    move-result v0

    goto :goto_3

    .line 18
    :cond_7
    iget-boolean v0, v0, Lajus;->b:Z

    :goto_3
    move v10, v0

    .line 12
    invoke-static {v3}, Ljava/lang/String;->valueOf(Ljava/lang/Object;)Ljava/lang/String;

    move-result-object v0

    invoke-static {v0}, Ljava/lang/String;->valueOf(Ljava/lang/Object;)Ljava/lang/String;

    move-result-object v1

    invoke-virtual {v1}, Ljava/lang/String;->length()I

    move-result v1

    new-instance v2, Ljava/lang/StringBuilder;

    add-int/lit8 v1, v1, 0x39

    invoke-direct {v2, v1}, Ljava/lang/StringBuilder;-><init>(I)V

    const-string v1, "Pass GEL payload to delayed event service. Payload type: "

    invoke-virtual {v2, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v2, v0}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v2}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v0

    .line 13
    invoke-static {}, Lajzw;->b()Lased;

    move-result-object v1

    new-instance v2, Lagkc;

    invoke-direct {v2, p0, v0}, Lagkc;-><init>(Lagke;Ljava/lang/String;)V

    .line 14
    invoke-static {v1, v2}, Labgc;->e(Lased;Labgb;)V

    if-eqz p2, :cond_8

    iget-object v0, v11, Lagke;->a:Lajuj;

    move-object v3, p1

    .line 15
    invoke-static/range {v3 .. v10}, Lagke;->g(Lawjf;JJLjava/lang/String;Ljava/lang/String;Z)Latdr;

    move-result-object v1

    .line 16
    invoke-interface {v0, v1}, Lajuj;->l(Latdr;)V

    goto :goto_4

    :cond_8
    new-instance v12, Lagkd;

    move-object v0, v12

    move-object v1, p0

    move-object v2, v3

    move-object v3, p1

    .line 17
    invoke-direct/range {v0 .. v10}, Lagkd;-><init>(Lagke;Lawje;Lawjf;JJLjava/lang/String;Ljava/lang/String;Z)V

    iget-object v0, v11, Lagke;->f:Ljava/util/concurrent/Executor;

    .line 18
    invoke-interface {v0, v12}, Ljava/util/concurrent/Executor;->execute(Ljava/lang/Runnable;)V

    :goto_4
    const/4 v0, 0x1

    return v0

    :cond_9
    return v2
.end method

.method private final i(Ljava/lang/String;)V
    .locals 4

    .line 1
    invoke-virtual {p0}, Ljava/lang/Object;->getClass()Ljava/lang/Class;

    move-result-object v0

    invoke-virtual {v0}, Ljava/lang/Class;->getCanonicalName()Ljava/lang/String;

    move-result-object v0

    invoke-static {v0}, Ljava/lang/String;->valueOf(Ljava/lang/Object;)Ljava/lang/String;

    move-result-object v1

    invoke-virtual {v1}, Ljava/lang/String;->length()I

    move-result v1

    invoke-virtual {p1}, Ljava/lang/String;->length()I

    move-result v2

    new-instance v3, Ljava/lang/StringBuilder;

    add-int/lit8 v1, v1, 0x21

    add-int/2addr v1, v2

    invoke-direct {v3, v1}, Ljava/lang/StringBuilder;-><init>(I)V

    invoke-virtual {v3, v0}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    const-string v0, " could not generate ClientEvent: "

    invoke-virtual {v3, v0}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v3, p1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v3}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p1

    const-string v0, "GEL_DELAYED_EVENT_DEBUG"

    .line 2
    invoke-static {v0, p1}, Labzs;->c(Ljava/lang/String;Ljava/lang/String;)V

    return-void
.end method


# virtual methods
.method public final a(Lawjf;)Z
    .locals 7

    const/4 v2, 0x0

    const-wide/16 v3, -0x1

    const/4 v5, 0x0

    const/4 v6, 0x0

    move-object v0, p0

    move-object v1, p1

    .line 1
    invoke-direct/range {v0 .. v6}, Lagke;->h(Lawjf;ZJLajwe;Lajus;)Z

    move-result p1

    return p1
.end method

.method public final b(Lawjf;J)V
    .locals 7

    const/4 v2, 0x0

    const/4 v5, 0x0

    const/4 v6, 0x0

    move-object v0, p0

    move-object v1, p1

    move-wide v3, p2

    .line 1
    invoke-direct/range {v0 .. v6}, Lagke;->h(Lawjf;ZJLajwe;Lajus;)Z

    return-void
.end method

.method public final c(Lawjf;Lajwe;)V
    .locals 7

    const/4 v2, 0x0

    const-wide/16 v3, -0x1

    const/4 v6, 0x0

    move-object v0, p0

    move-object v1, p1

    move-object v5, p2

    .line 1
    invoke-direct/range {v0 .. v6}, Lagke;->h(Lawjf;ZJLajwe;Lajus;)Z

    return-void
.end method

.method public final d(Lawjf;Lajwe;JLajus;)V
    .locals 7

    const/4 v2, 0x0

    move-object v0, p0

    move-object v1, p1

    move-wide v3, p3

    move-object v5, p2

    move-object v6, p5

    .line 1
    invoke-direct/range {v0 .. v6}, Lagke;->h(Lawjf;ZJLajwe;Lajus;)Z

    return-void
.end method

.method public final e(Lawjf;)V
    .locals 7

    const/4 v2, 0x1

    const-wide/16 v3, -0x1

    const/4 v5, 0x0

    const/4 v6, 0x0

    move-object v0, p0

    move-object v1, p1

    .line 1
    invoke-direct/range {v0 .. v6}, Lagke;->h(Lawjf;ZJLajwe;Lajus;)Z

    return-void
.end method

.method public final f(Lawjf;Lajwe;JLajus;)V
    .locals 7

    const/4 v2, 0x1

    move-object v0, p0

    move-object v1, p1

    move-wide v3, p3

    move-object v5, p2

    move-object v6, p5

    .line 1
    invoke-direct/range {v0 .. v6}, Lagke;->h(Lawjf;ZJLajwe;Lajus;)Z

    return-void
.end method
