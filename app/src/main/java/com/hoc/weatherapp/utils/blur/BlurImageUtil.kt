package com.hoc.weatherapp.utils.blur

import android.content.Context
import android.graphics.Bitmap
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur

object BlurImageUtil {
  fun blurRenderScript(bitmap: Bitmap, radius: Float, context: Context): Bitmap {
    val applicationContext = context.applicationContext
    var rsContext: RenderScript? = null

    try {
      val output = Bitmap.createBitmap(
        bitmap.width,
        bitmap.height,
        Bitmap.Config.ARGB_8888
      )

      // Create a RenderScript context.
      rsContext = RenderScript.create(applicationContext, RenderScript.ContextType.NORMAL)

      // Creates a RenderScript allocation for the blurred result.
      val inAlloc = Allocation.createFromBitmap(rsContext, bitmap)
      val outAlloc = Allocation.createTyped(rsContext, inAlloc.type)

      // Use the ScriptIntrinsicBlur intrinsic.
      ScriptIntrinsicBlur.create(rsContext, Element.U8_4(rsContext))
        .run {
          setRadius(radius)
          setInput(inAlloc)
          forEach(outAlloc)
        }

      // Copy to the output bitmap from the allocation.
      outAlloc.copyTo(output)
      return output
    } finally {
      rsContext?.finish()
    }
  }
}
