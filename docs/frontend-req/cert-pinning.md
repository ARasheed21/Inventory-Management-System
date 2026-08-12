# Certificate pinning helper

This document shows how to extract the TLS server public key hash (SHA-256, base64) for use in certificate pinning on clients.

Replace `example.com:443` with your production host:port.

1. Using OpenSSL (Linux/macOS/WSL):

```bash
openssl s_client -connect example.com:443 -servername example.com -showcerts \
  | openssl x509 -pubkey -noout \
  | openssl pkey -pubin -outform DER \
  | openssl dgst -sha256 -binary \
  | openssl base64
```

This prints the base64-encoded SHA-256 digest of the public key, which you can use as the pin value.

2. Example usage in frontend (conceptual):

- `pin = "sha256/BASE64_DIGEST=="`
- Use the platform's pinning API (e.g., `OkHttp` certificate pinner or native TLS APIs).

Notes:
- Do NOT pin to the leaf certificate if you plan to rotate certificates; pin to the public key instead.
- Keep backup pins for certificate rotation.
- For pinned deployments behind CDNs, ensure the CDN certificate key is considered.
