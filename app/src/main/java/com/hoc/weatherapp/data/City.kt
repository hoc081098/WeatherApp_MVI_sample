package com.hoc.weatherapp.data

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.android.parcel.Parcelize

@Entity
@Parcelize
data class City(
        @PrimaryKey
        val id: Long = -1,
        val name: String = "",
        val country: String = "",
        val lat: Double = Double.NEGATIVE_INFINITY,
        val lng: Double = Double.NEGATIVE_INFINITY
) : Parcelable