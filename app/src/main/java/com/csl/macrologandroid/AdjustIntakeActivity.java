package com.csl.macrologandroid;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.csl.macrologandroid.cache.UserSettingsCache;
import com.csl.macrologandroid.dtos.SettingsResponse;
import com.csl.macrologandroid.dtos.UserSettingsResponse;
import com.csl.macrologandroid.fragments.ChangeCaloriesFragment;
import com.csl.macrologandroid.lifecycle.Session;
import com.csl.macrologandroid.models.ChangeGoalMacros;
import com.csl.macrologandroid.fragments.ChangeMacrosFragment;
import com.csl.macrologandroid.services.UserService;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import okhttp3.ResponseBody;

public class AdjustIntakeActivity extends AppCompatActivity {

    private UserService service;
    private Disposable disposable;
    private UserSettingsResponse userSettings;
    private Button saveButton;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.activity_adjust_intake);
        Intent intent = getIntent();
        boolean intake = intent.getBooleanExtra("INTAKE", false);

        Button backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> finish());

        userSettings = UserSettingsCache.getInstance().getCache();

        if (intake) {
            backButton.setVisibility(View.INVISIBLE);
            TextView title = findViewById(R.id.adjust_intake_title);
            title.setVisibility(View.VISIBLE);
            userSettings = (UserSettingsResponse) intent.getSerializableExtra("userSettings");
        }

        service = new UserService();
        if (userSettings == null) {
            disposable = service.getUserSettings().subscribe(
                    res -> {
                        userSettings = res;
                        setupButtons();
                    },
                    err -> Log.e(this.getLocalClassName(), err.getMessage())
            );
        } else {
            setupButtons();
        }

        saveButton = findViewById(R.id.save_button);
        saveButton.setOnClickListener(v -> saveGoalMacros());
        saveButton.setEnabled(false);
    }

    private void setupButtons() {
        Button changeMacros = findViewById(R.id.button_macros);
        changeMacros.setOnClickListener(v -> {
            saveButton.setEnabled(true);
            ChangeMacrosFragment fragment = new ChangeMacrosFragment();
            Bundle goalMacros = new Bundle();
            goalMacros.putInt("goalProtein", userSettings.getGoalProtein());
            goalMacros.putInt("goalFat", userSettings.getGoalFat());
            goalMacros.putInt("goalCarbs", userSettings.getGoalCarbs());
            fragment.setArguments(goalMacros);
            setFragment(fragment);
        });

        Button changeCalories = findViewById(R.id.button_calories);
        changeCalories.setOnClickListener(v -> {
            saveButton.setEnabled(true);
            ChangeCaloriesFragment fragment = new ChangeCaloriesFragment();
            Bundle settings = new Bundle();
            settings.putSerializable("userSettings", userSettings);
            fragment.setArguments(settings);
            setFragment(fragment);
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        Session.getInstance().resetTimestamp();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (Session.getInstance().isExpired()) {
            Intent intent = new Intent(AdjustIntakeActivity.this, SplashscreenActivity.class);
            intent.putExtra("SESSION_EXPIRED", true);
            startActivity(intent);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (disposable != null) {
            disposable.dispose();
        }
    }

    private void setFragment(Fragment fragment) {
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        ft.replace(R.id.fragment_container, fragment);
        ft.addToBackStack(null);
        ft.commit();
    }

    private void saveGoalMacros() {
        FragmentManager fm = getSupportFragmentManager();
        List<Fragment> list = fm.getFragments();
        if (!list.isEmpty()) {
            Fragment frag = list.get(0);
            if (frag instanceof ChangeGoalMacros) {
                Bundle goal = ((ChangeGoalMacros) frag).getGoalMacros();
                List<Observable<ResponseBody>> obsList = new ArrayList<>();
                obsList.add(service.putSetting(new SettingsResponse(1, "goalProtein",
                        String.valueOf(goal.getInt("goalProtein")))));
                obsList.add(service.putSetting(new SettingsResponse(1, "goalFat",
                        String.valueOf(goal.getInt("goalFat")))));
                obsList.add(service.putSetting(new SettingsResponse(1, "goalCarbs",
                        String.valueOf(goal.getInt("goalCarbs")))));

                disposable = Observable.zip(obsList, i -> i)
                        .subscribe(res -> {
                            UserSettingsCache.getInstance().clearCache();
                            Intent resultIntent = new Intent();
                            resultIntent.putExtra("RELOAD", true);
                            setResult(Activity.RESULT_OK, resultIntent);
                            finish();
                        }, err -> Log.d(this.getLocalClassName(), err.getMessage()));

            }
        }
    }
}
