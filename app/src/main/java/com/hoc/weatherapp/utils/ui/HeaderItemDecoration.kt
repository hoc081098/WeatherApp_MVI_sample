package com.hoc.weatherapp.utils.ui

import android.graphics.Canvas
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding

class HeaderItemDecoration<VB : ViewBinding>(private val listener: StickyHeaderInterface<VB>) :
  RecyclerView.ItemDecoration() {
  private var headerViewBinding: VB? = null

  override fun onDrawOver(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
    super.onDrawOver(c, parent, state)

    val topChildPosition = parent.getChildAdapterPosition(parent.getChildAt(0) ?: return)
    if (topChildPosition == RecyclerView.NO_POSITION) return

    val headerPosition = listener.getHeaderPositionForItem(topChildPosition) ?: return

    val headerView = headerViewBinding?.root ?: listener.viewBinding(parent)
      .also {
        headerViewBinding = it
        fixLayoutSize(parent, it.root)
      }
      .root

    listener.bindHeaderData(headerViewBinding!!, headerPosition)

    val childInContact = getChildInContact(parent, headerView.bottom)

    if (childInContact != null &&
      parent.getChildAdapterPosition(childInContact)
        .let { it != RecyclerView.NO_POSITION && listener.isHeader(it) }
    ) {
      moveHeader(c, headerView, childInContact)
      return
    }

    drawHeader(c, headerView)
  }

  private fun drawHeader(c: Canvas, header: View) {
    c.save()
    c.translate(0F, 0F)
    header.draw(c)
    c.restore()
  }

  private fun moveHeader(c: Canvas, currentHeader: View, nextHeader: View?) {
    c.save()
    c.translate(0F, (nextHeader!!.top - currentHeader.height).toFloat())
    currentHeader.draw(c)
    c.restore()
  }

  private fun getChildInContact(parent: RecyclerView, contactPoint: Int): View? {
    return (0 until parent.childCount)
      .asSequence()
      .map { parent.getChildAt(it) }
      .find { contactPoint < it.bottom && it.top <= contactPoint }
  }

  private fun fixLayoutSize(parent: ViewGroup, view: View) {
    // Specs for parent (RecyclerView)
    val widthSpec = View.MeasureSpec.makeMeasureSpec(parent.width, View.MeasureSpec.EXACTLY)
    val heightSpec = View.MeasureSpec.makeMeasureSpec(parent.height, View.MeasureSpec.UNSPECIFIED)

    // Specs for children (headers)
    val childWidthSpec = ViewGroup.getChildMeasureSpec(
      widthSpec,
      parent.paddingLeft + parent.paddingRight,
      view.layoutParams.width
    )
    val childHeightSpec = ViewGroup.getChildMeasureSpec(
      heightSpec,
      parent.paddingTop + parent.paddingBottom,
      view.layoutParams.height
    )

    view.measure(childWidthSpec, childHeightSpec)

    view.layout(0, 0, view.measuredWidth, view.measuredHeight)
  }

  interface StickyHeaderInterface<VB : ViewBinding> {
    fun viewBinding(parent: RecyclerView): VB

    fun getHeaderPositionForItem(itemPosition: Int): Int?

    fun bindHeaderData(binding: VB, headerPosition: Int)

    fun isHeader(itemPosition: Int): Boolean
  }
}
