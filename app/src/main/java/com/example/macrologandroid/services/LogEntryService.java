package com.example.macrologandroid.services;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import com.example.macrologandroid.dtos.LogEntryRequest;
import com.example.macrologandroid.dtos.LogEntryResponse;
import com.example.macrologandroid.MainActivity;

import java.time.LocalDate;
import java.util.List;

import io.reactivex.Observable;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.ResponseBody;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

public class LogEntryService extends Service {

    private ApiService apiService;

    private String token = "";

    public LogEntryService() {
        token = MainActivity.getPreferences().getString("TOKEN", "");
        OkHttpClient.Builder client = new OkHttpClient.Builder();
        client.addInterceptor(chain -> {
            Request original = chain.request();
            Request request = original.newBuilder()
                    .addHeader("Authorization", "Bearer " + token)
                    .addHeader("Content-Type", "application/json")
                    .method(original.method(), original.body())
                    .build();
            return chain.proceed(request);
        });

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://macrolog-backend.herokuapp.com/")
                .client(client.build())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        apiService = retrofit.create(ApiService.class);
    }

    public boolean isTokenEmpty() {
        return token.isEmpty();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public Observable<List<LogEntryResponse>> getLogsForDay(LocalDate localDate) {
        return apiService.getLogsForDay(localDate);
    }

    public Observable<List<LogEntryResponse>> postLogEntry(List<LogEntryRequest> entries) {
        return apiService.postLogEntry(entries);
    }

    public Observable<ResponseBody> deleteLogEntry(long id) {
        return apiService.deleteLogEntry((int) id);
    }

    private interface ApiService {

        @GET("logs/day/{date}")
        Observable<List<LogEntryResponse>> getLogsForDay(@Path("date") LocalDate date);

        @POST("logs")
        Observable<List<LogEntryResponse>> postLogEntry(@Body List<LogEntryRequest> entries);

        @DELETE("logs/{id}")
        Observable<ResponseBody> deleteLogEntry(@Path("id") int id);

    }
}