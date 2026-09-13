package com.example.bitecheck.data.local;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

import com.example.bitecheck.model.Meal;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MealDao {

    private final BiteCheckDbHelper helper;

    public MealDao(Context context) {
        this.helper = new BiteCheckDbHelper(context.getApplicationContext());
    }

    public long insert(Meal meal) {
        if (meal.uuid == null) {
            meal.uuid = UUID.randomUUID().toString();
        }
        ContentValues values = new ContentValues();
        values.put("uuid", meal.uuid);
        values.put("user_id", meal.userId);
        values.put("food", meal.food);
        values.put("quantity", meal.quantity);
        values.put("unit", meal.unit);
        values.put("meal_type", meal.mealType);
        values.put("calories", meal.calories);
        values.put("logged_at", meal.loggedAt);
        values.put("synced", meal.synced ? 1 : 0);
        return helper.getWritableDatabase().insertWithOnConflict("meals", null, values, 
                android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE);
    }

    public int caloriesForDay(String userId, String day) {
        try (Cursor cursor = helper.getReadableDatabase().rawQuery(
                "SELECT COALESCE(SUM(calories), 0) FROM meals " +
                        "WHERE user_id = ? AND logged_at LIKE ?",
                new String[]{userId, day + "%"})) {
            cursor.moveToFirst();
            return cursor.getInt(0);
        }
    }

    public List<Meal> mealsForDay(String userId, String day) {
        List<Meal> meals = new ArrayList<>();
        try (Cursor cursor = helper.getReadableDatabase().query("meals",
                null, "user_id = ? AND logged_at LIKE ?",
                new String[]{userId, day + "%"}, null, null, "logged_at ASC")) {
            while (cursor.moveToNext()) {
                meals.add(fromCursor(cursor));
            }
        }
        return meals;
    }

    public List<Meal> unsynced(String userId) {
        List<Meal> meals = new ArrayList<>();
        try (Cursor cursor = helper.getReadableDatabase().query("meals",
                null, "user_id = ? AND synced = 0",
                new String[]{userId}, null, null, "logged_at ASC")) {
            while (cursor.moveToNext()) {
                meals.add(fromCursor(cursor));
            }
        }
        return meals;
    }

    public void delete(long id) {
        helper.getWritableDatabase().delete("meals", "id = ?",
                new String[]{String.valueOf(id)});
    }

    public void markSynced(long id) {
        ContentValues values = new ContentValues();
        values.put("synced", 1);
        helper.getWritableDatabase()
                .update("meals", values, "id = ?", new String[]{String.valueOf(id)});
    }

    private Meal fromCursor(Cursor cursor) {
        Meal meal = new Meal();
        meal.id = cursor.getLong(cursor.getColumnIndexOrThrow("id"));
        meal.uuid = cursor.getString(cursor.getColumnIndexOrThrow("uuid"));
        meal.userId = cursor.getString(cursor.getColumnIndexOrThrow("user_id"));
        meal.food = cursor.getString(cursor.getColumnIndexOrThrow("food"));
        meal.quantity = cursor.getDouble(cursor.getColumnIndexOrThrow("quantity"));
        meal.unit = cursor.getString(cursor.getColumnIndexOrThrow("unit"));
        meal.mealType = cursor.getString(cursor.getColumnIndexOrThrow("meal_type"));
        meal.calories = cursor.getInt(cursor.getColumnIndexOrThrow("calories"));
        meal.loggedAt = cursor.getString(cursor.getColumnIndexOrThrow("logged_at"));
        meal.synced = cursor.getInt(cursor.getColumnIndexOrThrow("synced")) == 1;
        return meal;
    }
}
