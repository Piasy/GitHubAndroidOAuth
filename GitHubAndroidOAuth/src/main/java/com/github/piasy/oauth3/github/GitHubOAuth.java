package com.github.piasy.oauth3.github;

import android.app.Activity;
import android.os.Parcelable;
import android.text.TextUtils;
import com.github.piasy.oauth3.github.model.GitHubUser;
import com.github.piasy.oauth3.github.view.OAuthActivity;
import com.google.auto.value.AutoValue;
import java.util.Locale;
import java.util.Random;

/**
 * Created by Piasy{github.com/Piasy} on 25/12/2016.
 */

@AutoValue
public abstract class GitHubOAuth implements Parcelable {

    public static final String TAG = "GitHubOAuth";

    public static final int OAUTH_REQ = 1229;

    public static final String RESULT_KEY_ERROR_CODE = "GitHubOAuth_RESULT_KEY_ERROR_CODE";
    public static final String RESULT_KEY_USER = "GitHubOAuth_RESULT_KEY_USER";
    public static final String RESULT_KEY_TOKEN = "GitHubOAuth_RESULT_KEY_TOKEN";
    public static final String RESULT_KEY_ERROR = "GitHubOAuth_RESULT_KEY_ERROR";

    public static final int ERROR_OAUTH_FAIL = 1;
    public static final int ERROR_API_FAIL = 2;
    public static final int ERROR_USER_CANCEL = 3;
    public static final int ERROR_UNKNOWN_ERROR = 4;

    private static final String GITHUB_AUTH_WITHOUT_SCOPE_URL_FORMATTER
            = "https://github.com/login/oauth/authorize?client_id=%s&state=%s&redirect_uri=%s";
    private static final String GITHUB_AUTH_URL_FORMATTER
            = "https://github.com/login/oauth/authorize?client_id=%s&scope=%s&state=%s"
              + "&redirect_uri=%s";

    private final Random mRandom = new Random(System.currentTimeMillis());

    public static Builder builder() {
        return new AutoValue_GitHubOAuth.Builder()
                .debug(false);
    }

    public abstract String clientId();

    public abstract String clientSecret();

    public abstract String scope();

    public abstract String redirectUrl();

    public abstract boolean debug();

    public void authorize(android.app.Fragment fragment) {
        OAuthActivity.startOAuth(fragment, this);
    }

    public void authorize(android.support.v4.app.Fragment fragment) {
        OAuthActivity.startOAuth(fragment, this);
    }

    public void authorize(Activity activity) {
        OAuthActivity.startOAuth(activity, this);
    }

    public String authUrl() {
        String state = String.valueOf(System.currentTimeMillis() + mRandom.nextLong());
        if (TextUtils.isEmpty(scope())) {
            return String.format(Locale.getDefault(), GITHUB_AUTH_WITHOUT_SCOPE_URL_FORMATTER,
                    clientId(), state, redirectUrl());
        } else {
            return String.format(Locale.getDefault(), GITHUB_AUTH_URL_FORMATTER, clientId(),
                    scope(), state, redirectUrl());
        }
    }

    @AutoValue.Builder
    public abstract static class Builder {

        public abstract Builder clientId(String clientId);

        public abstract Builder clientSecret(String clientSecret);

        public abstract Builder scope(String scope);

        public abstract Builder redirectUrl(String redirectUrl);

        public abstract Builder debug(boolean debug);

        public abstract GitHubOAuth build();
    }
}
