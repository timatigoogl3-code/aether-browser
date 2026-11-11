use std::collections::HashMap;
use std::io::{Read, Write};
use std::net::{TcpStream, ToSocketAddrs};
use std::time::Duration;
use flate2::read::GzDecoder;

use crate::cert_store::RootCertStore;
use crate::tls_client::TlsClient;

#[derive(Debug, Clone)]
pub struct ParsedUrl {
    pub is_https: bool,
    pub host: String,
    pub port: u16,
    pub path: String,
}

#[derive(Debug, Clone)]
pub struct HttpResponse {
    pub status_code: u16,
    pub headers: HashMap<String, String>,
    pub body: Vec<u8>,
    pub tls_version: Option<String>,
}

impl HttpResponse {
    pub fn body_as_string(&self) -> String {
        String::from_utf8_lossy(&self.body).to_string()
    }
}

pub struct HttpClient {
    cert_store: RootCertStore,
    tls_client: Option<TlsClient>,
}

impl HttpClient {
    pub fn new() -> Self {
        let cert_store = RootCertStore::load_bundled();
        let tls_client = TlsClient::new(&cert_store).ok();
        Self {
            cert_store,
            tls_client,
        }
    }

    pub fn parse_url(&self, url: &str) -> Result<ParsedUrl, String> {
        let (is_https, rest) = if let Some(stripped) = url.strip_prefix("https://") {
            (true, stripped)
        } else if let Some(stripped) = url.strip_prefix("http://") {
            (false, stripped)
        } else {
            return Err("URL must start with http:// or https://".to_string());
        };

        let (host_port, path) = match rest.find('/') {
            Some(idx) => (&rest[..idx], &rest[idx..]),
            None => (rest, "/"),
        };

        let (host, port) = if let Some((h, p)) = host_port.split_once(':') {
            let port_num = p.parse::<u16>().map_err(|e| format!("Invalid port: {e}"))?;
            (h.to_string(), port_num)
        } else {
            let default_port = if is_https { 443 } else { 80 };
            (host_port.to_string(), default_port)
        };

        Ok(ParsedUrl {
            is_https,
            host,
            port,
            path: path.to_string(),
        })
    }

    pub fn get(&self, url_str: &str) -> Result<HttpResponse, String> {
        let parsed = self.parse_url(url_str)?;
        let addr = format!("{}:{}", parsed.host, parsed.port);
        let socket_addrs: Vec<_> = addr.to_socket_addrs()
            .map_err(|e| format!("DNS resolution error for {}: {e}", parsed.host))?
            .collect();

        if socket_addrs.is_empty() {
            return Err(format!("Could not resolve host: {}", parsed.host));
        }

        let tcp = TcpStream::connect_timeout(&socket_addrs[0], Duration::from_secs(10))
            .map_err(|e| format!("TCP connection failed to {}: {e}", addr))?;

        let request = format!(
            "GET {} HTTP/1.1\r\n\
             Host: {}\r\n\
             User-Agent: AetherBrowser/0.1 (Android 4.4.2; Lenovo A536; Mobile)\r\n\
             Accept: text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8\r\n\
             Accept-Encoding: gzip\r\n\
             Connection: close\r\n\r\n",
            parsed.path, parsed.host
        );

        let mut raw_data = Vec::new();
        let tls_ver;

        if parsed.is_https {
            let tls = self.tls_client.as_ref().ok_or("TLS client not initialized")?;
            let (mut stream, _) = tls.connect(&parsed.host, tcp)?;
            stream.write_all(request.as_bytes())
                .map_err(|e| format!("Write failed: {e}"))?;
            stream.flush()
                .map_err(|e| format!("Flush failed: {e}"))?;
            stream.read_to_end(&mut raw_data)
                .map_err(|e| format!("Read failed: {e}"))?;
            tls_ver = stream.conn.protocol_version().map(|v| format!("{:?}", v));
        } else {
            let mut stream = tcp;
            stream.write_all(request.as_bytes())
                .map_err(|e| format!("Write failed: {e}"))?;
            stream.flush()
                .map_err(|e| format!("Flush failed: {e}"))?;
            stream.read_to_end(&mut raw_data)
                .map_err(|e| format!("Read failed: {e}"))?;
            tls_ver = None;
        }

        self.parse_http_response(raw_data, tls_ver)
    }

    fn parse_http_response(&self, raw: Vec<u8>, tls_version: Option<String>) -> Result<HttpResponse, String> {
        // Find header-body boundary \r\n\r\n
        let header_end = raw.windows(4).position(|w| w == b"\r\n\r\n")
            .ok_or("Malformed HTTP response: no CRLF CRLF found")?;

        let header_bytes = &raw[..header_end];
        let body_bytes = &raw[header_end + 4..];

        let header_str = String::from_utf8_lossy(header_bytes);
        let mut lines = header_str.lines();

        let status_line = lines.next().ok_or("Empty response")?;
        let status_parts: Vec<&str> = status_line.split_whitespace().collect();
        if status_parts.len() < 2 {
            return Err(format!("Invalid status line: {status_line}"));
        }
        let status_code: u16 = status_parts[1].parse()
            .map_err(|e| format!("Invalid status code {}: {e}", status_parts[1]))?;

        let mut headers = HashMap::new();
        for line in lines {
            if let Some((k, v)) = line.split_once(':') {
                headers.insert(k.trim().to_lowercase(), v.trim().to_string());
            }
        }

        let is_gzip = headers.get("content-encoding")
            .map(|v| v.to_lowercase().contains("gzip"))
            .unwrap_or(false);

        let final_body = if is_gzip {
            let mut decoder = GzDecoder::new(body_bytes);
            let mut decompressed = Vec::new();
            decoder.read_to_end(&mut decompressed)
                .map_err(|e| format!("Gzip decompression failed: {e}"))?;
            decompressed
        } else {
            body_bytes.to_vec()
        };

        Ok(HttpResponse {
            status_code,
            headers,
            body: final_body,
            tls_version,
        })
    }
}
