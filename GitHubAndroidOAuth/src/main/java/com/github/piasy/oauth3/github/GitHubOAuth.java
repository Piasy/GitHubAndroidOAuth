package com.github.piasy.oauth3.github;

import android.support.v4.app.FragmentManager;
import android.text.TextUtils;
import com.github.piasy.oauth3.github.model.ApiErrorAwareConverterFactory;
import com.github.piasy.oauth3.github.model.AutoGsonAdapterFactory;
import com.github.piasy.oauth3.github.model.GitHubApi;
import com.github.piasy.oauth3.github.view.OAuthDialogFragment;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.jakewharton.retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import java.util.Locale;
import java.util.Random;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by Piasy{github.com/Piasy} on 25/12/2016.
 */

public final class GitHubOAuth {

    public static final String TAG = "GitHubOAuth";

    private static final String GITHUB_AUTH_WITHOUT_SCOPE_URL_FORMATTER
            = "https://github.com/login/oauth/authorize?client_id=%s&state=%s&redirect_uri=%s";
    private static final String GITHUB_AUTH_URL_FORMATTER
            = "https://github.com/login/oauth/authorize?client_id=%s&scope=%s&state=%s"
              + "&redirect_uri=%s";

    private final String mClientId;
    private final String mClientSecret;
    private final String mScope;
    private final String mRedirectUrl;

    private final Listener mListener;
    private final CompositeDisposable mDisposable;

    private final Random mRandom;
    private final GitHubApi mGitHubApi;

    private GitHubOAuth(String clientId, String clientSecret, String scope, String redirectUrl,
            Listener listener) {
        if (TextUtils.isEmpty(clientId)) {
            throw new NullPointerException("Empty clientId");
        }
        if (TextUtils.isEmpty(clientSecret)) {
            throw new NullPointerException("Empty clientSecret");
        }
        if (TextUtils.isEmpty(clientId)) {
            throw new NullPointerException("Empty clientId");
        }
        if (TextUtils.isEmpty(redirectUrl)) {
            throw new NullPointerException("Empty redirectUrl");
        }
        if (listener == null) {
            throw new NullPointerException("Empty listener");
        }

        mClientId = clientId;
        mClientSecret = clientSecret;
        mScope = scope;
        mRedirectUrl = redirectUrl;
        mListener = listener;
        mDisposable = new CompositeDisposable();

        mRandom = new Random(System.currentTimeMillis());

        Gson gson = new GsonBuilder()
                .registerTypeAdapterFactory(AutoGsonAdapterFactory.create())
                .create();
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(GitHubApi.BASE_URL)
                .addCallAdapterFactory(
                        RxJava2CallAdapterFactory.createWithScheduler(Schedulers.io()))
                .addConverterFactory(
                        new ApiErrorAwareConverterFactory(GsonConverterFactory.create(gson),
                                GitHubApi.GitHubError.class))
                .build();

        mGitHubApi = retrofit.create(GitHubApi.class);
    }

    public void authorize(FragmentManager fragmentManager) {
        String state = String.valueOf(System.currentTimeMillis() + mRandom.nextLong());
        String authUrl;
        if (TextUtils.isEmpty(mScope)) {
            authUrl = String.format(Locale.getDefault(), GITHUB_AUTH_WITHOUT_SCOPE_URL_FORMATTER,
                    mClientId, state, mRedirectUrl);
        } else {
            authUrl = String.format(Locale.getDefault(), GITHUB_AUTH_URL_FORMATTER, mClientId,
                    mScope, state, mRedirectUrl);
        }
        OAuthDialogFragment.startOAuth(fragmentManager, authUrl, mRedirectUrl,
                new OAuthDialogFragment.Listener() {
                    @Override
                    public void onComplete(String code, String state) {
                        Disposable disposable = mGitHubApi.accessToken(mClientId, mClientSecret,
                                code, state)
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(new Consumer<GitHubApi.GitHubToken>() {
                                    @Override
                                    public void accept(GitHubApi.GitHubToken gitHubToken)
                                            throws Exception {
                                        mListener.onSuccess(gitHubToken.access_token());
                                    }
                                }, new Consumer<Throwable>() {
                                    @Override
                                    public void accept(Throwable throwable) throws Exception {
                                        if (throwable instanceof GitHubApi.GitHubError) {
                                            mListener.onFail(
                                                    ((GitHubApi.GitHubError) throwable).error());
                                        } else {
                                            mListener.onFail("Auth fail for unknown reason.");
                                        }
                                    }
                                });
                        mDisposable.add(disposable);
                    }

                    @Override
                    public void onError(String error) {
                        mListener.onFail(error);
                    }
                });
    }

    public void destroy() {
        mDisposable.dispose();
    }

    public interface Listener {

        void onSuccess(String token);

        void onFail(String error);
    }

    public static class Builder {

        private String mClientId;
        private String mClientSecret;
        private String mScope;
        private String mRedirectUrl;
        private Listener mListener;

        public Builder clientId(String clientId) {
            mClientId = clientId;
            return this;
        }

        public Builder clientSecret(String clientSecret) {
            mClientSecret = clientSecret;
            return this;
        }

        public Builder scope(String scope) {
            mScope = scope;
            return this;
        }

        public Builder redirectUrl(String redirectUrl) {
            mRedirectUrl = redirectUrl;
            return this;
        }

        public Builder listener(Listener listener) {
            mListener = listener;
            return this;
        }

        public GitHubOAuth build() {
            return new GitHubOAuth(mClientId, mClientSecret, mScope, mRedirectUrl, mListener);
        }
    }
}
