package com.example.bitecheck.util;

import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;

import com.example.bitecheck.data.local.BiteCheckDbHelper;
import com.example.bitecheck.data.local.MealDao;
import com.example.bitecheck.data.local.ProfileDao;
import com.example.bitecheck.data.local.WaterDao;
import com.example.bitecheck.data.local.WeightDao;
import com.example.bitecheck.data.remote.SupabaseDb;
import com.example.bitecheck.model.Meal;
import com.example.bitecheck.model.UserProfile;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/** Handles two-way synchronization between local SQLite and Supabase. */
public final class SyncManager {

    public static final String ACTION_SYNC_COMPLETE = "com.example.bitecheck.SYNC_COMPLETE";

    private SyncManager() {
    }

    /** Triggers a full push/pull sync. Call this on app resume. */
    public static void sync(Context context) {
        pushAll(context);
        pullAll(context);
    }

    /** Cleans up duplicate records caused by ID mismatches during sync. */
    private static void deduplicate(Context context) {
        try (BiteCheckDbHelper helper = new BiteCheckDbHelper(context)) {
            SQLiteDatabase db = helper.getWritableDatabase();
            // Delete duplicate meals (same user, food, and time)
            db.execSQL("DELETE FROM meals WHERE id NOT IN (SELECT MIN(id) FROM meals GROUP BY user_id, food, logged_at)");
            // Delete duplicate water logs
            db.execSQL("DELETE FROM water_logs WHERE id NOT IN (SELECT MIN(id) FROM water_logs GROUP BY user_id, amount_ml, logged_at)");
            // Delete duplicate weight logs
            db.execSQL("DELETE FROM weight_logs WHERE id NOT IN (SELECT MIN(id) FROM weight_logs GROUP BY user_id, weight_kg, logged_at)");
        } catch (Exception ignored) {}
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
            row.addProperty("uuid", meal.uuid);
            row.addProperty("user_id", meal.userId);
            row.addProperty("food", meal.food);
            row.addProperty("quantity", meal.quantity);
            row.addProperty("unit", meal.unit);
            row.addProperty("meal_type", meal.mealType);
            row.addProperty("calories", meal.calories);
            row.addProperty("logged_at", meal.loggedAt);
            long id = meal.id;
            SupabaseDb.upsert("meals", "uuid", row, token, (ok, error) -> {
                if (ok) {
                    mealDao.markSynced(id);
                }
            });
        }

        WaterDao waterDao = new WaterDao(app);
        for (WaterDao.Row water : waterDao.unsynced(userId)) {
            JsonObject row = new JsonObject();
            row.addProperty("uuid", water.uuid);
            row.addProperty("user_id", userId);
            row.addProperty("amount_ml", water.amountMl);
            row.addProperty("logged_at", water.loggedAt);
            long id = water.id;
            SupabaseDb.upsert("water_logs", "uuid", row, token, (ok, error) -> {
                if (ok) {
                    waterDao.markSynced(id);
                }
            });
        }

        WeightDao weightDao = new WeightDao(app);
        for (WeightDao.Row weight : weightDao.unsynced(userId)) {
            JsonObject row = new JsonObject();
            row.addProperty("uuid", weight.uuid);
            row.addProperty("user_id", userId);
            row.addProperty("weight_kg", weight.weightKg);
            row.addProperty("logged_at", weight.loggedAt);
            long id = weight.id;
            SupabaseDb.upsert("weight_logs", "uuid", row, token, (ok, error) -> {
                if (ok) {
                    weightDao.markSynced(id);
                }
            });
        }
    }

    public static void pullAll(Context context) {
        Context app = context.getApplicationContext();
        SessionManager session = new SessionManager(app);
        String userId = session.getUserId();
        String token = session.getAccessToken();
        if (!NetworkUtil.isOnline(app) || userId == null || token == null) {
            return;
        }

        // Pull Meals
        SupabaseDb.selectAll("meals", "user_id", userId, token, rows -> {
            if (rows != null) {
                MealDao dao = new MealDao(app);
                for (JsonElement el : rows) {
                    JsonObject row = el.getAsJsonObject();
                    Meal m = new Meal();
                    m.uuid = str(row, "uuid");
                    m.userId = str(row, "user_id");
                    m.food = str(row, "food");
                    m.quantity = num(row, "quantity");
                    m.unit = str(row, "unit");
                    m.mealType = str(row, "meal_type");
                    m.calories = (int) num(row, "calories");
                    m.loggedAt = DateUtil.normalize(str(row, "logged_at"));
                    m.synced = true;
                    dao.insert(m);
                }
                finishSync(app);
            }
        });

        // Pull Water
        SupabaseDb.selectAll("water_logs", "user_id", userId, token, rows -> {
            if (rows != null) {
                WaterDao dao = new WaterDao(app);
                for (JsonElement el : rows) {
                    JsonObject row = el.getAsJsonObject();
                    dao.insert(str(row, "user_id"), str(row, "uuid"), 
                            (int) num(row, "amount_ml"), DateUtil.normalize(str(row, "logged_at")), true);
                }
                finishSync(app);
            }
        });

        // Pull Weight
        SupabaseDb.selectAll("weight_logs", "user_id", userId, token, rows -> {
            if (rows != null) {
                WeightDao dao = new WeightDao(app);
                for (JsonElement el : rows) {
                    JsonObject row = el.getAsJsonObject();
                    dao.insert(str(row, "user_id"), str(row, "uuid"), 
                            num(row, "weight_kg"), DateUtil.normalize(str(row, "logged_at")), true);
                }
                finishSync(app);
            }
        });
    }

    private static void finishSync(Context app) {
        deduplicate(app);
        app.sendBroadcast(new Intent(ACTION_SYNC_COMPLETE));
    }

    private static String str(JsonObject obj, String key) {
        return obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsString() : "";
    }

    private static double num(JsonObject obj, String key) {
        return obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsDouble() : 0;
    }
}
