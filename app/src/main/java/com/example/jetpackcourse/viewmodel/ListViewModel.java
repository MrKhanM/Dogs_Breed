package com.example.jetpackcourse.viewmodel;

import android.app.Application;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.SystemClock;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.jetpackcourse.model.DogBreed;
import com.example.jetpackcourse.model.DogDao;
import com.example.jetpackcourse.model.DogDatabase;
import com.example.jetpackcourse.model.DogsApiService;
import com.example.jetpackcourse.util.NotificationHelper;
import com.example.jetpackcourse.util.SharedPreferencesHelper;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.Scheduler;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.observers.DisposableSingleObserver;
import io.reactivex.schedulers.Schedulers;

public class ListViewModel extends AndroidViewModel {

    private DogsApiService dogsService = new DogsApiService();
    private CompositeDisposable disposable = new CompositeDisposable();

    private AsyncTask<List<DogBreed>,Void,List<DogBreed>> insertTask;
    private AsyncTask<Void,Void,List<DogBreed>> retrieveTask;

    private SharedPreferencesHelper prefHelper = SharedPreferencesHelper.getInstance(getApplication());
    private long refreshTime = 5 * 60 * 1000 * 1000 * 1000L;

    public MutableLiveData<List<DogBreed>> dogs = new MutableLiveData<List<DogBreed>>();
    public MutableLiveData<Boolean> dogLoadError = new MutableLiveData<Boolean>();
    public MutableLiveData<Boolean> loading = new MutableLiveData<Boolean>();

    public ListViewModel(@NonNull Application application) {
        super(application);
    }

    public void refresh() {
        checkCacheDuration();
        long updateTime = prefHelper.getUpdateTime();
        long currentTime = System.nanoTime();
        if (updateTime != 0 && currentTime - updateTime < refreshTime){
            fetchFromDatabase();
        }else {
            fetchFromRemote();
        }
    }

    public void refreshBypassCache(){
        fetchFromRemote();
    }

    private void checkCacheDuration(){
        String cachePreferences = prefHelper.getCacheDuration();
        if (!cachePreferences.equals("")){
            try {
                int cachePreferencesInt = Integer.parseInt(cachePreferences);
                refreshTime = cachePreferencesInt * 1000 * 1000 * 1000L;
            }catch (NumberFormatException e){
                e.printStackTrace();
            }
        }
    }

    private void fetchFromDatabase(){
        loading.setValue(true);
        retrieveTask = new RetrieveDogsTask();
        retrieveTask.execute();
    }

    private void fetchFromRemote() {
        loading.setValue(true);
        disposable.add(
                dogsService.getDogs()
                        .subscribeOn(Schedulers.newThread())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribeWith(new DisposableSingleObserver<List<DogBreed>>() {
                            @Override
                            public void onSuccess(List<DogBreed> dogBreeds) {
                                insertTask = new InsertDogsTask();
                                insertTask.execute(dogBreeds);
                                Toast.makeText(getApplication(),"Dogs Retrieved from endpoint",Toast.LENGTH_SHORT).show();
                                NotificationHelper.getInstance(getApplication()).createNotification();
                            }
                            @Override
                            public void onError(Throwable e) {
                                dogLoadError.setValue(true);
                                loading.setValue(false);
                                e.printStackTrace();
                            }
                        })
        );
    }
    private void dogsRetrieved(List<DogBreed> dogsList){
        dogs.setValue(dogsList);
        dogLoadError.setValue(false);
        loading.setValue(false);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        disposable.clear();

        if (insertTask != null){
            insertTask.cancel(true);
            insertTask = null;
        }

        if (retrieveTask != null){
            retrieveTask.cancel(true);
            retrieveTask = null;
        }
    }
    private class InsertDogsTask extends AsyncTask<List<DogBreed>,Void,List<DogBreed>>{

        @Override
        protected List<DogBreed> doInBackground(List<DogBreed>... lists) {
            List<DogBreed>  list = lists[0];
            DogDao dao = DogDatabase.getInstance(getApplication()).dogDao();
            dao.deleteAllDogs();

            ArrayList<DogBreed> newList = new ArrayList<>(list);
            List<Long> result = dao.insertAll(newList.toArray(new DogBreed[0]));

            int i = 0;
            while (i<list.size()){
                list.get(i).uuid = result.get(i).intValue();
                ++i;
            }
            return list;
        }

        @Override
        protected void onPostExecute(List<DogBreed> dogBreeds) {
            dogsRetrieved(dogBreeds);
            prefHelper.saveUpdateTime(System.nanoTime());
        }

        }
    private class RetrieveDogsTask extends AsyncTask<Void , Void ,List<DogBreed>>{

        @Override
        protected List<DogBreed> doInBackground(Void... voids) {
            return DogDatabase.getInstance(getApplication()).dogDao().getAllDogs();
        }

        @Override
        protected void onPostExecute(List<DogBreed> dogBreeds) {
            dogsRetrieved(dogBreeds);
            Toast.makeText(getApplication(),"Dogs Retrieved from Database",Toast.LENGTH_SHORT).show();
        }
    }
}
