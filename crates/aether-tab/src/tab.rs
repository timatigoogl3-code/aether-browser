use serde::{Deserialize, Serialize};

#[derive(Debug, Clone, Serialize, Deserialize, PartialEq)]
pub struct TabState {
    pub id: u64,
    pub url: String,
    pub title: String,
    pub scroll_y: f32,
    pub is_hibernated: bool,
    pub dom_snapshot: Option<String>,
}

impl TabState {
    pub fn new(id: u64, url: &str, title: &str) -> Self {
        Self {
            id,
            url: url.to_string(),
            title: title.to_string(),
            scroll_y: 0.0,
            is_hibernated: false,
            dom_snapshot: None,
        }
    }
}
