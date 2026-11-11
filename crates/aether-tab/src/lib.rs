//! Aether Zero-RAM Multi-Tab Hibernation & Memory Budget Engine.

pub mod tab;
pub mod manager;

pub use tab::TabState;
pub use manager::TabManager;

pub fn name() -> &'static str {
    "aether-tab"
}
