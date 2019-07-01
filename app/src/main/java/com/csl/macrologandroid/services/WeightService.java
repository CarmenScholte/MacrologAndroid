package com.csl.macrologandroid.services;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import com.csl.macrologandroid.BuildConfig;
import com.csl.macrologandroid.MainActivity;
import com.csl.macrologandroid.dtos.WeightRequest;

import java.util.List;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.ResponseBody;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

public class WeightService extends Service {

    private final ApiService apiService;

    public WeightService() {
        String token = MainActivity.getPreferences().getString("TOKEN", "");
        OkHttpClient.Builder client = new OkHttpClient.Builder();
        client.addInterceptor(chain -> {
            Request original = chain.request();
            Request request = original.newBuilder()
                    .header("Authorization", "Bearer " + token)
                    .method(original.method(), original.body())
                    .build();
            return chain.proceed(request);
        });

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BuildConfig.SERVER_URL)
                .client(client.build())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(CustomGsonConverter.create())
                .build();

        apiService = retrofit.create(ApiService.class);
    }

    @SuppressWarnings("SameReturnValue")
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public Observable<List<WeightRequest>> getAllMeasurements() {
        return apiService.getAllMeasurements().subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
    }

    public Observable<ResponseBody> postMeasurement(WeightRequest weightRequest) {
        return apiService.postMeasurement(weightRequest).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
    }

    private interface ApiService {

        @GET("weight")
        Observable<List<WeightRequest>> getAllMeasurements();

        @POST("weight")
        Observable<ResponseBody> postMeasurement(@Body WeightRequest weightRequest);

    }
}
