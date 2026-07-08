package com.example.bitecheck.model;

public class UserProfile {

    public String userId;
    public String name;
    public String email;
    public String gender;          // "Male" / "Female"
    public String dob;             // dd/MM/yyyy
    public double heightCm;
    public double weightKg;
    public String goal;            // "lose" / "maintain" / "gain"
    public int activityLevel;      // 0..4 (sedentary..extra active)
    public int calorieTarget;      // kcal/day
    public boolean synced;
}
