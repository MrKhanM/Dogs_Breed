package com.example.jetpackcourse.model;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface DogDao {
    @Insert
    List<Long> insertAll(DogBreed... dogs);

    @Query("SELECT * FROM dogbreed")
    List<DogBreed> getAllDogs();

    @Query("SELECT * FROM dogbreed WHERE uuid = :dog_id")
    DogBreed getDog(int dog_id);

    @Query("DELETE FROM dogbreed")
    void deleteAllDogs();
}
