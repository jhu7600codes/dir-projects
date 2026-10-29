#!/bin/bash
# patch a go toolchain (GOROOT) so GOOS=ios binaries load on ios 10.
#
# why: since go 1.18, crypto/x509 on darwin/ios verifies certs through
# Security.framework (SecTrustEvaluateWithError = ios 12+,
# SecTrustCopyCertificateChain = ios 15+). those are hard imports, so dyld on
# ios 10 refuses to start the binary at all. this patch makes GOOS=ios use the
# plain "read a pem bundle from disk" path that linux/bsd use, and drops the
# Security.framework code for ios. GOOS=darwin is left alone semantically.
set -euo pipefail
GOROOT_DIR="${1:?usage: patch-go-ios10.sh GOROOT}"
X="$GOROOT_DIR/src/crypto/x509"
[ -f "$X/root_darwin.go" ] || { echo "no $X/root_darwin.go"; exit 1; }
if grep -q 'vlessproxy-ios10-patch' "$X/root_darwin.go"; then echo "already patched"; exit 0; fi

# 1. ios must never take the "platform verifier" branches.
for f in "$X"/*.go; do
  case "$f" in *_test.go) continue;; esac
  sed -i.bak -e 's/ || runtime\.GOOS == "ios"//g' -e 's/runtime\.GOOS == "ios" || //g' "$f"
  rm -f "$f.bak"
done

# 2. replace root_darwin.go (compiled for both darwin and ios) with a stub that
#    has no Security.framework references.
has_generic_loader=0
grep -q '^func loadSystemRoots' "$X/root.go" 2>/dev/null && has_generic_loader=1

{
cat <<'EOF'
// vlessproxy-ios10-patch: Security.framework verifier removed so the binary
// loads on ios 10. roots come from a pem bundle on disk.

package x509

import "errors"

// Possible certificate files; stop after finding one.
var certFiles = []string{
	"/usr/share/vlessproxy/cacert.pem",
	"/etc/ssl/cert.pem",
	"/usr/lib/ssl/cert.pem",
	"/etc/ssl/certs/ca-certificates.crt",
}

// Possible directories with certificate files; all will be read.
var certDirectories = []string{"/etc/ssl/certs"}

func (c *Certificate) systemVerify(opts *VerifyOptions) (chains [][]*Certificate, err error) {
	return nil, errors.New("x509: platform verifier not available in this build")
}
EOF
if [ "$has_generic_loader" = 0 ]; then
cat <<'EOF'

func loadSystemRoots() (*CertPool, error) {
	roots := NewCertPool()
	files := certFiles
	if f := os.Getenv("SSL_CERT_FILE"); f != "" {
		files = []string{f}
	}
	var firstErr error
	for _, file := range files {
		data, err := os.ReadFile(file)
		if err == nil {
			roots.AppendCertsFromPEM(data)
			return roots, nil
		}
		if firstErr == nil && !os.IsNotExist(err) {
			firstErr = err
		}
	}
	return roots, firstErr
}
EOF
fi
} > "$X/root_darwin.go.new"

if [ "$has_generic_loader" = 0 ]; then
  sed -i.bak 's/^import "errors"$/import (\n\t"errors"\n\t"os"\n)/' "$X/root_darwin.go.new"
  rm -f "$X/root_darwin.go.new.bak"
fi
mv "$X/root_darwin.go.new" "$X/root_darwin.go"

echo "patched $X"
grep -n 'GOOS == "ios"' "$X"/*.go | grep -v _test.go || true
