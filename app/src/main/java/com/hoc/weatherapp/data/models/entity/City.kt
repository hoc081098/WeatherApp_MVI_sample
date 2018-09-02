package com.hoc.weatherapp.data.models.entity

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.android.parcel.Parcelize

@Entity
@Parcelize
data class City(
        @PrimaryKey
        val id: Long = Long.MIN_VALUE,
        val name: String = "",
        val country: String = "",
        val lat: Double = Double.NEGATIVE_INFINITY,
        val lng: Double = Double.NEGATIVE_INFINITY
) : Parcelable