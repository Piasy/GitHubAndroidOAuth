package com.github.piasy.oauth3.github.model;

import com.google.gson.JsonSyntaxException;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Converter;
import retrofit2.Retrofit;

/**
 * Created by Piasy{github.com/Piasy} on 03/09/2016.
 */

public class ApiErrorAwareConverterFactory extends Converter.Factory {

    private final Converter.Factory mDelegateFactory;
    private Class<? extends ApiError> mApiErrorClazz;

    public ApiErrorAwareConverterFactory(Converter.Factory delegateFactory,
            Class<? extends ApiError> apiErrorClazz) {
        mDelegateFactory = delegateFactory;
        mApiErrorClazz = apiErrorClazz;
    }

    @Override
    public Converter<ResponseBody, ?> responseBodyConverter(Type type, Annotation[] annotations,
            Retrofit retrofit) {
        final Converter<ResponseBody, ?> apiErrorConverter =
                mDelegateFactory.responseBodyConverter(mApiErrorClazz, annotations, retrofit);
        final Converter<ResponseBody, ?> delegateConverter =
                mDelegateFactory.responseBodyConverter(type, annotations, retrofit);
        return new Converter<ResponseBody, Object>() {
            @Override
            public Object convert(ResponseBody value) throws IOException {
                // read them all, then create a new ResponseBody for ApiError
                // because the response body is wrapped, we can't clone the ResponseBody correctly
                MediaType mediaType = value.contentType();
                String stringBody = value.string();
                try {
                    Object apiError = apiErrorConverter.convert(
                            ResponseBody.create(mediaType, stringBody));
                    if (apiError instanceof ApiError && ((ApiError) apiError).valid()) {
                        throw (ApiError) apiError;
                    }
                } catch (JsonSyntaxException | NullPointerException ignored) {
                    // array not error object, or critical error field is missing.
                }

                // then create a new ResponseBody for normal body
                return delegateConverter.convert(ResponseBody.create(mediaType, stringBody));
            }
        };
    }

    @Override
    public Converter<?, RequestBody> requestBodyConverter(Type type,
            Annotation[] parameterAnnotations, Annotation[] methodAnnotations, Retrofit retrofit) {
        return mDelegateFactory.requestBodyConverter(type, parameterAnnotations, methodAnnotations,
                retrofit);
    }

    public abstract static class ApiError extends RuntimeException {
        public abstract boolean valid();
    }
}
