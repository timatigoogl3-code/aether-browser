use std::fs;
use std::path::{Path, PathBuf};
use crate::tab::TabState;

pub struct TabManager {
    tabs: Vec<TabState>,
    active_index: usize,
    max_active_in_ram: usize,
    storage_dir: PathBuf,
    next_id: u64,
}

impl TabManager {
    pub fn new<P: AsRef<Path>>(storage_dir: P, max_active_in_ram: usize) -> Self {
        let path = storage_dir.as_ref().to_path_buf();
        let _ = fs::create_dir_all(&path);
        Self {
            tabs: Vec::new(),
            active_index: 0,
            max_active_in_ram,
            storage_dir: path,
            next_id: 1,
        }
    }

    pub fn tab_count(&self) -> usize {
        self.tabs.len()
    }

    pub fn active_index(&self) -> usize {
        self.active_index
    }

    pub fn active_tab(&self) -> Option<&TabState> {
        self.tabs.get(self.active_index)
    }

    pub fn active_tab_mut(&mut self) -> Option<&mut TabState> {
        self.tabs.get_mut(self.active_index)
    }

    pub fn count_active_in_ram(&self) -> usize {
        self.tabs.iter().filter(|t| !t.is_hibernated).count()
    }

    pub fn open_tab(&mut self, url: &str, title: &str) -> u64 {
        // Enforce memory budget: hibernate current active tab if limit reached
        if self.count_active_in_ram() >= self.max_active_in_ram {
            self.hibernate_tab(self.active_index);
        }

        let id = self.next_id;
        self.next_id += 1;

        let tab = TabState::new(id, url, title);
        self.tabs.push(tab);
        self.active_index = self.tabs.len() - 1;
        id
    }

    pub fn switch_to(&mut self, index: usize) -> Result<(), String> {
        if index >= self.tabs.len() {
            return Err("Index out of bounds".to_string());
        }
        if index == self.active_index {
            return Ok(());
        }

        // Hibernate previous tab
        self.hibernate_tab(self.active_index);

        // Rehydrate target tab
        self.active_index = index;
        self.rehydrate_tab(index);

        Ok(())
    }

    pub fn close_tab(&mut self, index: usize) -> Result<(), String> {
        if index >= self.tabs.len() {
            return Err("Index out of bounds".to_string());
        }

        let tab = self.tabs.remove(index);
        let file_path = self.tab_file_path(tab.id);
        let _ = fs::remove_file(file_path);

        if !self.tabs.is_empty() {
            self.active_index = self.active_index.min(self.tabs.len() - 1);
            self.rehydrate_tab(self.active_index);
        } else {
            self.active_index = 0;
        }

        Ok(())
    }

    fn tab_file_path(&self, id: u64) -> PathBuf {
        self.storage_dir.join(format!("tab_{id}.json"))
    }

    fn hibernate_tab(&mut self, index: usize) {
        if let Some(tab) = self.tabs.get_mut(index) {
            tab.is_hibernated = true;
            let tab_id = tab.id;
            let file_path = self.storage_dir.join(format!("tab_{tab_id}.json"));
            if let Ok(serialized) = serde_json::to_string(tab) {
                let _ = fs::write(file_path, serialized);
            }
            // Clear in-memory snapshot to release memory
            tab.dom_snapshot = None;
        }
    }

    fn rehydrate_tab(&mut self, index: usize) {
        if let Some(tab) = self.tabs.get_mut(index) {
            let tab_id = tab.id;
            let file_path = self.storage_dir.join(format!("tab_{tab_id}.json"));
            if let Ok(content) = fs::read_to_string(file_path) {
                if let Ok(deserialized) = serde_json::from_str::<TabState>(&content) {
                    *tab = deserialized;
                }
            }
            tab.is_hibernated = false;
        }
    }
}
