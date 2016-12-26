package com.github.piasy.oauth3.github;

import android.support.v4.app.FragmentManager;
import android.support.v4.util.Pair;
import android.text.TextUtils;
import android.util.Log;
import com.github.piasy.oauth3.github.model.ApiErrorAwareConverterFactory;
import com.github.piasy.oauth3.github.model.AuthApi;
import com.github.piasy.oauth3.github.model.AutoGsonAdapterFactory;
import com.github.piasy.oauth3.github.model.GitHubError;
import com.github.piasy.oauth3.github.model.GitHubToken;
import com.github.piasy.oauth3.github.model.GitHubUser;
import com.github.piasy.oauth3.github.model.UserApi;
import com.github.piasy.oauth3.github.view.OAuthDialogFragment;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.jakewharton.retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import java.util.Locale;
import java.util.Random;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
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
    private final AuthApi mAuthApi;
    private final UserApi mUserApi;

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
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .addInterceptor(new HttpLoggingInterceptor(message -> Log.d(TAG, message))
                        .setLevel(HttpLoggingInterceptor.Level.BODY))
                .build();

        mAuthApi = new Retrofit.Builder()
                .baseUrl(AuthApi.BASE_URL)
                .client(okHttpClient)
                .addCallAdapterFactory(
                        RxJava2CallAdapterFactory.createWithScheduler(Schedulers.io()))
                .addConverterFactory(
                        new ApiErrorAwareConverterFactory(GsonConverterFactory.create(gson),
                                GitHubError.class))
                .build()
                .create(AuthApi.class);
        mUserApi = new Retrofit.Builder()
                .baseUrl(UserApi.BASE_URL)
                .client(okHttpClient)
                .addCallAdapterFactory(
                        RxJava2CallAdapterFactory.createWithScheduler(Schedulers.io()))
                .addConverterFactory(
                        new ApiErrorAwareConverterFactory(GsonConverterFactory.create(gson),
                                GitHubError.class))
                .build()
                .create(UserApi.class);
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
                        getAuthInfo(code, state);
                    }

                    @Override
                    public void onError(String error) {
                        mListener.onFail(error);
                    }
                });
    }

    private void getAuthInfo(String code, String state) {
        Observable<GitHubToken> tokenInfo = mAuthApi
                .accessToken(mClientId, mClientSecret, code, state)
                .publish()
                .autoConnect(2);
        Observable<GitHubUser> userInfo = tokenInfo
                .flatMap(token -> mUserApi.user("token " + token.access_token()));

        Disposable disposable = Observable.zip(tokenInfo, userInfo, Pair::create)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(pair -> mListener.onSuccess(pair.first.access_token(), pair.second),
                        throwable -> {
                            if (throwable instanceof GitHubError) {
                                mListener.onFail(((GitHubError) throwable).error());
                            } else {
                                mListener.onFail("Auth fail for unknown reason.");
                            }
                        });
        mDisposable.add(disposable);
    }

    public void destroy() {
        mDisposable.dispose();
    }

    public interface Listener {

        void onSuccess(String token, GitHubUser user);

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
