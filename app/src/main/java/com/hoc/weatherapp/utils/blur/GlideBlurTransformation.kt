package com.hoc.weatherapp.utils.blur

import android.content.Context
import android.graphics.Bitmap
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import java.security.MessageDigest

class GlideBlurTransformation(private val context: Context, private val radius: Float) :
  BitmapTransformation() {
  override fun transform(
    pool: BitmapPool,
    toTransform: Bitmap,
    outWidth: Int,
    outHeight: Int
  ): Bitmap {
    return BlurImageUtil.blurRenderScript(toTransform, radius, context)
  }

  override fun updateDiskCacheKey(messageDigest: MessageDigest) {
    messageDigest.update(("$ID$radius").toByteArray(CHARSET))
  }

  override fun equals(other: Any?): Boolean =
    this === other || radius == (other as? GlideBlurTransformation)?.radius

  override fun hashCode(): Int = 31 * radius.hashCode() + ID.hashCode()

  companion object {
    private const val ID = "com.hoc.weatherapp.utils.blur.GlideBlurTransformation"
  }
}
