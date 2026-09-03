.class public final Lynv;
.super Ljava/lang/Object;
.source "SourceFile"


# static fields
.field private static final e:Lynv;


# instance fields
.field private volatile a:Ljava/lang/Boolean;

.field private volatile b:Ljava/lang/String;

.field private volatile c:Ljava/lang/Integer;

.field private volatile d:Lypr;


# direct methods
.method static constructor <clinit>()V
    .locals 1

    .line 44
    new-instance v0, Lynv;

    invoke-direct {v0}, Lynv;-><init>()V

    sput-object v0, Lynv;->e:Lynv;

    return-void
.end method

.method private constructor <init>()V
    .locals 0

    .line 1
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    return-void
.end method

.method public static a(Landroid/content/Context;I)Landroid/content/pm/PackageInfo;
    .locals 1

    .line 38
    invoke-virtual {p0}, Landroid/content/Context;->getPackageManager()Landroid/content/pm/PackageManager;

    move-result-object v0

    invoke-virtual {p0}, Landroid/content/Context;->getPackageName()Ljava/lang/String;

    move-result-object p0

    invoke-virtual {v0, p0, p1}, Landroid/content/pm/PackageManager;->getPackageInfo(Ljava/lang/String;I)Landroid/content/pm/PackageInfo;

    move-result-object p0

    return-object p0
.end method

.method public static a(Landroid/content/Context;Landroid/content/pm/PackageManager;)Lypr;
    .locals 3

    .line 39
    :try_start_0
    sget-object v0, Lynv;->e:Lynv;

    iget-object v0, v0, Lynv;->d:Lypr;

    if-nez v0, :cond_0

    .line 40
    sget-object v0, Lynv;->e:Lynv;

    new-instance v1, Lypr;

    .line 41
    invoke-virtual {p0}, Landroid/content/Context;->getPackageName()Ljava/lang/String;

    move-result-object p0

    const/4 v2, 0x0

    invoke-virtual {p1, p0, v2}, Landroid/content/pm/PackageManager;->getPackageInfo(Ljava/lang/String;I)Landroid/content/pm/PackageInfo;

    move-result-object p0

    iget-object p0, p0, Landroid/content/pm/PackageInfo;->versionName:Ljava/lang/String;

    invoke-direct {v1, p0}, Lypr;-><init>(Ljava/lang/String;)V

    iput-object v1, v0, Lynv;->d:Lypr;
    :try_end_0
    .catch Landroid/content/pm/PackageManager$NameNotFoundException; {:try_start_0 .. :try_end_0} :catch_0

    .line 43
    :cond_0
    sget-object p0, Lynv;->e:Lynv;

    iget-object p0, p0, Lynv;->d:Lypr;

    return-object p0

    :catch_0
    move-exception p0

    .line 42
    new-instance p1, Ljava/lang/RuntimeException;

    const-string v0, "PackageManager did not find our package name!"

    invoke-direct {p1, v0, p0}, Ljava/lang/RuntimeException;-><init>(Ljava/lang/String;Ljava/lang/Throwable;)V

    throw p1
.end method

.method public static a(Landroid/content/Context;)Z
    .locals 2

    .line 2
    invoke-static {p0}, Lanvb;->a(Ljava/lang/Object;)Ljava/lang/Object;

    .line 3
    sget-object v0, Lynv;->e:Lynv;

    iget-object v0, v0, Lynv;->a:Ljava/lang/Boolean;

    if-nez v0, :cond_0

    .line 4
    sget-object v0, Lynv;->e:Lynv;

    .line 5
    invoke-virtual {p0}, Landroid/content/Context;->getPackageManager()Landroid/content/pm/PackageManager;

    move-result-object p0

    const-string v1, "android.hardware.type.television"

    invoke-virtual {p0, v1}, Landroid/content/pm/PackageManager;->hasSystemFeature(Ljava/lang/String;)Z

    move-result p0

    invoke-static {p0}, Ljava/lang/Boolean;->valueOf(Z)Ljava/lang/Boolean;

    move-result-object p0

    iput-object p0, v0, Lynv;->a:Ljava/lang/Boolean;

    .line 6
    :cond_0
    sget-object p0, Lynv;->e:Lynv;

    iget-object p0, p0, Lynv;->a:Ljava/lang/Boolean;

    invoke-virtual {p0}, Ljava/lang/Boolean;->booleanValue()Z

    move-result p0

    return p0
.end method

.method public static a(Landroid/content/Context;Landroid/content/SharedPreferences;)Z
    .locals 2

    .line 7
    invoke-static {p0}, Lanvb;->a(Ljava/lang/Object;)Ljava/lang/Object;

    .line 8
    invoke-static {p1}, Lanvb;->a(Ljava/lang/Object;)Ljava/lang/Object;

    const-string v0, "version"

    const-string v1, ""

    .line 9
    invoke-interface {p1, v0, v1}, Landroid/content/SharedPreferences;->getString(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;

    move-result-object v1

    .line 10
    invoke-virtual {p0}, Landroid/content/Context;->getApplicationContext()Landroid/content/Context;

    move-result-object p0

    invoke-static {p0}, Lynv;->b(Landroid/content/Context;)Ljava/lang/String;

    move-result-object p0

    .line 11
    invoke-virtual {p0, v1}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v1

    xor-int/lit8 v1, v1, 0x1

    if-eqz v1, :cond_0

    .line 12
    invoke-interface {p1}, Landroid/content/SharedPreferences;->edit()Landroid/content/SharedPreferences$Editor;

    move-result-object p1

    .line 13
    invoke-interface {p1, v0, p0}, Landroid/content/SharedPreferences$Editor;->putString(Ljava/lang/String;Ljava/lang/String;)Landroid/content/SharedPreferences$Editor;

    move-result-object p0

    .line 14
    invoke-interface {p0}, Landroid/content/SharedPreferences$Editor;->apply()V

    :cond_0
    return v1
.end method

.method public static a(Landroid/content/SharedPreferences;)Z
    .locals 2

    const-string v0, "visitor_id"

    const/4 v1, 0x0

    .line 15
    invoke-interface {p0, v0, v1}, Landroid/content/SharedPreferences;->getString(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;

    move-result-object p0

    if-eqz p0, :cond_0

    const/4 p0, 0x1

    return p0

    :cond_0
    const/4 p0, 0x0

    return p0
.end method

.method public static b(Landroid/content/Context;)Ljava/lang/String;
    .locals 1

    # translyte patch: this is the sole source of the "cver" value sent on
    # every innertube request (traced via Lagsd$a -> Lynv;->b -> Lagsz's
    # constructor, which builds the cplatform/c/cver/cos/cosver map) - the
    # real v14.34.54 client version, which the current backend no longer
    # accepts. Hardcoded to the community-known "oldest version that still
    # works" rather than reading PackageManager.getPackageInfo(...)
    # .versionName like the original body did, so AndroidManifest.xml and
    # every *other* PackageInfo-based check in the app (internal feature
    # gating, the About screen, etc.) still see the real "14.34.54" -
    # only what actually goes out over the network changes.
    const-string v0, "19.51.01"

    return-object v0
.end method

.method public static c(Landroid/content/Context;)I
    .locals 2

    .line 32
    invoke-static {p0}, Lanvb;->a(Ljava/lang/Object;)Ljava/lang/Object;

    .line 33
    sget-object v0, Lynv;->e:Lynv;

    iget-object v0, v0, Lynv;->c:Ljava/lang/Integer;

    if-nez v0, :cond_0

    const/4 v0, 0x0

    .line 34
    :try_start_0
    sget-object v1, Lynv;->e:Lynv;

    invoke-static {p0, v0}, Lynv;->a(Landroid/content/Context;I)Landroid/content/pm/PackageInfo;

    move-result-object p0

    iget p0, p0, Landroid/content/pm/PackageInfo;->versionCode:I

    invoke-static {p0}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    move-result-object p0

    iput-object p0, v1, Lynv;->c:Ljava/lang/Integer;
    :try_end_0
    .catch Landroid/content/pm/PackageManager$NameNotFoundException; {:try_start_0 .. :try_end_0} :catch_0

    goto :goto_0

    :catch_0
    move-exception p0

    const-string v1, "could not retrieve application version code"

    .line 36
    invoke-static {v1, p0}, Lymv;->b(Ljava/lang/String;Ljava/lang/Throwable;)V

    .line 37
    sget-object p0, Lynv;->e:Lynv;

    invoke-static {v0}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    move-result-object v0

    iput-object v0, p0, Lynv;->c:Ljava/lang/Integer;

    .line 35
    :cond_0
    :goto_0
    sget-object p0, Lynv;->e:Lynv;

    iget-object p0, p0, Lynv;->c:Ljava/lang/Integer;

    invoke-virtual {p0}, Ljava/lang/Integer;->intValue()I

    move-result p0

    return p0
.end method
