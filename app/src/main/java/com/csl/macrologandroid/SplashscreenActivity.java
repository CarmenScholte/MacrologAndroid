package com.csl.macrologandroid;

import android.content.Intent;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageView;
import android.widget.TextView;

import com.csl.macrologandroid.lifecycle.Session;
import com.csl.macrologandroid.services.HealthcheckService;

import java.net.SocketTimeoutException;

import io.reactivex.disposables.Disposable;

public class SplashscreenActivity extends AppCompatActivity {

    private HealthcheckService service;
    private String token;
    private int callCounter = 0;
    private Boolean expired;
    private Disposable disposable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splashscreen);

        service = new HealthcheckService();
        token = getSharedPreferences("AUTH", MODE_PRIVATE).getString("TOKEN", null);

        ImageView image = findViewById(R.id.animated_image);
        TextView waitMessage = findViewById(R.id.wait_message);

        Handler handler = new Handler();
        Animation fadeIn = new AlphaAnimation(0.0f, 1.0f);
        fadeIn.setDuration(1000);
        handler.postDelayed(() -> {
            image.setBackgroundResource(R.drawable.hamster_wheel);
            AnimatedVectorDrawable animation = (AnimatedVectorDrawable) image.getBackground();
            animation.start();

            image.startAnimation(fadeIn);
            waitMessage.setVisibility(View.VISIBLE);
            waitMessage.startAnimation(fadeIn);
        }, 2000);

        Intent intent = getIntent();
        expired = (Boolean) intent.getSerializableExtra("SESSION_EXPIRED");

        Session.getInstance().resetTimestamp();

        doHealthCheck();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (disposable != null) {
            disposable.dispose();
        }
    }

    private void doHealthCheck() {
        callCounter++;
        disposable = service.healthcheck(token)
                .subscribe(
                        res -> {
                            if (expired != null) {
                                finish();
                            } else {
                                startActivity(new Intent(SplashscreenActivity.this, MainActivity.class));
                                finish();
                            }
                        },
                        err -> {
                            Log.d(this.getLocalClassName(), err.getMessage());
                            if (err instanceof SocketTimeoutException && callCounter < 4) {
                                Log.d(this.getLocalClassName(), "retry: " + callCounter);
                                doHealthCheck();
                            }
                            else {
                                Intent intent = new Intent(this, MainActivity.class);
                                intent.putExtra("TOKEN_EXPIRED", true);
                                Log.d(this.getLocalClassName(), "token expired");
                                startActivity(intent);
                                finish();
                            }
                        });

    }
}