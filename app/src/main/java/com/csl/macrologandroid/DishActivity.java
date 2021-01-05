package com.csl.macrologandroid;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatCheckedTextView;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.csl.macrologandroid.adapters.AutocompleteAdapter;
import com.csl.macrologandroid.cache.DishCache;
import com.csl.macrologandroid.cache.FoodCache;
import com.csl.macrologandroid.dtos.DishResponse;
import com.csl.macrologandroid.dtos.FoodResponse;
import com.csl.macrologandroid.dtos.IngredientResponse;
import com.csl.macrologandroid.dtos.PortionResponse;
import com.csl.macrologandroid.services.DishService;
import com.csl.macrologandroid.services.FoodService;
import com.csl.macrologandroid.util.ListUtil;
import com.csl.macrologandroid.util.SpinnerSetupUtil;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import io.reactivex.disposables.Disposable;

public class DishActivity extends AppCompatActivity {

    private final List<String> allDishNames = new ArrayList<>();

    private DishResponse dishResponse;
    private TextInputLayout editDishNameLayout;
    private TextInputEditText editDishName;
    private LinearLayout ingredientsLayout;
    private Button saveButton;
    private Disposable disposable;
    private AutoCompleteTextView searchFoodTextView;
    private final List<String> autoCompleteList = new ArrayList<>();
    private Disposable foodDisposable;
    private List<FoodResponse> allFood;
    private List<IngredientResponse> displayedIngredients = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_dish);

        List<DishResponse> allDishes = DishCache.getInstance().getCache();
        for (DishResponse dish : allDishes) {
            allDishNames.add(dish.getName());
        }

        editDishNameLayout = findViewById(R.id.dish_name_layout);
        editDishName = findViewById(R.id.dish_name);
        editDishName.addTextChangedListener(dishNameWatcher);

        searchFoodTextView = findViewById(R.id.search_food);
        allFood = FoodCache.getInstance().getCache();
        if (allFood.size() == 0) {
            FoodService foodService = new FoodService(getToken());
            foodDisposable = foodService.getAllFood().subscribe(res -> {
                        allFood = res;
                        autoCompleteList.clear();
                        autoCompleteList.addAll(getFoodAutoCompleteList(allFood));
                        setupAutoCompleteTextView();
                    },
                    err -> Log.e(this.getClass().getSimpleName(), Objects.requireNonNull(err.getMessage())));
        } else {
            autoCompleteList.clear();
            autoCompleteList.addAll(getFoodAutoCompleteList(allFood));
            setupAutoCompleteTextView();
        }

        Button backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> finish());

        Intent intent = getIntent();
        dishResponse = (DishResponse) intent.getSerializableExtra("DISH");
        ingredientsLayout = findViewById(R.id.ingredients_layout);

        saveButton = findViewById(R.id.save_button);
        saveButton.setOnClickListener(v -> saveDish());

        // Add or edit
        if (dishResponse != null) {
            editDishName.setText(dishResponse.getName());
            displayedIngredients = dishResponse.getIngredients();
            for (IngredientResponse ingredient : displayedIngredients) {
                addIngredientToLayout(ingredient);
            }
        } else {
            editDishName.requestFocus();
        }
        saveButton.setEnabled(false);
    }

    @Override
    protected void onDestroy() {
        if (disposable != null) {
            disposable.dispose();
        }
        if (foodDisposable != null) {
            foodDisposable.dispose();
        }
        super.onDestroy();
    }

    private List<String> getFoodAutoCompleteList(List<FoodResponse> allFood) {
        List<String> autoCompleteList = new ArrayList<>();
        for (FoodResponse food : allFood) {
            autoCompleteList.add(food.getName());
        }
        return autoCompleteList;
    }

    private void setupAutoCompleteTextView() {
        ArrayAdapter<String> autocompleteAdapter = new AutocompleteAdapter(this, android.R.layout.simple_spinner_dropdown_item, autoCompleteList);
        searchFoodTextView.setAdapter(autocompleteAdapter);
        searchFoodTextView.setThreshold(2);
        searchFoodTextView.setOnItemClickListener((parent, view, position, id) -> {
            String foodName = ((AppCompatCheckedTextView) view).getText().toString();
            FoodResponse selectedFood = new SpinnerSetupUtil().getFoodFromList(foodName, allFood);
            IngredientResponse ingredientResponse = new IngredientResponse(1.0, selectedFood, null);
            displayedIngredients.add(ingredientResponse);
            searchFoodTextView.setText("");
            addIngredientToLayout(ingredientResponse);
        });

        searchFoodTextView.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_NEXT
                    && searchFoodTextView.isPopupShowing()
                    && autocompleteAdapter.getCount() != 0) {
                String selectedOption = autocompleteAdapter.getItem(0);
                if (selectedOption != null) {
                    searchFoodTextView.setText(selectedOption);
                    searchFoodTextView.dismissDropDown();
                }
                return true;
            }
            return false;
        });
    }

    private void addIngredientToLayout(IngredientResponse ingredient) {
        @SuppressLint("InflateParams")
        ConstraintLayout ingredientEntry = (ConstraintLayout) getLayoutInflater().inflate(R.layout.layout_edit_log_entry, null);

        TextView foodNameTextView = ingredientEntry.findViewById(R.id.food_name);
        foodNameTextView.setText(ingredient.getFood().getName());

        ImageView trashImageView = ingredientEntry.findViewById(R.id.trash_icon);
        trashImageView.setOnClickListener(v -> removeIngredient(ingredient));

        TextInputEditText amountEditText = ingredientEntry.findViewById(R.id.food_amount);
        amountEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // not needed
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                isSaveButtonEnabled();
            }

            @Override
            public void afterTextChanged(Editable s) {
                // not needed
            }
        });

        Spinner portionSpinner = ingredientEntry.findViewById(R.id.portion_spinner);
        setupPortionSpinner(portionSpinner, ingredient, amountEditText);

        ingredientsLayout.addView(ingredientEntry);
    }

    private void setupPortionSpinner(Spinner portionSpinner, IngredientResponse ingredient, TextInputEditText amount) {
        List<PortionResponse> allPortions = ingredient.getFood().getPortions();
        List<String> portionDescList = ListUtil.getPortionDescList(allPortions, true);

        ArrayAdapter<String> portionSpinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, portionDescList);
        portionSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        portionSpinner.setAdapter(portionSpinnerAdapter);
        if (ingredient.getPortion() != null) {
            PortionResponse portion = ListUtil.getPortionFromListById(ingredient.getPortion().getId(), allPortions);
            if (portion != null) {
                int index = portionDescList.indexOf(portion.getDescription() + " (" + portion.getGrams() + " gr)");
                portionSpinner.setSelection(index);
            }
        } else {
            portionSpinner.setSelection(portionDescList.size() - 1);
        }

        portionSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedPortion = ((AppCompatTextView) view).getText().toString();
                if (selectedPortion.equals("gram")) {
                    amount.setInputType(InputType.TYPE_CLASS_NUMBER);
                    amount.setText(String.valueOf(Math.round(ingredient.getMultiplier() * 100)));
                } else {
                    amount.setInputType(InputType.TYPE_NUMBER_FLAG_DECIMAL);
                    amount.setText(String.valueOf(ingredient.getMultiplier()));
                }
                isSaveButtonEnabled();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Not needed
            }
        });
    }

    private void removeIngredient(IngredientResponse ingredient) {
        int index = displayedIngredients.indexOf(ingredient);
        ConstraintLayout inner = (ConstraintLayout) ingredientsLayout.getChildAt(index);
        displayedIngredients.remove(ingredient);
        ingredientsLayout.removeView(inner);
        isSaveButtonEnabled();
    }

    private void saveDish() {
        if (editDishNameLayout.isErrorEnabled()) {
            return;
        }

        Long dishId = getDishId(dishResponse);
        String dishName = Objects.requireNonNull(editDishName.getText()).toString();

        List<IngredientResponse> newIngredients = new ArrayList<>();
        for (int i = 0; i < displayedIngredients.size(); i++) {
            getPortionAndAmountFromView(newIngredients, i);
        }

        DishResponse newDish = new DishResponse(dishId, dishName, newIngredients);
        DishService dishService = new DishService(getToken());
        disposable = dishService.postDish(newDish)
                .subscribe(res -> {
                    Intent resultIntent = new Intent();
                    setResult(Activity.RESULT_OK, resultIntent);
                    finish();
                }, err -> Log.e(this.getLocalClassName(), Objects.requireNonNull(err.getMessage())));

    }

    private Long getDishId(DishResponse dishResponse) {
        if (dishResponse != null) {
            return dishResponse.getId();
        }
        return null;
    }

    private void getPortionAndAmountFromView(List<IngredientResponse> newIngredients, int index) {
        IngredientResponse ingredient = displayedIngredients.get(index);
        ConstraintLayout inner = (ConstraintLayout) ingredientsLayout.getChildAt(index);
        Spinner portionSpinner = (Spinner) inner.getChildAt(2);
        TextInputEditText amount = inner.findViewById(R.id.food_amount);

        FoodResponse food = ingredient.getFood();
        PortionResponse selectedPortion = null;
        String selectedPortionDesc = portionSpinner.getSelectedItem().toString();
        if (!selectedPortionDesc.equals("gram")) {
            selectedPortion = ListUtil.getPortionFromListByName(selectedPortionDesc, food);
        }
        double multiplier = Double.parseDouble(Objects.requireNonNull(amount.getText()).toString());
        if (selectedPortion == null) {
            multiplier = multiplier / 100;
        }

        IngredientResponse newIngredient = new IngredientResponse(multiplier, food, selectedPortion);
        newIngredients.add(newIngredient);
    }

    private void isSaveButtonEnabled() {
        boolean nameCheck = editDishName.getText() != null && editDishName.getText().toString().length() != 0;
        if (dishResponse == null) {
            // Adding a new dish. Dish name can not be the same twice, so check name
            nameCheck = nameCheck && !allDishNames.contains(editDishName.getText().toString());
        } else {
            boolean foodNameChanged = !dishResponse.getName().equals(editDishName.getText().toString());
            if (foodNameChanged) {
                // If altering the name, the new name may not be present in the database
                nameCheck = nameCheck && !allDishNames.contains(editDishName.getText().toString());
            }
        }

        boolean emptyMultiplierCheck = false;

        int ingredientCount = ingredientsLayout.getChildCount();
        for (int i = 0; i < ingredientCount; i++) {
            ConstraintLayout inner = (ConstraintLayout) ingredientsLayout.getChildAt(i);
            TextInputEditText amountEditText = inner.findViewById(R.id.food_amount);
            String amountText = Objects.requireNonNull(amountEditText.getText()).toString();
            if (amountText.isEmpty()) {
                emptyMultiplierCheck = true;
            }
        }

        saveButton.setEnabled(nameCheck && ingredientsLayout.getChildCount() > 0 && !emptyMultiplierCheck);
    }

    private final TextWatcher dishNameWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            // Not needed
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            if (dishResponse == null) { // new dish
                if (allDishNames.contains(s.toString())) {
                    editDishNameLayout.setErrorEnabled(true);
                    editDishNameLayout.setError("You already have a dish named like this");
                } else {
                    editDishNameLayout.setErrorEnabled(false);
                    editDishNameLayout.setError("");
                }
            } else {
                // edit dish
                if (dishResponse.getName().equalsIgnoreCase(s.toString())) {
                    // nothing altered
                    editDishNameLayout.setErrorEnabled(false);
                    editDishNameLayout.setError("");
                } else if (allDishNames.contains(s.toString())) {
                    // new dish already exists in the database
                    editDishNameLayout.setErrorEnabled(true);
                    editDishNameLayout.setError("You already have a dish named like this");
                } else {
                    // new dish name is ok
                    editDishNameLayout.setErrorEnabled(false);
                    editDishNameLayout.setError("");
                }
            }
        }

        @Override
        public void afterTextChanged(Editable s) {
            isSaveButtonEnabled();
        }
    };

    private String getToken() {
        return getSharedPreferences("AUTH", MODE_PRIVATE).getString("TOKEN", "");
    }
}
