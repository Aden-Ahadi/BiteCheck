package com.example.bitecheck.data.remote;

import com.example.bitecheck.BuildConfig;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;

/** Shared OkHttp client + request scaffolding for all Supabase calls. */
public final class SupabaseClient {

    public static final String BASE_URL = BuildConfig.SUPABASE_URL;
    public static final String ANON_KEY = BuildConfig.SUPABASE_ANON_KEY;

    private static OkHttpClient client;

    private SupabaseClient() {
    }

    public static synchronized OkHttpClient http() {
        if (client == null) {
            client = new OkHttpClient.Builder()
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .build();
        }
        return client;
    }

    /** Request builder with the anon API key attached. */
    public static Request.Builder request(String path) {
        return new Request.Builder()
                .url(BASE_URL + path)
                .header("apikey", ANON_KEY);
    }

    /** Request builder authorized as the logged-in user (for PostgREST). */
    public static Request.Builder authorizedRequest(String path, String accessToken) {
        return request(path)
                .header("Authorization", "Bearer " + accessToken);
    }
}
