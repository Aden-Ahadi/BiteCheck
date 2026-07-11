package com.example.bitecheck.util;

import android.app.Activity;

import com.example.bitecheck.R;

/**
 * Directional slide transitions between screens. A screen slides in from the
 * right when moving "forward" (or to a tab further right) and from the left when
 * moving "back", settling with a subtle overshoot bounce. Call immediately after
 * {@code startActivity(...)} or {@code finish()}.
 */
public final class NavAnim {

    private NavAnim() {
    }

    /** New screen slides in from the right over the current one, which holds. */
    public static void forward(Activity activity) {
        activity.overridePendingTransition(R.anim.slide_in_right, R.anim.hold);
    }

    /** New screen slides in from the left over the current one, which holds. */
    public static void backward(Activity activity) {
        activity.overridePendingTransition(R.anim.slide_in_left, R.anim.hold);
    }
}
