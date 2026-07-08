package com.example.bitecheck.util;

import android.content.Context;
import android.content.SharedPreferences;

/** Stores the Supabase session and app state in SharedPreferences. */
public class SessionManager {

    private static final String PREFS = "bitecheck_session";
    private static final String KEY_ACCESS_TOKEN = "access_token";
    private static final String KEY_REFRESH_TOKEN = "refresh_token";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_ONBOARDED = "onboarded";
    private static final String KEY_NAME = "name";

    private final SharedPreferences prefs;

    public SessionManager(Context context) {
        prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public void saveSession(String userId, String email,
                            String accessToken, String refreshToken) {
        prefs.edit()
                .putString(KEY_USER_ID, userId)
                .putString(KEY_EMAIL, email)
                .putString(KEY_ACCESS_TOKEN, accessToken)
                .putString(KEY_REFRESH_TOKEN, refreshToken)
                .apply();
    }

    public boolean isLoggedIn() {
        return getAccessToken() != null;
    }

    public String getAccessToken() {
        return prefs.getString(KEY_ACCESS_TOKEN, null);
    }

    public String getRefreshToken() {
        return prefs.getString(KEY_REFRESH_TOKEN, null);
    }

    public String getUserId() {
        return prefs.getString(KEY_USER_ID, null);
    }

    public String getEmail() {
        return prefs.getString(KEY_EMAIL, null);
    }

    public void saveName(String name) {
        prefs.edit().putString(KEY_NAME, name).apply();
    }

    public String getName() {
        return prefs.getString(KEY_NAME, null);
    }

    /**
     * A friendly name to greet the user with: their saved name if we have one,
     * otherwise a capitalised version of their email handle (e.g.
     * "adenahadi@gmail.com" → "Adenahadi"). Never returns the app name.
     */
    public String getDisplayName() {
        String name = getName();
        if (name != null && !name.trim().isEmpty()) {
            return name.trim();
        }
        String email = getEmail();
        if (email != null && email.contains("@")) {
            String handle = email.substring(0, email.indexOf('@')).trim();
            if (!handle.isEmpty()) {
                return Character.toUpperCase(handle.charAt(0)) + handle.substring(1);
            }
        }
        return null;
    }

    public void setOnboarded(boolean onboarded) {
        prefs.edit().putBoolean(KEY_ONBOARDED, onboarded).apply();
    }

    public boolean isOnboarded() {
        return prefs.getBoolean(KEY_ONBOARDED, false);
    }

    public void clear() {
        prefs.edit().clear().apply();
    }
}
