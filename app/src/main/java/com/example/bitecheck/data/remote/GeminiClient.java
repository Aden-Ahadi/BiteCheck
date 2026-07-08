package com.example.bitecheck.data.remote;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.bitecheck.BuildConfig;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Gemini API client for the smart features: chat meal parsing (structured JSON
 * output) and the Food Advisor (text + optional photo). Callbacks run on the
 * main thread.
 */
public class GeminiClient {

    private static final String MODEL = "gemini-2.5-flash";
    private static final String ENDPOINT =
            "https://generativelanguage.googleapis.com/v1beta/models/" + MODEL
                    + ":generateContent";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final Gson GSON = new Gson();

    public static boolean isConfigured() {
        return !BuildConfig.GEMINI_API_KEY.isEmpty();
    }

    /** One food item extracted from a chat message. */
    public static class ParsedFood {
        public String food;
        public double quantity = 1;
        public String unit = "serving";
        public String mealType = "";
        public int calories;
    }

    /** Food Advisor verdict. */
    public static class Advice {
        public String verdict;          // "yes" | "careful" | "no"
        public int estimatedCalories;
        public String reason;
    }

    public interface ParseCallback {
        void onSuccess(List<ParsedFood> foods);

        /** The model needs one clarification (e.g. quantity) before logging. */
        void onQuestion(String question);

        void onError(String message);
    }

    public interface AdviceCallback {
        void onSuccess(Advice advice);

        void onError(String message);
    }

    /** Extracts foods + calorie estimates from a natural-language meal message. */
    public void parseMeal(String userText, ParseCallback callback) {
        String systemPrompt = "You are the meal-logging engine of a calorie tracker. "
                + "Extract every food or drink the user consumed from their message. "
                + "Estimate realistic calories for each item based on the quantity. "
                + "meal_type must be one of breakfast/lunch/dinner/snack if the user "
                + "implies it, otherwise an empty string. If the message contains no "
                + "food at all, return an empty foods array. Food names must be plain "
                + "text without emojis. "
                + "If a quantity is missing AND genuinely ambiguous AND it materially "
                + "changes the calories (e.g. 'eggs' with no number, 'some rice'), do "
                + "not guess: return an empty foods array and set 'question' to ONE "
                + "short friendly question asking for the amount(s). If a sensible "
                + "default exists ('a banana', 'a plate of ugali'), just use it and "
                + "leave 'question' empty. If the message already contains an answer "
                + "to a previous question, never ask again — use your best estimate.";

        JsonObject itemSchema = new JsonObject();
        itemSchema.addProperty("type", "OBJECT");
        JsonObject props = new JsonObject();
        props.add("food", schemaOf("STRING"));
        props.add("quantity", schemaOf("NUMBER"));
        props.add("unit", schemaOf("STRING"));
        props.add("meal_type", schemaOf("STRING"));
        props.add("calories", schemaOf("INTEGER"));
        itemSchema.add("properties", props);
        JsonArray required = new JsonArray();
        required.add("food");
        required.add("calories");
        itemSchema.add("required", required);

        JsonObject foodsSchema = new JsonObject();
        foodsSchema.addProperty("type", "ARRAY");
        foodsSchema.add("items", itemSchema);

        JsonObject schema = new JsonObject();
        schema.addProperty("type", "OBJECT");
        JsonObject rootProps = new JsonObject();
        rootProps.add("foods", foodsSchema);
        rootProps.add("question", schemaOf("STRING"));
        schema.add("properties", rootProps);
        JsonArray rootRequired = new JsonArray();
        rootRequired.add("foods");
        schema.add("required", rootRequired);

        JsonObject body = buildRequest(systemPrompt, userText, null, schema);
        enqueue(body, new RawCallback() {
            @Override
            public void onText(String jsonText) {
                List<ParsedFood> foods = new ArrayList<>();
                String question;
                try {
                    JsonObject root = JsonParser.parseString(jsonText).getAsJsonObject();
                    question = root.has("question") && !root.get("question").isJsonNull()
                            ? root.get("question").getAsString().trim() : "";
                    for (JsonElement element : root.getAsJsonArray("foods")) {
                        JsonObject item = element.getAsJsonObject();
                        ParsedFood food = new ParsedFood();
                        food.food = item.get("food").getAsString();
                        food.calories = item.get("calories").getAsInt();
                        if (item.has("quantity")) {
                            food.quantity = item.get("quantity").getAsDouble();
                        }
                        if (item.has("unit") && !item.get("unit").isJsonNull()) {
                            food.unit = item.get("unit").getAsString();
                        }
                        if (item.has("meal_type") && !item.get("meal_type").isJsonNull()) {
                            food.mealType = item.get("meal_type").getAsString();
                        }
                        foods.add(food);
                    }
                } catch (RuntimeException e) {
                    MAIN.post(() -> callback.onError("Unexpected AI response"));
                    return;
                }
                if (foods.isEmpty() && !question.isEmpty()) {
                    String q = question;
                    MAIN.post(() -> callback.onQuestion(q));
                } else {
                    MAIN.post(() -> callback.onSuccess(foods));
                }
            }

            @Override
            public void onError(String message) {
                MAIN.post(() -> callback.onError(message));
            }
        });
    }

    /**
     * "Should I eat this?" — judges a meal (text and/or JPEG photo as base64)
     * against the user's remaining calories and goal.
     */
    public void advise(String mealText, @Nullable String photoBase64Jpeg,
                       int remainingCalories, String goal, AdviceCallback callback) {
        String systemPrompt = "You are a friendly nutrition advisor in a calorie "
                + "tracking app. The user asks whether they should eat a meal, "
                + "described in text and/or shown in a photo. Estimate its calories, "
                + "then give a verdict: 'yes' if it comfortably fits their remaining "
                + "budget, 'careful' if it fits but uses most of it or is easy to "
                + "overeat, 'no' if it clearly exceeds it or fights their goal. "
                + "Keep the reason to 2 short sentences, encouraging, no lecturing. "
                + "Plain text only: never use emojis or special symbols. "
                + "User's goal: " + goal + ". Remaining calories today: "
                + remainingCalories + " kcal.";

        JsonObject schema = new JsonObject();
        schema.addProperty("type", "OBJECT");
        JsonObject props = new JsonObject();
        JsonObject verdict = schemaOf("STRING");
        JsonArray verdictEnum = new JsonArray();
        verdictEnum.add("yes");
        verdictEnum.add("careful");
        verdictEnum.add("no");
        verdict.add("enum", verdictEnum);
        props.add("verdict", verdict);
        props.add("estimated_calories", schemaOf("INTEGER"));
        props.add("reason", schemaOf("STRING"));
        schema.add("properties", props);
        JsonArray required = new JsonArray();
        required.add("verdict");
        required.add("estimated_calories");
        required.add("reason");
        schema.add("required", required);

        String userText = mealText == null || mealText.isEmpty()
                ? "Here is a photo of the meal." : mealText;
        JsonObject body = buildRequest(systemPrompt, userText, photoBase64Jpeg, schema);
        enqueue(body, new RawCallback() {
            @Override
            public void onText(String jsonText) {
                try {
                    JsonObject json = JsonParser.parseString(jsonText).getAsJsonObject();
                    Advice advice = new Advice();
                    advice.verdict = json.get("verdict").getAsString();
                    advice.estimatedCalories = json.get("estimated_calories").getAsInt();
                    advice.reason = json.get("reason").getAsString();
                    MAIN.post(() -> callback.onSuccess(advice));
                } catch (RuntimeException e) {
                    MAIN.post(() -> callback.onError("Unexpected AI response"));
                }
            }

            @Override
            public void onError(String message) {
                MAIN.post(() -> callback.onError(message));
            }
        });
    }

    private JsonObject buildRequest(String systemPrompt, String userText,
                                    @Nullable String photoBase64Jpeg, JsonObject schema) {
        JsonObject systemInstruction = new JsonObject();
        systemInstruction.add("parts", partsOfText(systemPrompt));

        JsonArray parts = new JsonArray();
        JsonObject textPart = new JsonObject();
        textPart.addProperty("text", userText);
        parts.add(textPart);
        if (photoBase64Jpeg != null) {
            JsonObject inlineData = new JsonObject();
            inlineData.addProperty("mime_type", "image/jpeg");
            inlineData.addProperty("data", photoBase64Jpeg);
            JsonObject imagePart = new JsonObject();
            imagePart.add("inline_data", inlineData);
            parts.add(imagePart);
        }
        JsonObject content = new JsonObject();
        content.addProperty("role", "user");
        content.add("parts", parts);
        JsonArray contents = new JsonArray();
        contents.add(content);

        JsonObject generationConfig = new JsonObject();
        generationConfig.addProperty("response_mime_type", "application/json");
        generationConfig.add("response_schema", schema);

        JsonObject body = new JsonObject();
        body.add("system_instruction", systemInstruction);
        body.add("contents", contents);
        body.add("generationConfig", generationConfig);
        return body;
    }

    private interface RawCallback {
        void onText(String text);

        void onError(String message);
    }

    /** Overload (503) and rate-limit (429) responses usually clear within seconds. */
    private static final int MAX_RETRIES = 2;

    private void enqueue(JsonObject body, RawCallback callback) {
        enqueue(body, callback, 0);
    }

    private void enqueue(JsonObject body, RawCallback callback, int attempt) {
        Request request = new Request.Builder()
                .url(ENDPOINT)
                .header("x-goog-api-key", BuildConfig.GEMINI_API_KEY)
                .post(RequestBody.create(GSON.toJson(body), JSON))
                .build();
        SupabaseClient.http().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                callback.onError(e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String responseBody = response.body() != null ? response.body().string() : "";
                if (!response.isSuccessful()) {
                    int code = response.code();
                    if ((code == 503 || code == 429) && attempt < MAX_RETRIES) {
                        long delayMs = (attempt + 1) * 2000L;
                        MAIN.postDelayed(() -> enqueue(body, callback, attempt + 1), delayMs);
                        return;
                    }
                    if (code == 503) {
                        callback.onError("The AI is busy right now — wait a moment and try again.");
                        return;
                    }
                    callback.onError(extractError(responseBody, code));
                    return;
                }
                String text = extractText(responseBody);
                if (text == null) {
                    callback.onError("Empty AI response");
                } else {
                    callback.onText(text);
                }
            }
        });
    }

    @Nullable
    private String extractText(String responseBody) {
        try {
            JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();
            JsonArray candidates = json.getAsJsonArray("candidates");
            if (candidates == null || candidates.size() == 0) {
                return null;
            }
            JsonArray parts = candidates.get(0).getAsJsonObject()
                    .getAsJsonObject("content")
                    .getAsJsonArray("parts");
            if (parts == null || parts.size() == 0) {
                return null;
            }
            return parts.get(0).getAsJsonObject().get("text").getAsString();
        } catch (RuntimeException e) {
            return null;
        }
    }

    private String extractError(String responseBody, int code) {
        try {
            JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();
            JsonObject error = json.getAsJsonObject("error");
            if (error != null && error.has("message")) {
                return error.get("message").getAsString();
            }
        } catch (RuntimeException ignored) {
            // fall through
        }
        return "AI request failed (HTTP " + code + ")";
    }

    private static JsonObject schemaOf(String type) {
        JsonObject schema = new JsonObject();
        schema.addProperty("type", type);
        return schema;
    }

    private static JsonArray partsOfText(String text) {
        JsonObject part = new JsonObject();
        part.addProperty("text", text);
        JsonArray parts = new JsonArray();
        parts.add(part);
        return parts;
    }
}
