package com.example.bitecheck.ui;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bitecheck.R;
import com.example.bitecheck.data.local.MealDao;
import com.example.bitecheck.model.Meal;
import com.example.bitecheck.util.DateUtil;
import com.example.bitecheck.util.SessionManager;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class MealHistoryActivity extends BaseNavActivity {

    private MealDao mealDao;
    private MealAdapter adapter;
    private String userId;
    private String selectedDay;

    @Override
    protected int getContentLayoutId() {
        return R.layout.content_history;
    }

    @Override
    protected int getBottomNavItemId() {
        return R.id.nav_history;
    }

    @Override
    protected CharSequence getScreenTitle() {
        return getString(R.string.nav_history);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mealDao = new MealDao(this);
        SessionManager session = new SessionManager(this);
        userId = session.getUserId() != null ? session.getUserId() : "local";
        selectedDay = DateUtil.today();

        adapter = new MealAdapter();
        RecyclerView recycler = findViewById(R.id.recycler_meals);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(adapter);
        attachSwipeToDelete(recycler);

        ImageButton fab = findViewById(R.id.fab_add_meal);
        fab.setOnClickListener(v -> showAddMealDialog());
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshList();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_history, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_pick_date) {
            showDatePicker();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void refreshList() {
        List<Meal> meals = mealDao.mealsForDay(userId, selectedDay);
        adapter.submit(meals);

        TextView dayText = findViewById(R.id.text_history_day);
        TextView totalText = findViewById(R.id.text_history_total);
        TextView emptyText = findViewById(R.id.text_history_empty);
        dayText.setText(DateUtil.displayDay(selectedDay));
        int total = mealDao.caloriesForDay(userId, selectedDay);
        totalText.setText(String.format(Locale.US, "%d kcal • %d meals",
                total, meals.size()));
        emptyText.setVisibility(meals.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            selectedDay = String.format(Locale.US, "%04d-%02d-%02d",
                    year, month + 1, dayOfMonth);
            refreshList();
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void showAddMealDialog() {
        BottomSheetDialog sheet =
                new BottomSheetDialog(this, R.style.Theme_BiteCheck_BottomSheet);
        View view = getLayoutInflater().inflate(R.layout.sheet_add_meal, null);
        TextInputEditText foodInput = view.findViewById(R.id.input_food);
        TextInputEditText caloriesInput = view.findViewById(R.id.input_meal_calories);
        TextInputEditText quantityInput = view.findViewById(R.id.input_quantity);
        TextInputEditText typeInput = view.findViewById(R.id.input_meal_type);
        TextInputLayout typeLayout = view.findViewById(R.id.layout_meal_type);
        View.OnClickListener openPicker = v -> showMealTypePicker(typeInput);
        typeInput.setOnClickListener(openPicker);
        typeLayout.setEndIconOnClickListener(openPicker);

        view.findViewById(R.id.btn_add_meal_cancel).setOnClickListener(v -> sheet.dismiss());
        view.findViewById(R.id.btn_add_meal_confirm).setOnClickListener(v -> {
            String food = foodInput.getText() == null
                    ? "" : foodInput.getText().toString().trim();
            String caloriesText = caloriesInput.getText() == null
                    ? "" : caloriesInput.getText().toString().trim();
            if (food.isEmpty() || caloriesText.isEmpty()) {
                Toast.makeText(this, R.string.error_required, Toast.LENGTH_SHORT).show();
                return;
            }
            Meal meal = new Meal();
            meal.userId = userId;
            meal.food = food;
            try {
                meal.calories = Integer.parseInt(caloriesText);
            } catch (NumberFormatException e) {
                meal.calories = 0;
            }
            String quantityText = quantityInput.getText() == null
                    ? "" : quantityInput.getText().toString().trim();
            try {
                meal.quantity = quantityText.isEmpty()
                        ? 1 : Double.parseDouble(quantityText);
            } catch (NumberFormatException e) {
                meal.quantity = 1;
            }
            meal.unit = "serving";
            meal.mealType = typeInput.getText() == null ? ""
                    : typeInput.getText().toString().trim().toLowerCase(Locale.US);
            meal.loggedAt = DateUtil.now();
            meal.synced = false;
            mealDao.insert(meal);
            selectedDay = DateUtil.today();
            refreshList();
            sheet.dismiss();
        });

        sheet.setContentView(view);
        // A sheet with text inputs must rise above the keyboard: expand it fully
        // and let the window resize so the lower fields stay reachable.
        sheet.setOnShowListener(d -> {
            if (sheet.getWindow() != null) {
                sheet.getWindow().setSoftInputMode(
                        WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
            }
            View bottomSheet = sheet.findViewById(
                    com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                BottomSheetBehavior.from(bottomSheet)
                        .setState(BottomSheetBehavior.STATE_EXPANDED);
            }
        });
        sheet.show();
    }

    /**
     * Meal type is chosen from a bottom sheet that slides up over the add-meal
     * sheet (which stays behind), settling with a little overshoot bounce, then
     * drops away when an option is tapped.
     */
    private void showMealTypePicker(TextInputEditText field) {
        BottomSheetDialog picker =
                new BottomSheetDialog(this, R.style.Theme_BiteCheck_BottomSheet);
        View content = getLayoutInflater().inflate(R.layout.sheet_meal_type, null);
        int[] ids = {R.id.opt_breakfast, R.id.opt_lunch, R.id.opt_dinner, R.id.opt_snack};
        String[] labels = {"Breakfast", "Lunch", "Dinner", "Snack"};
        for (int i = 0; i < ids.length; i++) {
            final String label = labels[i];
            content.findViewById(ids[i]).setOnClickListener(v -> {
                field.setText(label);
                picker.dismiss();
            });
        }
        picker.setContentView(content);
        picker.setOnShowListener(d -> {
            View sheetView = picker.findViewById(
                    com.google.android.material.R.id.design_bottom_sheet);
            if (sheetView != null) {
                float dy = getResources().getDisplayMetrics().density * 28;
                sheetView.setTranslationY(dy);
                sheetView.animate().translationY(0f)
                        .setInterpolator(new OvershootInterpolator(1.4f))
                        .setStartDelay(40).setDuration(340).start();
            }
        });
        picker.show();
    }

    private void attachSwipeToDelete(RecyclerView recycler) {
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(
                0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getBindingAdapterPosition();
                confirmDeleteMeal(position);
            }
        }).attachToRecyclerView(recycler);
    }

    /**
     * Swiping a meal only arms the delete — it's confirmed in a bottom sheet so
     * an accidental swipe can't wipe a log. Cancelling restores the swiped row.
     */
    private void confirmDeleteMeal(int position) {
        Meal meal = adapter.getAt(position);
        if (meal == null) {
            adapter.notifyItemChanged(position);
            return;
        }
        BottomSheetDialog sheet =
                new BottomSheetDialog(this, R.style.Theme_BiteCheck_BottomSheet);
        View view = getLayoutInflater().inflate(R.layout.sheet_delete_meal, null);

        boolean[] confirmed = {false};
        view.findViewById(R.id.btn_confirm_delete_meal).setOnClickListener(v -> {
            confirmed[0] = true;
            mealDao.delete(meal.id);
            adapter.removeAt(position);
            refreshHeader();
            sheet.dismiss();
            Toast.makeText(this, R.string.msg_meal_deleted, Toast.LENGTH_SHORT).show();
        });
        view.findViewById(R.id.btn_cancel_delete_meal).setOnClickListener(v -> sheet.dismiss());
        // Any dismissal that wasn't a confirm (cancel, tap-outside, back) must
        // un-swipe the row so it stays in the list.
        sheet.setOnDismissListener(d -> {
            if (!confirmed[0]) {
                adapter.notifyItemChanged(position);
            }
        });

        sheet.setContentView(view);
        sheet.show();
    }

    private void refreshHeader() {
        TextView totalText = findViewById(R.id.text_history_total);
        TextView emptyText = findViewById(R.id.text_history_empty);
        int total = mealDao.caloriesForDay(userId, selectedDay);
        totalText.setText(String.format(Locale.US, "%d kcal • %d meals",
                total, adapter.getItemCount()));
        emptyText.setVisibility(adapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
    }
}
