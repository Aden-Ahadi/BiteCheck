package com.example.bitecheck.ui;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import com.example.bitecheck.R;
import com.example.bitecheck.data.remote.SupabaseDb;
import com.example.bitecheck.util.NetworkUtil;
import com.example.bitecheck.util.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.JsonObject;

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

    /** Shared submit logic for the feedback + complaints forms. */
    static final class FormSubmitter {

        static void wire(BaseSecondaryActivity activity, String table) {
            TextInputEditText subject = activity.findViewById(R.id.input_subject);
            TextInputEditText message = activity.findViewById(R.id.input_message);
            MaterialButton submit = activity.findViewById(R.id.btn_submit);

            submit.setOnClickListener(v -> {
                String messageText = message.getText() == null
                        ? "" : message.getText().toString().trim();
                if (messageText.isEmpty()) {
                    Toast.makeText(activity, R.string.error_required,
                            Toast.LENGTH_SHORT).show();
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
                row.addProperty("subject", subject.getText() == null
                        ? "" : subject.getText().toString().trim());
                row.addProperty("message", messageText);
                SupabaseDb.insert(table, row, session.getAccessToken(), (ok, error) -> {
                    submit.setEnabled(true);
                    if (ok) {
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

        private FormSubmitter() {
        }
    }
}
