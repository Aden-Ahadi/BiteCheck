package com.example.bitecheck.util;

import android.content.Context;

import com.example.bitecheck.data.local.MealDao;
import com.example.bitecheck.data.local.ProfileDao;
import com.example.bitecheck.data.local.WaterDao;
import com.example.bitecheck.data.local.WeightDao;
import com.example.bitecheck.data.remote.SupabaseDb;
import com.example.bitecheck.model.Meal;
import com.example.bitecheck.model.UserProfile;
import com.google.gson.JsonObject;

/**
 * Offline-first sync: pushes locally written rows (synced = 0) to Supabase.
 * Best-effort — failures leave rows unsynced for the next attempt.
 */
public final class SyncManager {

    private SyncManager() {
    }

    public static void pushAll(Context context) {
        Context app = context.getApplicationContext();
        SessionManager session = new SessionManager(app);
        String userId = session.getUserId();
        String token = session.getAccessToken();
        if (!NetworkUtil.isOnline(app) || userId == null || token == null) {
            return;
        }

        ProfileDao profileDao = new ProfileDao(app);
        UserProfile profile = profileDao.get(userId);
        if (profile != null && !profile.synced) {
            JsonObject row = new JsonObject();
            row.addProperty("user_id", profile.userId);
            row.addProperty("name", profile.name);
            row.addProperty("gender", profile.gender);
            row.addProperty("dob", profile.dob);
            row.addProperty("height_cm", profile.heightCm);
            row.addProperty("weight_kg", profile.weightKg);
            row.addProperty("goal", profile.goal);
            row.addProperty("activity_level", profile.activityLevel);
            row.addProperty("calorie_target", profile.calorieTarget);
            SupabaseDb.upsert("profiles", "user_id", row, token, (ok, error) -> {
                if (ok) {
                    profileDao.markSynced(userId);
                }
            });
        }

        MealDao mealDao = new MealDao(app);
        for (Meal meal : mealDao.unsynced(userId)) {
            JsonObject row = new JsonObject();
            row.addProperty("user_id", meal.userId);
            row.addProperty("food", meal.food);
            row.addProperty("quantity", meal.quantity);
            row.addProperty("unit", meal.unit);
            row.addProperty("meal_type", meal.mealType);
            row.addProperty("calories", meal.calories);
            row.addProperty("logged_at", meal.loggedAt);
            long id = meal.id;
            SupabaseDb.insert("meals", row, token, (ok, error) -> {
                if (ok) {
                    mealDao.markSynced(id);
                }
            });
        }

        WaterDao waterDao = new WaterDao(app);
        for (WaterDao.Row water : waterDao.unsynced(userId)) {
            JsonObject row = new JsonObject();
            row.addProperty("user_id", userId);
            row.addProperty("amount_ml", water.amountMl);
            row.addProperty("logged_at", water.loggedAt);
            long id = water.id;
            SupabaseDb.insert("water_logs", row, token, (ok, error) -> {
                if (ok) {
                    waterDao.markSynced(id);
                }
            });
        }

        WeightDao weightDao = new WeightDao(app);
        for (WeightDao.Row weight : weightDao.unsynced(userId)) {
            JsonObject row = new JsonObject();
            row.addProperty("user_id", userId);
            row.addProperty("weight_kg", weight.weightKg);
            row.addProperty("logged_at", weight.loggedAt);
            long id = weight.id;
            SupabaseDb.insert("weight_logs", row, token, (ok, error) -> {
                if (ok) {
                    weightDao.markSynced(id);
                }
            });
        }
    }
}
