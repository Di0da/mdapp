package net.olejon.mdapp;

/*

Copyright 2017 Ole Jon Bjørkum

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program. If not, see http://www.gnu.org/licenses/.

*/

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieSyncManager;
import android.webkit.SslErrorHandler;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;

public class MedicationNlhFragment extends Fragment
{
    private WebView mWebView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, final Bundle savedInstanceState)
    {
        ViewGroup viewGroup = (ViewGroup) inflater.inflate(R.layout.fragment_medication_nlh, container, false);

        // Activity
        final Activity activity = getActivity();

        // Context
        final Context context = activity.getApplicationContext();

        // Tools
        final MyTools mTools = new MyTools(context);

        // Arguments
        final String pageUri = getArguments().getString("uri");

        // Progress bar
        final ProgressBar progressBar = (ProgressBar) viewGroup.findViewById(R.id.medication_nlh_progressbar);

        // Toolbar
        final LinearLayout toolbarSearchLayout = (LinearLayout) activity.findViewById(R.id.medication_toolbar_search_layout);
        final EditText toolbarSearchEditText = (EditText) activity.findViewById(R.id.medication_toolbar_search);

        // Web view
        mWebView = (WebView) viewGroup.findViewById(R.id.medication_nlh_content);

        final WebSettings webSettings = mWebView.getSettings();

        webSettings.setJavaScriptEnabled(true);
        webSettings.setAppCacheEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setAppCachePath(context.getCacheDir().getAbsolutePath());
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);

        mWebView.setWebViewClient(new WebViewClient()
        {
            @SuppressWarnings("deprecation")
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url)
            {
                if(!mTools.isDeviceConnected())
                {
                    mTools.showToast(getString(R.string.device_not_connected), 0);
                    return true;
                }
                else if(url.matches(".*/[^#]+#[^/]+$"))
                {
                    mWebView.loadUrl(url.replaceAll("#[^/]+$", ""));
                    return true;
                }
                else if(url.matches("^https?://play\\.google\\.com/.*") || url.matches("^https?://itunes\\.apple\\.com/.*"))
                {
                    mTools.showToast(getString(R.string.device_not_supported), 1);
                    return true;
                }
                else if(url.matches("^https?://.*?\\.pdf$") || url.matches("^https?://.*?\\.docx?$") || url.matches("^https?://.*?\\.xlsx?$") || url.matches("^https?://.*?\\.pptx?$"))
                {
                    mTools.downloadFile(view.getTitle(), url);
                    return true;
                }
                else if(url.startsWith("mailto:"))
                {
                    Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.parse(url));
                    startActivity(Intent.createChooser(intent, getString(R.string.project_feedback_text)));
                    return true;
                }
                else if(url.startsWith("tel:"))
                {
                    try
                    {
                        Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse(url));
                        startActivity(intent);
                    }
                    catch(Exception e)
                    {
                        new MaterialDialog.Builder(context).title(R.string.device_not_supported_dialog_title).content(getString(R.string.device_not_supported_dialog_message)).positiveText(R.string.device_not_supported_dialog_positive_button).contentColorRes(R.color.black).positiveColorRes(R.color.dark_blue).show();
                    }

                    return true;
                }

                return false;
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon)
            {
                progressBar.setVisibility(View.VISIBLE);

                toolbarSearchLayout.setVisibility(View.GONE);
                toolbarSearchEditText.setText("");
            }

            @Override
            public void onPageFinished(WebView view, String url)
            {
                progressBar.setVisibility(View.GONE);

                mWebView.loadUrl("javascript:if(document.getElementById('mainframe') != null) { document.getElementById('mainframe').firstElementChild.style.display = 'none'; } void(0);");
            }

            @Override
            public void onReceivedSslError(WebView view, @NonNull SslErrorHandler handler, SslError error)
            {
                handler.cancel();

                mWebView.stopLoading();

                progressBar.setVisibility(View.INVISIBLE);

                new MaterialDialog.Builder(context).title(R.string.device_not_supported_dialog_title).content(R.string.device_not_supported_dialog_ssl_error_message).positiveText(R.string.device_not_supported_dialog_positive_button).onPositive(new MaterialDialog.SingleButtonCallback()
                {
                    @Override
                    public void onClick(@NonNull MaterialDialog materialDialog, @NonNull DialogAction dialogAction)
                    {
                        mWebView.goBack();
                    }
                }).cancelListener(new DialogInterface.OnCancelListener()
                {
                    @Override
                    public void onCancel(DialogInterface dialogInterface)
                    {
                        mWebView.goBack();
                    }
                }).contentColorRes(R.color.black).positiveColorRes(R.color.dark_blue).show();
            }
        });

        if(savedInstanceState == null)
        {
            mWebView.loadUrl(pageUri);
        }
        else
        {
            mWebView.restoreState(savedInstanceState);
        }

        return viewGroup;
    }

    // Resume fragment
    @Override
    public void onResume()
    {
        super.onResume();

        mWebView.resumeTimers();
    }

    // Pause fragment
    @Override
    public void onPause()
    {
        super.onPause();

        mWebView.pauseTimers();

        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
        {
            //noinspection deprecation
            CookieSyncManager.getInstance().sync();
        }
    }

    // Save fragment
    @Override
    public void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);

        mWebView.saveState(outState);
    }
}