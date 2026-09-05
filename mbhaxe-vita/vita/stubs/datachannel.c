// Stand-in for hxDatachannel's native module (see
// https://github.com/RandomityGuy/hxDatachannel/blob/master/cpp/src/datachannel.c)
// on PS Vita.
//
// libdatachannel pulls in a full WebRTC/ICE/SCTP stack (usrsctp, libjuice,
// (m)bedTLS/OpenSSL, plog...) that hasn't been ported to vitasdk. Porting all
// of that is a project of its own and out of scope for getting the base game
// running, so this file implements the exact same HL primitive ABI
// (@:hlNative("datachannel", ...) in hxDatachannel's *.hl.hx files) as
// harmless no-ops instead. It lets marblegame.c link and run on Vita with
// multiplayer simply unavailable, rather than blocking the whole port on a
// WebRTC stack.
//
// None of the Haxe source (Net.hx, Main.hx, ClientConnection.hx) needed to
// change for this: RTC.init() still "succeeds" (inited = true), so the game
// menus behave as normal, but RTCPeerConnection.create_peer_connection always
// hands back a null handle and every other primitive treats a null/any handle
// as a no-op. onDataChannel / onStateChange / onOpen callbacks are simply
// never invoked, so a host/join attempt will sit there rather than crash;
// see VITA_PORT.md for the plan to give this a real backend later (or at
// least a "multiplayer not available" message on the Vita build).
//
// Build this as a static library and link it in place of datachannel.hdll
// when producing the Vita .self — see ../CMakeLists.txt.

#define HL_NAME(n) datachannel_##n

#include <hl.h>

typedef struct _hl_rtc_peerconnection hl_rtc_peerconnection;
typedef struct _hl_rtc_datachannel hl_rtc_datachannel;

HL_PRIM void HL_NAME(initialize)() {
	// no-op: nothing to initialize without a real WebRTC backend
}

HL_PRIM void HL_NAME(finalize)() {
	// no-op
}

HL_PRIM void HL_NAME(process_events)() {
	// no-op: nothing is ever pending since no connection is ever really opened
}

HL_PRIM hl_rtc_peerconnection *HL_NAME(create_peer_connection)(varray *iceServers, vstring *bindAddress, int portBegin, int portEnd, int mtu,
	int maxMessageSize) {
	// Real backend would open a socket and start ICE gathering here. We just
	// hand back a null handle; every other primitive below treats that as
	// "there's nothing to do."
	return NULL;
}

HL_PRIM void HL_NAME(close_peer_connection)(hl_rtc_peerconnection *pc) {
	// no-op
}

HL_PRIM void HL_NAME(set_peer_connection_callbacks)(hl_rtc_peerconnection *pc, vclosure *descCb, vclosure *candidateCb, vclosure *stateCb,
	vclosure *gatheringStateCb) {
	// no-op: we never fire these callbacks since no real connection exists
}

HL_PRIM void HL_NAME(set_remote_description)(hl_rtc_peerconnection *pc, vbyte *desc, vstring *type) {
	// no-op
}

HL_PRIM void HL_NAME(add_remote_candidate)(hl_rtc_peerconnection *pc, vbyte *candidate) {
	// no-op
}

HL_PRIM hl_rtc_datachannel *HL_NAME(create_datachannel)(hl_rtc_peerconnection *pc, vstring *name) {
	return NULL;
}

HL_PRIM hl_rtc_datachannel *HL_NAME(create_datachannel_ex)(hl_rtc_peerconnection *pc, vstring *name, bool unordered, int maxRetransmits,
	int maxLifetime) {
	return NULL;
}

HL_PRIM void HL_NAME(set_peerconnection_datachannel_cb)(hl_rtc_peerconnection *pc, vclosure *openCb) {
	// no-op: never invoked, no remote can ever open a channel to us
}

HL_PRIM void HL_NAME(set_datachannel_callbacks)(hl_rtc_datachannel *dc, vclosure *openCb, vclosure *closeCb, vclosure *errorCb, vclosure *msgCb,
	vclosure *bufferLowCb) {
	// no-op
}

HL_PRIM void HL_NAME(datachannel_send_message)(hl_rtc_datachannel *dc, vbyte *bytes, int len) {
	// no-op: Net.hx only sends once state == Open, which never happens here
}

HL_PRIM vdynobj *HL_NAME(get_datachannel_reliability)(hl_rtc_datachannel *dc) {
	vdynamic *obj = (vdynamic *)hl_alloc_dynobj();
	hl_dyn_seti(obj, hl_hash_utf8("unordered"), &hlt_bool, false);
	hl_dyn_seti(obj, hl_hash_utf8("maxRetransmits"), &hlt_i32, 0);
	hl_dyn_seti(obj, hl_hash_utf8("maxLifetime"), &hlt_i32, 0);
	return (vdynobj *)obj;
}

HL_PRIM vbyte *HL_NAME(get_local_address)(hl_rtc_peerconnection *pc) {
	return NULL;
}

HL_PRIM vbyte *HL_NAME(get_remote_address)(hl_rtc_peerconnection *pc) {
	return NULL;
}

HL_PRIM int HL_NAME(get_buffered_amount)(hl_rtc_datachannel *dc) {
	return 0;
}

HL_PRIM void HL_NAME(set_buffered_amount_low_threshold)(hl_rtc_datachannel *dc, int amt) {
	// no-op
}

#define _TPC _ABSTRACT(hl_rtc_peerconnection)
#define _TDC _ABSTRACT(hl_rtc_datachannel)

DEFINE_PRIM(_VOID, initialize, _NO_ARG);
DEFINE_PRIM(_VOID, finalize, _NO_ARG);
DEFINE_PRIM(_VOID, process_events, _NO_ARG);
DEFINE_PRIM(_TPC, create_peer_connection, _ARR _STRING _I32 _I32 _I32 _I32);
DEFINE_PRIM(_VOID, close_peer_connection, _TPC);
DEFINE_PRIM(_VOID, set_peer_connection_callbacks, _TPC _FUN(_VOID, _BYTES _BYTES) _FUN(_VOID, _BYTES) _FUN(_VOID, _I32) _FUN(_VOID, _I32));
DEFINE_PRIM(_VOID, set_remote_description, _TPC _BYTES _STRING);
DEFINE_PRIM(_VOID, add_remote_candidate, _TPC _BYTES);
DEFINE_PRIM(_TDC, create_datachannel, _TPC _STRING);
DEFINE_PRIM(_TDC, create_datachannel_ex, _TPC _STRING _BOOL _I32 _I32);
DEFINE_PRIM(_VOID, set_peerconnection_datachannel_cb, _TPC _FUN(_VOID, _TDC _BYTES));
DEFINE_PRIM(_VOID, set_datachannel_callbacks, _TDC _FUN(_VOID, _BYTES) _FUN(_VOID, _NO_ARG) _FUN(_VOID, _BYTES) _FUN(_VOID, _BYTES _I32) _FUN(_VOID, _NO_ARG));
DEFINE_PRIM(_VOID, datachannel_send_message, _TDC _BYTES _I32);
DEFINE_PRIM(_DYN, get_datachannel_reliability, _TDC);
DEFINE_PRIM(_BYTES, get_local_address, _TPC);
DEFINE_PRIM(_BYTES, get_remote_address, _TPC);
DEFINE_PRIM(_I32, get_buffered_amount, _TDC);
DEFINE_PRIM(_VOID, set_buffered_amount_low_threshold, _TDC _I32);
