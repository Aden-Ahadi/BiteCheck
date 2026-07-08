package com.example.bitecheck.ui;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;

import com.example.bitecheck.R;
import com.example.bitecheck.data.local.BiteCheckDbHelper;
import com.example.bitecheck.data.local.ProfileDao;
import com.example.bitecheck.data.remote.SupabaseDb;
import com.example.bitecheck.model.UserProfile;
import com.example.bitecheck.util.CalorieCalculator;
import com.example.bitecheck.util.NetworkUtil;
import com.example.bitecheck.util.SessionManager;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

public class AccountActivity extends BaseSecondaryActivity {

    private ActivityResultLauncher<String> pickImage;
    private ActivityResultLauncher<Void> takePhoto;

    @Override
    protected int getContentLayoutId() {
        return R.layout.content_account;
    }

    @Override
    protected CharSequence getScreenTitle() {
        return getString(R.string.nav_account);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        pickImage = registerForActivityResult(
                new ActivityResultContracts.GetContent(), uri -> {
                    if (uri != null) {
                        saveAvatarFromUri(uri);
                    }
                });
        takePhoto = registerForActivityResult(
                new ActivityResultContracts.TakePicturePreview(), bitmap -> {
                    if (bitmap != null) {
                        saveAvatarBitmap(bitmap);
                    }
                });

        findViewById(R.id.avatar_container).setOnClickListener(v -> showAvatarSourceSheet());
        findViewById(R.id.row_name).setOnClickListener(v -> showEditNameSheet());
        findViewById(R.id.btn_edit_profile).setOnClickListener(v -> {
            Intent intent = new Intent(this, OnboardingActivity.class);
            intent.putExtra(OnboardingActivity.EXTRA_EDIT, true);
            startActivity(intent);
        });
        findViewById(R.id.btn_logout).setOnClickListener(v -> logout());
        findViewById(R.id.btn_delete_account).setOnClickListener(v -> showDeleteSheet());
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadAvatar();
        bindProfile();
    }

    private void bindProfile() {
        SessionManager session = new SessionManager(this);
        String userId = session.getUserId() != null ? session.getUserId() : "local";
        UserProfile profile = new ProfileDao(this).get(userId);

        String displayName = profile != null && profile.name != null && !profile.name.isEmpty()
                ? profile.name : session.getDisplayName();
        ((TextView) findViewById(R.id.text_account_name))
                .setText(displayName != null ? displayName : getString(R.string.app_name));
        ((TextView) findViewById(R.id.text_account_email))
                .setText(session.getEmail() != null ? session.getEmail() : "");

        View card = findViewById(R.id.card_details);
        View noProfile = findViewById(R.id.text_no_profile);
        if (profile == null) {
            card.setVisibility(View.GONE);
            noProfile.setVisibility(View.VISIBLE);
            return;
        }
        card.setVisibility(View.VISIBLE);
        noProfile.setVisibility(View.GONE);

        setRow(R.id.row_goal, R.string.acc_goal, capitalize(profile.goal));
        setRow(R.id.row_gender, R.string.acc_gender, profile.gender);
        int age = CalorieCalculator.ageFromDob(profile.dob);
        setRow(R.id.row_age, R.string.acc_age,
                age > 0 ? getString(R.string.acc_age_format, age) : null);
        setRow(R.id.row_height, R.string.acc_height,
                getString(R.string.acc_height_format, profile.heightCm));
        setRow(R.id.row_weight, R.string.acc_weight,
                getString(R.string.weight_kg_format, profile.weightKg));
        setRow(R.id.row_target, R.string.acc_target,
                getString(R.string.kcal_format, profile.calorieTarget));
    }

    private void setRow(int rowId, int labelRes, String value) {
        View row = findViewById(rowId);
        ((TextView) row.findViewById(R.id.row_label)).setText(labelRes);
        ((TextView) row.findViewById(R.id.row_value))
                .setText(value != null && !value.isEmpty() ? value : getString(R.string.acc_dash));
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    // ---- Avatar ----

    private File avatarFile() {
        return new File(getFilesDir(), "avatar.jpg");
    }

    private void loadAvatar() {
        ShapeableImageView avatar = findViewById(R.id.img_avatar);
        File file = avatarFile();
        if (file.exists()) {
            Bitmap bmp = BitmapFactory.decodeFile(file.getAbsolutePath());
            if (bmp != null) {
                avatar.setPadding(0, 0, 0, 0);
                avatar.setImageTintList(null);
                avatar.setScaleType(ImageView.ScaleType.CENTER_CROP);
                avatar.setImageBitmap(bmp);
            }
        }
    }

    private void showAvatarSourceSheet() {
        BottomSheetDialog sheet =
                new BottomSheetDialog(this, R.style.Theme_BiteCheck_BottomSheet);
        View view = getLayoutInflater().inflate(R.layout.sheet_avatar_source, null);
        view.findViewById(R.id.opt_take_photo).setOnClickListener(v -> {
            sheet.dismiss();
            takePhoto.launch(null);
        });
        view.findViewById(R.id.opt_gallery).setOnClickListener(v -> {
            sheet.dismiss();
            pickImage.launch("image/*");
        });
        View remove = view.findViewById(R.id.opt_remove);
        remove.setVisibility(avatarFile().exists() ? View.VISIBLE : View.GONE);
        remove.setOnClickListener(v -> {
            sheet.dismiss();
            removeAvatar();
        });
        sheet.setContentView(view);
        sheet.show();
    }

    private void removeAvatar() {
        File file = avatarFile();
        if (file.exists()) {
            file.delete();
        }
        ShapeableImageView avatar = findViewById(R.id.img_avatar);
        int pad = Math.round(getResources().getDisplayMetrics().density * 26);
        avatar.setImageTintList(ColorStateList.valueOf(
                ContextCompat.getColor(this, R.color.bc_primary)));
        avatar.setScaleType(ImageView.ScaleType.FIT_CENTER);
        avatar.setPadding(pad, pad, pad, pad);
        avatar.setImageResource(R.drawable.ic_person);
    }

    private void saveAvatarFromUri(Uri uri) {
        try (InputStream in = getContentResolver().openInputStream(uri)) {
            Bitmap bmp = BitmapFactory.decodeStream(in);
            if (bmp != null) {
                saveAvatarBitmap(bmp);
            }
        } catch (IOException e) {
            Toast.makeText(this, R.string.msg_submit_failed, Toast.LENGTH_SHORT).show();
        }
    }

    private void saveAvatarBitmap(Bitmap bmp) {
        int max = 512;
        int longest = Math.max(bmp.getWidth(), bmp.getHeight());
        if (longest > max) {
            float scale = max / (float) longest;
            bmp = Bitmap.createScaledBitmap(bmp,
                    Math.round(bmp.getWidth() * scale),
                    Math.round(bmp.getHeight() * scale), true);
        }
        try (FileOutputStream fos = new FileOutputStream(avatarFile())) {
            bmp.compress(Bitmap.CompressFormat.JPEG, 90, fos);
        } catch (IOException e) {
            Toast.makeText(this, R.string.msg_submit_failed, Toast.LENGTH_SHORT).show();
            return;
        }
        loadAvatar();
    }

    // ---- Name ----

    private void showEditNameSheet() {
        BottomSheetDialog sheet =
                new BottomSheetDialog(this, R.style.Theme_BiteCheck_BottomSheet);
        View view = getLayoutInflater().inflate(R.layout.sheet_edit_name, null);
        TextInputEditText input = view.findViewById(R.id.input_name_edit);
        SessionManager session = new SessionManager(this);
        String current = session.getName();
        if (current != null) {
            input.setText(current);
            input.setSelection(current.length());
        }
        view.findViewById(R.id.btn_cancel_name).setOnClickListener(v -> sheet.dismiss());
        view.findViewById(R.id.btn_save_name).setOnClickListener(v -> {
            String name = input.getText() == null ? "" : input.getText().toString().trim();
            if (name.isEmpty()) {
                Toast.makeText(this, R.string.error_required, Toast.LENGTH_SHORT).show();
                return;
            }
            updateName(name);
            sheet.dismiss();
        });
        sheet.setContentView(view);
        sheet.setOnShowListener(d -> {
            if (sheet.getWindow() != null) {
                sheet.getWindow().setSoftInputMode(
                        WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
            }
        });
        sheet.show();
    }

    private void updateName(String newName) {
        SessionManager session = new SessionManager(this);
        session.saveName(newName);
        String userId = session.getUserId() != null ? session.getUserId() : "local";

        ProfileDao dao = new ProfileDao(this);
        UserProfile profile = dao.get(userId);
        if (profile != null) {
            profile.name = newName;
            profile.synced = false;
            dao.upsert(profile);
        }
        if (NetworkUtil.isOnline(this) && session.getAccessToken() != null
                && session.getUserId() != null) {
            JsonObject row = new JsonObject();
            row.addProperty("user_id", userId);
            row.addProperty("name", newName);
            SupabaseDb.upsert("profiles", "user_id", row, session.getAccessToken(),
                    (success, error) -> { });
        }
        bindProfile();
        Toast.makeText(this, R.string.msg_name_updated, Toast.LENGTH_SHORT).show();
    }

    // ---- Delete account ----

    private void showDeleteSheet() {
        BottomSheetDialog sheet =
                new BottomSheetDialog(this, R.style.Theme_BiteCheck_BottomSheet);
        View view = getLayoutInflater().inflate(R.layout.sheet_delete_account, null);
        view.findViewById(R.id.btn_cancel_delete).setOnClickListener(v -> sheet.dismiss());
        view.findViewById(R.id.btn_confirm_delete).setOnClickListener(v -> {
            sheet.dismiss();
            deleteAccount();
        });
        sheet.setContentView(view);
        sheet.show();
    }

    private void deleteAccount() {
        SessionManager session = new SessionManager(this);
        String userId = session.getUserId() != null ? session.getUserId() : "local";
        String token = session.getAccessToken();

        // Best-effort cloud cleanup of the user's own rows (RLS permits this).
        if (NetworkUtil.isOnline(this) && token != null && session.getUserId() != null) {
            SupabaseDb.delete("profiles", "user_id", userId, token, (ok, err) -> { });
            SupabaseDb.delete("meals", "user_id", userId, token, (ok, err) -> { });
            SupabaseDb.delete("water_logs", "user_id", userId, token, (ok, err) -> { });
            SupabaseDb.delete("weight_logs", "user_id", userId, token, (ok, err) -> { });
        }

        new BiteCheckDbHelper(this).clearUserData(userId);
        File avatar = avatarFile();
        if (avatar.exists()) {
            avatar.delete();
        }
        session.clear();

        Toast.makeText(this, R.string.msg_account_deleted, Toast.LENGTH_LONG).show();
        Intent intent = new Intent(this, WelcomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    private void logout() {
        new SessionManager(this).clear();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}
