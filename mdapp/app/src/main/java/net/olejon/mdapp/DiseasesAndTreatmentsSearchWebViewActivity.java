package net.olejon.mdapp;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

public class DiseasesAndTreatmentsSearchWebViewActivity extends ActionBarActivity
{
    private final Context mContext = this;

    private final MyTools mTools = new MyTools(mContext);

    private MenuItem goForwardMenuItem;
    private ProgressBar mProgressBar;
    private WebView mWebView;

    private String pageUri;

    // Create activity
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // Connected?
        if(!mTools.isDeviceConnected())
        {
            mTools.showToast(getString(R.string.device_not_connected), 1);

            finish();

            return;
        }

        // Transition
        overridePendingTransition(R.anim.slide_in_from_right, R.anim.slide_out_to_left);

        // Intent
        Intent intent = getIntent();

        final String pageTitle = intent.getStringExtra("title");

        pageUri = intent.getStringExtra("uri");

        // Layout
        setContentView(R.layout.activity_diseases_and_treatments_search_webview);

        // Toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.diseases_and_treatments_search_webview_toolbar);
        toolbar.setTitle(pageTitle);

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Progress bar
        mProgressBar = (ProgressBar) findViewById(R.id.diseases_and_treatments_search_webview_toolbar_progressbar_horizontal);

        // Web view
        mWebView = (WebView) findViewById(R.id.diseases_and_treatments_search_webview_content);

        WebSettings webSettings = mWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);
        webSettings.setUseWideViewPort(true);

        mWebView.setWebViewClient(new WebViewClient()
        {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url)
            {
                return false;
            }
        });

        mWebView.setWebChromeClient(new WebChromeClient()
        {
            @Override
            public void onProgressChanged(WebView view, int newProgress)
            {
                if(newProgress == 100)
                {
                    mProgressBar.setVisibility(View.INVISIBLE);

                    if(mWebView.canGoForward())
                    {
                        goForwardMenuItem.setVisible(true);
                    }
                    else
                    {
                        goForwardMenuItem.setVisible(false);
                    }
                }
                else
                {
                    mProgressBar.setVisibility(View.VISIBLE);
                    mProgressBar.setProgress(newProgress);
                }
            }
        });

        CookieManager cookieManager = CookieManager.getInstance();

        cookieManager.setCookie("http://nhi.no/", "userCategory=professional");
        cookieManager.setCookie("http://tidsskriftet.no/", "osevencookiepromptclosed=1");
        cookieManager.setCookie("https://helsenorge.no/", "mh-unsupportedbar=");

        mWebView.setInitialScale(100);
        mWebView.loadUrl(pageUri);
    }

    // Resume activity
    @Override
    protected void onResume()
    {
        super.onResume();

        mWebView.resumeTimers();
    }

    // Pause activity
    @Override
    protected void onPause()
    {
        super.onPause();

        mWebView.pauseTimers();

        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) CookieSyncManager.getInstance().sync();
    }

    // Back button
    @Override
    public void onBackPressed()
    {
        if(mWebView.canGoBack())
        {
            mWebView.goBack();
        }
        else
        {
            super.onBackPressed();

            overridePendingTransition(R.anim.slide_in_from_left, R.anim.slide_out_to_right);
        }
    }

    // Menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.menu_diseases_and_treatments_search_webview, menu);

        goForwardMenuItem = menu.findItem(R.id.diseases_and_treatments_search_webview_menu_go_forward);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch(item.getItemId())
        {
            case android.R.id.home:
            {
                finish();
                overridePendingTransition(R.anim.slide_in_from_left, R.anim.slide_out_to_right);
                return true;
            }
            case R.id.diseases_and_treatments_search_webview_menu_go_forward:
            {
                mWebView.goForward();
                return true;
            }
            case R.id.diseases_and_treatments_search_webview_menu_uri:
            {
                mTools.openUri(pageUri);
                return true;
            }
            default:
            {
                return super.onOptionsItemSelected(item);
            }
        }
    }
}
