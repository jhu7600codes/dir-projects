.class public final Lkel;
.super Lflw;
.source "SourceFile"

# interfaces
.implements Landroid/view/View$OnClickListener;
.implements Lflo;
.implements Lfmc;


# instance fields
.field private final a:Lfyx;

.field private final b:Landroid/view/LayoutInflater;

.field private final c:Landroid/content/res/Resources;

.field private final d:Ladzv;

.field private final e:Laqvi;

.field private final f:Laawi;

.field private final g:Laltp;

.field private final h:I

.field private final i:Laaul;

.field private j:Landroid/widget/ImageView;

.field private k:Ljava/lang/String;

.field private l:I


# direct methods
.method public constructor <init>(Laawi;Laltp;Lfyx;Laaul;Landroid/content/Context;Ladzv;Laqvi;I)V
    .locals 0

    .line 1
    invoke-direct {p0}, Lflw;-><init>()V

    .line 2
    iput-object p3, p0, Lkel;->a:Lfyx;

    .line 3
    invoke-static {p5}, Landroid/view/LayoutInflater;->from(Landroid/content/Context;)Landroid/view/LayoutInflater;

    move-result-object p3

    iput-object p3, p0, Lkel;->b:Landroid/view/LayoutInflater;

    .line 4
    invoke-virtual {p5}, Landroid/content/Context;->getResources()Landroid/content/res/Resources;

    move-result-object p3

    iput-object p3, p0, Lkel;->c:Landroid/content/res/Resources;

    .line 5
    iput-object p1, p0, Lkel;->f:Laawi;

    .line 6
    iput-object p2, p0, Lkel;->g:Laltp;

    .line 7
    iput-object p6, p0, Lkel;->d:Ladzv;

    .line 8
    iput-object p7, p0, Lkel;->e:Laqvi;

    .line 9
    iput p8, p0, Lkel;->h:I

    .line 10
    iput-object p4, p0, Lkel;->i:Laaul;

    return-void
.end method


# virtual methods
.method public final a()I
    .locals 1

    .line 99
    iget v0, p0, Lkel;->h:I

    return v0
.end method

.method public final a(I)V
    .locals 0

    .line 102
    iput p1, p0, Lkel;->l:I

    return-void
.end method

.method public final a(Landroid/view/MenuItem;)V
    .locals 4

    .line 38
    iget-object v0, p0, Lkel;->j:Landroid/widget/ImageView;

    if-nez v0, :cond_2

    .line 39
    iget-object v0, p0, Lkel;->b:Landroid/view/LayoutInflater;

    const v1, 0x7f0e0341

    const/4 v2, 0x0

    const/4 v3, 0x0

    .line 40
    invoke-virtual {v0, v1, v2, v3}, Landroid/view/LayoutInflater;->inflate(ILandroid/view/ViewGroup;Z)Landroid/view/View;

    move-result-object v0

    check-cast v0, Landroid/widget/ImageView;

    iput-object v0, p0, Lkel;->j:Landroid/widget/ImageView;

    .line 41
    iget-object v0, p0, Lkel;->i:Laaul;

    .line 42
    invoke-interface {v0}, Laaul;->a()Laroe;

    move-result-object v0

    const/4 v1, -0x1

    if-eqz v0, :cond_1

    .line 43
    iget v2, v0, Laroe;->a:I

    and-int/lit8 v2, v2, 0x10

    if-eqz v2, :cond_1

    .line 44
    iget-object v0, v0, Laroe;->e:Lawuh;

    if-eqz v0, :cond_0

    goto :goto_0

    .line 77
    :cond_0
    sget-object v0, Lawuh;->bu:Lawuh;

    .line 45
    :goto_0
    iget v1, v0, Lawuh;->Q:I

    :cond_1
    if-lez v1, :cond_2

    .line 46
    iget-object v0, p0, Lkel;->c:Landroid/content/res/Resources;

    invoke-virtual {v0}, Landroid/content/res/Resources;->getDisplayMetrics()Landroid/util/DisplayMetrics;

    move-result-object v0

    invoke-static {v0, v1}, Lyly;->a(Landroid/util/DisplayMetrics;I)I

    move-result v0

    .line 47
    iget-object v1, p0, Lkel;->j:Landroid/widget/ImageView;

    .line 48
    invoke-virtual {v1}, Landroid/widget/ImageView;->getPaddingTop()I

    move-result v2

    iget-object v3, p0, Lkel;->j:Landroid/widget/ImageView;

    invoke-virtual {v3}, Landroid/widget/ImageView;->getPaddingBottom()I

    move-result v3

    .line 49
    invoke-virtual {v1, v0, v2, v0, v3}, Landroid/widget/ImageView;->setPaddingRelative(IIII)V

    :cond_2
    const/4 v0, 0x2

    .line 50
    invoke-interface {p1, v0}, Landroid/view/MenuItem;->setShowAsAction(I)V

    .line 51
    iget-object v0, p0, Lkel;->g:Laltp;

    iget-object v1, p0, Lkel;->e:Laqvi;

    .line 52
    iget-object v1, v1, Laqvi;->e:Lattl;

    if-eqz v1, :cond_3

    goto :goto_1

    .line 76
    :cond_3
    sget-object v1, Lattl;->c:Lattl;

    .line 53
    :goto_1
    iget v1, v1, Lattl;->b:I

    invoke-static {v1}, Lattn;->a(I)Lattn;

    move-result-object v1

    if-eqz v1, :cond_4

    goto :goto_2

    .line 75
    :cond_4
    sget-object v1, Lattn;->a:Lattn;

    .line 54
    :goto_2
    invoke-interface {v0, v1}, Laltp;->a(Lattn;)I

    move-result v0

    # translyte patch: real device confirmed this returns 0 (no local icon
    # mapping) for at least one menu-item value the current server now
    # sends - Resources.getDrawable(0) throws Resources$NotFoundException
    # and crashes the whole options-menu construction. Skip overriding the
    # icon when that happens instead of crashing - the ImageView already
    # has whatever default icon its inflated layout (0x7f0e0341) came with.
    if-eqz v0, :cond_translyte_skip_icon

    .line 55
    iget-object v1, p0, Lkel;->j:Landroid/widget/ImageView;

    iget-object v2, p0, Lkel;->c:Landroid/content/res/Resources;

    invoke-virtual {v2, v0}, Landroid/content/res/Resources;->getDrawable(I)Landroid/graphics/drawable/Drawable;

    move-result-object v0

    invoke-virtual {v1, v0}, Landroid/widget/ImageView;->setImageDrawable(Landroid/graphics/drawable/Drawable;)V

    :cond_translyte_skip_icon

    .line 56
    iget-object v0, p0, Lkel;->j:Landroid/widget/ImageView;

    invoke-virtual {p0}, Lkel;->e()Ljava/lang/CharSequence;

    move-result-object v1

    invoke-virtual {v0, v1}, Landroid/widget/ImageView;->setContentDescription(Ljava/lang/CharSequence;)V

    .line 57
    iget-object v0, p0, Lkel;->j:Landroid/widget/ImageView;

    invoke-virtual {v0, p0}, Landroid/widget/ImageView;->setOnClickListener(Landroid/view/View$OnClickListener;)V

    .line 58
    iget-object v0, p0, Lkel;->j:Landroid/widget/ImageView;

    invoke-interface {p1, v0}, Landroid/view/MenuItem;->setActionView(Landroid/view/View;)Landroid/view/MenuItem;

    .line 59
    iget-object p1, p0, Lkel;->e:Laqvi;

    .line 60
    iget v0, p1, Laqvi;->a:I

    and-int/lit16 v0, v0, 0x800

    if-eqz v0, :cond_a

    .line 61
    iget-object p1, p1, Laqvi;->k:Latrr;

    if-eqz p1, :cond_5

    goto :goto_3

    .line 74
    :cond_5
    sget-object p1, Latrr;->c:Latrr;

    .line 62
    :goto_3
    iget p1, p1, Latrr;->a:I

    const v0, 0x61f53fb

    if-eq p1, v0, :cond_6

    goto :goto_7

    .line 64
    :cond_6
    iget-object p1, p0, Lkel;->a:Lfyx;

    iget-object v1, p0, Lkel;->e:Laqvi;

    .line 65
    iget-object v1, v1, Laqvi;->k:Latrr;

    if-eqz v1, :cond_7

    goto :goto_4

    .line 74
    :cond_7
    sget-object v1, Latrr;->c:Latrr;

    .line 66
    :goto_4
    iget v2, v1, Latrr;->a:I

    if-ne v2, v0, :cond_8

    .line 67
    iget-object v0, v1, Latrr;->b:Ljava/lang/Object;

    check-cast v0, Latrj;

    goto :goto_5

    .line 73
    :cond_8
    sget-object v0, Latrj;->j:Latrj;

    .line 68
    :goto_5
    iget-object v1, p0, Lkel;->j:Landroid/widget/ImageView;

    iget-object v2, p0, Lkel;->e:Laqvi;

    .line 69
    iget-object v2, v2, Laqvi;->k:Latrr;

    if-eqz v2, :cond_9

    goto :goto_6

    .line 72
    :cond_9
    sget-object v2, Latrr;->c:Latrr;

    .line 70
    :goto_6
    iget-object v3, p0, Lkel;->d:Ladzv;

    .line 71
    invoke-virtual {p1, v0, v1, v2, v3}, Lfyx;->a(Latrj;Landroid/view/View;Ljava/lang/Object;Ladzv;)V

    :cond_a
    :goto_7
    return-void
.end method

.method public final a(Lyho;I)V
    .locals 2

    .line 103
    iget-object v0, p0, Lkel;->j:Landroid/widget/ImageView;

    invoke-virtual {v0}, Landroid/widget/ImageView;->getDrawable()Landroid/graphics/drawable/Drawable;

    move-result-object v1

    invoke-virtual {p1, v1, p2}, Lyho;->a(Landroid/graphics/drawable/Drawable;I)Landroid/graphics/drawable/Drawable;

    move-result-object p1

    invoke-virtual {v0, p1}, Landroid/widget/ImageView;->setImageDrawable(Landroid/graphics/drawable/Drawable;)V

    return-void
.end method

.method public final b(Ljava/lang/String;)V
    .locals 0

    .line 101
    iput-object p1, p0, Lkel;->k:Ljava/lang/String;

    return-void
.end method

.method public final b()Z
    .locals 1

    const/4 v0, 0x1

    return v0
.end method

.method public final b(Landroid/view/MenuItem;)Z
    .locals 0

    const/4 p1, 0x0

    return p1
.end method

.method public final c()I
    .locals 1

    const/4 v0, 0x0

    return v0
.end method

.method public final d()Lflo;
    .locals 0

    return-object p0
.end method

.method public final e()Ljava/lang/CharSequence;
    .locals 1

    .line 79
    iget-object v0, p0, Lkel;->e:Laqvi;

    .line 80
    iget-object v0, v0, Laqvi;->q:Lapon;

    if-eqz v0, :cond_0

    goto :goto_0

    .line 98
    :cond_0
    sget-object v0, Lapon;->c:Lapon;

    .line 81
    :goto_0
    iget-object v0, v0, Lapon;->b:Lapol;

    if-eqz v0, :cond_1

    goto :goto_1

    .line 97
    :cond_1
    sget-object v0, Lapol;->c:Lapol;

    .line 82
    :goto_1
    iget v0, v0, Lapol;->a:I

    and-int/lit8 v0, v0, 0x2

    if-nez v0, :cond_5

    .line 83
    iget-object v0, p0, Lkel;->e:Laqvi;

    .line 84
    iget-object v0, v0, Laqvi;->p:Lapol;

    if-eqz v0, :cond_2

    goto :goto_2

    .line 90
    :cond_2
    sget-object v0, Lapol;->c:Lapol;

    .line 85
    :goto_2
    iget v0, v0, Lapol;->a:I

    and-int/lit8 v0, v0, 0x2

    if-eqz v0, :cond_4

    .line 86
    iget-object v0, p0, Lkel;->e:Laqvi;

    .line 87
    iget-object v0, v0, Laqvi;->p:Lapol;

    if-nez v0, :cond_3

    .line 88
    sget-object v0, Lapol;->c:Lapol;

    .line 89
    :cond_3
    iget-object v0, v0, Lapol;->b:Ljava/lang/String;

    return-object v0

    :cond_4
    const/4 v0, 0x0

    return-object v0

    .line 91
    :cond_5
    iget-object v0, p0, Lkel;->e:Laqvi;

    .line 92
    iget-object v0, v0, Laqvi;->q:Lapon;

    if-eqz v0, :cond_6

    goto :goto_3

    .line 96
    :cond_6
    sget-object v0, Lapon;->c:Lapon;

    .line 93
    :goto_3
    iget-object v0, v0, Lapon;->b:Lapol;

    if-nez v0, :cond_7

    .line 94
    sget-object v0, Lapol;->c:Lapol;

    .line 95
    :cond_7
    iget-object v0, v0, Lapol;->b:Ljava/lang/String;

    return-object v0
.end method

.method public final f()I
    .locals 1

    .line 100
    iget v0, p0, Lkel;->h:I

    add-int/lit16 v0, v0, 0x3e8

    return v0
.end method

.method public final onClick(Landroid/view/View;)V
    .locals 3

    .line 11
    iget-object p1, p0, Lkel;->e:Laqvi;

    .line 12
    iget v0, p1, Laqvi;->a:I

    const/high16 v1, 0x80000

    and-int/2addr v0, v1

    if-nez v0, :cond_0

    goto :goto_0

    .line 34
    :cond_0
    iget-object v0, p0, Lkel;->d:Ladzv;

    new-instance v1, Ladzq;

    .line 35
    iget-object p1, p1, Laqvi;->r:Lapgi;

    .line 36
    invoke-direct {v1, p1}, Ladzq;-><init>(Lapgi;)V

    const/4 p1, 0x3

    const/4 v2, 0x0

    .line 37
    invoke-interface {v0, p1, v1, v2}, Ladzv;->a(ILaebg;Lavyl;)V

    .line 13
    :goto_0
    new-instance p1, Ljava/util/HashMap;

    const/4 v0, 0x2

    invoke-direct {p1, v0}, Ljava/util/HashMap;-><init>(I)V

    .line 14
    iget-object v0, p0, Lkel;->k:Ljava/lang/String;

    const-string v1, "parent_csn"

    invoke-interface {p1, v1, v0}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    .line 15
    iget v0, p0, Lkel;->l:I

    invoke-static {v0}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    move-result-object v0

    const-string v1, "parent_ve_type"

    invoke-interface {p1, v1, v0}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    .line 16
    iget-object v0, p0, Lkel;->e:Laqvi;

    .line 17
    iget v1, v0, Laqvi;->a:I

    and-int/lit16 v1, v1, 0x4000

    if-nez v1, :cond_1

    goto :goto_1

    .line 30
    :cond_1
    iget-object v1, p0, Lkel;->f:Laawi;

    .line 31
    iget-object v0, v0, Laqvi;->n:Laron;

    if-nez v0, :cond_2

    .line 32
    sget-object v0, Laron;->d:Laron;

    .line 33
    :cond_2
    invoke-interface {v1, v0, p1}, Laawi;->a(Laron;Ljava/util/Map;)V

    .line 18
    :goto_1
    iget-object v0, p0, Lkel;->e:Laqvi;

    .line 19
    iget v1, v0, Laqvi;->a:I

    and-int/lit16 v1, v1, 0x1000

    if-nez v1, :cond_3

    goto :goto_2

    .line 26
    :cond_3
    iget-object v1, p0, Lkel;->f:Laawi;

    .line 27
    iget-object v0, v0, Laqvi;->l:Laron;

    if-nez v0, :cond_4

    .line 28
    sget-object v0, Laron;->d:Laron;

    .line 29
    :cond_4
    invoke-interface {v1, v0, p1}, Laawi;->a(Laron;Ljava/util/Map;)V

    .line 20
    :goto_2
    iget-object v0, p0, Lkel;->e:Laqvi;

    .line 21
    iget v1, v0, Laqvi;->a:I

    and-int/lit16 v1, v1, 0x2000

    if-eqz v1, :cond_6

    .line 22
    iget-object v1, p0, Lkel;->f:Laawi;

    .line 23
    iget-object v0, v0, Laqvi;->m:Laron;

    if-nez v0, :cond_5

    .line 24
    sget-object v0, Laron;->d:Laron;

    .line 25
    :cond_5
    invoke-interface {v1, v0, p1}, Laawi;->a(Laron;Ljava/util/Map;)V

    :cond_6
    return-void
.end method
