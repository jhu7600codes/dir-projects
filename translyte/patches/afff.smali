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

    # translyte: reverted - see afff/amog/adon/ftn note in kel.smali's
    # neighborhood / README. Returning a made-up nonzero id here for the
    # ~264 of 285 Lattn values this class was never wired to recognize
    # made a WRONG icon appear at nearly every one of the 108 call sites
    # across the app (most of which already treat 0 as "no icon, don't
    # draw one" and were perfectly fine with it) - this is the "+ glyph
    # showing up everywhere" bug. Restored original 0-returning behavior;
    # the actual crash risk is handled at the few call sites that don't
    # already tolerate 0 (see kel.smali), not here.
    return v0

    :cond_0
    const p1, 0x7f08047c

    return p1

    :cond_1
    const p1, 0x7f0802e9

    return p1

    :cond_2
    return v0
.end method
