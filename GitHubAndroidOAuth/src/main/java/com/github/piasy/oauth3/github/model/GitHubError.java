package com.github.piasy.oauth3.github.model;

import android.text.TextUtils;
import com.google.auto.value.AutoValue;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;

/**
 * Created by Piasy{github.com/Piasy} on 26/12/2016.
 */
@AutoValue
public abstract class GitHubError extends ApiErrorAwareConverterFactory.ApiError {

    public static TypeAdapter<GitHubError> typeAdapter(final Gson gson) {
        return new AutoValue_GitHubError.GsonTypeAdapter(gson);
    }

    public abstract String error();

    @Override
    public boolean valid() {
        return !TextUtils.isEmpty(error());
    }

    @Override
    public String toString() {
        return "GitHubError " + error();
    }
}
