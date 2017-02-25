package com.github.piasy.oauth3.github.presenter;

import android.support.v4.util.Pair;
import android.util.Log;
import com.github.piasy.oauth3.github.GitHubOAuth;
import com.github.piasy.oauth3.github.model.ApiErrorAwareConverterFactory;
import com.github.piasy.oauth3.github.model.AuthApi;
import com.github.piasy.oauth3.github.model.AutoGsonAdapterFactory;
import com.github.piasy.oauth3.github.model.GitHubError;
import com.github.piasy.oauth3.github.model.GitHubToken;
import com.github.piasy.oauth3.github.model.GitHubUser;
import com.github.piasy.oauth3.github.model.UserApi;
import com.github.piasy.oauth3.github.view.OAuthView;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by Piasy{github.com/Piasy} on 26/12/2016.
 */

public class OAuthPresenter {

    private final GitHubOAuth mGitHubOAuth;

    private final CompositeDisposable mDisposable;

    private final AuthApi mAuthApi;
    private final UserApi mUserApi;

    private OAuthView mOAuthView;

    public OAuthPresenter(GitHubOAuth gitHubOAuth) {
        mGitHubOAuth = gitHubOAuth;
        mDisposable = new CompositeDisposable();

        Gson gson = new GsonBuilder()
                .registerTypeAdapterFactory(AutoGsonAdapterFactory.create())
                .create();
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .addInterceptor(
                        new HttpLoggingInterceptor(message -> Log.d(GitHubOAuth.TAG, message))
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

    public void attatch(OAuthView oAuthView) {
        mOAuthView = oAuthView;
    }

    public void getAuthInfo(String code, String state) {
        Observable<GitHubToken> tokenInfo = mAuthApi
                .accessToken(mGitHubOAuth.clientId(), mGitHubOAuth.clientSecret(), code, state)
                .publish()
                .autoConnect(2);
        Observable<GitHubUser> userInfo = tokenInfo
                .flatMap(token -> mUserApi.user("token " + token.access_token()));

        Disposable disposable = Observable.zip(tokenInfo, userInfo, Pair::create)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(pair -> mOAuthView.authSuccess(pair.first.access_token(), pair.second),
                        throwable -> {
                            if (throwable instanceof GitHubError) {
                                mOAuthView.authFail(((GitHubError) throwable).error());
                            } else {
                                mOAuthView.authFail("Auth fail for unknown reason.");
                            }
                        });
        mDisposable.add(disposable);
    }

    public void destroy() {
        mDisposable.dispose();
    }
}
