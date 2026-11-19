package com.patternbox.core

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.EOFException
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread

/**
 * local wifi multiplayer. the host phone runs the real game, everyone else just
 * sends their buttons and draws whatever the host sends back (~30 times a second).
 * plain tcp, one message = [type byte][length int][payload].
 */
object Proto {
    const val PORT = 47474
    const val VERSION = 1
    const val HELLO: Byte = 1     // client -> host: version, name, fighter code
    const val INPUT: Byte = 2     // client -> host: buttons
    const val LOBBY: Byte = 3     // host -> client: who's here, mode, map
    const val START: Byte = 4     // host -> client: match begins, which fighter is yours
    const val SNAP: Byte = 5      // host -> client: the whole arena
    const val KICK: Byte = 6      // host -> client: go away (wrong version, full)

    fun write(out: DataOutputStream, type: Byte, payload: ByteArray) {
        synchronized(out) {
            out.writeByte(type.toInt()); out.writeInt(payload.size); out.write(payload); out.flush()
        }
    }

    fun read(inp: DataInputStream): Pair<Byte, ByteArray> {
        val type = inp.readByte()
        val len = inp.readInt()
        if (len < 0 || len > 4_000_000) throw EOFException("bad length $len")
        val b = ByteArray(len)
        inp.readFully(b)
        return type to b
    }

    fun bytes(block: DataOutputStream.() -> Unit): ByteArray {
        val bo = ByteArrayOutputStream()
        DataOutputStream(bo).use { it.block() }
        return bo.toByteArray()
    }

    fun data(b: ByteArray) = DataInputStream(ByteArrayInputStream(b))
}

data class LobbyInfo(val you: Int, val players: List<Pair<String, String>>, val mode: Mode, val map: GameMap)
data class StartInfo(val mode: Mode, val map: GameMap, val entries: List<Entry>, val yourFighter: Int)

/** one remote phone as the host sees it */
class Peer(internal val socket: Socket) {
    @Volatile var name = "someone"
    @Volatile var code = ""
    @Volatile var ready = false
    @Volatile var connected = true
    @Volatile var left = false
    @Volatile var right = false
    @Volatile var jump = false
    @Volatile var swings = 0
    @Volatile var abilities = 0
    // what the host already acted on
    var usedSwings = 0
    var usedAbilities = 0
    val loadout get() = Loadout.decode(code)

    internal val out = DataOutputStream(socket.getOutputStream().buffered())
    private val queue = ArrayBlockingQueue<Pair<Byte, ByteArray>>(6)

    init {
        thread(isDaemon = true, name = "pb-peer-writer") {
            try {
                while (connected) {
                    val m = queue.poll(500, TimeUnit.MILLISECONDS) ?: continue
                    Proto.write(out, m.first, m.second)
                }
            } catch (_: Exception) { connected = false }
        }
    }

    /** snapshots are disposable: if the phone is slow, drop the oldest instead of lagging further behind */
    fun send(type: Byte, payload: ByteArray) {
        if (!connected) return
        while (!queue.offer(type to payload)) queue.poll()
    }

    fun close() {
        connected = false
        runCatching { socket.close() }
    }
}

class NetHost(val port: Int = Proto.PORT) {
    val peers = CopyOnWriteArrayList<Peer>()
    @Volatile var onChange: () -> Unit = {}
    @Volatile var maxPeers = 5
    private var server: ServerSocket? = null
    @Volatile private var running = false

    val localPort get() = server?.localPort ?: port

    fun start() {
        val s = ServerSocket()
        s.reuseAddress = true
        s.bind(InetSocketAddress(port))
        server = s
        running = true
        thread(isDaemon = true, name = "pb-accept") {
            while (running) {
                val sock = try { s.accept() } catch (_: Exception) { break }
                sock.tcpNoDelay = true
                val p = Peer(sock)
                if (peers.count { it.connected } >= maxPeers) {
                    p.send(Proto.KICK, Proto.bytes { writeUTF("the game is full") })
                    Thread.sleep(200); p.close(); continue
                }
                peers += p
                thread(isDaemon = true, name = "pb-peer-reader") { readLoop(p) }
            }
        }
    }

    private fun readLoop(p: Peer) {
        try {
            val inp = DataInputStream(p.socket.getInputStream().buffered())
            while (running && p.connected) {
                val (type, b) = Proto.read(inp)
                val d = Proto.data(b)
                when (type) {
                    Proto.HELLO -> {
                        val v = d.readInt()
                        p.name = d.readUTF().take(24).ifBlank { "someone" }
                        p.code = d.readUTF()
                        if (v != Proto.VERSION || p.loadout == null) {
                            p.send(Proto.KICK, Proto.bytes { writeUTF(if (v != Proto.VERSION) "different patternbox version, update both phones" else "bad fighter") })
                            Thread.sleep(200); break
                        }
                        p.ready = true
                        onChange()
                    }
                    Proto.INPUT -> {
                        val bits = d.readByte().toInt()
                        p.left = bits and 1 != 0; p.right = bits and 2 != 0; p.jump = bits and 4 != 0
                        p.swings = d.readInt(); p.abilities = d.readInt()
                    }
                }
            }
        } catch (_: Exception) {
        }
        p.close()
        peers.remove(p)
        onChange()
    }

    val readyPeers get() = peers.filter { it.connected && it.ready }

    fun sendLobby(hostName: String, hostCode: String, mode: Mode, map: GameMap) {
        val list = listOf(hostName to hostCode) + readyPeers.map { it.name to it.code }
        readyPeers.forEachIndexed { i, p ->
            p.send(Proto.LOBBY, Proto.bytes {
                writeInt(i + 1); writeInt(mode.ordinal); writeInt(map.ordinal)
                writeInt(list.size); for ((n, c) in list) { writeUTF(n); writeUTF(c) }
            })
        }
    }

    /** fighterOf(peer) = index into the arena's fighters for that phone, -1 to just watch */
    fun sendStart(mode: Mode, map: GameMap, entries: List<Entry>, fighterOf: (Peer) -> Int) {
        for (p in readyPeers) {
            p.usedSwings = p.swings; p.usedAbilities = p.abilities
            p.send(Proto.START, Proto.bytes {
                writeInt(mode.ordinal); writeInt(map.ordinal); writeInt(fighterOf(p))
                writeInt(entries.size); for (e in entries) { writeUTF(e.name); writeUTF(e.loadout.encode()) }
            })
        }
    }

    fun sendSnapshot(bytes: ByteArray) {
        for (p in readyPeers) p.send(Proto.SNAP, bytes)
    }

    fun close() {
        running = false
        runCatching { server?.close() }
        for (p in peers) p.close()
        peers.clear()
    }
}

class NetClient {
    private var socket: Socket? = null
    private var out: DataOutputStream? = null
    @Volatile var connected = false; private set
    @Volatile var lobby: LobbyInfo? = null; private set
    @Volatile var error: String? = null; private set
    /** bumps every time a START arrives, so the ui can tell a rematch from the same match */
    @Volatile var startCount = 0; private set
    @Volatile var start: StartInfo? = null; private set
    val latestSnap = AtomicReference<ByteArray?>(null)
    @Volatile var onChange: () -> Unit = {}
    private var swings = 0
    private var abilities = 0

    /** blocking, call off the main thread */
    fun connect(host: String, name: String, code: String, port: Int = Proto.PORT, timeoutMs: Int = 4000) {
        try {
            val s = Socket()
            s.connect(InetSocketAddress(host, port), timeoutMs)
            s.tcpNoDelay = true
            socket = s
            val o = DataOutputStream(s.getOutputStream().buffered())
            out = o
            connected = true
            Proto.write(o, Proto.HELLO, Proto.bytes { writeInt(Proto.VERSION); writeUTF(name); writeUTF(code) })
            thread(isDaemon = true, name = "pb-client-reader") { readLoop(s) }
        } catch (e: Exception) {
            error = "couldn't reach $host (${e.message ?: "no answer"})"
            connected = false
        }
        onChange()
    }

    private fun readLoop(s: Socket) {
        try {
            val inp = DataInputStream(s.getInputStream().buffered())
            while (connected) {
                val (type, b) = Proto.read(inp)
                val d = Proto.data(b)
                when (type) {
                    Proto.LOBBY -> {
                        val you = d.readInt()
                        val mode = Mode.values()[d.readInt()]
                        val map = GameMap.values()[d.readInt()]
                        val n = d.readInt()
                        lobby = LobbyInfo(you, List(n) { d.readUTF() to d.readUTF() }, mode, map)
                        onChange()
                    }
                    Proto.START -> {
                        val mode = Mode.values()[d.readInt()]
                        val map = GameMap.values()[d.readInt()]
                        val yours = d.readInt()
                        val n = d.readInt()
                        val entries = List(n) {
                            val name = d.readUTF()
                            Entry(name, Loadout.decode(d.readUTF()) ?: Loadout.DEFAULT_P1)
                        }
                        latestSnap.set(null)
                        start = StartInfo(mode, map, entries, yours)
                        startCount++
                        onChange()
                    }
                    Proto.SNAP -> latestSnap.set(b)
                    Proto.KICK -> { error = d.readUTF(); break }
                }
            }
        } catch (_: Exception) {
            if (error == null) error = "lost connection to the host"
        }
        connected = false
        onChange()
    }

    fun sendInput(left: Boolean, right: Boolean, jump: Boolean) {
        val o = out ?: return
        if (!connected) return
        val bits = (if (left) 1 else 0) or (if (right) 2 else 0) or (if (jump) 4 else 0)
        val payload = Proto.bytes { writeByte(bits); writeInt(swings); writeInt(abilities) }
        thread(isDaemon = true) { runCatching { Proto.write(o, Proto.INPUT, payload) } }
    }

    fun swing() { swings++ }
    fun ability() { abilities++ }

    fun close() {
        connected = false
        runCatching { socket?.close() }
    }
}

/** the host's arena squeezed into bytes, and back into a mirror arena on the other phone */
object Snapshot {
    private const val MAX_DEBRIS = 150
    private const val MAX_PARTICLES = 160

    fun write(a: Arena, events: List<Event>, extra: String): ByteArray = Proto.bytes {
        writeFloat(a.width); writeFloat(a.height); writeFloat(a.time); writeFloat(a.shake)
        writeInt(a.winnerTeam ?: Int.MIN_VALUE); writeFloat(a.winT); writeInt(a.map.ordinal)
        writeFloat(a.zoneX ?: Float.NaN); writeFloat(a.wind)
        writeInt(a.scores.size); for ((t, v) in a.scores) { writeInt(t); writeFloat(v) }
        writeUTF(extra)

        writeInt(a.fighters.size)
        for (f in a.fighters) {
            writeInt(f.id); writeUTF(f.name); writeUTF(f.loadout.encode()); writeInt(f.team)
            writeFloat(f.x); writeFloat(f.y); writeFloat(f.ang); writeFloat(f.face)
            writeFloat(f.hp); writeFloat(f.maxHp); writeFloat(f.scale)
            var flags = 0
            fun bit(i: Int, b: Boolean) { if (b) flags = flags or (1 shl i) }
            bit(0, f.dead); bit(1, f.held); bit(2, f.freezeT > 0); bit(3, f.burnT > 0); bit(4, f.rageT > 0)
            bit(5, f.bubbleT > 0); bit(6, f.invisT > 0); bit(7, f.poisonT > 0); bit(8, f.weaponGone > 0)
            bit(9, f.weaponThrown); bit(10, f.minionT > 0); bit(11, f.bot)
            writeInt(flags)
            writeFloat(f.stun); writeFloat(f.swingT); writeFloat(f.abilityCd); writeFloat(f.look)
            writeInt(f.kos); writeFloat(f.dmgDealt)
            var plates = 0L
            f.plates.forEachIndexed { i, b -> if (b && i < 63) plates = plates or (1L shl i) }
            writeLong(plates)
        }

        writeInt(a.props.size)
        for (p in a.props) {
            writeByte(p.kind.ordinal); writeFloat(p.x); writeFloat(p.y); writeFloat(p.w); writeFloat(p.h)
            writeFloat(p.ang); writeFloat(p.flash)
        }

        writeInt(a.projectiles.size)
        for (p in a.projectiles) {
            writeInt(p.owner.id); writeByte(p.kind.ordinal); writeFloat(p.x); writeFloat(p.y); writeFloat(p.ang)
        }

        val deb = a.debris.takeLast(MAX_DEBRIS)
        writeInt(deb.size)
        for (d in deb) {
            writeFloat(d.x); writeFloat(d.y); writeFloat(d.ang)
            writeFloat(d.a.x); writeFloat(d.a.y); writeFloat(d.b.x); writeFloat(d.b.y)
            writeByte(d.kind.ordinal); writeInt(d.owner); writeFloat(d.life)
        }

        val parts = a.particles.takeLast(MAX_PARTICLES)
        writeInt(parts.size)
        for (p in parts) { writeFloat(p.x); writeFloat(p.y); writeByte(p.kind.ordinal); writeFloat(p.life); writeFloat(p.maxLife) }

        writeInt(a.texts.size)
        for (t in a.texts) { writeFloat(t.x); writeFloat(t.y); writeUTF(t.text); writeByte(t.tone.ordinal); writeFloat(t.life) }

        writeInt(a.booms.size)
        for (b in a.booms) { writeFloat(b.x); writeFloat(b.y); writeFloat(b.radius); writeFloat(b.t) }

        writeInt(a.beams.size)
        for (b in a.beams) { writeFloat(b.x1); writeFloat(b.y1); writeFloat(b.x2); writeFloat(b.y2); writeInt(b.team); writeFloat(b.life) }

        writeInt(events.size)
        for (e in events) {
            when (e) {
                is Event.Hit -> { writeByte(0); writeInt(e.dmg); writeBoolean(e.crit) }
                Event.Clang -> writeByte(1)
                is Event.Boom -> { writeByte(2); writeBoolean(e.big) }
                Event.Ko -> writeByte(3)
                Event.Shoot -> writeByte(4)
                Event.Jump -> writeByte(5)
                Event.Bounce -> writeByte(6)
                Event.Break -> writeByte(7)
                Event.Ability -> writeByte(8)
                Event.Win -> writeByte(9)
            }
        }
    }

    /** fills the mirror arena; returns the sounds to play and the mode's status line */
    fun apply(a: Arena, bytes: ByteArray): Pair<List<Event>, String> {
        val d = Proto.data(bytes)
        a.remote = true
        a.width = d.readFloat(); a.height = d.readFloat(); a.time = d.readFloat(); a.shake = d.readFloat()
        val w = d.readInt()
        a.setWinnerRemote(if (w == Int.MIN_VALUE) null else w, 0f)
        a.winT = d.readFloat()
        a.map = GameMap.values()[d.readInt()]
        a.zoneX = d.readFloat().takeIf { !it.isNaN() }
        a.wind = d.readFloat()
        a.scores.clear(); repeat(d.readInt()) { a.scores[d.readInt()] = d.readFloat() }
        val extra = d.readUTF()

        val old = a.fighters.associateBy { it.id }
        a.fighters.clear()
        repeat(d.readInt()) {
            val id = d.readInt(); val name = d.readUTF(); val code = d.readUTF(); val team = d.readInt()
            val prev = old[id]
            val f = if (prev != null && prev.name == name && prev.loadout.encode() == code) prev
            else Fighter(id, Loadout.decode(code) ?: Loadout.DEFAULT_P1, name, team)
            f.team = team
            f.x = d.readFloat(); f.y = d.readFloat(); f.ang = d.readFloat(); f.face = d.readFloat()
            f.hp = d.readFloat(); f.maxHp = d.readFloat(); f.scale = d.readFloat()
            val flags = d.readInt()
            fun bit(i: Int) = flags and (1 shl i) != 0
            f.dead = bit(0); f.held = bit(1)
            f.freezeT = if (bit(2)) 1f else 0f; f.burnT = if (bit(3)) 1f else 0f; f.rageT = if (bit(4)) 1f else 0f
            f.bubbleT = if (bit(5)) 1f else 0f; f.invisT = if (bit(6)) 1f else 0f; f.poisonT = if (bit(7)) 1f else 0f
            f.weaponGone = if (bit(8)) 1f else 0f; f.weaponThrown = bit(9); f.minionT = if (bit(10)) 1f else 0f
            f.bot = bit(11)
            f.stun = d.readFloat(); f.swingT = d.readFloat(); f.abilityCd = d.readFloat(); f.look = d.readFloat()
            f.kos = d.readInt(); f.dmgDealt = d.readFloat()
            val plates = d.readLong()
            for (i in f.plates.indices) f.plates[i] = i < 63 && plates and (1L shl i) != 0L
            a.fighters += f
        }
        val byId = a.fighters.associateBy { it.id }

        a.props.clear()
        repeat(d.readInt()) {
            val kind = Prop.Kind.values()[d.readByte().toInt()]
            val p = Prop(kind, d.readFloat(), d.readFloat(), d.readFloat(), d.readFloat())
            p.ang = d.readFloat(); p.flash = d.readFloat()
            a.props += p
        }

        a.projectiles.clear()
        repeat(d.readInt()) {
            val owner = byId[d.readInt()]
            val kind = WeaponType.values()[d.readByte().toInt()]
            val x = d.readFloat(); val y = d.readFloat(); val ang = d.readFloat()
            if (owner != null) {
                val shape = Geo.projectile(owner.loadout.weapon, if (kind == WeaponType.BOMB) 10f else 13f)
                a.projectiles += Projectile(owner, kind, x, y, 0f, 0f, shape).also { it.ang = ang }
            }
        }

        a.debris.clear()
        repeat(d.readInt()) {
            val x = d.readFloat(); val y = d.readFloat(); val ang = d.readFloat()
            val va = Vec(d.readFloat(), d.readFloat()); val vb = Vec(d.readFloat(), d.readFloat())
            val kind = Debris.Kind.values()[d.readByte().toInt()]
            val owner = d.readInt(); val life = d.readFloat()
            a.debris += Debris(x, y, va, vb, kind, owner, 0f, 0f).also { it.ang = ang; it.life = life }
        }

        a.particles.clear()
        repeat(d.readInt()) {
            val x = d.readFloat(); val y = d.readFloat(); val kind = Particle.Kind.values()[d.readByte().toInt()]
            val life = d.readFloat(); val max = d.readFloat()
            a.particles += Particle(x, y, 0f, 0f, kind, life, max)
        }

        a.texts.clear()
        repeat(d.readInt()) {
            val x = d.readFloat(); val y = d.readFloat(); val t = d.readUTF(); val tone = FloatText.Tone.values()[d.readByte().toInt()]
            a.texts += FloatText(x, y, t, tone, d.readFloat())
        }

        a.booms.clear()
        repeat(d.readInt()) {
            val b = Boom(d.readFloat(), d.readFloat(), d.readFloat()); b.t = d.readFloat(); a.booms += b
        }

        a.beams.clear()
        repeat(d.readInt()) {
            a.beams += Beam(d.readFloat(), d.readFloat(), d.readFloat(), d.readFloat(), d.readInt(), d.readFloat())
        }

        val events = List(d.readInt()) {
            when (d.readByte().toInt()) {
                0 -> Event.Hit(d.readInt(), d.readBoolean())
                1 -> Event.Clang
                2 -> Event.Boom(d.readBoolean())
                3 -> Event.Ko
                4 -> Event.Shoot
                5 -> Event.Jump
                6 -> Event.Bounce
                7 -> Event.Break
                8 -> Event.Ability
                else -> Event.Win
            }
        }
        return events to extra
    }
}

/** host side: copy each phone's buttons onto its fighter before every tick */
fun applyPeerInput(a: Arena, f: Fighter, p: Peer) {
    if (!p.connected) { f.bot = true; return }
    f.bot = false
    f.input.left = p.left; f.input.right = p.right; f.input.jump = p.jump
    if (p.swings != p.usedSwings) { p.usedSwings = p.swings; a.swing(f) }
    if (p.abilities != p.usedAbilities) { p.usedAbilities = p.abilities; a.useAbility(f) }
}
