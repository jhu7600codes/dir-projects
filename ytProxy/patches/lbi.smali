.class public final Llbi;
.super Lfmn;
.source "SourceFile"


# instance fields
.field public a:Landroid/widget/TextView;

.field public b:Z

.field private final c:Landroid/content/Context;

.field private final g:Laawi;

.field private final h:Laltp;

.field private i:Landroid/widget/TextView;


# direct methods
.method public constructor <init>(Landroid/view/ViewStub;Landroid/content/Context;Laawi;Laltp;)V
    .locals 0

    .line 1
    invoke-direct {p0, p1}, Lfmn;-><init>(Landroid/view/ViewStub;)V

    .line 2
    iput-object p2, p0, Llbi;->c:Landroid/content/Context;

    .line 3
    invoke-static {p3}, Lanvb;->a(Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object p1

    check-cast p1, Laawi;

    iput-object p1, p0, Llbi;->g:Laawi;

    .line 4
    iput-object p4, p0, Llbi;->h:Laltp;

    return-void
.end method


# virtual methods
.method public final a(IIII)V
    .locals 1

    .line 46
    iget-object v0, p0, Llbi;->a:Landroid/widget/TextView;

    if-eqz v0, :cond_0

    .line 47
    invoke-virtual {v0, p1, p2, p3, p4}, Landroid/widget/TextView;->setPadding(IIII)V

    :cond_0
    return-void
.end method

.method public final a(Laqry;)V
    .locals 1

    const/4 v0, 0x0

    .line 5
    invoke-virtual {p0, p1, v0}, Llbi;->a(Laqry;Ladzv;)V

    return-void
.end method

.method public final a(Laqry;Ladzv;)V
    .locals 4

    if-nez p1, :cond_0

    .line 6
    iget-object p1, p0, Llbi;->d:Landroid/view/ViewStub;

    const/16 p2, 0x8

    invoke-virtual {p1, p2}, Landroid/view/ViewStub;->setVisibility(I)V

    return-void

    :cond_0
    if-nez p2, :cond_1

    goto :goto_0

    .line 43
    :cond_1
    iget-object v0, p1, Laqry;->c:Latho;

    if-nez v0, :cond_2

    .line 44
    sget-object v0, Latho;->f:Latho;

    .line 45
    :cond_2
    invoke-static {v0, p2}, Laebh;->a(Latho;Ladzv;)V

    .line 7
    :goto_0
    invoke-virtual {p0}, Lfmn;->b()Landroid/view/View;

    move-result-object p2

    iput-object p2, p0, Llbi;->f:Landroid/view/View;

    .line 8
    iget-object p2, p0, Llbi;->f:Landroid/view/View;

    const v0, 0x7f0b02f9

    invoke-virtual {p2, v0}, Landroid/view/View;->findViewById(I)Landroid/view/View;

    move-result-object p2

    check-cast p2, Landroid/widget/TextView;

    iput-object p2, p0, Llbi;->i:Landroid/widget/TextView;

    .line 9
    iget-object p2, p0, Llbi;->f:Landroid/view/View;

    const v0, 0x7f0b02fa

    invoke-virtual {p2, v0}, Landroid/view/View;->findViewById(I)Landroid/view/View;

    move-result-object p2

    check-cast p2, Landroid/widget/TextView;

    iput-object p2, p0, Llbi;->a:Landroid/widget/TextView;

    .line 10
    iget-object p2, p0, Llbi;->i:Landroid/widget/TextView;

    const/4 v0, 0x0

    invoke-virtual {p2, v0}, Landroid/widget/TextView;->setIncludeFontPadding(Z)V

    .line 11
    iget-object p2, p0, Llbi;->a:Landroid/widget/TextView;

    invoke-virtual {p2, v0}, Landroid/widget/TextView;->setIncludeFontPadding(Z)V

    .line 12
    iget-object p2, p0, Llbi;->d:Landroid/view/ViewStub;

    invoke-virtual {p2, v0}, Landroid/view/ViewStub;->setVisibility(I)V

    .line 13
    iget-object p2, p0, Llbi;->i:Landroid/widget/TextView;

    .line 14
    iget-object v1, p1, Laqry;->b:Ljava/lang/String;

    .line 15
    invoke-static {p2, v1}, Lyiy;->a(Landroid/widget/TextView;Ljava/lang/CharSequence;)V

    .line 16
    iget-object p2, p0, Llbi;->a:Landroid/widget/TextView;

    .line 17
    iget v1, p1, Laqry;->a:I

    and-int/lit8 v1, v1, 0x2

    const/4 v2, 0x0

    if-eqz v1, :cond_4

    .line 18
    iget-object v1, p1, Laqry;->c:Latho;

    if-eqz v1, :cond_3

    goto :goto_1

    .line 42
    :cond_3
    sget-object v1, Latho;->f:Latho;

    goto :goto_1

    :cond_4
    move-object v1, v2

    .line 19
    :goto_1
    iget-object v3, p0, Llbi;->g:Laawi;

    .line 20
    invoke-static {v1, v3, v0}, Laawn;->a(Latho;Laawi;Z)Landroid/text/Spanned;

    move-result-object v1

    .line 21
    invoke-static {p2, v1}, Lyiy;->a(Landroid/widget/TextView;Ljava/lang/CharSequence;)V

    .line 22
    iget p2, p1, Laqry;->a:I

    and-int/lit8 p2, p2, 0x20

    if-eqz p2, :cond_b

    .line 23
    iget-object p2, p0, Llbi;->c:Landroid/content/Context;

    .line 24
    invoke-virtual {p2}, Landroid/content/Context;->getResources()Landroid/content/res/Resources;

    move-result-object p2

    iget-object v1, p0, Llbi;->h:Laltp;

    .line 25
    iget-object v3, p1, Laqry;->d:Lattl;

    if-eqz v3, :cond_5

    goto :goto_2

    .line 40
    :cond_5
    sget-object v3, Lattl;->c:Lattl;

    .line 26
    :goto_2
    iget v3, v3, Lattl;->b:I

    invoke-static {v3}, Lattn;->a(I)Lattn;

    move-result-object v3

    if-eqz v3, :cond_6

    goto :goto_3

    .line 39
    :cond_6
    sget-object v3, Lattn;->a:Lattn;

    .line 27
    :goto_3
    invoke-interface {v1, v3}, Laltp;->a(Lattn;)I

    move-result v1

    # ytProxy patch: Laltp lookup can legitimately return 0 (no local icon
    # for this Lattn value) - jump straight to the "clear all compound
    # drawables and return" path instead of crashing on getDrawable(0).
    if-eqz v1, :cond_b

    invoke-virtual {p2, v1}, Landroid/content/res/Resources;->getDrawable(I)Landroid/graphics/drawable/Drawable;

    move-result-object p2

    .line 28
    iget-object v1, p1, Laqry;->c:Latho;

    if-eqz v1, :cond_7

    goto :goto_4

    .line 38
    :cond_7
    sget-object v1, Latho;->f:Latho;

    .line 29
    :goto_4
    iget-object v1, v1, Latho;->b:Lapir;

    invoke-interface {v1}, Lapir;->size()I

    move-result v1

    if-gtz v1, :cond_8

    goto :goto_6

    .line 33
    :cond_8
    iget-object p1, p1, Laqry;->c:Latho;

    if-eqz p1, :cond_9

    goto :goto_5

    .line 38
    :cond_9
    sget-object p1, Latho;->f:Latho;

    .line 34
    :goto_5
    iget-object p1, p1, Latho;->b:Lapir;

    invoke-interface {p1, v0}, Lapir;->get(I)Ljava/lang/Object;

    move-result-object p1

    check-cast p1, Laths;

    .line 35
    iget p1, p1, Laths;->a:I

    and-int/lit16 p1, p1, 0x100

    if-eqz p1, :cond_a

    .line 36
    iget-object p1, p0, Llbi;->c:Landroid/content/Context;

    const v1, 0x7f040548

    invoke-static {p1, v1, v0}, Lypu;->a(Landroid/content/Context;II)I

    move-result p1

    .line 37
    invoke-static {p2, p1}, Lpt;->a(Landroid/graphics/drawable/Drawable;I)V

    .line 30
    :cond_a
    :goto_6
    iget-object p1, p0, Llbi;->a:Landroid/widget/TextView;

    invoke-virtual {p1}, Landroid/widget/TextView;->getLineHeight()I

    move-result p1

    .line 31
    invoke-virtual {p2, v0, v0, p1, p1}, Landroid/graphics/drawable/Drawable;->setBounds(IIII)V

    .line 32
    iget-object p1, p0, Llbi;->a:Landroid/widget/TextView;

    invoke-virtual {p1, p2, v2, v2, v2}, Landroid/widget/TextView;->setCompoundDrawables(Landroid/graphics/drawable/Drawable;Landroid/graphics/drawable/Drawable;Landroid/graphics/drawable/Drawable;Landroid/graphics/drawable/Drawable;)V

    return-void

    .line 41
    :cond_b
    iget-object p1, p0, Llbi;->a:Landroid/widget/TextView;

    invoke-virtual {p1, v2, v2, v2, v2}, Landroid/widget/TextView;->setCompoundDrawables(Landroid/graphics/drawable/Drawable;Landroid/graphics/drawable/Drawable;Landroid/graphics/drawable/Drawable;Landroid/graphics/drawable/Drawable;)V

    return-void
.end method

.method public final a()Z
    .locals 4

    .line 48
    iget-object v0, p0, Llbi;->f:Landroid/view/View;

    const/4 v1, 0x1

    const/4 v2, 0x0

    if-eqz v0, :cond_2

    invoke-virtual {v0}, Landroid/view/View;->getVisibility()I

    move-result v0

    if-nez v0, :cond_2

    iget-object v0, p0, Llbi;->a:Landroid/widget/TextView;

    if-eqz v0, :cond_2

    .line 49
    invoke-virtual {v0}, Landroid/widget/TextView;->getVisibility()I

    move-result v0

    if-nez v0, :cond_2

    iget-object v0, p0, Llbi;->i:Landroid/widget/TextView;

    if-eqz v0, :cond_1

    .line 50
    invoke-virtual {v0}, Landroid/widget/TextView;->getVisibility()I

    move-result v0

    const/16 v3, 0x8

    if-eq v0, v3, :cond_0

    goto :goto_0

    :cond_0
    return v1

    :cond_1
    const/4 v2, 0x1

    :cond_2
    :goto_0
    return v2
.end method
