package com.aether.browser;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.mozilla.geckoview.GeckoSession;
import org.mozilla.geckoview.GeckoView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "Aether/MainActivity";
    private static final String SEARCH_ENGINE_URL = "https://duckduckgo.com/?q=";
    private static final String HOMEPAGE_URL = "https://start.duckduckgo.com";

    private GeckoView mGeckoView;
    private EditText mUrlEditText;
    private ProgressBar mProgressBar;
    private TextView mTabCountBtn;
    private TextView mNewTabBtn;
    private ListView mTabDrawer;

    private TabManager mTabManager;
    private ArrayAdapter<String> mTabAdapter;
    private final List<String> mTabTitles = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mGeckoView = findViewById(R.id.geckoView);
        mUrlEditText = findViewById(R.id.urlEditText);
        mProgressBar = findViewById(R.id.pageProgressBar);
        mTabCountBtn = findViewById(R.id.tabCountBtn);
        mNewTabBtn = findViewById(R.id.newTabBtn);
        mTabDrawer = findViewById(R.id.tabDrawer);

        mTabManager = new TabManager(this, mGeckoView);

        setupOmnibox();
        setupTabControls();

        // Check if opened via intent
        String startUrl = HOMEPAGE_URL;
        Intent intent = getIntent();
        if (intent != null && Intent.ACTION_VIEW.equals(intent.getAction()) && intent.getData() != null) {
            startUrl = intent.getData().toString();
        }

        mTabManager.initFirstTab(startUrl);
        attachSessionDelegates(mTabManager.getActiveSession());
        updateTabUI();
    }

    private void setupOmnibox() {
        mUrlEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_GO ||
                (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN)) {
                String input = mUrlEditText.getText().toString().trim();
                if (!TextUtils.isEmpty(input)) {
                    navigateTo(input);
                    hideKeyboard();
                }
                return true;
            }
            return false;
        });
    }

    private void setupTabControls() {
        mTabAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, mTabTitles);
        mTabDrawer.setAdapter(mTabAdapter);

        mTabDrawer.setOnItemClickListener((parent, view, position, id) -> {
            mTabManager.switchToTab(position);
            attachSessionDelegates(mTabManager.getActiveSession());
            mTabDrawer.setVisibility(View.GONE);
            updateTabUI();
        });

        mTabCountBtn.setOnClickListener(v -> {
            if (mTabDrawer.getVisibility() == View.VISIBLE) {
                mTabDrawer.setVisibility(View.GONE);
            } else {
                updateTabDrawerList();
                mTabDrawer.setVisibility(View.VISIBLE);
            }
        });

        mNewTabBtn.setOnClickListener(v -> {
            mTabManager.openNewTab(HOMEPAGE_URL);
            attachSessionDelegates(mTabManager.getActiveSession());
            mTabDrawer.setVisibility(View.GONE);
            updateTabUI();
        });
    }

    private void navigateTo(String query) {
        String url;
        if (query.startsWith("http://") || query.startsWith("https://")) {
            url = query;
        } else if (query.contains(".") && !query.contains(" ")) {
            url = "https://" + query;
        } else {
            url = SEARCH_ENGINE_URL + query;
        }

        TabManager.TabItem activeTab = mTabManager.getActiveTab();
        if (activeTab != null) {
            activeTab.url = url;
        }
        GeckoSession session = mTabManager.getActiveSession();
        if (session != null) {
            session.loadUri(url);
        }
    }

    private void attachSessionDelegates(GeckoSession session) {
        if (session == null) return;

        session.setProgressDelegate(new GeckoSession.ProgressDelegate() {
            @Override
            public void onProgressChange(GeckoSession s, int progress) {
                mProgressBar.setProgress(progress);
                if (progress >= 100) {
                    mProgressBar.setVisibility(View.GONE);
                } else {
                    mProgressBar.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onPageStart(GeckoSession s, String url) {
                mProgressBar.setVisibility(View.VISIBLE);
                mProgressBar.setProgress(10);
                mUrlEditText.setText(url);
                TabManager.TabItem active = mTabManager.getActiveTab();
                if (active != null) active.url = url;
            }

            @Override
            public void onPageStop(GeckoSession s, boolean success) {
                mProgressBar.setVisibility(View.GONE);
            }
        });

        session.setNavigationDelegate(new GeckoSession.NavigationDelegate() {
            @Override
            public void onLocationChange(GeckoSession s, String url) {
                mUrlEditText.setText(url);
                TabManager.TabItem active = mTabManager.getActiveTab();
                if (active != null) active.url = url;
            }

            @Override
            public void onCanGoBack(GeckoSession s, boolean canGoBack) {}

            @Override
            public void onCanGoForward(GeckoSession s, boolean canGoForward) {}
        });

        session.setContentDelegate(new GeckoSession.ContentDelegate() {
            @Override
            public void onTitleChange(GeckoSession s, String title) {
                TabManager.TabItem active = mTabManager.getActiveTab();
                if (active != null) {
                    active.title = title;
                }
            }
        });
    }

    private void updateTabUI() {
        int count = mTabManager.getTabCount();
        mTabCountBtn.setText(String.valueOf(count));
        TabManager.TabItem active = mTabManager.getActiveTab();
        if (active != null && !TextUtils.isEmpty(active.url)) {
            mUrlEditText.setText(active.url);
        }
    }

    private void updateTabDrawerList() {
        mTabTitles.clear();
        List<TabManager.TabItem> tabs = mTabManager.getTabs();
        for (int i = 0; i < tabs.size(); i++) {
            TabManager.TabItem t = tabs.get(i);
            String title = TextUtils.isEmpty(t.title) ? t.url : t.title;
            mTabTitles.add((i + 1) + ". " + title);
        }
        mTabAdapter.notifyDataSetChanged();
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
        View view = getCurrentFocus();
        if (view != null && imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    @Override
    public void onBackPressed() {
        if (mTabDrawer.getVisibility() == View.VISIBLE) {
            mTabDrawer.setVisibility(View.GONE);
            return;
        }
        GeckoSession session = mTabManager.getActiveSession();
        if (session != null) {
            session.goBack();
            return;
        }
        super.onBackPressed();
    }
}
