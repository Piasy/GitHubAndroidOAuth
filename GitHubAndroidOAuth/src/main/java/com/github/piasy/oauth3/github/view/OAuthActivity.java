package com.github.piasy.oauth3.github.view;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import com.github.piasy.oauth3.github.GitHubOAuth;
import com.github.piasy.oauth3.github.R;
import com.github.piasy.oauth3.github.model.GitHubUser;
import com.github.piasy.oauth3.github.presenter.OAuthPresenter;
import icepick.Icepick;
import icepick.State;
import io.reactivex.subjects.ReplaySubject;
import java.lang.ref.WeakReference;
import okhttp3.HttpUrl;

public class OAuthActivity extends AppCompatActivity implements OAuthView {

    private static final String ARG_KEY_AUTH = "ARG_KEY_AUTH";

    private static final int STATE_NOT_REQ = 0;
    private static final int STATE_SEND_REQ = 1;
    private static final int STATE_WAIT_CODE = 2;
    private static final int STATE_CALL_API = 3;
    private static final int STATE_SUCCESS = 4;
    private static final int STATE_FAIL = 5;

    private static WeakReference<ReplaySubject<Pair<OAuthResult, String>>> sOAuthResultSubject;

    @State
    int mState = STATE_NOT_REQ;
    @State
    GitHubOAuth mGitHubOAuth;
    @State
    OAuthResult mOAuthResult;

    private OAuthPresenter mOAuthPresenter;
    private ReplaySubject<Pair<OAuthResult, String>> mOAuthResultSubject;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(GitHubOAuth.TAG, "OAuthActivity: onCreate "
                               + "savedInstanceState = " + savedInstanceState
                               + ", getIntent() = " + getIntent());

        setContentView(R.layout.progress_dialog);

        Icepick.restoreInstanceState(this, savedInstanceState);

        if (mGitHubOAuth == null) {
            mGitHubOAuth = getIntent().getParcelableExtra(ARG_KEY_AUTH);
        }

        // init reference
        if (sOAuthResultSubject == null || sOAuthResultSubject.get() == null) {
            mOAuthResultSubject = ReplaySubject.create();
            sOAuthResultSubject = new WeakReference<>(mOAuthResultSubject);
        } else {
            mOAuthResultSubject = sOAuthResultSubject.get();
        }

        if (isBrowserIntent(getIntent())) {
            Log.d(GitHubOAuth.TAG, "OAuthActivity: Got browser intent in new created instance.");

            Pair<OAuthResult, String> result = getOAuthResult(getIntent());
            mOAuthResultSubject.onNext(result);
            finish();
            return;
        } else if (mGitHubOAuth == null) {
            authFail(GitHubOAuth.ERROR_UNKNOWN_ERROR, "Invalid launch intent");
            return;
        }

        mOAuthPresenter = new OAuthPresenter(mGitHubOAuth);
        mOAuthPresenter.attach(this);

        Log.d(GitHubOAuth.TAG, "OAuthActivity: onCreate mState = " + mState);
        switch (mState) {
            case STATE_SEND_REQ:
                // recreated after send request, check `sOAuthResultSubject`
                mState = STATE_WAIT_CODE;
                mOAuthPresenter.waitCode(mOAuthResultSubject);
                break;
            case STATE_CALL_API:
                // recreated after got code, because code can only be used once, so we fail
                authFail(GitHubOAuth.ERROR_UNKNOWN_ERROR, "Activity killed when call api");
                break;
            case STATE_NOT_REQ:
                handleLaunchIntent();
                break;
            default:
                // we may got killed at STATE_WAIT_CODE, it's too complicated to handle, just fail
                authFail(GitHubOAuth.ERROR_UNKNOWN_ERROR, "un-handled state " + mState);
                break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        Log.d(GitHubOAuth.TAG, "OAuthActivity: onResume mState = " + mState);

        if (mState == STATE_SEND_REQ) {
            authFail(GitHubOAuth.ERROR_USER_CANCEL, "User canceled.");
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (mState == STATE_NOT_REQ) {
            mState = STATE_SEND_REQ;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        Log.d(GitHubOAuth.TAG, "OAuthActivity: onDestroy");

        if (mOAuthPresenter != null) {
            mOAuthPresenter.destroy();
        }
        // reduce reference
        mOAuthResultSubject = null;
    }

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
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        Icepick.saveInstanceState(this, outState);
    }

    private void handleLaunchIntent() {
        Log.d(GitHubOAuth.TAG,
                "OAuthActivity: Open browser to request auth: " + mGitHubOAuth.authUrl());
        Intent browser = new Intent(Intent.ACTION_VIEW, Uri.parse(mGitHubOAuth.authUrl()));
        startActivity(browser);
    }

    @Override
    public void authSuccess(String token, GitHubUser user) {
        mState = STATE_SUCCESS;
        Intent data = new Intent();
        data.putExtra(GitHubOAuth.RESULT_KEY_TOKEN, token);
        data.putExtra(GitHubOAuth.RESULT_KEY_USER, user);
        setResult(RESULT_OK, data);
        finish();
    }

    @Override
    public void authFail(int code, String error) {
        mState = STATE_FAIL;
        Intent data = new Intent();
        data.putExtra(GitHubOAuth.RESULT_KEY_ERROR_CODE, code);
        data.putExtra(GitHubOAuth.RESULT_KEY_ERROR, error);
        setResult(RESULT_CANCELED, data);
        finish();
    }

    @Override
    public void codeArrived(OAuthResult result) {
        mState = STATE_CALL_API;
        mOAuthResult = result;
    }

    @Override
    public void onBackPressed() {
        authFail(GitHubOAuth.ERROR_USER_CANCEL, "User canceled.");
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        Log.d(GitHubOAuth.TAG, "OAuthActivity: onNewIntent " + intent);

        handleBrowserResult(intent);
    }

    private boolean handleBrowserResult(Intent intent) {
        if (isBrowserIntent(intent)) {
            Pair<OAuthResult, String> result = getOAuthResult(intent);
            if (TextUtils.isEmpty(result.second)) {
                mState = STATE_CALL_API;
                mOAuthResult = result.first;
                mOAuthPresenter.getAuthInfo(mOAuthResult.code(), mOAuthResult.state());
            } else {
                authFail(GitHubOAuth.ERROR_OAUTH_FAIL, result.second);
            }
            return true;
        }

        return false;
    }

    private boolean isBrowserIntent(Intent intent) {
        return TextUtils.equals(intent.getAction(), Intent.ACTION_VIEW) && intent.getData() != null;
    }

    private Pair<OAuthResult, String> getOAuthResult(Intent intent) {
        HttpUrl httpUrl = HttpUrl.parse(intent.getData().toString());
        String error = httpUrl.queryParameter("error");
        if (TextUtils.isEmpty(error)) {
            String code = httpUrl.queryParameter("code");
            String state = httpUrl.queryParameter("state");
            return Pair.create(OAuthResult.create(code, state), null);
        } else {
            return Pair.create(null, error);
        }
    }
}
