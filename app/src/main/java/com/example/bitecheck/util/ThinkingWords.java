package com.example.bitecheck.util;

import android.os.Handler;
import android.os.Looper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Cycles through playful food-themed status words while the AI is working,
 * Claude Code style ("Simmering…", "Marinating…"). Call start() with a
 * listener that updates the UI, and stop() when the response arrives.
 */
public final class ThinkingWords {

    public interface Listener {
        void onWord(String word);
    }

    private static final String[] WORDS = {
            "Simmering", "Marinating", "Chewing on it", "Counting calories",
            "Taste-testing", "Whisking", "Preheating", "Consulting the chef",
            "Weighing it up", "Stirring the pot", "Seasoning", "Plating up",
            "Peeking in the oven", "Reading the recipe"
    };
    private static final long INTERVAL_MS = 1600;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable tick;

    public void start(Listener listener) {
        stop();
        List<String> deck = new ArrayList<>(Arrays.asList(WORDS));
        Collections.shuffle(deck);
        tick = new Runnable() {
            private int index = 0;

            @Override
            public void run() {
                listener.onWord(deck.get(index % deck.size()) + "…");
                index++;
                handler.postDelayed(this, INTERVAL_MS);
            }
        };
        handler.post(tick);
    }

    public void stop() {
        if (tick != null) {
            handler.removeCallbacks(tick);
            tick = null;
        }
    }
}
