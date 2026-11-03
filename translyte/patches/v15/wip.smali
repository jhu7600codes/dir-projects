.class final Lwip;
.super Lutr;
.source "PG"


# instance fields
.field final synthetic d:Lvnf;


# direct methods
.method public constructor <init>(Lthp;Ljava/lang/String;Lvnf;)V
    .locals 0

    iput-object p3, p0, Lwip;->d:Lvnf;

    .line 1
    invoke-direct {p0, p1, p2}, Lutr;-><init>(Lthp;Ljava/lang/String;)V

    return-void
.end method


# virtual methods
.method protected final a(Lust;)V
    .locals 0

    # translyte patch: this is the sole call site (Lwip is the only Lutr
    # subclass) that commits a freshly server-fetched Phenotype/experiment
    # snapshot into the app's live SharedPreferences-backed config - the
    # exact mechanism Google uses to remotely flip UI/behavior on an
    # already-installed app without pushing an update. Made a no-op: the
    # app still fetches snapshots (harmless network chatter, unchanged),
    # it just never applies them, so every experiment-gated code path
    # keeps using whatever's already compiled/cached rather than whatever
    # bucket the server would otherwise switch it into.
    return-void
.end method
