.class public final Ldyw;
.super Ljava/lang/Object;
.source "PG"


# instance fields
.field private final a:Landroid/content/Context;

.field private final b:Laccp;

.field private final c:Leja;

.field private final d:Ladbp;


# direct methods
.method public constructor <init>(Landroid/content/Context;Laccp;Leja;Ladbp;)V
    .locals 0

    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    iput-object p1, p0, Ldyw;->a:Landroid/content/Context;

    iput-object p2, p0, Ldyw;->b:Laccp;

    iput-object p3, p0, Ldyw;->c:Leja;

    iput-object p4, p0, Ldyw;->d:Ladbp;

    return-void
.end method


# virtual methods
.method public final a(Landroid/content/Intent;)Landroid/content/Intent;
    .locals 9

    iget-object v0, p0, Ldyw;->b:Laccp;

    iget-object v1, p0, Ldyw;->d:Ladbp;

    .line 1
    sget-object v2, Ladbv;->a:Lawyy;

    .line 2
    invoke-virtual {v1}, Ladbp;->b()Lauqj;

    move-result-object v1

    iget-object v1, v1, Lauqj;->h:Laybg;

    if-nez v1, :cond_0

    .line 3
    sget-object v1, Laybg;->C:Laybg;

    :cond_0
    iget-object v1, v1, Laybg;->d:Lbavg;

    if-nez v1, :cond_1

    .line 4
    sget-object v1, Lbavg;->d:Lbavg;

    :cond_1
    iget v2, v1, Lbavg;->a:I

    and-int/lit8 v2, v2, 0x10

    const/4 v3, 0x0

    if-eqz v2, :cond_2

    iget-object v1, v1, Lbavg;->b:Ljava/lang/String;

    goto :goto_0

    :cond_2
    move-object v1, v3

    .line 5
    :goto_0
    new-instance v2, Laccp;

    invoke-direct {v2, v1}, Laccp;-><init>(Ljava/lang/String;)V

    invoke-virtual {v0, v2}, Laccp;->b(Laccp;)I

    move-result v0

    const/4 v1, 0x1

    if-gez v0, :cond_3

    goto :goto_2

    .line 16
    :cond_3
    iget-object v0, p0, Ldyw;->a:Landroid/content/Context;

    .line 6
    invoke-static {v0}, Lacam;->b(Landroid/content/Context;)I

    move-result v0

    iget-object v2, p0, Ldyw;->c:Leja;

    iget-object v2, v2, Leja;->a:Labzp;

    const-string v4, "min_app_version"

    const/4 v5, 0x0

    .line 7
    invoke-virtual {v2, v4, v5}, Labzp;->e(Ljava/lang/String;I)I

    move-result v2

    iget-object v4, p0, Ldyw;->c:Leja;

    iget-object v4, v4, Leja;->a:Labzp;

    const-string v6, "blacklisted_app_versions"

    const-string v7, ""

    .line 8
    invoke-virtual {v4, v6, v7}, Labzp;->d(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;

    move-result-object v4

    # ytProxy diagnostic: log the real values feeding this gate so the
    # actual cause of a still-triggering update screen is evidence, not
    # another guess. Purely additive - does not change behavior.
    new-instance v8, Ljava/lang/StringBuilder;

    invoke-direct {v8}, Ljava/lang/StringBuilder;-><init>()V

    const-string v7, "ytProxy: real versionCode="

    invoke-virtual {v8, v7}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v8, v0}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    const-string v7, " min_app_version="

    invoke-virtual {v8, v7}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v8, v2}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    const-string v7, " blacklisted_app_versions=["

    invoke-virtual {v8, v7}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v8, v4}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    const-string v7, "]"

    invoke-virtual {v8, v7}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v8}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v8

    const-string v7, "ytProxyDebug"

    invoke-static {v7, v8}, Landroid/util/Log;->w(Ljava/lang/String;Ljava/lang/String;)I

    new-instance v6, Landroid/util/SparseBooleanArray;

    .line 9
    invoke-direct {v6}, Landroid/util/SparseBooleanArray;-><init>()V

    const-string v7, ","

    .line 10
    invoke-virtual {v4, v7}, Ljava/lang/String;->split(Ljava/lang/String;)[Ljava/lang/String;

    move-result-object v4

    array-length v7, v4

    :goto_1
    if-ge v5, v7, :cond_4

    aget-object v8, v4, v5

    .line 11
    :try_start_0
    invoke-static {v8}, Ljava/lang/Integer;->parseInt(Ljava/lang/String;)I

    move-result v8

    invoke-virtual {v6, v8, v1}, Landroid/util/SparseBooleanArray;->put(IZ)V
    :try_end_0
    .catch Ljava/lang/NumberFormatException; {:try_start_0 .. :try_end_0} :catch_0

    :catch_0
    add-int/lit8 v5, v5, 0x1

    goto :goto_1

    :cond_4
    if-lt v0, v2, :cond_5

    .line 12
    invoke-virtual {v6, v0}, Landroid/util/SparseBooleanArray;->get(I)Z

    move-result v0

    if-nez v0, :cond_5

    return-object v3

    .line 5
    :cond_5
    :goto_2
    new-instance v0, Landroid/content/Intent;

    iget-object v2, p0, Ldyw;->a:Landroid/content/Context;

    const-class v3, Lcom/google/android/apps/youtube/app/application/upgrade/NewVersionAvailableActivity;

    .line 13
    invoke-direct {v0, v2, v3}, Landroid/content/Intent;-><init>(Landroid/content/Context;Ljava/lang/Class;)V

    const/high16 v2, 0x10000000

    .line 14
    invoke-virtual {v0, v2}, Landroid/content/Intent;->setFlags(I)Landroid/content/Intent;

    const-string v2, "show_force_upgrade"

    .line 15
    invoke-virtual {v0, v2, v1}, Landroid/content/Intent;->putExtra(Ljava/lang/String;Z)Landroid/content/Intent;

    if-eqz p1, :cond_6

    const-string v1, "forward_intent"

    .line 16
    invoke-virtual {v0, v1, p1}, Landroid/content/Intent;->putExtra(Ljava/lang/String;Landroid/os/Parcelable;)Landroid/content/Intent;

    :cond_6
    return-object v0
.end method
