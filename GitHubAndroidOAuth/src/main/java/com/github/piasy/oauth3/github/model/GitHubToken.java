package com.github.piasy.oauth3.github.model;

import com.google.auto.value.AutoValue;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;

/**
 * Created by Piasy{github.com/Piasy} on 26/12/2016.
 */
@AutoValue
public abstract class GitHubToken {

    public static TypeAdapter<GitHubToken> typeAdapter(final Gson gson) {
        return new AutoValue_GitHubToken.GsonTypeAdapter(gson);
    }

    public abstract String access_token();

    public abstract String token_type();

    public abstract String scope();
}
