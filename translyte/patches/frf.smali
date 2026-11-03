.class public final Lfrf;
.super Ljava/lang/Object;
.source "SourceFile"

# interfaces
.implements Lfrc;


# instance fields
.field public a:Landroid/widget/TextView;

.field private final b:Landroid/app/Activity;

.field private final c:Laltp;

.field private d:Landroid/view/animation/Animation;

.field private e:Landroid/view/animation/Animation;

.field private f:Lfra;

.field private g:Landroid/widget/FrameLayout;

.field private h:Landroid/widget/FrameLayout;

.field private i:Z


# direct methods
.method public constructor <init>(Landroid/app/Activity;Laltp;)V
    .locals 0

    .line 1
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    .line 2
    iput-object p1, p0, Lfrf;->b:Landroid/app/Activity;

    .line 3
    iput-object p2, p0, Lfrf;->c:Laltp;

    const/4 p1, 0x0

    .line 4
    iput-boolean p1, p0, Lfrf;->i:Z

    return-void
.end method

.method private final d()V
    .locals 2

    .line 11
    iget-object v0, p0, Lfrf;->h:Landroid/widget/FrameLayout;

    iget-object v1, p0, Lfrf;->g:Landroid/widget/FrameLayout;

    invoke-virtual {v0, v1}, Landroid/widget/FrameLayout;->addView(Landroid/view/View;)V

    .line 12
    iget-object v0, p0, Lfrf;->h:Landroid/widget/FrameLayout;

    const v1, 0x7f0b0397

    invoke-virtual {v0, v1}, Landroid/widget/FrameLayout;->findViewById(I)Landroid/view/View;

    move-result-object v0

    check-cast v0, Landroid/widget/FrameLayout;

    iput-object v0, p0, Lfrf;->g:Landroid/widget/FrameLayout;

    const v1, 0x7f0b0398

    .line 13
    invoke-virtual {v0, v1}, Landroid/widget/FrameLayout;->findViewById(I)Landroid/view/View;

    move-result-object v0

    check-cast v0, Landroid/widget/TextView;

    iput-object v0, p0, Lfrf;->a:Landroid/widget/TextView;

    return-void
.end method


# virtual methods
.method public final a()Lanuu;
    .locals 1

    .line 43
    iget-object v0, p0, Lfrf;->f:Lfra;

    invoke-static {v0}, Lanuu;->c(Ljava/lang/Object;)Lanuu;

    move-result-object v0

    return-object v0
.end method

.method public final a(Landroid/widget/FrameLayout;)V
    .locals 2

    .line 5
    iget-object v0, p0, Lfrf;->h:Landroid/widget/FrameLayout;

    if-eqz v0, :cond_0

    iget-object v1, p0, Lfrf;->g:Landroid/widget/FrameLayout;

    if-eqz v1, :cond_0

    .line 6
    invoke-virtual {v0, v1}, Landroid/widget/FrameLayout;->removeView(Landroid/view/View;)V

    :cond_0
    const/4 v0, 0x0

    .line 7
    iput-object v0, p0, Lfrf;->f:Lfra;

    .line 8
    iput-object p1, p0, Lfrf;->h:Landroid/widget/FrameLayout;

    .line 9
    iget-object p1, p0, Lfrf;->g:Landroid/widget/FrameLayout;

    if-eqz p1, :cond_1

    .line 10
    invoke-direct {p0}, Lfrf;->d()V

    :cond_1
    return-void
.end method

.method public final a(Lfra;)V
    .locals 4

    .line 14
    iget-object v0, p0, Lfrf;->h:Landroid/widget/FrameLayout;

    if-eqz v0, :cond_3

    .line 15
    iget-object v0, p0, Lfrf;->g:Landroid/widget/FrameLayout;

    const/4 v1, 0x0

    if-nez v0, :cond_0

    .line 16
    iget-object v0, p0, Lfrf;->b:Landroid/app/Activity;

    .line 17
    invoke-static {v0}, Landroid/view/LayoutInflater;->from(Landroid/content/Context;)Landroid/view/LayoutInflater;

    move-result-object v0

    const v2, 0x7f0e0135

    iget-object v3, p0, Lfrf;->h:Landroid/widget/FrameLayout;

    invoke-virtual {v0, v2, v3, v1}, Landroid/view/LayoutInflater;->inflate(ILandroid/view/ViewGroup;Z)Landroid/view/View;

    move-result-object v0

    check-cast v0, Landroid/widget/FrameLayout;

    iput-object v0, p0, Lfrf;->g:Landroid/widget/FrameLayout;

    .line 18
    invoke-direct {p0}, Lfrf;->d()V

    .line 19
    :cond_0
    iget-boolean v0, p0, Lfrf;->i:Z

    if-nez v0, :cond_1

    .line 20
    iget-object v0, p0, Lfrf;->b:Landroid/app/Activity;

    const v2, 0x7f01004a

    invoke-static {v0, v2}, Landroid/view/animation/AnimationUtils;->loadAnimation(Landroid/content/Context;I)Landroid/view/animation/Animation;

    move-result-object v0

    iput-object v0, p0, Lfrf;->e:Landroid/view/animation/Animation;

    .line 21
    iget-object v0, p0, Lfrf;->b:Landroid/app/Activity;

    const v2, 0x7f01004b

    invoke-static {v0, v2}, Landroid/view/animation/AnimationUtils;->loadAnimation(Landroid/content/Context;I)Landroid/view/animation/Animation;

    move-result-object v0

    iput-object v0, p0, Lfrf;->d:Landroid/view/animation/Animation;

    .line 22
    new-instance v2, Lfre;

    invoke-direct {v2, p0}, Lfre;-><init>(Lfrf;)V

    invoke-virtual {v0, v2}, Landroid/view/animation/Animation;->setAnimationListener(Landroid/view/animation/Animation$AnimationListener;)V

    const/4 v0, 0x1

    .line 23
    iput-boolean v0, p0, Lfrf;->i:Z

    .line 24
    :cond_1
    iget-object v0, p0, Lfrf;->f:Lfra;

    if-eq p1, v0, :cond_2

    .line 25
    iput-object p1, p0, Lfrf;->f:Lfra;

    .line 26
    iget-object v0, p0, Lfrf;->a:Landroid/widget/TextView;

    invoke-virtual {p1}, Lfra;->a()Ljava/lang/CharSequence;

    move-result-object v2

    invoke-virtual {v0, v2}, Landroid/widget/TextView;->setText(Ljava/lang/CharSequence;)V

    .line 27
    iget-object v0, p0, Lfrf;->c:Laltp;

    invoke-virtual {p1}, Lfra;->b()Lattn;

    move-result-object p1

    invoke-interface {v0, p1}, Laltp;->a(Lattn;)I

    move-result p1

    # translyte patch: Laltp lookup can legitimately return 0 (no local icon
    # for this Lattn value) - skip straight to the "no icon override" path
    # instead of crashing Resources.getDrawable(0).
    if-eqz p1, :cond_2

    .line 28
    iget-object v0, p0, Lfrf;->b:Landroid/app/Activity;

    invoke-virtual {v0}, Landroid/app/Activity;->getResources()Landroid/content/res/Resources;

    move-result-object v0

    invoke-virtual {v0, p1}, Landroid/content/res/Resources;->getDrawable(I)Landroid/graphics/drawable/Drawable;

    move-result-object p1

    if-eqz p1, :cond_2

    .line 29
    iget-object v0, p0, Lfrf;->a:Landroid/widget/TextView;

    const/4 v2, 0x0

    invoke-static {v0, p1, v2, v2}, Laam;->a(Landroid/widget/TextView;Landroid/graphics/drawable/Drawable;Landroid/graphics/drawable/Drawable;Landroid/graphics/drawable/Drawable;)V

    .line 31
    :cond_2
    iget-object p1, p0, Lfrf;->g:Landroid/widget/FrameLayout;

    invoke-virtual {p1}, Landroid/widget/FrameLayout;->bringToFront()V

    .line 32
    iget-object p1, p0, Lfrf;->a:Landroid/widget/TextView;

    invoke-virtual {p1, v1}, Landroid/widget/TextView;->setVisibility(I)V

    .line 33
    iget-object p1, p0, Lfrf;->a:Landroid/widget/TextView;

    iget-object v0, p0, Lfrf;->e:Landroid/view/animation/Animation;

    invoke-virtual {p1, v0}, Landroid/widget/TextView;->startAnimation(Landroid/view/animation/Animation;)V

    return-void

    .line 30
    :cond_3
    new-instance p1, Ljava/lang/IllegalStateException;

    const-string v0, "Controller must be initialized for a feed before the content pill can be shown."

    invoke-direct {p1, v0}, Ljava/lang/IllegalStateException;-><init>(Ljava/lang/String;)V

    throw p1
.end method

.method public final a(Z)V
    .locals 2

    .line 34
    iget-object v0, p0, Lfrf;->a:Landroid/widget/TextView;

    if-eqz v0, :cond_1

    iget-object v1, p0, Lfrf;->h:Landroid/widget/FrameLayout;

    if-eqz v1, :cond_1

    if-eqz p1, :cond_0

    .line 35
    iget-object p1, p0, Lfrf;->d:Landroid/view/animation/Animation;

    invoke-virtual {v0, p1}, Landroid/widget/TextView;->startAnimation(Landroid/view/animation/Animation;)V

    return-void

    :cond_0
    const/16 p1, 0x8

    .line 36
    invoke-virtual {v0, p1}, Landroid/widget/TextView;->setVisibility(I)V

    :cond_1
    return-void
.end method

.method public final b()Lanuu;
    .locals 1

    .line 37
    invoke-virtual {p0}, Lfrf;->a()Lanuu;

    move-result-object v0

    invoke-virtual {v0}, Lanuu;->a()Z

    move-result v0

    if-eqz v0, :cond_0

    .line 38
    iget-object v0, p0, Lfrf;->g:Landroid/widget/FrameLayout;

    invoke-static {v0}, Lanuu;->c(Ljava/lang/Object;)Lanuu;

    move-result-object v0

    return-object v0

    .line 39
    :cond_0
    sget-object v0, Lantt;->a:Lantt;

    return-object v0
.end method

.method public final c()Lanuu;
    .locals 1

    .line 40
    invoke-virtual {p0}, Lfrf;->a()Lanuu;

    move-result-object v0

    invoke-virtual {v0}, Lanuu;->a()Z

    move-result v0

    if-eqz v0, :cond_0

    .line 41
    iget-object v0, p0, Lfrf;->a:Landroid/widget/TextView;

    invoke-static {v0}, Lanuu;->c(Ljava/lang/Object;)Lanuu;

    move-result-object v0

    return-object v0

    .line 42
    :cond_0
    sget-object v0, Lantt;->a:Lantt;

    return-object v0
.end method
