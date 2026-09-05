// Stand-in for hashlink's libs/uv/uv.c (a binding to real libuv) on PS Vita.
//
// libuv itself hasn't been ported to vitasdk, and everything this module is
// actually used for here is networking-adjacent (hl.uv.Fs file-watching for
// asset hot-reload - already disabled, see ResourceLoader.hx's
// `hxd.res.Resource.LIVE_UPDATE = false` - and the TCP wrapper colyseus-
// websocket/Net.hx could reach for) which is already out of scope for this
// build the same way vita/stubs/datachannel.c defers multiplayer. Same
// approach here: implement the exact HL primitive ABI as harmless no-ops
// rather than porting libuv, so the game links and a file-watch or connect
// attempt just never fires its callback instead of crashing.
//
// Build this as a static library and link it in place of uv.hdll.

#define HL_NAME(n) uv_##n

#include <hl.h>

typedef struct _uv_loop uv_loop;
typedef struct _uv_handle uv_handle;

#define _LOOP _ABSTRACT(uv_loop)
#define _HANDLE _ABSTRACT(uv_handle)
#define _TCP _HANDLE
#define _FS _HANDLE
#define _CALLB _FUN(_VOID, _NO_ARG)

HL_PRIM void HL_NAME(close_handle)(uv_handle *h, vclosure *c) {
	// no-op: nothing was ever really opened
}

HL_PRIM bool HL_NAME(stream_write)(uv_handle *s, vbyte *b, int size, vclosure *c) {
	return false;
}

HL_PRIM bool HL_NAME(stream_read_start)(uv_handle *s, vclosure *c) {
	return false;
}

HL_PRIM void HL_NAME(stream_read_stop)(uv_handle *s) {
	// no-op
}

HL_PRIM bool HL_NAME(stream_listen)(uv_handle *s, int count, vclosure *c) {
	return false;
}

HL_PRIM uv_handle *HL_NAME(tcp_init_wrap)(uv_loop *loop) {
	return NULL;
}

HL_PRIM uv_handle *HL_NAME(tcp_connect_wrap)(uv_handle *t, int host, int port, vclosure *c) {
	return NULL;
}

HL_PRIM bool HL_NAME(tcp_bind_wrap)(uv_handle *t, int host, int port) {
	return false;
}

HL_PRIM uv_handle *HL_NAME(tcp_accept_wrap)(uv_handle *t) {
	return NULL;
}

HL_PRIM void HL_NAME(tcp_nodelay_wrap)(uv_handle *t, bool enable) {
	// no-op
}

HL_PRIM uv_handle *HL_NAME(fs_start_wrap)(uv_loop *loop, vclosure *cb, char *path) {
	// never fires cb - matches LIVE_UPDATE=false already disabling this path
	return NULL;
}

HL_PRIM bool HL_NAME(fs_stop_wrap)(uv_handle *handle) {
	return true;
}

HL_PRIM uv_loop *HL_NAME(default_loop)() {
	// non-NULL so callers that just check "do I have a loop" are happy;
	// never dereferenced since nothing above ever hands back a real handle
	static int fake_loop;
	return (uv_loop *)&fake_loop;
}

HL_PRIM int HL_NAME(loop_close)(uv_loop *loop) {
	return 0;
}

HL_PRIM int HL_NAME(run)(uv_loop *loop, int mode) {
	// nothing pending, ever
	return 0;
}

HL_PRIM int HL_NAME(loop_alive)(uv_loop *loop) {
	return 0;
}

HL_PRIM void HL_NAME(stop)(uv_loop *loop) {
	// no-op
}

HL_PRIM vbyte *HL_NAME(strerror)(int err) {
	return (vbyte *)USTR("uv not available on this platform");
}

DEFINE_PRIM(_VOID, close_handle, _HANDLE _CALLB);
DEFINE_PRIM(_BOOL, stream_write, _HANDLE _BYTES _I32 _FUN(_VOID, _BOOL));
DEFINE_PRIM(_BOOL, stream_read_start, _HANDLE _FUN(_VOID, _BYTES _I32));
DEFINE_PRIM(_VOID, stream_read_stop, _HANDLE);
DEFINE_PRIM(_BOOL, stream_listen, _HANDLE _I32 _CALLB);
DEFINE_PRIM(_TCP, tcp_init_wrap, _LOOP);
DEFINE_PRIM(_HANDLE, tcp_connect_wrap, _TCP _I32 _I32 _FUN(_VOID, _BOOL));
DEFINE_PRIM(_BOOL, tcp_bind_wrap, _TCP _I32 _I32);
DEFINE_PRIM(_HANDLE, tcp_accept_wrap, _HANDLE);
DEFINE_PRIM(_VOID, tcp_nodelay_wrap, _TCP _BOOL);
DEFINE_PRIM(_FS, fs_start_wrap, _LOOP _FUN(_VOID, _I32) _BYTES);
DEFINE_PRIM(_BOOL, fs_stop_wrap, _FS);
DEFINE_PRIM(_LOOP, default_loop, _NO_ARG);
DEFINE_PRIM(_I32, loop_close, _LOOP);
DEFINE_PRIM(_I32, run, _LOOP _I32);
DEFINE_PRIM(_I32, loop_alive, _LOOP);
DEFINE_PRIM(_VOID, stop, _LOOP);
DEFINE_PRIM(_BYTES, strerror, _I32);
