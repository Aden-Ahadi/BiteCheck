package com.example.bitecheck.data.local;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.annotation.Nullable;

import com.example.bitecheck.model.UserProfile;

public class ProfileDao {

    private final BiteCheckDbHelper helper;

    public ProfileDao(Context context) {
        this.helper = new BiteCheckDbHelper(context.getApplicationContext());
    }

    public void upsert(UserProfile profile) {
        ContentValues values = new ContentValues();
        values.put("user_id", profile.userId);
        values.put("name", profile.name);
        values.put("email", profile.email);
        values.put("gender", profile.gender);
        values.put("dob", profile.dob);
        values.put("height_cm", profile.heightCm);
        values.put("weight_kg", profile.weightKg);
        values.put("goal", profile.goal);
        values.put("activity_level", profile.activityLevel);
        values.put("calorie_target", profile.calorieTarget);
        values.put("synced", profile.synced ? 1 : 0);
        SQLiteDatabase db = helper.getWritableDatabase();
        db.insertWithOnConflict("profile", null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    @Nullable
    public UserProfile get(String userId) {
        SQLiteDatabase db = helper.getReadableDatabase();
        try (Cursor cursor = db.query("profile", null, "user_id = ?",
                new String[]{userId}, null, null, null)) {
            if (!cursor.moveToFirst()) {
                return null;
            }
            UserProfile profile = new UserProfile();
            profile.userId = cursor.getString(cursor.getColumnIndexOrThrow("user_id"));
            profile.name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
            profile.email = cursor.getString(cursor.getColumnIndexOrThrow("email"));
            profile.gender = cursor.getString(cursor.getColumnIndexOrThrow("gender"));
            profile.dob = cursor.getString(cursor.getColumnIndexOrThrow("dob"));
            profile.heightCm = cursor.getDouble(cursor.getColumnIndexOrThrow("height_cm"));
            profile.weightKg = cursor.getDouble(cursor.getColumnIndexOrThrow("weight_kg"));
            profile.goal = cursor.getString(cursor.getColumnIndexOrThrow("goal"));
            profile.activityLevel = cursor.getInt(cursor.getColumnIndexOrThrow("activity_level"));
            profile.calorieTarget = cursor.getInt(cursor.getColumnIndexOrThrow("calorie_target"));
            profile.synced = cursor.getInt(cursor.getColumnIndexOrThrow("synced")) == 1;
            return profile;
        }
    }

    public void markSynced(String userId) {
        ContentValues values = new ContentValues();
        values.put("synced", 1);
        helper.getWritableDatabase()
                .update("profile", values, "user_id = ?", new String[]{userId});
    }
}
