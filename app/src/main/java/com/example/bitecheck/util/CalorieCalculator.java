package com.example.bitecheck.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/** Daily calorie target via Mifflin-St Jeor BMR x activity factor +/- goal offset. */
public final class CalorieCalculator {

    private static final double[] ACTIVITY_FACTORS = {1.2, 1.375, 1.55, 1.725, 1.9};
    private static final int GOAL_OFFSET = 500;
    private static final int MIN_TARGET = 1200;

    private CalorieCalculator() {
    }

    public static int dailyTarget(String gender, int ageYears, double heightCm,
                                  double weightKg, int activityLevel, String goal) {
        double bmr = 10 * weightKg + 6.25 * heightCm - 5 * ageYears;
        bmr += "Male".equalsIgnoreCase(gender) ? 5 : -161;

        int level = Math.max(0, Math.min(activityLevel, ACTIVITY_FACTORS.length - 1));
        double maintenance = bmr * ACTIVITY_FACTORS[level];

        if ("lose".equals(goal)) {
            maintenance -= GOAL_OFFSET;
        } else if ("gain".equals(goal)) {
            maintenance += GOAL_OFFSET;
        }
        return Math.max(MIN_TARGET, (int) Math.round(maintenance));
    }

    /** Age in whole years from a dd/MM/yyyy date-of-birth string; -1 if unparseable. */
    public static int ageFromDob(String dob) {
        try {
            SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy", Locale.US);
            Date birthDate = format.parse(dob);
            if (birthDate == null) {
                return -1;
            }
            Calendar birth = Calendar.getInstance();
            birth.setTime(birthDate);
            Calendar now = Calendar.getInstance();
            int age = now.get(Calendar.YEAR) - birth.get(Calendar.YEAR);
            if (now.get(Calendar.DAY_OF_YEAR) < birth.get(Calendar.DAY_OF_YEAR)) {
                age--;
            }
            return age;
        } catch (ParseException e) {
            return -1;
        }
    }
}
