use aether_tab::TabManager;

#[test]
fn test_zero_ram_multi_tab_budget() {
    let temp_dir = std::env::temp_dir().join("aether_tab_test");
    let mut manager = TabManager::new(&temp_dir, 1); // Only 1 tab allowed active in RAM!

    // Open 15 tabs sequentially
    for i in 1..=15 {
        let url = format!("https://example.com/page{i}");
        let title = format!("Page {i}");
        let _id = manager.open_tab(&url, &title);
        assert_eq!(manager.count_active_in_ram(), 1, "Only 1 tab must remain active in RAM");
    }

    assert_eq!(manager.tab_count(), 15);
    assert_eq!(manager.count_active_in_ram(), 1);

    // Switch between tabs and verify fast rehydration from disk
    manager.switch_to(0).expect("Switch to tab 0");
    assert_eq!(manager.count_active_in_ram(), 1);
    assert_eq!(manager.active_tab().unwrap().url, "https://example.com/page1");

    manager.switch_to(7).expect("Switch to tab 7");
    assert_eq!(manager.count_active_in_ram(), 1);
    assert_eq!(manager.active_tab().unwrap().url, "https://example.com/page8");

    // Close tab and verify auto-rehydration of neighbor
    manager.close_tab(7).expect("Close tab");
    assert_eq!(manager.tab_count(), 14);
    assert_eq!(manager.count_active_in_ram(), 1);

    let _ = std::fs::remove_dir_all(temp_dir);
}
