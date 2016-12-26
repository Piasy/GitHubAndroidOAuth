package com.github.piasy.oauth3.github.model;

import android.text.TextUtils;
import com.google.auto.value.AutoValue;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import io.reactivex.Observable;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.Headers;
import retrofit2.http.POST;

/**
 * Created by Piasy{github.com/Piasy} on 26/12/2016.
 */

public interface GitHubApi {

    String BASE_URL = "https://github.com/";

    @Headers({ "Accept: application/json" })
    @POST("login/oauth/access_token")
    @FormUrlEncoded
    Observable<GitHubToken> accessToken(@Field("client_id") String clientId,
            @Field("client_secret") String clientSecret, @Field("code") String code,
            @Field("state") String state);

    @AutoValue
    abstract class GitHubToken {

        public static TypeAdapter<GitHubToken> typeAdapter(final Gson gson) {
            return new AutoValue_GitHubApi_GitHubToken.GsonTypeAdapter(gson);
        }

        public abstract String access_token();

        public abstract String token_type();

        public abstract String scope();
    }

    @AutoValue
    abstract class GitHubError extends ApiErrorAwareConverterFactory.ApiError {

        public static TypeAdapter<GitHubError> typeAdapter(final Gson gson) {
            return new AutoValue_GitHubApi_GitHubError.GsonTypeAdapter(gson);
        }

        public abstract String error();

        public abstract String error_description();

        public abstract String error_uri();

        @Override
        public boolean valid() {
            return !TextUtils.isEmpty(error());
        }
    }
}
