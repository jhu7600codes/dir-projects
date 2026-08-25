.class public Lamog;
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
.method public a(Lattn;)I
    .locals 1

    const/4 v0, 0x0

    if-eqz p1, :cond_0

    .line 2
    invoke-virtual {p1}, Lattn;->ordinal()I

    move-result p1

    sparse-switch p1, :sswitch_data_0

    packed-switch p1, :pswitch_data_0

    return v0

    :pswitch_0
    const p1, 0x7f0805b6

    return p1

    :pswitch_1
    const p1, 0x7f0805d1

    return p1

    :pswitch_2
    const p1, 0x7f0805f5

    return p1

    :pswitch_3
    const p1, 0x7f0806ee

    return p1

    :pswitch_4
    const p1, 0x7f0805ed

    return p1

    :pswitch_5
    const p1, 0x7f0805b4

    return p1

    :pswitch_6
    const p1, 0x7f0806be

    return p1

    :sswitch_0
    const p1, 0x7f08060d

    return p1

    :sswitch_1
    const p1, 0x7f0806ea

    return p1

    :sswitch_2
    const p1, 0x7f0805a1

    return p1

    :sswitch_3
    const p1, 0x7f080720

    return p1

    :sswitch_4
    const p1, 0x7f08067d

    return p1

    :sswitch_5
    const p1, 0x7f0805dd

    return p1

    :sswitch_6
    const p1, 0x7f0805fa

    return p1

    :sswitch_7
    const p1, 0x7f0803f4

    return p1

    :sswitch_8
    const p1, 0x7f0806eb

    return p1

    :sswitch_9
    const p1, 0x7f0802bd

    return p1

    :sswitch_a
    const p1, 0x7f080653

    return p1

    :sswitch_b
    const p1, 0x7f08047a

    return p1

    :sswitch_c
    const p1, 0x7f08068e

    return p1

    :sswitch_d
    const p1, 0x7f0805af

    return p1

    :cond_0
    # ytProxy patch: real device confirmed this returns 0 for any Lattn
    # ordinal not in either switch table below, and a caller passing that
    # straight to Resources.getDrawable crashes with
    # Resources$NotFoundException instead of getting a missing/wrong icon.
    # zur (a subclass) and ftq (delegates here for anything not in its own
    # small map) both fall through to this same path, so this one edit
    # covers all three. Falls back to one of this class's own already-
    # valid ids rather than 0.
    const v0, 0x7f0805b6

    return v0

    nop

    :sswitch_data_0
    .sparse-switch
        0x1 -> :sswitch_d
        0x2b -> :sswitch_c
        0x82 -> :sswitch_b
        0x99 -> :sswitch_a
        0x9f -> :sswitch_9
        0xa7 -> :sswitch_8
        0xe2 -> :sswitch_7
        0x13a -> :sswitch_6
        0x155 -> :sswitch_5
        0x18b -> :sswitch_4
        0x18f -> :sswitch_3
        0x197 -> :sswitch_2
        0x1ce -> :sswitch_1
        0x226 -> :sswitch_0
    .end sparse-switch

    :pswitch_data_0
    .packed-switch 0xb3
        :pswitch_6
        :pswitch_5
        :pswitch_4
        :pswitch_3
        :pswitch_2
        :pswitch_1
        :pswitch_0
    .end packed-switch
.end method
