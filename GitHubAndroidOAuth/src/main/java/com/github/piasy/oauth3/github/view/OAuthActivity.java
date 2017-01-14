package com.github.piasy.oauth3.github.view;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Window;
import com.github.piasy.oauth3.github.GitHubOAuth;
import com.github.piasy.oauth3.github.R;
import com.github.piasy.oauth3.github.model.GitHubUser;
import com.github.piasy.oauth3.github.presenter.OAuthPresenter;
import okhttp3.HttpUrl;

public class OAuthActivity extends AppCompatActivity implements OAuthView {

    private static final String ARG_KEY_AUTH = "ARG_KEY_AUTH";

    private OAuthPresenter mOAuthPresenter;

    public static void startOAuth(android.app.Fragment fragment, GitHubOAuth gitHubOAuth) {
        fragment.startActivityForResult(createIntent(fragment.getActivity(), gitHubOAuth),
                GitHubOAuth.OAUTH_REQ);
    }

    public static void startOAuth(android.support.v4.app.Fragment fragment,
            GitHubOAuth gitHubOAuth) {
        fragment.startActivityForResult(createIntent(fragment.getContext(), gitHubOAuth),
                GitHubOAuth.OAUTH_REQ);
    }

    public static void startOAuth(Activity activity, GitHubOAuth gitHubOAuth) {
        activity.startActivityForResult(createIntent(activity, gitHubOAuth), GitHubOAuth.OAUTH_REQ);
    }

    private static Intent createIntent(Context context, GitHubOAuth gitHubOAuth) {
        Intent intent = new Intent(context, OAuthActivity.class);
        intent.putExtra(ARG_KEY_AUTH, gitHubOAuth);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        GitHubOAuth gitHubOAuth = getIntent().getParcelableExtra(ARG_KEY_AUTH);

        if (gitHubOAuth == null) {
            Log.e(GitHubOAuth.TAG, "OAuthActivity: Invalid intent, finishing...");
            finish();
            return;
        }

        Log.d(GitHubOAuth.TAG, "OAuthActivity: onCreate " + gitHubOAuth);

        mOAuthPresenter = new OAuthPresenter(gitHubOAuth);

        ProgressDialog spinner = new ProgressDialog(this);
        spinner.requestWindowFeature(Window.FEATURE_NO_TITLE);
        spinner.setMessage(getString(R.string.oauth_loading_message));
        spinner.show();

        if (savedInstanceState == null) {
            Log.d(GitHubOAuth.TAG,
                    "OAuthActivity: Open browser to request auth: " + gitHubOAuth.authUrl());
            Intent browser = new Intent(Intent.ACTION_VIEW, Uri.parse(gitHubOAuth.authUrl()));
            startActivity(browser);
        }
        mOAuthPresenter.attach(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        mOAuthPresenter.destroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.d(GitHubOAuth.TAG, "onSaveInstanceState " + outState);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        Log.d(GitHubOAuth.TAG, "OAuthActivity: onNewIntent " + intent);

        if (TextUtils.equals(intent.getAction(), Intent.ACTION_VIEW) && intent.getData() != null) {
            HttpUrl httpUrl = HttpUrl.parse(intent.getData().toString());
            String error = httpUrl.queryParameter("error");
            if (TextUtils.isEmpty(error)) {
                mOAuthPresenter.getAuthInfo(httpUrl.queryParameter("code"),
                        httpUrl.queryParameter("state"));
            } else {
                authFail(error);
            }
        }
    }

    @Override
    public void authSuccess(String token, GitHubUser user) {
        Intent data = new Intent();
        data.putExtra(GitHubOAuth.RESULT_KEY_TOKEN, token);
        data.putExtra(GitHubOAuth.RESULT_KEY_USER, user);
        setResult(RESULT_OK, data);
        finish();
    }

    @Override
    public void authFail(String error) {
        Intent data = new Intent();
        data.putExtra(GitHubOAuth.RESULT_KEY_ERROR, error);
        setResult(RESULT_CANCELED, data);
        finish();
    }
}
