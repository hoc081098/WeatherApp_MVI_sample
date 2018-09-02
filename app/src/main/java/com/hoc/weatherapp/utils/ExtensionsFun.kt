package com.hoc.weatherapp.utils

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.fragment.app.Fragment

fun Context.toast(message: CharSequence): Toast = Toast.makeText(
    this,
    message,
    Toast.LENGTH_SHORT
).also(Toast::show)

fun Fragment.toast(message: CharSequence) = context?.toast(message)

inline fun <reified T : Any> T.debug(msg: Any?, tag: String? = null) {
    Log.d(tag ?: this::class.java.simpleName, msg.toString())
}
