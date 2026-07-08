package com.example.bitecheck.model;

public class Meal {

    public long id;
    public String userId;
    public String food;
    public double quantity;
    public String unit;
    public String mealType;   // breakfast / lunch / dinner / snack
    public int calories;
    public String loggedAt;   // yyyy-MM-dd HH:mm:ss (local)
    public boolean synced;
}
