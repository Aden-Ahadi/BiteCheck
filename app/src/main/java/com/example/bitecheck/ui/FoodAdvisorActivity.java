package com.example.bitecheck.ui;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.example.bitecheck.R;
import com.example.bitecheck.data.local.MealDao;
import com.example.bitecheck.data.local.ProfileDao;
import com.example.bitecheck.data.remote.GeminiClient;
import com.example.bitecheck.model.UserProfile;
import com.example.bitecheck.util.DateUtil;
import com.example.bitecheck.util.NetworkUtil;
import com.example.bitecheck.util.SessionManager;
import com.example.bitecheck.util.ThinkingWords;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;

import java.io.ByteArrayOutputStream;
import java.util.Locale;

/** "Should I eat this?" — text and/or photo, judged against today's budget. */
public class FoodAdvisorActivity extends BaseTypingActivity {

    private static final int REQUEST_PHOTO = 71;

    private final GeminiClient gemini = new GeminiClient();
    private final ThinkingWords thinkingWords = new ThinkingWords();
    private Bitmap photo;
    private MaterialButton askButton;

    @Override
    protected int getContentLayoutId() {
        return R.layout.content_advisor;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        askButton = findViewById(R.id.btn_ask_advisor);

        findViewById(R.id.btn_photo).setOnClickListener(v -> {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (intent.resolveActivity(getPackageManager()) != null) {
                // Thumbnail-size capture: plenty for the vision model, no file plumbing.
                startActivityForResult(intent, REQUEST_PHOTO);
            } else {
                Toast.makeText(this, R.string.advisor_no_camera, Toast.LENGTH_SHORT).show();
            }
        });

        askButton.setOnClickListener(v -> ask());

        findViewById(R.id.btn_reset_advisor).setOnClickListener(v -> resetUI());
    }

    private void resetUI() {
        photo = null;
        findViewById(R.id.image_meal_preview).setVisibility(View.GONE);
        findViewById(R.id.card_advice).setVisibility(View.GONE);
        TextInputEditText input = findViewById(R.id.input_advisor);
        input.setText("");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_PHOTO && resultCode == RESULT_OK && data != null
                && data.getExtras() != null) {
            photo = (Bitmap) data.getExtras().get("data");
            ImageView preview = findViewById(R.id.image_meal_preview);
            preview.setImageBitmap(photo);
            preview.setVisibility(View.VISIBLE);
        }
    }

    private void ask() {
        TextInputEditText input = findViewById(R.id.input_advisor);
        String text = input.getText() == null ? "" : input.getText().toString().trim();
        if (text.isEmpty() && photo == null) {
            Toast.makeText(this, R.string.advisor_need_input, Toast.LENGTH_SHORT).show();
            return;
        }

        // Dismiss the keyboard
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null && getCurrentFocus() != null) {
            imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        }

        if (!NetworkUtil.isOnline(this)) {
            Toast.makeText(this, R.string.error_no_internet, Toast.LENGTH_LONG).show();
            return;
        }
        if (!GeminiClient.isConfigured()) {
            Toast.makeText(this, R.string.advisor_no_key, Toast.LENGTH_LONG).show();
            return;
        }

        SessionManager session = new SessionManager(this);
        String userId = session.getUserId() != null ? session.getUserId() : "local";
        UserProfile profile = new ProfileDao(this).get(userId);
        int target = profile != null ? profile.calorieTarget : 2000;
        int remaining = target - new MealDao(this).caloriesForDay(userId, DateUtil.today());
        String goal = profile != null ? profile.goal : "maintain";

        askButton.setEnabled(false);
        thinkingWords.start(word -> showAdvice(word, R.color.bc_text_secondary));

        gemini.advise(text, photoAsBase64(), remaining, goal,
                new GeminiClient.AdviceCallback() {
                    @Override
                    public void onSuccess(GeminiClient.Advice advice) {
                        resetButton();
                        showVerdict(advice);
                    }

                    @Override
                    public void onError(String message) {
                        resetButton();
                        showAdvice(getString(R.string.advisor_error, message),
                                R.color.bc_score_bad);
                    }
                });
    }

    private void showVerdict(GeminiClient.Advice advice) {
        String headline;
        int color;
        switch (advice.verdict) {
            case "yes":
                headline = getString(R.string.advisor_verdict_yes);
                color = R.color.bc_score_excellent;
                break;
            case "careful":
                headline = getString(R.string.advisor_verdict_careful);
                color = R.color.bc_score_poor;
                break;
            default:
                headline = getString(R.string.advisor_verdict_no);
                color = R.color.bc_score_bad;
                break;
        }
        String body = String.format(Locale.US, "%s\n\n≈ %d kcal\n\n%s",
                headline, advice.estimatedCalories, advice.reason);
        showAdvice(body, color);
    }

    private void showAdvice(String text, int colorRes) {
        MaterialCardView card = findViewById(R.id.card_advice);
        TextView adviceText = findViewById(R.id.text_advice);
        View resetBtn = findViewById(R.id.btn_reset_advisor);

        card.setVisibility(View.VISIBLE);
        adviceText.setText(text);
        adviceText.setTextColor(ContextCompat.getColor(this, colorRes));

        // Only show the reset button if we are NOT currently thinking (rotating words)
        resetBtn.setVisibility(thinkingWords.isStarted() ? View.GONE : View.VISIBLE);
    }

    private void resetButton() {
        thinkingWords.stop();
        askButton.setEnabled(true);
        askButton.setText(R.string.action_ask_advisor);
    }

    @Override
    protected void onDestroy() {
        thinkingWords.stop();
        super.onDestroy();
    }

    private String photoAsBase64() {
        if (photo == null) {
            return null;
        }
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        photo.compress(Bitmap.CompressFormat.JPEG, 85, stream);
        return Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP);
    }
}
