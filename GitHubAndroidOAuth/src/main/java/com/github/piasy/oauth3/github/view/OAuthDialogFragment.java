package com.github.piasy.oauth3.github.view;

import android.app.ProgressDialog;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import com.github.piasy.dialogfragmentanywhere.BaseDialogFragment;
import com.github.piasy.github.androido.auth3.R;
import com.github.piasy.oauth3.github.GitHubOAuth;
import okhttp3.HttpUrl;

import static butterknife.ButterKnife.findById;

/**
 * Created by Piasy{github.com/Piasy} on 25/12/2016.
 */

public class OAuthDialogFragment extends BaseDialogFragment {

    private static final String ARG_KEY_AUTH_URL = "ARG_KEY_AUTH_URL";
    private static final String ARG_KEY_REDIRECT_URL = "ARG_KEY_REDIRECT_URL";

    private String mAuthUrl;
    private String mRedirectUrl;

    private ProgressDialog mSpinner;
    private Listener mListener;

    public static void startOAuth(FragmentManager fragmentManager, String authUrl,
            String redirectUrl, Listener listener) {
        OAuthDialogFragment fragment = new OAuthDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_KEY_AUTH_URL, authUrl);
        args.putString(ARG_KEY_REDIRECT_URL, redirectUrl);
        fragment.setArguments(args);
        fragment.setListener(listener);
        fragment.show(fragmentManager, OAuthDialogFragment.class.getName());
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAuthUrl = getArguments().getString(ARG_KEY_AUTH_URL);
        mRedirectUrl = getArguments().getString(ARG_KEY_REDIRECT_URL);
    }

    @Override
    protected int getLayoutRes() {
        return R.layout.fragment_auth;
    }

    @Override
    protected int getGravity() {
        return Gravity.CENTER;
    }

    @Override
    protected int getWidth() {
        Resources resources = getResources();
        return resources.getDisplayMetrics().widthPixels
               - resources.getDimensionPixelSize(R.dimen.auth_dialog_margin) * 2;
    }

    @Override
    protected int getHeight() {
        Resources resources = getResources();
        return resources.getDisplayMetrics().heightPixels
               - resources.getDimensionPixelSize(R.dimen.auth_dialog_margin) * 2;
    }

    @Override
    protected void bindView(View rootView) {
        super.bindView(rootView);

        if (mListener == null) {
            Log.e(GitHubOAuth.TAG, "Listener not set, or recreate after destroy.");
            safeDismiss();
        }

        mSpinner = new ProgressDialog(getContext());
        mSpinner.requestWindowFeature(Window.FEATURE_NO_TITLE);
        mSpinner.setMessage(getString(R.string.oauth_loading_message));

        WebView webView = findById(rootView, R.id.mWebView);
        webView.setVerticalScrollBarEnabled(false);
        webView.setHorizontalScrollBarEnabled(false);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                Log.d(GitHubOAuth.TAG, "Redirecting URL " + url);

                if (url.startsWith(mRedirectUrl)) {
                    HttpUrl httpUrl = HttpUrl.parse(url);
                    String error = httpUrl.queryParameter("error");
                    if (TextUtils.isEmpty(error)) {
                        mListener.onComplete(httpUrl.queryParameter("code"),
                                httpUrl.queryParameter("state"));
                    } else {
                        mListener.onError(error);
                    }
                    safeDismiss();
                    return true;
                }
                return false;
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);

                Log.d(GitHubOAuth.TAG, "Loading URL: " + url);
                mSpinner.show();
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);

                mSpinner.hide();
            }

            @Override
            public void onReceivedError(WebView view, int errorCode,
                    String description, String failingUrl) {
                Log.d(GitHubOAuth.TAG, "Page error: " + description);

                super.onReceivedError(view, errorCode, description, failingUrl);
                safeDismiss();
            }
        });
        webView.loadUrl(mAuthUrl);
    }

    private void setListener(Listener listener) {
        mListener = listener;
    }

    public interface Listener {

        void onComplete(String code, String state);

        void onError(String error);
    }
}
