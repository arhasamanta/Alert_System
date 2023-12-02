
package com.example.myapplication;

import androidx.room.PrimaryKey;

@androidx.room.Entity(tableName = "alert_locations")
public class AlertLocation{
   @PrimaryKey(autoGenerate = true)
   private int id;

   private double latitude;
   private double longitude;
   private String message;
   private String virusOutbreak;





   public AlertLocation(double latitude, double longitude, String message, String virusOutbreak) {
      this.latitude = latitude;
      this.longitude = longitude;
      this.message = message;
      this.virusOutbreak = virusOutbreak;
   }

   public int getId() {
      return id;
   }

   public void setId(int id) {
      this.id = id;
   }

   public double getLatitude() {
      return latitude;
   }

   public double getLongitude() {
      return longitude;
   }

   public String getMessage() {
      return message;
   }

   public String getVirusOutbreak() {
      return virusOutbreak;
   }
}
