.class public final Llyb;
.super Lflc;
.source "PG"

# interfaces
.implements Landroid/view/View$OnClickListener;
.implements Lfkv;
.implements Lflk;


# instance fields
.field public a:Landroid/view/View$OnLongClickListener;

.field private final b:Lapii;

.field private final c:Landroid/view/LayoutInflater;

.field private final d:Landroid/content/res/Resources;

.field private final e:Lagmk;

.field private final f:Laufs;

.field private final g:Laddm;

.field private final h:Lapbe;

.field private final i:Lapjd;

.field private final j:I

.field private final k:Ljava/util/List;

.field private l:Landroid/widget/ImageView;

.field private m:Ljava/lang/String;

.field private n:I

.field private o:Landroid/view/View;

.field private p:Lmgq;

.field private final q:Lmau;

.field private final r:Ladbp;


# direct methods
.method public constructor <init>(Laddm;Lapbe;Lapii;Landroid/content/Context;Lmat;Lapjd;Ladbp;Lagmk;Laufs;Ljava/util/List;)V
    .locals 0

    invoke-direct {p0}, Lflc;-><init>()V

    iput-object p3, p0, Llyb;->b:Lapii;

    .line 1
    invoke-static {p4}, Landroid/view/LayoutInflater;->from(Landroid/content/Context;)Landroid/view/LayoutInflater;

    move-result-object p3

    iput-object p3, p0, Llyb;->c:Landroid/view/LayoutInflater;

    .line 2
    invoke-virtual {p4}, Landroid/content/Context;->getResources()Landroid/content/res/Resources;

    move-result-object p3

    iput-object p3, p0, Llyb;->d:Landroid/content/res/Resources;

    iput-object p1, p0, Llyb;->g:Laddm;

    iput-object p2, p0, Llyb;->h:Lapbe;

    iput-object p6, p0, Llyb;->i:Lapjd;

    iput-object p8, p0, Llyb;->e:Lagmk;

    iput-object p9, p0, Llyb;->f:Laufs;

    .line 3
    invoke-virtual {p5}, Lmat;->b()Lmau;

    move-result-object p1

    iput-object p1, p0, Llyb;->q:Lmau;

    iput-object p10, p0, Llyb;->k:Ljava/util/List;

    iput-object p7, p0, Llyb;->r:Ladbp;

    const p1, 0x7f0406dd

    .line 4
    invoke-static {p4, p1}, Laccz;->d(Landroid/content/Context;I)I

    move-result p1

    iput p1, p0, Llyb;->j:I

    return-void
.end method


# virtual methods
.method public final a(Labto;I)V
    .locals 2

    iget-object v0, p0, Llyb;->r:Ladbp;

    .line 1
    invoke-static {v0}, Lgcz;->aw(Ladbp;)Z

    move-result v0

    if-eqz v0, :cond_0

    iget-object v0, p0, Llyb;->l:Landroid/widget/ImageView;

    .line 2
    invoke-virtual {v0}, Landroid/widget/ImageView;->getContext()Landroid/content/Context;

    move-result-object v0

    const v1, 0x7f0406a7

    invoke-static {v0, v1}, Laccz;->d(Landroid/content/Context;I)I

    move-result v0

    if-ne p2, v0, :cond_0

    iget-object p2, p0, Llyb;->l:Landroid/widget/ImageView;

    .line 4
    invoke-virtual {p2}, Landroid/widget/ImageView;->getDrawable()Landroid/graphics/drawable/Drawable;

    move-result-object v0

    iget v1, p0, Llyb;->j:I

    invoke-virtual {p1, v0, v1}, Labto;->d(Landroid/graphics/drawable/Drawable;I)Landroid/graphics/drawable/Drawable;

    move-result-object p1

    invoke-virtual {p2, p1}, Landroid/widget/ImageView;->setImageDrawable(Landroid/graphics/drawable/Drawable;)V

    return-void

    :cond_0
    iget-object v0, p0, Llyb;->l:Landroid/widget/ImageView;

    .line 3
    invoke-virtual {v0}, Landroid/widget/ImageView;->getDrawable()Landroid/graphics/drawable/Drawable;

    move-result-object v1

    invoke-virtual {p1, v1, p2}, Labto;->d(Landroid/graphics/drawable/Drawable;I)Landroid/graphics/drawable/Drawable;

    move-result-object p1

    invoke-virtual {v0, p1}, Landroid/widget/ImageView;->setImageDrawable(Landroid/graphics/drawable/Drawable;)V

    return-void
.end method

.method public final b()I
    .locals 1

    iget-object v0, p0, Llyb;->q:Lmau;

    .line 1
    invoke-virtual {v0}, Lmau;->a()I

    move-result v0

    return v0
.end method

.method public final c(Landroid/view/MenuItem;)V
    .locals 4

    iget-object v0, p0, Llyb;->o:Landroid/view/View;

    if-nez v0, :cond_0

    iget-object v0, p0, Llyb;->c:Landroid/view/LayoutInflater;

    const v1, 0x7f0e033d

    const/4 v2, 0x0

    const/4 v3, 0x0

    .line 1
    invoke-virtual {v0, v1, v2, v3}, Landroid/view/LayoutInflater;->inflate(ILandroid/view/ViewGroup;Z)Landroid/view/View;

    move-result-object v0

    iput-object v0, p0, Llyb;->o:Landroid/view/View;

    const v1, 0x7f0b08c4

    .line 2
    invoke-virtual {v0, v1}, Landroid/view/View;->findViewById(I)Landroid/view/View;

    move-result-object v0

    check-cast v0, Landroid/widget/ImageView;

    iput-object v0, p0, Llyb;->l:Landroid/widget/ImageView;

    iget-object v0, p0, Llyb;->o:Landroid/view/View;

    const v1, 0x7f0b0991

    .line 3
    invoke-virtual {v0, v1}, Landroid/view/View;->findViewById(I)Landroid/view/View;

    move-result-object v0

    check-cast v0, Landroid/view/ViewStub;

    const-class v1, Landroid/widget/TextView;

    invoke-static {v0, v1}, Labuw;->a(Landroid/view/ViewStub;Ljava/lang/Class;)Labuw;

    move-result-object v0

    iget-object v1, p0, Llyb;->o:Landroid/view/View;

    const v2, 0x7f0b0992

    .line 4
    invoke-virtual {v1, v2}, Landroid/view/View;->findViewById(I)Landroid/view/View;

    move-result-object v1

    check-cast v1, Landroid/view/ViewStub;

    const-class v2, Landroid/view/View;

    invoke-static {v1, v2}, Labuw;->a(Landroid/view/ViewStub;Ljava/lang/Class;)Labuw;

    move-result-object v1

    new-instance v2, Lmgq;

    .line 5
    invoke-direct {v2, v1, v0}, Lmgq;-><init>(Labuw;Labuw;)V

    iput-object v2, p0, Llyb;->p:Lmgq;

    :cond_0
    const/4 v0, 0x2

    .line 6
    invoke-interface {p1, v0}, Landroid/view/MenuItem;->setShowAsAction(I)V

    iget-object v0, p0, Llyb;->h:Lapbe;

    iget-object v1, p0, Llyb;->f:Laufs;

    iget-object v1, v1, Laufs;->e:Lavyz;

    if-nez v1, :cond_1

    .line 7
    sget-object v1, Lavyz;->c:Lavyz;

    :cond_1
    iget v1, v1, Lavyz;->b:I

    .line 8
    invoke-static {v1}, Lavyy;->a(I)Lavyy;

    move-result-object v1

    if-nez v1, :cond_2

    sget-object v1, Lavyy;->a:Lavyy;

    .line 9
    :cond_2
    invoke-interface {v0, v1}, Lapbe;->a(Lavyy;)I

    move-result v0

    iget-object v1, p0, Llyb;->l:Landroid/widget/ImageView;

    iget-object v2, p0, Llyb;->d:Landroid/content/res/Resources;

    # ytProxy patch: same Lftt/Lapbe unmapped-icon (0) crash as mjl.smali -
    # ImageView.setImageDrawable(null) is a documented safe no-op (just
    # clears the icon), so skip straight to it instead of crashing on
    # getDrawable(0). Generic fix; per-icon correctness is a later pass.
    .line 10
    if-eqz v0, :cond_ytproxy_null_menu_icon

    invoke-virtual {v2, v0}, Landroid/content/res/Resources;->getDrawable(I)Landroid/graphics/drawable/Drawable;

    move-result-object v0

    goto :cond_ytproxy_menu_icon_done

    :cond_ytproxy_null_menu_icon
    const/4 v0, 0x0

    :cond_ytproxy_menu_icon_done
    invoke-virtual {v1, v0}, Landroid/widget/ImageView;->setImageDrawable(Landroid/graphics/drawable/Drawable;)V

    iget-object v0, p0, Llyb;->l:Landroid/widget/ImageView;

    .line 11
    invoke-virtual {p0}, Llyb;->h()Ljava/lang/CharSequence;

    move-result-object v1

    invoke-virtual {v0, v1}, Landroid/widget/ImageView;->setContentDescription(Ljava/lang/CharSequence;)V

    iget-object v0, p0, Llyb;->l:Landroid/widget/ImageView;

    .line 12
    invoke-virtual {v0, p0}, Landroid/widget/ImageView;->setOnClickListener(Landroid/view/View$OnClickListener;)V

    iget-object v0, p0, Llyb;->a:Landroid/view/View$OnLongClickListener;

    if-eqz v0, :cond_3

    iget-object v1, p0, Llyb;->l:Landroid/widget/ImageView;

    .line 13
    invoke-virtual {v1, v0}, Landroid/widget/ImageView;->setOnLongClickListener(Landroid/view/View$OnLongClickListener;)V

    :cond_3
    iget-object v0, p0, Llyb;->o:Landroid/view/View;

    .line 14
    invoke-interface {p1, v0}, Landroid/view/MenuItem;->setActionView(Landroid/view/View;)Landroid/view/MenuItem;

    iget-object p1, p0, Llyb;->f:Laufs;

    iget v0, p1, Laufs;->a:I

    and-int/lit16 v0, v0, 0x800

    if-eqz v0, :cond_8

    iget-object p1, p1, Laufs;->k:Lavxq;

    if-nez p1, :cond_4

    .line 15
    sget-object p1, Lavxq;->c:Lavxq;

    :cond_4
    iget p1, p1, Lavxq;->a:I

    const v0, 0x61f53fb

    if-ne p1, v0, :cond_8

    iget-object p1, p0, Llyb;->b:Lapii;

    iget-object v1, p0, Llyb;->f:Laufs;

    iget-object v1, v1, Laufs;->k:Lavxq;

    if-nez v1, :cond_5

    sget-object v1, Lavxq;->c:Lavxq;

    :cond_5
    iget v2, v1, Lavxq;->a:I

    if-ne v2, v0, :cond_6

    iget-object v0, v1, Lavxq;->b:Ljava/lang/Object;

    .line 16
    check-cast v0, Lavxm;

    goto :goto_0

    .line 17
    :cond_6
    sget-object v0, Lavxm;->j:Lavxm;

    .line 16
    :goto_0
    iget-object v1, p0, Llyb;->l:Landroid/widget/ImageView;

    iget-object v2, p0, Llyb;->f:Laufs;

    iget-object v2, v2, Laufs;->k:Lavxq;

    if-nez v2, :cond_7

    sget-object v2, Lavxq;->c:Lavxq;

    :cond_7
    iget-object v3, p0, Llyb;->e:Lagmk;

    .line 18
    invoke-virtual {p1, v0, v1, v2, v3}, Lapii;->a(Lavxm;Landroid/view/View;Ljava/lang/Object;Lagmk;)V

    :cond_8
    iget-object p1, p0, Llyb;->i:Lapjd;

    iget-object v0, p0, Llyb;->f:Laufs;

    iget v1, v0, Laufs;->a:I

    and-int/lit16 v1, v1, 0x400

    if-eqz v1, :cond_9

    iget-object v0, v0, Laufs;->j:Ljava/lang/String;

    iget-object v1, p0, Llyb;->l:Landroid/widget/ImageView;

    .line 19
    invoke-virtual {p1, v0, v1}, Lapjd;->e(Ljava/lang/String;Landroid/view/View;)V

    :cond_9
    return-void
.end method

.method public final d()Z
    .locals 1

    const/4 v0, 0x1

    return v0
.end method

.method public final e()I
    .locals 1

    const/4 v0, 0x0

    return v0
.end method

.method public final f()Lfkv;
    .locals 0

    return-object p0
.end method

.method public final g()Z
    .locals 1

    const/4 v0, 0x0

    return v0
.end method

.method public final h()Ljava/lang/CharSequence;
    .locals 1

    iget-object v0, p0, Llyb;->f:Laufs;

    iget-object v0, v0, Laufs;->q:Latjy;

    if-nez v0, :cond_0

    .line 1
    sget-object v0, Latjy;->c:Latjy;

    :cond_0
    iget-object v0, v0, Latjy;->b:Latjx;

    if-nez v0, :cond_1

    .line 2
    sget-object v0, Latjx;->d:Latjx;

    :cond_1
    iget v0, v0, Latjx;->a:I

    and-int/lit8 v0, v0, 0x2

    if-eqz v0, :cond_4

    iget-object v0, p0, Llyb;->f:Laufs;

    iget-object v0, v0, Laufs;->q:Latjy;

    if-nez v0, :cond_2

    sget-object v0, Latjy;->c:Latjy;

    :cond_2
    iget-object v0, v0, Latjy;->b:Latjx;

    if-nez v0, :cond_3

    sget-object v0, Latjx;->d:Latjx;

    :cond_3
    iget-object v0, v0, Latjx;->b:Ljava/lang/String;

    return-object v0

    :cond_4
    iget-object v0, p0, Llyb;->f:Laufs;

    iget-object v0, v0, Laufs;->p:Latjx;

    if-nez v0, :cond_5

    sget-object v0, Latjx;->d:Latjx;

    :cond_5
    iget v0, v0, Latjx;->a:I

    and-int/lit8 v0, v0, 0x2

    if-eqz v0, :cond_7

    iget-object v0, p0, Llyb;->f:Laufs;

    iget-object v0, v0, Laufs;->p:Latjx;

    if-nez v0, :cond_6

    sget-object v0, Latjx;->d:Latjx;

    :cond_6
    iget-object v0, v0, Latjx;->b:Ljava/lang/String;

    return-object v0

    :cond_7
    const/4 v0, 0x0

    return-object v0
.end method

.method public final i()I
    .locals 1

    iget-object v0, p0, Llyb;->q:Lmau;

    iget v0, v0, Lmau;->a:I

    add-int/lit16 v0, v0, 0x3e8

    return v0
.end method

.method public final j()Z
    .locals 1

    const/4 v0, 0x1

    return v0
.end method

.method public final k(Lbdqc;)V
    .locals 2

    iget-object v0, p0, Llyb;->p:Lmgq;

    iget-object v1, v0, Lmgq;->c:Lbdrk;

    if-eqz v1, :cond_0

    check-cast v1, Ljava/util/concurrent/atomic/AtomicReference;

    .line 1
    invoke-static {v1}, Lbemt;->i(Ljava/util/concurrent/atomic/AtomicReference;)V

    const/4 v1, 0x0

    iput-object v1, v0, Lmgq;->c:Lbdrk;

    :cond_0
    new-instance v1, Lmgp;

    .line 2
    invoke-direct {v1, v0}, Lmgp;-><init>(Lmgq;)V

    invoke-virtual {p1, v1}, Lbdqc;->N(Lbdsh;)Lbdrk;

    move-result-object p1

    iput-object p1, v0, Lmgq;->c:Lbdrk;

    return-void
.end method

.method public final l()Ljava/util/List;
    .locals 1

    iget-object v0, p0, Llyb;->k:Ljava/util/List;

    return-object v0
.end method

.method public final m(Ljava/lang/String;)V
    .locals 0

    iput-object p1, p0, Llyb;->m:Ljava/lang/String;

    return-void
.end method

.method public final n(I)V
    .locals 0

    iput p1, p0, Llyb;->n:I

    return-void
.end method

.method public final onClick(Landroid/view/View;)V
    .locals 3

    iget-object p1, p0, Llyb;->f:Laufs;

    iget v0, p1, Laufs;->a:I

    const/high16 v1, 0x80000

    and-int/2addr v0, v1

    if-eqz v0, :cond_0

    iget-object v0, p0, Llyb;->e:Lagmk;

    new-instance v1, Lagmc;

    iget-object p1, p1, Laufs;->r:Latcs;

    .line 1
    invoke-direct {v1, p1}, Lagmc;-><init>(Latcs;)V

    const/4 p1, 0x3

    const/4 v2, 0x0

    .line 2
    invoke-interface {v0, p1, v1, v2}, Lagmk;->C(ILagnt;Laxdo;)V

    :cond_0
    new-instance p1, Ljava/util/HashMap;

    const/4 v0, 0x2

    .line 3
    invoke-direct {p1, v0}, Ljava/util/HashMap;-><init>(I)V

    iget-object v0, p0, Llyb;->m:Ljava/lang/String;

    const-string v1, "parent_csn"

    .line 4
    invoke-interface {p1, v1, v0}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    iget v0, p0, Llyb;->n:I

    .line 5
    invoke-static {v0}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    move-result-object v0

    const-string v1, "parent_ve_type"

    invoke-interface {p1, v1, v0}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    iget-object v0, p0, Llyb;->f:Laufs;

    iget v1, v0, Laufs;->a:I

    and-int/lit16 v1, v1, 0x4000

    if-eqz v1, :cond_2

    iget-object v1, p0, Llyb;->g:Laddm;

    iget-object v0, v0, Laufs;->n:Lauqq;

    if-nez v0, :cond_1

    .line 6
    sget-object v0, Lauqq;->e:Lauqq;

    .line 7
    :cond_1
    invoke-interface {v1, v0, p1}, Laddm;->a(Lauqq;Ljava/util/Map;)V

    :cond_2
    iget-object v0, p0, Llyb;->f:Laufs;

    iget v1, v0, Laufs;->a:I

    and-int/lit16 v1, v1, 0x1000

    if-eqz v1, :cond_4

    iget-object v1, p0, Llyb;->g:Laddm;

    iget-object v0, v0, Laufs;->l:Lauqq;

    if-nez v0, :cond_3

    .line 8
    sget-object v0, Lauqq;->e:Lauqq;

    .line 9
    :cond_3
    invoke-interface {v1, v0, p1}, Laddm;->a(Lauqq;Ljava/util/Map;)V

    :cond_4
    iget-object v0, p0, Llyb;->f:Laufs;

    iget v1, v0, Laufs;->a:I

    and-int/lit16 v1, v1, 0x2000

    if-eqz v1, :cond_6

    iget-object v1, p0, Llyb;->g:Laddm;

    iget-object v0, v0, Laufs;->m:Lauqq;

    if-nez v0, :cond_5

    .line 10
    sget-object v0, Lauqq;->e:Lauqq;

    .line 11
    :cond_5
    invoke-interface {v1, v0, p1}, Laddm;->a(Lauqq;Ljava/util/Map;)V

    :cond_6
    return-void
.end method
