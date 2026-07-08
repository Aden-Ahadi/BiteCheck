package com.example.bitecheck.data.local;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/** Local SQLite store — the offline-first source of truth. */
public class BiteCheckDbHelper extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "bitecheck.db";
    public static final int DATABASE_VERSION = 2;

    public BiteCheckDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE profile (" +
                "user_id TEXT PRIMARY KEY," +
                "name TEXT," +
                "email TEXT," +
                "gender TEXT," +
                "dob TEXT," +
                "height_cm REAL," +
                "weight_kg REAL," +
                "goal TEXT," +
                "activity_level TEXT," +
                "calorie_target INTEGER," +
                "synced INTEGER NOT NULL DEFAULT 0)");

        db.execSQL("CREATE TABLE meals (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "user_id TEXT NOT NULL," +
                "food TEXT NOT NULL," +
                "quantity REAL," +
                "unit TEXT," +
                "meal_type TEXT," +
                "calories INTEGER NOT NULL," +
                "logged_at TEXT NOT NULL," +
                "synced INTEGER NOT NULL DEFAULT 0)");

        db.execSQL("CREATE TABLE water_logs (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "user_id TEXT NOT NULL," +
                "amount_ml INTEGER NOT NULL," +
                "logged_at TEXT NOT NULL," +
                "synced INTEGER NOT NULL DEFAULT 0)");

        db.execSQL("CREATE TABLE weight_logs (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "user_id TEXT NOT NULL," +
                "weight_kg REAL NOT NULL," +
                "logged_at TEXT NOT NULL," +
                "synced INTEGER NOT NULL DEFAULT 0)");

        // Seeded common-food table: offline fallback for chat parsing (Phase 5).
        db.execSQL("CREATE TABLE foods (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "name TEXT NOT NULL," +
                "keywords TEXT NOT NULL," +
                "calories_per_serving INTEGER NOT NULL," +
                "serving_desc TEXT NOT NULL)");
        seedFoods(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            seedFoods(db);   // v1 created the foods table empty
        }
    }

    /** Wipe every row belonging to a user (account deletion). Keeps seeded foods. */
    public void clearUserData(String userId) {
        SQLiteDatabase db = getWritableDatabase();
        String[] args = {userId};
        db.delete("meals", "user_id = ?", args);
        db.delete("water_logs", "user_id = ?", args);
        db.delete("weight_logs", "user_id = ?", args);
        db.delete("profile", "user_id = ?", args);
    }

    /** Common foods with approximate calories — the offline chat fallback. */
    private void seedFoods(SQLiteDatabase db) {
        String[][] seeds = {
                {"Egg", "egg,eggs,boiled egg,fried egg", "78", "1 egg"},
                {"Omelette", "omelette,omelet", "155", "2-egg omelette"},
                {"Bread", "bread,slice of bread,slices of bread,toast", "80", "1 slice"},
                {"Chapati", "chapati,chapatis,roti", "120", "1 chapati"},
                {"Rice", "rice,white rice,fried rice,pilau", "205", "1 cup cooked"},
                {"Ugali", "ugali,posho,fufu", "180", "1 serving"},
                {"Beans", "beans,bean stew", "220", "1 cup"},
                {"Chicken", "chicken,chicken breast,grilled chicken,fried chicken", "230", "1 piece"},
                {"Beef", "beef,beef stew,steak,nyama", "250", "1 serving"},
                {"Fish", "fish,tilapia,fried fish", "200", "1 fillet"},
                {"Pasta", "pasta,spaghetti,noodles,macaroni", "220", "1 cup cooked"},
                {"Pizza", "pizza,pizza slice,slice of pizza", "285", "1 slice"},
                {"Burger", "burger,hamburger,cheeseburger", "500", "1 burger"},
                {"Fries", "fries,french fries,chips", "365", "1 medium serving"},
                {"Salad", "salad,green salad,kachumbari", "80", "1 bowl"},
                {"Oatmeal", "oatmeal,porridge,oats,uji", "150", "1 bowl"},
                {"Banana", "banana,bananas", "105", "1 banana"},
                {"Apple", "apple,apples", "95", "1 apple"},
                {"Orange", "orange,oranges", "62", "1 orange"},
                {"Avocado", "avocado,avocados", "240", "1 avocado"},
                {"Milk", "milk,glass of milk", "150", "1 glass"},
                {"Yogurt", "yogurt,yoghurt", "120", "1 cup"},
                {"Cheese", "cheese", "110", "1 slice"},
                {"Tea with sugar", "tea,chai", "45", "1 cup"},
                {"Coffee", "coffee,latte,cappuccino", "60", "1 cup"},
                {"Soda", "soda,coke,soft drink,fanta,sprite", "150", "1 can"},
                {"Juice", "juice,orange juice,mango juice", "110", "1 glass"},
                {"Chocolate", "chocolate,chocolate bar,candy", "230", "1 bar"},
                {"Biscuits", "biscuit,biscuits,cookies,cookie", "60", "1 biscuit"},
                {"Samosa", "samosa,samosas,sambusa", "140", "1 samosa"},
                {"Mandazi", "mandazi,mahamri,doughnut,donut", "190", "1 piece"},
                {"Pancake", "pancake,pancakes", "175", "1 pancake"},
                {"Sausage", "sausage,sausages,smokie", "150", "1 sausage"},
                {"Peanuts", "peanuts,groundnuts,nuts", "170", "1 handful"},
        };
        for (String[] seed : seeds) {
            db.execSQL("INSERT INTO foods (name, keywords, calories_per_serving, serving_desc) "
                            + "VALUES (?, ?, ?, ?)",
                    new Object[]{seed[0], seed[1], Integer.parseInt(seed[2]), seed[3]});
        }
    }
}
