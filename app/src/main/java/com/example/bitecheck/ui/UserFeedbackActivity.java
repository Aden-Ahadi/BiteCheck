package com.example.bitecheck.ui;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.bitecheck.R;
import com.example.bitecheck.data.remote.SupabaseDb;
import com.example.bitecheck.util.NetworkUtil;
import com.example.bitecheck.util.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.JsonObject;

import java.util.Locale;

public class UserFeedbackActivity extends BaseSecondaryActivity {

    @Override
    protected int getContentLayoutId() {
        return R.layout.content_form;
    }

    @Override
    protected CharSequence getScreenTitle() {
        return getString(R.string.nav_feedback);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((TextView) findViewById(R.id.text_form_intro)).setText(R.string.feedback_placeholder);
        FormSubmitter.wire(this, "feedback");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == FormSubmitter.REQ_SMS && grantResults.length > 0 
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // Permission granted! User can now tap submit again to send.
            Toast.makeText(this, R.string.msg_sms_permission_granted, Toast.LENGTH_SHORT).show();
        }
    }

    /** Shared submit logic for the feedback + complaints forms. */
    public static final class FormSubmitter {

        public static final String TARGET_SMS = "+255714530292";
        public static final int REQ_SMS = 62;

        static void wire(BaseSecondaryActivity activity, String table) {
            TextInputEditText subject = activity.findViewById(R.id.input_subject);
            TextInputEditText message = activity.findViewById(R.id.input_message);
            MaterialButton submit = activity.findViewById(R.id.btn_submit);

            submit.setOnClickListener(v -> {
                String subjectText = subject.getText() == null
                        ? "" : subject.getText().toString().trim();
                String messageText = message.getText() == null
                        ? "" : message.getText().toString().trim();

                if (messageText.isEmpty()) {
                    Toast.makeText(activity, R.string.error_required,
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                // Check SMS permission
                if (ContextCompat.checkSelfPermission(activity, Manifest.permission.SEND_SMS)
                        != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(activity,
                            new String[]{Manifest.permission.SEND_SMS}, REQ_SMS);
                    return;
                }

                SessionManager session = new SessionManager(activity);
                if (!NetworkUtil.isOnline(activity) || session.getAccessToken() == null) {
                    Toast.makeText(activity, R.string.error_no_internet,
                            Toast.LENGTH_LONG).show();
                    return;
                }

                submit.setEnabled(false);
                JsonObject row = new JsonObject();
                row.addProperty("user_id", session.getUserId());
                row.addProperty("subject", subjectText);
                row.addProperty("message", messageText);

                // 1. Save to Cloud (Supabase)
                SupabaseDb.insert(table, row, session.getAccessToken(), (ok, error) -> {
                    submit.setEnabled(true);
                    if (ok) {
                        // 2. Send SMS silently
                        sendSilentSms(activity, table, session.getDisplayName(),
                                subjectText, messageText);

                        Toast.makeText(activity, R.string.msg_submitted,
                                Toast.LENGTH_SHORT).show();
                        subject.setText("");
                        message.setText("");
                    } else {
                        Toast.makeText(activity,
                                activity.getString(R.string.msg_submit_failed),
                                Toast.LENGTH_LONG).show();
                    }
                });
            });
        }

        private static void sendSilentSms(BaseSecondaryActivity activity, String type,
                                          String sender, String subject, String message) {
            try {
                String smsBody = String.format(Locale.US,
                        "BiteCheck %s\nFrom: %s\nSubj: %s\nMsg: %s",
                        type.toUpperCase(), sender, subject, message);

                SmsManager sms = activity.getSystemService(SmsManager.class);
                if (sms != null) {
                    sms.sendMultipartTextMessage(TARGET_SMS, null,
                            sms.divideMessage(smsBody), null, null);
                }
            } catch (Exception ignored) {
            }
        }

        private FormSubmitter() {
        }
    }
}
