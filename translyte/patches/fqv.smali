.class public final Lfqv;
.super Ljava/lang/Object;
.source "SourceFile"

# interfaces
.implements Lfrc;


# instance fields
.field public a:Landroid/widget/LinearLayout;

.field private final b:Landroid/app/Activity;

.field private c:Landroid/widget/FrameLayout;

.field private d:Landroid/widget/TextView;

.field private e:Landroid/widget/ImageView;

.field private f:Z

.field private g:Landroid/view/animation/AlphaAnimation;

.field private h:Landroid/view/animation/TranslateAnimation;

.field private i:Landroid/view/animation/TranslateAnimation;

.field private j:Landroid/view/animation/AnimationSet;

.field private k:Landroid/view/animation/AnimationSet;

.field private l:Landroid/animation/ValueAnimator;

.field private m:Lfra;

.field private final n:Laltp;

.field private o:Landroid/widget/FrameLayout;

.field private p:Landroid/content/res/Resources$Theme;


# direct methods
.method public constructor <init>(Landroid/app/Activity;Laltp;)V
    .locals 0

    .line 1
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    .line 2
    iput-object p1, p0, Lfqv;->b:Landroid/app/Activity;

    const/4 p1, 0x0

    .line 3
    iput-boolean p1, p0, Lfqv;->f:Z

    .line 4
    iput-object p2, p0, Lfqv;->n:Laltp;

    return-void
.end method

.method private final d()V
    .locals 2

    .line 76
    iget-object v0, p0, Lfqv;->o:Landroid/widget/FrameLayout;

    iget-object v1, p0, Lfqv;->c:Landroid/widget/FrameLayout;

    invoke-virtual {v0, v1}, Landroid/widget/FrameLayout;->addView(Landroid/view/View;)V

    .line 77
    iget-object v0, p0, Lfqv;->o:Landroid/widget/FrameLayout;

    const v1, 0x7f0b0177

    invoke-virtual {v0, v1}, Landroid/widget/FrameLayout;->findViewById(I)Landroid/view/View;

    move-result-object v0

    check-cast v0, Landroid/widget/FrameLayout;

    iput-object v0, p0, Lfqv;->c:Landroid/widget/FrameLayout;

    const v1, 0x7f0b0175

    .line 78
    invoke-virtual {v0, v1}, Landroid/widget/FrameLayout;->findViewById(I)Landroid/view/View;

    move-result-object v0

    check-cast v0, Landroid/widget/LinearLayout;

    iput-object v0, p0, Lfqv;->a:Landroid/widget/LinearLayout;

    .line 79
    iget-object v0, p0, Lfqv;->c:Landroid/widget/FrameLayout;

    const v1, 0x7f0b0176

    invoke-virtual {v0, v1}, Landroid/widget/FrameLayout;->findViewById(I)Landroid/view/View;

    move-result-object v0

    check-cast v0, Landroid/widget/TextView;

    iput-object v0, p0, Lfqv;->d:Landroid/widget/TextView;

    .line 80
    iget-object v0, p0, Lfqv;->c:Landroid/widget/FrameLayout;

    const v1, 0x7f0b0174

    invoke-virtual {v0, v1}, Landroid/widget/FrameLayout;->findViewById(I)Landroid/view/View;

    move-result-object v0

    check-cast v0, Landroid/widget/ImageView;

    iput-object v0, p0, Lfqv;->e:Landroid/widget/ImageView;

    return-void
.end method


# virtual methods
.method public final a()Lanuu;
    .locals 1

    .line 69
    iget-object v0, p0, Lfqv;->m:Lfra;

    invoke-static {v0}, Lanuu;->c(Ljava/lang/Object;)Lanuu;

    move-result-object v0

    return-object v0
.end method

.method public final a(Landroid/widget/FrameLayout;)V
    .locals 2

    .line 5
    iget-object v0, p0, Lfqv;->o:Landroid/widget/FrameLayout;

    if-eqz v0, :cond_0

    iget-object v1, p0, Lfqv;->c:Landroid/widget/FrameLayout;

    if-eqz v1, :cond_0

    .line 6
    invoke-virtual {v0, v1}, Landroid/widget/FrameLayout;->removeView(Landroid/view/View;)V

    :cond_0
    const/4 v0, 0x0

    .line 7
    iput-object v0, p0, Lfqv;->m:Lfra;

    .line 8
    iput-object p1, p0, Lfqv;->o:Landroid/widget/FrameLayout;

    .line 9
    iget-object p1, p0, Lfqv;->c:Landroid/widget/FrameLayout;

    if-eqz p1, :cond_1

    .line 10
    invoke-direct {p0}, Lfqv;->d()V

    :cond_1
    return-void
.end method

.method public final a(Lfra;)V
    .locals 5

    .line 11
    iget-object v0, p0, Lfqv;->o:Landroid/widget/FrameLayout;

    if-eqz v0, :cond_6

    .line 12
    iget-object v0, p0, Lfqv;->c:Landroid/widget/FrameLayout;

    const/4 v1, 0x0

    if-nez v0, :cond_0

    .line 13
    iget-object v0, p0, Lfqv;->b:Landroid/app/Activity;

    .line 14
    invoke-static {v0}, Landroid/view/LayoutInflater;->from(Landroid/content/Context;)Landroid/view/LayoutInflater;

    move-result-object v0

    const v2, 0x7f0e0075

    iget-object v3, p0, Lfqv;->o:Landroid/widget/FrameLayout;

    .line 15
    invoke-virtual {v0, v2, v3, v1}, Landroid/view/LayoutInflater;->inflate(ILandroid/view/ViewGroup;Z)Landroid/view/View;

    move-result-object v0

    check-cast v0, Landroid/widget/FrameLayout;

    iput-object v0, p0, Lfqv;->c:Landroid/widget/FrameLayout;

    .line 16
    invoke-direct {p0}, Lfqv;->d()V

    .line 17
    iget-object v0, p0, Lfqv;->a:Landroid/widget/LinearLayout;

    invoke-virtual {v0}, Landroid/widget/LinearLayout;->getBackground()Landroid/graphics/drawable/Drawable;

    move-result-object v2

    invoke-static {v0, v2}, Lyiy;->a(Landroid/view/View;Landroid/graphics/drawable/Drawable;)V

    .line 18
    :cond_0
    iget-object v0, p0, Lfqv;->o:Landroid/widget/FrameLayout;

    invoke-virtual {v0}, Landroid/widget/FrameLayout;->getContext()Landroid/content/Context;

    move-result-object v0

    invoke-virtual {v0}, Landroid/content/Context;->getTheme()Landroid/content/res/Resources$Theme;

    move-result-object v0

    .line 19
    iput-object v0, p0, Lfqv;->p:Landroid/content/res/Resources$Theme;

    .line 21
    iget-boolean v2, p0, Lfqv;->f:Z

    if-nez v2, :cond_2

    .line 22
    iget-object v2, p0, Lfqv;->b:Landroid/app/Activity;

    const v3, 0x7f01001c

    .line 23
    invoke-static {v2, v3}, Landroid/view/animation/AnimationUtils;->loadAnimation(Landroid/content/Context;I)Landroid/view/animation/Animation;

    move-result-object v2

    check-cast v2, Landroid/view/animation/AlphaAnimation;

    iput-object v2, p0, Lfqv;->g:Landroid/view/animation/AlphaAnimation;

    .line 24
    iget-object v2, p0, Lfqv;->b:Landroid/app/Activity;

    const v3, 0x7f010018

    .line 25
    invoke-static {v2, v3}, Landroid/view/animation/AnimationUtils;->loadAnimation(Landroid/content/Context;I)Landroid/view/animation/Animation;

    move-result-object v2

    check-cast v2, Landroid/view/animation/TranslateAnimation;

    iput-object v2, p0, Lfqv;->h:Landroid/view/animation/TranslateAnimation;

    .line 26
    iget-object v2, p0, Lfqv;->b:Landroid/app/Activity;

    const v3, 0x7f010019

    .line 27
    invoke-static {v2, v3}, Landroid/view/animation/AnimationUtils;->loadAnimation(Landroid/content/Context;I)Landroid/view/animation/Animation;

    move-result-object v2

    check-cast v2, Landroid/view/animation/TranslateAnimation;

    iput-object v2, p0, Lfqv;->i:Landroid/view/animation/TranslateAnimation;

    .line 28
    iget-object v2, p0, Lfqv;->b:Landroid/app/Activity;

    const v3, 0x7f01001a

    .line 29
    invoke-static {v2, v3}, Landroid/view/animation/AnimationUtils;->loadAnimation(Landroid/content/Context;I)Landroid/view/animation/Animation;

    move-result-object v2

    check-cast v2, Landroid/view/animation/AnimationSet;

    iput-object v2, p0, Lfqv;->j:Landroid/view/animation/AnimationSet;

    .line 30
    iget-object v2, p0, Lfqv;->b:Landroid/app/Activity;

    const v3, 0x7f01001b

    .line 31
    invoke-static {v2, v3}, Landroid/view/animation/AnimationUtils;->loadAnimation(Landroid/content/Context;I)Landroid/view/animation/Animation;

    move-result-object v2

    check-cast v2, Landroid/view/animation/AnimationSet;

    iput-object v2, p0, Lfqv;->k:Landroid/view/animation/AnimationSet;

    const/4 v2, 0x2

    new-array v2, v2, [I

    .line 32
    fill-array-data v2, :array_0

    invoke-static {v2}, Landroid/animation/ValueAnimator;->ofInt([I)Landroid/animation/ValueAnimator;

    move-result-object v2

    iput-object v2, p0, Lfqv;->l:Landroid/animation/ValueAnimator;

    .line 33
    iget-object v3, p0, Lfqv;->b:Landroid/app/Activity;

    .line 34
    invoke-virtual {v3}, Landroid/app/Activity;->getResources()Landroid/content/res/Resources;

    move-result-object v3

    const v4, 0x7f0c0008

    invoke-virtual {v3, v4}, Landroid/content/res/Resources;->getInteger(I)I

    move-result v3

    int-to-long v3, v3

    .line 35
    invoke-virtual {v2, v3, v4}, Landroid/animation/ValueAnimator;->setDuration(J)Landroid/animation/ValueAnimator;

    .line 36
    iget-object v2, p0, Lfqv;->l:Landroid/animation/ValueAnimator;

    iget-object v3, p0, Lfqv;->b:Landroid/app/Activity;

    .line 37
    invoke-virtual {v3}, Landroid/app/Activity;->getResources()Landroid/content/res/Resources;

    move-result-object v3

    const v4, 0x7f0c0009

    invoke-virtual {v3, v4}, Landroid/content/res/Resources;->getInteger(I)I

    move-result v3

    int-to-long v3, v3

    .line 38
    invoke-virtual {v2, v3, v4}, Landroid/animation/ValueAnimator;->setStartDelay(J)V

    .line 39
    sget v2, Landroid/os/Build$VERSION;->SDK_INT:I

    const/16 v3, 0x16

    if-lt v2, v3, :cond_1

    .line 40
    new-instance v2, Lawv;

    invoke-direct {v2}, Lawv;-><init>()V

    .line 41
    iget-object v3, p0, Lfqv;->g:Landroid/view/animation/AlphaAnimation;

    invoke-virtual {v3, v2}, Landroid/view/animation/AlphaAnimation;->setInterpolator(Landroid/view/animation/Interpolator;)V

    .line 42
    iget-object v3, p0, Lfqv;->h:Landroid/view/animation/TranslateAnimation;

    invoke-virtual {v3, v2}, Landroid/view/animation/TranslateAnimation;->setInterpolator(Landroid/view/animation/Interpolator;)V

    .line 43
    iget-object v3, p0, Lfqv;->i:Landroid/view/animation/TranslateAnimation;

    invoke-virtual {v3, v2}, Landroid/view/animation/TranslateAnimation;->setInterpolator(Landroid/view/animation/Interpolator;)V

    .line 44
    iget-object v3, p0, Lfqv;->j:Landroid/view/animation/AnimationSet;

    invoke-virtual {v3, v2}, Landroid/view/animation/AnimationSet;->setInterpolator(Landroid/view/animation/Interpolator;)V

    .line 45
    iget-object v3, p0, Lfqv;->k:Landroid/view/animation/AnimationSet;

    invoke-virtual {v3, v2}, Landroid/view/animation/AnimationSet;->setInterpolator(Landroid/view/animation/Interpolator;)V

    .line 46
    iget-object v3, p0, Lfqv;->l:Landroid/animation/ValueAnimator;

    invoke-virtual {v3, v2}, Landroid/animation/ValueAnimator;->setInterpolator(Landroid/animation/TimeInterpolator;)V

    .line 47
    :cond_1
    iget-object v2, p0, Lfqv;->i:Landroid/view/animation/TranslateAnimation;

    new-instance v3, Lfqx;

    invoke-direct {v3, p0}, Lfqx;-><init>(Lfqv;)V

    invoke-virtual {v2, v3}, Landroid/view/animation/TranslateAnimation;->setAnimationListener(Landroid/view/animation/Animation$AnimationListener;)V

    .line 48
    :cond_2
    iget-boolean v2, p0, Lfqv;->f:Z

    if-nez v2, :cond_3

    goto :goto_0

    :cond_3
    if-ne v0, v0, :cond_4

    goto :goto_1

    .line 65
    :cond_4
    iget-object v0, p0, Lfqv;->l:Landroid/animation/ValueAnimator;

    invoke-virtual {v0}, Landroid/animation/ValueAnimator;->removeAllUpdateListeners()V

    .line 49
    :goto_0
    iget-object v0, p0, Lfqv;->o:Landroid/widget/FrameLayout;

    .line 50
    invoke-virtual {v0}, Landroid/widget/FrameLayout;->getContext()Landroid/content/Context;

    move-result-object v0

    const v2, 0x7f040553

    invoke-static {v0, v2, v1}, Lypu;->a(Landroid/content/Context;II)I

    move-result v0

    .line 51
    iget-object v2, p0, Lfqv;->l:Landroid/animation/ValueAnimator;

    new-instance v3, Lfqu;

    invoke-direct {v3, p0, v0}, Lfqu;-><init>(Lfqv;I)V

    invoke-virtual {v2, v3}, Landroid/animation/ValueAnimator;->addUpdateListener(Landroid/animation/ValueAnimator$AnimatorUpdateListener;)V

    :goto_1
    const/4 v0, 0x1

    .line 52
    iput-boolean v0, p0, Lfqv;->f:Z

    .line 53
    iget-object v0, p0, Lfqv;->m:Lfra;

    if-eq p1, v0, :cond_5

    .line 54
    iput-object p1, p0, Lfqv;->m:Lfra;

    .line 55
    iget-object v0, p0, Lfqv;->d:Landroid/widget/TextView;

    invoke-virtual {p1}, Lfra;->a()Ljava/lang/CharSequence;

    move-result-object v2

    invoke-virtual {v0, v2}, Landroid/widget/TextView;->setText(Ljava/lang/CharSequence;)V

    .line 56
    iget-object v0, p0, Lfqv;->n:Laltp;

    invoke-virtual {p1}, Lfra;->b()Lattn;

    move-result-object p1

    invoke-interface {v0, p1}, Laltp;->a(Lattn;)I

    move-result p1

    # translyte patch: Laltp lookup can legitimately return 0 (no local icon
    # for this Lattn value) - skip straight to the "no icon override" path
    # instead of crashing Resources.getDrawable(0).
    if-eqz p1, :cond_5

    .line 57
    iget-object v0, p0, Lfqv;->b:Landroid/app/Activity;

    invoke-virtual {v0}, Landroid/app/Activity;->getResources()Landroid/content/res/Resources;

    move-result-object v0

    invoke-virtual {v0, p1}, Landroid/content/res/Resources;->getDrawable(I)Landroid/graphics/drawable/Drawable;

    move-result-object p1

    .line 58
    iget-object v0, p0, Lfqv;->e:Landroid/widget/ImageView;

    invoke-virtual {v0, p1}, Landroid/widget/ImageView;->setImageDrawable(Landroid/graphics/drawable/Drawable;)V

    .line 59
    :cond_5
    iget-object p1, p0, Lfqv;->c:Landroid/widget/FrameLayout;

    invoke-virtual {p1}, Landroid/widget/FrameLayout;->bringToFront()V

    .line 60
    iget-object p1, p0, Lfqv;->a:Landroid/widget/LinearLayout;

    invoke-virtual {p1, v1}, Landroid/widget/LinearLayout;->setVisibility(I)V

    .line 61
    iget-object p1, p0, Lfqv;->a:Landroid/widget/LinearLayout;

    iget-object v0, p0, Lfqv;->h:Landroid/view/animation/TranslateAnimation;

    invoke-virtual {p1, v0}, Landroid/widget/LinearLayout;->startAnimation(Landroid/view/animation/Animation;)V

    .line 62
    iget-object p1, p0, Lfqv;->d:Landroid/widget/TextView;

    iget-object v0, p0, Lfqv;->g:Landroid/view/animation/AlphaAnimation;

    invoke-virtual {p1, v0}, Landroid/widget/TextView;->startAnimation(Landroid/view/animation/Animation;)V

    .line 63
    iget-object p1, p0, Lfqv;->e:Landroid/widget/ImageView;

    iget-object v0, p0, Lfqv;->j:Landroid/view/animation/AnimationSet;

    invoke-virtual {p1, v0}, Landroid/widget/ImageView;->startAnimation(Landroid/view/animation/Animation;)V

    return-void

    .line 64
    :cond_6
    new-instance p1, Ljava/lang/IllegalStateException;

    const-string v0, "Controller must be initialized for a feed before the content pill can be shown."

    invoke-direct {p1, v0}, Ljava/lang/IllegalStateException;-><init>(Ljava/lang/String;)V

    throw p1

    nop

    :array_0
    .array-data 4
        0x33
        0x0
    .end array-data
.end method

.method public final a(Z)V
    .locals 1

    .line 66
    iget-object v0, p0, Lfqv;->o:Landroid/widget/FrameLayout;

    if-eqz v0, :cond_1

    iget-object v0, p0, Lfqv;->c:Landroid/widget/FrameLayout;

    if-eqz v0, :cond_1

    if-eqz p1, :cond_0

    .line 67
    iget-object p1, p0, Lfqv;->a:Landroid/widget/LinearLayout;

    iget-object v0, p0, Lfqv;->i:Landroid/view/animation/TranslateAnimation;

    invoke-virtual {p1, v0}, Landroid/widget/LinearLayout;->startAnimation(Landroid/view/animation/Animation;)V

    return-void

    .line 68
    :cond_0
    iget-object p1, p0, Lfqv;->a:Landroid/widget/LinearLayout;

    const/16 v0, 0x8

    invoke-virtual {p1, v0}, Landroid/widget/LinearLayout;->setVisibility(I)V

    :cond_1
    return-void
.end method

.method public final b()Lanuu;
    .locals 1

    .line 70
    invoke-virtual {p0}, Lfqv;->a()Lanuu;

    move-result-object v0

    invoke-virtual {v0}, Lanuu;->a()Z

    move-result v0

    if-eqz v0, :cond_0

    .line 71
    iget-object v0, p0, Lfqv;->c:Landroid/widget/FrameLayout;

    invoke-static {v0}, Lanuu;->c(Ljava/lang/Object;)Lanuu;

    move-result-object v0

    return-object v0

    .line 72
    :cond_0
    sget-object v0, Lantt;->a:Lantt;

    return-object v0
.end method

.method public final c()Lanuu;
    .locals 1

    .line 73
    invoke-virtual {p0}, Lfqv;->a()Lanuu;

    move-result-object v0

    invoke-virtual {v0}, Lanuu;->a()Z

    move-result v0

    if-eqz v0, :cond_0

    .line 74
    iget-object v0, p0, Lfqv;->a:Landroid/widget/LinearLayout;

    invoke-static {v0}, Lanuu;->c(Ljava/lang/Object;)Lanuu;

    move-result-object v0

    return-object v0

    .line 75
    :cond_0
    sget-object v0, Lantt;->a:Lantt;

    return-object v0
.end method
