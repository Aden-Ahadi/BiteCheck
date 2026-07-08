package com.example.bitecheck.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;

import com.example.bitecheck.R;
import com.example.bitecheck.data.local.MealDao;
import com.example.bitecheck.data.local.ProfileDao;
import com.example.bitecheck.data.remote.GeminiClient;
import com.example.bitecheck.model.Meal;
import com.example.bitecheck.model.UserProfile;
import com.example.bitecheck.util.DateUtil;
import com.example.bitecheck.util.FoodMatcher;
import com.example.bitecheck.util.NetworkUtil;
import com.example.bitecheck.util.SessionManager;
import com.example.bitecheck.util.ThinkingWords;
import com.google.android.material.textfield.TextInputEditText;

import java.util.List;
import java.util.Locale;

/**
 * Chat meal logging — the main feature of BiteCheck (hence "MainActivity").
 * Natural-language messages are parsed by Gemini (or the offline keyword
 * matcher), confirmed with the user, then saved like any other meal.
 */
public class MainActivity extends BaseTypingActivity {

    private ChatAdapter chatAdapter;
    private RecyclerView recycler;
    private TextInputEditText input;
    private MealDao mealDao;
    private String userId;
    private final GeminiClient gemini = new GeminiClient();
    private final ThinkingWords thinkingWords = new ThinkingWords();

    /** Set while the bot is waiting for a quantity answer (one round max). */
    private String pendingMealText;
    private String pendingQuestion;

    @Override
    protected int getContentLayoutId() {
        return R.layout.content_chat;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mealDao = new MealDao(this);
        SessionManager session = new SessionManager(this);
        userId = session.getUserId() != null ? session.getUserId() : "local";

        chatAdapter = new ChatAdapter();
        recycler = findViewById(R.id.recycler_chat);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(chatAdapter);
        chatAdapter.add(getString(R.string.chat_welcome), false);

        input = findViewById(R.id.input_chat);
        findViewById(R.id.btn_send).setOnClickListener(v -> send());
    }

    private void send() {
        String text = input.getText() == null ? "" : input.getText().toString().trim();
        if (text.isEmpty()) {
            return;
        }
        input.setText("");
        chatAdapter.add(text, true);
        int thinkingPos = chatAdapter.add(getString(R.string.chat_thinking), false);
        scrollToBottom();

        // If the bot just asked for a quantity, fold the answer into the
        // original message so the model sees the full picture in one shot.
        boolean isFollowUp = pendingMealText != null;
        String textToParse = isFollowUp
                ? "The user said: \"" + pendingMealText + "\". You asked: \""
                        + pendingQuestion + "\". They answered: \"" + text
                        + "\". Log the full meal now."
                : text;
        pendingMealText = null;
        pendingQuestion = null;

        if (NetworkUtil.isOnline(this) && GeminiClient.isConfigured()) {
            thinkingWords.start(word -> chatAdapter.replace(thinkingPos, word));
            gemini.parseMeal(textToParse, new GeminiClient.ParseCallback() {
                @Override
                public void onSuccess(List<GeminiClient.ParsedFood> foods) {
                    handleParsedFoods(foods, thinkingPos, false);
                }

                @Override
                public void onQuestion(String question) {
                    thinkingWords.stop();
                    if (isFollowUp) {
                        // One round only — don't let the model interrogate.
                        handleParsedFoods(FoodMatcher.match(MainActivity.this, text),
                                thinkingPos, true);
                        return;
                    }
                    pendingMealText = text;
                    pendingQuestion = question;
                    chatAdapter.replace(thinkingPos, question);
                    scrollToBottom();
                }

                @Override
                public void onError(String message) {
                    // AI unavailable — degrade to the local keyword matcher.
                    handleParsedFoods(FoodMatcher.match(MainActivity.this, text),
                            thinkingPos, true);
                }
            });
        } else {
            handleParsedFoods(FoodMatcher.match(this, text), thinkingPos, true);
        }
    }

    private void handleParsedFoods(List<GeminiClient.ParsedFood> foods,
                                   int thinkingPos, boolean offline) {
        thinkingWords.stop();
        if (foods.isEmpty()) {
            chatAdapter.replace(thinkingPos, getString(R.string.chat_no_foods));
            scrollToBottom();
            return;
        }

        int total = 0;
        for (GeminiClient.ParsedFood food : foods) {
            total += food.calories;
        }

        chatAdapter.replace(thinkingPos, getString(R.string.chat_found_foods));
        scrollToBottom();
        showConfirmSheet(foods, total, offline);
    }

    /** Confirmation as a Material bottom sheet instead of a centered dialog. */
    private void showConfirmSheet(List<GeminiClient.ParsedFood> foods,
                                  int total, boolean offline) {
        BottomSheetDialog sheet =
                new BottomSheetDialog(this, R.style.Theme_BiteCheck_BottomSheet);
        View view = getLayoutInflater().inflate(R.layout.sheet_confirm_meal, null);

        LinearLayout container = view.findViewById(R.id.container_items);
        for (GeminiClient.ParsedFood food : foods) {
            View row = getLayoutInflater()
                    .inflate(R.layout.item_confirm_food, container, false);
            ((TextView) row.findViewById(R.id.text_food)).setText(describeName(food));
            ((TextView) row.findViewById(R.id.text_kcal))
                    .setText(getString(R.string.kcal_format, food.calories));
            container.addView(row);
        }
        ((TextView) view.findViewById(R.id.text_total))
                .setText(getString(R.string.kcal_format, total));

        view.findViewById(R.id.btn_sheet_log).setOnClickListener(v -> {
            saveMeals(foods);
            int remaining = remainingToday();
            String reply = getString(R.string.chat_logged_format,
                    foods.size(), total, Math.max(0, remaining));
            if (offline) {
                reply += "\n" + getString(R.string.chat_offline_note);
            }
            chatAdapter.add(reply, false);
            scrollToBottom();
            sheet.dismiss();
        });
        // Cancel button and swipe/back both route through cancel().
        view.findViewById(R.id.btn_sheet_cancel).setOnClickListener(v -> sheet.cancel());
        sheet.setOnCancelListener(d -> {
            chatAdapter.add(getString(R.string.chat_not_logged), false);
            scrollToBottom();
        });

        sheet.setContentView(view);
        sheet.show();
    }

    private void saveMeals(List<GeminiClient.ParsedFood> foods) {
        for (GeminiClient.ParsedFood food : foods) {
            Meal meal = new Meal();
            meal.userId = userId;
            meal.food = food.food;
            meal.quantity = food.quantity;
            meal.unit = food.unit;
            meal.mealType = FoodMatcher.inferMealType(food.mealType);
            meal.calories = food.calories;
            meal.loggedAt = DateUtil.now();
            meal.synced = false;
            mealDao.insert(meal);
        }
    }

    private int remainingToday() {
        UserProfile profile = new ProfileDao(this).get(userId);
        int target = profile != null ? profile.calorieTarget : 2000;
        return target - mealDao.caloriesForDay(userId, DateUtil.today());
    }

    /** "2 pcs French fries" — quantity, unit and name without the kcal. */
    private String describeName(GeminiClient.ParsedFood food) {
        String quantity = food.quantity == Math.floor(food.quantity)
                ? String.valueOf((long) food.quantity) : String.valueOf(food.quantity);
        return String.format(Locale.US, "%s %s %s",
                quantity, food.unit, food.food).trim().replaceAll("\\s+", " ");
    }

    private void scrollToBottom() {
        recycler.post(() ->
                recycler.smoothScrollToPosition(chatAdapter.getItemCount() - 1));
    }

    @Override
    protected void onDestroy() {
        thinkingWords.stop();
        super.onDestroy();
    }
}
