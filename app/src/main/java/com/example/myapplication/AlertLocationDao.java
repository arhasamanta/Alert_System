package com.example.myapplication;



import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface AlertLocationDao {
    @Insert
    void insert(AlertLocation alertLocation);

    @Query("select * FROM alert_locations")
    List<AlertLocation> getAllAlertLocations();
}
