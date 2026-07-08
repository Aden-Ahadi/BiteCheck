package com.example.bitecheck.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bitecheck.R;
import com.example.bitecheck.model.Meal;
import com.example.bitecheck.util.DateUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MealAdapter extends RecyclerView.Adapter<MealAdapter.MealViewHolder> {

    private final List<Meal> meals = new ArrayList<>();

    public void submit(List<Meal> newMeals) {
        meals.clear();
        meals.addAll(newMeals);
        notifyDataSetChanged();
    }

    public Meal getAt(int position) {
        return meals.get(position);
    }

    public void removeAt(int position) {
        meals.remove(position);
        notifyItemRemoved(position);
    }

    @NonNull
    @Override
    public MealViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_meal, parent, false);
        return new MealViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MealViewHolder holder, int position) {
        Meal meal = meals.get(position);
        holder.food.setText(meal.food);

        StringBuilder details = new StringBuilder();
        if (meal.quantity > 0) {
            details.append(trimNumber(meal.quantity));
            if (meal.unit != null && !meal.unit.isEmpty()) {
                details.append(' ').append(meal.unit);
            }
        }
        if (meal.mealType != null && !meal.mealType.isEmpty()) {
            if (details.length() > 0) {
                details.append(" • ");
            }
            details.append(meal.mealType);
        }
        String time = DateUtil.displayTime(meal.loggedAt);
        if (!time.isEmpty()) {
            if (details.length() > 0) {
                details.append(" • ");
            }
            details.append(time);
        }
        holder.details.setText(details.toString());
        holder.calories.setText(String.format(Locale.US, "%d kcal", meal.calories));
    }

    @Override
    public int getItemCount() {
        return meals.size();
    }

    private String trimNumber(double value) {
        return value == Math.floor(value)
                ? String.valueOf((long) value) : String.valueOf(value);
    }

    static class MealViewHolder extends RecyclerView.ViewHolder {
        final TextView food;
        final TextView details;
        final TextView calories;

        MealViewHolder(@NonNull View itemView) {
            super(itemView);
            food = itemView.findViewById(R.id.text_food);
            details = itemView.findViewById(R.id.text_details);
            calories = itemView.findViewById(R.id.text_calories);
        }
    }
}
