package com.hoc.weatherapp.data.models.entity

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

/**
 * Declaring the column info allows for the renaming of variables without implementing a
 * database migration, as the column name would not change.
 */

@Entity(tableName = "cities")
@Parcelize
data class City(
  /**
   * Id of city
   */
  @PrimaryKey(autoGenerate = false)
  @ColumnInfo(name = "id")
  val id: Long,

  /**
   * Name of city
   */
  @ColumnInfo(name = "name")
  val name: String = "",

  /**
   * Country of city
   */
  @ColumnInfo(name = "country")
  val country: String = "",

  /**
   * Latitude of city
   */
  @ColumnInfo(name = "lat")
  val lat: Double = Double.NEGATIVE_INFINITY,

  /**
   * Longitude of city
   */
  @ColumnInfo(name = "lng")
  val lng: Double = Double.NEGATIVE_INFINITY,

  /**
   * The time zone name.
   */
  @ColumnInfo(name = "zone_id")
  val zoneId: String
) : Parcelable
