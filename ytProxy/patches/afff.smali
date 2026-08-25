.class public final Lafff;
.super Ljava/lang/Object;
.source "SourceFile"

# interfaces
.implements Laltp;


# direct methods
.method public constructor <init>()V
    .locals 0

    .line 1
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    return-void
.end method


# virtual methods
.method public final a(Lattn;)I
    .locals 2

    const/4 v0, 0x0

    if-eqz p1, :cond_2

    .line 2
    invoke-virtual {p1}, Lattn;->ordinal()I

    move-result p1

    const/16 v1, 0xb1

    if-eq p1, v1, :cond_1

    const/16 v1, 0xb2

    if-eq p1, v1, :cond_0

    # ytProxy patch: real device confirmed this class returns 0 for any
    # Lattn ordinal it doesn't specifically recognize (both branches
    # below), and a caller passing that straight to Resources.getDrawable
    # crashes with Resources$NotFoundException instead of getting a
    # missing/wrong icon. Fall back to one of this class's own already-
    # valid ids rather than 0.
    const p1, 0x7f08047c

    return p1

    :cond_0
    const p1, 0x7f08047c

    return p1

    :cond_1
    const p1, 0x7f0802e9

    return p1

    :cond_2
    const p1, 0x7f08047c

    return p1
.end method
