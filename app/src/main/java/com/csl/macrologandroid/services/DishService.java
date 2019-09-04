package com.csl.macrologandroid.services;

import com.csl.macrologandroid.BuildConfig;
import com.csl.macrologandroid.dtos.DishResponse;

import java.util.List;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;

public class DishService {

    private final ApiService apiService;

    public DishService(String token) {
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
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        apiService = retrofit.create(ApiService.class);
    }

    public Observable<List<DishResponse>> getAllDishes() {
        return apiService.getAllDishes().subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
    }

    private interface ApiService {

        @GET("dishes")
        Observable<List<DishResponse>> getAllDishes();
    }
}
