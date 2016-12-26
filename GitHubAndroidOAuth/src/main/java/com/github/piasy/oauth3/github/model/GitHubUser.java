package com.github.piasy.oauth3.github.model;

import android.support.annotation.Nullable;
import com.google.auto.value.AutoValue;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;

/**
 * Created by Piasy{github.com/Piasy} on 26/12/2016.
 */

@AutoValue
public abstract class GitHubUser {
    public static TypeAdapter<GitHubUser> typeAdapter(final Gson gson) {
        return new AutoValue_GitHubUser.GsonTypeAdapter(gson);
    }

    public abstract String login();

    public abstract String name();

    public abstract String avatar_url();

    @Nullable
    public abstract String email();

    @Nullable
    public abstract String bio();
}
