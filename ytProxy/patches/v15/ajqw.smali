.class public final Lajqw;
.super Ljava/lang/Object;
.source "PG"

# interfaces
.implements Lbctq;


# instance fields
.field private final a:Lbeou;

.field private final b:Lbeou;


# direct methods
.method public constructor <init>(Lbeou;Lbeou;)V
    .locals 0

    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    iput-object p1, p0, Lajqw;->a:Lbeou;

    iput-object p2, p0, Lajqw;->b:Lbeou;

    return-void
.end method

.method public static a(Lbeou;Lbeou;Lbeou;)Lajqw;
    .locals 0

    new-instance p1, Lajqw;

    .line 1
    invoke-direct {p1, p0, p2}, Lajqw;-><init>(Lbeou;Lbeou;)V

    return-object p1
.end method

.method public static b(Lajsa;Landroid/content/Context;)Lajrs;
    .locals 4

    .line 1
    invoke-static {p1}, Larmo;->t(Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Lacam;->c:Lacam;

    iget-object v0, v0, Lacam;->a:Ljava/lang/Boolean;

    if-nez v0, :cond_0

    sget-object v0, Lacam;->c:Lacam;

    .line 2
    invoke-virtual {p1}, Landroid/content/Context;->getPackageManager()Landroid/content/pm/PackageManager;

    move-result-object v1

    const-string v2, "android.hardware.type.television"

    invoke-virtual {v1, v2}, Landroid/content/pm/PackageManager;->hasSystemFeature(Ljava/lang/String;)Z

    move-result v1

    invoke-static {v1}, Ljava/lang/Boolean;->valueOf(Z)Ljava/lang/Boolean;

    move-result-object v1

    iput-object v1, v0, Lacam;->a:Ljava/lang/Boolean;

    :cond_0
    sget-object v0, Lacam;->c:Lacam;

    iget-object v0, v0, Lacam;->a:Ljava/lang/Boolean;

    .line 3
    invoke-virtual {v0}, Ljava/lang/Boolean;->booleanValue()Z

    move-result v0

    if-eqz v0, :cond_1

    .line 4
    sget-object v0, Lajrq;->c:Lajrq;

    goto :goto_0

    .line 5
    :cond_1
    invoke-static {p1}, Lakqp;->i(Landroid/content/Context;)Z

    move-result v0

    if-eqz v0, :cond_2

    sget-object v0, Lajrq;->d:Lajrq;

    goto :goto_0

    :cond_2
    sget-object v0, Lajrq;->b:Lajrq;

    .line 4
    :goto_0
    iget-object v1, p0, Lajsa;->c:Larmr;

    if-eqz v1, :cond_3

    check-cast v1, Lozv;

    .line 6
    invoke-virtual {v1}, Lozv;->a()Ljava/lang/String;

    move-result-object v1

    goto :goto_1

    .line 7
    :cond_3
    invoke-virtual {p1}, Landroid/content/Context;->getPackageName()Ljava/lang/String;

    move-result-object v1

    .line 6
    :goto_1
    iget-object v2, p0, Lajsa;->d:Larmr;

    if-eqz v2, :cond_4

    check-cast v2, Lozu;

    .line 8
    invoke-virtual {v2}, Lozu;->a()Ljava/lang/String;

    move-result-object p1

    goto :goto_2

    .line 9
    :cond_4
    invoke-static {p1}, Lacam;->a(Landroid/content/Context;)Ljava/lang/String;

    move-result-object p1

    .line 8
    :goto_2
    iget-object p0, p0, Lajsa;->b:Lajrr;

    new-instance v2, Lajrs;

    const/4 v3, 0x1

    .line 10
    invoke-static {v1, v3}, Lasof;->l(Ljava/lang/Object;I)V

    const/4 v3, 0x2

    invoke-static {p1, v3}, Lasof;->l(Ljava/lang/Object;I)V

    const/4 v3, 0x3

    invoke-static {v0, v3}, Lasof;->l(Ljava/lang/Object;I)V

    const/4 v3, 0x4

    invoke-static {p0, v3}, Lasof;->l(Ljava/lang/Object;I)V

    # ytProxy patch: override only the version string fed into the network
    # client-info map (Lajrs's "cver"/"cbrver" fields), leaving Lacam;->a
    # (this app's own general-purpose version accessor, used by ~16 other
    # call sites incl. the About screen and update-nag UI) completely
    # untouched, so the app's own self-identity still reads real 15.46.34
    # everywhere except what's actually sent over the wire.
    const-string p1, "21.33.324"

    invoke-direct {v2, v1, p1, v0, p0}, Lajrs;-><init>(Ljava/lang/String;Ljava/lang/String;Lajrq;Lajrr;)V

    return-object v2
.end method


# virtual methods
.method public final bridge synthetic get()Ljava/lang/Object;
    .locals 2

    iget-object v0, p0, Lajqw;->a:Lbeou;

    check-cast v0, Lbctr;

    iget-object v0, v0, Lbctr;->a:Ljava/lang/Object;

    check-cast v0, Lajsa;

    iget-object v1, p0, Lajqw;->b:Lbeou;

    invoke-interface {v1}, Lbeou;->get()Ljava/lang/Object;

    move-result-object v1

    check-cast v1, Landroid/content/Context;

    invoke-static {v0, v1}, Lajqw;->b(Lajsa;Landroid/content/Context;)Lajrs;

    move-result-object v0

    return-object v0
.end method
