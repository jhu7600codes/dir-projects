.class public final Lxan;
.super Lalnj;
.source "SourceFile"

# interfaces
.implements Lwvt;
.implements Lxen;


# instance fields
.field public A:Z

.field public B:Larqp;

.field private final C:Lalid;

.field private final D:Lalto;

.field private final E:Laltp;

.field private final F:Lwvq;

.field private final G:Lxem;

.field private final H:Lxbv;

.field private final I:Lwvj;

.field private final J:Lxej;

.field private final K:I

.field private final L:I

.field private final M:I

.field private final N:I

.field private final O:I

.field private final P:I

.field private final Q:I

.field private final R:I

.field private final S:I

.field private final T:I

.field private final U:I

.field private final V:I

.field private final W:I

.field private final X:I

.field private final Y:I

.field private final Z:I

.field public final a:Landroid/content/Context;

.field private aA:Landroid/view/View;

.field private aB:Landroid/widget/ImageView;

.field private aC:Landroid/widget/TextView;

.field private aD:Landroid/view/ViewGroup;

.field private aE:Landroid/view/ViewGroup;

.field private aF:Landroid/view/View;

.field private aG:Landroid/view/View;

.field private aH:Landroid/widget/FrameLayout;

.field private aI:Landroid/widget/FrameLayout;

.field private aJ:Landroid/widget/FrameLayout;

.field private aK:Landroid/widget/TextView;

.field private aL:Landroid/view/View;

.field private final aM:Lxdo;

.field private final aN:Lxdq;

.field private final aO:Lalrh;

.field private final aP:Lalrp;

.field private final aQ:Landroid/text/SpannableStringBuilder;

.field private final aR:Ljava/lang/StringBuilder;

.field private aS:Lalmp;

.field private final aT:Lxev;

.field private final aa:Landroid/widget/FrameLayout;

.field private ab:Z

.field private ac:Z

.field private ad:Landroid/animation/Animator;

.field private final ae:Lxbc;

.field private final af:Lxbc;

.field private final ag:Lxbc;

.field private ah:Landroid/view/View;

.field private ai:Landroid/widget/ImageView;

.field private aj:Lxbd;

.field private ak:Landroid/widget/TextView;

.field private al:Landroid/view/ViewGroup;

.field private am:Landroid/widget/TextView;

.field private an:Landroid/widget/ImageView;

.field private ao:Landroid/widget/TextView;

.field private ap:Landroid/widget/TextView;

.field private aq:Landroid/widget/ImageView;

.field private ar:Landroid/view/View;

.field private as:Landroid/widget/ImageView;

.field private at:Landroid/widget/TextView;

.field private au:Landroid/widget/FrameLayout;

.field private av:Landroid/widget/TextView;

.field private aw:Landroid/view/View;

.field private ax:Landroid/widget/TextView;

.field private ay:Landroid/widget/TextView;

.field private az:Landroid/widget/TextView;

.field public final b:Lwwo;

.field public final c:Lwwg;

.field public final d:Laawi;

.field public final e:Lxdv;

.field public final f:Laaul;

.field public final g:I

.field public final h:I

.field public final i:I

.field public final j:I

.field public final k:I

.field public l:I

.field public m:Z

.field public n:Landroid/view/ViewTreeObserver$OnPreDrawListener;

.field public o:Landroid/view/View;

.field public p:Landroid/view/View;

.field public q:Landroid/view/View;

.field public r:Landroid/widget/TextView;

.field public s:Landroid/widget/TextView;

.field public t:Landroid/view/ViewGroup;

.field public u:Landroid/widget/ImageView;

.field public v:Landroid/widget/ImageView;

.field public w:Landroid/widget/ImageView;

.field public x:Landroid/widget/ImageView;

.field public y:Landroid/widget/FrameLayout;

.field public z:Landroid/view/ViewTreeObserver$OnScrollChangedListener;


# direct methods
.method public constructor <init>(Landroid/content/Context;Lalid;Laawi;Lalto;Laltz;Lwwo;Lwwg;Laltp;Lwvq;Lxem;Lxdv;Lxbv;Laaul;Lwvj;Lxdq;Lxej;Lalrj;Lyps;Lxev;)V
    .locals 9

    move-object v0, p0

    move-object v1, p1

    .line 1
    invoke-direct {p0}, Lalnj;-><init>()V

    const/4 v2, 0x5

    .line 2
    iput v2, v0, Lxan;->l:I

    const/4 v3, 0x0

    .line 3
    iput-boolean v3, v0, Lxan;->ab:Z

    .line 4
    iput-boolean v3, v0, Lxan;->ac:Z

    .line 5
    iput-boolean v3, v0, Lxan;->m:Z

    .line 6
    new-instance v4, Lalrp;

    invoke-direct {v4}, Lalrp;-><init>()V

    iput-object v4, v0, Lxan;->aP:Lalrp;

    .line 7
    new-instance v4, Landroid/text/SpannableStringBuilder;

    invoke-direct {v4}, Landroid/text/SpannableStringBuilder;-><init>()V

    iput-object v4, v0, Lxan;->aQ:Landroid/text/SpannableStringBuilder;

    .line 8
    new-instance v4, Ljava/lang/StringBuilder;

    invoke-direct {v4}, Ljava/lang/StringBuilder;-><init>()V

    iput-object v4, v0, Lxan;->aR:Ljava/lang/StringBuilder;

    .line 9
    invoke-static {p1}, Lanvb;->a(Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v4

    check-cast v4, Landroid/content/Context;

    iput-object v4, v0, Lxan;->a:Landroid/content/Context;

    .line 10
    invoke-static {p2}, Lanvb;->a(Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v4

    check-cast v4, Lalid;

    iput-object v4, v0, Lxan;->C:Lalid;

    .line 11
    invoke-static {p4}, Lanvb;->a(Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v4

    check-cast v4, Lalto;

    iput-object v4, v0, Lxan;->D:Lalto;

    .line 12
    invoke-static {p3}, Lanvb;->a(Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v4

    check-cast v4, Laawi;

    iput-object v4, v0, Lxan;->d:Laawi;

    .line 13
    invoke-static {p6}, Lanvb;->a(Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v4

    check-cast v4, Lwwo;

    iput-object v4, v0, Lxan;->b:Lwwo;

    .line 14
    invoke-static/range {p7 .. p7}, Lanvb;->a(Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v4

    check-cast v4, Lwwg;

    iput-object v4, v0, Lxan;->c:Lwwg;

    .line 15
    invoke-static/range {p9 .. p9}, Lanvb;->a(Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v4

    check-cast v4, Lwvq;

    iput-object v4, v0, Lxan;->F:Lwvq;

    .line 16
    invoke-static/range {p10 .. p10}, Lanvb;->a(Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v4

    check-cast v4, Lxem;

    iput-object v4, v0, Lxan;->G:Lxem;

    move-object/from16 v4, p13

    .line 17
    iput-object v4, v0, Lxan;->f:Laaul;

    move-object/from16 v4, p14

    .line 18
    iput-object v4, v0, Lxan;->I:Lwvj;

    move-object/from16 v4, p8

    .line 19
    iput-object v4, v0, Lxan;->E:Laltp;

    .line 20
    invoke-static/range {p15 .. p15}, Lanvb;->a(Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v4

    check-cast v4, Lxdq;

    iput-object v4, v0, Lxan;->aN:Lxdq;

    .line 21
    invoke-static/range {p16 .. p16}, Lanvb;->a(Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v4

    check-cast v4, Lxej;

    iput-object v4, v0, Lxan;->J:Lxej;

    .line 22
    invoke-static/range {p11 .. p11}, Lanvb;->a(Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v4

    check-cast v4, Lxdv;

    iput-object v4, v0, Lxan;->e:Lxdv;

    .line 23
    invoke-static/range {p12 .. p12}, Lanvb;->a(Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v4

    check-cast v4, Lxbv;

    iput-object v4, v0, Lxan;->H:Lxbv;

    .line 24
    invoke-static/range {p19 .. p19}, Lanvb;->a(Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v4

    check-cast v4, Lxev;

    iput-object v4, v0, Lxan;->aT:Lxev;

    move-object v4, p3

    move-object/from16 v5, p11

    .line 25
    iput-object v4, v5, Lxdv;->a:Laawi;

    .line 26
    new-instance v4, Landroid/widget/FrameLayout;

    invoke-direct {v4, p1}, Landroid/widget/FrameLayout;-><init>(Landroid/content/Context;)V

    iput-object v4, v0, Lxan;->aa:Landroid/widget/FrameLayout;

    .line 27
    iget-object v4, v0, Lxan;->a:Landroid/content/Context;

    invoke-static {v4}, Landroid/view/LayoutInflater;->from(Landroid/content/Context;)Landroid/view/LayoutInflater;

    move-result-object v4

    .line 28
    iget-object v5, v0, Lxan;->aa:Landroid/widget/FrameLayout;

    const v6, 0x7f0e00ec

    .line 29
    invoke-virtual {v4, v6, v5, v3}, Landroid/view/LayoutInflater;->inflate(ILandroid/view/ViewGroup;Z)Landroid/view/View;

    move-result-object v5

    invoke-static {v5}, Lxan;->b(Landroid/view/View;)Lxbc;

    move-result-object v5

    iput-object v5, v0, Lxan;->ae:Lxbc;

    .line 30
    iget-object v5, v0, Lxan;->aa:Landroid/widget/FrameLayout;

    const v6, 0x7f0e00ed

    .line 31
    invoke-virtual {v4, v6, v5, v3}, Landroid/view/LayoutInflater;->inflate(ILandroid/view/ViewGroup;Z)Landroid/view/View;

    move-result-object v5

    invoke-static {v5}, Lxan;->b(Landroid/view/View;)Lxbc;

    move-result-object v5

    iput-object v5, v0, Lxan;->af:Lxbc;

    .line 32
    iget-object v5, v0, Lxan;->aa:Landroid/widget/FrameLayout;

    const v6, 0x7f0e006f

    .line 33
    invoke-virtual {v4, v6, v5, v3}, Landroid/view/LayoutInflater;->inflate(ILandroid/view/ViewGroup;Z)Landroid/view/View;

    move-result-object v4

    invoke-static {v4}, Lxan;->b(Landroid/view/View;)Lxbc;

    move-result-object v4

    iput-object v4, v0, Lxan;->ag:Lxbc;

    .line 34
    new-instance v4, Lxdo;

    .line 35
    invoke-interface {p5}, Laltz;->get()Ljava/lang/Object;

    move-result-object v5

    check-cast v5, Lalmz;

    invoke-direct {v4, p1, v5}, Lxdo;-><init>(Landroid/content/Context;Lalmz;)V

    iput-object v4, v0, Lxan;->aM:Lxdo;

    .line 36
    new-instance v4, Lalrh;

    iget-object v5, v0, Lxan;->aP:Lalrp;

    const/4 v6, 0x1

    move-object/from16 v7, p17

    invoke-direct {v4, p1, v7, v6, v5}, Lalrh;-><init>(Landroid/content/Context;Lalrc;ZLalrm;)V

    iput-object v4, v0, Lxan;->aO:Lalrh;

    .line 37
    invoke-virtual {p1}, Landroid/content/Context;->getResources()Landroid/content/res/Resources;

    move-result-object v4

    .line 38
    new-instance v5, Landroid/util/TypedValue;

    invoke-direct {v5}, Landroid/util/TypedValue;-><init>()V

    .line 39
    invoke-virtual {p1}, Landroid/content/Context;->getTheme()Landroid/content/res/Resources$Theme;

    move-result-object v7

    if-eqz v7, :cond_0

    .line 40
    invoke-virtual {p1}, Landroid/content/Context;->getTheme()Landroid/content/res/Resources$Theme;

    move-result-object v7

    const v8, 0x101004d

    invoke-virtual {v7, v8, v5, v6}, Landroid/content/res/Resources$Theme;->resolveAttribute(ILandroid/util/TypedValue;Z)Z

    move-result v7

    if-eqz v7, :cond_0

    iget v7, v5, Landroid/util/TypedValue;->type:I

    if-ne v7, v2, :cond_0

    .line 41
    invoke-virtual {p1}, Landroid/content/Context;->getResources()Landroid/content/res/Resources;

    move-result-object v2

    invoke-virtual {v2}, Landroid/content/res/Resources;->getDisplayMetrics()Landroid/util/DisplayMetrics;

    move-result-object v2

    invoke-virtual {v5, v2}, Landroid/util/TypedValue;->getDimension(Landroid/util/DisplayMetrics;)F

    move-result v2

    float-to-int v2, v2

    goto :goto_0

    :cond_0
    const/4 v2, 0x0

    .line 42
    :goto_0
    iput v2, v0, Lxan;->K:I

    const v2, 0x7f070268

    .line 43
    invoke-virtual {v4, v2}, Landroid/content/res/Resources;->getDimensionPixelSize(I)I

    move-result v2

    iput v2, v0, Lxan;->L:I

    const v2, 0x7f070b86

    .line 44
    invoke-virtual {v4, v2}, Landroid/content/res/Resources;->getDimensionPixelSize(I)I

    move-result v2

    iput v2, v0, Lxan;->M:I

    const v2, 0x7f070b85

    .line 45
    invoke-virtual {v4, v2}, Landroid/content/res/Resources;->getDimensionPixelSize(I)I

    move-result v2

    iput v2, v0, Lxan;->N:I

    const v2, 0x7f07020e

    .line 46
    invoke-virtual {v4, v2}, Landroid/content/res/Resources;->getDimensionPixelSize(I)I

    move-result v2

    iput v2, v0, Lxan;->O:I

    const v2, 0x7f07020f

    .line 47
    invoke-virtual {v4, v2}, Landroid/content/res/Resources;->getDimensionPixelSize(I)I

    move-result v2

    iput v2, v0, Lxan;->g:I

    const v2, 0x7f070246

    .line 48
    invoke-virtual {v4, v2}, Landroid/content/res/Resources;->getDimensionPixelSize(I)I

    move-result v2

    iput v2, v0, Lxan;->h:I

    const v2, 0x7f070248

    .line 49
    invoke-virtual {v4, v2}, Landroid/content/res/Resources;->getDimensionPixelSize(I)I

    move-result v2

    iput v2, v0, Lxan;->j:I

    const v2, 0x7f070249

    .line 50
    invoke-virtual {v4, v2}, Landroid/content/res/Resources;->getDimensionPixelSize(I)I

    move-result v2

    iput v2, v0, Lxan;->i:I

    const v2, 0x7f07024b

    .line 51
    invoke-virtual {v4, v2}, Landroid/content/res/Resources;->getDimensionPixelSize(I)I

    move-result v2

    iput v2, v0, Lxan;->k:I

    const v2, 0x7f070247

    .line 52
    invoke-virtual {v4, v2}, Landroid/content/res/Resources;->getDimensionPixelSize(I)I

    move-result v2

    iput v2, v0, Lxan;->P:I

    const v2, 0x7f07024a

    .line 53
    invoke-virtual {v4, v2}, Landroid/content/res/Resources;->getDimensionPixelSize(I)I

    move-result v2

    iput v2, v0, Lxan;->Q:I

    const v2, 0x7f07021d

    .line 54
    invoke-virtual {v4, v2}, Landroid/content/res/Resources;->getDimensionPixelSize(I)I

    move-result v2

    iput v2, v0, Lxan;->R:I

    const v2, 0x7f070b84

    .line 55
    invoke-virtual {v4, v2}, Landroid/content/res/Resources;->getDimensionPixelSize(I)I

    move-result v2

    iput v2, v0, Lxan;->S:I

    const v2, 0x7f070213

    .line 56
    invoke-virtual {v4, v2}, Landroid/content/res/Resources;->getDimensionPixelSize(I)I

    move-result v2

    iput v2, v0, Lxan;->T:I

    const v2, 0x7f07026d

    .line 57
    invoke-virtual {v4, v2}, Landroid/content/res/Resources;->getDimensionPixelSize(I)I

    move-result v2

    iput v2, v0, Lxan;->U:I

    const v2, 0x7f07026e

    .line 58
    invoke-virtual {v4, v2}, Landroid/content/res/Resources;->getDimensionPixelSize(I)I

    move-result v2

    iput v2, v0, Lxan;->V:I

    const v2, 0x7f040126

    .line 59
    invoke-static {p1, v2, v3}, Lypu;->a(Landroid/content/Context;II)I

    move-result v2

    iput v2, v0, Lxan;->W:I

    const v2, 0x7f040127

    .line 60
    invoke-static {p1, v2, v3}, Lypu;->a(Landroid/content/Context;II)I

    move-result v2

    iput v2, v0, Lxan;->X:I

    const v2, 0x7f04057c

    .line 61
    invoke-static {p1, v2, v3}, Lypu;->a(Landroid/content/Context;II)I

    move-result v2

    iput v2, v0, Lxan;->Y:I

    const v2, 0x7f04058e

    .line 62
    invoke-static {p1, v2, v3}, Lypu;->a(Landroid/content/Context;II)I

    move-result v1

    iput v1, v0, Lxan;->Z:I

    .line 63
    iget-object v1, v0, Lxan;->ae:Lxbc;

    invoke-direct {p0, v1, v3}, Lxan;->a(Lxbc;Z)V

    .line 64
    iget-object v1, v0, Lxan;->af:Lxbc;

    invoke-direct {p0, v1, v3}, Lxan;->a(Lxbc;Z)V

    .line 65
    iget-object v1, v0, Lxan;->ag:Lxbc;

    invoke-direct {p0, v1, v6}, Lxan;->a(Lxbc;Z)V

    return-void
.end method

.method private final a(I)V
    .locals 1

    .line 480
    iget-object v0, p0, Lxan;->ai:Landroid/widget/ImageView;

    invoke-virtual {v0}, Landroid/widget/ImageView;->getLayoutParams()Landroid/view/ViewGroup$LayoutParams;

    move-result-object v0

    iput p1, v0, Landroid/view/ViewGroup$LayoutParams;->width:I

    .line 481
    iget-object v0, p0, Lxan;->ai:Landroid/widget/ImageView;

    invoke-virtual {v0}, Landroid/widget/ImageView;->getLayoutParams()Landroid/view/ViewGroup$LayoutParams;

    move-result-object v0

    iput p1, v0, Landroid/view/ViewGroup$LayoutParams;->height:I

    .line 482
    iget-object p1, p0, Lxan;->ai:Landroid/widget/ImageView;

    invoke-virtual {p1}, Landroid/widget/ImageView;->requestLayout()V

    return-void
.end method

.method private static final a(Landroid/view/View;Lapon;)V
    .locals 1

    if-eqz p1, :cond_1

    .line 1363
    iget v0, p1, Lapon;->a:I

    and-int/lit8 v0, v0, 0x1

    if-eqz v0, :cond_1

    .line 1364
    iget-object p1, p1, Lapon;->b:Lapol;

    if-nez p1, :cond_0

    .line 1365
    sget-object p1, Lapol;->c:Lapol;

    .line 1366
    :cond_0
    iget-object p1, p1, Lapol;->b:Ljava/lang/String;

    .line 1367
    invoke-virtual {p0, p1}, Landroid/view/View;->setContentDescription(Ljava/lang/CharSequence;)V

    return-void

    :cond_1
    const-string p1, ""

    .line 1368
    invoke-virtual {p0, p1}, Landroid/view/View;->setContentDescription(Ljava/lang/CharSequence;)V

    return-void
.end method

.method private final a(Landroid/widget/ImageView;Lattl;Laroz;I)V
    .locals 1

    if-eqz p2, :cond_1

    .line 234
    iget p2, p2, Lattl;->b:I

    invoke-static {p2}, Lattn;->a(I)Lattn;

    move-result-object p2

    if-eqz p2, :cond_0

    goto :goto_0

    .line 241
    :cond_0
    sget-object p2, Lattn;->a:Lattn;

    goto :goto_0

    .line 242
    :cond_1
    sget-object p2, Lattn;->ch:Lattn;

    .line 235
    :goto_0
    iget-object v0, p0, Lxan;->E:Laltp;

    invoke-interface {v0, p2}, Laltp;->a(Lattn;)I

    move-result p2

    invoke-virtual {p1, p2}, Landroid/widget/ImageView;->setImageResource(I)V

    if-nez p3, :cond_2

    goto :goto_1

    .line 238
    :cond_2
    iget p2, p3, Laroz;->a:I

    const v0, 0x70fec16

    if-ne p2, v0, :cond_3

    .line 239
    iget-object p2, p3, Laroz;->b:Ljava/lang/Object;

    check-cast p2, Laqtc;

    .line 240
    iget p2, p2, Laqtc;->d:I

    goto :goto_2

    .line 236
    :cond_3
    :goto_1
    iget-object p2, p0, Lxan;->a:Landroid/content/Context;

    const/4 p3, 0x0

    invoke-static {p2, p4, p3}, Lypu;->a(Landroid/content/Context;II)I

    move-result p2

    .line 237
    :goto_2
    invoke-virtual {p1, p2}, Landroid/widget/ImageView;->setColorFilter(I)V

    return-void
.end method

.method private final a(Laror;Ladzv;Ljava/util/Map;)V
    .locals 2

    .line 454
    iget v0, p1, Laror;->a:I

    const v1, 0x8000

    and-int/2addr v0, v1

    if-eqz v0, :cond_5

    .line 455
    iget-object p1, p1, Laror;->q:Lazpz;

    if-eqz p1, :cond_0

    goto :goto_0

    .line 479
    :cond_0
    sget-object p1, Lazpz;->a:Lazpz;

    .line 456
    :goto_0
    sget-object v0, Lcom/google/protos/youtube/api/innertube/ButtonRendererOuterClass;->buttonRenderer:Lapig;

    .line 457
    invoke-static {v0}, Lapia;->access$000(Laphm;)Lapig;

    move-result-object v0

    .line 458
    invoke-virtual {p1, v0}, Lapie;->a(Lapig;)V

    .line 459
    iget-object p1, p1, Lapie;->h:Laphr;

    iget-object v1, v0, Lapig;->d:Lapid;

    invoke-virtual {p1, v1}, Laphr;->b(Laphu;)Ljava/lang/Object;

    move-result-object p1

    if-nez p1, :cond_1

    .line 460
    iget-object p1, v0, Lapig;->b:Ljava/lang/Object;

    goto :goto_1

    .line 478
    :cond_1
    invoke-virtual {v0, p1}, Lapig;->a(Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object p1

    .line 461
    :goto_1
    check-cast p1, Laqvi;

    .line 462
    iget-object v0, p0, Lxan;->aj:Lxbd;

    iget-object v0, v0, Lxbd;->k:Landroid/view/View;

    .line 463
    iget-object v1, p1, Laqvi;->q:Lapon;

    if-eqz v1, :cond_2

    goto :goto_2

    .line 477
    :cond_2
    sget-object v1, Lapon;->c:Lapon;

    .line 464
    :goto_2
    invoke-static {v0, v1}, Lxan;->a(Landroid/view/View;Lapon;)V

    .line 465
    iget-boolean v0, p0, Lxan;->m:Z

    if-eqz v0, :cond_4

    .line 466
    iget-object v0, p0, Lxan;->aj:Lxbd;

    iget-object v0, v0, Lxbd;->k:Landroid/view/View;

    const v1, 0x7f0b0302

    .line 467
    invoke-virtual {v0, v1}, Landroid/view/View;->findViewById(I)Landroid/view/View;

    move-result-object v0

    check-cast v0, Landroid/widget/TextView;

    .line 468
    iget-object v1, p1, Laqvi;->g:Latho;

    if-nez v1, :cond_3

    .line 469
    sget-object v1, Latho;->f:Latho;

    .line 470
    :cond_3
    invoke-static {v1}, Lakzk;->a(Latho;)Landroid/text/Spanned;

    move-result-object v1

    invoke-virtual {v0, v1}, Landroid/widget/TextView;->setText(Ljava/lang/CharSequence;)V

    .line 471
    :cond_4
    iget-object v0, p0, Lxan;->aj:Lxbd;

    iget-object v0, v0, Lxbd;->k:Landroid/view/View;

    new-instance v1, Lxaw;

    invoke-direct {v1, p0, p1, p2, p3}, Lxaw;-><init>(Lxan;Laqvi;Ladzv;Ljava/util/Map;)V

    invoke-virtual {v0, v1}, Landroid/view/View;->setOnClickListener(Landroid/view/View$OnClickListener;)V

    .line 472
    iget-object p3, p0, Lxan;->aj:Lxbd;

    iget-object p3, p3, Lxbd;->k:Landroid/view/View;

    const/4 v0, 0x0

    invoke-virtual {p3, v0}, Landroid/view/View;->setVisibility(I)V

    .line 473
    new-instance p3, Ladzq;

    .line 474
    iget-object p1, p1, Laqvi;->r:Lapgi;

    .line 475
    invoke-direct {p3, p1}, Ladzq;-><init>(Lapgi;)V

    .line 476
    invoke-interface {p2, p3}, Ladzv;->b(Laebg;)V

    :cond_5
    return-void
.end method

.method private final a(Laror;Ljava/util/Map;)V
    .locals 5

    .line 404
    iget-object v0, p1, Laror;->d:Laqvp;

    if-eqz v0, :cond_0

    goto :goto_0

    .line 426
    :cond_0
    sget-object v0, Laqvp;->d:Laqvp;

    .line 405
    :goto_0
    iget v0, v0, Laqvp;->a:I

    const/4 v1, 0x1

    and-int/2addr v0, v1

    if-nez v0, :cond_1

    const/4 p1, 0x0

    goto :goto_2

    .line 423
    :cond_1
    iget-object p1, p1, Laror;->d:Laqvp;

    if-eqz p1, :cond_2

    goto :goto_1

    .line 426
    :cond_2
    sget-object p1, Laqvp;->d:Laqvp;

    .line 424
    :goto_1
    iget-object p1, p1, Laqvp;->b:Laqvi;

    if-nez p1, :cond_3

    .line 425
    sget-object p1, Laqvi;->s:Laqvi;

    :cond_3
    :goto_2
    if-eqz p1, :cond_9

    .line 407
    iget-object v0, p0, Lxan;->aj:Lxbd;

    iget-object v0, v0, Lxbd;->j:Landroid/widget/TextView;

    const-string v2, ""

    if-nez v0, :cond_4

    goto :goto_5

    .line 417
    :cond_4
    iget v3, p1, Laqvi;->a:I

    and-int/lit16 v3, v3, 0x80

    if-nez v3, :cond_5

    move-object v3, v2

    goto :goto_4

    .line 419
    :cond_5
    iget-object v3, p1, Laqvi;->g:Latho;

    if-eqz v3, :cond_6

    goto :goto_3

    .line 421
    :cond_6
    sget-object v3, Latho;->f:Latho;

    .line 420
    :goto_3
    invoke-static {v3}, Lakzk;->a(Latho;)Landroid/text/Spanned;

    move-result-object v3

    .line 418
    :goto_4
    invoke-virtual {v0, v3}, Landroid/widget/TextView;->setText(Ljava/lang/CharSequence;)V

    .line 408
    :goto_5
    iget-object v0, p0, Lxan;->aj:Lxbd;

    iget-object v0, v0, Lxbd;->i:Landroid/view/View;

    .line 409
    iget v3, p1, Laqvi;->a:I

    const v4, 0x8000

    and-int/2addr v3, v4

    if-eqz v3, :cond_8

    .line 410
    iget-object v2, p1, Laqvi;->p:Lapol;

    if-eqz v2, :cond_7

    goto :goto_6

    .line 415
    :cond_7
    sget-object v2, Lapol;->c:Lapol;

    .line 411
    :goto_6
    iget-object v2, v2, Lapol;->b:Ljava/lang/String;

    .line 412
    :cond_8
    invoke-virtual {v0, v2}, Landroid/view/View;->setContentDescription(Ljava/lang/CharSequence;)V

    .line 413
    iget-object v0, p0, Lxan;->aj:Lxbd;

    iget-object v0, v0, Lxbd;->i:Landroid/view/View;

    new-instance v2, Lxau;

    invoke-direct {v2, p0, p1, p2}, Lxau;-><init>(Lxan;Laqvi;Ljava/util/Map;)V

    invoke-virtual {v0, v2}, Landroid/view/View;->setOnClickListener(Landroid/view/View$OnClickListener;)V

    .line 414
    invoke-direct {p0, v1}, Lxan;->a(Z)V

    return-void

    :cond_9
    const/4 p1, 0x0

    .line 422
    invoke-direct {p0, p1}, Lxan;->a(Z)V

    return-void
.end method

.method private final a(Larph;Landroid/view/View;Landroid/widget/TextView;Landroid/widget/ImageView;)V
    .locals 4

    if-eqz p2, :cond_b

    const/16 v0, 0x8

    if-eqz p1, :cond_a

    const/4 v1, 0x0

    .line 202
    invoke-virtual {p2, v1}, Landroid/view/View;->setVisibility(I)V

    .line 203
    iget v1, p1, Larph;->a:I

    and-int/lit8 v1, v1, 0x2

    const/4 v2, 0x0

    if-eqz v1, :cond_1

    .line 204
    iget-object v1, p1, Larph;->c:Latho;

    if-eqz v1, :cond_0

    goto :goto_0

    .line 231
    :cond_0
    sget-object v1, Latho;->f:Latho;

    goto :goto_0

    :cond_1
    move-object v1, v2

    .line 205
    :goto_0
    invoke-static {v1}, Lakzk;->a(Latho;)Landroid/text/Spanned;

    move-result-object v1

    .line 206
    invoke-virtual {p3, v1}, Landroid/widget/TextView;->setText(Ljava/lang/CharSequence;)V

    .line 207
    iget p3, p1, Larph;->a:I

    and-int/lit8 p3, p3, 0x1

    if-eqz p3, :cond_3

    .line 208
    iget-object p3, p1, Larph;->b:Lattl;

    if-eqz p3, :cond_2

    goto :goto_1

    .line 229
    :cond_2
    sget-object p3, Lattl;->c:Lattl;

    goto :goto_1

    :cond_3
    move-object p3, v2

    .line 209
    :goto_1
    iget v1, p1, Larph;->a:I

    and-int/lit8 v1, v1, 0x4

    if-eqz v1, :cond_5

    .line 210
    iget-object v1, p1, Larph;->d:Laroz;

    if-eqz v1, :cond_4

    goto :goto_2

    .line 227
    :cond_4
    sget-object v1, Laroz;->c:Laroz;

    goto :goto_2

    :cond_5
    move-object v1, v2

    :goto_2
    const v3, 0x7f04012d

    .line 211
    invoke-direct {p0, p4, p3, v1, v3}, Lxan;->a(Landroid/widget/ImageView;Lattl;Laroz;I)V

    .line 212
    invoke-virtual {p2, v2}, Landroid/view/View;->setOnClickListener(Landroid/view/View$OnClickListener;)V

    .line 213
    iget p3, p1, Larph;->a:I

    and-int/lit8 p3, p3, 0x2

    if-eqz p3, :cond_7

    .line 214
    iget-object v2, p1, Larph;->c:Latho;

    if-eqz v2, :cond_6

    goto :goto_3

    .line 225
    :cond_6
    sget-object v2, Latho;->f:Latho;

    .line 215
    :cond_7
    :goto_3
    invoke-static {v2}, Lakzk;->b(Latho;)Ljava/lang/CharSequence;

    move-result-object p3

    .line 216
    invoke-virtual {p2, p3}, Landroid/view/View;->setContentDescription(Ljava/lang/CharSequence;)V

    .line 217
    iget p3, p1, Larph;->a:I

    and-int/2addr p3, v0

    if-eqz p3, :cond_9

    .line 218
    iget-object p1, p1, Larph;->e:Latho;

    if-eqz p1, :cond_8

    goto :goto_4

    .line 225
    :cond_8
    sget-object p1, Latho;->f:Latho;

    .line 219
    :goto_4
    invoke-static {p1}, Lakzk;->a(Latho;)Landroid/text/Spanned;

    move-result-object p1

    invoke-virtual {p1}, Ljava/lang/Object;->toString()Ljava/lang/String;

    move-result-object p1

    .line 220
    iget-object p3, p0, Lxan;->a:Landroid/content/Context;

    const-string p4, "accessibility"

    .line 221
    invoke-virtual {p3, p4}, Landroid/content/Context;->getSystemService(Ljava/lang/String;)Ljava/lang/Object;

    move-result-object p3

    check-cast p3, Landroid/view/accessibility/AccessibilityManager;

    .line 222
    invoke-virtual {p3}, Landroid/view/accessibility/AccessibilityManager;->isEnabled()Z

    move-result p3

    if-nez p3, :cond_9

    .line 223
    new-instance p3, Lxat;

    invoke-direct {p3, p0, p1, p2}, Lxat;-><init>(Lxan;Ljava/lang/String;Landroid/view/View;)V

    .line 224
    invoke-virtual {p2, p3}, Landroid/view/View;->setOnClickListener(Landroid/view/View$OnClickListener;)V

    :cond_9
    return-void

    .line 233
    :cond_a
    invoke-virtual {p2, v0}, Landroid/view/View;->setVisibility(I)V

    :cond_b
    return-void
.end method

.method private final a(Larqp;Z)V
    .locals 10

    .line 181
    iget-object v0, p1, Larqp;->q:Latho;

    if-eqz v0, :cond_0

    goto :goto_0

    .line 201
    :cond_0
    sget-object v0, Latho;->f:Latho;

    .line 182
    :goto_0
    iget-object v1, p0, Lxan;->d:Laawi;

    const/4 v2, 0x0

    invoke-static {v0, v1, v2}, Laawn;->a(Latho;Laawi;Z)Landroid/text/Spanned;

    move-result-object v5

    .line 183
    invoke-static {v5}, Landroid/text/TextUtils;->isEmpty(Ljava/lang/CharSequence;)Z

    move-result v0

    if-nez v0, :cond_1

    goto :goto_1

    .line 199
    :cond_1
    iget v0, p1, Larqp;->b:I

    and-int/lit16 v0, v0, 0x80

    if-eqz v0, :cond_2

    .line 200
    iget-object p1, p0, Lxan;->r:Landroid/widget/TextView;

    const/16 p2, 0x8

    invoke-virtual {p1, p2}, Landroid/widget/TextView;->setVisibility(I)V

    return-void

    .line 184
    :cond_2
    :goto_1
    iget-object v0, p0, Lxan;->r:Landroid/widget/TextView;

    invoke-virtual {v0, v2}, Landroid/widget/TextView;->setVisibility(I)V

    .line 185
    iget-object v0, p0, Lxan;->aQ:Landroid/text/SpannableStringBuilder;

    invoke-virtual {v0}, Landroid/text/SpannableStringBuilder;->clear()V

    .line 186
    iget-object v0, p0, Lxan;->aR:Ljava/lang/StringBuilder;

    invoke-virtual {v0, v2}, Ljava/lang/StringBuilder;->setLength(I)V

    .line 187
    invoke-static {v5}, Landroid/text/TextUtils;->isEmpty(Ljava/lang/CharSequence;)Z

    move-result v0

    if-eqz v0, :cond_3

    .line 188
    iget-object p1, p0, Lxan;->r:Landroid/widget/TextView;

    const/4 v0, 0x0

    invoke-virtual {p1, v0}, Landroid/widget/TextView;->setText(Ljava/lang/CharSequence;)V

    goto :goto_4

    .line 190
    :cond_3
    iget-object v0, p0, Lxan;->aQ:Landroid/text/SpannableStringBuilder;

    invoke-virtual {v0, v5}, Landroid/text/SpannableStringBuilder;->append(Ljava/lang/CharSequence;)Landroid/text/SpannableStringBuilder;

    .line 191
    iget-object v0, p0, Lxan;->aR:Ljava/lang/StringBuilder;

    invoke-virtual {v0, v5}, Ljava/lang/StringBuilder;->append(Ljava/lang/CharSequence;)Ljava/lang/StringBuilder;

    .line 192
    iget-object v3, p0, Lxan;->aO:Lalrh;

    .line 193
    iget-object v0, p1, Larqp;->q:Latho;

    if-eqz v0, :cond_4

    :goto_2
    move-object v4, v0

    goto :goto_3

    .line 198
    :cond_4
    sget-object v0, Latho;->f:Latho;

    goto :goto_2

    .line 194
    :goto_3
    iget-object v6, p0, Lxan;->aQ:Landroid/text/SpannableStringBuilder;

    iget-object v7, p0, Lxan;->aR:Ljava/lang/StringBuilder;

    iget-object v0, p0, Lxan;->r:Landroid/widget/TextView;

    .line 195
    invoke-virtual {v0}, Landroid/widget/TextView;->getId()I

    move-result v9

    move-object v8, p1

    .line 196
    invoke-virtual/range {v3 .. v9}, Lalrh;->a(Latho;Ljava/lang/CharSequence;Landroid/text/SpannableStringBuilder;Ljava/lang/StringBuilder;Ljava/lang/Object;I)V

    .line 197
    iget-object p1, p0, Lxan;->r:Landroid/widget/TextView;

    iget-object v0, p0, Lxan;->aQ:Landroid/text/SpannableStringBuilder;

    invoke-virtual {p1, v0}, Landroid/widget/TextView;->setText(Ljava/lang/CharSequence;)V

    .line 189
    :goto_4
    iget-object p1, p0, Lxan;->r:Landroid/widget/TextView;

    if-eqz p2, :cond_5

    iget p2, p0, Lxan;->l:I

    goto :goto_5

    :cond_5
    const p2, 0x7fffffff

    :goto_5
    invoke-virtual {p1, p2}, Landroid/widget/TextView;->setMaxLines(I)V

    return-void
.end method

.method private final a(Ljava/lang/StringBuilder;Larqp;)V
    .locals 4

    .line 487
    iget-object v0, p2, Larqp;->J:Laqqw;

    if-eqz v0, :cond_0

    goto :goto_0

    .line 516
    :cond_0
    sget-object v0, Laqqw;->c:Laqqw;

    .line 488
    :goto_0
    iget v0, v0, Laqqw;->a:I

    const v1, 0x5ec9696

    if-ne v0, v1, :cond_8

    .line 489
    iget-object p2, p2, Larqp;->J:Laqqw;

    if-nez p2, :cond_1

    .line 490
    sget-object p2, Laqqw;->c:Laqqw;

    .line 491
    :cond_1
    iget v0, p2, Laqqw;->a:I

    if-ne v0, v1, :cond_2

    .line 492
    iget-object p2, p2, Laqqw;->b:Ljava/lang/Object;

    check-cast p2, Lazal;

    goto :goto_1

    .line 515
    :cond_2
    sget-object p2, Lazal;->l:Lazal;

    .line 493
    :goto_1
    iget-object v0, p0, Lxan;->ax:Landroid/widget/TextView;

    invoke-virtual {v0}, Landroid/widget/TextView;->getText()Ljava/lang/CharSequence;

    move-result-object v0

    invoke-interface {v0}, Ljava/lang/CharSequence;->toString()Ljava/lang/String;

    move-result-object v0

    .line 494
    invoke-virtual {p1, v0}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    const-string v0, ". "

    .line 496
    invoke-virtual {p1, v0}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    .line 497
    iget-object p2, p2, Lazal;->d:Lapir;

    .line 498
    invoke-interface {p2}, Ljava/util/List;->iterator()Ljava/util/Iterator;

    move-result-object p2

    :cond_3
    :goto_2
    invoke-interface {p2}, Ljava/util/Iterator;->hasNext()Z

    move-result v1

    if-eqz v1, :cond_8

    invoke-interface {p2}, Ljava/util/Iterator;->next()Ljava/lang/Object;

    move-result-object v1

    check-cast v1, Lazah;

    .line 499
    iget v2, v1, Lazah;->a:I

    and-int/lit8 v2, v2, 0x1

    const/4 v3, 0x0

    if-eqz v2, :cond_5

    .line 500
    iget-object v2, v1, Lazah;->b:Latho;

    if-eqz v2, :cond_4

    goto :goto_3

    .line 512
    :cond_4
    sget-object v2, Latho;->f:Latho;

    goto :goto_3

    :cond_5
    move-object v2, v3

    .line 501
    :goto_3
    invoke-static {v2}, Lakzk;->a(Latho;)Landroid/text/Spanned;

    move-result-object v2

    .line 502
    invoke-virtual {p1, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/CharSequence;)Ljava/lang/StringBuilder;

    .line 503
    invoke-virtual {p1, v0}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    .line 504
    iget v2, v1, Lazah;->a:I

    and-int/lit8 v2, v2, 0x20

    if-eqz v2, :cond_7

    .line 505
    iget-object v3, v1, Lazah;->g:Latho;

    if-eqz v3, :cond_6

    goto :goto_4

    .line 510
    :cond_6
    sget-object v3, Latho;->f:Latho;

    .line 506
    :cond_7
    :goto_4
    invoke-static {v3}, Lakzk;->a(Latho;)Landroid/text/Spanned;

    move-result-object v1

    .line 507
    invoke-static {v1}, Landroid/text/TextUtils;->isEmpty(Ljava/lang/CharSequence;)Z

    move-result v2

    if-nez v2, :cond_3

    .line 508
    invoke-virtual {p1, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/CharSequence;)Ljava/lang/StringBuilder;

    .line 509
    invoke-virtual {p1, v0}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    goto :goto_2

    :cond_8
    return-void
.end method

.method private final a(Lxbc;Z)V
    .locals 3

    .line 173
    iget-object v0, p1, Lxbc;->a:Landroid/view/View;

    .line 174
    invoke-virtual {v0}, Landroid/view/View;->getViewTreeObserver()Landroid/view/ViewTreeObserver;

    move-result-object v1

    new-instance v2, Lxaz;

    invoke-direct {v2, p0, p1, p2, v0}, Lxaz;-><init>(Lxan;Lxbc;ZLandroid/view/View;)V

    .line 175
    invoke-virtual {v1, v2}, Landroid/view/ViewTreeObserver;->addOnGlobalLayoutListener(Landroid/view/ViewTreeObserver$OnGlobalLayoutListener;)V

    return-void
.end method

.method private final a(Z)V
    .locals 2

    if-nez p1, :cond_0

    const/4 v0, 0x4

    goto :goto_0

    :cond_0
    const/4 v0, 0x0

    .line 374
    :goto_0
    iget-object v1, p0, Lxan;->aj:Lxbd;

    iget-object v1, v1, Lxbd;->i:Landroid/view/View;

    .line 375
    invoke-virtual {v1, v0}, Landroid/view/View;->setVisibility(I)V

    .line 376
    invoke-virtual {v1, p1}, Landroid/view/View;->setClickable(Z)V

    .line 377
    iget-object p1, p0, Lxan;->aj:Lxbd;

    iget-object p1, p1, Lxbd;->j:Landroid/widget/TextView;

    if-eqz p1, :cond_1

    .line 378
    invoke-virtual {p1, v0}, Landroid/widget/TextView;->setVisibility(I)V

    :cond_1
    return-void
.end method

.method private static final a(Lalmp;)Z
    .locals 2

    const/4 v0, 0x0

    const-string v1, "ignoreIndentedComment"

    .line 1369
    invoke-virtual {p0, v1, v0}, Lalmp;->a(Ljava/lang/String;Z)Z

    move-result v1

    if-nez v1, :cond_0

    const-string v1, "indentedComment"

    .line 1370
    invoke-virtual {p0, v1, v0}, Lalmp;->a(Ljava/lang/String;Z)Z

    move-result p0

    if-eqz p0, :cond_0

    const/4 p0, 0x1

    return p0

    :cond_0
    return v0
.end method

.method private final a(Laqvp;Landroid/widget/ImageView;Ladzv;Ljava/util/Map;)Z
    .locals 3

    .line 387
    iget-object p1, p1, Laqvp;->b:Laqvi;

    if-eqz p1, :cond_0

    goto :goto_0

    .line 403
    :cond_0
    sget-object p1, Laqvi;->s:Laqvi;

    .line 388
    :goto_0
    iget v0, p1, Laqvi;->a:I

    and-int/lit8 v0, v0, 0x10

    const/4 v1, 0x0

    if-nez v0, :cond_1

    const/4 p1, 0x4

    .line 389
    invoke-virtual {p2, p1}, Landroid/widget/ImageView;->setVisibility(I)V

    const/4 p1, 0x0

    .line 390
    invoke-virtual {p2, p1}, Landroid/widget/ImageView;->setOnClickListener(Landroid/view/View$OnClickListener;)V

    return v1

    .line 391
    :cond_1
    iget-object v0, p0, Lxan;->E:Laltp;

    .line 392
    iget-object v2, p1, Laqvi;->e:Lattl;

    if-eqz v2, :cond_2

    goto :goto_1

    .line 402
    :cond_2
    sget-object v2, Lattl;->c:Lattl;

    .line 393
    :goto_1
    iget v2, v2, Lattl;->b:I

    invoke-static {v2}, Lattn;->a(I)Lattn;

    move-result-object v2

    if-eqz v2, :cond_3

    goto :goto_2

    .line 401
    :cond_3
    sget-object v2, Lattn;->a:Lattn;

    .line 394
    :goto_2
    invoke-interface {v0, v2}, Laltp;->a(Lattn;)I

    move-result v0

    .line 395
    invoke-virtual {p2, v0}, Landroid/widget/ImageView;->setImageResource(I)V

    .line 396
    invoke-virtual {p2, v1}, Landroid/widget/ImageView;->setVisibility(I)V

    .line 397
    iget-object v0, p1, Laqvi;->q:Lapon;

    if-eqz v0, :cond_4

    goto :goto_3

    .line 400
    :cond_4
    sget-object v0, Lapon;->c:Lapon;

    .line 398
    :goto_3
    invoke-static {p2, v0}, Lxan;->a(Landroid/view/View;Lapon;)V

    .line 399
    new-instance v0, Lxav;

    invoke-direct {v0, p0, p1, p3, p4}, Lxav;-><init>(Lxan;Laqvi;Ladzv;Ljava/util/Map;)V

    invoke-virtual {p2, v0}, Landroid/widget/ImageView;->setOnClickListener(Landroid/view/View$OnClickListener;)V

    const/4 p1, 0x1

    return p1
.end method

.method private static final b(Landroid/view/View;)Lxbc;
    .locals 2

    .line 1238
    new-instance v0, Lxbc;

    const/4 v1, 0x0

    invoke-direct {v0, v1}, Lxbc;-><init>(B)V

    .line 1239
    iput-object p0, v0, Lxbc;->a:Landroid/view/View;

    const v1, 0x7f0b030d

    .line 1240
    invoke-virtual {p0, v1}, Landroid/view/View;->findViewById(I)Landroid/view/View;

    move-result-object v1

    check-cast v1, Landroid/widget/TextView;

    iput-object v1, v0, Lxbc;->g:Landroid/widget/TextView;

    const v1, 0x7f0b0722

    .line 1241
    invoke-virtual {p0, v1}, Landroid/view/View;->findViewById(I)Landroid/view/View;

    move-result-object v1

    iput-object v1, v0, Lxbc;->d:Landroid/view/View;

    const v1, 0x7f0b030f

    .line 1242
    invoke-virtual {p0, v1}, Landroid/view/View;->findViewById(I)Landroid/view/View;

    move-result-object v1

    check-cast v1, Landroid/widget/ImageView;

    iput-object v1, v0, Lxbc;->e:Landroid/widget/ImageView;

    const v1, 0x7f0b0313

    .line 1243
    invoke-virtual {p0, v1}, Landroid/view/View;->findViewById(I)Landroid/view/View;

    move-result-object v1

    check-cast v1, Landroid/widget/TextView;

    iput-object v1, v0, Lxbc;->h:Landroid/widget/TextView;

    const v1, 0x7f0b031b

    .line 1244
    invoke-virtual {p0, v1}, Landroid/view/View;->findViewById(I)Landroid/view/View;

    move-result-object v1

    check-cast v1, Landroid/widget/TextView;

    iput-object v1, v0, Lxbc;->i:Landroid/widget/TextView;

    const v1, 0x7f0b006a

    .line 1245
    invoke-virtual {p0, v1}, Landroid/view/View;->findViewById(I)Landroid/view/View;

    move-result-object v1

    check-cast v1, Landroid/view/ViewGroup;

    iput-object v1, v0, Lxbc;->j:Landroid/view/ViewGroup;

    const v1, 0x7f0b0841

    .line 1246
    invoke-virtual {p0, v1}, Landroid/view/View;->findViewById(I)Landroid/view/View;

    move-result-object v1

    check-cast v1, Landroid/view/ViewGroup;

    iput-object v1, v0, Lxbc;->l:Landroid/view/ViewGroup;

    const v1, 0x7f0b030b

    .line 1247
    invoke-virtual {p0, v1}, Landroid/view/View;->findViewById(I)Landroid/view/View;

    move-result-object v1

    check-cast v1, Landroid/widget/ImageView;

    iput-object v1, v0, Lxbc;->m:Landroid/widget/ImageView;

    const v1, 0x7f0b0336

    .line 1248
    invoke-virtual {p0, v1}, Landroid/view/View;->findViewById(I)Landroid/view/View;

    move-result-object v1

    check-cast v1, Landroid/widget/ImageView;

    iput-object v1, v0, Lxbc;->n:Landroid/widget/ImageView;

    const v1, 0x7f0b033b

    .line 1249
    invoke-virtual {p0, v1}, Landroid/view/View;->findViewById(I)Landroid/view/View;

    move-result-object v1

    check-cast v1, Landroid/widget/ImageView;

    iput-object v1, v0, Lxbc;->o:Landroid/widget/ImageView;

    const v1, 0x7f0b0311

    .line 1250
    invoke-virtual {p0, v1}, Landroid/view/View;->findViewById(I)Landroid/view/View;

    move-result-object v1

    check-cast v1, Landroid/widget/ImageView;

    iput-object v1, v0, Lxbc;->p:Landroid/widget/ImageView;

    const v1, 0x7f0b0347

    .line 1251
    invoke-virtual {p0, v1}, Landroid/view/View;->findViewById(I)Landroid/view/View;

    move-result-object v1

    check-cast v1, Landroid/widget/TextView;

    iput-object v1, v0, Lxbc;->q:Landroid/widget/TextView;

    const v1, 0x7f0b0985

    .line 1252
    invoke-virtual {p0, v1}, Landroid/view/View;->findViewById(I)Landroid/view/View;

    move-result-object v1

    check-cast v1, Landroid/widget/ImageView;

    iput-object v1, v0, Lxbc;->r:Landroid/widget/ImageView;

    const v1, 0x7f0b0986

    .line 1253
    invoke-virtual {p0, v1}, Landroid/view/View;->findViewById(I)Landroid/view/View;

    move-result-object v1

    check-cast v1, Landroid/widget/TextView;

    iput-object v1, v0, Lxbc;->s:Landroid/widget/TextView;

    const v1, 0x7f0b0744

    .line 1254
    invoke-virtual {p0, v1}, Landroid/view/View;->findViewById(I)Landroid/view/View;

    move-result-object v1

    check-cast v1, Landroid/widget/TextView;

    iput-object v1, v0, Lxbc;->t:Landroid/widget/TextView;

    const v1, 0x7f0b033f

    .line 1255
    invoke-virtual {p0, v1}, Landroid/view/View;->findViewById(I)Landroid/view/View;

    move-result-object v1

    check-cast v1, Landroid/widget/ImageView;

    iput-object v1, v0, Lxbc;->u:Landroid/widget/ImageView;

    const v1, 0x7f0b0cf6

    .line 1256
    invoke-virtual {p0, v1}, Landroid/view/View;->findViewById(I)Landroid/view/View;

    move-result-object v1

    iput-object v1, v0, Lxbc;->v:Landroid/view/View;

    const v1, 0x7f0b0cfa

    .line 1257
    invoke-virtual {p0, v1}, Landroid/view/View;->findViewById(I)Landroid/view/View;

    move-result-object v1

    check-cast v1, Landroid/widget/TextView;

    iput-object v1, v0, Lxbc;->x:Landroid/widget/TextView;

    const v1, 0x7f0b0cf7

    .line 1258
    invoke-virtual {p0, v1}, Landroid/view/View;->findViewById(I)Landroid/view/View;

    move-result-object v1

    check-cast v1, Landroid/widget/ImageView;

    iput-object v1, v0, Lxbc;->w:Landroid/widget/ImageView;

    const v1, 0x7f0b0157

    .line 1259
    invoke-virtual {p0, v1}, Landroid/view/View;->findViewById(I)Landroid/view/View;

    move-result-object v1

    check-cast v1, Landroid/widget/FrameLayout;

    iput-object v1, v0, Lxbc;->L:Landroid/widget/FrameLayout;

    const v1, 0x7f0b0159

    .line 1260
    invoke-virtual {p0, v1}, Landroid/view/View;->findViewById(I)Landroid/view/View;

    move-result-object v1

    check-cast v1, Landroid/widget/FrameLayout;

    iput-object v1, v0, Lxbc;->M:Landroid/widget/FrameLayout;

    const v1, 0x7f0b015b

    .line 1261
    invoke-virtual {p0, v1}, Landroid/view/View;->findViewById(I)Landroid/view/View;

    move-result-object v1

    check-cast v1, Landroid/widget/FrameLayout;

    iput-object v1, v0, Lxbc;->N:Landroid/widget/FrameLayout;

    const v1, 0x7f0b0400

    .line 1262
    invoke-virtual {p0, v1}, Landroid/view/View;->findViewById(I)Landroid/view/View;

    move-result-object v1

    check-cast v1, Landroid/widget/FrameLayout;

    iput-object v1, v0, Lxbc;->O:Landroid/widget/FrameLayout;

    const v1, 0x7f0b0401

    .line 1263
    invoke-virtual {p0, v1}, Landroid/view/View;->findViewById(I)Landroid/view/View;

    move-result-object v1

    check-cast v1, Landroid/widget/TextView;

    iput-object v1, v0, Lxbc;->k:Landroid/widget/TextView;

    const v1, 0x7f0b0327

    .line 1264
    invoke-virtual {p0, v1}, Landroid/view/View;->findViewById(I)Landroid/view/View;

    move-result-object v1

    check-cast v1, Landroid/widget/FrameLayout;

    iput-object v1, v0, Lxbc;->y:Landroid/widget/FrameLayout;

    const v1, 0x7f0b031d

    .line 1265
    invoke-virtual {p0, v1}, Landroid/view/View;->findViewById(I)Landroid/view/View;

    move-result-object v1

    check-cast v1, Landroid/view/ViewGroup;

    iput-object v1, v0, Lxbc;->H:Landroid/view/ViewGroup;

    const v1, 0x7f0b032e

    .line 1266
    invoke-virtual {p0, v1}, Landroid/view/View;->findViewById(I)Landroid/view/View;

    move-result-object v1

    check-cast v1, Landroid/view/ViewGroup;

    iput-object v1, v0, Lxbc;->I:Landroid/view/ViewGroup;

    const v1, 0x7f0b0328

    .line 1267
    invoke-virtual {p0, v1}, Landroid/view/View;->findViewById(I)Landroid/view/View;

    move-result-object v1

    check-cast v1, Landroid/widget/TextView;

    iput-object v1, v0, Lxbc;->z:Landroid/widget/TextView;

    const v1, 0x7f0b09ed

    .line 1268
    invoke-virtual {p0, v1}, Landroid/view/View;->findViewById(I)Landroid/view/View;

    move-result-object v1

    iput-object v1, v0, Lxbc;->A:Landroid/view/View;

    const v1, 0x7f0b0332

    .line 1269
    invoke-virtual {p0, v1}, Landroid/view/View;->findViewById(I)Landroid/view/View;

    move-result-object v1

    check-cast v1, Landroid/widget/TextView;

    iput-object v1, v0, Lxbc;->D:Landroid/widget/TextView;

    const v1, 0x7f0b0333

    .line 1270
    invoke-virtual {p0, v1}, Landroid/view/View;->findViewById(I)Landroid/view/View;

    move-result-object v1

    check-cast v1, Landroid/widget/TextView;

    iput-object v1, v0, Lxbc;->B:Landroid/widget/TextView;

    const v1, 0x7f0b0334

    .line 1271
    invoke-virtual {p0, v1}, Landroid/view/View;->findViewById(I)Landroid/view/View;

    move-result-object v1

    check-cast v1, Landroid/widget/TextView;

    iput-object v1, v0, Lxbc;->C:Landroid/widget/TextView;

    const v1, 0x7f0b0cf9

    .line 1272
    invoke-virtual {p0, v1}, Landroid/view/View;->findViewById(I)Landroid/view/View;

    move-result-object v1

    iput-object v1, v0, Lxbc;->E:Landroid/view/View;

    const v1, 0x7f0b0cfb

    .line 1273
    invoke-virtual {p0, v1}, Landroid/view/View;->findViewById(I)Landroid/view/View;

    move-result-object v1

    check-cast v1, Landroid/widget/TextView;

    iput-object v1, v0, Lxbc;->G:Landroid/widget/TextView;

    const v1, 0x7f0b0cf8

    .line 1274
    invoke-virtual {p0, v1}, Landroid/view/View;->findViewById(I)Landroid/view/View;

    move-result-object v1

    check-cast v1, Landroid/widget/ImageView;

    iput-object v1, v0, Lxbc;->F:Landroid/widget/ImageView;

    const v1, 0x7f0b0331

    .line 1275
    invoke-virtual {p0, v1}, Landroid/view/View;->findViewById(I)Landroid/view/View;

    move-result-object v1

    iput-object v1, v0, Lxbc;->K:Landroid/view/View;

    const v1, 0x7f0b031f

    .line 1276
    invoke-virtual {p0, v1}, Landroid/view/View;->findViewById(I)Landroid/view/View;

    move-result-object v1

    iput-object v1, v0, Lxbc;->J:Landroid/view/View;

    const v1, 0x7f0b031a

    .line 1277
    invoke-virtual {p0, v1}, Landroid/view/View;->findViewById(I)Landroid/view/View;

    move-result-object v1

    iput-object v1, v0, Lxbc;->P:Landroid/view/View;

    const v1, 0x7f0b0063

    .line 1278
    invoke-virtual {p0, v1}, Landroid/view/View;->findViewById(I)Landroid/view/View;

    move-result-object v1

    iput-object v1, v0, Lxbc;->b:Landroid/view/View;

    const v1, 0x7f0b0e6e

    .line 1279
    invoke-virtual {p0, v1}, Landroid/view/View;->findViewById(I)Landroid/view/View;

    move-result-object p0

    iput-object p0, v0, Lxbc;->c:Landroid/view/View;

    return-object v0
.end method

.method private final b()V
    .locals 4

    .line 483
    iget-object v0, p0, Lxan;->aj:Lxbd;

    iget-object v0, v0, Lxbd;->j:Landroid/widget/TextView;

    if-eqz v0, :cond_0

    .line 484
    invoke-virtual {v0}, Landroid/widget/TextView;->getVisibility()I

    move-result v0

    if-eqz v0, :cond_0

    .line 485
    iget-object v0, p0, Lxan;->aj:Lxbd;

    iget-object v0, v0, Lxbd;->j:Landroid/widget/TextView;

    const/4 v1, 0x4

    invoke-virtual {v0, v1}, Landroid/widget/TextView;->setVisibility(I)V

    .line 486
    :cond_0
    iget-object v0, p0, Lxan;->aj:Lxbd;

    iget-object v0, v0, Lxbd;->d:Landroid/view/View;

    iget v1, p0, Lxan;->h:I

    iget v2, p0, Lxan;->g:I

    iget v3, p0, Lxan;->i:I

    invoke-static {v0, v1, v2, v3, v2}, Lxdw;->a(Landroid/view/View;IIII)V

    return-void
.end method

.method private final b(Laror;Ljava/util/Map;)V
    .locals 3

    .line 427
    iget-object v0, p1, Laror;->e:Lazpz;

    if-nez v0, :cond_0

    .line 428
    sget-object v0, Lazpz;->a:Lazpz;

    .line 429
    :cond_0
    sget-object v1, Lcom/google/protos/youtube/api/innertube/ButtonRendererOuterClass;->buttonRenderer:Lapig;

    .line 430
    invoke-static {v1}, Lapia;->access$000(Laphm;)Lapig;

    move-result-object v1

    .line 431
    invoke-virtual {v0, v1}, Lapie;->a(Lapig;)V

    .line 432
    iget-object v0, v0, Lapie;->h:Laphr;

    iget-object v1, v1, Lapig;->d:Lapid;

    invoke-virtual {v0, v1}, Laphr;->a(Laphu;)Z

    move-result v0

    if-eqz v0, :cond_6

    .line 433
    iget-object p1, p1, Laror;->e:Lazpz;

    if-nez p1, :cond_1

    .line 434
    sget-object p1, Lazpz;->a:Lazpz;

    .line 435
    :cond_1
    sget-object v0, Lcom/google/protos/youtube/api/innertube/ButtonRendererOuterClass;->buttonRenderer:Lapig;

    .line 436
    invoke-static {v0}, Lapia;->access$000(Laphm;)Lapig;

    move-result-object v0

    .line 437
    invoke-virtual {p1, v0}, Lapie;->a(Lapig;)V

    .line 438
    iget-object p1, p1, Lapie;->h:Laphr;

    iget-object v1, v0, Lapig;->d:Lapid;

    invoke-virtual {p1, v1}, Laphr;->b(Laphu;)Ljava/lang/Object;

    move-result-object p1

    if-nez p1, :cond_2

    .line 439
    iget-object p1, v0, Lapig;->b:Ljava/lang/Object;

    goto :goto_0

    .line 452
    :cond_2
    invoke-virtual {v0, p1}, Lapig;->a(Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object p1

    .line 440
    :goto_0
    check-cast p1, Laqvi;

    .line 441
    iget-object v0, p0, Lxan;->aj:Lxbd;

    iget-object v0, v0, Lxbd;->l:Lanuu;

    invoke-virtual {v0}, Lanuu;->a()Z

    move-result v0

    if-eqz v0, :cond_5

    .line 442
    iget-object v0, p0, Lxan;->aj:Lxbd;

    iget-object v0, v0, Lxbd;->l:Lanuu;

    .line 443
    invoke-virtual {v0}, Lanuu;->b()Ljava/lang/Object;

    move-result-object v0

    check-cast v0, Landroid/view/View;

    .line 444
    iget v1, p1, Laqvi;->a:I

    const v2, 0x8000

    and-int/2addr v1, v2

    if-nez v1, :cond_3

    const-string v1, ""

    goto :goto_2

    .line 449
    :cond_3
    iget-object v1, p1, Laqvi;->p:Lapol;

    if-eqz v1, :cond_4

    goto :goto_1

    .line 451
    :cond_4
    sget-object v1, Lapol;->c:Lapol;

    .line 450
    :goto_1
    iget-object v1, v1, Lapol;->b:Ljava/lang/String;

    .line 444
    :goto_2
    invoke-virtual {v0, v1}, Landroid/view/View;->setContentDescription(Ljava/lang/CharSequence;)V

    .line 445
    :cond_5
    iget-object v0, p0, Lxan;->aj:Lxbd;

    iget-object v0, v0, Lxbd;->l:Lanuu;

    .line 446
    invoke-virtual {v0}, Lanuu;->b()Ljava/lang/Object;

    move-result-object v0

    check-cast v0, Landroid/view/View;

    new-instance v1, Lxax;

    invoke-direct {v1, p0, p1, p2}, Lxax;-><init>(Lxan;Laqvi;Ljava/util/Map;)V

    .line 447
    invoke-virtual {v0, v1}, Landroid/view/View;->setOnClickListener(Landroid/view/View$OnClickListener;)V

    const/4 p1, 0x1

    .line 448
    invoke-direct {p0, p1}, Lxan;->b(Z)V

    return-void

    :cond_6
    const/4 p1, 0x0

    .line 453
    invoke-direct {p0, p1}, Lxan;->b(Z)V

    return-void
.end method

.method private final b(Larqp;Z)V
    .locals 8

    .line 1280
    iget-object v0, p0, Lxan;->au:Landroid/widget/FrameLayout;

    invoke-virtual {v0}, Landroid/widget/FrameLayout;->removeAllViews()V

    .line 1281
    iget-object v1, p0, Lxan;->G:Lxem;

    .line 1282
    iget-object v0, p1, Larqp;->J:Laqqw;

    if-eqz v0, :cond_0

    goto :goto_0

    .line 1362
    :cond_0
    sget-object v0, Laqqw;->c:Laqqw;

    .line 1283
    :goto_0
    iget v0, v0, Laqqw;->a:I

    const v2, 0x5ec9696

    const/4 v7, 0x0

    if-eq v0, v2, :cond_1

    move-object v3, v7

    goto :goto_2

    .line 1357
    :cond_1
    iget-object v0, p1, Larqp;->J:Laqqw;

    if-nez v0, :cond_2

    .line 1358
    sget-object v0, Laqqw;->c:Laqqw;

    .line 1359
    :cond_2
    iget v3, v0, Laqqw;->a:I

    if-ne v3, v2, :cond_3

    .line 1360
    iget-object v0, v0, Laqqw;->b:Ljava/lang/Object;

    check-cast v0, Lazal;

    goto :goto_1

    .line 1361
    :cond_3
    sget-object v0, Lazal;->l:Lazal;

    :goto_1
    move-object v3, v0

    :goto_2
    if-nez v3, :cond_4

    move-object p2, v7

    goto :goto_3

    .line 1353
    :cond_4
    iget-object v0, p1, Larqp;->f:Ljava/lang/String;

    .line 1354
    invoke-static {v0}, Lxem;->a(Ljava/lang/String;)Landroid/net/Uri;

    move-result-object v2

    .line 1355
    iget-wide v4, v3, Lazal;->j:J

    move v6, p2

    .line 1356
    invoke-virtual/range {v1 .. v6}, Lxem;->a(Landroid/net/Uri;Ljava/lang/Object;JZ)Ljava/lang/Object;

    move-result-object p2

    check-cast p2, Lazal;

    :goto_3
    const/4 v0, 0x1

    const/16 v1, 0x8

    const/4 v2, 0x0

    if-eqz p2, :cond_18

    .line 1286
    iget-object v3, p0, Lxan;->aM:Lxdo;

    iget-object v4, p0, Lxan;->aS:Lalmp;

    invoke-virtual {v3, v4}, Lallh;->a(Lalmp;)Lalmp;

    move-result-object v3

    .line 1287
    iget-object v4, p0, Lxan;->au:Landroid/widget/FrameLayout;

    iget-object v5, p0, Lxan;->aM:Lxdo;

    invoke-virtual {v5, v3, p2}, Lallh;->a(Lalmp;Ljava/lang/Object;)Landroid/view/View;

    move-result-object v3

    invoke-virtual {v4, v3}, Landroid/widget/FrameLayout;->addView(Landroid/view/View;)V

    .line 1288
    iget-object v3, p0, Lxan;->ay:Landroid/widget/TextView;

    .line 1289
    iget v4, p2, Lazal;->a:I

    and-int/lit8 v4, v4, 0x40

    if-eqz v4, :cond_6

    .line 1290
    iget-object v4, p2, Lazal;->h:Latho;

    if-eqz v4, :cond_5

    goto :goto_4

    .line 1350
    :cond_5
    sget-object v4, Latho;->f:Latho;

    goto :goto_4

    :cond_6
    move-object v4, v7

    .line 1291
    :goto_4
    invoke-static {v4}, Lakzk;->a(Latho;)Landroid/text/Spanned;

    move-result-object v4

    .line 1292
    invoke-virtual {v3, v4}, Landroid/widget/TextView;->setText(Ljava/lang/CharSequence;)V

    .line 1293
    iget-object v3, p0, Lxan;->ax:Landroid/widget/TextView;

    .line 1294
    iget v4, p2, Lazal;->a:I

    and-int/lit8 v4, v4, 0x20

    if-eqz v4, :cond_8

    .line 1295
    iget-object p2, p2, Lazal;->g:Latho;

    if-eqz p2, :cond_7

    goto :goto_5

    .line 1350
    :cond_7
    sget-object p2, Latho;->f:Latho;

    goto :goto_5

    :cond_8
    move-object p2, v7

    .line 1296
    :goto_5
    invoke-static {p2}, Lakzk;->a(Latho;)Landroid/text/Spanned;

    move-result-object p2

    .line 1297
    invoke-virtual {v3, p2}, Landroid/widget/TextView;->setText(Ljava/lang/CharSequence;)V

    .line 1298
    iget-object p2, p0, Lxan;->az:Landroid/widget/TextView;

    .line 1299
    iget v3, p1, Larqp;->a:I

    const/high16 v4, 0x20000

    and-int/2addr v3, v4

    if-eqz v3, :cond_a

    .line 1300
    iget-object v3, p1, Larqp;->u:Latho;

    if-eqz v3, :cond_9

    goto :goto_6

    .line 1348
    :cond_9
    sget-object v3, Latho;->f:Latho;

    goto :goto_6

    :cond_a
    move-object v3, v7

    .line 1301
    :goto_6
    invoke-static {v3}, Lakzk;->a(Latho;)Landroid/text/Spanned;

    move-result-object v3

    .line 1302
    invoke-virtual {p2, v3}, Landroid/widget/TextView;->setText(Ljava/lang/CharSequence;)V

    .line 1303
    iget p2, p1, Larqp;->a:I

    and-int/lit8 p2, p2, 0x10

    if-eqz p2, :cond_c

    .line 1304
    iget-object p2, p1, Larqp;->i:Latho;

    if-eqz p2, :cond_b

    goto :goto_7

    .line 1346
    :cond_b
    sget-object p2, Latho;->f:Latho;

    goto :goto_7

    :cond_c
    move-object p2, v7

    .line 1305
    :goto_7
    invoke-static {p2}, Lakzk;->a(Latho;)Landroid/text/Spanned;

    move-result-object p2

    .line 1306
    invoke-static {p2}, Landroid/text/TextUtils;->isEmpty(Ljava/lang/CharSequence;)Z

    move-result v3

    if-eqz v3, :cond_d

    .line 1307
    iget-object p1, p0, Lxan;->av:Landroid/widget/TextView;

    const-string p2, ""

    invoke-virtual {p1, p2}, Landroid/widget/TextView;->setText(Ljava/lang/CharSequence;)V

    .line 1308
    iget-object p1, p0, Lxan;->av:Landroid/widget/TextView;

    invoke-virtual {p1, v1}, Landroid/widget/TextView;->setVisibility(I)V

    .line 1309
    iget-object p1, p0, Lxan;->aw:Landroid/view/View;

    if-eqz p1, :cond_15

    .line 1310
    invoke-virtual {p1, v1}, Landroid/view/View;->setVisibility(I)V

    goto/16 :goto_d

    .line 1320
    :cond_d
    iget-object v3, p0, Lxan;->av:Landroid/widget/TextView;

    invoke-virtual {v3, p2}, Landroid/widget/TextView;->setText(Ljava/lang/CharSequence;)V

    .line 1321
    iget-object p2, p0, Lxan;->av:Landroid/widget/TextView;

    invoke-virtual {p2, v2}, Landroid/widget/TextView;->setVisibility(I)V

    .line 1322
    iget-object p1, p1, Larqp;->C:Larpb;

    if-eqz p1, :cond_e

    goto :goto_8

    .line 1345
    :cond_e
    sget-object p1, Larpb;->f:Larpb;

    .line 1323
    :goto_8
    iget-object p1, p1, Larpb;->c:Larox;

    if-eqz p1, :cond_f

    goto :goto_9

    .line 1344
    :cond_f
    sget-object p1, Larox;->f:Larox;

    .line 1324
    :goto_9
    iget p2, p1, Larox;->a:I

    and-int/2addr p2, v0

    if-eqz p2, :cond_14

    .line 1325
    iget-object p2, p1, Larox;->b:Lattl;

    if-eqz p2, :cond_10

    goto :goto_a

    .line 1343
    :cond_10
    sget-object p2, Lattl;->c:Lattl;

    .line 1326
    :goto_a
    iget p2, p2, Lattl;->b:I

    invoke-static {p2}, Lattn;->a(I)Lattn;

    move-result-object p2

    if-nez p2, :cond_11

    .line 1327
    sget-object p2, Lattn;->a:Lattn;

    .line 1328
    :cond_11
    sget-object v3, Lattn;->bQ:Lattn;

    if-eq p2, v3, :cond_14

    .line 1329
    iget-object p2, p0, Lxan;->a:Landroid/content/Context;

    .line 1330
    invoke-virtual {p2}, Landroid/content/Context;->getResources()Landroid/content/res/Resources;

    move-result-object p2

    iget-object v3, p0, Lxan;->E:Laltp;

    .line 1331
    iget-object p1, p1, Larox;->b:Lattl;

    if-eqz p1, :cond_12

    goto :goto_b

    .line 1343
    :cond_12
    sget-object p1, Lattl;->c:Lattl;

    .line 1332
    :goto_b
    iget p1, p1, Lattl;->b:I

    invoke-static {p1}, Lattn;->a(I)Lattn;

    move-result-object p1

    if-eqz p1, :cond_13

    goto :goto_c

    .line 1342
    :cond_13
    sget-object p1, Lattn;->a:Lattn;

    .line 1333
    :goto_c
    invoke-interface {v3, p1}, Laltp;->a(Lattn;)I

    move-result p1

    # ytProxy patch: this Drawable feeds setBounds/setCompoundDrawablesRelative
    # unconditionally below with no null-safe path to skip to, so unlike the
    # other call sites, substitute a known-valid id instead of leaving 0
    # (which would crash Resources.getDrawable) when the Laltp lookup misses.
    if-nez p1, :cond_ytproxy_ok_a

    const p1, 0x7f0805b6

    :cond_ytproxy_ok_a
    invoke-virtual {p2, p1}, Landroid/content/res/Resources;->getDrawable(I)Landroid/graphics/drawable/Drawable;

    move-result-object p1

    const/16 p2, 0x32

    .line 1334
    invoke-virtual {p1, v2, v2, p2, p2}, Landroid/graphics/drawable/Drawable;->setBounds(IIII)V

    .line 1335
    iget-object p2, p0, Lxan;->av:Landroid/widget/TextView;

    .line 1336
    invoke-virtual {p2, v7, v7, p1, v7}, Landroid/widget/TextView;->setCompoundDrawablesRelative(Landroid/graphics/drawable/Drawable;Landroid/graphics/drawable/Drawable;Landroid/graphics/drawable/Drawable;Landroid/graphics/drawable/Drawable;)V

    .line 1337
    iget-object p1, p0, Lxan;->av:Landroid/widget/TextView;

    iget-object p2, p0, Lxan;->a:Landroid/content/Context;

    .line 1338
    invoke-virtual {p2}, Landroid/content/Context;->getResources()Landroid/content/res/Resources;

    move-result-object p2

    const v3, 0x7f070214

    invoke-virtual {p2, v3}, Landroid/content/res/Resources;->getDimensionPixelSize(I)I

    move-result p2

    .line 1339
    invoke-virtual {p1, p2}, Landroid/widget/TextView;->setCompoundDrawablePadding(I)V

    .line 1340
    :cond_14
    iget-object p1, p0, Lxan;->aw:Landroid/view/View;

    if-eqz p1, :cond_15

    .line 1341
    invoke-virtual {p1, v2}, Landroid/view/View;->setVisibility(I)V

    .line 1311
    :cond_15
    :goto_d
    iget-object p1, p0, Lxan;->aG:Landroid/view/View;

    if-nez p1, :cond_16

    goto :goto_f

    .line 1317
    :cond_16
    iget-object p2, p0, Lxan;->ay:Landroid/widget/TextView;

    invoke-virtual {p2}, Landroid/widget/TextView;->getText()Ljava/lang/CharSequence;

    move-result-object p2

    invoke-interface {p2}, Ljava/lang/CharSequence;->length()I

    move-result p2

    if-lez p2, :cond_17

    const/4 p2, 0x0

    goto :goto_e

    :cond_17
    const/16 p2, 0x8

    .line 1318
    :goto_e
    invoke-virtual {p1, p2}, Landroid/view/View;->setVisibility(I)V

    goto :goto_f

    :cond_18
    const/4 v0, 0x0

    .line 1313
    :goto_f
    iget-object p1, p0, Lxan;->au:Landroid/widget/FrameLayout;

    if-nez v0, :cond_19

    const/16 p2, 0x8

    goto :goto_10

    :cond_19
    const/4 p2, 0x0

    :goto_10
    invoke-virtual {p1, p2}, Landroid/widget/FrameLayout;->setVisibility(I)V

    .line 1314
    iget-object p1, p0, Lxan;->aE:Landroid/view/ViewGroup;

    invoke-virtual {p1, p2}, Landroid/view/ViewGroup;->setVisibility(I)V

    .line 1315
    iget-object p1, p0, Lxan;->aD:Landroid/view/ViewGroup;

    if-nez v0, :cond_1a

    const/4 v1, 0x0

    :cond_1a
    invoke-virtual {p1, v1}, Landroid/view/ViewGroup;->setVisibility(I)V

    return-void
.end method

.method private final b(Z)V
    .locals 2

    .line 379
    iget-object v0, p0, Lxan;->aj:Lxbd;

    iget-object v0, v0, Lxbd;->l:Lanuu;

    invoke-virtual {v0}, Lanuu;->a()Z

    move-result v0

    if-eqz v0, :cond_1

    .line 380
    iget-object v0, p0, Lxan;->aj:Lxbd;

    iget-object v0, v0, Lxbd;->l:Lanuu;

    invoke-virtual {v0}, Lanuu;->b()Ljava/lang/Object;

    move-result-object v0

    check-cast v0, Landroid/view/View;

    if-nez p1, :cond_0

    const/16 v1, 0x8

    goto :goto_0

    :cond_0
    const/4 v1, 0x0

    .line 381
    :goto_0
    invoke-virtual {v0, v1}, Landroid/view/View;->setVisibility(I)V

    .line 382
    invoke-virtual {v0, p1}, Landroid/view/View;->setClickable(Z)V

    :cond_1
    return-void
.end method

.method private final c()V
    .locals 3

    .line 539
    iget-object v0, p0, Lxan;->r:Landroid/widget/TextView;

    const/4 v1, 0x0

    if-eqz v0, :cond_0

    .line 540
    invoke-virtual {v0, v1}, Landroid/widget/TextView;->setText(Ljava/lang/CharSequence;)V

    .line 541
    iget-object v0, p0, Lxan;->n:Landroid/view/ViewTreeObserver$OnPreDrawListener;

    if-eqz v0, :cond_0

    .line 542
    iget-object v0, p0, Lxan;->r:Landroid/widget/TextView;

    invoke-virtual {v0}, Landroid/widget/TextView;->getViewTreeObserver()Landroid/view/ViewTreeObserver;

    move-result-object v0

    iget-object v2, p0, Lxan;->n:Landroid/view/ViewTreeObserver$OnPreDrawListener;

    invoke-virtual {v0, v2}, Landroid/view/ViewTreeObserver;->removeOnPreDrawListener(Landroid/view/ViewTreeObserver$OnPreDrawListener;)V

    .line 543
    iput-object v1, p0, Lxan;->n:Landroid/view/ViewTreeObserver$OnPreDrawListener;

    .line 544
    :cond_0
    iget-object v0, p0, Lxan;->s:Landroid/widget/TextView;

    if-eqz v0, :cond_1

    .line 545
    invoke-virtual {v0, v1}, Landroid/widget/TextView;->setOnClickListener(Landroid/view/View$OnClickListener;)V

    .line 546
    :cond_1
    iget-object v0, p0, Lxan;->aj:Lxbd;

    const/16 v1, 0x8

    if-eqz v0, :cond_2

    iget-object v0, v0, Lxbd;->a:Landroid/view/ViewGroup;

    if-eqz v0, :cond_2

    .line 547
    invoke-virtual {v0, v1}, Landroid/view/ViewGroup;->setVisibility(I)V

    .line 548
    :cond_2
    iget-object v0, p0, Lxan;->q:Landroid/view/View;

    if-eqz v0, :cond_3

    .line 549
    invoke-virtual {v0, v1}, Landroid/view/View;->setVisibility(I)V

    .line 550
    :cond_3
    iget-object v0, p0, Lxan;->aj:Lxbd;

    if-eqz v0, :cond_4

    iget-object v0, v0, Lxbd;->k:Landroid/view/View;

    if-eqz v0, :cond_4

    .line 551
    invoke-virtual {v0, v1}, Landroid/view/View;->setVisibility(I)V

    :cond_4
    return-void
.end method

.method private final c(Larqp;)V
    .locals 9

    const/4 v0, 0x0

    .line 67
    iput-boolean v0, p0, Lxan;->ab:Z

    .line 68
    iput-boolean v0, p0, Lxan;->ac:Z

    .line 69
    iget-object v1, p0, Lxan;->aa:Landroid/widget/FrameLayout;

    invoke-virtual {v1}, Landroid/widget/FrameLayout;->removeAllViews()V

    .line 70
    iget-object v1, p0, Lxan;->af:Lxbc;

    .line 71
    iget-object v2, p1, Larqp;->W:Lapir;

    invoke-interface {v2}, Lapir;->size()I

    move-result v2

    const/4 v3, 0x1

    if-lez v2, :cond_3

    .line 72
    iget-object v2, p1, Larqp;->W:Lapir;

    .line 73
    invoke-interface {v2}, Ljava/util/List;->iterator()Ljava/util/Iterator;

    move-result-object v2

    :goto_0
    invoke-interface {v2}, Ljava/util/Iterator;->hasNext()Z

    move-result v4

    if-eqz v4, :cond_3

    invoke-interface {v2}, Ljava/util/Iterator;->next()Ljava/lang/Object;

    move-result-object v4

    check-cast v4, Larql;

    .line 74
    iget v4, v4, Larql;->b:I

    invoke-static {v4}, Larqn;->a(I)I

    move-result v4

    if-eqz v4, :cond_0

    goto :goto_1

    :cond_0
    const/4 v4, 0x1

    :goto_1
    add-int/lit8 v4, v4, -0x1

    if-eq v4, v3, :cond_2

    const/4 v5, 0x5

    if-eq v4, v5, :cond_1

    goto :goto_0

    .line 78
    :cond_1
    iput-boolean v3, p0, Lxan;->ac:Z

    .line 79
    iget-object v1, p0, Lxan;->ae:Lxbc;

    goto :goto_0

    .line 76
    :cond_2
    iput-boolean v3, p0, Lxan;->ab:Z

    .line 77
    iget-object v1, p0, Lxan;->ag:Lxbc;

    goto :goto_0

    .line 81
    :cond_3
    iget-object v2, v1, Lxbc;->a:Landroid/view/View;

    .line 82
    new-instance v4, Lxbd;

    invoke-direct {v4, v0}, Lxbd;-><init>(B)V

    iput-object v4, p0, Lxan;->aj:Lxbd;

    const v4, 0x7f0b0310

    const v5, 0x7f0b0317

    const v6, 0x7f0b031c

    if-nez p1, :cond_4

    goto/16 :goto_3

    .line 143
    :cond_4
    iget v7, p1, Larqp;->b:I

    const/high16 v8, 0x200000

    and-int/2addr v7, v8

    if-eqz v7, :cond_7

    .line 144
    iget-object p1, p1, Larqp;->T:Larrb;

    if-eqz p1, :cond_5

    goto :goto_2

    .line 172
    :cond_5
    sget-object p1, Larrb;->c:Larrb;

    .line 145
    :goto_2
    iget p1, p1, Larrb;->b:I

    invoke-static {p1}, Larrd;->a(I)I

    move-result p1

    if-eqz p1, :cond_7

    const/4 v7, 0x7

    if-ne p1, v7, :cond_7

    .line 146
    iput-boolean v3, p0, Lxan;->m:Z

    const p1, 0x7f0b006b

    .line 147
    invoke-virtual {v2, p1}, Landroid/view/View;->findViewById(I)Landroid/view/View;

    move-result-object v0

    if-nez v0, :cond_6

    const v0, 0x7f0b006c

    .line 148
    invoke-virtual {v2, v0}, Landroid/view/View;->findViewById(I)Landroid/view/View;

    move-result-object v0

    check-cast v0, Landroid/view/ViewStub;

    const v3, 0x7f0e00e8

    .line 149
    invoke-virtual {v0, v3}, Landroid/view/ViewStub;->setLayoutResource(I)V

    .line 150
    invoke-virtual {v0}, Landroid/view/ViewStub;->inflate()Landroid/view/View;

    .line 151
    :cond_6
    invoke-virtual {v2, p1}, Landroid/view/View;->findViewById(I)Landroid/view/View;

    move-result-object v0

    .line 152
    iget-object v3, p0, Lxan;->aj:Lxbd;

    invoke-virtual {v2, p1}, Landroid/view/View;->findViewById(I)Landroid/view/View;

    move-result-object p1

    check-cast p1, Landroid/view/ViewGroup;

    iput-object p1, v3, Lxbd;->a:Landroid/view/ViewGroup;

    .line 153
    iget-object p1, p0, Lxan;->aj:Lxbd;

    const v3, 0x7f0b0307

    invoke-virtual {v2, v3}, Landroid/view/View;->findViewById(I)Landroid/view/View;

    move-result-object v3

    iput-object v3, p1, Lxbd;->b:Landroid/view/View;

    .line 154
    iget-object p1, p0, Lxan;->aj:Lxbd;

    const v3, 0x7f0b0308

    .line 155
    invoke-virtual {v2, v3}, Landroid/view/View;->findViewById(I)Landroid/view/View;

    move-result-object v3

    check-cast v3, Landroid/widget/TextView;

    iput-object v3, p1, Lxbd;->c:Landroid/widget/TextView;

    .line 156
    iget-object p1, p0, Lxan;->aj:Lxbd;

    const v3, 0x7f0b0305

    invoke-virtual {v2, v3}, Landroid/view/View;->findViewById(I)Landroid/view/View;

    move-result-object v3

    iput-object v3, p1, Lxbd;->d:Landroid/view/View;

    .line 157
    iget-object p1, p0, Lxan;->aj:Lxbd;

    const v3, 0x7f0b0303

    .line 158
    invoke-virtual {v0, v3}, Landroid/view/View;->findViewById(I)Landroid/view/View;

    move-result-object v3

    check-cast v3, Landroid/view/ViewGroup;

    iput-object v3, p1, Lxbd;->e:Landroid/view/ViewGroup;

    .line 159
    iget-object p1, p0, Lxan;->aj:Lxbd;

    .line 160
    invoke-virtual {v0, v6}, Landroid/view/View;->findViewById(I)Landroid/view/View;

    move-result-object v3

    check-cast v3, Landroid/widget/ImageView;

    iput-object v3, p1, Lxbd;->f:Landroid/widget/ImageView;

    .line 161
    iget-object p1, p0, Lxan;->aj:Lxbd;

    .line 162
    invoke-virtual {v0, v5}, Landroid/view/View;->findViewById(I)Landroid/view/View;

    move-result-object v3

    check-cast v3, Landroid/widget/ImageView;

    iput-object v3, p1, Lxbd;->g:Landroid/widget/ImageView;

    .line 163
    iget-object p1, p0, Lxan;->aj:Lxbd;

    .line 164
    invoke-virtual {v0, v4}, Landroid/view/View;->findViewById(I)Landroid/view/View;

    move-result-object v0

    check-cast v0, Landroid/widget/ImageView;

    iput-object v0, p1, Lxbd;->h:Landroid/widget/ImageView;

    .line 165
    iget-object p1, p0, Lxan;->aj:Lxbd;

    .line 166
    sget-object v0, Lantt;->a:Lantt;

    .line 167
    iput-object v0, p1, Lxbd;->l:Lanuu;

    .line 168
    iget-object p1, p0, Lxan;->aj:Lxbd;

    const v0, 0x7f0b0309

    invoke-virtual {v2, v0}, Landroid/view/View;->findViewById(I)Landroid/view/View;

    move-result-object v0

    iput-object v0, p1, Lxbd;->i:Landroid/view/View;

    .line 169
    iget-object p1, p0, Lxan;->aj:Lxbd;

    const v0, 0x7f0b030a

    invoke-virtual {v2, v0}, Landroid/view/View;->findViewById(I)Landroid/view/View;

    move-result-object v0

    check-cast v0, Landroid/widget/TextView;

    iput-object v0, p1, Lxbd;->j:Landroid/widget/TextView;

    .line 170
    iget-object p1, p0, Lxan;->aj:Lxbd;

    const v0, 0x7f0b0301

    .line 171
    invoke-virtual {v2, v0}, Landroid/view/View;->findViewById(I)Landroid/view/View;

    move-result-object v0

    iput-object v0, p1, Lxbd;->k:Landroid/view/View;

    goto/16 :goto_4

    .line 83
    :cond_7
    :goto_3
    iput-boolean v0, p0, Lxan;->m:Z

    .line 84
    iget-object p1, p0, Lxan;->aj:Lxbd;

    iget-object v0, v1, Lxbc;->j:Landroid/view/ViewGroup;

    iput-object v0, p1, Lxbd;->a:Landroid/view/ViewGroup;

    const v0, 0x7f0b0323

    .line 85
    invoke-virtual {v2, v0}, Landroid/view/View;->findViewById(I)Landroid/view/View;

    move-result-object v0

    iput-object v0, p1, Lxbd;->b:Landroid/view/View;

    .line 86
    iget-object p1, p0, Lxan;->aj:Lxbd;

    const v0, 0x7f0b0325

    invoke-virtual {v2, v0}, Landroid/view/View;->findViewById(I)Landroid/view/View;

    move-result-object v0

    check-cast v0, Landroid/widget/TextView;

    iput-object v0, p1, Lxbd;->c:Landroid/widget/TextView;

    .line 87
    iget-object p1, p0, Lxan;->aj:Lxbd;

    const v0, 0x7f0b0318

    invoke-virtual {v2, v0}, Landroid/view/View;->findViewById(I)Landroid/view/View;

    move-result-object v0

    iput-object v0, p1, Lxbd;->d:Landroid/view/View;

    .line 88
    iget-object p1, p0, Lxan;->aj:Lxbd;

    const v0, 0x7f0b0316

    invoke-virtual {v2, v0}, Landroid/view/View;->findViewById(I)Landroid/view/View;

    move-result-object v0

    check-cast v0, Landroid/view/ViewGroup;

    iput-object v0, p1, Lxbd;->e:Landroid/view/ViewGroup;

    .line 89
    iget-object p1, p0, Lxan;->aj:Lxbd;

    invoke-virtual {v2, v6}, Landroid/view/View;->findViewById(I)Landroid/view/View;

    move-result-object v0

    check-cast v0, Landroid/widget/ImageView;

    iput-object v0, p1, Lxbd;->f:Landroid/widget/ImageView;

    .line 90
    iget-object p1, p0, Lxan;->aj:Lxbd;

    .line 91
    invoke-virtual {v2, v5}, Landroid/view/View;->findViewById(I)Landroid/view/View;

    move-result-object v0

    check-cast v0, Landroid/widget/ImageView;

    iput-object v0, p1, Lxbd;->g:Landroid/widget/ImageView;

    .line 92
    iget-object p1, p0, Lxan;->aj:Lxbd;

    .line 93
    invoke-virtual {v2, v4}, Landroid/view/View;->findViewById(I)Landroid/view/View;

    move-result-object v0

    check-cast v0, Landroid/widget/ImageView;

    iput-object v0, p1, Lxbd;->h:Landroid/widget/ImageView;

    .line 94
    iget-object p1, p0, Lxan;->aj:Lxbd;

    const v0, 0x7f0b033e

    .line 95
    invoke-virtual {v2, v0}, Landroid/view/View;->findViewById(I)Landroid/view/View;

    move-result-object v0

    invoke-static {v0}, Lanuu;->c(Ljava/lang/Object;)Lanuu;

    move-result-object v0

    iput-object v0, p1, Lxbd;->l:Lanuu;

    .line 96
    iget-object p1, p0, Lxan;->aj:Lxbd;

    const v0, 0x7f0b0338

    invoke-virtual {v2, v0}, Landroid/view/View;->findViewById(I)Landroid/view/View;

    move-result-object v0

    iput-object v0, p1, Lxbd;->i:Landroid/view/View;

    .line 97
    iget-object p1, p0, Lxan;->aj:Lxbd;

    const v0, 0x7f0b0339

    invoke-virtual {v2, v0}, Landroid/view/View;->findViewById(I)Landroid/view/View;

    move-result-object v0

    check-cast v0, Landroid/widget/TextView;

    iput-object v0, p1, Lxbd;->j:Landroid/widget/TextView;

    .line 98
    iget-object p1, p0, Lxan;->aj:Lxbd;

    const v0, 0x7f0b03f2

    invoke-virtual {v2, v0}, Landroid/view/View;->findViewById(I)Landroid/view/View;

    move-result-object v0

    iput-object v0, p1, Lxbd;->k:Landroid/view/View;

    .line 99
    :goto_4
    iget-object p1, p0, Lxan;->aj:Lxbd;

    iput-object p1, v1, Lxbc;->f:Lxbd;

    .line 100
    iget-boolean p1, p0, Lxan;->ab:Z

    invoke-direct {p0, v1, p1}, Lxan;->a(Lxbc;Z)V

    .line 101
    iget-object p1, v1, Lxbc;->a:Landroid/view/View;

    iput-object p1, p0, Lxan;->o:Landroid/view/View;

    .line 102
    iget-object p1, v1, Lxbc;->e:Landroid/widget/ImageView;

    iput-object p1, p0, Lxan;->ai:Landroid/widget/ImageView;

    .line 103
    iget-object p1, v1, Lxbc;->g:Landroid/widget/TextView;

    iput-object p1, p0, Lxan;->ak:Landroid/widget/TextView;

    .line 104
    iget-object p1, v1, Lxbc;->d:Landroid/view/View;

    iput-object p1, p0, Lxan;->ah:Landroid/view/View;

    .line 105
    iget-object p1, v1, Lxbc;->h:Landroid/widget/TextView;

    iput-object p1, p0, Lxan;->r:Landroid/widget/TextView;

    .line 106
    iget-object p1, v1, Lxbc;->i:Landroid/widget/TextView;

    iput-object p1, p0, Lxan;->s:Landroid/widget/TextView;

    .line 107
    iget-object p1, v1, Lxbc;->k:Landroid/widget/TextView;

    iput-object p1, p0, Lxan;->aK:Landroid/widget/TextView;

    .line 108
    iget-object p1, v1, Lxbc;->j:Landroid/view/ViewGroup;

    iput-object p1, p0, Lxan;->al:Landroid/view/ViewGroup;

    .line 109
    iget-object p1, v1, Lxbc;->l:Landroid/view/ViewGroup;

    iput-object p1, p0, Lxan;->t:Landroid/view/ViewGroup;

    .line 110
    iget-object p1, v1, Lxbc;->m:Landroid/widget/ImageView;

    iput-object p1, p0, Lxan;->u:Landroid/widget/ImageView;

    .line 111
    iget-object p1, v1, Lxbc;->n:Landroid/widget/ImageView;

    iput-object p1, p0, Lxan;->v:Landroid/widget/ImageView;

    .line 112
    iget-object p1, v1, Lxbc;->o:Landroid/widget/ImageView;

    iput-object p1, p0, Lxan;->w:Landroid/widget/ImageView;

    .line 113
    iget-object p1, v1, Lxbc;->p:Landroid/widget/ImageView;

    iput-object p1, p0, Lxan;->x:Landroid/widget/ImageView;

    .line 114
    iget-object p1, v1, Lxbc;->q:Landroid/widget/TextView;

    iput-object p1, p0, Lxan;->am:Landroid/widget/TextView;

    .line 115
    iget-object p1, v1, Lxbc;->r:Landroid/widget/ImageView;

    iput-object p1, p0, Lxan;->an:Landroid/widget/ImageView;

    .line 116
    iget-object p1, v1, Lxbc;->s:Landroid/widget/TextView;

    iput-object p1, p0, Lxan;->ao:Landroid/widget/TextView;

    .line 117
    iget-object p1, v1, Lxbc;->t:Landroid/widget/TextView;

    iput-object p1, p0, Lxan;->ap:Landroid/widget/TextView;

    .line 118
    iget-object p1, v1, Lxbc;->u:Landroid/widget/ImageView;

    iput-object p1, p0, Lxan;->aq:Landroid/widget/ImageView;

    .line 119
    iget-object p1, v1, Lxbc;->v:Landroid/view/View;

    iput-object p1, p0, Lxan;->ar:Landroid/view/View;

    .line 120
    iget-object p1, v1, Lxbc;->x:Landroid/widget/TextView;

    iput-object p1, p0, Lxan;->at:Landroid/widget/TextView;

    .line 121
    iget-object p1, v1, Lxbc;->w:Landroid/widget/ImageView;

    iput-object p1, p0, Lxan;->as:Landroid/widget/ImageView;

    .line 122
    iget-object p1, v1, Lxbc;->L:Landroid/widget/FrameLayout;

    iput-object p1, p0, Lxan;->y:Landroid/widget/FrameLayout;

    .line 123
    iget-object p1, v1, Lxbc;->M:Landroid/widget/FrameLayout;

    iput-object p1, p0, Lxan;->aH:Landroid/widget/FrameLayout;

    .line 124
    iget-object p1, v1, Lxbc;->N:Landroid/widget/FrameLayout;

    iput-object p1, p0, Lxan;->aI:Landroid/widget/FrameLayout;

    .line 125
    iget-object p1, v1, Lxbc;->O:Landroid/widget/FrameLayout;

    iput-object p1, p0, Lxan;->aJ:Landroid/widget/FrameLayout;

    .line 126
    iget-object p1, v1, Lxbc;->y:Landroid/widget/FrameLayout;

    iput-object p1, p0, Lxan;->au:Landroid/widget/FrameLayout;

    .line 127
    iget-object p1, v1, Lxbc;->z:Landroid/widget/TextView;

    iput-object p1, p0, Lxan;->av:Landroid/widget/TextView;

    .line 128
    iget-object p1, v1, Lxbc;->A:Landroid/view/View;

    iput-object p1, p0, Lxan;->aw:Landroid/view/View;

    .line 129
    iget-object p1, v1, Lxbc;->H:Landroid/view/ViewGroup;

    iput-object p1, p0, Lxan;->aD:Landroid/view/ViewGroup;

    .line 130
    iget-object p1, v1, Lxbc;->I:Landroid/view/ViewGroup;

    iput-object p1, p0, Lxan;->aE:Landroid/view/ViewGroup;

    .line 131
    iget-object p1, v1, Lxbc;->D:Landroid/widget/TextView;

    iput-object p1, p0, Lxan;->az:Landroid/widget/TextView;

    .line 132
    iget-object p1, v1, Lxbc;->B:Landroid/widget/TextView;

    iput-object p1, p0, Lxan;->ax:Landroid/widget/TextView;

    .line 133
    iget-object p1, v1, Lxbc;->C:Landroid/widget/TextView;

    iput-object p1, p0, Lxan;->ay:Landroid/widget/TextView;

    .line 134
    iget-object p1, v1, Lxbc;->E:Landroid/view/View;

    iput-object p1, p0, Lxan;->aA:Landroid/view/View;

    .line 135
    iget-object p1, v1, Lxbc;->F:Landroid/widget/ImageView;

    iput-object p1, p0, Lxan;->aB:Landroid/widget/ImageView;

    .line 136
    iget-object p1, v1, Lxbc;->G:Landroid/widget/TextView;

    iput-object p1, p0, Lxan;->aC:Landroid/widget/TextView;

    .line 137
    iget-object p1, v1, Lxbc;->K:Landroid/view/View;

    iput-object p1, p0, Lxan;->aG:Landroid/view/View;

    .line 138
    iget-object p1, v1, Lxbc;->J:Landroid/view/View;

    iput-object p1, p0, Lxan;->aF:Landroid/view/View;

    .line 139
    iget-object p1, v1, Lxbc;->P:Landroid/view/View;

    iput-object p1, p0, Lxan;->aL:Landroid/view/View;

    .line 140
    iget-object p1, v1, Lxbc;->b:Landroid/view/View;

    iput-object p1, p0, Lxan;->p:Landroid/view/View;

    .line 141
    iget-object p1, v1, Lxbc;->c:Landroid/view/View;

    iput-object p1, p0, Lxan;->q:Landroid/view/View;

    .line 142
    iget-object p1, p0, Lxan;->aa:Landroid/widget/FrameLayout;

    iget-object v0, p0, Lxan;->o:Landroid/view/View;

    invoke-virtual {p1, v0}, Landroid/widget/FrameLayout;->addView(Landroid/view/View;)V

    return-void
.end method

.method private final d(Larqp;)I
    .locals 1

    .line 176
    iget p1, p1, Larqp;->l:I

    invoke-static {p1}, Larqr;->a(I)I

    move-result p1

    if-nez p1, :cond_0

    goto :goto_0

    :cond_0
    const/4 v0, 0x3

    if-ne p1, v0, :cond_1

    .line 178
    iget p1, p0, Lxan;->S:I

    goto :goto_1

    .line 177
    :cond_1
    :goto_0
    iget p1, p0, Lxan;->R:I

    :goto_1
    return p1
.end method

.method private final e(Larqp;)V
    .locals 10

    .line 243
    iget-object v0, p0, Lxan;->am:Landroid/widget/TextView;

    .line 244
    iget v1, p1, Larqp;->a:I

    const/high16 v2, 0x20000

    and-int/2addr v1, v2

    const/4 v2, 0x0

    if-eqz v1, :cond_1

    .line 245
    iget-object v1, p1, Larqp;->u:Latho;

    if-eqz v1, :cond_0

    goto :goto_0

    .line 358
    :cond_0
    sget-object v1, Latho;->f:Latho;

    goto :goto_0

    :cond_1
    move-object v1, v2

    .line 246
    :goto_0
    invoke-static {v1}, Lakzk;->a(Latho;)Landroid/text/Spanned;

    move-result-object v1

    .line 247
    invoke-virtual {v0, v1}, Landroid/widget/TextView;->setText(Ljava/lang/CharSequence;)V

    .line 248
    iget v0, p1, Larqp;->a:I

    and-int/lit8 v0, v0, 0x10

    const/4 v1, 0x1

    const/4 v3, 0x4

    const/16 v4, 0x8

    const/4 v5, 0x0

    if-nez v0, :cond_2

    .line 249
    iget-object v0, p0, Lxan;->ak:Landroid/widget/TextView;

    invoke-virtual {v0, v4}, Landroid/widget/TextView;->setVisibility(I)V

    .line 250
    iget-object v0, p0, Lxan;->aF:Landroid/view/View;

    if-eqz v0, :cond_18

    .line 251
    invoke-virtual {v0, v4}, Landroid/view/View;->setVisibility(I)V

    goto/16 :goto_e

    .line 284
    :cond_2
    iget-object v0, p0, Lxan;->ak:Landroid/widget/TextView;

    .line 285
    iget-object v6, p0, Lxan;->B:Larqp;

    .line 286
    iget v7, v6, Larqp;->a:I

    and-int/lit8 v7, v7, 0x10

    if-eqz v7, :cond_4

    .line 287
    iget-object v6, v6, Larqp;->i:Latho;

    if-eqz v6, :cond_3

    goto :goto_1

    .line 356
    :cond_3
    sget-object v6, Latho;->f:Latho;

    goto :goto_1

    :cond_4
    move-object v6, v2

    .line 288
    :goto_1
    invoke-static {v6}, Lakzk;->a(Latho;)Landroid/text/Spanned;

    move-result-object v6

    .line 289
    invoke-virtual {v0, v6}, Landroid/widget/TextView;->setText(Ljava/lang/CharSequence;)V

    .line 290
    iget-object v0, p0, Lxan;->ak:Landroid/widget/TextView;

    iget v6, p0, Lxan;->Z:I

    invoke-virtual {v0, v6}, Landroid/widget/TextView;->setTextColor(I)V

    .line 291
    iget-object v0, p0, Lxan;->ak:Landroid/widget/TextView;

    iget v6, p0, Lxan;->W:I

    invoke-virtual {v0, v6}, Landroid/widget/TextView;->setBackgroundColor(I)V

    .line 292
    iget-object v0, p0, Lxan;->ak:Landroid/widget/TextView;

    invoke-virtual {v0, v2}, Landroid/widget/TextView;->setBackgroundDrawable(Landroid/graphics/drawable/Drawable;)V

    .line 293
    iget-object v0, p0, Lxan;->ak:Landroid/widget/TextView;

    invoke-virtual {v0, v5, v5, v5, v5}, Landroid/widget/TextView;->setPadding(IIII)V

    .line 294
    iget-object v0, p0, Lxan;->ak:Landroid/widget/TextView;

    invoke-virtual {v0, v5}, Landroid/widget/TextView;->setCompoundDrawablePadding(I)V

    .line 295
    iget-object v0, p0, Lxan;->ak:Landroid/widget/TextView;

    invoke-static {v0, v2, v2, v2}, Laam;->a(Landroid/widget/TextView;Landroid/graphics/drawable/Drawable;Landroid/graphics/drawable/Drawable;Landroid/graphics/drawable/Drawable;)V

    .line 296
    iget-object v0, p1, Larqp;->C:Larpb;

    if-eqz v0, :cond_5

    goto :goto_2

    .line 355
    :cond_5
    sget-object v0, Larpb;->f:Larpb;

    .line 297
    :goto_2
    iget v0, v0, Larpb;->a:I

    and-int/lit8 v0, v0, 0x2

    if-eqz v0, :cond_17

    .line 298
    iget-object v0, p1, Larqp;->C:Larpb;

    if-eqz v0, :cond_6

    goto :goto_3

    .line 355
    :cond_6
    sget-object v0, Larpb;->f:Larpb;

    .line 299
    :goto_3
    iget-object v0, v0, Larpb;->c:Larox;

    if-eqz v0, :cond_7

    goto :goto_4

    .line 354
    :cond_7
    sget-object v0, Larox;->f:Larox;

    .line 300
    :goto_4
    iget v6, v0, Larox;->a:I

    and-int/2addr v6, v4

    if-eqz v6, :cond_9

    .line 301
    iget-object v6, v0, Larox;->d:Latho;

    if-eqz v6, :cond_8

    goto :goto_5

    .line 352
    :cond_8
    sget-object v6, Latho;->f:Latho;

    goto :goto_5

    :cond_9
    move-object v6, v2

    .line 302
    :goto_5
    invoke-static {v6}, Lakzk;->a(Latho;)Landroid/text/Spanned;

    move-result-object v6

    .line 303
    invoke-static {v6}, Landroid/text/TextUtils;->isEmpty(Ljava/lang/CharSequence;)Z

    move-result v7

    if-nez v7, :cond_a

    .line 304
    iget-object v7, p0, Lxan;->ak:Landroid/widget/TextView;

    invoke-virtual {v7, v6}, Landroid/widget/TextView;->setText(Ljava/lang/CharSequence;)V

    .line 305
    :cond_a
    iget v6, v0, Larox;->a:I

    and-int/2addr v6, v3

    if-eqz v6, :cond_d

    .line 306
    iget-object v6, v0, Larox;->c:Laroz;

    if-eqz v6, :cond_b

    goto :goto_6

    .line 351
    :cond_b
    sget-object v6, Laroz;->c:Laroz;

    .line 307
    :goto_6
    iget v7, v6, Laroz;->a:I

    const v8, 0x70fec16

    if-ne v7, v8, :cond_c

    .line 308
    iget-object v6, v6, Laroz;->b:Ljava/lang/Object;

    check-cast v6, Laqtc;

    goto :goto_7

    .line 350
    :cond_c
    sget-object v6, Laqtc;->f:Laqtc;

    .line 309
    :goto_7
    iget-object v7, p0, Lxan;->a:Landroid/content/Context;

    invoke-virtual {v7}, Landroid/content/Context;->getResources()Landroid/content/res/Resources;

    move-result-object v7

    const v8, 0x7f080168

    invoke-virtual {v7, v8}, Landroid/content/res/Resources;->getDrawable(I)Landroid/graphics/drawable/Drawable;

    move-result-object v7

    .line 310
    iget v8, v6, Laqtc;->b:I

    .line 311
    sget-object v9, Landroid/graphics/PorterDuff$Mode;->SRC_IN:Landroid/graphics/PorterDuff$Mode;

    invoke-virtual {v7, v8, v9}, Landroid/graphics/drawable/Drawable;->setColorFilter(ILandroid/graphics/PorterDuff$Mode;)V

    .line 312
    iget-object v8, p0, Lxan;->ak:Landroid/widget/TextView;

    invoke-virtual {v8, v7}, Landroid/widget/TextView;->setBackgroundDrawable(Landroid/graphics/drawable/Drawable;)V

    .line 313
    iget-object v7, p0, Lxan;->ak:Landroid/widget/TextView;

    .line 314
    iget v6, v6, Laqtc;->c:I

    .line 315
    invoke-virtual {v7, v6}, Landroid/widget/TextView;->setTextColor(I)V

    .line 316
    :cond_d
    iget-object v6, p0, Lxan;->ak:Landroid/widget/TextView;

    invoke-virtual {v6}, Landroid/widget/TextView;->getCurrentTextColor()I

    move-result v6

    .line 317
    iget v7, v0, Larox;->a:I

    and-int/2addr v7, v1

    if-eqz v7, :cond_17

    .line 318
    iget-object v7, v0, Larox;->b:Lattl;

    if-eqz v7, :cond_e

    goto :goto_8

    .line 349
    :cond_e
    sget-object v7, Lattl;->c:Lattl;

    .line 319
    :goto_8
    iget v7, v7, Lattl;->b:I

    invoke-static {v7}, Lattn;->a(I)Lattn;

    move-result-object v7

    if-eqz v7, :cond_f

    goto :goto_9

    .line 348
    :cond_f
    sget-object v7, Lattn;->a:Lattn;

    .line 320
    :goto_9
    sget-object v8, Lattn;->bQ:Lattn;

    const v9, 0x7f070215

    if-ne v7, v8, :cond_10

    .line 321
    iget-object v0, p0, Lxan;->a:Landroid/content/Context;

    invoke-virtual {v0}, Landroid/content/Context;->getResources()Landroid/content/res/Resources;

    move-result-object v0

    const v7, 0x7f080237

    invoke-virtual {v0, v7}, Landroid/content/res/Resources;->getDrawable(I)Landroid/graphics/drawable/Drawable;

    move-result-object v0

    goto :goto_d

    .line 332
    :cond_10
    iget-object v7, v0, Larox;->b:Lattl;

    if-eqz v7, :cond_11

    goto :goto_a

    .line 347
    :cond_11
    sget-object v7, Lattl;->c:Lattl;

    .line 333
    :goto_a
    iget v7, v7, Lattl;->b:I

    invoke-static {v7}, Lattn;->a(I)Lattn;

    move-result-object v7

    if-nez v7, :cond_12

    .line 334
    sget-object v7, Lattn;->a:Lattn;

    .line 335
    :cond_12
    sget-object v8, Lattn;->ck:Lattn;

    if-ne v7, v8, :cond_13

    .line 336
    iget-object v0, p0, Lxan;->a:Landroid/content/Context;

    invoke-virtual {v0}, Landroid/content/Context;->getResources()Landroid/content/res/Resources;

    move-result-object v0

    const v7, 0x7f0805ea

    invoke-virtual {v0, v7}, Landroid/content/res/Resources;->getDrawable(I)Landroid/graphics/drawable/Drawable;

    move-result-object v0

    goto :goto_d

    .line 339
    :cond_13
    iget-object v7, p0, Lxan;->a:Landroid/content/Context;

    .line 340
    invoke-virtual {v7}, Landroid/content/Context;->getResources()Landroid/content/res/Resources;

    move-result-object v7

    iget-object v8, p0, Lxan;->E:Laltp;

    .line 341
    iget-object v0, v0, Larox;->b:Lattl;

    if-eqz v0, :cond_14

    goto :goto_b

    .line 347
    :cond_14
    sget-object v0, Lattl;->c:Lattl;

    .line 342
    :goto_b
    iget v0, v0, Lattl;->b:I

    invoke-static {v0}, Lattn;->a(I)Lattn;

    move-result-object v0

    if-eqz v0, :cond_15

    goto :goto_c

    .line 346
    :cond_15
    sget-object v0, Lattn;->a:Lattn;

    .line 343
    :goto_c
    invoke-interface {v8, v0}, Laltp;->a(Lattn;)I

    move-result v0

    # ytProxy patch: same as the other xan.smali site above - this Drawable
    # feeds setBounds unconditionally below with no null-safe path to skip
    # to, so substitute a known-valid id instead of leaving 0 when the
    # Laltp lookup misses.
    if-nez v0, :cond_ytproxy_ok_b

    const v0, 0x7f0805b6

    :cond_ytproxy_ok_b
    invoke-virtual {v7, v0}, Landroid/content/res/Resources;->getDrawable(I)Landroid/graphics/drawable/Drawable;

    move-result-object v0

    const v9, 0x7f070214

    .line 322
    :goto_d
    iget v7, p0, Lxan;->T:I

    invoke-virtual {v0, v5, v5, v7, v7}, Landroid/graphics/drawable/Drawable;->setBounds(IIII)V

    const/high16 v7, -0x1000000

    if-eq v6, v7, :cond_16

    .line 323
    sget-object v7, Landroid/graphics/PorterDuff$Mode;->SRC_IN:Landroid/graphics/PorterDuff$Mode;

    invoke-virtual {v0, v6, v7}, Landroid/graphics/drawable/Drawable;->setColorFilter(ILandroid/graphics/PorterDuff$Mode;)V

    .line 324
    :cond_16
    iget-object v6, p0, Lxan;->ak:Landroid/widget/TextView;

    .line 325
    invoke-virtual {v6, v2, v2, v0, v2}, Landroid/widget/TextView;->setCompoundDrawablesRelative(Landroid/graphics/drawable/Drawable;Landroid/graphics/drawable/Drawable;Landroid/graphics/drawable/Drawable;Landroid/graphics/drawable/Drawable;)V

    .line 326
    iget-object v0, p0, Lxan;->ak:Landroid/widget/TextView;

    iget-object v6, p0, Lxan;->a:Landroid/content/Context;

    .line 327
    invoke-virtual {v6}, Landroid/content/Context;->getResources()Landroid/content/res/Resources;

    move-result-object v6

    invoke-virtual {v6, v9}, Landroid/content/res/Resources;->getDimensionPixelSize(I)I

    move-result v6

    .line 328
    invoke-virtual {v0, v6}, Landroid/widget/TextView;->setCompoundDrawablePadding(I)V

    .line 329
    :cond_17
    iget-object v0, p0, Lxan;->ak:Landroid/widget/TextView;

    invoke-virtual {v0, v5}, Landroid/widget/TextView;->setVisibility(I)V

    .line 330
    iget-object v0, p0, Lxan;->aF:Landroid/view/View;

    if-eqz v0, :cond_18

    .line 331
    invoke-virtual {v0, v5}, Landroid/view/View;->setVisibility(I)V

    .line 252
    :cond_18
    :goto_e
    iget-object v0, p0, Lxan;->aq:Landroid/widget/ImageView;

    invoke-virtual {v0, v4}, Landroid/widget/ImageView;->setVisibility(I)V

    .line 253
    iget-object v0, p1, Larqp;->E:Larpb;

    if-eqz v0, :cond_19

    goto :goto_f

    .line 283
    :cond_19
    sget-object v0, Larpb;->f:Larpb;

    .line 254
    :goto_f
    iget v0, v0, Larpb;->a:I

    and-int/2addr v0, v3

    if-eqz v0, :cond_20

    .line 255
    iget-object v0, p1, Larqp;->E:Larpb;

    if-eqz v0, :cond_1a

    goto :goto_10

    .line 283
    :cond_1a
    sget-object v0, Larpb;->f:Larpb;

    .line 256
    :goto_10
    iget-object v0, v0, Larpb;->d:Larpf;

    if-eqz v0, :cond_1b

    goto :goto_11

    .line 282
    :cond_1b
    sget-object v0, Larpf;->f:Larpf;

    .line 257
    :goto_11
    iget v6, v0, Larpf;->b:I

    if-eq v6, v3, :cond_1f

    .line 258
    iget-object v3, p0, Lxan;->aq:Landroid/widget/ImageView;

    if-ne v6, v1, :cond_1c

    .line 259
    iget-object v1, v0, Larpf;->c:Ljava/lang/Object;

    check-cast v1, Lattl;

    goto :goto_12

    :cond_1c
    move-object v1, v2

    .line 260
    :goto_12
    iget v6, v0, Larpf;->a:I

    and-int/2addr v6, v4

    if-eqz v6, :cond_1d

    .line 261
    iget-object v0, v0, Larpf;->e:Laroz;

    if-nez v0, :cond_1e

    .line 262
    sget-object v0, Laroz;->c:Laroz;

    goto :goto_13

    :cond_1d
    move-object v0, v2

    :cond_1e
    :goto_13
    const v6, 0x7f04012c

    .line 263
    invoke-direct {p0, v3, v1, v0, v6}, Lxan;->a(Landroid/widget/ImageView;Lattl;Laroz;I)V

    goto :goto_14

    .line 277
    :cond_1f
    iget-object v1, p0, Lxan;->aq:Landroid/widget/ImageView;

    .line 278
    iget-object v0, v0, Larpf;->c:Ljava/lang/Object;

    check-cast v0, Lbayv;

    .line 279
    invoke-virtual {v1, v2}, Landroid/widget/ImageView;->setColorFilter(Landroid/graphics/ColorFilter;)V

    .line 280
    iget-object v3, p0, Lxan;->C:Lalid;

    invoke-interface {v3, v1}, Lalid;->a(Landroid/widget/ImageView;)V

    .line 281
    iget-object v3, p0, Lxan;->C:Lalid;

    invoke-interface {v3, v1, v0}, Lalid;->a(Landroid/widget/ImageView;Lbayv;)V

    .line 264
    :goto_14
    iget-object v0, p0, Lxan;->aq:Landroid/widget/ImageView;

    invoke-virtual {v0, v5}, Landroid/widget/ImageView;->setVisibility(I)V

    .line 265
    :cond_20
    iget-object v0, p1, Larqp;->F:Larpb;

    if-eqz v0, :cond_21

    goto :goto_15

    .line 274
    :cond_21
    sget-object v0, Larpb;->f:Larpb;

    .line 266
    :goto_15
    iget v0, v0, Larpb;->a:I

    and-int/2addr v0, v4

    if-eqz v0, :cond_24

    .line 267
    iget-object p1, p1, Larqp;->F:Larpb;

    if-eqz p1, :cond_22

    goto :goto_16

    .line 272
    :cond_22
    sget-object p1, Larpb;->f:Larpb;

    .line 268
    :goto_16
    iget-object v2, p1, Larpb;->e:Larph;

    if-eqz v2, :cond_23

    goto :goto_17

    .line 271
    :cond_23
    sget-object v2, Larph;->f:Larph;

    .line 269
    :cond_24
    :goto_17
    iget-object p1, p0, Lxan;->ar:Landroid/view/View;

    iget-object v0, p0, Lxan;->at:Landroid/widget/TextView;

    iget-object v1, p0, Lxan;->as:Landroid/widget/ImageView;

    invoke-direct {p0, v2, p1, v0, v1}, Lxan;->a(Larph;Landroid/view/View;Landroid/widget/TextView;Landroid/widget/ImageView;)V

    .line 270
    iget-object p1, p0, Lxan;->aA:Landroid/view/View;

    iget-object v0, p0, Lxan;->aC:Landroid/widget/TextView;

    iget-object v1, p0, Lxan;->aB:Landroid/widget/ImageView;

    invoke-direct {p0, v2, p1, v0, v1}, Lxan;->a(Larph;Landroid/view/View;Landroid/widget/TextView;Landroid/widget/ImageView;)V

    return-void
.end method

.method private final f(Larqp;)V
    .locals 4

    .line 360
    iget-object v0, p0, Lxan;->aT:Lxev;

    .line 361
    invoke-virtual {v0, p1}, Lxev;->d(Larqp;)Larqp;

    move-result-object v0

    const/4 v1, 0x1

    if-nez v0, :cond_1

    .line 362
    iget-object v0, p0, Lxan;->aJ:Landroid/widget/FrameLayout;

    const/16 v2, 0x8

    invoke-virtual {v0, v2}, Landroid/widget/FrameLayout;->setVisibility(I)V

    .line 363
    invoke-static {p1}, Lxan;->k(Larqp;)Laqvi;

    move-result-object p1

    if-eqz p1, :cond_0

    .line 364
    invoke-direct {p0, v1}, Lxan;->a(Z)V

    :cond_0
    return-void

    .line 365
    :cond_1
    iget-object v0, p0, Lxan;->aT:Lxev;

    invoke-virtual {v0, p1}, Lxev;->d(Larqp;)Larqp;

    move-result-object p1

    .line 366
    iget-object v0, p0, Lxan;->aM:Lxdo;

    iget-object v2, p0, Lxan;->aS:Lalmp;

    invoke-virtual {v0, v2}, Lallh;->a(Lalmp;)Lalmp;

    move-result-object v0

    .line 367
    iget-object v2, p0, Lxan;->B:Larqp;

    const-string v3, "creatorReplyParentComment"

    invoke-virtual {v0, v3, v2}, Lalmp;->a(Ljava/lang/String;Ljava/lang/Object;)V

    .line 368
    invoke-static {v1}, Ljava/lang/Boolean;->valueOf(Z)Ljava/lang/Boolean;

    move-result-object v1

    const-string v2, "indentedComment"

    invoke-virtual {v0, v2, v1}, Lalmp;->a(Ljava/lang/String;Ljava/lang/Object;)V

    .line 369
    iget-object v1, p0, Lxan;->aM:Lxdo;

    .line 370
    invoke-virtual {v1, v0, p1}, Lallh;->a(Lalmp;Ljava/lang/Object;)Landroid/view/View;

    move-result-object p1

    check-cast p1, Landroid/view/ViewGroup;

    .line 371
    iget-object v0, p0, Lxan;->aJ:Landroid/widget/FrameLayout;

    const/4 v1, 0x0

    invoke-virtual {v0, p1, v1}, Landroid/widget/FrameLayout;->addView(Landroid/view/View;I)V

    .line 372
    iget-object p1, p0, Lxan;->aJ:Landroid/widget/FrameLayout;

    invoke-virtual {p1, v1}, Landroid/widget/FrameLayout;->setVisibility(I)V

    .line 373
    invoke-direct {p0, v1}, Lxan;->a(Z)V

    return-void
.end method

.method private final g(Larqp;)Ljava/lang/String;
    .locals 1

    .line 517
    iget-object v0, p1, Larqp;->C:Larpb;

    if-eqz v0, :cond_0

    goto :goto_0

    .line 537
    :cond_0
    sget-object v0, Larpb;->f:Larpb;

    .line 518
    :goto_0
    iget-object v0, v0, Larpb;->c:Larox;

    if-eqz v0, :cond_1

    goto :goto_1

    .line 536
    :cond_1
    sget-object v0, Larox;->f:Larox;

    .line 519
    :goto_1
    iget-object v0, v0, Larox;->d:Latho;

    if-eqz v0, :cond_2

    goto :goto_2

    .line 535
    :cond_2
    sget-object v0, Latho;->f:Latho;

    .line 520
    :goto_2
    iget-object v0, v0, Latho;->e:Lathq;

    if-eqz v0, :cond_3

    goto :goto_3

    .line 534
    :cond_3
    sget-object v0, Lathq;->c:Lathq;

    .line 521
    :goto_3
    iget v0, v0, Lathq;->a:I

    and-int/lit8 v0, v0, 0x1

    if-eqz v0, :cond_9

    .line 522
    iget-object p1, p1, Larqp;->C:Larpb;

    if-eqz p1, :cond_4

    goto :goto_4

    .line 532
    :cond_4
    sget-object p1, Larpb;->f:Larpb;

    .line 523
    :goto_4
    iget-object p1, p1, Larpb;->c:Larox;

    if-eqz p1, :cond_5

    goto :goto_5

    .line 531
    :cond_5
    sget-object p1, Larox;->f:Larox;

    .line 524
    :goto_5
    iget-object p1, p1, Larox;->d:Latho;

    if-eqz p1, :cond_6

    goto :goto_6

    .line 530
    :cond_6
    sget-object p1, Latho;->f:Latho;

    .line 525
    :goto_6
    iget-object p1, p1, Latho;->e:Lathq;

    if-eqz p1, :cond_7

    goto :goto_7

    .line 529
    :cond_7
    sget-object p1, Lathq;->c:Lathq;

    .line 526
    :goto_7
    iget-object p1, p1, Lathq;->b:Lapol;

    if-nez p1, :cond_8

    .line 527
    sget-object p1, Lapol;->c:Lapol;

    .line 528
    :cond_8
    iget-object p1, p1, Lapol;->b:Ljava/lang/String;

    return-object p1

    .line 533
    :cond_9
    iget-object p1, p0, Lxan;->ak:Landroid/widget/TextView;

    invoke-virtual {p1}, Landroid/widget/TextView;->getText()Ljava/lang/CharSequence;

    move-result-object p1

    invoke-interface {p1}, Ljava/lang/CharSequence;->toString()Ljava/lang/String;

    move-result-object p1

    return-object p1
.end method

.method private static final h(Larqp;)Ljava/lang/String;
    .locals 1

    .line 1189
    iget-object p0, p0, Larqp;->w:Larot;

    if-eqz p0, :cond_0

    goto :goto_0

    .line 1205
    :cond_0
    sget-object p0, Larot;->c:Larot;

    .line 1190
    :goto_0
    iget-object p0, p0, Larot;->b:Laror;

    if-eqz p0, :cond_1

    goto :goto_1

    .line 1204
    :cond_1
    sget-object p0, Laror;->s:Laror;

    .line 1191
    :goto_1
    iget-object p0, p0, Laror;->g:Lasjz;

    if-eqz p0, :cond_2

    goto :goto_2

    .line 1203
    :cond_2
    sget-object p0, Lasjz;->c:Lasjz;

    .line 1192
    :goto_2
    iget-object p0, p0, Lasjz;->b:Lasjx;

    if-eqz p0, :cond_3

    goto :goto_3

    .line 1202
    :cond_3
    sget-object p0, Lasjx;->n:Lasjx;

    .line 1193
    :goto_3
    iget-boolean v0, p0, Lasjx;->f:Z

    if-eqz v0, :cond_8

    .line 1194
    iget-boolean v0, p0, Lasjx;->g:Z

    if-nez v0, :cond_8

    .line 1195
    iget-object v0, p0, Lasjx;->j:Lapon;

    if-eqz v0, :cond_4

    goto :goto_4

    .line 1201
    :cond_4
    sget-object v0, Lapon;->c:Lapon;

    .line 1196
    :goto_4
    iget v0, v0, Lapon;->a:I

    and-int/lit8 v0, v0, 0x1

    if-nez v0, :cond_5

    goto :goto_6

    .line 1197
    :cond_5
    iget-object p0, p0, Lasjx;->j:Lapon;

    if-eqz p0, :cond_6

    goto :goto_5

    .line 1201
    :cond_6
    sget-object p0, Lapon;->c:Lapon;

    .line 1198
    :goto_5
    iget-object p0, p0, Lapon;->b:Lapol;

    if-nez p0, :cond_7

    .line 1199
    sget-object p0, Lapol;->c:Lapol;

    .line 1200
    :cond_7
    iget-object p0, p0, Lapol;->b:Ljava/lang/String;

    return-object p0

    :cond_8
    :goto_6
    const-string p0, ""

    return-object p0
.end method

.method private static final i(Larqp;)Ljava/lang/String;
    .locals 0

    .line 1206
    iget-object p0, p0, Larqp;->v:Latho;

    if-eqz p0, :cond_0

    goto :goto_0

    .line 1212
    :cond_0
    sget-object p0, Latho;->f:Latho;

    .line 1207
    :goto_0
    iget-object p0, p0, Latho;->e:Lathq;

    if-eqz p0, :cond_1

    goto :goto_1

    .line 1211
    :cond_1
    sget-object p0, Lathq;->c:Lathq;

    .line 1208
    :goto_1
    iget-object p0, p0, Lathq;->b:Lapol;

    if-nez p0, :cond_2

    .line 1209
    sget-object p0, Lapol;->c:Lapol;

    .line 1210
    :cond_2
    iget-object p0, p0, Lapol;->b:Ljava/lang/String;

    return-object p0
.end method

.method private static final j(Larqp;)Larqd;
    .locals 1

    .line 1213
    iget-object v0, p0, Larqp;->y:Larqf;

    if-eqz v0, :cond_0

    goto :goto_0

    .line 1220
    :cond_0
    sget-object v0, Larqf;->c:Larqf;

    .line 1214
    :goto_0
    iget v0, v0, Larqf;->a:I

    and-int/lit8 v0, v0, 0x1

    if-eqz v0, :cond_3

    .line 1215
    iget-object p0, p0, Larqp;->y:Larqf;

    if-eqz p0, :cond_1

    goto :goto_1

    .line 1218
    :cond_1
    sget-object p0, Larqf;->c:Larqf;

    .line 1216
    :goto_1
    iget-object p0, p0, Larqf;->b:Larqd;

    if-nez p0, :cond_2

    .line 1217
    sget-object p0, Larqd;->f:Larqd;

    :cond_2
    return-object p0

    :cond_3
    const/4 p0, 0x0

    return-object p0
.end method

.method private static final k(Larqp;)Laqvi;
    .locals 1

    .line 1221
    iget-object p0, p0, Larqp;->w:Larot;

    if-eqz p0, :cond_0

    goto :goto_0

    .line 1230
    :cond_0
    sget-object p0, Larot;->c:Larot;

    .line 1222
    :goto_0
    iget-object p0, p0, Larot;->b:Laror;

    if-eqz p0, :cond_1

    goto :goto_1

    .line 1229
    :cond_1
    sget-object p0, Laror;->s:Laror;

    .line 1223
    :goto_1
    iget-object v0, p0, Laror;->d:Laqvp;

    if-eqz v0, :cond_2

    goto :goto_2

    .line 1228
    :cond_2
    sget-object v0, Laqvp;->d:Laqvp;

    .line 1224
    :goto_2
    iget v0, v0, Laqvp;->a:I

    and-int/lit8 v0, v0, 0x1

    if-eqz v0, :cond_5

    .line 1225
    iget-object p0, p0, Laror;->d:Laqvp;

    if-eqz p0, :cond_3

    goto :goto_3

    .line 1228
    :cond_3
    sget-object p0, Laqvp;->d:Laqvp;

    .line 1226
    :goto_3
    iget-object p0, p0, Laqvp;->b:Laqvi;

    if-nez p0, :cond_4

    .line 1227
    sget-object p0, Laqvi;->s:Laqvi;

    :cond_4
    return-object p0

    :cond_5
    const/4 p0, 0x0

    return-object p0
.end method

.method private static final l(Larqp;)Ljava/lang/CharSequence;
    .locals 0

    .line 1231
    iget-object p0, p0, Larqp;->F:Larpb;

    if-eqz p0, :cond_0

    goto :goto_0

    .line 1237
    :cond_0
    sget-object p0, Larpb;->f:Larpb;

    .line 1232
    :goto_0
    iget-object p0, p0, Larpb;->e:Larph;

    if-eqz p0, :cond_1

    goto :goto_1

    .line 1236
    :cond_1
    sget-object p0, Larph;->f:Larph;

    .line 1233
    :goto_1
    iget-object p0, p0, Larph;->c:Latho;

    if-eqz p0, :cond_2

    goto :goto_2

    .line 1235
    :cond_2
    sget-object p0, Latho;->f:Latho;

    .line 1234
    :goto_2
    invoke-static {p0}, Lakzk;->a(Latho;)Landroid/text/Spanned;

    move-result-object p0

    return-object p0
.end method

.method private static final m(Larqp;)Ljava/lang/String;
    .locals 0

    .line 1371
    invoke-static {p0}, Lxan;->k(Larqp;)Laqvi;

    move-result-object p0

    if-eqz p0, :cond_3

    .line 1372
    iget-object p0, p0, Laqvi;->g:Latho;

    if-eqz p0, :cond_0

    goto :goto_0

    .line 1378
    :cond_0
    sget-object p0, Latho;->f:Latho;

    .line 1373
    :goto_0
    iget-object p0, p0, Latho;->e:Lathq;

    if-eqz p0, :cond_1

    goto :goto_1

    .line 1377
    :cond_1
    sget-object p0, Lathq;->c:Lathq;

    .line 1374
    :goto_1
    iget-object p0, p0, Lathq;->b:Lapol;

    if-nez p0, :cond_2

    .line 1375
    sget-object p0, Lapol;->c:Lapol;

    .line 1376
    :cond_2
    iget-object p0, p0, Lapol;->b:Ljava/lang/String;

    return-object p0

    :cond_3
    const-string p0, ""

    return-object p0
.end method


# virtual methods
.method public final L_()Landroid/view/View;
    .locals 1

    .line 538
    iget-object v0, p0, Lxan;->aa:Landroid/widget/FrameLayout;

    return-object v0
.end method

.method public final synthetic a(Lalmp;Ljava/lang/Object;)V
    .locals 34

    move-object/from16 v6, p0

    move-object/from16 v7, p1

    .line 591
    move-object/from16 v15, p2

    check-cast v15, Larqp;

    .line 592
    iput-object v15, v6, Lxan;->B:Larqp;

    .line 593
    iput-object v7, v6, Lxan;->aS:Lalmp;

    .line 594
    invoke-direct/range {p0 .. p0}, Lxan;->c()V

    .line 595
    invoke-direct {v6, v15}, Lxan;->c(Larqp;)V

    .line 596
    iget-object v0, v6, Lxan;->aO:Lalrh;

    invoke-virtual {v0}, Lalrh;->a()V

    .line 597
    iget-object v0, v6, Lxan;->aP:Lalrp;

    iget-object v1, v6, Lxan;->r:Landroid/widget/TextView;

    .line 598
    iput-object v1, v0, Lalrp;->a:Landroid/view/View;

    .line 599
    iget-object v14, v7, Ladzz;->a:Ladzv;

    .line 600
    iget-object v0, v15, Larqp;->q:Latho;

    if-eqz v0, :cond_0

    goto :goto_0

    .line 1148
    :cond_0
    sget-object v0, Latho;->f:Latho;

    .line 601
    :goto_0
    invoke-static {v0, v14}, Laebh;->a(Latho;Ladzv;)V

    const-string v0, "sectionController"

    .line 602
    invoke-virtual {v7, v0}, Lalmp;->a(Ljava/lang/String;)Ljava/lang/Object;

    move-result-object v0

    move-object v9, v0

    check-cast v9, Lalty;

    const-string v0, "commentThreadMutator"

    .line 603
    invoke-virtual {v7, v0}, Lalmp;->a(Ljava/lang/String;)Ljava/lang/Object;

    move-result-object v1

    check-cast v1, Lwvo;

    const/4 v5, 0x0

    const-string v2, "creatorReplyParentComment"

    .line 604
    invoke-virtual {v7, v2, v5}, Lalmp;->b(Ljava/lang/String;Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v2

    check-cast v2, Larqp;

    .line 605
    iget-object v8, v6, Lxan;->I:Lwvj;

    if-eqz v2, :cond_1

    move-object v11, v2

    goto :goto_1

    :cond_1
    move-object v11, v15

    :goto_1
    const/4 v4, 0x1

    const/4 v3, 0x0

    if-eqz v2, :cond_2

    const/4 v13, 0x1

    goto :goto_2

    :cond_2
    const/4 v13, 0x0

    :goto_2
    move-object v10, v1

    move-object v12, v14

    .line 606
    invoke-virtual/range {v8 .. v13}, Lwvj;->a(Lalty;Lwvo;Larqp;Ladzv;Z)Lwvh;

    move-result-object v12

    .line 607
    new-instance v2, Ljava/util/HashMap;

    invoke-direct {v2}, Ljava/util/HashMap;-><init>()V

    const-string v8, "com.google.android.libraries.youtube.innertube.endpoint.tag"

    .line 608
    invoke-interface {v2, v8, v12}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    .line 609
    invoke-virtual/range {p1 .. p1}, Lalmp;->b()Ljava/util/Map;

    move-result-object v8

    invoke-interface {v2, v8}, Ljava/util/Map;->putAll(Ljava/util/Map;)V

    .line 610
    iget-object v8, v6, Lxan;->aT:Lxev;

    iget-object v9, v6, Lxan;->B:Larqp;

    invoke-virtual {v8, v9}, Lxev;->c(Larqp;)Z

    move-result v8

    if-eqz v8, :cond_4

    .line 611
    iget-object v8, v6, Lxan;->d:Laawi;

    iget-object v9, v6, Lxan;->aT:Lxev;

    iget-object v10, v6, Lxan;->B:Larqp;

    .line 612
    invoke-virtual {v9, v10}, Lxev;->c(Larqp;)Z

    move-result v9

    if-nez v9, :cond_3

    invoke-static {}, Lanyk;->g()Lanyk;

    move-result-object v9

    goto :goto_3

    .line 1146
    :cond_3
    iget-object v9, v10, Larqp;->P:Lapir;

    .line 613
    :goto_3
    invoke-interface {v8, v9, v12}, Laawi;->a(Ljava/util/List;Ljava/lang/Object;)V

    .line 614
    iget-object v8, v6, Lxan;->aT:Lxev;

    iget-object v9, v6, Lxan;->B:Larqp;

    invoke-virtual {v8, v9}, Lxev;->b(Larqp;)V

    .line 615
    :cond_4
    iget-object v8, v6, Lxan;->o:Landroid/view/View;

    iget v9, v6, Lxan;->K:I

    invoke-virtual {v8, v9}, Landroid/view/View;->setMinimumHeight(I)V

    .line 616
    iget-object v8, v6, Lxan;->o:Landroid/view/View;

    invoke-virtual {v8}, Landroid/view/View;->getPaddingLeft()I

    move-result v9

    iget v10, v6, Lxan;->U:I

    iget-object v11, v6, Lxan;->o:Landroid/view/View;

    invoke-virtual {v11}, Landroid/view/View;->getPaddingRight()I

    move-result v11

    invoke-virtual {v8, v9, v10, v11, v3}, Landroid/view/View;->setPadding(IIII)V

    .line 617
    iget-object v8, v6, Lxan;->B:Larqp;

    .line 618
    iget v8, v8, Larqp;->g:I

    invoke-static {v8}, Larqt;->a(I)I

    move-result v8

    const/4 v13, 0x3

    if-eqz v8, :cond_6

    if-eq v8, v13, :cond_5

    goto :goto_4

    .line 1145
    :cond_5
    iget v8, v6, Lxan;->X:I

    goto :goto_5

    .line 620
    :cond_6
    :goto_4
    iget v8, v6, Lxan;->W:I

    .line 621
    :goto_5
    iget-object v9, v6, Lxan;->o:Landroid/view/View;

    invoke-virtual {v9, v8}, Landroid/view/View;->setBackgroundColor(I)V

    .line 622
    iget-object v9, v6, Lxan;->aT:Lxev;

    iget-object v10, v6, Lxan;->B:Larqp;

    invoke-virtual {v9, v10}, Lxev;->a(Larqp;)Z

    move-result v9

    if-eqz v9, :cond_7

    .line 623
    iget-object v9, v6, Lxan;->J:Lxej;

    iget-object v10, v6, Lxan;->o:Landroid/view/View;

    iget v11, v6, Lxan;->Y:I

    .line 624
    invoke-virtual {v9, v10, v8, v11}, Lxej;->a(Landroid/view/View;II)Landroid/animation/Animator;

    move-result-object v8

    iput-object v8, v6, Lxan;->ad:Landroid/animation/Animator;

    .line 625
    invoke-virtual {v8}, Landroid/animation/Animator;->start()V

    .line 626
    iget-object v8, v6, Lxan;->aT:Lxev;

    iget-object v9, v6, Lxan;->B:Larqp;

    invoke-virtual {v8, v9, v3}, Lxev;->a(Larqp;Z)V

    .line 627
    :cond_7
    iget v8, v15, Larqp;->b:I

    and-int/lit16 v8, v8, 0x100

    if-nez v8, :cond_8

    goto :goto_6

    .line 1144
    :cond_8
    iget-object v8, v6, Lxan;->o:Landroid/view/View;

    invoke-virtual {v8}, Landroid/view/View;->getBackground()Landroid/graphics/drawable/Drawable;

    move-result-object v9

    invoke-static {v8, v9}, Lyiy;->a(Landroid/view/View;Landroid/graphics/drawable/Drawable;)V

    .line 628
    :goto_6
    iget-object v8, v6, Lxan;->o:Landroid/view/View;

    new-instance v9, Lxam;

    invoke-direct {v9, v6, v15, v2, v14}, Lxam;-><init>(Lxan;Larqp;Ljava/util/Map;Ladzv;)V

    invoke-virtual {v8, v9}, Landroid/view/View;->setOnClickListener(Landroid/view/View$OnClickListener;)V

    .line 629
    iget v8, v15, Larqp;->d:I

    const/16 v9, 0xc

    if-ne v8, v9, :cond_9

    .line 630
    iget-object v8, v15, Larqp;->e:Ljava/lang/Object;

    check-cast v8, Latho;

    goto :goto_7

    .line 1143
    :cond_9
    sget-object v8, Latho;->f:Latho;

    .line 631
    :goto_7
    invoke-static {v8}, Lakzk;->a(Latho;)Landroid/text/Spanned;

    move-result-object v8

    .line 632
    invoke-static {v8}, Landroid/text/TextUtils;->isEmpty(Ljava/lang/CharSequence;)Z

    move-result v9

    const-string v11, ""

    const/16 v10, 0x8

    if-eqz v9, :cond_a

    .line 633
    iget-object v8, v6, Lxan;->ap:Landroid/widget/TextView;

    invoke-virtual {v8, v11}, Landroid/widget/TextView;->setText(Ljava/lang/CharSequence;)V

    .line 634
    iget-object v8, v6, Lxan;->ap:Landroid/widget/TextView;

    invoke-virtual {v8, v10}, Landroid/widget/TextView;->setVisibility(I)V

    goto :goto_8

    .line 1130
    :cond_a
    iget-object v9, v6, Lxan;->ap:Landroid/widget/TextView;

    invoke-virtual {v9, v8}, Landroid/widget/TextView;->setText(Ljava/lang/CharSequence;)V

    .line 1131
    iget-object v8, v6, Lxan;->ap:Landroid/widget/TextView;

    invoke-virtual {v8, v3}, Landroid/widget/TextView;->setVisibility(I)V

    .line 1132
    iget-object v8, v6, Lxan;->aS:Lalmp;

    invoke-static {v8}, Lxan;->a(Lalmp;)Z

    move-result v8

    if-eqz v8, :cond_b

    .line 1133
    iget-object v8, v6, Lxan;->o:Landroid/view/View;

    .line 1134
    invoke-virtual {v8}, Landroid/view/View;->getPaddingLeft()I

    move-result v9

    iget v13, v6, Lxan;->V:I

    iget-object v5, v6, Lxan;->o:Landroid/view/View;

    .line 1135
    invoke-virtual {v5}, Landroid/view/View;->getPaddingRight()I

    move-result v5

    iget-object v10, v6, Lxan;->o:Landroid/view/View;

    .line 1136
    invoke-virtual {v10}, Landroid/view/View;->getPaddingBottom()I

    move-result v10

    .line 1137
    invoke-virtual {v8, v9, v13, v5, v10}, Landroid/view/View;->setPadding(IIII)V

    goto :goto_8

    .line 1138
    :cond_b
    iget-object v5, v6, Lxan;->o:Landroid/view/View;

    .line 1139
    invoke-virtual {v5}, Landroid/view/View;->getPaddingLeft()I

    move-result v8

    iget v9, v6, Lxan;->U:I

    iget-object v10, v6, Lxan;->o:Landroid/view/View;

    .line 1140
    invoke-virtual {v10}, Landroid/view/View;->getPaddingRight()I

    move-result v10

    iget-object v13, v6, Lxan;->o:Landroid/view/View;

    .line 1141
    invoke-virtual {v13}, Landroid/view/View;->getPaddingBottom()I

    move-result v13

    .line 1142
    invoke-virtual {v5, v8, v9, v10, v13}, Landroid/view/View;->setPadding(IIII)V

    .line 635
    :goto_8
    iget-object v5, v15, Larqp;->D:Larpb;

    if-eqz v5, :cond_c

    goto :goto_9

    .line 1129
    :cond_c
    sget-object v5, Larpb;->f:Larpb;

    .line 636
    :goto_9
    iget v5, v5, Larpb;->a:I

    and-int/2addr v5, v4

    const/4 v13, 0x2

    if-eqz v5, :cond_11

    .line 637
    iget-object v5, v15, Larqp;->D:Larpb;

    if-eqz v5, :cond_d

    goto :goto_a

    .line 1125
    :cond_d
    sget-object v5, Larpb;->f:Larpb;

    .line 638
    :goto_a
    iget-object v5, v5, Larpb;->b:Larpd;

    if-eqz v5, :cond_e

    goto :goto_b

    .line 1124
    :cond_e
    sget-object v5, Larpd;->c:Larpd;

    .line 639
    :goto_b
    iget-object v8, v6, Lxan;->an:Landroid/widget/ImageView;

    invoke-virtual {v8, v3}, Landroid/widget/ImageView;->setVisibility(I)V

    .line 640
    iget-object v8, v6, Lxan;->ao:Landroid/widget/TextView;

    .line 641
    iget v9, v5, Larpd;->a:I

    and-int/2addr v9, v13

    if-eqz v9, :cond_10

    .line 642
    iget-object v5, v5, Larpd;->b:Latho;

    if-eqz v5, :cond_f

    goto :goto_c

    .line 1122
    :cond_f
    sget-object v5, Latho;->f:Latho;

    goto :goto_c

    :cond_10
    const/4 v5, 0x0

    .line 643
    :goto_c
    invoke-static {v5}, Lakzk;->a(Latho;)Landroid/text/Spanned;

    move-result-object v5

    .line 644
    invoke-virtual {v8, v5}, Landroid/widget/TextView;->setText(Ljava/lang/CharSequence;)V

    .line 645
    iget-object v5, v6, Lxan;->ao:Landroid/widget/TextView;

    invoke-virtual {v5, v3}, Landroid/widget/TextView;->setVisibility(I)V

    const/4 v10, 0x0

    goto :goto_d

    .line 1126
    :cond_11
    iget-object v5, v6, Lxan;->an:Landroid/widget/ImageView;

    const/16 v8, 0x8

    invoke-virtual {v5, v8}, Landroid/widget/ImageView;->setVisibility(I)V

    .line 1127
    iget-object v5, v6, Lxan;->ao:Landroid/widget/TextView;

    invoke-virtual {v5, v8}, Landroid/widget/TextView;->setVisibility(I)V

    .line 1128
    iget-object v5, v6, Lxan;->ao:Landroid/widget/TextView;

    const/4 v10, 0x0

    invoke-virtual {v5, v10}, Landroid/widget/TextView;->setText(Ljava/lang/CharSequence;)V

    :goto_d
    const/4 v5, 0x5

    .line 646
    iput v5, v6, Lxan;->l:I

    .line 647
    iget-object v5, v15, Larqp;->V:Larqv;

    if-eqz v5, :cond_12

    goto :goto_e

    .line 1121
    :cond_12
    sget-object v5, Larqv;->c:Larqv;

    .line 648
    :goto_e
    iget v5, v5, Larqv;->b:I

    invoke-static {v5}, Larqx;->a(I)I

    move-result v5

    const v9, 0x3061cf4

    const v8, 0x3049143

    const v3, 0x303c1d6

    const v4, 0x7326ad9

    const/4 v13, 0x4

    if-eqz v5, :cond_17

    const/4 v10, 0x3

    if-ne v5, v10, :cond_17

    .line 650
    iget v5, v15, Larqp;->b:I

    and-int/lit16 v5, v5, 0x80

    if-eqz v5, :cond_16

    .line 651
    iget-object v5, v15, Larqp;->J:Laqqw;

    if-eqz v5, :cond_13

    goto :goto_f

    .line 1119
    :cond_13
    sget-object v5, Laqqw;->c:Laqqw;

    .line 652
    :goto_f
    iget v5, v5, Laqqw;->a:I

    if-ne v5, v4, :cond_15

    :cond_14
    const/4 v5, 0x2

    goto :goto_10

    :cond_15
    if-eq v5, v3, :cond_14

    if-eq v5, v8, :cond_14

    if-eq v5, v9, :cond_14

    const v10, 0x5ec9696

    if-ne v5, v10, :cond_17

    .line 1118
    iput v13, v6, Lxan;->l:I

    goto :goto_11

    .line 653
    :goto_10
    iput v5, v6, Lxan;->l:I

    goto :goto_12

    :cond_16
    const/4 v5, 0x2

    const/4 v10, 0x6

    .line 1120
    iput v10, v6, Lxan;->l:I

    goto :goto_12

    :cond_17
    :goto_11
    const/4 v5, 0x2

    .line 654
    :goto_12
    invoke-interface {v1}, Lwvo;->a()Z

    move-result v1

    .line 655
    iget-object v10, v15, Larqp;->J:Laqqw;

    if-eqz v10, :cond_18

    goto :goto_13

    .line 1116
    :cond_18
    sget-object v10, Laqqw;->c:Laqqw;

    .line 656
    :goto_13
    iget v10, v10, Laqqw;->a:I

    .line 657
    iget-object v3, v15, Larqp;->V:Larqv;

    if-eqz v3, :cond_19

    goto :goto_14

    .line 1115
    :cond_19
    sget-object v3, Larqv;->c:Larqv;

    .line 658
    :goto_14
    iget v3, v3, Larqv;->b:I

    invoke-static {v3}, Larqx;->a(I)I

    move-result v3

    if-eqz v3, :cond_1a

    goto :goto_15

    :cond_1a
    const/4 v3, 0x1

    .line 659
    :goto_15
    iget-object v5, v15, Larqp;->s:Laqvp;

    if-eqz v5, :cond_1b

    goto :goto_16

    .line 1113
    :cond_1b
    sget-object v5, Laqvp;->d:Laqvp;

    .line 660
    :goto_16
    iget v5, v5, Laqvp;->a:I

    const/16 v18, 0x1

    and-int/lit8 v5, v5, 0x1

    if-eqz v5, :cond_29

    if-eqz v1, :cond_22

    .line 662
    iget-object v5, v6, Lxan;->aS:Lalmp;

    .line 663
    invoke-virtual {v5, v0}, Lalmp;->a(Ljava/lang/String;)Ljava/lang/Object;

    move-result-object v0

    check-cast v0, Lwvo;

    if-eqz v0, :cond_1c

    .line 664
    invoke-interface {v0}, Lwvo;->b()Larsz;

    move-result-object v5

    goto :goto_17

    :cond_1c
    const/4 v5, 0x0

    :goto_17
    if-eqz v5, :cond_1f

    .line 665
    iget-object v0, v5, Larsz;->b:Larqz;

    if-eqz v0, :cond_1d

    goto :goto_18

    .line 1110
    :cond_1d
    sget-object v0, Larqz;->c:Larqz;

    .line 666
    :goto_18
    iget v0, v0, Larqz;->a:I

    const/16 v18, 0x1

    and-int/lit8 v0, v0, 0x1

    if-eqz v0, :cond_1f

    .line 667
    iget-object v0, v5, Larsz;->b:Larqz;

    if-eqz v0, :cond_1e

    goto :goto_19

    .line 1108
    :cond_1e
    sget-object v0, Larqz;->c:Larqz;

    .line 668
    :goto_19
    iget-object v5, v0, Larqz;->b:Larqp;

    if-nez v5, :cond_20

    .line 669
    sget-object v5, Larqp;->ac:Larqp;

    goto :goto_1a

    :cond_1f
    const/4 v5, 0x0

    :cond_20
    :goto_1a
    if-nez v5, :cond_21

    goto :goto_1b

    .line 1105
    :cond_21
    iget v0, v5, Larqp;->a:I

    const/16 v18, 0x1

    and-int/lit8 v0, v0, 0x1

    if-eqz v0, :cond_22

    .line 1106
    iget-object v0, v5, Larqp;->f:Ljava/lang/String;

    iget-object v5, v15, Larqp;->f:Ljava/lang/String;

    .line 1107
    invoke-virtual {v0, v5}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_29

    :cond_22
    :goto_1b
    const v0, 0x5ec9696

    if-eq v10, v0, :cond_24

    :cond_23
    const/4 v5, 0x1

    goto :goto_1c

    :cond_24
    const/4 v5, 0x3

    if-eq v3, v5, :cond_23

    goto :goto_20

    .line 671
    :goto_1c
    invoke-direct {v6, v15, v5}, Lxan;->a(Larqp;Z)V

    .line 672
    new-instance v5, Lxay;

    invoke-direct {v5, v6}, Lxay;-><init>(Lxan;)V

    iput-object v5, v6, Lxan;->n:Landroid/view/ViewTreeObserver$OnPreDrawListener;

    .line 673
    iget-object v5, v6, Lxan;->r:Landroid/widget/TextView;

    invoke-virtual {v5}, Landroid/widget/TextView;->getViewTreeObserver()Landroid/view/ViewTreeObserver;

    move-result-object v5

    iget-object v0, v6, Lxan;->n:Landroid/view/ViewTreeObserver$OnPreDrawListener;

    invoke-virtual {v5, v0}, Landroid/view/ViewTreeObserver;->addOnPreDrawListener(Landroid/view/ViewTreeObserver$OnPreDrawListener;)V

    .line 674
    iget-object v0, v15, Larqp;->s:Laqvp;

    if-eqz v0, :cond_25

    goto :goto_1d

    .line 1103
    :cond_25
    sget-object v0, Laqvp;->d:Laqvp;

    .line 675
    :goto_1d
    iget-object v0, v0, Laqvp;->b:Laqvi;

    if-eqz v0, :cond_26

    goto :goto_1e

    .line 1102
    :cond_26
    sget-object v0, Laqvi;->s:Laqvi;

    .line 676
    :goto_1e
    iget-object v5, v6, Lxan;->s:Landroid/widget/TextView;

    .line 677
    iget v8, v0, Laqvi;->a:I

    and-int/lit16 v8, v8, 0x80

    if-eqz v8, :cond_28

    .line 678
    iget-object v8, v0, Laqvi;->g:Latho;

    if-eqz v8, :cond_27

    goto :goto_1f

    .line 1100
    :cond_27
    sget-object v8, Latho;->f:Latho;

    goto :goto_1f

    :cond_28
    const/4 v8, 0x0

    .line 679
    :goto_1f
    invoke-static {v8}, Lakzk;->a(Latho;)Landroid/text/Spanned;

    move-result-object v8

    .line 680
    invoke-virtual {v5, v8}, Landroid/widget/TextView;->setText(Ljava/lang/CharSequence;)V

    .line 681
    iget-object v5, v6, Lxan;->s:Landroid/widget/TextView;

    const/16 v8, 0x8

    invoke-virtual {v5, v8}, Landroid/widget/TextView;->setVisibility(I)V

    .line 682
    iget-object v5, v6, Lxan;->s:Landroid/widget/TextView;

    new-instance v8, Lxar;

    invoke-direct {v8, v6, v0, v14, v15}, Lxar;-><init>(Lxan;Laqvi;Ladzv;Larqp;)V

    invoke-virtual {v5, v8}, Landroid/widget/TextView;->setOnClickListener(Landroid/view/View$OnClickListener;)V

    goto :goto_21

    .line 1104
    :cond_29
    :goto_20
    invoke-virtual {v6, v15}, Lxan;->b(Larqp;)V

    .line 683
    :goto_21
    invoke-direct {v6, v15}, Lxan;->e(Larqp;)V

    if-nez v15, :cond_2a

    :goto_22
    const/4 v0, 0x0

    goto :goto_25

    .line 1093
    :cond_2a
    iget-object v0, v15, Larqp;->x:Laxet;

    if-eqz v0, :cond_2b

    goto :goto_23

    .line 1099
    :cond_2b
    sget-object v0, Laxet;->d:Laxet;

    .line 1094
    :goto_23
    iget v0, v0, Laxet;->a:I

    const/4 v5, 0x1

    and-int/2addr v0, v5

    if-nez v0, :cond_2c

    goto :goto_22

    .line 1096
    :cond_2c
    iget-object v0, v15, Larqp;->x:Laxet;

    if-eqz v0, :cond_2d

    goto :goto_24

    .line 1099
    :cond_2d
    sget-object v0, Laxet;->d:Laxet;

    .line 1097
    :goto_24
    iget-object v0, v0, Laxet;->b:Laxep;

    if-nez v0, :cond_2e

    .line 1098
    sget-object v0, Laxep;->m:Laxep;

    .line 685
    :cond_2e
    :goto_25
    iget-boolean v5, v6, Lxan;->m:Z

    if-eqz v5, :cond_2f

    .line 686
    iget-object v5, v6, Lxan;->q:Landroid/view/View;

    .line 687
    iget-object v8, v6, Lxan;->p:Landroid/view/View;

    const/16 v13, 0x8

    invoke-virtual {v8, v13}, Landroid/view/View;->setVisibility(I)V

    goto :goto_26

    :cond_2f
    const/16 v13, 0x8

    .line 1090
    iget-object v5, v6, Lxan;->p:Landroid/view/View;

    .line 1091
    iget-object v8, v6, Lxan;->q:Landroid/view/View;

    if-eqz v8, :cond_30

    .line 1092
    invoke-virtual {v8, v13}, Landroid/view/View;->setVisibility(I)V

    .line 688
    :cond_30
    :goto_26
    invoke-virtual {v5}, Landroid/view/View;->getLayoutParams()Landroid/view/ViewGroup$LayoutParams;

    move-result-object v8

    check-cast v8, Landroid/widget/RelativeLayout$LayoutParams;

    const v9, 0x7f0b0157

    const/4 v13, 0x3

    .line 689
    invoke-virtual {v8, v13, v9}, Landroid/widget/RelativeLayout$LayoutParams;->addRule(II)V

    .line 690
    invoke-virtual {v5, v8}, Landroid/view/View;->setLayoutParams(Landroid/view/ViewGroup$LayoutParams;)V

    if-nez v0, :cond_31

    const/16 v8, 0x8

    goto :goto_27

    :cond_31
    const/4 v8, 0x0

    .line 691
    :goto_27
    invoke-virtual {v5, v8}, Landroid/view/View;->setVisibility(I)V

    if-eqz v0, :cond_33

    .line 692
    iget-object v8, v0, Laxep;->h:Lapon;

    if-eqz v8, :cond_32

    goto :goto_28

    .line 1087
    :cond_32
    sget-object v8, Lapon;->c:Lapon;

    goto :goto_28

    :cond_33
    const/4 v8, 0x0

    .line 693
    :goto_28
    invoke-static {v5, v8}, Lxan;->a(Landroid/view/View;Lapon;)V

    .line 694
    iget-object v8, v6, Lxan;->D:Lalto;

    iget-object v9, v6, Lxan;->o:Landroid/view/View;

    move/from16 v29, v10

    const/16 v4, 0x8

    const/16 v17, 0x0

    move-object v10, v5

    move-object/from16 v30, v11

    move-object v11, v0

    const v5, 0x5ec9696

    move-object v13, v14

    invoke-interface/range {v8 .. v13}, Lalto;->a(Landroid/view/View;Landroid/view/View;Laxep;Ljava/lang/Object;Ladzv;)V

    .line 695
    iget-boolean v8, v6, Lxan;->A:Z

    if-eqz v8, :cond_34

    .line 696
    iget-object v0, v6, Lxan;->o:Landroid/view/View;

    invoke-virtual {v0}, Landroid/view/View;->getViewTreeObserver()Landroid/view/ViewTreeObserver;

    move-result-object v0

    iget-object v8, v6, Lxan;->z:Landroid/view/ViewTreeObserver$OnScrollChangedListener;

    invoke-virtual {v0, v8}, Landroid/view/ViewTreeObserver;->removeOnScrollChangedListener(Landroid/view/ViewTreeObserver$OnScrollChangedListener;)V

    goto :goto_2b

    :cond_34
    if-eqz v0, :cond_37

    .line 1077
    iget v8, v0, Laxep;->a:I

    and-int/lit16 v8, v8, 0x80

    if-eqz v8, :cond_37

    .line 1078
    iget-object v8, v0, Laxep;->g:Laxeh;

    if-eqz v8, :cond_35

    goto :goto_29

    .line 1085
    :cond_35
    sget-object v8, Laxeh;->c:Laxeh;

    .line 1079
    :goto_29
    iget v9, v8, Laxeh;->a:I

    const v10, 0x61f53fb

    if-ne v9, v10, :cond_36

    .line 1080
    iget-object v8, v8, Laxeh;->b:Ljava/lang/Object;

    check-cast v8, Latrj;

    goto :goto_2a

    .line 1084
    :cond_36
    sget-object v8, Latrj;->j:Latrj;

    goto :goto_2a

    :cond_37
    move-object/from16 v8, v17

    :goto_2a
    if-eqz v8, :cond_38

    .line 1081
    new-instance v9, Lxaq;

    invoke-direct {v9, v6, v8, v0, v14}, Lxaq;-><init>(Lxan;Latrj;Laxep;Ladzv;)V

    iput-object v9, v6, Lxan;->z:Landroid/view/ViewTreeObserver$OnScrollChangedListener;

    .line 1082
    iget-boolean v0, v6, Lxan;->A:Z

    if-nez v0, :cond_38

    .line 1083
    iget-object v0, v6, Lxan;->o:Landroid/view/View;

    invoke-virtual {v0}, Landroid/view/View;->getViewTreeObserver()Landroid/view/ViewTreeObserver;

    move-result-object v0

    iget-object v8, v6, Lxan;->z:Landroid/view/ViewTreeObserver$OnScrollChangedListener;

    invoke-virtual {v0, v8}, Landroid/view/ViewTreeObserver;->addOnScrollChangedListener(Landroid/view/ViewTreeObserver$OnScrollChangedListener;)V

    .line 697
    :cond_38
    :goto_2b
    iget-object v0, v6, Lxan;->y:Landroid/widget/FrameLayout;

    const/4 v8, 0x0

    invoke-virtual {v0, v8}, Landroid/widget/FrameLayout;->setClickable(Z)V

    .line 698
    iget-object v0, v6, Lxan;->y:Landroid/widget/FrameLayout;

    invoke-virtual {v0}, Landroid/widget/FrameLayout;->removeAllViews()V

    .line 699
    iget-object v0, v6, Lxan;->y:Landroid/widget/FrameLayout;

    invoke-virtual {v0, v4}, Landroid/widget/FrameLayout;->setVisibility(I)V

    .line 700
    iget-object v0, v6, Lxan;->y:Landroid/widget/FrameLayout;

    .line 701
    invoke-virtual {v0}, Landroid/widget/FrameLayout;->getLayoutParams()Landroid/view/ViewGroup$LayoutParams;

    move-result-object v8

    check-cast v8, Landroid/widget/RelativeLayout$LayoutParams;

    const v9, 0x7f0b0327

    const/4 v13, 0x3

    .line 702
    invoke-virtual {v8, v13, v9}, Landroid/widget/RelativeLayout$LayoutParams;->addRule(II)V

    .line 703
    invoke-virtual {v0, v8}, Landroid/widget/FrameLayout;->setLayoutParams(Landroid/view/ViewGroup$LayoutParams;)V

    .line 704
    iget-object v0, v15, Larqp;->J:Laqqw;

    if-nez v0, :cond_39

    .line 705
    sget-object v0, Laqqw;->c:Laqqw;

    .line 706
    :cond_39
    iget v0, v0, Laqqw;->a:I

    const v8, 0x7326ad9

    if-eq v0, v8, :cond_3a

    move-object/from16 v0, v17

    goto :goto_2c

    .line 1072
    :cond_3a
    iget-object v0, v15, Larqp;->J:Laqqw;

    if-nez v0, :cond_3b

    .line 1073
    sget-object v0, Laqqw;->c:Laqqw;

    .line 1074
    :cond_3b
    iget v9, v0, Laqqw;->a:I

    if-ne v9, v8, :cond_3c

    .line 1075
    iget-object v0, v0, Laqqw;->b:Ljava/lang/Object;

    check-cast v0, Laqoy;

    goto :goto_2c

    .line 1076
    :cond_3c
    sget-object v0, Laqoy;->e:Laqoy;

    :goto_2c
    const-string v8, "postsV2FullThumbnailStyle"

    if-eqz v0, :cond_40

    .line 708
    iget-object v9, v6, Lxan;->aM:Lxdo;

    iget-object v10, v6, Lxan;->aS:Lalmp;

    invoke-virtual {v9, v10}, Lallh;->a(Lalmp;)Lalmp;

    move-result-object v9

    .line 709
    iget-boolean v10, v6, Lxan;->ab:Z

    if-eqz v10, :cond_3e

    .line 710
    iget-object v10, v6, Lxan;->r:Landroid/widget/TextView;

    invoke-virtual {v10}, Landroid/widget/TextView;->getVisibility()I

    move-result v10

    if-eqz v10, :cond_3d

    .line 711
    iget-object v10, v6, Lxan;->y:Landroid/widget/FrameLayout;

    .line 712
    invoke-virtual {v10}, Landroid/widget/FrameLayout;->getLayoutParams()Landroid/view/ViewGroup$LayoutParams;

    move-result-object v11

    check-cast v11, Landroid/widget/RelativeLayout$LayoutParams;

    const v12, 0x7f0b030f

    .line 713
    invoke-virtual {v11, v13, v12}, Landroid/widget/RelativeLayout$LayoutParams;->addRule(II)V

    .line 714
    invoke-virtual {v10, v11}, Landroid/widget/FrameLayout;->setLayoutParams(Landroid/view/ViewGroup$LayoutParams;)V

    :cond_3d
    const/4 v10, 0x1

    .line 715
    invoke-static {v10}, Ljava/lang/Boolean;->valueOf(Z)Ljava/lang/Boolean;

    move-result-object v11

    invoke-virtual {v9, v8, v11}, Lalmp;->a(Ljava/lang/String;Ljava/lang/Object;)V

    goto :goto_2d

    :cond_3e
    const/4 v10, 0x1

    .line 716
    :goto_2d
    iget-object v11, v6, Lxan;->aM:Lxdo;

    .line 717
    invoke-virtual {v11, v9, v0}, Lallh;->a(Lalmp;Ljava/lang/Object;)Landroid/view/View;

    move-result-object v0

    .line 718
    iget-object v9, v6, Lxan;->y:Landroid/widget/FrameLayout;

    invoke-virtual {v9, v0}, Landroid/widget/FrameLayout;->addView(Landroid/view/View;)V

    .line 719
    iget-object v0, v6, Lxan;->y:Landroid/widget/FrameLayout;

    const/4 v9, 0x0

    invoke-virtual {v0, v9}, Landroid/widget/FrameLayout;->setVisibility(I)V

    .line 720
    iget-boolean v0, v15, Larqp;->R:Z

    if-nez v0, :cond_3f

    goto :goto_2e

    .line 1068
    :cond_3f
    iget-object v0, v6, Lxan;->y:Landroid/widget/FrameLayout;

    iget-object v11, v6, Lxan;->a:Landroid/content/Context;

    .line 1069
    invoke-virtual {v11}, Landroid/content/Context;->getResources()Landroid/content/res/Resources;

    move-result-object v11

    const v12, 0x7f130392

    invoke-virtual {v11, v12}, Landroid/content/res/Resources;->getString(I)Ljava/lang/String;

    move-result-object v11

    .line 1070
    invoke-virtual {v0, v11}, Landroid/widget/FrameLayout;->setContentDescription(Ljava/lang/CharSequence;)V

    .line 1071
    iget-object v11, v6, Lxan;->y:Landroid/widget/FrameLayout;

    new-instance v12, Lxas;

    move-object v0, v12

    move/from16 p2, v1

    move-object/from16 v1, p0

    move-object/from16 v31, v2

    move-object v2, v15

    move v9, v3

    const/4 v10, 0x0

    move-object v3, v14

    const/16 v10, 0x8

    const/16 v32, 0x1

    move-object/from16 v4, v31

    move/from16 v5, p2

    invoke-direct/range {v0 .. v5}, Lxas;-><init>(Lxan;Larqp;Ladzv;Ljava/util/Map;Z)V

    invoke-virtual {v11, v12}, Landroid/widget/FrameLayout;->setOnClickListener(Landroid/view/View$OnClickListener;)V

    goto :goto_2f

    :cond_40
    :goto_2e
    move/from16 p2, v1

    move-object/from16 v31, v2

    move v9, v3

    const/16 v10, 0x8

    const/16 v32, 0x1

    .line 721
    :goto_2f
    iget-object v0, v6, Lxan;->aH:Landroid/widget/FrameLayout;

    invoke-virtual {v0}, Landroid/widget/FrameLayout;->removeAllViews()V

    .line 722
    iget-object v0, v6, Lxan;->aH:Landroid/widget/FrameLayout;

    invoke-virtual {v0, v10}, Landroid/widget/FrameLayout;->setVisibility(I)V

    .line 723
    iget-object v0, v15, Larqp;->J:Laqqw;

    if-nez v0, :cond_41

    .line 724
    sget-object v0, Laqqw;->c:Laqqw;

    .line 725
    :cond_41
    iget v0, v0, Laqqw;->a:I

    const v1, 0x3061cf4

    if-eq v0, v1, :cond_42

    goto :goto_31

    .line 1058
    :cond_42
    iget-object v0, v15, Larqp;->J:Laqqw;

    if-nez v0, :cond_43

    .line 1059
    sget-object v0, Laqqw;->c:Laqqw;

    .line 1060
    :cond_43
    iget v2, v0, Laqqw;->a:I

    if-ne v2, v1, :cond_44

    .line 1061
    iget-object v0, v0, Laqqw;->b:Ljava/lang/Object;

    check-cast v0, Layxj;

    goto :goto_30

    .line 1067
    :cond_44
    sget-object v0, Layxj;->u:Layxj;

    .line 1062
    :goto_30
    iget-object v1, v6, Lxan;->aM:Lxdo;

    iget-object v2, v6, Lxan;->aS:Lalmp;

    .line 1063
    invoke-virtual {v1, v2}, Lallh;->a(Lalmp;)Lalmp;

    move-result-object v1

    .line 1064
    iget-object v2, v6, Lxan;->aM:Lxdo;

    invoke-virtual {v2, v1, v0}, Lallh;->a(Lalmp;Ljava/lang/Object;)Landroid/view/View;

    move-result-object v0

    .line 1065
    iget-object v1, v6, Lxan;->aH:Landroid/widget/FrameLayout;

    invoke-virtual {v1, v0}, Landroid/widget/FrameLayout;->addView(Landroid/view/View;)V

    .line 1066
    iget-object v0, v6, Lxan;->aH:Landroid/widget/FrameLayout;

    const/4 v1, 0x0

    invoke-virtual {v0, v1}, Landroid/widget/FrameLayout;->setVisibility(I)V

    .line 726
    :goto_31
    iget-object v0, v6, Lxan;->aI:Landroid/widget/FrameLayout;

    invoke-virtual {v0}, Landroid/widget/FrameLayout;->removeAllViews()V

    .line 727
    iget-object v0, v6, Lxan;->aI:Landroid/widget/FrameLayout;

    invoke-virtual {v0, v10}, Landroid/widget/FrameLayout;->setVisibility(I)V

    .line 728
    iget-object v0, v15, Larqp;->J:Laqqw;

    if-nez v0, :cond_45

    .line 729
    sget-object v0, Laqqw;->c:Laqqw;

    .line 730
    :cond_45
    iget v0, v0, Laqqw;->a:I

    const v1, 0x303c1d6

    if-eq v0, v1, :cond_4a

    .line 731
    iget-object v0, v15, Larqp;->J:Laqqw;

    if-nez v0, :cond_46

    .line 732
    sget-object v0, Laqqw;->c:Laqqw;

    .line 733
    :cond_46
    iget v0, v0, Laqqw;->a:I

    const v1, 0x3049143

    if-eq v0, v1, :cond_47

    :goto_32
    move/from16 v0, p2

    const/4 v1, 0x0

    goto/16 :goto_35

    .line 1034
    :cond_47
    iget-object v0, v15, Larqp;->J:Laqqw;

    if-nez v0, :cond_48

    .line 1035
    sget-object v0, Laqqw;->c:Laqqw;

    .line 1036
    :cond_48
    iget v2, v0, Laqqw;->a:I

    if-ne v2, v1, :cond_49

    .line 1037
    iget-object v0, v0, Laqqw;->b:Ljava/lang/Object;

    check-cast v0, Larxu;

    goto :goto_33

    .line 1046
    :cond_49
    sget-object v0, Larxu;->I:Larxu;

    .line 1038
    :goto_33
    iget-object v1, v6, Lxan;->aM:Lxdo;

    iget-object v2, v6, Lxan;->aS:Lalmp;

    .line 1039
    invoke-virtual {v1, v2}, Lallh;->a(Lalmp;)Lalmp;

    move-result-object v1

    .line 1040
    invoke-static/range {v32 .. v32}, Ljava/lang/Boolean;->valueOf(Z)Ljava/lang/Boolean;

    move-result-object v2

    const-string v3, "postsV2FullToolbarStyle"

    invoke-virtual {v1, v3, v2}, Lalmp;->a(Ljava/lang/String;Ljava/lang/Object;)V

    const/4 v2, 0x0

    .line 1041
    invoke-static {v2}, Ljava/lang/Boolean;->valueOf(Z)Ljava/lang/Boolean;

    move-result-object v3

    const-string v4, "showLineSeparator"

    invoke-virtual {v1, v4, v3}, Lalmp;->a(Ljava/lang/String;Ljava/lang/Object;)V

    .line 1042
    iget-object v3, v6, Lxan;->aM:Lxdo;

    .line 1043
    invoke-virtual {v3, v1, v0}, Lallh;->a(Lalmp;Ljava/lang/Object;)Landroid/view/View;

    move-result-object v0

    .line 1044
    iget-object v1, v6, Lxan;->aI:Landroid/widget/FrameLayout;

    invoke-virtual {v1, v0}, Landroid/widget/FrameLayout;->addView(Landroid/view/View;)V

    .line 1045
    iget-object v0, v6, Lxan;->aI:Landroid/widget/FrameLayout;

    invoke-virtual {v0, v2}, Landroid/widget/FrameLayout;->setVisibility(I)V

    goto :goto_32

    .line 1047
    :cond_4a
    iget-object v0, v15, Larqp;->J:Laqqw;

    if-nez v0, :cond_4b

    .line 1048
    sget-object v0, Laqqw;->c:Laqqw;

    .line 1049
    :cond_4b
    iget v1, v0, Laqqw;->a:I

    const v2, 0x303c1d6

    if-ne v1, v2, :cond_4c

    .line 1050
    iget-object v0, v0, Laqqw;->b:Ljava/lang/Object;

    check-cast v0, Lbcdm;

    goto :goto_34

    .line 1057
    :cond_4c
    sget-object v0, Lbcdm;->K:Lbcdm;

    .line 1051
    :goto_34
    iget-object v1, v6, Lxan;->aM:Lxdo;

    iget-object v2, v6, Lxan;->aS:Lalmp;

    .line 1052
    invoke-virtual {v1, v2}, Lallh;->a(Lalmp;)Lalmp;

    move-result-object v1

    .line 1053
    invoke-static/range {v32 .. v32}, Ljava/lang/Boolean;->valueOf(Z)Ljava/lang/Boolean;

    move-result-object v2

    invoke-virtual {v1, v8, v2}, Lalmp;->a(Ljava/lang/String;Ljava/lang/Object;)V

    .line 1054
    iget-object v2, v6, Lxan;->aM:Lxdo;

    invoke-virtual {v2, v1, v0}, Lallh;->a(Lalmp;Ljava/lang/Object;)Landroid/view/View;

    move-result-object v0

    .line 1055
    iget-object v1, v6, Lxan;->aI:Landroid/widget/FrameLayout;

    invoke-virtual {v1, v0}, Landroid/widget/FrameLayout;->addView(Landroid/view/View;)V

    .line 1056
    iget-object v0, v6, Lxan;->aI:Landroid/widget/FrameLayout;

    const/4 v1, 0x0

    invoke-virtual {v0, v1}, Landroid/widget/FrameLayout;->setVisibility(I)V

    move/from16 v0, p2

    .line 734
    :goto_35
    invoke-direct {v6, v15, v0}, Lxan;->b(Larqp;Z)V

    .line 735
    iget-boolean v2, v6, Lxan;->m:Z

    if-eqz v2, :cond_62

    .line 736
    iget-object v2, v15, Larqp;->w:Larot;

    if-eqz v2, :cond_4d

    goto :goto_36

    .line 1009
    :cond_4d
    sget-object v2, Larot;->c:Larot;

    .line 737
    :goto_36
    iget v2, v2, Larot;->a:I

    const/4 v3, 0x1

    and-int/2addr v2, v3

    if-eqz v2, :cond_61

    .line 738
    invoke-static {v15}, Lxan;->j(Larqp;)Larqd;

    move-result-object v2

    if-eqz v2, :cond_4e

    goto/16 :goto_48

    .line 944
    :cond_4e
    iget-object v2, v15, Larqp;->w:Larot;

    if-eqz v2, :cond_4f

    goto :goto_37

    .line 1009
    :cond_4f
    sget-object v2, Larot;->c:Larot;

    .line 945
    :goto_37
    iget-object v2, v2, Larot;->b:Laror;

    if-eqz v2, :cond_50

    :goto_38
    move-object v4, v2

    move-object/from16 v2, v31

    goto :goto_39

    .line 1008
    :cond_50
    sget-object v2, Laror;->s:Laror;

    goto :goto_38

    .line 946
    :goto_39
    invoke-direct {v6, v4, v2}, Lxan;->a(Laror;Ljava/util/Map;)V

    .line 947
    iget-object v5, v6, Lxan;->aj:Lxbd;

    iget-object v5, v5, Lxbd;->l:Lanuu;

    invoke-virtual {v5}, Lanuu;->a()Z

    move-result v5

    if-eqz v5, :cond_51

    .line 948
    invoke-direct {v6, v4, v2}, Lxan;->b(Laror;Ljava/util/Map;)V

    .line 949
    :cond_51
    iget-object v5, v6, Lxan;->e:Lxdv;

    iget-object v8, v6, Lxan;->aj:Lxbd;

    iget-object v12, v8, Lxbd;->b:Landroid/view/View;

    iget-object v11, v8, Lxbd;->d:Landroid/view/View;

    .line 950
    iget-object v8, v5, Lxdv;->b:Lxem;

    .line 951
    iget-object v1, v15, Larqp;->f:Ljava/lang/String;

    .line 952
    invoke-virtual {v8, v1, v4, v0}, Lxem;->a(Ljava/lang/String;Laror;Z)Laqwb;

    move-result-object v1

    .line 953
    iget-object v8, v5, Lxdv;->b:Lxem;

    .line 954
    iget-object v10, v15, Larqp;->f:Ljava/lang/String;

    .line 955
    invoke-virtual {v8, v10, v4, v0}, Lxem;->b(Ljava/lang/String;Laror;Z)Laqwb;

    move-result-object v10

    if-nez v1, :cond_52

    move/from16 v33, v9

    move-object v5, v11

    move-object v3, v12

    move-object/from16 v27, v14

    move-object/from16 v28, v15

    const/4 v1, 0x4

    const/4 v7, 0x0

    goto/16 :goto_3c

    :cond_52
    if-eqz v10, :cond_55

    .line 1001
    iget-object v8, v5, Lxdv;->h:Ljava/util/Map;

    invoke-static {v1, v12, v8}, Lxdv;->a(Laqwb;Landroid/view/View;Ljava/util/Map;)V

    .line 1002
    iget-object v8, v5, Lxdv;->h:Ljava/util/Map;

    invoke-static {v10, v11, v8}, Lxdv;->b(Laqwb;Landroid/view/View;Ljava/util/Map;)V

    .line 1003
    iget v1, v1, Laqwb;->a:I

    and-int/lit16 v1, v1, 0x400

    if-eqz v1, :cond_53

    move/from16 v33, v9

    move-object v3, v10

    move-object/from16 p2, v11

    move-object/from16 v27, v14

    move-object/from16 v28, v15

    const/4 v7, 0x0

    move-object v15, v12

    goto :goto_3a

    .line 1007
    :cond_53
    new-instance v1, Lxdy;

    move-object v8, v1

    move/from16 v33, v9

    move-object v9, v5

    move-object v3, v10

    const/4 v7, 0x0

    move-object v10, v15

    move-object/from16 p2, v11

    move-object v11, v4

    move-object/from16 v17, v12

    move v12, v0

    move-object v13, v14

    move-object/from16 v27, v14

    move-object v14, v2

    move-object/from16 v28, v15

    move-object/from16 v15, v17

    move-object/from16 v16, p2

    invoke-direct/range {v8 .. v16}, Lxdy;-><init>(Lxdv;Larqp;Laror;ZLadzv;Ljava/util/Map;Landroid/view/View;Landroid/view/View;)V

    invoke-virtual {v15, v1}, Landroid/view/View;->setOnClickListener(Landroid/view/View$OnClickListener;)V

    .line 1003
    :goto_3a
    iget v1, v3, Laqwb;->a:I

    and-int/lit16 v1, v1, 0x400

    if-eqz v1, :cond_54

    move-object/from16 v5, p2

    move-object v3, v15

    goto :goto_3b

    .line 1006
    :cond_54
    new-instance v1, Lxdx;

    move-object v8, v1

    move-object v9, v5

    move-object/from16 v10, v28

    move-object v11, v4

    move v12, v0

    move-object/from16 v13, v27

    move-object v14, v2

    move-object v3, v15

    move-object/from16 v16, p2

    invoke-direct/range {v8 .. v16}, Lxdx;-><init>(Lxdv;Larqp;Laror;ZLadzv;Ljava/util/Map;Landroid/view/View;Landroid/view/View;)V

    move-object/from16 v5, p2

    invoke-virtual {v5, v1}, Landroid/view/View;->setOnClickListener(Landroid/view/View$OnClickListener;)V

    .line 1004
    :goto_3b
    invoke-virtual {v3, v7}, Landroid/view/View;->setVisibility(I)V

    .line 1005
    invoke-virtual {v5, v7}, Landroid/view/View;->setVisibility(I)V

    move-object/from16 v3, v28

    const/4 v1, 0x4

    goto :goto_3d

    :cond_55
    move/from16 v33, v9

    move-object v5, v11

    move-object v3, v12

    move-object/from16 v27, v14

    move-object/from16 v28, v15

    const/4 v7, 0x0

    const/4 v1, 0x4

    .line 956
    :goto_3c
    invoke-virtual {v3, v1}, Landroid/view/View;->setVisibility(I)V

    .line 957
    invoke-virtual {v3, v7}, Landroid/view/View;->setClickable(Z)V

    .line 958
    invoke-virtual {v5, v1}, Landroid/view/View;->setVisibility(I)V

    .line 959
    invoke-virtual {v5, v7}, Landroid/view/View;->setClickable(Z)V

    move-object/from16 v3, v28

    .line 960
    :goto_3d
    iget-boolean v5, v3, Larqp;->S:Z

    if-eqz v5, :cond_57

    :cond_56
    :goto_3e
    move-object/from16 v5, v27

    goto/16 :goto_44

    .line 983
    :cond_57
    iget-object v5, v6, Lxan;->H:Lxbv;

    iget-object v8, v6, Lxan;->o:Landroid/view/View;

    iget-object v9, v6, Lxan;->aj:Lxbd;

    iget-object v10, v9, Lxbd;->f:Landroid/widget/ImageView;

    iget-object v11, v9, Lxbd;->e:Landroid/view/ViewGroup;

    iget-object v12, v9, Lxbd;->g:Landroid/widget/ImageView;

    iget-object v9, v9, Lxbd;->h:Landroid/widget/ImageView;

    .line 984
    iget-object v13, v3, Larqp;->f:Ljava/lang/String;

    move-object/from16 v16, v5

    move-object/from16 v17, v8

    move-object/from16 v18, v10

    move-object/from16 v19, v11

    move-object/from16 v20, v12

    move-object/from16 v21, v9

    move-object/from16 v22, v13

    move-object/from16 v23, v4

    move-object/from16 v24, v27

    move-object/from16 v25, v2

    move/from16 v26, v0

    .line 985
    invoke-virtual/range {v16 .. v26}, Lxbv;->a(Landroid/view/View;Landroid/widget/ImageView;Landroid/view/ViewGroup;Landroid/widget/ImageView;Landroid/widget/ImageView;Ljava/lang/String;Laror;Ladzv;Ljava/util/Map;Z)V

    .line 986
    iget-object v0, v4, Laror;->g:Lasjz;

    if-eqz v0, :cond_58

    goto :goto_3f

    .line 999
    :cond_58
    sget-object v0, Lasjz;->c:Lasjz;

    .line 987
    :goto_3f
    iget-object v0, v0, Lasjz;->b:Lasjx;

    if-eqz v0, :cond_59

    goto :goto_40

    .line 998
    :cond_59
    sget-object v0, Lasjx;->n:Lasjx;

    .line 988
    :goto_40
    iget v0, v0, Lasjx;->a:I

    and-int/lit16 v0, v0, 0x2000

    if-eqz v0, :cond_56

    .line 989
    iget-object v0, v6, Lxan;->o:Landroid/view/View;

    const v5, 0x7f0b0304

    invoke-virtual {v0, v5}, Landroid/view/View;->findViewById(I)Landroid/view/View;

    move-result-object v0

    check-cast v0, Landroid/widget/TextView;

    .line 990
    iget-object v5, v4, Laror;->g:Lasjz;

    if-eqz v5, :cond_5a

    goto :goto_41

    .line 997
    :cond_5a
    sget-object v5, Lasjz;->c:Lasjz;

    .line 991
    :goto_41
    iget-object v5, v5, Lasjz;->b:Lasjx;

    if-eqz v5, :cond_5b

    goto :goto_42

    .line 996
    :cond_5b
    sget-object v5, Lasjx;->n:Lasjx;

    .line 992
    :goto_42
    iget-object v5, v5, Lasjx;->l:Latho;

    if-eqz v5, :cond_5c

    goto :goto_43

    .line 995
    :cond_5c
    sget-object v5, Latho;->f:Latho;

    .line 993
    :goto_43
    invoke-static {v5}, Lakzk;->a(Latho;)Landroid/text/Spanned;

    move-result-object v5

    .line 994
    invoke-virtual {v0, v5}, Landroid/widget/TextView;->setText(Ljava/lang/CharSequence;)V

    goto :goto_3e

    .line 961
    :goto_44
    invoke-direct {v6, v4, v5, v2}, Lxan;->a(Laror;Ladzv;Ljava/util/Map;)V

    .line 962
    iget-object v0, v6, Lxan;->aj:Lxbd;

    iget-object v0, v0, Lxbd;->a:Landroid/view/ViewGroup;

    const-string v4, "com.google.android.libraries.youtube.comment.comment_thread_has_replies_key"

    move-object/from16 v7, p1

    const/4 v8, 0x0

    .line 963
    invoke-virtual {v7, v4}, Lalmp;->a(Ljava/lang/String;)Ljava/lang/Object;

    move-result-object v9

    instance-of v9, v9, Ljava/lang/Boolean;

    if-eqz v9, :cond_5e

    .line 964
    invoke-virtual {v7, v4}, Lalmp;->a(Ljava/lang/String;)Ljava/lang/Object;

    move-result-object v4

    check-cast v4, Ljava/lang/Boolean;

    invoke-virtual {v4}, Ljava/lang/Boolean;->booleanValue()Z

    move-result v4

    if-eqz v4, :cond_5d

    .line 965
    invoke-virtual {v0}, Landroid/view/View;->getPaddingLeft()I

    move-result v4

    .line 966
    invoke-virtual {v0}, Landroid/view/View;->getPaddingTop()I

    move-result v7

    .line 967
    invoke-virtual {v0}, Landroid/view/View;->getPaddingRight()I

    move-result v9

    .line 968
    invoke-virtual {v0, v4, v7, v9, v8}, Landroid/view/View;->setPadding(IIII)V

    goto :goto_45

    .line 974
    :cond_5d
    invoke-virtual {v0}, Landroid/view/View;->getPaddingLeft()I

    move-result v4

    .line 975
    invoke-virtual {v0}, Landroid/view/View;->getPaddingTop()I

    move-result v7

    .line 976
    invoke-virtual {v0}, Landroid/view/View;->getPaddingRight()I

    move-result v9

    .line 977
    invoke-virtual {v0}, Landroid/view/View;->getResources()Landroid/content/res/Resources;

    move-result-object v10

    const v11, 0x7f070424

    .line 978
    invoke-virtual {v10, v11}, Landroid/content/res/Resources;->getDimensionPixelSize(I)I

    move-result v10

    .line 979
    invoke-virtual {v0, v4, v7, v9, v10}, Landroid/view/View;->setPadding(IIII)V

    :cond_5e
    :goto_45
    const/4 v0, 0x0

    .line 969
    :goto_46
    iget-object v4, v6, Lxan;->aj:Lxbd;

    iget-object v4, v4, Lxbd;->a:Landroid/view/ViewGroup;

    invoke-virtual {v4}, Landroid/view/ViewGroup;->getChildCount()I

    move-result v4

    if-ge v0, v4, :cond_60

    .line 970
    iget-object v4, v6, Lxan;->aj:Lxbd;

    iget-object v4, v4, Lxbd;->a:Landroid/view/ViewGroup;

    invoke-virtual {v4, v0}, Landroid/view/ViewGroup;->getChildAt(I)Landroid/view/View;

    move-result-object v4

    .line 971
    invoke-virtual {v4}, Landroid/view/View;->getVisibility()I

    move-result v4

    if-eqz v4, :cond_5f

    add-int/lit8 v0, v0, 0x1

    goto :goto_46

    :cond_5f
    const/4 v0, 0x0

    goto :goto_47

    :cond_60
    const/16 v0, 0x8

    .line 972
    :goto_47
    iget-object v4, v6, Lxan;->aj:Lxbd;

    iget-object v4, v4, Lxbd;->a:Landroid/view/ViewGroup;

    invoke-virtual {v4, v0}, Landroid/view/ViewGroup;->setVisibility(I)V

    const/16 v4, 0x8

    goto/16 :goto_51

    :cond_61
    :goto_48
    move/from16 v33, v9

    move-object v5, v14

    move-object v3, v15

    move-object/from16 v2, v31

    const/4 v1, 0x4

    const/4 v8, 0x0

    .line 739
    iget-object v0, v6, Lxan;->aj:Lxbd;

    iget-object v0, v0, Lxbd;->a:Landroid/view/ViewGroup;

    const/16 v4, 0x8

    invoke-virtual {v0, v4}, Landroid/view/ViewGroup;->setVisibility(I)V

    goto/16 :goto_51

    :cond_62
    move/from16 v33, v9

    move-object v5, v14

    move-object v3, v15

    move-object/from16 v2, v31

    const/4 v1, 0x4

    const/16 v4, 0x8

    const/4 v8, 0x0

    .line 1010
    iget-object v7, v3, Larqp;->w:Larot;

    if-eqz v7, :cond_63

    goto :goto_49

    .line 1033
    :cond_63
    sget-object v7, Larot;->c:Larot;

    .line 1011
    :goto_49
    iget v7, v7, Larot;->a:I

    const/4 v9, 0x1

    and-int/2addr v7, v9

    if-eqz v7, :cond_6b

    .line 1012
    invoke-static {v3}, Lxan;->j(Larqp;)Larqd;

    move-result-object v7

    if-eqz v7, :cond_64

    goto/16 :goto_50

    .line 1014
    :cond_64
    iget-object v7, v3, Larqp;->w:Larot;

    if-eqz v7, :cond_65

    goto :goto_4a

    .line 1033
    :cond_65
    sget-object v7, Larot;->c:Larot;

    .line 1015
    :goto_4a
    iget-object v7, v7, Larot;->b:Laror;

    if-eqz v7, :cond_66

    goto :goto_4b

    .line 1032
    :cond_66
    sget-object v7, Laror;->s:Laror;

    .line 1016
    :goto_4b
    invoke-direct {v6, v7, v2}, Lxan;->a(Laror;Ljava/util/Map;)V

    .line 1017
    invoke-direct {v6, v7, v2}, Lxan;->b(Laror;Ljava/util/Map;)V

    .line 1018
    iget-object v9, v6, Lxan;->e:Lxdv;

    iget-object v10, v6, Lxan;->B:Larqp;

    iget-object v11, v6, Lxan;->aj:Lxbd;

    iget-object v12, v11, Lxbd;->b:Landroid/view/View;

    move-object/from16 v21, v12

    check-cast v21, Landroid/widget/ImageView;

    iget-object v12, v11, Lxbd;->d:Landroid/view/View;

    move-object/from16 v22, v12

    check-cast v22, Landroid/widget/ImageView;

    iget-object v11, v11, Lxbd;->c:Landroid/widget/TextView;

    iget-boolean v12, v6, Lxan;->ab:Z

    if-nez v12, :cond_67

    .line 1019
    iget-object v12, v9, Lxdv;->e:Ljava/util/Map;

    goto :goto_4c

    .line 1031
    :cond_67
    iget-object v12, v9, Lxdv;->g:Ljava/util/Map;

    :goto_4c
    move-object/from16 v25, v12

    const/16 v26, 0x0

    move-object/from16 v16, v9

    move-object/from16 v17, v7

    move-object/from16 v18, v2

    move/from16 v19, v0

    move-object/from16 v20, v10

    move-object/from16 v23, v11

    move-object/from16 v24, v5

    .line 1020
    invoke-virtual/range {v16 .. v26}, Lxdv;->a(Laror;Ljava/util/Map;ZLarqp;Landroid/widget/ImageView;Landroid/widget/ImageView;Landroid/widget/TextView;Ladzv;Ljava/util/Map;Lxee;)V

    .line 1021
    iget-boolean v9, v3, Larqp;->S:Z

    if-eqz v9, :cond_68

    goto :goto_4d

    .line 1028
    :cond_68
    iget-object v9, v6, Lxan;->H:Lxbv;

    iget-object v10, v6, Lxan;->o:Landroid/view/View;

    iget-object v11, v6, Lxan;->aj:Lxbd;

    iget-object v12, v11, Lxbd;->f:Landroid/widget/ImageView;

    iget-object v13, v11, Lxbd;->e:Landroid/view/ViewGroup;

    iget-object v14, v11, Lxbd;->g:Landroid/widget/ImageView;

    iget-object v11, v11, Lxbd;->h:Landroid/widget/ImageView;

    .line 1029
    iget-object v15, v3, Larqp;->f:Ljava/lang/String;

    move-object/from16 v16, v9

    move-object/from16 v17, v10

    move-object/from16 v18, v12

    move-object/from16 v19, v13

    move-object/from16 v20, v14

    move-object/from16 v21, v11

    move-object/from16 v22, v15

    move-object/from16 v23, v7

    move-object/from16 v24, v5

    move-object/from16 v25, v2

    move/from16 v26, v0

    .line 1030
    invoke-virtual/range {v16 .. v26}, Lxbv;->a(Landroid/view/View;Landroid/widget/ImageView;Landroid/view/ViewGroup;Landroid/widget/ImageView;Landroid/widget/ImageView;Ljava/lang/String;Laror;Ladzv;Ljava/util/Map;Z)V

    .line 1022
    :goto_4d
    invoke-direct {v6, v7, v5, v2}, Lxan;->a(Laror;Ladzv;Ljava/util/Map;)V

    const/4 v0, 0x0

    .line 1023
    :goto_4e
    iget-object v7, v6, Lxan;->al:Landroid/view/ViewGroup;

    invoke-virtual {v7}, Landroid/view/ViewGroup;->getChildCount()I

    move-result v7

    if-ge v0, v7, :cond_6a

    .line 1024
    iget-object v7, v6, Lxan;->al:Landroid/view/ViewGroup;

    invoke-virtual {v7, v0}, Landroid/view/ViewGroup;->getChildAt(I)Landroid/view/View;

    move-result-object v7

    .line 1025
    invoke-virtual {v7}, Landroid/view/View;->getVisibility()I

    move-result v7

    if-eqz v7, :cond_69

    add-int/lit8 v0, v0, 0x1

    goto :goto_4e

    :cond_69
    const/4 v0, 0x0

    goto :goto_4f

    :cond_6a
    const/16 v0, 0x8

    .line 1026
    :goto_4f
    iget-object v7, v6, Lxan;->al:Landroid/view/ViewGroup;

    invoke-virtual {v7, v0}, Landroid/view/ViewGroup;->setVisibility(I)V

    goto :goto_51

    .line 1013
    :cond_6b
    :goto_50
    iget-object v0, v6, Lxan;->al:Landroid/view/ViewGroup;

    invoke-virtual {v0, v4}, Landroid/view/ViewGroup;->setVisibility(I)V

    .line 740
    :goto_51
    iget-object v0, v3, Larqp;->Y:Lazpz;

    if-eqz v0, :cond_6c

    goto :goto_52

    .line 943
    :cond_6c
    sget-object v0, Lazpz;->a:Lazpz;

    .line 741
    :goto_52
    sget-object v7, Lcom/google/protos/youtube/api/innertube/ButtonRendererOuterClass;->buttonRenderer:Lapig;

    .line 742
    invoke-static {v7}, Lapia;->access$000(Laphm;)Lapig;

    move-result-object v7

    .line 743
    invoke-virtual {v0, v7}, Lapie;->a(Lapig;)V

    .line 744
    iget-object v0, v0, Lapie;->h:Laphr;

    iget-object v9, v7, Lapig;->d:Lapid;

    invoke-virtual {v0, v9}, Laphr;->b(Laphu;)Ljava/lang/Object;

    move-result-object v0

    if-nez v0, :cond_6d

    .line 745
    iget-object v0, v7, Lapig;->b:Ljava/lang/Object;

    goto :goto_53

    .line 942
    :cond_6d
    invoke-virtual {v7, v0}, Lapig;->a(Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v0

    .line 746
    :goto_53
    check-cast v0, Laqvi;

    .line 747
    iget v0, v0, Laqvi;->a:I

    and-int/lit16 v0, v0, 0x80

    if-nez v0, :cond_6e

    const/4 v10, 0x0

    goto/16 :goto_59

    .line 912
    :cond_6e
    iget-object v0, v3, Larqp;->Y:Lazpz;

    if-eqz v0, :cond_6f

    goto :goto_54

    .line 941
    :cond_6f
    sget-object v0, Lazpz;->a:Lazpz;

    .line 913
    :goto_54
    sget-object v7, Lcom/google/protos/youtube/api/innertube/ButtonRendererOuterClass;->buttonRenderer:Lapig;

    .line 914
    invoke-static {v7}, Lapia;->access$000(Laphm;)Lapig;

    move-result-object v7

    .line 915
    invoke-virtual {v0, v7}, Lapie;->a(Lapig;)V

    .line 916
    iget-object v0, v0, Lapie;->h:Laphr;

    iget-object v9, v7, Lapig;->d:Lapid;

    invoke-virtual {v0, v9}, Laphr;->b(Laphu;)Ljava/lang/Object;

    move-result-object v0

    if-nez v0, :cond_70

    .line 917
    iget-object v0, v7, Lapig;->b:Ljava/lang/Object;

    goto :goto_55

    .line 940
    :cond_70
    invoke-virtual {v7, v0}, Lapig;->a(Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v0

    .line 918
    :goto_55
    check-cast v0, Laqvi;

    .line 919
    iget-object v7, v6, Lxan;->a:Landroid/content/Context;

    const v9, 0x7f040590

    invoke-static {v7, v9, v8}, Lypu;->a(Landroid/content/Context;II)I

    move-result v7

    .line 920
    iget-object v9, v6, Lxan;->a:Landroid/content/Context;

    const v10, 0x7f08022d

    invoke-static {v9, v10}, Lob;->a(Landroid/content/Context;I)Landroid/graphics/drawable/Drawable;

    move-result-object v9

    .line 921
    invoke-static {v9, v7}, Lpt;->a(Landroid/graphics/drawable/Drawable;I)V

    .line 922
    iget-object v7, v6, Lxan;->aK:Landroid/widget/TextView;

    const/4 v10, 0x0

    invoke-virtual {v7, v9, v10, v10, v10}, Landroid/widget/TextView;->setCompoundDrawablesWithIntrinsicBounds(Landroid/graphics/drawable/Drawable;Landroid/graphics/drawable/Drawable;Landroid/graphics/drawable/Drawable;Landroid/graphics/drawable/Drawable;)V

    .line 923
    iget-object v7, v6, Lxan;->aK:Landroid/widget/TextView;

    .line 924
    iget-object v9, v0, Laqvi;->g:Latho;

    if-eqz v9, :cond_71

    goto :goto_56

    .line 939
    :cond_71
    sget-object v9, Latho;->f:Latho;

    .line 925
    :goto_56
    invoke-static {v9}, Lakzk;->a(Latho;)Landroid/text/Spanned;

    move-result-object v9

    invoke-virtual {v7, v9}, Landroid/widget/TextView;->setText(Ljava/lang/CharSequence;)V

    .line 926
    iget-object v7, v6, Lxan;->aK:Landroid/widget/TextView;

    .line 927
    iget v9, v0, Laqvi;->a:I

    const v11, 0x8000

    and-int/2addr v9, v11

    if-eqz v9, :cond_73

    .line 928
    iget-object v9, v0, Laqvi;->p:Lapol;

    if-eqz v9, :cond_72

    goto :goto_57

    .line 937
    :cond_72
    sget-object v9, Lapol;->c:Lapol;

    .line 929
    :goto_57
    iget-object v11, v9, Lapol;->b:Ljava/lang/String;

    goto :goto_58

    :cond_73
    move-object/from16 v11, v30

    .line 930
    :goto_58
    invoke-virtual {v7, v11}, Landroid/widget/TextView;->setContentDescription(Ljava/lang/CharSequence;)V

    .line 931
    iget-object v7, v6, Lxan;->aK:Landroid/widget/TextView;

    new-instance v9, Lxao;

    invoke-direct {v9, v6, v0, v5}, Lxao;-><init>(Lxan;Laqvi;Ladzv;)V

    invoke-virtual {v7, v9}, Landroid/widget/TextView;->setOnClickListener(Landroid/view/View$OnClickListener;)V

    .line 932
    iget-object v7, v6, Lxan;->aK:Landroid/widget/TextView;

    invoke-virtual {v7, v8}, Landroid/widget/TextView;->setVisibility(I)V

    .line 933
    new-instance v7, Ladzq;

    .line 934
    iget-object v0, v0, Laqvi;->r:Lapgi;

    .line 935
    invoke-direct {v7, v0}, Ladzq;-><init>(Lapgi;)V

    .line 936
    invoke-interface {v5, v7}, Ladzv;->b(Laebg;)V

    .line 748
    :goto_59
    iget-object v0, v6, Lxan;->t:Landroid/view/ViewGroup;

    if-eqz v0, :cond_7a

    .line 749
    invoke-static {v3}, Lxan;->j(Larqp;)Larqd;

    move-result-object v0

    if-nez v0, :cond_74

    .line 750
    iget-object v0, v6, Lxan;->t:Landroid/view/ViewGroup;

    invoke-virtual {v0, v4}, Landroid/view/ViewGroup;->setVisibility(I)V

    goto :goto_5e

    .line 894
    :cond_74
    iget-object v7, v0, Larqd;->b:Laqvp;

    if-eqz v7, :cond_75

    goto :goto_5a

    .line 911
    :cond_75
    sget-object v7, Laqvp;->d:Laqvp;

    .line 895
    :goto_5a
    iget-object v9, v6, Lxan;->u:Landroid/widget/ImageView;

    .line 896
    invoke-direct {v6, v7, v9, v5, v2}, Lxan;->a(Laqvp;Landroid/widget/ImageView;Ladzv;Ljava/util/Map;)Z

    move-result v7

    .line 897
    iget-object v9, v0, Larqd;->c:Laqvp;

    if-eqz v9, :cond_76

    goto :goto_5b

    .line 911
    :cond_76
    sget-object v9, Laqvp;->d:Laqvp;

    .line 898
    :goto_5b
    iget-object v11, v6, Lxan;->v:Landroid/widget/ImageView;

    .line 899
    invoke-direct {v6, v9, v11, v5, v2}, Lxan;->a(Laqvp;Landroid/widget/ImageView;Ladzv;Ljava/util/Map;)Z

    move-result v9

    or-int/2addr v7, v9

    .line 900
    iget-object v9, v0, Larqd;->d:Laqvp;

    if-eqz v9, :cond_77

    goto :goto_5c

    .line 911
    :cond_77
    sget-object v9, Laqvp;->d:Laqvp;

    .line 901
    :goto_5c
    iget-object v11, v6, Lxan;->w:Landroid/widget/ImageView;

    .line 902
    invoke-direct {v6, v9, v11, v5, v2}, Lxan;->a(Laqvp;Landroid/widget/ImageView;Ladzv;Ljava/util/Map;)Z

    move-result v9

    or-int/2addr v7, v9

    .line 903
    iget-object v0, v0, Larqd;->e:Laqvp;

    if-eqz v0, :cond_78

    goto :goto_5d

    .line 911
    :cond_78
    sget-object v0, Laqvp;->d:Laqvp;

    .line 904
    :goto_5d
    iget-object v9, v6, Lxan;->x:Landroid/widget/ImageView;

    .line 905
    invoke-direct {v6, v0, v9, v5, v2}, Lxan;->a(Laqvp;Landroid/widget/ImageView;Ladzv;Ljava/util/Map;)Z

    move-result v0

    or-int/2addr v0, v7

    if-eqz v0, :cond_79

    .line 906
    iget-object v0, v6, Lxan;->t:Landroid/view/ViewGroup;

    .line 907
    invoke-virtual {v0}, Landroid/view/ViewGroup;->getViewTreeObserver()Landroid/view/ViewTreeObserver;

    move-result-object v0

    new-instance v2, Lxba;

    invoke-direct {v2, v6}, Lxba;-><init>(Lxan;)V

    .line 908
    invoke-virtual {v0, v2}, Landroid/view/ViewTreeObserver;->addOnGlobalLayoutListener(Landroid/view/ViewTreeObserver$OnGlobalLayoutListener;)V

    .line 909
    iget-object v0, v6, Lxan;->t:Landroid/view/ViewGroup;

    invoke-virtual {v0, v8}, Landroid/view/ViewGroup;->setVisibility(I)V

    goto :goto_5e

    .line 910
    :cond_79
    iget-object v0, v6, Lxan;->t:Landroid/view/ViewGroup;

    invoke-virtual {v0, v4}, Landroid/view/ViewGroup;->setVisibility(I)V

    .line 751
    :cond_7a
    :goto_5e
    iget-object v0, v6, Lxan;->ap:Landroid/widget/TextView;

    invoke-virtual {v0}, Landroid/widget/TextView;->getText()Ljava/lang/CharSequence;

    move-result-object v0

    invoke-interface {v0}, Ljava/lang/CharSequence;->toString()Ljava/lang/String;

    move-result-object v0

    .line 752
    iget-object v2, v6, Lxan;->ao:Landroid/widget/TextView;

    invoke-virtual {v2}, Landroid/widget/TextView;->getText()Ljava/lang/CharSequence;

    move-result-object v2

    invoke-interface {v2}, Ljava/lang/CharSequence;->toString()Ljava/lang/String;

    move-result-object v2

    .line 753
    invoke-direct {v6, v3}, Lxan;->g(Larqp;)Ljava/lang/String;

    move-result-object v5

    .line 754
    iget-object v7, v3, Larqp;->E:Larpb;

    if-eqz v7, :cond_7b

    goto :goto_5f

    .line 893
    :cond_7b
    sget-object v7, Larpb;->f:Larpb;

    .line 755
    :goto_5f
    iget-object v7, v7, Larpb;->d:Larpf;

    if-eqz v7, :cond_7c

    goto :goto_60

    .line 892
    :cond_7c
    sget-object v7, Larpf;->f:Larpf;

    .line 756
    :goto_60
    iget v7, v7, Larpf;->a:I

    and-int/2addr v7, v1

    if-nez v7, :cond_7d

    move-object/from16 v11, v30

    goto :goto_63

    .line 887
    :cond_7d
    iget-object v7, v3, Larqp;->E:Larpb;

    if-eqz v7, :cond_7e

    goto :goto_61

    .line 891
    :cond_7e
    sget-object v7, Larpb;->f:Larpb;

    .line 888
    :goto_61
    iget-object v7, v7, Larpb;->d:Larpf;

    if-eqz v7, :cond_7f

    goto :goto_62

    .line 890
    :cond_7f
    sget-object v7, Larpf;->f:Larpf;

    .line 889
    :goto_62
    iget-object v11, v7, Larpf;->d:Ljava/lang/String;

    .line 758
    :goto_63
    iget-object v7, v6, Lxan;->r:Landroid/widget/TextView;

    invoke-virtual {v7}, Landroid/widget/TextView;->getText()Ljava/lang/CharSequence;

    move-result-object v7

    invoke-interface {v7}, Ljava/lang/CharSequence;->toString()Ljava/lang/String;

    move-result-object v7

    .line 759
    iget-object v9, v6, Lxan;->am:Landroid/widget/TextView;

    invoke-virtual {v9}, Landroid/widget/TextView;->getText()Ljava/lang/CharSequence;

    move-result-object v9

    invoke-interface {v9}, Ljava/lang/CharSequence;->toString()Ljava/lang/String;

    move-result-object v9

    .line 760
    invoke-static {v3}, Lxan;->l(Larqp;)Ljava/lang/CharSequence;

    move-result-object v12

    .line 761
    invoke-static {v3}, Lxan;->i(Larqp;)Ljava/lang/String;

    move-result-object v13

    .line 762
    invoke-static {v3}, Lxan;->m(Larqp;)Ljava/lang/String;

    move-result-object v14

    .line 763
    invoke-static {v3}, Lxan;->h(Larqp;)Ljava/lang/String;

    move-result-object v15

    .line 764
    new-instance v10, Ljava/lang/StringBuilder;

    invoke-direct {v10}, Ljava/lang/StringBuilder;-><init>()V

    const-string v4, ". "

    .line 765
    invoke-static {v0}, Landroid/text/TextUtils;->isEmpty(Ljava/lang/CharSequence;)Z

    move-result v16

    if-nez v16, :cond_80

    .line 766
    invoke-virtual {v10, v0}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    .line 767
    invoke-virtual {v10, v4}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    .line 768
    :cond_80
    invoke-static {v2}, Landroid/text/TextUtils;->isEmpty(Ljava/lang/CharSequence;)Z

    move-result v0

    if-nez v0, :cond_81

    .line 769
    invoke-virtual {v10, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    .line 770
    invoke-virtual {v10, v4}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    .line 771
    :cond_81
    iget-boolean v0, v6, Lxan;->ac:Z

    if-eqz v0, :cond_82

    .line 772
    invoke-virtual {v10, v5}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    .line 773
    invoke-virtual {v10, v4}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    .line 774
    invoke-virtual {v10, v11}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    .line 775
    invoke-virtual {v10, v4}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    .line 776
    invoke-virtual {v10, v9}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    .line 777
    invoke-virtual {v10, v4}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    .line 778
    invoke-virtual {v10, v12}, Ljava/lang/StringBuilder;->append(Ljava/lang/CharSequence;)Ljava/lang/StringBuilder;

    .line 779
    invoke-virtual {v10, v4}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    .line 780
    invoke-virtual {v10, v7}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    .line 781
    invoke-virtual {v10, v4}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    goto :goto_64

    .line 877
    :cond_82
    invoke-virtual {v10, v7}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    .line 878
    invoke-virtual {v10, v4}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    .line 879
    invoke-virtual {v10, v5}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    .line 880
    invoke-virtual {v10, v4}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    .line 881
    invoke-virtual {v10, v11}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    .line 882
    invoke-virtual {v10, v4}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    .line 883
    invoke-virtual {v10, v9}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    .line 884
    invoke-virtual {v10, v4}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    .line 885
    invoke-virtual {v10, v12}, Ljava/lang/StringBuilder;->append(Ljava/lang/CharSequence;)Ljava/lang/StringBuilder;

    .line 886
    invoke-virtual {v10, v4}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    .line 782
    :goto_64
    invoke-direct {v6, v10, v3}, Lxan;->a(Ljava/lang/StringBuilder;Larqp;)V

    .line 783
    invoke-virtual {v10, v14}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    .line 784
    invoke-virtual {v10, v4}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    .line 785
    invoke-virtual {v10, v13}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    .line 786
    invoke-virtual {v10, v4}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    .line 787
    invoke-virtual {v10, v15}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    .line 788
    iget-object v0, v3, Larqp;->J:Laqqw;

    if-nez v0, :cond_83

    .line 789
    sget-object v0, Laqqw;->c:Laqqw;

    .line 790
    :cond_83
    iget v0, v0, Laqqw;->a:I

    const v2, 0x5ec9696

    if-ne v0, v2, :cond_85

    .line 791
    iget-object v0, v6, Lxan;->o:Landroid/view/View;

    const/4 v4, 0x2

    invoke-virtual {v0, v4}, Landroid/view/View;->setImportantForAccessibility(I)V

    .line 792
    iget-object v0, v6, Lxan;->o:Landroid/view/View;

    invoke-virtual {v0, v8}, Landroid/view/View;->setFocusable(Z)V

    .line 793
    iget-object v0, v6, Lxan;->r:Landroid/widget/TextView;

    const/4 v5, 0x1

    invoke-virtual {v0, v5}, Landroid/widget/TextView;->setImportantForAccessibility(I)V

    .line 794
    iget-object v0, v6, Lxan;->aj:Lxbd;

    iget-object v0, v0, Lxbd;->c:Landroid/widget/TextView;

    invoke-virtual {v0, v4}, Landroid/widget/TextView;->setImportantForAccessibility(I)V

    .line 795
    iget-object v0, v6, Lxan;->aj:Lxbd;

    iget-object v0, v0, Lxbd;->j:Landroid/widget/TextView;

    if-eqz v0, :cond_84

    .line 796
    invoke-virtual {v0, v4}, Landroid/widget/TextView;->setImportantForAccessibility(I)V

    .line 797
    :cond_84
    iget-object v0, v6, Lxan;->ai:Landroid/widget/ImageView;

    invoke-virtual {v0, v5}, Landroid/widget/ImageView;->setImportantForAccessibility(I)V

    .line 798
    iget-object v0, v6, Lxan;->aE:Landroid/view/ViewGroup;

    invoke-static {v0, v1}, Lye;->b(Landroid/view/View;I)V

    .line 799
    iget-object v0, v6, Lxan;->r:Landroid/widget/TextView;

    invoke-virtual {v10}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v4

    invoke-virtual {v0, v4}, Landroid/widget/TextView;->setContentDescription(Ljava/lang/CharSequence;)V

    goto :goto_65

    :cond_85
    const/4 v4, 0x2

    const/4 v5, 0x1

    .line 868
    iget-object v0, v6, Lxan;->o:Landroid/view/View;

    invoke-virtual {v0, v5}, Landroid/view/View;->setImportantForAccessibility(I)V

    .line 869
    iget-object v0, v6, Lxan;->o:Landroid/view/View;

    invoke-virtual {v0, v5}, Landroid/view/View;->setFocusable(Z)V

    .line 870
    iget-object v0, v6, Lxan;->r:Landroid/widget/TextView;

    invoke-virtual {v0, v4}, Landroid/widget/TextView;->setImportantForAccessibility(I)V

    .line 871
    iget-object v0, v6, Lxan;->aj:Lxbd;

    iget-object v0, v0, Lxbd;->c:Landroid/widget/TextView;

    invoke-virtual {v0, v8}, Landroid/widget/TextView;->setImportantForAccessibility(I)V

    .line 872
    iget-object v0, v6, Lxan;->aj:Lxbd;

    iget-object v0, v0, Lxbd;->j:Landroid/widget/TextView;

    if-eqz v0, :cond_86

    .line 873
    invoke-virtual {v0, v8}, Landroid/widget/TextView;->setImportantForAccessibility(I)V

    .line 874
    :cond_86
    iget-object v0, v6, Lxan;->ai:Landroid/widget/ImageView;

    invoke-virtual {v0, v8}, Landroid/widget/ImageView;->setImportantForAccessibility(I)V

    .line 875
    iget-object v0, v6, Lxan;->aE:Landroid/view/ViewGroup;

    invoke-virtual {v0, v8}, Landroid/view/ViewGroup;->setImportantForAccessibility(I)V

    .line 876
    iget-object v0, v6, Lxan;->o:Landroid/view/View;

    invoke-virtual {v10}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v4

    invoke-virtual {v0, v4}, Landroid/view/View;->setContentDescription(Ljava/lang/CharSequence;)V

    .line 800
    :goto_65
    invoke-direct {v6, v3}, Lxan;->f(Larqp;)V

    .line 801
    iget-object v0, v6, Lxan;->ai:Landroid/widget/ImageView;

    .line 802
    invoke-virtual {v0}, Landroid/widget/ImageView;->getLayoutParams()Landroid/view/ViewGroup$LayoutParams;

    move-result-object v0

    check-cast v0, Landroid/widget/RelativeLayout$LayoutParams;

    const/16 v4, 0xf

    .line 803
    invoke-virtual {v0, v4, v8}, Landroid/widget/RelativeLayout$LayoutParams;->addRule(II)V

    const v4, 0x7f0b0744

    const/4 v5, 0x3

    .line 804
    invoke-virtual {v0, v5, v4}, Landroid/widget/RelativeLayout$LayoutParams;->addRule(II)V

    const/4 v4, 0x1

    .line 805
    iput-boolean v4, v0, Landroid/widget/RelativeLayout$LayoutParams;->alignWithParent:Z

    .line 806
    iget-object v4, v6, Lxan;->ai:Landroid/widget/ImageView;

    invoke-virtual {v4, v0}, Landroid/widget/ImageView;->setLayoutParams(Landroid/view/ViewGroup$LayoutParams;)V

    .line 807
    invoke-direct {v6, v3}, Lxan;->d(Larqp;)I

    move-result v0

    .line 808
    iget-object v4, v6, Lxan;->aS:Lalmp;

    invoke-static {v4}, Lxan;->a(Lalmp;)Z

    move-result v4

    if-eqz v4, :cond_88

    .line 809
    iget v4, v6, Lxan;->N:I

    .line 810
    iget-object v7, v6, Lxan;->aj:Lxbd;

    iget-object v7, v7, Lxbd;->j:Landroid/widget/TextView;

    const/16 v9, 0x8

    if-eqz v7, :cond_87

    .line 811
    invoke-virtual {v7, v9}, Landroid/widget/TextView;->setVisibility(I)V

    .line 812
    :cond_87
    iget-object v7, v6, Lxan;->aj:Lxbd;

    iget-object v7, v7, Lxbd;->i:Landroid/view/View;

    invoke-virtual {v7, v1}, Landroid/view/View;->setVisibility(I)V

    .line 813
    iget-object v1, v6, Lxan;->aj:Lxbd;

    iget-object v1, v1, Lxbd;->d:Landroid/view/View;

    iget v7, v6, Lxan;->P:I

    iget v10, v6, Lxan;->g:I

    iget v11, v6, Lxan;->Q:I

    invoke-static {v1, v7, v10, v11, v10}, Lxdw;->a(Landroid/view/View;IIII)V

    goto :goto_67

    :cond_88
    const/16 v9, 0x8

    .line 862
    iget v1, v3, Larqp;->l:I

    invoke-static {v1}, Larqr;->a(I)I

    move-result v1

    if-eqz v1, :cond_8a

    if-eq v1, v5, :cond_89

    goto :goto_66

    .line 866
    :cond_89
    iget v4, v6, Lxan;->M:I

    .line 867
    invoke-direct/range {p0 .. p0}, Lxan;->b()V

    goto :goto_67

    .line 864
    :cond_8a
    :goto_66
    iget v4, v6, Lxan;->L:I

    .line 865
    invoke-direct/range {p0 .. p0}, Lxan;->b()V

    .line 814
    :goto_67
    iget-object v1, v6, Lxan;->ah:Landroid/view/View;

    new-instance v7, Landroid/widget/RelativeLayout$LayoutParams;

    const/4 v10, -0x1

    invoke-direct {v7, v4, v10}, Landroid/widget/RelativeLayout$LayoutParams;-><init>(II)V

    invoke-virtual {v1, v7}, Landroid/view/View;->setLayoutParams(Landroid/view/ViewGroup$LayoutParams;)V

    .line 815
    invoke-direct {v6, v0}, Lxan;->a(I)V

    .line 816
    iget-object v1, v6, Lxan;->ai:Landroid/widget/ImageView;

    const/4 v4, 0x1

    invoke-virtual {v1, v4}, Landroid/widget/ImageView;->setImportantForAccessibility(I)V

    .line 817
    iget-object v1, v6, Lxan;->ai:Landroid/widget/ImageView;

    .line 818
    iget-object v4, v3, Larqp;->m:Lbayv;

    if-eqz v4, :cond_8b

    goto :goto_68

    .line 861
    :cond_8b
    sget-object v4, Lbayv;->f:Lbayv;

    .line 819
    :goto_68
    iget-object v4, v4, Lbayv;->d:Lapon;

    if-eqz v4, :cond_8c

    goto :goto_69

    .line 860
    :cond_8c
    sget-object v4, Lapon;->c:Lapon;

    .line 820
    :goto_69
    invoke-static {v1, v4}, Lxan;->a(Landroid/view/View;Lapon;)V

    .line 821
    iget-object v1, v6, Lxan;->ai:Landroid/widget/ImageView;

    const/4 v4, 0x0

    invoke-virtual {v1, v4}, Landroid/widget/ImageView;->setImageBitmap(Landroid/graphics/Bitmap;)V

    .line 822
    iget-object v1, v3, Larqp;->m:Lbayv;

    if-eqz v1, :cond_8d

    goto :goto_6a

    .line 859
    :cond_8d
    sget-object v1, Lbayv;->f:Lbayv;

    .line 823
    :goto_6a
    invoke-static {v1, v0}, Lalis;->b(Lbayv;I)Landroid/net/Uri;

    move-result-object v0

    if-eqz v0, :cond_8f

    .line 824
    iget-object v1, v6, Lxan;->ai:Landroid/widget/ImageView;

    invoke-virtual {v1, v0}, Landroid/widget/ImageView;->setTag(Ljava/lang/Object;)V

    .line 825
    iget-object v1, v6, Lxan;->C:Lalid;

    iget-object v4, v6, Lxan;->ai:Landroid/widget/ImageView;

    invoke-interface {v1, v4, v0}, Lalid;->a(Landroid/widget/ImageView;Landroid/net/Uri;)V

    .line 826
    iget v0, v3, Larqp;->a:I

    and-int/lit16 v0, v0, 0x200

    if-eqz v0, :cond_8e

    .line 827
    iget-object v0, v6, Lxan;->ai:Landroid/widget/ImageView;

    new-instance v1, Lxap;

    invoke-direct {v1, v6, v3}, Lxap;-><init>(Lxan;Larqp;)V

    invoke-virtual {v0, v1}, Landroid/widget/ImageView;->setOnClickListener(Landroid/view/View$OnClickListener;)V

    goto :goto_6b

    .line 858
    :cond_8e
    iget-object v0, v6, Lxan;->ai:Landroid/widget/ImageView;

    const/4 v1, 0x0

    invoke-virtual {v0, v1}, Landroid/widget/ImageView;->setOnClickListener(Landroid/view/View$OnClickListener;)V

    .line 828
    :cond_8f
    :goto_6b
    iget-object v0, v3, Larqp;->h:Lazpz;

    if-eqz v0, :cond_90

    goto :goto_6c

    .line 857
    :cond_90
    sget-object v0, Lazpz;->a:Lazpz;

    .line 829
    :goto_6c
    sget-object v1, Lcom/google/protos/youtube/api/innertube/HintRendererOuterClass;->hintRenderer:Lapig;

    .line 830
    invoke-static {v1}, Lapia;->access$000(Laphm;)Lapig;

    move-result-object v1

    .line 831
    invoke-virtual {v0, v1}, Lapie;->a(Lapig;)V

    .line 832
    iget-object v0, v0, Lapie;->h:Laphr;

    iget-object v1, v1, Lapig;->d:Lapid;

    invoke-virtual {v0, v1}, Laphr;->a(Laphu;)Z

    move-result v0

    if-nez v0, :cond_91

    goto :goto_6f

    .line 846
    :cond_91
    iget-object v0, v6, Lxan;->c:Lwwg;

    .line 847
    iget-object v1, v3, Larqp;->h:Lazpz;

    if-eqz v1, :cond_92

    goto :goto_6d

    .line 857
    :cond_92
    sget-object v1, Lazpz;->a:Lazpz;

    .line 848
    :goto_6d
    sget-object v4, Lcom/google/protos/youtube/api/innertube/HintRendererOuterClass;->hintRenderer:Lapig;

    .line 849
    invoke-static {v4}, Lapia;->access$000(Laphm;)Lapig;

    move-result-object v4

    .line 850
    invoke-virtual {v1, v4}, Lapie;->a(Lapig;)V

    .line 851
    iget-object v1, v1, Lapie;->h:Laphr;

    iget-object v7, v4, Lapig;->d:Lapid;

    invoke-virtual {v1, v7}, Laphr;->b(Laphu;)Ljava/lang/Object;

    move-result-object v1

    if-nez v1, :cond_93

    .line 852
    iget-object v1, v4, Lapig;->b:Ljava/lang/Object;

    goto :goto_6e

    .line 856
    :cond_93
    invoke-virtual {v4, v1}, Lapig;->a(Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v1

    .line 853
    :goto_6e
    check-cast v1, Latrj;

    iget-object v4, v6, Lxan;->ai:Landroid/widget/ImageView;

    iget-object v7, v6, Lxan;->aS:Lalmp;

    .line 854
    iget-object v7, v7, Ladzz;->a:Ladzv;

    .line 855
    invoke-interface {v0, v1, v4, v3, v7}, Lwwg;->a(Latrj;Landroid/view/View;Ljava/lang/Object;Ladzv;)V

    .line 833
    :goto_6f
    iget-object v0, v6, Lxan;->aL:Landroid/view/View;

    .line 834
    iget-boolean v1, v3, Larqp;->O:Z

    if-nez v1, :cond_94

    const/16 v8, 0x8

    .line 835
    :cond_94
    invoke-virtual {v0, v8}, Landroid/view/View;->setVisibility(I)V

    .line 836
    iget-object v0, v6, Lxan;->F:Lwvq;

    iget-object v1, v6, Lxan;->B:Larqp;

    .line 837
    iget-object v0, v0, Lwvq;->b:Ljava/util/Map;

    invoke-static {v0, v1, v6}, Lyll;->a(Ljava/util/Map;Ljava/lang/Object;Ljava/lang/Object;)V

    move/from16 v0, v29

    if-eq v0, v2, :cond_95

    :goto_70
    move/from16 v3, v33

    goto :goto_71

    .line 842
    :cond_95
    iget-object v0, v6, Lxan;->G:Lxem;

    .line 843
    iget-object v1, v3, Larqp;->f:Ljava/lang/String;

    .line 844
    invoke-static {v1}, Lxem;->a(Ljava/lang/String;)Landroid/net/Uri;

    move-result-object v1

    invoke-virtual {v0, v1, v6}, Lxem;->a(Landroid/net/Uri;Lxen;)V

    goto :goto_70

    :goto_71
    if-eq v3, v5, :cond_96

    return-void

    .line 840
    :cond_96
    iget-object v0, v6, Lxan;->aN:Lxdq;

    const/4 v1, 0x1

    .line 841
    iput-boolean v1, v0, Lxdq;->a:Z

    return-void
.end method

.method public final a(Lalmz;)V
    .locals 2

    .line 552
    iget-object p1, p0, Lxan;->aO:Lalrh;

    invoke-virtual {p1}, Lalrh;->a()V

    .line 553
    iget-object p1, p0, Lxan;->o:Landroid/view/View;

    const/4 v0, 0x0

    invoke-virtual {p1, v0}, Landroid/view/View;->setClickable(Z)V

    .line 554
    iget-object p1, p0, Lxan;->F:Lwvq;

    iget-object v0, p0, Lxan;->B:Larqp;

    .line 555
    iget-object v1, p1, Lwvq;->b:Ljava/util/Map;

    invoke-static {v1, v0, p0}, Lyll;->b(Ljava/util/Map;Ljava/lang/Object;Ljava/lang/Object;)Z

    .line 556
    iget-object p1, p1, Lwvq;->b:Ljava/util/Map;

    invoke-static {p1, v0}, Lyll;->c(Ljava/util/Map;Ljava/lang/Object;)V

    .line 557
    iget-object p1, p0, Lxan;->G:Lxem;

    invoke-virtual {p1, p0}, Lxem;->a(Lxen;)V

    .line 558
    invoke-direct {p0}, Lxan;->c()V

    .line 559
    iget-object p1, p0, Lxan;->al:Landroid/view/ViewGroup;

    const/16 v0, 0x8

    if-eqz p1, :cond_0

    .line 560
    invoke-virtual {p1, v0}, Landroid/view/ViewGroup;->setVisibility(I)V

    .line 561
    :cond_0
    iget-object p1, p0, Lxan;->t:Landroid/view/ViewGroup;

    if-eqz p1, :cond_1

    .line 562
    invoke-virtual {p1, v0}, Landroid/view/ViewGroup;->setVisibility(I)V

    .line 563
    :cond_1
    iget-object p1, p0, Lxan;->aK:Landroid/widget/TextView;

    if-eqz p1, :cond_2

    .line 564
    invoke-virtual {p1, v0}, Landroid/widget/TextView;->setVisibility(I)V

    .line 565
    :cond_2
    iget-object p1, p0, Lxan;->aM:Lxdo;

    iget-object v1, p0, Lxan;->y:Landroid/widget/FrameLayout;

    invoke-virtual {p1, v1}, Lallh;->a(Landroid/view/ViewGroup;)V

    .line 566
    iget-object p1, p0, Lxan;->aM:Lxdo;

    iget-object v1, p0, Lxan;->aH:Landroid/widget/FrameLayout;

    invoke-virtual {p1, v1}, Lallh;->a(Landroid/view/ViewGroup;)V

    .line 567
    iget-object p1, p0, Lxan;->aM:Lxdo;

    iget-object v1, p0, Lxan;->aI:Landroid/widget/FrameLayout;

    invoke-virtual {p1, v1}, Lallh;->a(Landroid/view/ViewGroup;)V

    .line 568
    iget-object p1, p0, Lxan;->aM:Lxdo;

    iget-object v1, p0, Lxan;->au:Landroid/widget/FrameLayout;

    invoke-virtual {p1, v1}, Lallh;->a(Landroid/view/ViewGroup;)V

    .line 569
    iget-object p1, p0, Lxan;->aM:Lxdo;

    iget-object v1, p0, Lxan;->aJ:Landroid/widget/FrameLayout;

    invoke-virtual {p1, v1}, Lallh;->a(Landroid/view/ViewGroup;)V

    .line 570
    iget-object p1, p0, Lxan;->aL:Landroid/view/View;

    invoke-virtual {p1, v0}, Landroid/view/View;->setVisibility(I)V

    .line 571
    iget-object p1, p0, Lxan;->ad:Landroid/animation/Animator;

    if-eqz p1, :cond_3

    invoke-virtual {p1}, Landroid/animation/Animator;->isRunning()Z

    move-result p1

    if-eqz p1, :cond_3

    .line 572
    iget-object p1, p0, Lxan;->ad:Landroid/animation/Animator;

    invoke-virtual {p1}, Landroid/animation/Animator;->end()V

    :cond_3
    const/4 p1, 0x0

    .line 573
    iput-object p1, p0, Lxan;->ad:Landroid/animation/Animator;

    return-void
.end method

.method public final a(Landroid/view/View;)V
    .locals 2

    .line 385
    invoke-virtual {p1}, Landroid/view/View;->getVisibility()I

    move-result v0

    if-nez v0, :cond_0

    .line 386
    iget v0, p0, Lxan;->O:I

    iget v1, p0, Lxan;->g:I

    invoke-static {p1, v0, v1, v0, v1}, Lxdw;->a(Landroid/view/View;IIII)V

    :cond_0
    return-void
.end method

.method public final a(Laqvi;Ladzv;Ljava/util/Map;)V
    .locals 3

    .line 576
    iget v0, p1, Laqvi;->a:I

    and-int/lit16 v1, v0, 0x2000

    if-eqz v1, :cond_1

    .line 577
    iget-object v0, p1, Laqvi;->m:Laron;

    if-eqz v0, :cond_0

    goto :goto_0

    .line 585
    :cond_0
    sget-object v0, Laron;->d:Laron;

    goto :goto_0

    :cond_1
    and-int/lit16 v0, v0, 0x4000

    if-eqz v0, :cond_5

    .line 586
    iget-object v0, p1, Laqvi;->n:Laron;

    if-nez v0, :cond_2

    .line 587
    sget-object v0, Laron;->d:Laron;

    .line 578
    :cond_2
    :goto_0
    iget v1, p1, Laqvi;->a:I

    const/high16 v2, 0x80000

    and-int/2addr v1, v2

    if-eqz v1, :cond_3

    .line 579
    new-instance v1, Ladzq;

    .line 580
    iget-object p1, p1, Laqvi;->r:Lapgi;

    .line 581
    invoke-direct {v1, p1}, Ladzq;-><init>(Lapgi;)V

    const/4 p1, 0x3

    const/4 v2, 0x0

    .line 582
    invoke-interface {p2, p1, v1, v2}, Ladzv;->a(ILaebg;Lavyl;)V

    :cond_3
    if-eqz p3, :cond_4

    const-string p1, "com.google.android.libraries.youtube.comment.action_tag"

    const-string p2, ""

    .line 583
    invoke-interface {p3, p1, p2}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    .line 584
    :cond_4
    iget-object p1, p0, Lxan;->d:Laawi;

    invoke-interface {p1, v0, p3}, Laawi;->a(Laron;Ljava/util/Map;)V

    :cond_5
    return-void
.end method

.method public final a(Larqp;)V
    .locals 2

    .line 574
    iget-object v0, p0, Lxan;->aM:Lxdo;

    iget-object v1, p0, Lxan;->aJ:Landroid/widget/FrameLayout;

    invoke-virtual {v0, v1}, Lallh;->a(Landroid/view/ViewGroup;)V

    .line 575
    invoke-direct {p0, p1}, Lxan;->f(Larqp;)V

    return-void
.end method

.method protected final synthetic a(Ljava/lang/Object;)[B
    .locals 0

    .line 588
    check-cast p1, Larqp;

    .line 589
    iget-object p1, p1, Larqp;->H:Lapgi;

    .line 590
    invoke-virtual {p1}, Lapgi;->d()[B

    move-result-object p1

    return-object p1
.end method

.method public final synthetic a_(Ljava/lang/Object;)V
    .locals 5

    .line 1149
    check-cast p1, Lazal;

    .line 1150
    iget-object v0, p0, Lxan;->B:Larqp;

    .line 1151
    iget-object v0, v0, Larqp;->J:Laqqw;

    if-eqz v0, :cond_0

    goto :goto_0

    .line 1188
    :cond_0
    sget-object v0, Laqqw;->c:Laqqw;

    .line 1152
    :goto_0
    iget v0, v0, Laqqw;->a:I

    const v1, 0x5ec9696

    if-ne v0, v1, :cond_9

    .line 1153
    iget-object v0, p0, Lxan;->aS:Lalmp;

    const-string v2, "commentThreadMutator"

    .line 1154
    invoke-virtual {v0, v2}, Lalmp;->a(Ljava/lang/String;)Ljava/lang/Object;

    move-result-object v0

    check-cast v0, Lwvo;

    .line 1155
    sget-object v2, Laqqw;->c:Laqqw;

    invoke-virtual {v2}, Lapia;->createBuilder()Laphz;

    move-result-object v2

    check-cast v2, Laqqz;

    .line 1156
    invoke-virtual {v2}, Laphz;->copyOnWrite()V

    .line 1157
    iget-object v3, v2, Laqqz;->instance:Lapia;

    check-cast v3, Laqqw;

    const/4 v4, 0x0

    if-eqz p1, :cond_8

    .line 1158
    iput-object p1, v3, Laqqw;->b:Ljava/lang/Object;

    .line 1159
    iput v1, v3, Laqqw;->a:I

    .line 1160
    invoke-virtual {v2}, Laphz;->build()Lapia;

    move-result-object p1

    check-cast p1, Laqqw;

    .line 1161
    iget-object v1, p0, Lxan;->B:Larqp;

    .line 1162
    invoke-virtual {v1}, Lapia;->toBuilder()Laphz;

    move-result-object v1

    check-cast v1, Larqs;

    .line 1163
    invoke-virtual {v1}, Laphz;->copyOnWrite()V

    .line 1164
    iget-object v2, v1, Larqs;->instance:Lapia;

    check-cast v2, Larqp;

    if-eqz p1, :cond_7

    .line 1165
    iput-object p1, v2, Larqp;->J:Laqqw;

    .line 1166
    iget p1, v2, Larqp;->b:I

    or-int/lit16 p1, p1, 0x80

    iput p1, v2, Larqp;->b:I

    .line 1167
    invoke-virtual {v1}, Laphz;->build()Lapia;

    move-result-object p1

    check-cast p1, Larqp;

    .line 1168
    iget-object v1, p0, Lxan;->aT:Lxev;

    iget-object v2, p0, Lxan;->B:Larqp;

    invoke-virtual {v1, v2}, Lxev;->c(Larqp;)Z

    move-result v1

    if-eqz v1, :cond_1

    goto :goto_1

    .line 1184
    :cond_1
    iget-object v1, p1, Larqp;->P:Lapir;

    invoke-interface {v1}, Lapir;->size()I

    move-result v1

    if-lez v1, :cond_2

    .line 1185
    iget-object v1, p0, Lxan;->aT:Lxev;

    invoke-virtual {v1, p1}, Lxev;->b(Larqp;)V

    .line 1169
    :cond_2
    :goto_1
    iget-object v1, p0, Lxan;->aT:Lxev;

    iget-object v2, p0, Lxan;->B:Larqp;

    invoke-virtual {v1, v2}, Lxev;->a(Larqp;)Z

    move-result v1

    .line 1170
    iget-boolean v2, p1, Larqp;->Z:Z

    if-ne v1, v2, :cond_3

    goto :goto_2

    .line 1181
    :cond_3
    iget-object v1, p0, Lxan;->aT:Lxev;

    iget-object v2, p0, Lxan;->B:Larqp;

    .line 1182
    invoke-virtual {v1, v2}, Lxev;->a(Larqp;)Z

    move-result v2

    .line 1183
    invoke-virtual {v1, p1, v2}, Lxev;->a(Larqp;Z)V

    .line 1171
    :goto_2
    iget-object v1, p0, Lxan;->aT:Lxev;

    iget-object v2, p0, Lxan;->B:Larqp;

    .line 1172
    invoke-virtual {v1, v2}, Lxev;->d(Larqp;)Larqp;

    move-result-object v1

    .line 1173
    iget-object v2, p1, Larqp;->M:Larqz;

    if-eqz v2, :cond_4

    goto :goto_3

    .line 1180
    :cond_4
    sget-object v2, Larqz;->c:Larqz;

    .line 1174
    :goto_3
    iget-object v2, v2, Larqz;->b:Larqp;

    if-eqz v2, :cond_5

    goto :goto_4

    .line 1179
    :cond_5
    sget-object v2, Larqp;->ac:Larqp;

    .line 1175
    :goto_4
    invoke-static {v1, v2}, Lanuv;->a(Ljava/lang/Object;Ljava/lang/Object;)Z

    move-result v1

    if-nez v1, :cond_6

    .line 1176
    iget-object v1, p0, Lxan;->aT:Lxev;

    iget-object v2, p0, Lxan;->B:Larqp;

    invoke-virtual {v1, v2}, Lxev;->d(Larqp;)Larqp;

    move-result-object v2

    invoke-virtual {v1, p1, v2}, Lxev;->a(Larqp;Larqp;)V

    .line 1177
    :cond_6
    iput-object p1, p0, Lxan;->B:Larqp;

    .line 1178
    invoke-interface {v0}, Lwvo;->a()Z

    move-result v0

    invoke-direct {p0, p1, v0}, Lxan;->b(Larqp;Z)V

    return-void

    .line 1186
    :cond_7
    throw v4

    .line 1187
    :cond_8
    throw v4

    :cond_9
    return-void
.end method

.method public final b(Larqp;)V
    .locals 1

    const/4 v0, 0x0

    .line 179
    invoke-direct {p0, p1, v0}, Lxan;->a(Larqp;Z)V

    .line 180
    iget-object p1, p0, Lxan;->s:Landroid/widget/TextView;

    const/16 v0, 0x8

    invoke-virtual {p1, v0}, Landroid/widget/TextView;->setVisibility(I)V

    return-void
.end method
