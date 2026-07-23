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
            "Peeking in the oven", "Reading the recipe", "Sizzling",
            "Sautéing", "Folding the batter", "Garnishing", "Infusing flavors",
            "Zesting", "Kneading the dough", "Checking the macros",
            "Boiling the water", "Squeezing the lemon", "Reducing the sauce",
            "Fermenting ideas", "Brewing the response", "Chopping the herbs",
            "Checking the ripeness", "Balancing the nutrition", "Scanning the pantry",
            "Steaming", "Poaching", "Sharpening the knives", "Whipping it up",
            "Tasting the seasoning", "Setting the table", "Cleaning the greens",
            "Caramelizing", "Grilling the data", "Roasting the numbers",
            "Baking a response", "Mincing the details", "Glazing",
            "Hunting for protein", "De-glazing the logic", "Toasting the bytes",
            "Wait, did I add salt?", "Calculating the crunch", "Biting off more than I can chew",
            "Polishing the silverware", "Cracking the code eggs", "Butter-ing up the API",
            "Spicing things up", "Peeling back the layers", "Sifting the truth",
            "Mashing the inputs", "Asking the waiter", "Licking the spoon",
            "Scraping the bowl", "Sous-viding the data", "Pickling the results",
            "Sprinkling some magic", "Frothing the milk", "Flipping the pancake",
            "Waffling on it", "Crumbling the cookies", "Chilling in the fridge",
            "Rising like dough", "Checking the oven light", "Searching for a fork",
            "Summoning the avocado", "Consulting the cookbook", "Donning the apron",
            "Trying to be healthy", "Ignoring the donut", "Thinking of pizza"
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
