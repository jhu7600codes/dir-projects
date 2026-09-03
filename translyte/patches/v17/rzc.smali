.class public final Lrzc;
.super Ljava/lang/Object;
.source "PG"

# interfaces
.implements Lywt;


# instance fields
.field public final synthetic a:Ljava/lang/Object;

.field final synthetic b:Ljava/lang/Object;

.field final synthetic c:Ljava/lang/Object;

.field final synthetic d:Ljava/lang/Object;

.field private final synthetic e:I


# direct methods
.method public constructor <init>(Lacsq;Lrzk;Lcom/google/protos/youtube/api/innertube/UpdateBackstagePollActionOuterClass$UpdateBackstagePollAction;Lammj;I[B[B[B[B[B)V
    .locals 0

    iput p5, p0, Lrzc;->e:I

    iput-object p1, p0, Lrzc;->d:Ljava/lang/Object;

    iput-object p2, p0, Lrzc;->a:Ljava/lang/Object;

    iput-object p3, p0, Lrzc;->b:Ljava/lang/Object;

    iput-object p4, p0, Lrzc;->c:Ljava/lang/Object;

    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    return-void
.end method

.method public constructor <init>(Luyl;Lahto;Ljava/util/Map;Laivi;I)V
    .locals 0

    iput p5, p0, Lrzc;->e:I

    iput-object p1, p0, Lrzc;->d:Ljava/lang/Object;

    iput-object p2, p0, Lrzc;->a:Ljava/lang/Object;

    iput-object p3, p0, Lrzc;->b:Ljava/lang/Object;

    iput-object p4, p0, Lrzc;->c:Ljava/lang/Object;

    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    return-void
.end method

.method public constructor <init>(Lvhm;Ljava/lang/Runnable;Labhf;Lutv;I)V
    .locals 0

    iput p5, p0, Lrzc;->e:I

    iput-object p1, p0, Lrzc;->a:Ljava/lang/Object;

    iput-object p2, p0, Lrzc;->b:Ljava/lang/Object;

    iput-object p3, p0, Lrzc;->c:Ljava/lang/Object;

    iput-object p4, p0, Lrzc;->d:Ljava/lang/Object;

    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    return-void
.end method

.method public constructor <init>(Lytt;Lqwv;Lulj;Lbr;I)V
    .locals 0

    iput p5, p0, Lrzc;->e:I

    iput-object p1, p0, Lrzc;->b:Ljava/lang/Object;

    iput-object p2, p0, Lrzc;->a:Ljava/lang/Object;

    iput-object p3, p0, Lrzc;->c:Ljava/lang/Object;

    iput-object p4, p0, Lrzc;->d:Ljava/lang/Object;

    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    return-void
.end method


# virtual methods
.method public final a(Lcun;)V
    .locals 10

    iget v0, p0, Lrzc;->e:I

    if-eqz v0, :cond_8

    const/4 v1, 0x1

    if-eq v0, v1, :cond_6

    const/4 v1, 0x2

    if-eq v0, v1, :cond_3

    iget-object v0, p0, Lrzc;->a:Ljava/lang/Object;

    check-cast v0, Lvhm;

    .line 14
    iget-object v0, v0, Lvhm;->d:Lvhp;

    invoke-virtual {v0}, Lvhp;->l()Lvim;

    move-result-object v1

    if-eqz v1, :cond_0

    iget-object v0, v0, Lvhp;->n:Ltbf;

    invoke-interface {v0, p1}, Ltbf;->b(Ljava/lang/Throwable;)Ljava/lang/String;

    move-result-object p1

    iget-object v0, p0, Lrzc;->d:Ljava/lang/Object;

    iget-object v5, p0, Lrzc;->c:Ljava/lang/Object;

    iget-object v6, p0, Lrzc;->b:Ljava/lang/Object;

    new-instance v9, Lvhl;

    move-object v4, v0

    check-cast v4, Lutv;

    const/4 v7, 0x0

    const/4 v8, 0x0

    move-object v2, v9

    move-object v3, p0

    invoke-direct/range {v2 .. v8}, Lvhl;-><init>(Lrzc;Lutv;Labhf;Ljava/lang/Runnable;I[B)V

    .line 15
    invoke-interface {v1, p1, v9}, Lvim;->D(Ljava/lang/CharSequence;Ljava/lang/Runnable;)V

    :cond_0
    iget-object p1, p0, Lrzc;->a:Ljava/lang/Object;

    check-cast p1, Lvhm;

    iget-object p1, p1, Lvhm;->d:Lvhp;

    iget-object p1, p1, Lvhp;->w:Lvhr;

    if-eqz p1, :cond_1

    .line 16
    invoke-virtual {p1}, Lvhr;->f()V

    :cond_1
    iget-object p1, p0, Lrzc;->b:Ljava/lang/Object;

    if-eqz p1, :cond_2

    .line 17
    invoke-interface {p1}, Ljava/lang/Runnable;->run()V

    :cond_2
    return-void

    :cond_3
    iget-object v0, p0, Lrzc;->d:Ljava/lang/Object;

    check-cast v0, Luyl;

    iget-object v0, v0, Luyl;->a:Lsrk;

    new-instance v1, Luym;

    iget-object v2, p0, Lrzc;->a:Ljava/lang/Object;

    iget-object v3, p0, Lrzc;->b:Ljava/lang/Object;

    const-string v4, "com.google.android.libraries.youtube.innertube.endpoint.tag"

    .line 1
    invoke-static {v3, v4}, Lued;->ct(Ljava/util/Map;Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v3

    check-cast v2, Lahto;

    invoke-direct {v1, v2, v3}, Luym;-><init>(Lahto;Ljava/lang/Object;)V

    .line 2
    invoke-virtual {v0, v1}, Lsrk;->d(Ljava/lang/Object;)V

    iget-object v0, p0, Lrzc;->c:Ljava/lang/Object;

    if-eqz v0, :cond_5

    check-cast v0, Laivi;

    iget v1, v0, Laivi;->b:I

    and-int/lit8 v1, v1, 0x20

    if-eqz v1, :cond_5

    iget-object p1, p0, Lrzc;->d:Ljava/lang/Object;

    check-cast p1, Luyl;

    iget-object p1, p1, Luyl;->b:Lulj;

    iget-object v0, v0, Laivi;->h:Lahto;

    if-nez v0, :cond_4

    .line 4
    sget-object v0, Lahto;->a:Lahto;

    :cond_4
    iget-object v1, p0, Lrzc;->b:Ljava/lang/Object;

    .line 5
    invoke-interface {p1, v0, v1}, Lulj;->c(Lahto;Ljava/util/Map;)V

    return-void

    :cond_5
    iget-object v0, p0, Lrzc;->d:Ljava/lang/Object;

    check-cast v0, Luyl;

    iget-object v0, v0, Luyl;->e:Ltbf;

    .line 3
    invoke-interface {v0, p1}, Ltbf;->e(Ljava/lang/Throwable;)V

    return-void

    :cond_6
    iget-object p1, p0, Lrzc;->b:Ljava/lang/Object;

    .line 6
    invoke-interface {p1}, Lytt;->t()Z

    move-result p1

    # translyte: t() (kids-onboarding-status-fetched gate) genuinely fails
    # here - real evidence via logcat: GoogleAuthUtil throws
    # "SecurityException: Access denied, missing google package
    # permission or GET_ACCOUNTS" repeatedly right before finishAffinity()
    # fires below, closing the app a split second after launch. Root
    # cause of the SecurityException itself (some microG account-access
    # gate rejecting this specific package/signature) not fully pinned
    # down - not chasing that further here since this bypass fixes the
    # actual crash regardless of why the underlying account query fails,
    # and matches the real outcome for any account that was never
    # subject to a kids-onboarding gate in the first place.
    goto :cond_7

    const-string p1, "Failed to fetch kids onboarding status, finishing the App."

    .line 7
    invoke-static {p1}, Ltex;->b(Ljava/lang/String;)V

    iget-object p1, p0, Lrzc;->d:Ljava/lang/Object;

    check-cast p1, Lbr;

    .line 8
    invoke-virtual {p1}, Lbr;->finishAffinity()V

    :cond_7
    return-void

    :cond_8
    iget-object v0, p0, Lrzc;->d:Ljava/lang/Object;

    check-cast v0, Lacsq;

    iget-object v0, v0, Lacsq;->e:Ljava/lang/Object;

    .line 9
    invoke-interface {v0, p1}, Ltbf;->e(Ljava/lang/Throwable;)V

    iget-object p1, p0, Lrzc;->a:Ljava/lang/Object;

    invoke-interface {p1}, Lrzk;->a()Lahwf;

    move-result-object p1

    iget-object v0, p0, Lrzc;->d:Ljava/lang/Object;

    iget-object p1, p1, Lahwf;->c:Lahvf;

    if-nez p1, :cond_9

    .line 10
    sget-object p1, Lahvf;->a:Lahvf;

    :cond_9
    iget v1, p1, Lahvf;->b:I

    const v2, 0x3b6687b

    if-ne v1, v2, :cond_a

    iget-object p1, p1, Lahvf;->c:Ljava/lang/Object;

    .line 11
    check-cast p1, Lahvd;

    goto :goto_0

    .line 12
    :cond_a
    sget-object p1, Lahvd;->a:Lahvd;

    .line 11
    :goto_0
    iget-object p1, p1, Lahvd;->i:Ljava/lang/String;

    iget-object v1, p0, Lrzc;->c:Ljava/lang/Object;

    iget-object v2, p0, Lrzc;->b:Ljava/lang/Object;

    check-cast v2, Lcom/google/protos/youtube/api/innertube/UpdateBackstagePollActionOuterClass$UpdateBackstagePollAction;

    check-cast v1, Lammj;

    check-cast v0, Lacsq;

    .line 13
    invoke-virtual {v0, p1, v1, v2}, Lacsq;->S(Ljava/lang/String;Lammj;Lcom/google/protos/youtube/api/innertube/UpdateBackstagePollActionOuterClass$UpdateBackstagePollAction;)V

    return-void
.end method

.method public final synthetic mO(Ljava/lang/Object;)V
    .locals 9

    .line 60
    iget v0, p0, Lrzc;->e:I

    const/4 v1, 0x1

    if-eqz v0, :cond_14

    if-eq v0, v1, :cond_9

    const/4 v1, 0x2

    if-eq v0, v1, :cond_2

    check-cast p1, Labhg;

    iget-object v0, p0, Lrzc;->a:Ljava/lang/Object;

    check-cast v0, Lvhm;

    iget-object v0, v0, Lvhm;->d:Lvhp;

    invoke-virtual {v0}, Lvhp;->l()Lvim;

    move-result-object v0

    if-eqz v0, :cond_0

    .line 61
    invoke-interface {v0}, Lvim;->C()V

    :cond_0
    iget-object v0, p0, Lrzc;->b:Ljava/lang/Object;

    if-eqz v0, :cond_1

    .line 62
    invoke-interface {v0}, Ljava/lang/Runnable;->run()V

    :cond_1
    iget-object v0, p0, Lrzc;->a:Ljava/lang/Object;

    .line 63
    invoke-interface {p1}, Labhg;->b()Ljava/lang/Object;

    move-result-object p1

    check-cast p1, Lakuh;

    check-cast v0, Lvhj;

    invoke-virtual {v0, p1}, Lvhj;->l(Lakuh;)V

    return-void

    .line 1
    :cond_2
    check-cast p1, Lajqr;

    iget-object v0, p0, Lrzc;->d:Ljava/lang/Object;

    check-cast v0, Luyl;

    iget-object v0, v0, Luyl;->a:Lsrk;

    new-instance v1, Luym;

    iget-object v2, p0, Lrzc;->a:Ljava/lang/Object;

    iget-object v3, p0, Lrzc;->b:Ljava/lang/Object;

    const-string v4, "com.google.android.libraries.youtube.innertube.endpoint.tag"

    .line 2
    invoke-static {v3, v4}, Lued;->ct(Ljava/util/Map;Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v3

    check-cast v2, Lahto;

    invoke-direct {v1, v2, v3}, Luym;-><init>(Lahto;Ljava/lang/Object;)V

    .line 3
    invoke-virtual {v0, v1}, Lsrk;->d(Ljava/lang/Object;)V

    iget-object v0, p0, Lrzc;->b:Ljava/lang/Object;

    .line 4
    invoke-static {v0, v4}, Lued;->ct(Ljava/util/Map;Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v0

    .line 5
    instance-of v1, v0, Lywt;

    if-eqz v1, :cond_3

    .line 6
    move-object v1, v0

    check-cast v1, Lywt;

    .line 7
    invoke-interface {v1, p1}, Lywt;->mO(Ljava/lang/Object;)V

    :cond_3
    iget-object v1, p0, Lrzc;->c:Ljava/lang/Object;

    if-eqz v1, :cond_7

    check-cast v1, Laivi;

    iget-object v1, v1, Laivi;->f:Laggm;

    .line 8
    invoke-interface {v1}, Laggm;->size()I

    move-result v1

    if-lez v1, :cond_4

    iget-object v1, p0, Lrzc;->c:Ljava/lang/Object;

    check-cast v1, Laivi;

    iget-object v1, v1, Laivi;->f:Laggm;

    .line 9
    invoke-interface {v1}, Ljava/util/List;->iterator()Ljava/util/Iterator;

    move-result-object v1

    :goto_0
    invoke-interface {v1}, Ljava/util/Iterator;->hasNext()Z

    move-result v2

    if-eqz v2, :cond_4

    invoke-interface {v1}, Ljava/util/Iterator;->next()Ljava/lang/Object;

    move-result-object v2

    check-cast v2, Lahto;

    iget-object v3, p0, Lrzc;->d:Ljava/lang/Object;

    check-cast v3, Luyl;

    iget-object v4, v3, Luyl;->b:Lulj;

    iget-object v3, v3, Luyl;->c:Luyj;

    .line 10
    invoke-interface {v3, v2, p1}, Luyj;->a(Lahto;Lajqr;)Lahto;

    move-result-object v2

    iget-object v3, p0, Lrzc;->b:Ljava/lang/Object;

    invoke-interface {v4, v2, v3}, Lulj;->c(Lahto;Ljava/util/Map;)V

    goto :goto_0

    :cond_4
    iget-object v1, p0, Lrzc;->c:Ljava/lang/Object;

    check-cast v1, Laivi;

    iget-object v1, v1, Laivi;->i:Laivj;

    if-nez v1, :cond_5

    .line 11
    sget-object v1, Laivj;->a:Laivj;

    :cond_5
    iget-boolean v1, v1, Laivj;->b:Z

    if-eqz v1, :cond_6

    iget-object v1, p0, Lrzc;->d:Ljava/lang/Object;

    check-cast v1, Luyl;

    iget-object v1, v1, Luyl;->a:Lsrk;

    new-instance v2, Luyp;

    iget-object v3, p0, Lrzc;->a:Ljava/lang/Object;

    check-cast v3, Lahto;

    invoke-direct {v2, v3, v0}, Luyp;-><init>(Lahto;Ljava/lang/Object;)V

    .line 12
    invoke-virtual {v1, v2}, Lsrk;->d(Ljava/lang/Object;)V

    :cond_6
    iget-object v1, p1, Lajqr;->d:Laggm;

    .line 13
    invoke-interface {v1}, Laggm;->size()I

    move-result v1

    if-lez v1, :cond_8

    iget-object v1, p0, Lrzc;->d:Ljava/lang/Object;

    check-cast v1, Luyl;

    iget-object v1, v1, Luyl;->b:Lulj;

    iget-object v2, p1, Lajqr;->d:Laggm;

    iget-object v3, p0, Lrzc;->b:Ljava/lang/Object;

    .line 14
    invoke-interface {v1, v2, v3}, Lulj;->d(Ljava/util/List;Ljava/util/Map;)V

    goto :goto_1

    .line 22
    :cond_7
    iget-object v1, p0, Lrzc;->a:Ljava/lang/Object;

    .line 15
    sget-object v2, Lcom/google/protos/youtube/api/innertube/UndoFeedbackEndpointOuterClass$UndoFeedbackEndpoint;->undoFeedbackEndpoint:Lagfu;

    check-cast v1, Lagfr;

    invoke-virtual {v1, v2}, Lagfr;->re(Lagfe;)Z

    move-result v1

    if-eqz v1, :cond_8

    iget-object v1, p0, Lrzc;->b:Ljava/lang/Object;

    const-string v2, "feedback_undo"

    .line 16
    invoke-static {v1, v2}, Lued;->ct(Ljava/util/Map;Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v1

    iget-object v2, p0, Lrzc;->d:Ljava/lang/Object;

    check-cast v2, Luyl;

    iget-object v2, v2, Luyl;->a:Lsrk;

    new-instance v3, Luyq;

    iget-object v4, p0, Lrzc;->a:Ljava/lang/Object;

    check-cast v4, Lahto;

    invoke-direct {v3, v4, v0, v1}, Luyq;-><init>(Lahto;Ljava/lang/Object;Ljava/lang/Object;)V

    .line 17
    invoke-virtual {v2, v3}, Lsrk;->d(Ljava/lang/Object;)V

    iget-object v1, p0, Lrzc;->a:Ljava/lang/Object;

    sget-object v2, Lcom/google/protos/youtube/api/innertube/UndoFeedbackEndpointOuterClass$UndoFeedbackEndpoint;->undoFeedbackEndpoint:Lagfu;

    check-cast v1, Lagfr;

    .line 18
    invoke-virtual {v1, v2}, Lagfr;->rd(Lagfe;)Ljava/lang/Object;

    move-result-object v1

    check-cast v1, Lcom/google/protos/youtube/api/innertube/UndoFeedbackEndpointOuterClass$UndoFeedbackEndpoint;

    iget-object v1, v1, Lcom/google/protos/youtube/api/innertube/UndoFeedbackEndpointOuterClass$UndoFeedbackEndpoint;->c:Laggm;

    .line 19
    invoke-interface {v1}, Ljava/util/List;->isEmpty()Z

    move-result v2

    if-nez v2, :cond_8

    iget-object v2, p0, Lrzc;->d:Ljava/lang/Object;

    check-cast v2, Luyl;

    iget-object v2, v2, Luyl;->b:Lulj;

    iget-object v3, p0, Lrzc;->b:Ljava/lang/Object;

    .line 20
    invoke-interface {v2, v1, v3}, Lulj;->d(Ljava/util/List;Ljava/util/Map;)V

    .line 14
    :cond_8
    :goto_1
    iget-object v1, p0, Lrzc;->d:Ljava/lang/Object;

    check-cast v1, Luyl;

    iget-object v1, v1, Luyl;->d:Luyk;

    .line 21
    invoke-interface {v1, p1, v0}, Luyk;->a(Lajqr;Ljava/lang/Object;)V

    iget-object v0, p0, Lrzc;->d:Ljava/lang/Object;

    iget-object p1, p1, Lajqr;->e:Lageq;

    check-cast v0, Luyl;

    .line 22
    invoke-virtual {v0, p1}, Luyl;->b(Lageq;)V

    return-void

    .line 23
    :cond_9
    check-cast p1, Lsns;

    iget-object v0, p1, Lsns;->b:Ljava/lang/Object;

    .line 24
    invoke-interface {v0}, Ljava/util/List;->iterator()Ljava/util/Iterator;

    move-result-object v0

    :cond_a
    invoke-interface {v0}, Ljava/util/Iterator;->hasNext()Z

    move-result v1

    if-eqz v1, :cond_10

    invoke-interface {v0}, Ljava/util/Iterator;->next()Ljava/lang/Object;

    move-result-object v1

    check-cast v1, Lacxl;

    .line 25
    invoke-virtual {v1}, Lacxl;->i()Ljava/util/List;

    move-result-object v2

    invoke-interface {v2}, Ljava/util/List;->iterator()Ljava/util/Iterator;

    move-result-object v2

    :cond_b
    invoke-interface {v2}, Ljava/util/Iterator;->hasNext()Z

    move-result v3

    if-eqz v3, :cond_d

    invoke-interface {v2}, Ljava/util/Iterator;->next()Ljava/lang/Object;

    move-result-object v3

    check-cast v3, Luux;

    iget-object v4, v3, Luux;->d:Lcom/google/protos/youtube/api/innertube/StartModularOnboardingCommandOuterClass$StartModularOnboardingCommand;

    if-eqz v4, :cond_b

    iget-object p1, p0, Lrzc;->b:Ljava/lang/Object;

    .line 29
    invoke-interface {p1}, Lytt;->t()Z

    move-result p1

    if-eqz p1, :cond_c

    iget-object p1, p0, Lrzc;->a:Ljava/lang/Object;

    .line 30
    invoke-interface {p1}, Lqwv;->n()V

    :cond_c
    iget-object p1, p0, Lrzc;->c:Ljava/lang/Object;

    .line 31
    invoke-virtual {v3}, Luux;->c()Lahto;

    move-result-object v0

    invoke-interface {p1, v0}, Lulj;->a(Lahto;)V

    return-void

    :cond_d
    iget-object v2, p0, Lrzc;->b:Ljava/lang/Object;

    .line 26
    invoke-interface {v2}, Lytt;->t()Z

    move-result v2

    if-nez v2, :cond_a

    .line 27
    invoke-virtual {v1}, Lacxl;->i()Ljava/util/List;

    move-result-object v1

    invoke-interface {v1}, Ljava/util/List;->iterator()Ljava/util/Iterator;

    move-result-object v1

    :cond_e
    invoke-interface {v1}, Ljava/util/Iterator;->hasNext()Z

    move-result v2

    if-eqz v2, :cond_a

    invoke-interface {v1}, Ljava/util/Iterator;->next()Ljava/lang/Object;

    move-result-object v2

    check-cast v2, Luux;

    .line 28
    invoke-virtual {v2}, Luux;->o()Z

    move-result v3

    if-nez v3, :cond_f

    invoke-virtual {v2}, Luux;->l()Z

    move-result v3

    if-eqz v3, :cond_e

    :cond_f
    iget-object v3, v2, Luux;->b:Lanha;

    if-eqz v3, :cond_e

    iget-object p1, p0, Lrzc;->c:Ljava/lang/Object;

    .line 32
    invoke-virtual {v2}, Luux;->c()Lahto;

    move-result-object v0

    invoke-interface {p1, v0}, Lulj;->a(Lahto;)V

    return-void

    :cond_10
    iget-object v0, p0, Lrzc;->b:Ljava/lang/Object;

    .line 33
    invoke-interface {v0}, Lytt;->t()Z

    move-result v0

    # translyte: same t() bypass as the other call site in this class -
    # see the comment there.
    goto :cond_13

    iget-object v0, p1, Lsns;->a:Ljava/lang/Object;

    check-cast v0, Luva;

    .line 34
    invoke-virtual {v0}, Luva;->c()Ljava/util/List;

    move-result-object v0

    invoke-interface {v0}, Ljava/util/List;->isEmpty()Z

    move-result v0

    const-string v1, "Failed to fetch kids onboarding status, finishing the App."

    if-nez v0, :cond_12

    iget-object p1, p1, Lsns;->a:Ljava/lang/Object;

    check-cast p1, Luva;

    .line 35
    invoke-virtual {p1}, Luva;->c()Ljava/util/List;

    move-result-object p1

    invoke-interface {p1}, Ljava/util/List;->iterator()Ljava/util/Iterator;

    move-result-object p1

    :cond_11
    invoke-interface {p1}, Ljava/util/Iterator;->hasNext()Z

    move-result v0

    if-eqz v0, :cond_13

    invoke-interface {p1}, Ljava/util/Iterator;->next()Ljava/lang/Object;

    move-result-object v0

    check-cast v0, Luuz;

    iget-boolean v0, v0, Luuz;->a:Z

    if-eqz v0, :cond_11

    .line 36
    invoke-static {v1}, Ltex;->b(Ljava/lang/String;)V

    iget-object p1, p0, Lrzc;->d:Ljava/lang/Object;

    check-cast p1, Lbr;

    .line 37
    invoke-virtual {p1}, Lbr;->finishAffinity()V

    return-void

    .line 38
    :cond_12
    invoke-static {v1}, Ltex;->b(Ljava/lang/String;)V

    iget-object p1, p0, Lrzc;->d:Ljava/lang/Object;

    check-cast p1, Lbr;

    .line 39
    invoke-virtual {p1}, Lbr;->finishAffinity()V

    :cond_13
    return-void

    .line 40
    :cond_14
    check-cast p1, Lajmk;

    iget-object v0, p0, Lrzc;->a:Ljava/lang/Object;

    invoke-interface {v0}, Lrzk;->a()Lahwf;

    move-result-object v0

    iget-object p1, p1, Lajmk;->c:Laggm;

    .line 41
    invoke-interface {p1}, Ljava/util/List;->iterator()Ljava/util/Iterator;

    move-result-object p1

    :cond_15
    :goto_2
    invoke-interface {p1}, Ljava/util/Iterator;->hasNext()Z

    move-result v2

    const v3, 0x3b6687b

    if-eqz v2, :cond_1e

    invoke-interface {p1}, Ljava/util/Iterator;->next()Ljava/lang/Object;

    move-result-object v2

    check-cast v2, Lajma;

    iget-object v4, v2, Lajma;->g:Lajmb;

    if-nez v4, :cond_16

    .line 42
    sget-object v4, Lajmb;->a:Lajmb;

    :cond_16
    iget v4, v4, Lajmb;->b:I

    const v5, 0x5ec9696

    if-ne v4, v5, :cond_15

    iget-object v4, p0, Lrzc;->d:Ljava/lang/Object;

    check-cast v4, Lacsq;

    iget-object v4, v4, Lacsq;->a:Ljava/lang/Object;

    iget-object v6, v0, Lahwf;->c:Lahvf;

    if-nez v6, :cond_17

    .line 43
    sget-object v6, Lahvf;->a:Lahvf;

    :cond_17
    iget v7, v6, Lahvf;->b:I

    if-ne v7, v3, :cond_18

    iget-object v6, v6, Lahvf;->c:Ljava/lang/Object;

    .line 44
    check-cast v6, Lahvd;

    goto :goto_3

    .line 45
    :cond_18
    sget-object v6, Lahvd;->a:Lahvd;

    .line 44
    :goto_3
    iget-object v6, v6, Lahvd;->i:Ljava/lang/String;

    iget-object v7, v2, Lajma;->g:Lajmb;

    if-nez v7, :cond_19

    sget-object v7, Lajmb;->a:Lajmb;

    :cond_19
    iget v8, v7, Lajmb;->b:I

    if-ne v8, v5, :cond_1a

    iget-object v5, v7, Lajmb;->c:Ljava/lang/Object;

    .line 46
    check-cast v5, Lammj;

    goto :goto_4

    .line 47
    :cond_1a
    sget-object v5, Lammj;->a:Lammj;

    .line 46
    :goto_4
    check-cast v4, Logw;

    .line 48
    invoke-virtual {v4, v6, v5}, Logw;->l(Ljava/lang/String;Lammj;)V

    iget-object v4, p0, Lrzc;->d:Ljava/lang/Object;

    check-cast v4, Lacsq;

    iget-object v4, v4, Lacsq;->a:Ljava/lang/Object;

    iget-object v5, v0, Lahwf;->c:Lahvf;

    if-nez v5, :cond_1b

    sget-object v5, Lahvf;->a:Lahvf;

    :cond_1b
    iget v6, v5, Lahvf;->b:I

    if-ne v6, v3, :cond_1c

    iget-object v3, v5, Lahvf;->c:Ljava/lang/Object;

    .line 49
    check-cast v3, Lahvd;

    goto :goto_5

    .line 51
    :cond_1c
    sget-object v3, Lahvd;->a:Lahvd;

    .line 49
    :goto_5
    iget-object v3, v3, Lahvd;->i:Ljava/lang/String;

    iget-wide v5, v2, Lajma;->j:J

    iget v2, v2, Lajma;->i:I

    .line 50
    invoke-static {v2}, Lahut;->b(I)Lahut;

    move-result-object v2

    if-nez v2, :cond_1d

    sget-object v2, Lahut;->a:Lahut;

    :cond_1d
    check-cast v4, Logw;

    .line 51
    invoke-virtual {v4, v3, v5, v6, v2}, Logw;->m(Ljava/lang/String;JLahut;)V

    goto :goto_2

    .line 45
    :cond_1e
    iget-object p1, p0, Lrzc;->b:Ljava/lang/Object;

    check-cast p1, Lcom/google/protos/youtube/api/innertube/UpdateBackstagePollActionOuterClass$UpdateBackstagePollAction;

    iget p1, p1, Lcom/google/protos/youtube/api/innertube/UpdateBackstagePollActionOuterClass$UpdateBackstagePollAction;->c:I

    .line 52
    invoke-static {p1}, Lahut;->b(I)Lahut;

    move-result-object p1

    if-nez p1, :cond_1f

    sget-object p1, Lahut;->a:Lahut;

    :cond_1f
    sget-object v2, Lahut;->d:Lahut;

    if-ne p1, v2, :cond_26

    iget-object p1, v0, Lahwf;->f:Lahvx;

    if-nez p1, :cond_20

    .line 53
    sget-object p1, Lahvx;->a:Lahvx;

    :cond_20
    iget-object v0, p0, Lrzc;->d:Ljava/lang/Object;

    check-cast v0, Lacsq;

    iget-object v0, v0, Lacsq;->c:Ljava/lang/Object;

    const/4 v2, 0x0

    if-eqz p1, :cond_25

    iget v4, p1, Lahvx;->b:I

    and-int/2addr v1, v4

    if-eqz v1, :cond_25

    iget-object p1, p1, Lahvx;->c:Lahvv;

    if-nez p1, :cond_21

    .line 54
    sget-object p1, Lahvv;->a:Lahvv;

    :cond_21
    check-cast v0, Lsns;

    .line 55
    invoke-virtual {v0, p1}, Lsns;->c(Lahvv;)Ljava/util/List;

    move-result-object p1

    invoke-interface {p1}, Ljava/util/List;->iterator()Ljava/util/Iterator;

    move-result-object p1

    :cond_22
    invoke-interface {p1}, Ljava/util/Iterator;->hasNext()Z

    move-result v0

    if-eqz v0, :cond_25

    invoke-interface {p1}, Ljava/util/Iterator;->next()Ljava/lang/Object;

    move-result-object v0

    check-cast v0, Lahvf;

    iget v1, v0, Lahvf;->b:I

    if-ne v1, v3, :cond_23

    iget-object v1, v0, Lahvf;->c:Ljava/lang/Object;

    .line 56
    check-cast v1, Lahvd;

    goto :goto_6

    .line 57
    :cond_23
    sget-object v1, Lahvd;->a:Lahvd;

    .line 56
    :goto_6
    iget-boolean v1, v1, Lahvd;->o:Z

    if-eqz v1, :cond_22

    iget p1, v0, Lahvf;->b:I

    if-ne p1, v3, :cond_24

    iget-object p1, v0, Lahvf;->c:Ljava/lang/Object;

    .line 58
    move-object v2, p1

    check-cast v2, Lahvd;

    goto :goto_7

    .line 59
    :cond_24
    sget-object v2, Lahvd;->a:Lahvd;

    :cond_25
    :goto_7
    if-eqz v2, :cond_26

    .line 58
    iget-object p1, p0, Lrzc;->a:Ljava/lang/Object;

    .line 59
    invoke-interface {p1, v2}, Lrzk;->c(Lahvd;)V

    :cond_26
    return-void
.end method

.method public final synthetic mP()V
    .locals 0

    return-void
.end method
