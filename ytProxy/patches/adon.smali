.class public final Ladon;
.super Ljava/lang/Object;
.source "SourceFile"

# interfaces
.implements Laltp;


# instance fields
.field private volatile a:Ljava/util/EnumMap;


# direct methods
.method public constructor <init>()V
    .locals 5

    .line 1
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    .line 2
    new-instance v0, Ljava/util/EnumMap;

    const-class v1, Lattn;

    invoke-direct {v0, v1}, Ljava/util/EnumMap;-><init>(Ljava/lang/Class;)V

    iput-object v0, p0, Ladon;->a:Ljava/util/EnumMap;

    .line 3
    iget-object v0, p0, Ladon;->a:Ljava/util/EnumMap;

    sget-object v1, Lattn;->cC:Lattn;

    const v2, 0x7f08067e

    invoke-static {v2}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    move-result-object v2

    invoke-virtual {v0, v1, v2}, Ljava/util/EnumMap;->put(Ljava/lang/Enum;Ljava/lang/Object;)Ljava/lang/Object;

    .line 4
    iget-object v0, p0, Ladon;->a:Ljava/util/EnumMap;

    sget-object v1, Lattn;->cp:Lattn;

    const v2, 0x7f0806e4

    invoke-static {v2}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    move-result-object v2

    invoke-virtual {v0, v1, v2}, Ljava/util/EnumMap;->put(Ljava/lang/Enum;Ljava/lang/Object;)Ljava/lang/Object;

    .line 5
    iget-object v0, p0, Ladon;->a:Ljava/util/EnumMap;

    sget-object v1, Lattn;->bI:Lattn;

    const v2, 0x7f0806cb

    .line 6
    invoke-static {v2}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    move-result-object v2

    .line 7
    invoke-virtual {v0, v1, v2}, Ljava/util/EnumMap;->put(Ljava/lang/Enum;Ljava/lang/Object;)Ljava/lang/Object;

    .line 8
    iget-object v0, p0, Ladon;->a:Ljava/util/EnumMap;

    sget-object v1, Lattn;->bH:Lattn;

    invoke-virtual {v0, v1, v2}, Ljava/util/EnumMap;->put(Ljava/lang/Enum;Ljava/lang/Object;)Ljava/lang/Object;

    .line 9
    iget-object v0, p0, Ladon;->a:Ljava/util/EnumMap;

    sget-object v1, Lattn;->cb:Lattn;

    const v2, 0x7f080235

    .line 10
    invoke-static {v2}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    move-result-object v2

    .line 11
    invoke-virtual {v0, v1, v2}, Ljava/util/EnumMap;->put(Ljava/lang/Enum;Ljava/lang/Object;)Ljava/lang/Object;

    .line 12
    iget-object v0, p0, Ladon;->a:Ljava/util/EnumMap;

    sget-object v1, Lattn;->cd:Lattn;

    const v3, 0x7f080234

    .line 13
    invoke-static {v3}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    move-result-object v3

    .line 14
    invoke-virtual {v0, v1, v3}, Ljava/util/EnumMap;->put(Ljava/lang/Enum;Ljava/lang/Object;)Ljava/lang/Object;

    .line 15
    iget-object v0, p0, Ladon;->a:Ljava/util/EnumMap;

    sget-object v1, Lattn;->cf:Lattn;

    const v4, 0x7f080756

    .line 16
    invoke-static {v4}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    move-result-object v4

    .line 17
    invoke-virtual {v0, v1, v4}, Ljava/util/EnumMap;->put(Ljava/lang/Enum;Ljava/lang/Object;)Ljava/lang/Object;

    .line 18
    iget-object v0, p0, Ladon;->a:Ljava/util/EnumMap;

    sget-object v1, Lattn;->A:Lattn;

    const v4, 0x7f08070b

    .line 19
    invoke-static {v4}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    move-result-object v4

    .line 20
    invoke-virtual {v0, v1, v4}, Ljava/util/EnumMap;->put(Ljava/lang/Enum;Ljava/lang/Object;)Ljava/lang/Object;

    .line 21
    iget-object v0, p0, Ladon;->a:Ljava/util/EnumMap;

    sget-object v1, Lattn;->z:Lattn;

    invoke-virtual {v0, v1, v4}, Ljava/util/EnumMap;->put(Ljava/lang/Enum;Ljava/lang/Object;)Ljava/lang/Object;

    .line 22
    iget-object v0, p0, Ladon;->a:Ljava/util/EnumMap;

    sget-object v1, Lattn;->cE:Lattn;

    const v4, 0x7f0805d2

    .line 23
    invoke-static {v4}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    move-result-object v4

    .line 24
    invoke-virtual {v0, v1, v4}, Ljava/util/EnumMap;->put(Ljava/lang/Enum;Ljava/lang/Object;)Ljava/lang/Object;

    .line 25
    iget-object v0, p0, Ladon;->a:Ljava/util/EnumMap;

    sget-object v1, Lattn;->x:Lattn;

    const v4, 0x7f0805ca

    .line 26
    invoke-static {v4}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    move-result-object v4

    .line 27
    invoke-virtual {v0, v1, v4}, Ljava/util/EnumMap;->put(Ljava/lang/Enum;Ljava/lang/Object;)Ljava/lang/Object;

    .line 28
    iget-object v0, p0, Ladon;->a:Ljava/util/EnumMap;

    sget-object v1, Lattn;->y:Lattn;

    const v4, 0x7f080690

    .line 29
    invoke-static {v4}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    move-result-object v4

    .line 30
    invoke-virtual {v0, v1, v4}, Ljava/util/EnumMap;->put(Ljava/lang/Enum;Ljava/lang/Object;)Ljava/lang/Object;

    .line 31
    iget-object v0, p0, Ladon;->a:Ljava/util/EnumMap;

    sget-object v1, Lattn;->cv:Lattn;

    const v4, 0x7f080426

    invoke-static {v4}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    move-result-object v4

    invoke-virtual {v0, v1, v4}, Ljava/util/EnumMap;->put(Ljava/lang/Enum;Ljava/lang/Object;)Ljava/lang/Object;

    .line 32
    iget-object v0, p0, Ladon;->a:Ljava/util/EnumMap;

    sget-object v1, Lattn;->cx:Lattn;

    const v4, 0x7f080292

    invoke-static {v4}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    move-result-object v4

    invoke-virtual {v0, v1, v4}, Ljava/util/EnumMap;->put(Ljava/lang/Enum;Ljava/lang/Object;)Ljava/lang/Object;

    .line 33
    iget-object v0, p0, Ladon;->a:Ljava/util/EnumMap;

    sget-object v1, Lattn;->cA:Lattn;

    const v4, 0x7f080640

    invoke-static {v4}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    move-result-object v4

    invoke-virtual {v0, v1, v4}, Ljava/util/EnumMap;->put(Ljava/lang/Enum;Ljava/lang/Object;)Ljava/lang/Object;

    .line 34
    iget-object v0, p0, Ladon;->a:Ljava/util/EnumMap;

    sget-object v1, Lattn;->cB:Lattn;

    const v4, 0x7f08063f

    invoke-static {v4}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    move-result-object v4

    invoke-virtual {v0, v1, v4}, Ljava/util/EnumMap;->put(Ljava/lang/Enum;Ljava/lang/Object;)Ljava/lang/Object;

    .line 35
    iget-object v0, p0, Ladon;->a:Ljava/util/EnumMap;

    sget-object v1, Lattn;->cy:Lattn;

    const v4, 0x7f08068b

    invoke-static {v4}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    move-result-object v4

    invoke-virtual {v0, v1, v4}, Ljava/util/EnumMap;->put(Ljava/lang/Enum;Ljava/lang/Object;)Ljava/lang/Object;

    .line 36
    iget-object v0, p0, Ladon;->a:Ljava/util/EnumMap;

    sget-object v1, Lattn;->cz:Lattn;

    const v4, 0x7f080689

    invoke-static {v4}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    move-result-object v4

    invoke-virtual {v0, v1, v4}, Ljava/util/EnumMap;->put(Ljava/lang/Enum;Ljava/lang/Object;)Ljava/lang/Object;

    .line 37
    iget-object v0, p0, Ladon;->a:Ljava/util/EnumMap;

    sget-object v1, Lattn;->cl:Lattn;

    const v4, 0x7f08069b

    invoke-static {v4}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    move-result-object v4

    invoke-virtual {v0, v1, v4}, Ljava/util/EnumMap;->put(Ljava/lang/Enum;Ljava/lang/Object;)Ljava/lang/Object;

    .line 38
    iget-object v0, p0, Ladon;->a:Ljava/util/EnumMap;

    sget-object v1, Lattn;->cc:Lattn;

    invoke-virtual {v0, v1, v2}, Ljava/util/EnumMap;->put(Ljava/lang/Enum;Ljava/lang/Object;)Ljava/lang/Object;

    .line 39
    iget-object v0, p0, Ladon;->a:Ljava/util/EnumMap;

    sget-object v1, Lattn;->as:Lattn;

    const v2, 0x7f080654

    invoke-static {v2}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    move-result-object v2

    invoke-virtual {v0, v1, v2}, Ljava/util/EnumMap;->put(Ljava/lang/Enum;Ljava/lang/Object;)Ljava/lang/Object;

    .line 40
    iget-object v0, p0, Ladon;->a:Ljava/util/EnumMap;

    sget-object v1, Lattn;->at:Lattn;

    invoke-virtual {v0, v1, v2}, Ljava/util/EnumMap;->put(Ljava/lang/Enum;Ljava/lang/Object;)Ljava/lang/Object;

    .line 41
    iget-object v0, p0, Ladon;->a:Ljava/util/EnumMap;

    sget-object v1, Lattn;->ec:Lattn;

    const v2, 0x7f0802ef

    invoke-static {v2}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    move-result-object v2

    invoke-virtual {v0, v1, v2}, Ljava/util/EnumMap;->put(Ljava/lang/Enum;Ljava/lang/Object;)Ljava/lang/Object;

    .line 42
    iget-object v0, p0, Ladon;->a:Ljava/util/EnumMap;

    sget-object v1, Lattn;->ed:Lattn;

    invoke-virtual {v0, v1, v2}, Ljava/util/EnumMap;->put(Ljava/lang/Enum;Ljava/lang/Object;)Ljava/lang/Object;

    .line 43
    iget-object v0, p0, Ladon;->a:Ljava/util/EnumMap;

    sget-object v1, Lattn;->ca:Lattn;

    invoke-virtual {v0, v1, v3}, Ljava/util/EnumMap;->put(Ljava/lang/Enum;Ljava/lang/Object;)Ljava/lang/Object;

    .line 44
    iget-object v0, p0, Ladon;->a:Ljava/util/EnumMap;

    sget-object v1, Lattn;->bZ:Lattn;

    const v2, 0x7f0805de

    invoke-static {v2}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    move-result-object v2

    invoke-virtual {v0, v1, v2}, Ljava/util/EnumMap;->put(Ljava/lang/Enum;Ljava/lang/Object;)Ljava/lang/Object;

    .line 45
    iget-object v0, p0, Ladon;->a:Ljava/util/EnumMap;

    sget-object v1, Lattn;->ce:Lattn;

    const v2, 0x7f080415

    invoke-static {v2}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    move-result-object v2

    invoke-virtual {v0, v1, v2}, Ljava/util/EnumMap;->put(Ljava/lang/Enum;Ljava/lang/Object;)Ljava/lang/Object;

    .line 46
    iget-object v0, p0, Ladon;->a:Ljava/util/EnumMap;

    sget-object v1, Lattn;->bP:Lattn;

    const v2, 0x7f0805fc

    invoke-static {v2}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    move-result-object v2

    invoke-virtual {v0, v1, v2}, Ljava/util/EnumMap;->put(Ljava/lang/Enum;Ljava/lang/Object;)Ljava/lang/Object;

    .line 47
    iget-object v0, p0, Ladon;->a:Ljava/util/EnumMap;

    sget-object v1, Lattn;->bO:Lattn;

    const v2, 0x7f0805f8

    invoke-static {v2}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    move-result-object v2

    invoke-virtual {v0, v1, v2}, Ljava/util/EnumMap;->put(Ljava/lang/Enum;Ljava/lang/Object;)Ljava/lang/Object;

    .line 48
    iget-object v0, p0, Ladon;->a:Ljava/util/EnumMap;

    sget-object v1, Lattn;->cn:Lattn;

    const v2, 0x7f0805ba

    invoke-static {v2}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    move-result-object v2

    invoke-virtual {v0, v1, v2}, Ljava/util/EnumMap;->put(Ljava/lang/Enum;Ljava/lang/Object;)Ljava/lang/Object;

    .line 49
    iget-object v0, p0, Ladon;->a:Ljava/util/EnumMap;

    sget-object v1, Lattn;->de:Lattn;

    const v2, 0x7f0806d0

    invoke-static {v2}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    move-result-object v2

    invoke-virtual {v0, v1, v2}, Ljava/util/EnumMap;->put(Ljava/lang/Enum;Ljava/lang/Object;)Ljava/lang/Object;

    .line 50
    iget-object v0, p0, Ladon;->a:Ljava/util/EnumMap;

    sget-object v1, Lattn;->aG:Lattn;

    const v2, 0x7f080623

    invoke-static {v2}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    move-result-object v2

    invoke-virtual {v0, v1, v2}, Ljava/util/EnumMap;->put(Ljava/lang/Enum;Ljava/lang/Object;)Ljava/lang/Object;

    .line 51
    iget-object v0, p0, Ladon;->a:Ljava/util/EnumMap;

    sget-object v1, Lattn;->w:Lattn;

    const v2, 0x7f08060e

    invoke-static {v2}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    move-result-object v2

    invoke-virtual {v0, v1, v2}, Ljava/util/EnumMap;->put(Ljava/lang/Enum;Ljava/lang/Object;)Ljava/lang/Object;

    .line 52
    iget-object v0, p0, Ladon;->a:Ljava/util/EnumMap;

    sget-object v1, Lattn;->cF:Lattn;

    const v2, 0x7f080639

    invoke-static {v2}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    move-result-object v2

    invoke-virtual {v0, v1, v2}, Ljava/util/EnumMap;->put(Ljava/lang/Enum;Ljava/lang/Object;)Ljava/lang/Object;

    .line 53
    iget-object v0, p0, Ladon;->a:Ljava/util/EnumMap;

    sget-object v1, Lattn;->t:Lattn;

    const v2, 0x7f0805bc

    invoke-static {v2}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    move-result-object v2

    invoke-virtual {v0, v1, v2}, Ljava/util/EnumMap;->put(Ljava/lang/Enum;Ljava/lang/Object;)Ljava/lang/Object;

    .line 54
    iget-object v0, p0, Ladon;->a:Ljava/util/EnumMap;

    sget-object v1, Lattn;->cw:Lattn;

    const v2, 0x7f0805be

    invoke-static {v2}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    move-result-object v2

    invoke-virtual {v0, v1, v2}, Ljava/util/EnumMap;->put(Ljava/lang/Enum;Ljava/lang/Object;)Ljava/lang/Object;

    .line 55
    iget-object v0, p0, Ladon;->a:Ljava/util/EnumMap;

    sget-object v1, Lattn;->O:Lattn;

    const v2, 0x7f080610

    invoke-static {v2}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    move-result-object v2

    invoke-virtual {v0, v1, v2}, Ljava/util/EnumMap;->put(Ljava/lang/Enum;Ljava/lang/Object;)Ljava/lang/Object;

    .line 56
    iget-object v0, p0, Ladon;->a:Ljava/util/EnumMap;

    sget-object v1, Lattn;->dx:Lattn;

    const v2, 0x7f08075b

    invoke-static {v2}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    move-result-object v2

    invoke-virtual {v0, v1, v2}, Ljava/util/EnumMap;->put(Ljava/lang/Enum;Ljava/lang/Object;)Ljava/lang/Object;

    .line 57
    iget-object v0, p0, Ladon;->a:Ljava/util/EnumMap;

    sget-object v1, Lattn;->eb:Lattn;

    const v2, 0x7f080723

    invoke-static {v2}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    move-result-object v2

    invoke-virtual {v0, v1, v2}, Ljava/util/EnumMap;->put(Ljava/lang/Enum;Ljava/lang/Object;)Ljava/lang/Object;

    .line 58
    iget-object v0, p0, Ladon;->a:Ljava/util/EnumMap;

    sget-object v1, Lattn;->ea:Lattn;

    const v2, 0x7f08068d

    .line 59
    invoke-static {v2}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    move-result-object v2

    .line 60
    invoke-virtual {v0, v1, v2}, Ljava/util/EnumMap;->put(Ljava/lang/Enum;Ljava/lang/Object;)Ljava/lang/Object;

    .line 61
    iget-object v0, p0, Ladon;->a:Ljava/util/EnumMap;

    sget-object v1, Lattn;->aF:Lattn;

    const v2, 0x7f08069e

    invoke-static {v2}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    move-result-object v2

    invoke-virtual {v0, v1, v2}, Ljava/util/EnumMap;->put(Ljava/lang/Enum;Ljava/lang/Object;)Ljava/lang/Object;

    return-void
.end method


# virtual methods
.method public final a(Lattn;)I
    .locals 1

    .line 62
    iget-object v0, p0, Ladon;->a:Ljava/util/EnumMap;

    invoke-virtual {v0, p1}, Ljava/util/EnumMap;->containsKey(Ljava/lang/Object;)Z

    move-result v0

    if-eqz v0, :cond_0

    iget-object v0, p0, Ladon;->a:Ljava/util/EnumMap;

    invoke-virtual {v0, p1}, Ljava/util/EnumMap;->get(Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object p1

    check-cast p1, Ljava/lang/Integer;

    invoke-virtual {p1}, Ljava/lang/Integer;->intValue()I

    move-result p1

    return p1

    :cond_0
    # ytProxy: reverted, see amog.smali - restored original 0-returning
    # behavior instead of a made-up nonzero fallback (was causing a wrong
    # icon to appear at nearly every Laltp call site, not just the rare
    # crash case).
    const/4 p1, 0x0

    return p1
.end method
