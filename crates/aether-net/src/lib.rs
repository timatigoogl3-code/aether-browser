//! Aether Network & TLS 1.3 Engine with standalone Mozilla NSS Root CA store.

pub mod cert_store;
pub mod tls_client;
pub mod http_client;

pub use cert_store::RootCertStore;
pub use tls_client::TlsClient;
pub use http_client::{HttpClient, HttpResponse, ParsedUrl};

pub fn name() -> &'static str {
    "aether-net"
}
