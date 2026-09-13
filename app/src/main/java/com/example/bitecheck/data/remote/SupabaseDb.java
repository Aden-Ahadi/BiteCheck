package com.example.bitecheck.data.remote;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/** Minimal PostgREST access for Supabase tables. Callbacks run on the main thread. */
public class SupabaseDb {

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final Gson GSON = new Gson();

    public interface WriteCallback {
        void onResult(boolean success, String error);
    }

    public interface RowCallback {
        /** row is null when no match was found or the request failed. */
        void onResult(JsonObject row);
    }

    /** Fetch a single row where filterColumn equals filterValue. */
    public static void selectOne(String table, String filterColumn, String filterValue,
                                 String accessToken, RowCallback callback) {
        Request request = SupabaseClient
                .authorizedRequest("/rest/v1/" + table
                        + "?" + filterColumn + "=eq." + filterValue + "&limit=1", accessToken)
                .get()
                .build();
        SupabaseClient.http().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                MAIN.post(() -> callback.onResult(null));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                JsonObject row = null;
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JsonArray rows = JsonParser
                                .parseString(response.body().string()).getAsJsonArray();
                        if (rows.size() > 0) {
                            row = rows.get(0).getAsJsonObject();
                        }
                    } catch (RuntimeException ignored) {
                        // Malformed body: treat as not found.
                    }
                }
                response.close();
                JsonObject finalRow = row;
                MAIN.post(() -> callback.onResult(finalRow));
            }
        });
    }

    public interface RowsCallback {
        /** rows is null if the request failed. */
        void onResult(JsonArray rows);
    }

    /** Fetch rows where filterColumn equals filterValue. */
    public static void selectAll(String table, String filterColumn, String filterValue,
                                 String accessToken, RowsCallback callback) {
        Request request = SupabaseClient
                .authorizedRequest("/rest/v1/" + table
                        + "?" + filterColumn + "=eq." + filterValue, accessToken)
                .get()
                .build();
        SupabaseClient.http().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                MAIN.post(() -> callback.onResult(null));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                JsonArray rows = null;
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        rows = JsonParser.parseString(response.body().string()).getAsJsonArray();
                    } catch (RuntimeException ignored) {
                    }
                }
                response.close();
                JsonArray finalRows = rows;
                MAIN.post(() -> callback.onResult(finalRows));
            }
        });
    }

    /** Insert-or-update one row keyed by conflictColumn (e.g. "user_id"). */
    public static void upsert(String table, String conflictColumn, JsonObject row,
                              String accessToken, WriteCallback callback) {
        Request request = SupabaseClient
                .authorizedRequest("/rest/v1/" + table + "?on_conflict=" + conflictColumn,
                        accessToken)
                .header("Prefer", "resolution=merge-duplicates")
                .post(RequestBody.create(GSON.toJson(row), JSON))
                .build();
        enqueue(request, callback);
    }

    /** Delete rows where filterColumn equals filterValue. */
    public static void delete(String table, String filterColumn, String filterValue,
                              String accessToken, WriteCallback callback) {
        Request request = SupabaseClient
                .authorizedRequest("/rest/v1/" + table
                        + "?" + filterColumn + "=eq." + filterValue, accessToken)
                .delete()
                .build();
        enqueue(request, callback);
    }

    /** Plain insert of one row. */
    public static void insert(String table, JsonObject row,
                              String accessToken, WriteCallback callback) {
        Request request = SupabaseClient
                .authorizedRequest("/rest/v1/" + table, accessToken)
                .post(RequestBody.create(GSON.toJson(row), JSON))
                .build();
        enqueue(request, callback);
    }

    private static void enqueue(Request request, WriteCallback callback) {
        SupabaseClient.http().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                MAIN.post(() -> callback.onResult(false, e.getMessage()));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                boolean ok = response.isSuccessful();
                String error = null;
                if (!ok && response.body() != null) {
                    error = response.body().string();
                }
                response.close();
                String finalError = error;
                MAIN.post(() -> callback.onResult(ok, finalError));
            }
        });
    }
}
