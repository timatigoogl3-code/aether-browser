#[test]
fn test_workspace_crates_exist() {
    assert!(aether_core::version().contains("0.1.0"));
    assert_eq!(aether_net::name(), "aether-net");
    assert_eq!(aether_dom::name(), "aether-dom");
    assert_eq!(aether_js::name(), "aether-js");
    assert_eq!(aether_layout::name(), "aether-layout");
    assert_eq!(aether_render::name(), "aether-render");
    assert_eq!(aether_tab::name(), "aether-tab");
}
