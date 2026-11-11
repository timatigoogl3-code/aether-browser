use std::sync::Arc;
use rustls::pki_types::ServerName;
use rustls::{ClientConfig, ClientConnection, StreamOwned};
use std::net::TcpStream;
use crate::cert_store::RootCertStore;

pub type TlsStream = StreamOwned<ClientConnection, TcpStream>;

pub struct TlsClient {
    config: Arc<ClientConfig>,
}

impl TlsClient {
    pub fn new(cert_store: &RootCertStore) -> Result<Self, String> {
        let _ = rustls::crypto::ring::default_provider().install_default();

        let config = ClientConfig::builder()
            .with_root_certificates(cert_store.inner())
            .with_no_client_auth();

        Ok(Self {
            config: Arc::new(config),
        })
    }

    pub fn connect(&self, host: &str, tcp_stream: TcpStream) -> Result<(TlsStream, Option<String>), String> {
        let server_name = ServerName::try_from(host.to_string())
            .map_err(|e| format!("Invalid DNS name: {e}"))?;

        let conn = ClientConnection::new(self.config.clone(), server_name)
            .map_err(|e| format!("TLS connection setup failed: {e}"))?;

        let stream = StreamOwned::new(conn, tcp_stream);
        Ok((stream, None))
    }
}
