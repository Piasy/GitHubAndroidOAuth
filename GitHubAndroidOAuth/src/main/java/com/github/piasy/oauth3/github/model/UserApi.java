package com.github.piasy.oauth3.github.model;

import io.reactivex.Observable;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;

/**
 * Created by Piasy{github.com/Piasy} on 26/12/2016.
 */

public interface UserApi {

    String BASE_URL = "https://api.github.com/";

    @GET("user")
    Observable<GitHubUser> user(@Header("Authorization") String token);
}
