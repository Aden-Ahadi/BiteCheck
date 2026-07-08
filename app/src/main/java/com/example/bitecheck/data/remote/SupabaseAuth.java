package com.example.bitecheck.data.remote;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/** Supabase GoTrue auth over REST. All callbacks run on the main thread. */
public class SupabaseAuth {

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final Gson GSON = new Gson();

    /** Result of a successful sign-in (or sign-up with auto-confirm enabled). */
    public static class Session {
        public String userId;
        public String email;
        public String accessToken;
        public String refreshToken;
    }

    public interface AuthCallback {
        /** session is null when sign-up succeeded but email confirmation is pending. */
        void onSuccess(@Nullable Session session);

        void onError(String message);
    }

    public interface HealthCallback {
        void onResult(boolean reachable);
    }

    /** GET /auth/v1/health — used by the pre-login connection check. */
    public void checkHealth(HealthCallback callback) {
        Request request = SupabaseClient.request("/auth/v1/health").get().build();
        SupabaseClient.http().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                MAIN.post(() -> callback.onResult(false));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                boolean ok = response.isSuccessful();
                response.close();
                MAIN.post(() -> callback.onResult(ok));
            }
        });
    }

    public void signIn(String email, String password, AuthCallback callback) {
        JsonObject body = new JsonObject();
        body.addProperty("email", email);
        body.addProperty("password", password);
        Request request = SupabaseClient.request("/auth/v1/token?grant_type=password")
                .post(RequestBody.create(GSON.toJson(body), JSON))
                .build();
        enqueueAuth(request, callback);
    }

    public void signUp(String email, String password, String fullName, AuthCallback callback) {
        JsonObject data = new JsonObject();
        data.addProperty("full_name", fullName);
        JsonObject body = new JsonObject();
        body.addProperty("email", email);
        body.addProperty("password", password);
        body.add("data", data);
        Request request = SupabaseClient.request("/auth/v1/signup")
                .post(RequestBody.create(GSON.toJson(body), JSON))
                .build();
        enqueueAuth(request, callback);
    }

    public void recoverPassword(String email, AuthCallback callback) {
        JsonObject body = new JsonObject();
        body.addProperty("email", email);
        Request request = SupabaseClient.request("/auth/v1/recover")
                .post(RequestBody.create(GSON.toJson(body), JSON))
                .build();
        SupabaseClient.http().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                MAIN.post(() -> callback.onError(e.getMessage()));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String responseBody = response.body() != null ? response.body().string() : "";
                if (response.isSuccessful()) {
                    MAIN.post(() -> callback.onSuccess(null));
                } else {
                    String message = extractError(responseBody, response.code());
                    MAIN.post(() -> callback.onError(message));
                }
            }
        });
    }

    private void enqueueAuth(Request request, AuthCallback callback) {
        SupabaseClient.http().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                MAIN.post(() -> callback.onError(e.getMessage()));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String responseBody = response.body() != null ? response.body().string() : "";
                if (!response.isSuccessful()) {
                    String message = extractError(responseBody, response.code());
                    MAIN.post(() -> callback.onError(message));
                    return;
                }
                Session session = parseSession(responseBody);
                MAIN.post(() -> callback.onSuccess(session));
            }
        });
    }

    /** Returns null when the response has no session (email confirmation pending). */
    @Nullable
    private Session parseSession(String body) {
        try {
            JsonObject json = JsonParser.parseString(body).getAsJsonObject();
            if (!json.has("access_token")) {
                return null;
            }
            Session session = new Session();
            session.accessToken = json.get("access_token").getAsString();
            session.refreshToken = json.has("refresh_token")
                    ? json.get("refresh_token").getAsString() : null;
            JsonObject user = json.getAsJsonObject("user");
            if (user != null) {
                session.userId = user.get("id").getAsString();
                session.email = user.has("email") ? user.get("email").getAsString() : null;
            }
            return session;
        } catch (RuntimeException e) {
            return null;
        }
    }

    private String extractError(String body, int code) {
        try {
            JsonObject json = JsonParser.parseString(body).getAsJsonObject();
            for (String key : new String[]{"error_description", "msg", "message", "error"}) {
                if (json.has(key) && json.get(key).isJsonPrimitive()) {
                    return json.get(key).getAsString();
                }
            }
        } catch (RuntimeException ignored) {
            // fall through to generic message
        }
        return "Request failed (HTTP " + code + ")";
    }
}
