.class public final Lmjl;
.super Ljava/lang/Object;
.source "PG"

# interfaces
.implements Lmgs;
.implements Labsf;
.implements Labsg;
.implements Lfvs;
.implements Lfvp;
.implements Labhv;


# instance fields
.field private final A:Lkyc;

.field private final B:Lfuw;

.field private C:Larqe;

.field private final D:Ljava/util/Map;

.field private E:Z

.field private F:Lixd;

.field private final G:Ladac;

.field public final a:Laddm;

.field public final b:Lfvt;

.field public final c:Leqa;

.field public final d:Liqr;

.field public final e:Ljava/util/Map;

.field public final f:Ljava/util/Set;

.field public final g:Lbdqn;

.field public final h:Labiu;

.field public final i:Lbdqw;

.field public final j:Lekn;

.field public final k:Lmkg;

.field public l:Larpz;

.field public m:Lcom/google/android/apps/youtube/app/ui/pivotbar/PivotTabsBar;

.field public n:I

.field public final o:Ladbp;

.field private final p:Lapbe;

.field private final q:Lakde;

.field private final r:Llxp;

.field private final s:Ladbt;

.field private final t:Lbeou;

.field private final u:Laovo;

.field private final v:Laddd;

.field private final w:Limt;

.field private final x:Lijh;

.field private final y:Ljta;

.field private final z:Ljiy;


# direct methods
.method public constructor <init>(Landroid/content/res/Resources;Lapbe;Laddm;Llxp;Lakde;Lfvt;Ladbt;Leqa;Lbeou;Laovo;Laddd;Lbdqn;Liqr;Labiu;Lbdqw;Ladbp;Limt;Lijh;Ljta;Ljiy;Lkyc;Lfuw;Lekn;Lmkg;Ladac;)V
    .locals 8

    move-object v0, p0

    move-object v1, p5

    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    move-object v2, p2

    iput-object v2, v0, Lmjl;->p:Lapbe;

    move-object v2, p3

    iput-object v2, v0, Lmjl;->a:Laddm;

    iput-object v1, v0, Lmjl;->q:Lakde;

    move-object v2, p4

    iput-object v2, v0, Lmjl;->r:Llxp;

    move-object v2, p6

    iput-object v2, v0, Lmjl;->b:Lfvt;

    move-object v2, p7

    iput-object v2, v0, Lmjl;->s:Ladbt;

    move-object/from16 v2, p8

    iput-object v2, v0, Lmjl;->c:Leqa;

    move-object/from16 v2, p9

    iput-object v2, v0, Lmjl;->t:Lbeou;

    move-object/from16 v2, p10

    iput-object v2, v0, Lmjl;->u:Laovo;

    move-object/from16 v2, p11

    iput-object v2, v0, Lmjl;->v:Laddd;

    .line 1
    invoke-virtual {p1}, Landroid/content/res/Resources;->getConfiguration()Landroid/content/res/Configuration;

    move-result-object v2

    iget v2, v2, Landroid/content/res/Configuration;->smallestScreenWidthDp:I

    iput v2, v0, Lmjl;->n:I

    move-object/from16 v2, p12

    iput-object v2, v0, Lmjl;->g:Lbdqn;

    move-object/from16 v2, p13

    iput-object v2, v0, Lmjl;->d:Liqr;

    move-object/from16 v3, p14

    iput-object v3, v0, Lmjl;->h:Labiu;

    move-object/from16 v3, p15

    iput-object v3, v0, Lmjl;->i:Lbdqw;

    move-object/from16 v3, p16

    iput-object v3, v0, Lmjl;->o:Ladbp;

    move-object/from16 v4, p17

    iput-object v4, v0, Lmjl;->w:Limt;

    move-object/from16 v4, p18

    iput-object v4, v0, Lmjl;->x:Lijh;

    move-object/from16 v4, p19

    iput-object v4, v0, Lmjl;->y:Ljta;

    move-object/from16 v4, p20

    iput-object v4, v0, Lmjl;->z:Ljiy;

    move-object/from16 v4, p21

    iput-object v4, v0, Lmjl;->A:Lkyc;

    move-object/from16 v4, p22

    iput-object v4, v0, Lmjl;->B:Lfuw;

    move-object/from16 v4, p23

    iput-object v4, v0, Lmjl;->j:Lekn;

    move-object/from16 v4, p24

    iput-object v4, v0, Lmjl;->k:Lmkg;

    move-object/from16 v4, p25

    iput-object v4, v0, Lmjl;->G:Ladac;

    .line 2
    invoke-static {}, Larpz;->j()Larpz;

    move-result-object v5

    iput-object v5, v0, Lmjl;->l:Larpz;

    new-instance v5, Ljava/util/WeakHashMap;

    .line 3
    invoke-direct {v5}, Ljava/util/WeakHashMap;-><init>()V

    invoke-static {v5}, Ljava/util/Collections;->newSetFromMap(Ljava/util/Map;)Ljava/util/Set;

    move-result-object v5

    iput-object v5, v0, Lmjl;->f:Ljava/util/Set;

    sget-object v5, Larst;->b:Larqe;

    iput-object v5, v0, Lmjl;->C:Larqe;

    new-instance v5, Ljava/util/LinkedHashMap;

    .line 4
    invoke-direct {v5}, Ljava/util/LinkedHashMap;-><init>()V

    iput-object v5, v0, Lmjl;->D:Ljava/util/Map;

    new-instance v5, Ljava/util/HashMap;

    .line 5
    invoke-direct {v5}, Ljava/util/HashMap;-><init>()V

    iput-object v5, v0, Lmjl;->e:Ljava/util/Map;

    const/16 v6, 0x10

    .line 6
    invoke-static {v6}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    move-result-object v6

    const-string v7, "com.google.android.apps.youtube.app.endpoint.flags"

    invoke-interface {v5, v7, v6}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    .line 7
    invoke-virtual/range {p16 .. p16}, Ladbp;->b()Lauqj;

    move-result-object v3

    iget-object v3, v3, Lauqj;->q:Lbaho;

    if-nez v3, :cond_0

    .line 8
    sget-object v3, Lbaho;->o:Lbaho;

    :cond_0
    iget-object v3, v3, Lbaho;->g:Latwg;

    if-nez v3, :cond_1

    .line 9
    sget-object v3, Latwg;->j:Latwg;

    :cond_1
    iget v3, v3, Latwg;->i:I

    invoke-static {v3}, Latwi;->a(I)I

    move-result v3

    if-nez v3, :cond_2

    goto :goto_0

    :cond_2
    const/4 v5, 0x1

    if-eq v3, v5, :cond_3

    .line 11
    invoke-virtual/range {p25 .. p25}, Ladac;->a()Lbdqx;

    move-result-object v2

    new-instance v3, Lmjh;

    invoke-direct {v3, p0}, Lmjh;-><init>(Lmjl;)V

    .line 12
    invoke-virtual {v2, v3}, Lbdqx;->y(Lbdsj;)Lbdpw;

    move-result-object v2

    .line 13
    invoke-virtual {v2}, Lbdpw;->I()Lbdrk;

    goto :goto_1

    .line 10
    :cond_3
    :goto_0
    invoke-interface/range {p13 .. p13}, Liqr;->b()Lbdqh;

    move-result-object v2

    invoke-virtual {v2}, Lbdqh;->t()Lbdpw;

    move-result-object v2

    invoke-virtual {v2}, Lbdpw;->I()Lbdrk;

    :goto_1
    const-wide/16 v2, 0x0

    .line 14
    invoke-virtual {p5, p0, v2, v3}, Lakde;->e(Lakdd;J)V

    return-void
.end method

.method private final A(I)I
    .locals 3

    iget-object v0, p0, Lmjl;->D:Ljava/util/Map;

    .line 1
    invoke-interface {v0}, Ljava/util/Map;->entrySet()Ljava/util/Set;

    move-result-object v0

    invoke-interface {v0}, Ljava/util/Set;->iterator()Ljava/util/Iterator;

    move-result-object v0

    :cond_0
    invoke-interface {v0}, Ljava/util/Iterator;->hasNext()Z

    move-result v1

    if-eqz v1, :cond_1

    invoke-interface {v0}, Ljava/util/Iterator;->next()Ljava/lang/Object;

    move-result-object v1

    check-cast v1, Ljava/util/Map$Entry;

    .line 2
    invoke-interface {v1}, Ljava/util/Map$Entry;->getValue()Ljava/lang/Object;

    move-result-object v2

    check-cast v2, Ljava/lang/Integer;

    invoke-virtual {v2}, Ljava/lang/Integer;->intValue()I

    move-result v2

    if-ne p1, v2, :cond_0

    .line 3
    invoke-interface {v1}, Ljava/util/Map$Entry;->getKey()Ljava/lang/Object;

    move-result-object p1

    check-cast p1, Ljava/lang/Integer;

    invoke-virtual {p1}, Ljava/lang/Integer;->intValue()I

    move-result p1

    :cond_1
    return p1
.end method

.method private final B(I)I
    .locals 1

    iget-object v0, p0, Lmjl;->D:Ljava/util/Map;

    .line 1
    invoke-static {p1}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    move-result-object p1

    invoke-interface {v0, p1}, Ljava/util/Map;->get(Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object p1

    check-cast p1, Ljava/lang/Integer;

    invoke-virtual {p1}, Ljava/lang/Integer;->intValue()I

    move-result p1

    return p1
.end method

.method private final u(Ljava/lang/Runnable;)V
    .locals 3

    iget-object v0, p0, Lmjl;->d:Liqr;

    check-cast v0, Lirp;

    const/4 v1, 0x1

    .line 1
    invoke-virtual {v0, v1}, Lirp;->c(Z)Lbdqh;

    move-result-object v0

    iget-object v1, p0, Lmjl;->h:Labiu;

    .line 2
    invoke-virtual {v1}, Labiu;->b()Lbdpw;

    move-result-object v1

    new-instance v2, Labeu;

    .line 3
    invoke-direct {v2, v1}, Labeu;-><init>(Lbdpw;)V

    .line 2
    invoke-virtual {v0, v2}, Lbdqh;->h(Lbdql;)Lbdqh;

    move-result-object v0

    new-instance v1, Lmjg;

    invoke-direct {v1, p0, p1}, Lmjg;-><init>(Lmjl;Ljava/lang/Runnable;)V

    .line 4
    invoke-virtual {v0, v1}, Lbdqh;->D(Lbdsh;)Lbdrk;

    return-void
.end method

.method private final v(Z)V
    .locals 1

    if-eqz p1, :cond_0

    iget-object p1, p0, Lmjl;->m:Lcom/google/android/apps/youtube/app/ui/pivotbar/PivotTabsBar;

    const/4 v0, 0x0

    .line 1
    invoke-virtual {p1, v0}, Lcom/google/android/apps/youtube/app/ui/pivotbar/PivotTabsBar;->setTranslationY(F)V

    :cond_0
    iget-object p1, p0, Lmjl;->m:Lcom/google/android/apps/youtube/app/ui/pivotbar/PivotTabsBar;

    .line 2
    invoke-virtual {p1}, Labsh;->k()I

    move-result p1

    if-lez p1, :cond_1

    iget-object p1, p0, Lmjl;->c:Leqa;

    .line 3
    invoke-interface {p1}, Leqa;->i()Leqs;

    move-result-object p1

    invoke-direct {p0, p1}, Lmjl;->x(Leqs;)Z

    move-result p1

    if-eqz p1, :cond_1

    iget-object p1, p0, Lmjl;->m:Lcom/google/android/apps/youtube/app/ui/pivotbar/PivotTabsBar;

    const/4 v0, 0x0

    .line 5
    invoke-virtual {p1, v0}, Lcom/google/android/apps/youtube/app/ui/pivotbar/PivotTabsBar;->setVisibility(I)V

    return-void

    .line 4
    :cond_1
    invoke-virtual {p0}, Lmjl;->o()V

    return-void
.end method

.method private final w(Layvi;)Larls;
    .locals 2

    iget-object v0, p1, Layvi;->k:Lazoc;

    if-nez v0, :cond_0

    .line 1
    sget-object v0, Lazoc;->a:Lazoc;

    .line 2
    :cond_0
    sget-object v1, Lcom/google/protos/youtube/api/innertube/UploadProgressArrowRendererOuterClass$UploadProgressArrowRenderer;->uploadProgressArrowRenderer:Latdw;

    .line 3
    invoke-virtual {v0, v1}, Latdu;->b(Latdg;)Z

    move-result v0

    if-eqz v0, :cond_2

    iget-object p1, p1, Layvi;->k:Lazoc;

    if-nez p1, :cond_1

    sget-object p1, Lazoc;->a:Lazoc;

    :cond_1
    sget-object v0, Lcom/google/protos/youtube/api/innertube/UploadProgressArrowRendererOuterClass$UploadProgressArrowRenderer;->uploadProgressArrowRenderer:Latdw;

    .line 4
    invoke-virtual {p1, v0}, Latdu;->c(Latdg;)Ljava/lang/Object;

    move-result-object p1

    check-cast p1, Lcom/google/protos/youtube/api/innertube/UploadProgressArrowRendererOuterClass$UploadProgressArrowRenderer;

    .line 5
    invoke-static {p1}, Larls;->i(Ljava/lang/Object;)Larls;

    move-result-object p1

    goto :goto_0

    .line 7
    :cond_2
    sget-object p1, Larkq;->a:Larkq;

    .line 5
    :goto_0
    new-instance v0, Lmjb;

    .line 6
    invoke-direct {v0, p0}, Lmjb;-><init>(Lmjl;)V

    .line 7
    invoke-virtual {p1, v0}, Larls;->h(Larli;)Larls;

    move-result-object p1

    return-object p1
.end method

.method private final x(Leqs;)Z
    .locals 5

    iget-object v0, p0, Lmjl;->F:Lixd;

    iget-object v1, p0, Lmjl;->o:Ladbp;

    .line 1
    invoke-virtual {v1}, Ladbp;->b()Lauqj;

    move-result-object v1

    iget-object v1, v1, Lauqj;->k:Laxwe;

    if-nez v1, :cond_0

    .line 2
    sget-object v1, Laxwe;->T:Laxwe;

    :cond_0
    iget-boolean v1, v1, Laxwe;->k:Z

    .line 3
    invoke-virtual {p1}, Leqs;->a()Z

    move-result v2

    const/4 v3, 0x1

    const/4 v4, 0x0

    if-nez v2, :cond_2

    .line 4
    invoke-virtual {p1}, Leqs;->c()Z

    move-result p1

    if-nez p1, :cond_2

    if-nez v1, :cond_3

    if-eqz v0, :cond_3

    .line 5
    invoke-interface {v0}, Lixd;->a()Z

    move-result p1

    if-eqz p1, :cond_1

    goto :goto_0

    :cond_1
    return v3

    :cond_2
    :goto_0
    const/4 v3, 0x0

    :cond_3
    return v3
.end method

.method private final y(Lfvh;)Z
    .locals 1

    if-eqz p1, :cond_1

    iget-object v0, p0, Lmjl;->B:Lfuw;

    .line 1
    invoke-virtual {v0, p1}, Lfuw;->a(Lfvh;)Z

    move-result p1

    if-eqz p1, :cond_0

    goto :goto_0

    :cond_0
    const/4 p1, 0x0

    return p1

    :cond_1
    :goto_0
    const/4 p1, 0x1

    return p1
.end method

.method private final z(I)Z
    .locals 3

    iget-object v0, p0, Lmjl;->v:Laddd;

    iget-object v1, p0, Lmjl;->b:Lfvt;

    .line 1
    invoke-interface {v1}, Lfvt;->a()Lfvh;

    move-result-object v1

    if-eqz v1, :cond_0

    .line 2
    invoke-virtual {v1}, Lfvh;->d()Lauqq;

    move-result-object v1

    goto :goto_0

    :cond_0
    const/4 v1, 0x0

    :goto_0
    iget-object v2, p0, Lmjl;->l:Larpz;

    .line 3
    invoke-virtual {v2, p1}, Larpz;->get(I)Ljava/lang/Object;

    move-result-object p1

    check-cast p1, Lmhm;

    iget-object p1, p1, Lmhm;->c:Larls;

    sget-object v2, Lauqq;->e:Lauqq;

    invoke-virtual {p1, v2}, Larls;->c(Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object p1

    check-cast p1, Lauqq;

    .line 4
    invoke-virtual {v0, v1, p1}, Laddd;->a(Lauqq;Lauqq;)Z

    move-result p1

    return p1
.end method


# virtual methods
.method public final a(II)V
    .locals 2

    iget-object p1, p0, Lmjl;->b:Lfvt;

    .line 1
    invoke-interface {p1}, Lfvt;->a()Lfvh;

    move-result-object p1

    const/4 v0, 0x0

    const/4 v1, 0x1

    if-ne p2, v1, :cond_0

    .line 2
    invoke-direct {p0, p1}, Lmjl;->y(Lfvh;)Z

    move-result p2

    if-eqz p2, :cond_0

    const/4 v0, 0x1

    .line 3
    :cond_0
    invoke-virtual {p0, p1, v0}, Lmjl;->q(Lfvh;Z)V

    return-void
.end method

.method public final aI(Lfvu;)V
    .locals 1

    iget-object p1, p0, Lmjl;->b:Lfvt;

    .line 1
    invoke-interface {p1}, Lfvt;->a()Lfvh;

    move-result-object p1

    const/4 v0, 0x0

    .line 2
    invoke-virtual {p0, p1, v0}, Lmjl;->q(Lfvh;Z)V

    return-void
.end method

.method public final b(Lixd;)V
    .locals 0

    iput-object p1, p0, Lmjl;->F:Lixd;

    return-void
.end method

.method public final d()Z
    .locals 3

    iget-object v0, p0, Lmjl;->l:Larpz;

    iget-object v1, p0, Lmjl;->m:Lcom/google/android/apps/youtube/app/ui/pivotbar/PivotTabsBar;

    iget v1, v1, Labsh;->q:I

    .line 1
    invoke-virtual {v0, v1}, Larpz;->get(I)Ljava/lang/Object;

    move-result-object v0

    check-cast v0, Lmhm;

    if-nez v0, :cond_0

    const/4 v0, 0x0

    return v0

    :cond_0
    iget-object v1, v0, Lmhm;->c:Larls;

    invoke-virtual {v1}, Larls;->a()Z

    move-result v1

    if-eqz v1, :cond_1

    iget-object v1, p0, Lmjl;->a:Laddm;

    iget-object v0, v0, Lmhm;->c:Larls;

    .line 2
    invoke-virtual {v0}, Larls;->b()Ljava/lang/Object;

    move-result-object v0

    check-cast v0, Lauqq;

    iget-object v2, p0, Lmjl;->e:Ljava/util/Map;

    invoke-interface {v1, v0, v2}, Laddm;->a(Lauqq;Ljava/util/Map;)V

    :cond_1
    const/4 v0, 0x1

    return v0
.end method

.method public final e(Z)V
    .locals 5

    const/4 v0, 0x0

    if-eqz p1, :cond_4

    const/4 p1, 0x0

    :goto_0
    iget-object v1, p0, Lmjl;->l:Larpz;

    .line 3
    invoke-virtual {v1}, Larpz;->size()I

    move-result v1

    if-ge p1, v1, :cond_6

    iget-object v1, p0, Lmjl;->m:Lcom/google/android/apps/youtube/app/ui/pivotbar/PivotTabsBar;

    iget v1, v1, Labsh;->q:I

    if-ne p1, v1, :cond_0

    goto :goto_3

    :cond_0
    iget-object v1, p0, Lmjl;->l:Larpz;

    .line 4
    invoke-virtual {v1, p1}, Larpz;->get(I)Ljava/lang/Object;

    move-result-object v1

    check-cast v1, Lmhm;

    iget-object v1, v1, Lmhm;->d:Larls;

    const-string v2, ""

    invoke-virtual {v1, v2}, Larls;->c(Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v1

    check-cast v1, Ljava/lang/String;

    const-string v2, "FEactivity"

    .line 5
    invoke-virtual {v1, v2}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v3

    if-eqz v3, :cond_3

    iget-object v1, p0, Lmjl;->q:Lakde;

    const-string v3, "FEshared"

    .line 6
    invoke-virtual {v1, v3}, Lakde;->c(Ljava/lang/String;)Z

    move-result v1

    const/4 v3, 0x1

    if-nez v1, :cond_2

    iget-object v1, p0, Lmjl;->q:Lakde;

    const-string v4, "FEnotifications_inbox"

    .line 7
    invoke-virtual {v1, v4}, Lakde;->c(Ljava/lang/String;)Z

    move-result v1

    if-eqz v1, :cond_1

    goto :goto_1

    :cond_1
    const/4 v3, 0x0

    :cond_2
    :goto_1
    iget-object v1, p0, Lmjl;->q:Lakde;

    .line 8
    invoke-virtual {v1, v2}, Lakde;->d(Ljava/lang/String;)I

    move-result v1

    goto :goto_2

    .line 11
    :cond_3
    iget-object v2, p0, Lmjl;->q:Lakde;

    .line 9
    invoke-virtual {v2, v1}, Lakde;->c(Ljava/lang/String;)Z

    move-result v3

    iget-object v2, p0, Lmjl;->q:Lakde;

    .line 10
    invoke-virtual {v2, v1}, Lakde;->d(Ljava/lang/String;)I

    move-result v1

    .line 8
    :goto_2
    iget-object v2, p0, Lmjl;->m:Lcom/google/android/apps/youtube/app/ui/pivotbar/PivotTabsBar;

    .line 11
    invoke-virtual {v2, p1, v3, v1}, Lcom/google/android/apps/youtube/app/ui/pivotbar/PivotTabsBar;->i(IZI)V

    :goto_3
    add-int/lit8 p1, p1, 0x1

    goto :goto_0

    :cond_4
    const/4 p1, 0x0

    .line 10
    :goto_4
    iget-object v1, p0, Lmjl;->l:Larpz;

    .line 1
    invoke-virtual {v1}, Larpz;->size()I

    move-result v1

    if-ge p1, v1, :cond_6

    iget-object v1, p0, Lmjl;->m:Lcom/google/android/apps/youtube/app/ui/pivotbar/PivotTabsBar;

    iget v2, v1, Labsh;->q:I

    if-eq p1, v2, :cond_5

    .line 2
    invoke-virtual {v1, p1, v0, v0}, Lcom/google/android/apps/youtube/app/ui/pivotbar/PivotTabsBar;->i(IZI)V

    :cond_5
    add-int/lit8 p1, p1, 0x1

    goto :goto_4

    :cond_6
    return-void
.end method

.method public final f()V
    .locals 0

    .line 1
    invoke-virtual {p0}, Lmjl;->k()V

    return-void
.end method

.method public final g()Laecf;
    .locals 1

    iget-object v0, p0, Lmjl;->d:Liqr;

    .line 1
    invoke-interface {v0}, Liqr;->a()Lbdqc;

    move-result-object v0

    invoke-virtual {v0}, Lbdqc;->s()Ljava/lang/Object;

    move-result-object v0

    check-cast v0, Laecf;

    return-object v0
.end method

.method public final h(Ljava/lang/Runnable;)V
    .locals 4

    iget-object v0, p0, Lmjl;->r:Llxp;

    .line 1
    invoke-interface {v0}, Llxp;->d()V

    iget-object v0, p0, Lmjl;->q:Lakde;

    .line 2
    invoke-static {}, Labgi;->d()V

    iget-object v1, v0, Lakde;->a:Ljava/util/Map;

    .line 3
    invoke-interface {v1}, Ljava/util/Map;->entrySet()Ljava/util/Set;

    move-result-object v1

    invoke-interface {v1}, Ljava/util/Set;->iterator()Ljava/util/Iterator;

    move-result-object v1

    :goto_0
    invoke-interface {v1}, Ljava/util/Iterator;->hasNext()Z

    move-result v2

    if-eqz v2, :cond_1

    invoke-interface {v1}, Ljava/util/Iterator;->next()Ljava/lang/Object;

    move-result-object v2

    check-cast v2, Ljava/util/Map$Entry;

    .line 4
    invoke-interface {v2}, Ljava/util/Map$Entry;->getValue()Ljava/lang/Object;

    move-result-object v3

    check-cast v3, Ljava/lang/Boolean;

    invoke-virtual {v3}, Ljava/lang/Boolean;->booleanValue()Z

    move-result v3

    if-nez v3, :cond_0

    .line 5
    invoke-interface {v2}, Ljava/util/Map$Entry;->getKey()Ljava/lang/Object;

    move-result-object v3

    check-cast v3, Lakda;

    iget-object v3, v3, Lakda;->a:Ljava/lang/String;

    .line 6
    invoke-virtual {v0, v3}, Lakde;->g(Ljava/lang/String;)V

    .line 7
    :cond_0
    invoke-interface {v2}, Ljava/util/Map$Entry;->getKey()Ljava/lang/Object;

    move-result-object v2

    check-cast v2, Lakda;

    iget-object v2, v2, Lakda;->a:Ljava/lang/String;

    invoke-static {}, Lakdk;->a()Lakdk;

    move-result-object v3

    .line 6
    invoke-virtual {v0, v2, v3}, Lakde;->b(Ljava/lang/String;Lakdk;)V

    goto :goto_0

    :cond_1
    iget-object v0, v0, Lakde;->a:Ljava/util/Map;

    .line 8
    invoke-interface {v0}, Ljava/util/Map;->clear()V

    .line 9
    invoke-direct {p0, p1}, Lmjl;->u(Ljava/lang/Runnable;)V

    return-void
.end method

.method public final i(Lfvh;)V
    .locals 4

    if-nez p1, :cond_0

    sget-object p1, Larkq;->a:Larkq;

    goto/16 :goto_3

    .line 23
    :cond_0
    iget-object v0, p0, Lmjl;->x:Lijh;

    .line 1
    invoke-virtual {v0, p1}, Lijh;->a(Lfvh;)Z

    move-result v0

    if-nez v0, :cond_2

    .line 2
    invoke-virtual {p1}, Lfvh;->k()Z

    move-result v0

    if-nez v0, :cond_2

    iget-object v0, p0, Lmjl;->w:Limt;

    .line 3
    invoke-virtual {v0, p1}, Limt;->b(Lfvh;)Z

    move-result v0

    if-eqz v0, :cond_1

    goto :goto_0

    :cond_1
    const/4 v0, 0x0

    goto :goto_2

    .line 4
    :cond_2
    :goto_0
    invoke-virtual {p1}, Lfvh;->d()Lauqq;

    move-result-object v0

    .line 5
    sget-object v1, Lcom/google/protos/youtube/api/innertube/BrowseEndpointOuterClass;->browseEndpoint:Latdw;

    .line 6
    invoke-virtual {v0, v1}, Latdu;->c(Latdg;)Ljava/lang/Object;

    move-result-object v0

    check-cast v0, Laufa;

    iget-object v1, p0, Lmjl;->C:Larqe;

    iget-object v2, v0, Laufa;->i:Ljava/lang/String;

    .line 7
    invoke-virtual {v2}, Ljava/lang/String;->isEmpty()Z

    move-result v2

    if-nez v2, :cond_3

    iget-object v0, v0, Laufa;->i:Ljava/lang/String;

    goto :goto_1

    .line 20
    :cond_3
    iget-object v0, v0, Laufa;->b:Ljava/lang/String;

    .line 8
    :goto_1
    invoke-virtual {v1, v0}, Larqe;->get(Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v0

    check-cast v0, Ljava/lang/Integer;

    :goto_2
    iget-object v1, p0, Lmjl;->y:Ljta;

    .line 9
    invoke-virtual {v1, p1}, Ljta;->a(Lfvh;)Z

    move-result v1

    const-string v2, "FElibrary"

    if-eqz v1, :cond_4

    iget-object v0, p0, Lmjl;->C:Larqe;

    .line 10
    invoke-virtual {v0, v2}, Larqe;->get(Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v0

    check-cast v0, Ljava/lang/Integer;

    :cond_4
    iget-object v1, p0, Lmjl;->z:Ljiy;

    iget-object v3, p1, Lfvh;->a:Ljava/lang/Class;

    iget-object v1, v1, Ljiy;->a:Ljava/lang/Class;

    if-ne v3, v1, :cond_6

    iget-object v0, p0, Lmjl;->C:Larqe;

    const-string v1, "offline_playlist_top_level_tab_id"

    .line 11
    invoke-virtual {p1, v1}, Lfvh;->m(Ljava/lang/String;)Ljava/lang/String;

    move-result-object v1

    if-nez v1, :cond_5

    move-object v1, v2

    .line 12
    :cond_5
    invoke-virtual {v0, v1}, Larqe;->get(Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v0

    check-cast v0, Ljava/lang/Integer;

    .line 13
    :cond_6
    invoke-virtual {p1}, Lfvh;->d()Lauqq;

    move-result-object v1

    if-eqz v1, :cond_7

    .line 14
    invoke-virtual {p1}, Lfvh;->d()Lauqq;

    move-result-object v1

    sget-object v3, Lcom/google/protos/youtube/api/innertube/BrowseEndpointOuterClass;->browseEndpoint:Latdw;

    .line 15
    invoke-virtual {v1, v3}, Latdu;->b(Latdg;)Z

    move-result v1

    if-eqz v1, :cond_7

    .line 16
    invoke-virtual {p1}, Lfvh;->d()Lauqq;

    move-result-object p1

    sget-object v1, Lcom/google/protos/youtube/api/innertube/BrowseEndpointOuterClass;->browseEndpoint:Latdw;

    .line 17
    invoke-virtual {p1, v1}, Latdu;->c(Latdg;)Ljava/lang/Object;

    move-result-object p1

    check-cast p1, Laufa;

    iget-object p1, p1, Laufa;->b:Ljava/lang/String;

    const-string v1, "FEhistory"

    .line 18
    invoke-virtual {v1, p1}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result p1

    if-eqz p1, :cond_7

    iget-object p1, p0, Lmjl;->C:Larqe;

    .line 19
    invoke-virtual {p1, v2}, Larqe;->get(Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object p1

    move-object v0, p1

    check-cast v0, Ljava/lang/Integer;

    :cond_7
    if-eqz v0, :cond_8

    .line 20
    invoke-static {v0}, Larls;->i(Ljava/lang/Object;)Larls;

    move-result-object p1

    goto :goto_3

    :cond_8
    sget-object p1, Larkq;->a:Larkq;

    .line 0
    :goto_3
    invoke-virtual {p1}, Larls;->a()Z

    move-result v0

    if-nez v0, :cond_9

    return-void

    .line 21
    :cond_9
    invoke-virtual {p1}, Larls;->b()Ljava/lang/Object;

    move-result-object p1

    check-cast p1, Ljava/lang/Integer;

    invoke-virtual {p1}, Ljava/lang/Integer;->intValue()I

    move-result p1

    iget-object v0, p0, Lmjl;->b:Lfvt;

    .line 22
    invoke-direct {p0, p1}, Lmjl;->B(I)I

    move-result p1

    const/4 v1, 0x0

    .line 23
    invoke-interface {v0, p1, v1}, Lfvt;->i(II)V

    return-void
.end method

.method public final jS(Ljava/lang/Class;Ljava/lang/Object;I)[Ljava/lang/Class;
    .locals 1

    const/4 p1, -0x1

    if-eq p3, p1, :cond_1

    if-nez p3, :cond_0

    .line 1
    check-cast p2, Lyhx;

    .line 2
    invoke-virtual {p0}, Lmjl;->k()V

    const/4 p1, 0x0

    goto :goto_0

    .line 0
    :cond_0
    new-instance p1, Ljava/lang/IllegalStateException;

    new-instance p2, Ljava/lang/StringBuilder;

    const/16 v0, 0x20

    .line 3
    invoke-direct {p2, v0}, Ljava/lang/StringBuilder;-><init>(I)V

    const-string v0, "unsupported op code: "

    invoke-virtual {p2, v0}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {p2, p3}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    invoke-virtual {p2}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p2

    invoke-direct {p1, p2}, Ljava/lang/IllegalStateException;-><init>(Ljava/lang/String;)V

    throw p1

    :cond_1
    const/4 p1, 0x1

    new-array p1, p1, [Ljava/lang/Class;

    const/4 p2, 0x0

    .line 0
    const-class p3, Lyhx;

    aput-object p3, p1, p2

    :goto_0
    return-object p1
.end method

.method public final k()V
    .locals 1

    sget-object v0, Lmjf;->a:Ljava/lang/Runnable;

    .line 1
    invoke-direct {p0, v0}, Lmjl;->u(Ljava/lang/Runnable;)V

    return-void
.end method

.method public final lJ(Leqs;Leqs;)V
    .locals 0

    invoke-static {p0, p2}, Lmee;->c(Lepz;Leqs;)V

    return-void
.end method

.method public final lK(Leqs;)V
    .locals 1

    .line 1
    invoke-direct {p0, p1}, Lmjl;->x(Leqs;)Z

    move-result v0

    if-nez v0, :cond_0

    .line 2
    invoke-virtual {p0}, Lmjl;->o()V

    :cond_0
    iget-boolean v0, p0, Lmjl;->E:Z

    if-eqz v0, :cond_2

    .line 3
    invoke-virtual {p1}, Leqs;->d()Z

    move-result v0

    if-nez v0, :cond_1

    invoke-virtual {p1}, Leqs;->i()Z

    move-result p1

    if-nez p1, :cond_2

    .line 4
    :cond_1
    invoke-virtual {p0}, Lmjl;->n()V

    :cond_2
    return-void
.end method

.method public final m(F)V
    .locals 2

    iget-boolean v0, p0, Lmjl;->E:Z

    if-eqz v0, :cond_0

    const/4 v0, 0x0

    .line 1
    invoke-direct {p0, v0}, Lmjl;->v(Z)V

    iget-object v0, p0, Lmjl;->m:Lcom/google/android/apps/youtube/app/ui/pivotbar/PivotTabsBar;

    .line 2
    invoke-virtual {v0}, Lcom/google/android/apps/youtube/app/ui/pivotbar/PivotTabsBar;->getHeight()I

    move-result v0

    iget-object v1, p0, Lmjl;->m:Lcom/google/android/apps/youtube/app/ui/pivotbar/PivotTabsBar;

    int-to-float v0, v0

    mul-float p1, p1, v0

    .line 3
    invoke-virtual {v1, p1}, Lcom/google/android/apps/youtube/app/ui/pivotbar/PivotTabsBar;->setTranslationY(F)V

    return-void

    .line 4
    :cond_0
    invoke-virtual {p0}, Lmjl;->o()V

    return-void
.end method

.method public final mR(IIZ)V
    .locals 6

    iget-object v0, p0, Lmjl;->l:Larpz;

    .line 1
    invoke-virtual {v0, p2}, Larpz;->get(I)Ljava/lang/Object;

    move-result-object v0

    check-cast v0, Lmhm;

    iget-object v1, p0, Lmjl;->q:Lakde;

    iget-object v0, v0, Lmhm;->d:Larls;

    const-string v2, ""

    .line 2
    invoke-virtual {v0, v2}, Larls;->c(Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v0

    check-cast v0, Ljava/lang/String;

    invoke-virtual {v1, v0}, Lakde;->g(Ljava/lang/String;)V

    iget-object v0, p0, Lmjl;->t:Lbeou;

    .line 3
    invoke-interface {v0}, Lbeou;->get()Ljava/lang/Object;

    move-result-object v0

    check-cast v0, Lmho;

    const/4 v1, 0x0

    if-eqz p3, :cond_0

    .line 4
    invoke-interface {v0}, Lmho;->a()V

    :cond_0
    const/4 v2, 0x0

    :goto_0
    iget-object v3, p0, Lmjl;->l:Larpz;

    .line 5
    invoke-virtual {v3}, Larpz;->size()I

    move-result v3

    if-ge v2, v3, :cond_4

    iget-object v3, p0, Lmjl;->C:Larqe;

    const-string v4, "FElibrary"

    .line 6
    invoke-virtual {v3, v4}, Larqe;->get(Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v3

    check-cast v3, Ljava/lang/Integer;

    iget-object v4, p0, Lmjl;->l:Larpz;

    .line 7
    invoke-virtual {v4, v2}, Larpz;->get(I)Ljava/lang/Object;

    move-result-object v4

    check-cast v4, Lmhm;

    iget-object v5, v4, Lmhm;->b:Larls;

    invoke-virtual {v5}, Larls;->a()Z

    move-result v5

    if-eqz v5, :cond_3

    if-eqz v3, :cond_2

    .line 8
    invoke-virtual {v3}, Ljava/lang/Integer;->intValue()I

    move-result v3

    if-eq v3, v2, :cond_1

    goto :goto_1

    .line 10
    :cond_1
    invoke-interface {v0}, Lmho;->c()Z

    move-result v3

    if-eqz v3, :cond_3

    iget-object v3, p0, Lmjl;->m:Lcom/google/android/apps/youtube/app/ui/pivotbar/PivotTabsBar;

    .line 11
    invoke-virtual {v3, v2}, Labsh;->l(I)Landroid/view/View;

    move-result-object v3

    invoke-interface {v0}, Lmho;->g()Lapib;

    move-result-object v5

    .line 12
    invoke-interface {v0, v4, v3, v5}, Lmho;->e(Lmhm;Landroid/view/View;Lapib;)V

    goto :goto_2

    .line 8
    :cond_2
    :goto_1
    iget-object v3, p0, Lmjl;->m:Lcom/google/android/apps/youtube/app/ui/pivotbar/PivotTabsBar;

    .line 9
    invoke-virtual {v3, v2}, Labsh;->l(I)Landroid/view/View;

    move-result-object v3

    const/4 v5, 0x0

    invoke-interface {v0, v4, v3, v5}, Lmho;->e(Lmhm;Landroid/view/View;Lapib;)V

    :cond_3
    :goto_2
    add-int/lit8 v2, v2, 0x1

    goto :goto_0

    :cond_4
    if-nez p3, :cond_5

    return-void

    :cond_5
    if-ne p2, p1, :cond_7

    .line 12
    iget-object p3, p0, Lmjl;->b:Lfvt;

    .line 13
    invoke-interface {p3}, Lfvt;->j()Z

    move-result p3

    if-eqz p3, :cond_7

    .line 14
    invoke-direct {p0, p2}, Lmjl;->z(I)Z

    move-result p3

    if-eqz p3, :cond_7

    iget-object p1, p0, Lmjl;->f:Ljava/util/Set;

    .line 29
    invoke-interface {p1}, Ljava/util/Set;->iterator()Ljava/util/Iterator;

    move-result-object p1

    :goto_3
    invoke-interface {p1}, Ljava/util/Iterator;->hasNext()Z

    move-result p2

    if-eqz p2, :cond_6

    invoke-interface {p1}, Ljava/util/Iterator;->next()Ljava/lang/Object;

    move-result-object p2

    check-cast p2, Lmgr;

    .line 30
    invoke-interface {p2}, Lmgr;->i()V

    goto :goto_3

    :cond_6
    iget-object p1, p0, Lmjl;->b:Lfvt;

    .line 31
    invoke-interface {p1}, Lfvt;->k()V

    return-void

    :cond_7
    if-ne p2, p1, :cond_b

    iget-object p1, p0, Lmjl;->b:Lfvt;

    .line 15
    invoke-interface {p1}, Lfvt;->j()Z

    move-result p1

    if-nez p1, :cond_8

    iget-object p1, p0, Lmjl;->b:Lfvt;

    .line 16
    invoke-interface {p1}, Lfvt;->f()Z

    :cond_8
    iget-object p1, p0, Lmjl;->b:Lfvt;

    .line 17
    invoke-interface {p1}, Lfvt;->j()Z

    move-result p1

    if-eqz p1, :cond_a

    invoke-direct {p0, p2}, Lmjl;->z(I)Z

    move-result p1

    if-nez p1, :cond_9

    goto :goto_4

    :cond_9
    return-void

    .line 18
    :cond_a
    :goto_4
    invoke-virtual {p0}, Lmjl;->d()Z

    return-void

    :cond_b
    const/4 p3, -0x1

    if-eq p1, p3, :cond_c

    iget-object p3, p0, Lmjl;->l:Larpz;

    .line 19
    invoke-virtual {p3}, Larpz;->size()I

    move-result p3

    if-ge p1, p3, :cond_c

    iget-object p3, p0, Lmjl;->l:Larpz;

    .line 20
    invoke-virtual {p3, p1}, Larpz;->get(I)Ljava/lang/Object;

    move-result-object p3

    check-cast p3, Lmhm;

    iget-object p3, p3, Lmhm;->d:Larls;

    invoke-virtual {p3}, Larls;->a()Z

    move-result v0

    if-eqz v0, :cond_c

    .line 21
    invoke-virtual {p3}, Larls;->b()Ljava/lang/Object;

    move-result-object v0

    check-cast v0, Ljava/lang/String;

    invoke-virtual {v0}, Ljava/lang/String;->isEmpty()Z

    move-result v0

    if-nez v0, :cond_c

    iget-object v0, p0, Lmjl;->q:Lakde;

    .line 22
    invoke-virtual {p3}, Larls;->b()Ljava/lang/Object;

    move-result-object p3

    check-cast p3, Ljava/lang/String;

    invoke-virtual {v0, p3}, Lakde;->d(Ljava/lang/String;)I

    move-result p3

    if-lez p3, :cond_c

    iget-object v0, p0, Lmjl;->m:Lcom/google/android/apps/youtube/app/ui/pivotbar/PivotTabsBar;

    const/4 v2, 0x1

    .line 23
    invoke-virtual {v0, p1, v2, p3}, Lcom/google/android/apps/youtube/app/ui/pivotbar/PivotTabsBar;->i(IZI)V

    :cond_c
    if-eq p1, p2, :cond_e

    iget-object p1, p0, Lmjl;->b:Lfvt;

    .line 24
    invoke-direct {p0, p2}, Lmjl;->B(I)I

    move-result p3

    .line 25
    invoke-interface {p1, p3, v1}, Lfvt;->i(II)V

    iget-object p1, p0, Lmjl;->b:Lfvt;

    .line 26
    invoke-interface {p1}, Lfvt;->a()Lfvh;

    move-result-object p1

    invoke-direct {p0, p1}, Lmjl;->y(Lfvh;)Z

    move-result p1

    if-eqz p1, :cond_d

    .line 27
    invoke-virtual {p0}, Lmjl;->d()Z

    :cond_d
    iget-object p1, p0, Lmjl;->m:Lcom/google/android/apps/youtube/app/ui/pivotbar/PivotTabsBar;

    .line 28
    invoke-virtual {p1, p2, v1, v1}, Lcom/google/android/apps/youtube/app/ui/pivotbar/PivotTabsBar;->i(IZI)V

    :cond_e
    return-void
.end method

.method public final n()V
    .locals 1

    const/4 v0, 0x1

    .line 1
    invoke-direct {p0, v0}, Lmjl;->v(Z)V

    return-void
.end method

.method final o()V
    .locals 2

    iget-object v0, p0, Lmjl;->m:Lcom/google/android/apps/youtube/app/ui/pivotbar/PivotTabsBar;

    const/16 v1, 0x8

    .line 1
    invoke-virtual {v0, v1}, Lcom/google/android/apps/youtube/app/ui/pivotbar/PivotTabsBar;->setVisibility(I)V

    return-void
.end method

.method public final p(Z)V
    .locals 20

    move-object/from16 v0, p0

    iget-object v1, v0, Lmjl;->m:Lcom/google/android/apps/youtube/app/ui/pivotbar/PivotTabsBar;

    if-nez v1, :cond_0

    return-void

    :cond_0
    const-string v2, ""

    const/4 v3, 0x0

    if-eqz p1, :cond_1d

    .line 6
    invoke-virtual {v1}, Labsh;->kf()V

    .line 7
    invoke-static {}, Larqe;->m()Larqb;

    move-result-object v1

    const/4 v4, 0x0

    :goto_0
    iget-object v5, v0, Lmjl;->l:Larpz;

    .line 8
    invoke-virtual {v5}, Larpz;->size()I

    move-result v5

    const/4 v6, 0x1

    if-ge v4, v5, :cond_1a

    iget-object v5, v0, Lmjl;->l:Larpz;

    .line 9
    invoke-virtual {v5, v4}, Larpz;->get(I)Ljava/lang/Object;

    move-result-object v5

    check-cast v5, Lmhm;

    iget-object v7, v5, Lmhm;->d:Larls;

    .line 10
    invoke-virtual {v7, v2}, Larls;->c(Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v7

    check-cast v7, Ljava/lang/String;

    iget-object v8, v5, Lmhm;->a:Latfj;

    .line 11
    instance-of v9, v8, Layvi;

    if-eqz v9, :cond_12

    iget-boolean v8, v5, Lmhm;->f:Z

    if-nez v8, :cond_2

    iget-object v8, v0, Lmjl;->q:Lakde;

    iget-object v9, v5, Lmhm;->d:Larls;

    .line 25
    invoke-virtual {v9, v2}, Larls;->c(Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v9

    check-cast v9, Ljava/lang/String;

    .line 26
    invoke-virtual {v8, v9}, Lakde;->c(Ljava/lang/String;)Z

    move-result v8

    if-eqz v8, :cond_1

    goto :goto_1

    :cond_1
    const/4 v12, 0x0

    goto :goto_2

    :cond_2
    :goto_1
    const/4 v12, 0x1

    :goto_2
    iget-object v8, v5, Lmhm;->a:Latfj;

    .line 27
    check-cast v8, Layvi;

    iget-object v9, v5, Lmhm;->g:Larls;

    .line 28
    sget-object v10, Layvk;->a:Layvk;

    invoke-virtual {v9, v10}, Larls;->c(Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v9

    move-object v15, v9

    check-cast v15, Layvk;

    iget-object v9, v0, Lmjl;->o:Ladbp;

    .line 29
    invoke-static {v9}, Lgcz;->aw(Ladbp;)Z

    move-result v9

    const v10, 0x12f9f174

    if-eqz v9, :cond_b

    iget-object v9, v0, Lmjl;->p:Lapbe;

    instance-of v13, v9, Lftt;

    if-eqz v13, :cond_b

    iget-object v13, v0, Lmjl;->m:Lcom/google/android/apps/youtube/app/ui/pivotbar/PivotTabsBar;

    iget-object v14, v8, Layvi;->f:Lavyz;

    if-nez v14, :cond_3

    .line 43
    sget-object v14, Lavyz;->c:Lavyz;

    :cond_3
    iget v14, v14, Lavyz;->b:I

    .line 44
    invoke-static {v14}, Lavyy;->a(I)Lavyy;

    move-result-object v14

    if-nez v14, :cond_4

    sget-object v14, Lavyy;->a:Lavyy;

    :cond_4
    check-cast v9, Lftt;

    .line 45
    invoke-virtual {v9, v14, v3}, Lftt;->b(Lavyy;Z)I

    move-result v9

    iget-object v14, v0, Lmjl;->p:Lapbe;

    iget-object v11, v8, Layvi;->f:Lavyz;

    if-nez v11, :cond_5

    sget-object v11, Lavyz;->c:Lavyz;

    :cond_5
    iget v11, v11, Lavyz;->b:I

    invoke-static {v11}, Lavyy;->a(I)Lavyy;

    move-result-object v11

    if-nez v11, :cond_6

    sget-object v11, Lavyy;->a:Lavyy;

    :cond_6
    check-cast v14, Lftt;

    .line 46
    invoke-virtual {v14, v11, v6}, Lftt;->b(Lavyy;Z)I

    move-result v11

    iget v14, v8, Layvi;->a:I

    and-int/lit8 v14, v14, 0x8

    if-eqz v14, :cond_7

    iget-object v14, v8, Layvi;->e:Lavrt;

    if-nez v14, :cond_8

    .line 47
    sget-object v14, Lavrt;->f:Lavrt;

    goto :goto_3

    :cond_7
    const/4 v14, 0x0

    .line 48
    :cond_8
    :goto_3
    invoke-static {v14}, Laogg;->a(Lavrt;)Landroid/text/Spanned;

    move-result-object v14

    iget-object v3, v0, Lmjl;->q:Lakde;

    iget-object v6, v8, Layvi;->b:Ljava/lang/String;

    .line 49
    invoke-virtual {v3, v6}, Lakde;->d(Ljava/lang/String;)I

    move-result v3

    iget-object v6, v8, Layvi;->h:Layve;

    if-nez v6, :cond_9

    .line 50
    sget-object v6, Layve;->c:Layve;

    :cond_9
    move-object/from16 v18, v2

    iget v2, v6, Layve;->a:I

    if-ne v2, v10, :cond_a

    iget-object v2, v6, Layve;->b:Ljava/lang/Object;

    .line 51
    check-cast v2, Lbaur;

    goto :goto_4

    .line 52
    :cond_a
    sget-object v2, Lbaur;->b:Lbaur;

    .line 51
    :goto_4
    iget-object v2, v2, Lbaur;->a:Latfe;

    .line 53
    invoke-static {v2}, Ljava/util/Collections;->unmodifiableMap(Ljava/util/Map;)Ljava/util/Map;

    move-result-object v2

    .line 54
    invoke-direct {v0, v8}, Lmjl;->w(Layvi;)Larls;

    move-result-object v6

    new-instance v10, Landroid/graphics/drawable/StateListDrawable;

    .line 55
    invoke-direct {v10}, Landroid/graphics/drawable/StateListDrawable;-><init>()V

    const/4 v8, 0x1

    new-array v8, v8, [I

    const v16, 0x10102fe

    const/16 v17, 0x0

    aput v16, v8, v17

    move-object/from16 v19, v1

    # translyte patch: Lftt/Lapbe icon lookups (a(Lavyy;)I / b(Lavyy;Z)I)
    # return 0 for any Lavyy this 2021-era map was never given a mapping
    # for, and this real crash (Resources$NotFoundException: Resource ID
    # #0x0, confirmed on real device) is Context.getDrawable(0) on
    # exactly that. Skip adding this StateListDrawable state instead of
    # crashing - a missing state just isn't drawn, same non-crashing
    # degrade used for v14's equivalent Laltp bug.
    .line 56
    if-eqz v11, :cond_translyte_skip_sel

    invoke-virtual {v13}, Lcom/google/android/apps/youtube/app/ui/pivotbar/PivotTabsBar;->getContext()Landroid/content/Context;

    move-result-object v1

    .line 57
    invoke-virtual {v1, v11}, Landroid/content/Context;->getDrawable(I)Landroid/graphics/drawable/Drawable;

    move-result-object v1

    .line 58
    invoke-virtual {v10, v8, v1}, Landroid/graphics/drawable/StateListDrawable;->addState([ILandroid/graphics/drawable/Drawable;)V

    :cond_translyte_skip_sel
    sget-object v1, Landroid/util/StateSet;->WILD_CARD:[I

    .line 56
    if-eqz v9, :cond_translyte_skip_def

    invoke-virtual {v13}, Lcom/google/android/apps/youtube/app/ui/pivotbar/PivotTabsBar;->getContext()Landroid/content/Context;

    move-result-object v8

    .line 59
    invoke-virtual {v8, v9}, Landroid/content/Context;->getDrawable(I)Landroid/graphics/drawable/Drawable;

    move-result-object v8

    .line 60
    invoke-virtual {v10, v1, v8}, Landroid/graphics/drawable/StateListDrawable;->addState([ILandroid/graphics/drawable/Drawable;)V

    :cond_translyte_skip_def

    move-object v9, v13

    move-object v11, v14

    move v13, v3

    move-object v14, v2

    move-object/from16 v16, v6

    .line 56
    invoke-virtual/range {v9 .. v16}, Lcom/google/android/apps/youtube/app/ui/pivotbar/PivotTabsBar;->c(Landroid/graphics/drawable/Drawable;Ljava/lang/CharSequence;ZILjava/util/Map;Layvk;Larls;)Landroid/view/View;

    move-result-object v1

    goto :goto_7

    :cond_b
    move-object/from16 v19, v1

    move-object/from16 v18, v2

    .line 52
    iget-object v9, v0, Lmjl;->m:Lcom/google/android/apps/youtube/app/ui/pivotbar/PivotTabsBar;

    iget-object v1, v0, Lmjl;->p:Lapbe;

    iget-object v2, v8, Layvi;->f:Lavyz;

    if-nez v2, :cond_c

    .line 30
    sget-object v2, Lavyz;->c:Lavyz;

    :cond_c
    iget v2, v2, Lavyz;->b:I

    .line 31
    invoke-static {v2}, Lavyy;->a(I)Lavyy;

    move-result-object v2

    if-nez v2, :cond_d

    sget-object v2, Lavyy;->a:Lavyy;

    .line 32
    :cond_d
    invoke-interface {v1, v2}, Lapbe;->a(Lavyy;)I

    move-result v1

    iget v2, v8, Layvi;->a:I

    and-int/lit8 v2, v2, 0x8

    if-eqz v2, :cond_e

    iget-object v11, v8, Layvi;->e:Lavrt;

    if-nez v11, :cond_f

    .line 33
    sget-object v11, Lavrt;->f:Lavrt;

    goto :goto_5

    :cond_e
    const/4 v11, 0x0

    .line 34
    :cond_f
    :goto_5
    invoke-static {v11}, Laogg;->a(Lavrt;)Landroid/text/Spanned;

    move-result-object v11

    iget-object v2, v0, Lmjl;->q:Lakde;

    iget-object v3, v8, Layvi;->b:Ljava/lang/String;

    .line 35
    invoke-virtual {v2, v3}, Lakde;->d(Ljava/lang/String;)I

    move-result v13

    iget-object v2, v8, Layvi;->h:Layve;

    if-nez v2, :cond_10

    .line 36
    sget-object v2, Layve;->c:Layve;

    :cond_10
    iget v3, v2, Layve;->a:I

    if-ne v3, v10, :cond_11

    iget-object v2, v2, Layve;->b:Ljava/lang/Object;

    .line 37
    check-cast v2, Lbaur;

    goto :goto_6

    .line 38
    :cond_11
    sget-object v2, Lbaur;->b:Lbaur;

    .line 37
    :goto_6
    iget-object v2, v2, Lbaur;->a:Latfe;

    .line 39
    invoke-static {v2}, Ljava/util/Collections;->unmodifiableMap(Ljava/util/Map;)Ljava/util/Map;

    move-result-object v14

    .line 40
    invoke-direct {v0, v8}, Lmjl;->w(Layvi;)Larls;

    move-result-object v16

    .line 41
    invoke-virtual {v9}, Lcom/google/android/apps/youtube/app/ui/pivotbar/PivotTabsBar;->getContext()Landroid/content/Context;

    move-result-object v2

    # translyte patch: same Lftt/Lapbe unmapped-icon (0) crash as above.
    # Real device confirmed passing null here just moves the crash one
    # step later - PivotTabsBar.c()'s downstream mie/mit construction
    # has its own Preconditions.checkNotNull on this Drawable. Fall back
    # to one of Lftt's own already-valid ids instead of null (resource
    # ids are global, not scoped to the class that references them).
    .line 42
    if-eqz v1, :cond_translyte_null_icon_a

    invoke-virtual {v2, v1}, Landroid/content/Context;->getDrawable(I)Landroid/graphics/drawable/Drawable;

    move-result-object v10

    goto :cond_translyte_icon_done_a

    :cond_translyte_null_icon_a
    const v1, 0x7f0806fa

    invoke-virtual {v2, v1}, Landroid/content/Context;->getDrawable(I)Landroid/graphics/drawable/Drawable;

    move-result-object v10

    :cond_translyte_icon_done_a

    .line 41
    invoke-virtual/range {v9 .. v16}, Lcom/google/android/apps/youtube/app/ui/pivotbar/PivotTabsBar;->c(Landroid/graphics/drawable/Drawable;Ljava/lang/CharSequence;ZILjava/util/Map;Layvk;Larls;)Landroid/view/View;

    move-result-object v1

    .line 61
    :goto_7
    invoke-static {v1}, Larls;->i(Ljava/lang/Object;)Larls;

    move-result-object v1

    goto :goto_8

    :cond_12
    move-object/from16 v19, v1

    move-object/from16 v18, v2

    .line 12
    instance-of v1, v8, Layvd;

    if-eqz v1, :cond_17

    .line 13
    check-cast v8, Layvd;

    iget-object v1, v5, Lmhm;->g:Larls;

    .line 14
    sget-object v2, Layvk;->a:Layvk;

    invoke-virtual {v1, v2}, Larls;->c(Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v1

    check-cast v1, Layvk;

    iget-object v2, v0, Lmjl;->m:Lcom/google/android/apps/youtube/app/ui/pivotbar/PivotTabsBar;

    iget-object v3, v0, Lmjl;->p:Lapbe;

    iget-object v6, v8, Layvd;->f:Lavyz;

    if-nez v6, :cond_13

    .line 15
    sget-object v6, Lavyz;->c:Lavyz;

    :cond_13
    iget v6, v6, Lavyz;->b:I

    .line 16
    invoke-static {v6}, Lavyy;->a(I)Lavyy;

    move-result-object v6

    if-nez v6, :cond_14

    sget-object v6, Lavyy;->a:Lavyy;

    .line 17
    :cond_14
    invoke-interface {v3, v6}, Lapbe;->a(Lavyy;)I

    move-result v3

    iget-object v6, v8, Layvd;->e:Latjy;

    if-nez v6, :cond_15

    .line 18
    sget-object v6, Latjy;->c:Latjy;

    :cond_15
    iget-object v6, v6, Latjy;->b:Latjx;

    if-nez v6, :cond_16

    .line 19
    sget-object v6, Latjx;->d:Latjx;

    :cond_16
    iget-object v14, v6, Latjx;->b:Ljava/lang/String;

    iget-object v12, v2, Lcom/google/android/apps/youtube/app/ui/pivotbar/PivotTabsBar;->p:Landroid/widget/LinearLayout;

    .line 20
    invoke-virtual {v2}, Lcom/google/android/apps/youtube/app/ui/pivotbar/PivotTabsBar;->getContext()Landroid/content/Context;

    move-result-object v6

    # translyte patch: same Lftt/Lapbe unmapped-icon (0) crash as above,
    # same downstream Lmie construction as the site above this one -
    # real device confirmed it requires a non-null Drawable
    # (Preconditions.checkNotNull), so fall back to a real icon instead
    # of null.
    .line 21
    if-eqz v3, :cond_translyte_null_icon_b

    invoke-virtual {v6, v3}, Landroid/content/Context;->getDrawable(I)Landroid/graphics/drawable/Drawable;

    move-result-object v13

    goto :cond_translyte_icon_done_b

    :cond_translyte_null_icon_b
    const v3, 0x7f0806fa

    invoke-virtual {v6, v3}, Landroid/content/Context;->getDrawable(I)Landroid/graphics/drawable/Drawable;

    move-result-object v13

    :cond_translyte_icon_done_b

    new-instance v3, Lmie;

    new-instance v15, Ljava/util/HashMap;

    .line 22
    invoke-direct {v15}, Ljava/util/HashMap;-><init>()V

    const v11, 0x7f0e022d

    sget-object v16, Larkq;->a:Larkq;

    move-object v9, v3

    move-object v10, v2

    .line 23
    invoke-direct/range {v9 .. v16}, Lmie;-><init>(Lcom/google/android/apps/youtube/app/ui/pivotbar/PivotTabsBar;ILandroid/view/ViewGroup;Landroid/graphics/drawable/Drawable;Ljava/lang/CharSequence;Ljava/util/Map;Larls;)V

    const/4 v6, 0x0

    .line 20
    invoke-virtual {v2, v3, v6, v6, v1}, Lcom/google/android/apps/youtube/app/ui/pivotbar/PivotTabsBar;->d(Lmie;ZILayvk;)Landroid/view/View;

    move-result-object v1

    .line 24
    invoke-static {v1}, Larls;->i(Ljava/lang/Object;)Larls;

    move-result-object v1

    goto :goto_8

    :cond_17
    sget-object v1, Larkq;->a:Larkq;

    .line 61
    :goto_8
    invoke-virtual {v1}, Larls;->a()Z

    move-result v2

    if-nez v2, :cond_18

    move-object/from16 v3, v19

    goto :goto_9

    .line 62
    :cond_18
    invoke-virtual {v1}, Larls;->b()Ljava/lang/Object;

    move-result-object v1

    check-cast v1, Landroid/view/View;

    iget-object v2, v0, Lmjl;->o:Ladbp;

    .line 63
    invoke-static {v2}, Lgcz;->au(Ladbp;)Z

    move-result v2

    if-eqz v2, :cond_19

    .line 64
    invoke-static {v1}, Laphk;->e(Landroid/view/View;)V

    .line 65
    :cond_19
    invoke-static {v4}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    move-result-object v2

    move-object/from16 v3, v19

    invoke-virtual {v3, v7, v2}, Larqb;->e(Ljava/lang/Object;Ljava/lang/Object;)V

    iget-object v6, v0, Lmjl;->D:Ljava/util/Map;

    .line 66
    invoke-interface {v6, v2, v2}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    iget-object v2, v0, Lmjl;->u:Laovo;

    .line 67
    invoke-virtual {v2, v5, v1}, Laovo;->a(Ljava/lang/Object;Landroid/view/View;)V

    :goto_9
    add-int/lit8 v4, v4, 0x1

    move-object v1, v3

    move-object/from16 v2, v18

    const/4 v3, 0x0

    goto/16 :goto_0

    :cond_1a
    move-object v3, v1

    move-object/from16 v18, v2

    .line 24
    iget-object v1, v0, Lmjl;->m:Lcom/google/android/apps/youtube/app/ui/pivotbar/PivotTabsBar;

    iput-object v0, v1, Labsh;->s:Labsf;

    iput-object v0, v1, Labsh;->t:Labsg;

    .line 68
    invoke-virtual {v3}, Larqb;->b()Larqe;

    move-result-object v1

    iput-object v1, v0, Lmjl;->C:Larqe;

    iget-object v1, v0, Lmjl;->b:Lfvt;

    .line 69
    invoke-interface {v1}, Lfvt;->a()Lfvh;

    move-result-object v1

    invoke-direct {v0, v1}, Lmjl;->y(Lfvh;)Z

    move-result v1

    if-nez v1, :cond_1c

    iget-object v1, v0, Lmjl;->b:Lfvt;

    .line 70
    invoke-interface {v1}, Lfvt;->a()Lfvh;

    move-result-object v1

    .line 71
    invoke-virtual {v1}, Lfvh;->d()Lauqq;

    move-result-object v1

    sget-object v2, Lcom/google/protos/youtube/api/innertube/BrowseEndpointOuterClass;->browseEndpoint:Latdw;

    .line 72
    invoke-virtual {v1, v2}, Latdu;->c(Latdg;)Ljava/lang/Object;

    move-result-object v1

    check-cast v1, Laufa;

    iget-object v1, v1, Laufa;->b:Ljava/lang/String;

    .line 73
    invoke-virtual {v1}, Ljava/lang/String;->isEmpty()Z

    move-result v2

    if-nez v2, :cond_1c

    iget-object v2, v0, Lmjl;->C:Larqe;

    .line 74
    invoke-virtual {v2, v1}, Larqe;->containsKey(Ljava/lang/Object;)Z

    move-result v2

    if-eqz v2, :cond_1c

    iget-object v2, v0, Lmjl;->C:Larqe;

    .line 75
    invoke-virtual {v2, v1}, Larqe;->get(Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v1

    check-cast v1, Ljava/lang/Integer;

    invoke-virtual {v1}, Ljava/lang/Integer;->intValue()I

    move-result v1

    iget-object v2, v0, Lmjl;->b:Lfvt;

    invoke-interface {v2}, Lfvt;->c()I

    move-result v2

    if-eq v1, v2, :cond_1c

    iget-object v1, v0, Lmjl;->b:Lfvt;

    .line 76
    invoke-interface {v1}, Lfvt;->c()I

    move-result v1

    const/4 v2, -0x1

    if-eq v1, v2, :cond_1c

    iget-object v2, v0, Lmjl;->b:Lfvt;

    .line 77
    invoke-interface {v2}, Lfvt;->a()Lfvh;

    move-result-object v2

    invoke-direct {v0, v2}, Lmjl;->y(Lfvh;)Z

    move-result v2

    if-nez v2, :cond_1b

    iget-object v2, v0, Lmjl;->b:Lfvt;

    .line 78
    invoke-interface {v2}, Lfvt;->a()Lfvh;

    move-result-object v2

    .line 79
    invoke-virtual {v2}, Lfvh;->d()Lauqq;

    move-result-object v2

    sget-object v3, Lcom/google/protos/youtube/api/innertube/BrowseEndpointOuterClass;->browseEndpoint:Latdw;

    .line 80
    invoke-virtual {v2, v3}, Latdu;->c(Latdg;)Ljava/lang/Object;

    move-result-object v2

    check-cast v2, Laufa;

    iget-object v2, v2, Laufa;->b:Ljava/lang/String;

    goto :goto_a

    :cond_1b
    move-object/from16 v2, v18

    :goto_a
    iget-object v3, v0, Lmjl;->C:Larqe;

    .line 81
    invoke-virtual {v3, v2}, Larqe;->get(Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v2

    check-cast v2, Ljava/lang/Integer;

    invoke-virtual {v2}, Ljava/lang/Integer;->intValue()I

    move-result v2

    if-eqz v2, :cond_1c

    iget-object v3, v0, Lmjl;->D:Ljava/util/Map;

    .line 82
    invoke-static {v2}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    move-result-object v4

    invoke-static {v1}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    move-result-object v1

    invoke-interface {v3, v4, v1}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    const/4 v1, 0x0

    const/4 v3, 0x0

    :goto_b
    if-ge v1, v2, :cond_1c

    iget-object v4, v0, Lmjl;->D:Ljava/util/Map;

    const/4 v5, 0x1

    add-int/2addr v3, v5

    .line 83
    invoke-static {v1}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    move-result-object v6

    invoke-interface {v4, v6}, Ljava/util/Map;->get(Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v6

    check-cast v6, Ljava/lang/Integer;

    invoke-static {v3}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    move-result-object v7

    invoke-interface {v4, v6, v7}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    add-int/lit8 v1, v1, 0x1

    goto :goto_b

    :cond_1c
    iget-object v1, v0, Lmjl;->b:Lfvt;

    .line 84
    invoke-interface {v1}, Lfvt;->c()I

    move-result v1

    invoke-direct {v0, v1}, Lmjl;->A(I)I

    move-result v1

    if-ltz v1, :cond_1f

    iget-object v2, v0, Lmjl;->m:Lcom/google/android/apps/youtube/app/ui/pivotbar/PivotTabsBar;

    .line 85
    invoke-virtual {v2}, Labsh;->k()I

    move-result v2

    if-ge v1, v2, :cond_1f

    iget-object v2, v0, Lmjl;->m:Lcom/google/android/apps/youtube/app/ui/pivotbar/PivotTabsBar;

    const/4 v3, 0x0

    .line 86
    invoke-virtual {v2, v1, v3}, Labsh;->m(IZ)V

    goto :goto_e

    :cond_1d
    :goto_c
    move-object/from16 v18, v2

    .line 89
    iget-object v1, v0, Lmjl;->l:Larpz;

    .line 1
    invoke-virtual {v1}, Larpz;->size()I

    move-result v1

    if-ge v3, v1, :cond_1f

    iget-object v1, v0, Lmjl;->m:Lcom/google/android/apps/youtube/app/ui/pivotbar/PivotTabsBar;

    iget v1, v1, Labsh;->q:I

    if-eq v3, v1, :cond_1e

    iget-object v1, v0, Lmjl;->l:Larpz;

    .line 2
    invoke-virtual {v1, v3}, Larpz;->get(I)Ljava/lang/Object;

    move-result-object v1

    check-cast v1, Lmhm;

    iget-object v1, v1, Lmhm;->d:Larls;

    move-object/from16 v2, v18

    invoke-virtual {v1, v2}, Larls;->c(Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v1

    check-cast v1, Ljava/lang/String;

    iget-object v4, v0, Lmjl;->m:Lcom/google/android/apps/youtube/app/ui/pivotbar/PivotTabsBar;

    iget-object v5, v0, Lmjl;->q:Lakde;

    .line 3
    invoke-virtual {v5, v1}, Lakde;->c(Ljava/lang/String;)Z

    move-result v5

    iget-object v6, v0, Lmjl;->q:Lakde;

    .line 4
    invoke-virtual {v6, v1}, Lakde;->d(Ljava/lang/String;)I

    move-result v1

    .line 5
    invoke-virtual {v4, v3, v5, v1}, Lcom/google/android/apps/youtube/app/ui/pivotbar/PivotTabsBar;->i(IZI)V

    goto :goto_d

    :cond_1e
    move-object/from16 v2, v18

    :goto_d
    add-int/lit8 v3, v3, 0x1

    goto :goto_c

    .line 86
    :cond_1f
    :goto_e
    iget-object v1, v0, Lmjl;->b:Lfvt;

    .line 87
    invoke-interface {v1}, Lfvt;->a()Lfvh;

    move-result-object v1

    invoke-virtual {v0, v1}, Lmjl;->r(Lfvh;)Z

    move-result v1

    if-eqz v1, :cond_20

    .line 88
    invoke-virtual/range {p0 .. p0}, Lmjl;->n()V

    return-void

    .line 89
    :cond_20
    invoke-virtual/range {p0 .. p0}, Lmjl;->o()V

    return-void
.end method

.method public final q(Lfvh;Z)V
    .locals 2

    iget-object v0, p0, Lmjl;->t:Lbeou;

    .line 1
    invoke-interface {v0}, Lbeou;->get()Ljava/lang/Object;

    move-result-object v0

    check-cast v0, Lmho;

    invoke-interface {v0, p1}, Lmho;->b(Lfvh;)V

    .line 2
    invoke-virtual {p0, p1}, Lmjl;->r(Lfvh;)Z

    move-result v0

    iput-boolean v0, p0, Lmjl;->E:Z

    if-eqz v0, :cond_3

    .line 3
    invoke-virtual {p0}, Lmjl;->n()V

    iget-object v0, p0, Lmjl;->b:Lfvt;

    .line 4
    invoke-interface {v0}, Lfvt;->c()I

    move-result v0

    invoke-direct {p0, v0}, Lmjl;->A(I)I

    move-result v0

    if-ltz v0, :cond_0

    iget-object v1, p0, Lmjl;->m:Lcom/google/android/apps/youtube/app/ui/pivotbar/PivotTabsBar;

    .line 5
    invoke-virtual {v1}, Labsh;->k()I

    move-result v1

    if-ge v0, v1, :cond_0

    iget-object v1, p0, Lmjl;->m:Lcom/google/android/apps/youtube/app/ui/pivotbar/PivotTabsBar;

    .line 6
    invoke-virtual {v1, v0, p2}, Labsh;->m(IZ)V

    :cond_0
    iget-object p2, p0, Lmjl;->s:Ladbt;

    .line 7
    invoke-static {p1, p2}, Lsxy;->b(Lfvh;Ladbt;)Z

    move-result p1

    iget-object p2, p0, Lmjl;->m:Lcom/google/android/apps/youtube/app/ui/pivotbar/PivotTabsBar;

    iget-boolean v0, p2, Lcom/google/android/apps/youtube/app/ui/pivotbar/PivotTabsBar;->k:Z

    if-ne v0, p1, :cond_1

    return-void

    :cond_1
    iput-boolean p1, p2, Lcom/google/android/apps/youtube/app/ui/pivotbar/PivotTabsBar;->k:Z

    if-eqz p1, :cond_2

    iget-object p1, p2, Lcom/google/android/apps/youtube/app/ui/pivotbar/PivotTabsBar;->d:Landroid/graphics/drawable/Drawable;

    goto :goto_0

    .line 8
    :cond_2
    iget-object p1, p2, Lcom/google/android/apps/youtube/app/ui/pivotbar/PivotTabsBar;->c:Landroid/graphics/drawable/Drawable;

    :goto_0
    invoke-virtual {p2, p1}, Lcom/google/android/apps/youtube/app/ui/pivotbar/PivotTabsBar;->setBackground(Landroid/graphics/drawable/Drawable;)V

    invoke-virtual {p2}, Lcom/google/android/apps/youtube/app/ui/pivotbar/PivotTabsBar;->f()V

    return-void

    .line 9
    :cond_3
    invoke-virtual {p0}, Lmjl;->o()V

    return-void
.end method

.method final r(Lfvh;)Z
    .locals 4

    const/4 v0, 0x1

    if-nez p1, :cond_0

    return v0

    .line 1
    :cond_0
    invoke-virtual {p1}, Lfvh;->d()Lauqq;

    move-result-object v1

    const/4 v2, 0x0

    if-eqz v1, :cond_2

    .line 2
    sget-object v3, Lcom/google/protos/youtube/api/innertube/ConversationReplyPanelEndpointOuterClass$ConversationReplyPanelEndpoint;->conversationReplyPanelEndpoint:Latdw;

    .line 3
    invoke-virtual {v1, v3}, Latdu;->b(Latdg;)Z

    move-result v3

    if-nez v3, :cond_1

    sget-object v3, Lcom/google/protos/youtube/api/innertube/ConversationParticipantsEndpointOuterClass$ConversationParticipantsEndpoint;->conversationParticipantsEndpoint:Latdw;

    .line 4
    invoke-virtual {v1, v3}, Latdu;->b(Latdg;)Z

    move-result v3

    if-nez v3, :cond_1

    sget-object v3, Lcom/google/protos/youtube/api/innertube/InviteMoreEndpointOuterClass$InviteMoreEndpoint;->inviteMoreEndpoint:Latdw;

    .line 5
    invoke-virtual {v1, v3}, Latdu;->b(Latdg;)Z

    move-result v3

    if-nez v3, :cond_1

    sget-object v3, Lcom/google/protos/youtube/api/innertube/SearchEndpointOuterClass;->searchEndpoint:Latdw;

    .line 6
    invoke-virtual {v1, v3}, Latdu;->c(Latdg;)Ljava/lang/Object;

    move-result-object v3

    check-cast v3, Lazqa;

    iget-object v3, v3, Lazqa;->e:Ljava/lang/String;

    .line 7
    invoke-virtual {v3}, Ljava/lang/String;->isEmpty()Z

    move-result v3

    if-eqz v3, :cond_1

    goto :goto_0

    :cond_1
    return v2

    :cond_2
    :goto_0
    if-eqz v1, :cond_4

    .line 8
    sget-object v3, Lcom/google/protos/youtube/api/innertube/WatchEndpointOuterClass;->watchEndpoint:Latdw;

    .line 9
    invoke-virtual {v1, v3}, Latdu;->b(Latdg;)Z

    move-result v3

    if-nez v3, :cond_3

    sget-object v3, Lcom/google/protos/youtube/api/innertube/WatchPlaylistEndpointOuterClass;->watchPlaylistEndpoint:Latdw;

    .line 10
    invoke-virtual {v1, v3}, Latdu;->b(Latdg;)Z

    move-result v3

    if-nez v3, :cond_3

    sget-object v3, Layrh;->a:Latdw;

    .line 11
    invoke-virtual {v1, v3}, Latdu;->b(Latdg;)Z

    move-result v3

    if-nez v3, :cond_3

    goto :goto_1

    :cond_3
    return v2

    :cond_4
    :goto_1
    if-eqz v1, :cond_6

    .line 12
    sget-object v3, Lcom/google/protos/youtube/api/innertube/BrowseEndpointOuterClass;->browseEndpoint:Latdw;

    .line 13
    invoke-virtual {v1, v3}, Latdu;->c(Latdg;)Ljava/lang/Object;

    move-result-object v1

    check-cast v1, Laufa;

    iget-object v1, v1, Laufa;->b:Ljava/lang/String;

    const-string v3, "FEvideo_picker"

    .line 14
    invoke-virtual {v1, v3}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v1

    if-nez v1, :cond_5

    goto :goto_2

    :cond_5
    return v2

    :cond_6
    :goto_2
    iget-object v1, p0, Lmjl;->A:Lkyc;

    invoke-virtual {v1, p1}, Lkyc;->a(Lfvh;)Z

    move-result p1

    if-eqz p1, :cond_7

    return v2

    :cond_7
    return v0
.end method

.method public final s(Ljava/lang/String;)V
    .locals 0

    return-void
.end method

.method public final t(Ljava/lang/String;ZI)V
    .locals 3

    .line 1
    invoke-static {p1}, Landroid/text/TextUtils;->isEmpty(Ljava/lang/CharSequence;)Z

    move-result v0

    if-nez v0, :cond_8

    iget-object v0, p0, Lmjl;->l:Larpz;

    invoke-virtual {v0}, Larpz;->isEmpty()Z

    move-result v0

    if-eqz v0, :cond_0

    goto/16 :goto_2

    :cond_0
    iget-object v0, p0, Lmjl;->C:Larqe;

    .line 2
    invoke-virtual {v0, p1}, Larqe;->get(Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v0

    check-cast v0, Ljava/lang/Integer;

    iget-object v1, p0, Lmjl;->C:Larqe;

    const-string v2, "FEactivity"

    .line 3
    invoke-virtual {v1, v2}, Larqe;->containsKey(Ljava/lang/Object;)Z

    move-result v1

    if-nez v0, :cond_6

    const-string p2, "FEshared"

    .line 4
    invoke-virtual {p2, p1}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result p3

    if-eqz p3, :cond_2

    if-eqz v1, :cond_1

    goto :goto_0

    .line 12
    :cond_1
    invoke-virtual {p0}, Lmjl;->k()V

    return-void

    :cond_2
    if-eqz v1, :cond_5

    .line 5
    :goto_0
    invoke-virtual {p2, p1}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result p2

    if-nez p2, :cond_3

    const-string p2, "FEnotifications_inbox"

    .line 6
    invoke-virtual {p2, p1}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result p1

    if-eqz p1, :cond_5

    :cond_3
    iget-object p1, p0, Lmjl;->C:Larqe;

    .line 7
    invoke-virtual {p1, v2}, Larqe;->get(Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object p1

    move-object v0, p1

    check-cast v0, Ljava/lang/Integer;

    iget-object p1, p0, Lmjl;->q:Lakde;

    .line 8
    invoke-virtual {p1, v2}, Lakde;->d(Ljava/lang/String;)I

    move-result p3

    if-lez p3, :cond_4

    const/4 p2, 0x1

    goto :goto_1

    :cond_4
    const/4 p2, 0x0

    goto :goto_1

    :cond_5
    return-void

    .line 9
    :cond_6
    :goto_1
    invoke-virtual {v0}, Ljava/lang/Integer;->intValue()I

    move-result p1

    iget-object v1, p0, Lmjl;->m:Lcom/google/android/apps/youtube/app/ui/pivotbar/PivotTabsBar;

    iget v1, v1, Labsh;->q:I

    if-ne p1, v1, :cond_7

    return-void

    .line 10
    :cond_7
    invoke-virtual {v0}, Ljava/lang/Integer;->intValue()I

    move-result p1

    if-ltz p1, :cond_8

    invoke-virtual {v0}, Ljava/lang/Integer;->intValue()I

    move-result p1

    iget-object v1, p0, Lmjl;->l:Larpz;

    invoke-virtual {v1}, Larpz;->size()I

    move-result v1

    if-ge p1, v1, :cond_8

    iget-object p1, p0, Lmjl;->m:Lcom/google/android/apps/youtube/app/ui/pivotbar/PivotTabsBar;

    .line 11
    invoke-virtual {v0}, Ljava/lang/Integer;->intValue()I

    move-result v0

    invoke-virtual {p1, v0, p2, p3}, Lcom/google/android/apps/youtube/app/ui/pivotbar/PivotTabsBar;->i(IZI)V

    :cond_8
    :goto_2
    return-void
.end method
