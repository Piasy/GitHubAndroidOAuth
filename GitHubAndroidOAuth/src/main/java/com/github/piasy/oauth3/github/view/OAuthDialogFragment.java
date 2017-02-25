package com.github.piasy.oauth3.github.view;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import com.github.piasy.dialogfragmentanywhere.BaseDialogFragment;
import com.github.piasy.github.androido.auth3.R;
import com.github.piasy.oauth3.github.GitHubOAuth;
import com.github.piasy.oauth3.github.model.GitHubUser;
import com.github.piasy.oauth3.github.presenter.OAuthPresenter;
import okhttp3.HttpUrl;

import static butterknife.ButterKnife.findById;

/**
 * Created by Piasy{github.com/Piasy} on 25/12/2016.
 */

public class OAuthDialogFragment extends BaseDialogFragment implements OAuthView {

    private static final String ARG_KEY_AUTH = "ARG_KEY_AUTH";
    private static final int REQ_AUTH = 1024;

    private GitHubOAuth mGitHubOAuth;

    private GitHubOAuth.Listener mListener;
    private OAuthPresenter mOAuthPresenter;

    private WebView mWebView;
    private ProgressBar mProgress;

    public static void startOAuth(Fragment host, GitHubOAuth gitHubOAuth) {
        if (!(host instanceof GitHubOAuth.Listener)) {
            throw new IllegalArgumentException(
                    "host must implement " + GitHubOAuth.Listener.class.getName());
        }
        OAuthDialogFragment fragment = new OAuthDialogFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_KEY_AUTH, gitHubOAuth);
        fragment.setArguments(args);
        fragment.setTargetFragment(host, REQ_AUTH);
        fragment.show(host.getFragmentManager(), OAuthDialogFragment.class.getName());
    }

    public static void startOAuth(AppCompatActivity host, GitHubOAuth gitHubOAuth) {
        if (!(host instanceof GitHubOAuth.Listener)) {
            throw new IllegalArgumentException(
                    "host must implement " + GitHubOAuth.Listener.class.getName());
        }
        OAuthDialogFragment fragment = new OAuthDialogFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_KEY_AUTH, gitHubOAuth);
        fragment.setArguments(args);
        fragment.show(host.getSupportFragmentManager(), OAuthDialogFragment.class.getName());
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (getTargetFragment() instanceof GitHubOAuth.Listener) {
            mListener = (GitHubOAuth.Listener) getTargetFragment();
        } else {
            mListener = (GitHubOAuth.Listener) getActivity();
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();

        mListener = null;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mGitHubOAuth = getArguments().getParcelable(ARG_KEY_AUTH);
        if (mGitHubOAuth == null) {
            throw new NullPointerException("null auth info");
        }
        mOAuthPresenter = new OAuthPresenter(mGitHubOAuth);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mOAuthPresenter.attatch(this);
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
               - resources.getDimensionPixelSize(R.dimen.auth_dialog_margin_side) * 2;
    }

    @Override
    protected int getHeight() {
        Resources resources = getResources();
        return resources.getDimensionPixelSize(R.dimen.auth_dialog_height);
    }

    @Override
    protected void bindView(View rootView) {
        super.bindView(rootView);

        if (mListener == null) {
            Log.e(GitHubOAuth.TAG, "Listener not set, or recreate after destroy.");
            safeDismiss();
        }

        mWebView = findById(rootView, R.id.mWebView);
        mProgress = findById(rootView, R.id.mProgress);
    }

    @Override
    protected void startBusiness() {
        super.startBusiness();

        mWebView.setVerticalScrollBarEnabled(false);
        mWebView.setHorizontalScrollBarEnabled(false);
        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                Log.d(GitHubOAuth.TAG, "Redirecting URL " + url);

                if (url.startsWith(mGitHubOAuth.redirectUrl())) {
                    HttpUrl httpUrl = HttpUrl.parse(url);
                    String error = httpUrl.queryParameter("error");
                    if (TextUtils.isEmpty(error)) {
                        mOAuthPresenter.getAuthInfo(httpUrl.queryParameter("code"),
                                httpUrl.queryParameter("state"));
                    } else {
                        authFail(error);
                    }
                    return true;
                }
                return false;
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);

                Log.d(GitHubOAuth.TAG, "Loading URL: " + url);
                mProgress.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);

                mProgress.setVisibility(View.GONE);
            }
        });
        mWebView.loadUrl(mGitHubOAuth.authUrl());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mProgress.setVisibility(View.GONE);
        mOAuthPresenter.destroy();
    }

    @Override
    public void authSuccess(String token, GitHubUser user) {
        mListener.onSuccess(token, user);
        safeDismiss();
    }

    @Override
    public void authFail(String error) {
        mListener.onFail(error);
        safeDismiss();
    }
}
