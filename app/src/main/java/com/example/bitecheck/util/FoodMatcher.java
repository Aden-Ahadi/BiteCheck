package com.example.bitecheck.util;

import android.content.Context;
import android.database.Cursor;

import com.example.bitecheck.data.local.BiteCheckDbHelper;
import com.example.bitecheck.data.remote.GeminiClient;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Offline fallback for chat meal logging: keyword-matches the message against
 * the seeded foods table. Understands simple quantities ("2 eggs", "two eggs").
 */
public final class FoodMatcher {

    private static final Map<String, Integer> WORD_NUMBERS = new HashMap<>();

    static {
        String[] words = {"one", "two", "three", "four", "five",
                "six", "seven", "eight", "nine", "ten"};
        for (int i = 0; i < words.length; i++) {
            WORD_NUMBERS.put(words[i], i + 1);
        }
    }

    private FoodMatcher() {
    }

    public static List<GeminiClient.ParsedFood> match(Context context, String message) {
        String text = " " + message.toLowerCase(Locale.US) + " ";
        String mealType = detectMealType(text);
        List<GeminiClient.ParsedFood> results = new ArrayList<>();

        BiteCheckDbHelper helper = new BiteCheckDbHelper(context.getApplicationContext());
        try (Cursor cursor = helper.getReadableDatabase().query("foods",
                new String[]{"name", "keywords", "calories_per_serving"},
                null, null, null, null, null)) {
            while (cursor.moveToNext()) {
                String name = cursor.getString(0);
                String[] keywords = cursor.getString(1).split(",");
                int caloriesPerServing = cursor.getInt(2);
                for (String keyword : keywords) {
                    String needle = keyword.trim();
                    int index = text.indexOf(" " + needle);
                    if (index < 0) {
                        continue;
                    }
                    double quantity = quantityBefore(text, index);
                    GeminiClient.ParsedFood food = new GeminiClient.ParsedFood();
                    food.food = name;
                    food.quantity = quantity;
                    food.unit = "serving";
                    food.mealType = mealType;
                    food.calories = (int) Math.round(caloriesPerServing * quantity);
                    results.add(food);
                    break; // one match per food entry
                }
            }
        }
        return results;
    }

    /** Meal type from an explicit mention, else inferred from the clock. */
    public static String inferMealType(String explicit) {
        if (explicit != null && !explicit.isEmpty()) {
            return explicit;
        }
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (hour < 11) {
            return "breakfast";
        }
        if (hour < 16) {
            return "lunch";
        }
        if (hour < 21) {
            return "dinner";
        }
        return "snack";
    }

    private static String detectMealType(String text) {
        for (String type : new String[]{"breakfast", "lunch", "dinner", "snack", "supper"}) {
            if (text.contains(type)) {
                return "supper".equals(type) ? "dinner" : type;
            }
        }
        return "";
    }

    /** Looks at the two words before the keyword for a count ("2", "two"). */
    private static double quantityBefore(String text, int keywordIndex) {
        String before = text.substring(0, keywordIndex).trim();
        String[] tokens = before.split("\\s+");
        for (int i = tokens.length - 1; i >= Math.max(0, tokens.length - 2); i--) {
            String token = tokens[i];
            if (WORD_NUMBERS.containsKey(token)) {
                return WORD_NUMBERS.get(token);
            }
            try {
                double value = Double.parseDouble(token);
                if (value > 0 && value < 50) {
                    return value;
                }
            } catch (NumberFormatException ignored) {
                // not a number, keep looking
            }
        }
        return 1;
    }
}
