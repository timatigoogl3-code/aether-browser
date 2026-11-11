use std::sync::Arc;
use rustls::RootCertStore as RustlsRootStore;

/// Independent Mozilla NSS Root CA Certificate Store
#[derive(Clone)]
pub struct RootCertStore {
    inner: Arc<RustlsRootStore>,
    count: usize,
}

impl RootCertStore {
    /// Load the bundled Mozilla NSS Root Certificates.
    /// This completely bypasses the host Android 4.4 expired CA store.
    pub fn load_bundled() -> Self {
        let mut root_store = RustlsRootStore::empty();
        let mut count = 0;
        for cert in webpki_roots::TLS_SERVER_ROOTS.iter() {
            root_store.roots.push(cert.clone());
            count += 1;
        }

        Self {
            inner: Arc::new(root_store),
            count,
        }
    }

    pub fn inner(&self) -> Arc<RustlsRootStore> {
        self.inner.clone()
    }

    pub fn count(&self) -> usize {
        self.count
    }
}
