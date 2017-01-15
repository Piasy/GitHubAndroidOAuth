package com.github.piasy.oauth3.github.presenter;

import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import com.github.piasy.oauth3.github.GitHubOAuth;
import com.github.piasy.oauth3.github.model.ApiErrorAwareConverterFactory;
import com.github.piasy.oauth3.github.model.AuthApi;
import com.github.piasy.oauth3.github.model.AutoGsonAdapterFactory;
import com.github.piasy.oauth3.github.model.GitHubError;
import com.github.piasy.oauth3.github.model.GitHubToken;
import com.github.piasy.oauth3.github.model.GitHubUser;
import com.github.piasy.oauth3.github.model.UserApi;
import com.github.piasy.oauth3.github.view.OAuthResult;
import com.github.piasy.oauth3.github.view.OAuthView;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.jakewharton.retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.functions.Functions;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.Subject;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by Piasy{github.com/Piasy} on 26/12/2016.
 */

public class OAuthPresenter {

    private static final Pair<OAuthResult, String> STOP_WAITING = Pair.create(null, null);
    private static final int WAIT_CODE_TIMEOUT_SECONDS = 3;

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

        OkHttpClient.Builder httpClientBuilder = new OkHttpClient.Builder();
        if (mGitHubOAuth.debug()) {
            httpClientBuilder.addInterceptor(
                    new HttpLoggingInterceptor(message -> Log.d(GitHubOAuth.TAG, message))
                            .setLevel(HttpLoggingInterceptor.Level.BODY));
        }

        Retrofit.Builder retrofitBuilder = new Retrofit.Builder()
                .client(httpClientBuilder.build())
                .addCallAdapterFactory(
                        RxJava2CallAdapterFactory.createWithScheduler(Schedulers.io()))
                .addConverterFactory(
                        new ApiErrorAwareConverterFactory(GsonConverterFactory.create(gson),
                                GitHubError.class));

        mAuthApi = retrofitBuilder
                .baseUrl(AuthApi.BASE_URL)
                .build()
                .create(AuthApi.class);
        mUserApi = retrofitBuilder
                .baseUrl(UserApi.BASE_URL)
                .build()
                .create(UserApi.class);

        // see https://github.com/ReactiveX/RxJava/issues/4996#issuecomment-272686780
        RxJavaPlugins.setErrorHandler(Functions.emptyConsumer());
    }

    public void attach(OAuthView oAuthView) {
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
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(pair -> {
                            if (mOAuthView != null) {
                                mOAuthView.authSuccess(pair.first.access_token(), pair.second);
                            }
                        },
                        throwable -> {
                            Log.d(GitHubOAuth.TAG, "Presenter: consume " + throwable);
                            if (mOAuthView != null) {
                                if (throwable instanceof GitHubError) {
                                    mOAuthView.authFail(GitHubOAuth.ERROR_API_FAIL,
                                            ((GitHubError) throwable).error());
                                } else {
                                    unknownFailure();
                                }
                            }
                        });
        mDisposable.add(disposable);
    }

    public void destroy() {
        mDisposable.dispose();
    }

    public void waitCode(Subject<Pair<OAuthResult, String>> oAuthResultSubject) {
        Disposable disposable = Observable.merge(
                Observable.just(STOP_WAITING)
                        .delay(WAIT_CODE_TIMEOUT_SECONDS, TimeUnit.SECONDS),
                oAuthResultSubject)
                .take(1)
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(pair -> {
                    if (mOAuthView != null) {
                        if (pair == STOP_WAITING) {
                            mOAuthView.authFail(GitHubOAuth.ERROR_UNKNOWN_ERROR,
                                    "Code didn't arrived in time.");
                        } else if (!TextUtils.isEmpty(pair.second)) {
                            mOAuthView.authFail(GitHubOAuth.ERROR_OAUTH_FAIL, pair.second);
                        } else {
                            mOAuthView.codeArrived(pair.first);
                            getAuthInfo(pair.first.code(), pair.first.state());
                        }
                    }
                }, throwable -> {
                    if (mOAuthView != null) {
                        unknownFailure();
                    }
                });
        mDisposable.add(disposable);
    }

    private void unknownFailure() {
        mOAuthView.authFail(GitHubOAuth.ERROR_UNKNOWN_ERROR, "Auth fail for unknown reason.");
    }
}
