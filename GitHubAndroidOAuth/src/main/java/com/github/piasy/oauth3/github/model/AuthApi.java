package com.github.piasy.oauth3.github.model;

import io.reactivex.Observable;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.Headers;
import retrofit2.http.POST;

/**
 * Created by Piasy{github.com/Piasy} on 26/12/2016.
 */

public interface AuthApi {

    String BASE_URL = "https://github.com/";

    @Headers({ "Accept: application/json" })
    @POST("login/oauth/access_token")
    @FormUrlEncoded
    Observable<GitHubToken> accessToken(@Field("client_id") String clientId,
            @Field("client_secret") String clientSecret, @Field("code") String code,
            @Field("state") String state);
}
