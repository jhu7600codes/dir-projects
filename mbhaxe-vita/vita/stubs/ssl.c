// Stand-in for hashlink's libs/ssl/ssl.c (an mbedTLS binding) on PS Vita.
//
// mbedTLS itself isn't ported here (out of scope alongside the rest of
// networking - see vita/stubs/datachannel.c and vita/stubs/uv.c), so this
// implements the same HL primitive ABI as harmless no-ops/failures: every
// handshake fails immediately rather than hanging or crashing, and cert
// loading always returns null (as if the file/path/default trust store was
// never found). Real TLS on this platform would need porting mbedTLS to
// vitasdk (which does have community mbedtls packages - unlike libdatachannel,
// this one's plausible if TLS ends up needed for e.g. a matchmaking HTTP
// call), then swapping this stub back out for the real ssl.c.
//
// Build this as a static library and link it in place of ssl.hdll.

#define HL_NAME(n) ssl_##n

#include <hl.h>

typedef struct _hl_socket hl_socket;
typedef struct _ssl_context ssl_context;
typedef struct _ssl_config ssl_config;
typedef struct _hl_ssl_cert hl_ssl_cert;
typedef struct _hl_ssl_pkey hl_ssl_pkey;

#define _SOCK _ABSTRACT(hl_socket)
#define TSSL _ABSTRACT(mbedtls_ssl_context)
#define TCONF _ABSTRACT(mbedtls_ssl_config)
#define TCERT _ABSTRACT(hl_ssl_cert)
#define TPKEY _ABSTRACT(hl_ssl_pkey)

HL_PRIM void HL_NAME(ssl_init)() {
	// no-op
}

HL_PRIM ssl_context *HL_NAME(ssl_new)(ssl_config *config) {
	return NULL;
}

HL_PRIM void HL_NAME(ssl_close)(ssl_context *ssl) {
	// no-op
}

HL_PRIM int HL_NAME(ssl_handshake)(ssl_context *ssl) {
	return -1; // fail immediately rather than hang
}

HL_PRIM void HL_NAME(ssl_set_bio)(ssl_context *ssl, vdynamic *ctx) {
	// no-op
}

HL_PRIM void HL_NAME(ssl_set_socket)(ssl_context *ssl, hl_socket *socket) {
	// no-op
}

HL_PRIM void HL_NAME(ssl_set_hostname)(ssl_context *ssl, vbyte *hostname) {
	// no-op
}

HL_PRIM hl_ssl_cert *HL_NAME(ssl_get_peer_certificate)(ssl_context *ssl) {
	return NULL;
}

HL_PRIM int HL_NAME(ssl_send_char)(ssl_context *ssl, int c) {
	return -1;
}

HL_PRIM int HL_NAME(ssl_send)(ssl_context *ssl, vbyte *buf, int pos, int len) {
	return -1;
}

HL_PRIM int HL_NAME(ssl_recv_char)(ssl_context *ssl) {
	return -1;
}

HL_PRIM int HL_NAME(ssl_recv)(ssl_context *ssl, vbyte *buf, int pos, int len) {
	return -1;
}

HL_PRIM ssl_config *HL_NAME(conf_new)(bool server) {
	return NULL;
}

HL_PRIM void HL_NAME(conf_close)(ssl_config *conf) {
	// no-op
}

HL_PRIM void HL_NAME(conf_set_ca)(ssl_config *conf, hl_ssl_cert *cert) {
	// no-op
}

HL_PRIM void HL_NAME(conf_set_verify)(ssl_config *conf, int mode) {
	// no-op
}

HL_PRIM void HL_NAME(conf_set_cert)(ssl_config *conf, hl_ssl_cert *cert, hl_ssl_pkey *key) {
	// no-op
}

HL_PRIM void HL_NAME(conf_set_servername_callback)(ssl_config *conf, vclosure *cb) {
	// no-op: never called back, no server-side TLS here anyway
}

HL_PRIM hl_ssl_cert *HL_NAME(cert_load_file)(vbyte *file) {
	return NULL;
}

HL_PRIM hl_ssl_cert *HL_NAME(cert_load_path)(vbyte *path) {
	return NULL;
}

HL_PRIM hl_ssl_cert *HL_NAME(cert_load_defaults)() {
	return NULL;
}

HL_PRIM vbyte *HL_NAME(cert_get_subject)(hl_ssl_cert *cert, vbyte *objname) {
	return NULL;
}

HL_PRIM vbyte *HL_NAME(cert_get_issuer)(hl_ssl_cert *cert, vbyte *objname) {
	return NULL;
}

HL_PRIM varray *HL_NAME(cert_get_altnames)(hl_ssl_cert *cert) {
	return hl_alloc_array(&hlt_bytes, 0);
}

HL_PRIM varray *HL_NAME(cert_get_notbefore)(hl_ssl_cert *cert) {
	return NULL;
}

HL_PRIM varray *HL_NAME(cert_get_notafter)(hl_ssl_cert *cert) {
	return NULL;
}

HL_PRIM hl_ssl_cert *HL_NAME(cert_get_next)(hl_ssl_cert *cert) {
	return NULL;
}

HL_PRIM hl_ssl_cert *HL_NAME(cert_add_pem)(hl_ssl_cert *cert, vbyte *data) {
	return NULL;
}

HL_PRIM hl_ssl_cert *HL_NAME(cert_add_der)(hl_ssl_cert *cert, vbyte *data, int len) {
	return NULL;
}

HL_PRIM hl_ssl_pkey *HL_NAME(key_from_der)(vbyte *data, int len, bool pub) {
	return NULL;
}

HL_PRIM hl_ssl_pkey *HL_NAME(key_from_pem)(vbyte *data, bool pub, vbyte *pass) {
	return NULL;
}

HL_PRIM vbyte *HL_NAME(dgst_make)(vbyte *data, int len, vbyte *alg, int *size) {
	*size = 0;
	return NULL;
}

HL_PRIM vbyte *HL_NAME(dgst_sign)(vbyte *data, int len, hl_ssl_pkey *key, vbyte *alg, int *size) {
	*size = 0;
	return NULL;
}

HL_PRIM bool HL_NAME(dgst_verify)(vbyte *data, int dlen, vbyte *sign, int slen, hl_ssl_pkey *key, vbyte *alg) {
	return false;
}

DEFINE_PRIM(_VOID, ssl_init, _NO_ARG);
DEFINE_PRIM(TSSL, ssl_new, TCONF);
DEFINE_PRIM(_VOID, ssl_close, TSSL);
DEFINE_PRIM(_I32, ssl_handshake, TSSL);
DEFINE_PRIM(_VOID, ssl_set_bio, TSSL _DYN);
DEFINE_PRIM(_VOID, ssl_set_socket, TSSL _SOCK);
DEFINE_PRIM(_VOID, ssl_set_hostname, TSSL _BYTES);
DEFINE_PRIM(TCERT, ssl_get_peer_certificate, TSSL);
DEFINE_PRIM(_I32, ssl_send_char, TSSL _I32);
DEFINE_PRIM(_I32, ssl_send, TSSL _BYTES _I32 _I32);
DEFINE_PRIM(_I32, ssl_recv_char, TSSL);
DEFINE_PRIM(_I32, ssl_recv, TSSL _BYTES _I32 _I32);
DEFINE_PRIM(TCONF, conf_new, _BOOL);
DEFINE_PRIM(_VOID, conf_close, TCONF);
DEFINE_PRIM(_VOID, conf_set_ca, TCONF TCERT);
DEFINE_PRIM(_VOID, conf_set_verify, TCONF _I32);
DEFINE_PRIM(_VOID, conf_set_cert, TCONF TCERT TPKEY);
DEFINE_PRIM(_VOID, conf_set_servername_callback, TCONF _FUN(_OBJ(TCERT TPKEY), _BYTES));
DEFINE_PRIM(TCERT, cert_load_defaults, _NO_ARG);
DEFINE_PRIM(TCERT, cert_load_file, _BYTES);
DEFINE_PRIM(TCERT, cert_load_path, _BYTES);
DEFINE_PRIM(_BYTES, cert_get_subject, TCERT _BYTES);
DEFINE_PRIM(_BYTES, cert_get_issuer, TCERT _BYTES);
DEFINE_PRIM(_ARR, cert_get_altnames, TCERT);
DEFINE_PRIM(_ARR, cert_get_notbefore, TCERT);
DEFINE_PRIM(_ARR, cert_get_notafter, TCERT);
DEFINE_PRIM(TCERT, cert_get_next, TCERT);
DEFINE_PRIM(TCERT, cert_add_pem, TCERT _BYTES);
DEFINE_PRIM(TCERT, cert_add_der, TCERT _BYTES _I32);
DEFINE_PRIM(TPKEY, key_from_der, _BYTES _I32 _BOOL);
DEFINE_PRIM(TPKEY, key_from_pem, _BYTES _BOOL _BYTES);
DEFINE_PRIM(_BYTES, dgst_make, _BYTES _I32 _BYTES _REF(_I32));
DEFINE_PRIM(_BYTES, dgst_sign, _BYTES _I32 TPKEY _BYTES _REF(_I32));
DEFINE_PRIM(_BOOL, dgst_verify, _BYTES _I32 _BYTES _I32 TPKEY _BYTES);
