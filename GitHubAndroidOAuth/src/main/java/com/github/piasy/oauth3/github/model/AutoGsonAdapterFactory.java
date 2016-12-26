package com.github.piasy.oauth3.github.model;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;

import com.ryanharter.auto.value.gson.GsonTypeAdapterFactory;

/**
 * Created by Piasy{github.com/Piasy} on 26/12/2016.
 */
@GsonTypeAdapterFactory
public abstract class AutoGsonAdapterFactory implements TypeAdapterFactory {

    public static TypeAdapterFactory create() {
        final TypeAdapterFactory factory = new AutoValueGson_AutoGsonAdapterFactory();

        return new TypeAdapterFactory() {
            @Override
            public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
                TypeAdapter<T> typeAdapter = factory.create(gson, type);
                return typeAdapter != null ? typeAdapter.nullSafe() : null;
            }
        };
    }
}
