package com.hoc.weatherapp.utils.ui

import android.annotation.SuppressLint
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ItemTouchHelper.ACTION_STATE_SWIPE
import androidx.recyclerview.widget.ItemTouchHelper.LEFT
import androidx.recyclerview.widget.ItemTouchHelper.RIGHT
import androidx.recyclerview.widget.RecyclerView
import com.hoc.weatherapp.R

enum class ButtonState {
  GONE,
  LEFT_VISIBLE,
  RIGHT_VISIBLE
}

interface SwipeControllerActions {
  fun onLeftClicked(adapterPosition: Int)
  fun onRightClicked(adapterPosition: Int)
}

class SwipeController(private val buttonsActions: SwipeControllerActions) :
  ItemTouchHelper.Callback() {
  private var buttonShowedState: ButtonState =
    ButtonState.GONE
  private var swipeBack: Boolean = false
  private val buttonWidth = 192f
  private var currentViewHolder: RecyclerView.ViewHolder? = null
  private var buttonInstance: RectF? = null

  override fun getMovementFlags(
    recyclerView: RecyclerView,
    viewHolder: RecyclerView.ViewHolder
  ): Int {
    return makeMovementFlags(0, LEFT or RIGHT)
  }

  override fun onMove(
    recyclerView: RecyclerView,
    viewHolder: RecyclerView.ViewHolder,
    target: RecyclerView.ViewHolder
  ): Boolean {
    return false
  }

  override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
  }

  override fun convertToAbsoluteDirection(flags: Int, layoutDirection: Int): Int {
    if (swipeBack) {
      swipeBack = buttonShowedState != ButtonState.GONE
      return 0
    }
    return super.convertToAbsoluteDirection(flags, layoutDirection)
  }

  override fun onChildDraw(
    c: Canvas,
    recyclerView: RecyclerView,
    viewHolder: RecyclerView.ViewHolder,
    dX: Float,
    dY: Float,
    actionState: Int,
    isCurrentlyActive: Boolean
  ) {
    @Suppress("NAME_SHADOWING") var dX = dX

    if (actionState == ACTION_STATE_SWIPE) {
      if (buttonShowedState != ButtonState.GONE) {
        if (buttonShowedState == ButtonState.LEFT_VISIBLE) dX = maxOf(dX, buttonWidth)
        if (buttonShowedState == ButtonState.RIGHT_VISIBLE) dX = minOf(dX, -buttonWidth)
        super.onChildDraw(
          c,
          recyclerView,
          viewHolder,
          dX,
          dY,
          actionState,
          isCurrentlyActive
        )
      } else {
        setTouchListener(
          c,
          recyclerView,
          viewHolder,
          dX,
          dY,
          actionState,
          isCurrentlyActive
        )
      }
    }

    if (buttonShowedState == ButtonState.GONE) {
      super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
    }
    currentViewHolder = viewHolder
  }

  @SuppressLint("ClickableViewAccessibility")
  private fun setTouchListener(
    c: Canvas,
    recyclerView: RecyclerView,
    viewHolder: RecyclerView.ViewHolder,
    dX: Float,
    dY: Float,
    actionState: Int,
    isCurrentlyActive: Boolean
  ) {
    recyclerView.setOnTouchListener { _, event ->
      swipeBack = event.action == MotionEvent.ACTION_CANCEL || event.action ==
        MotionEvent.ACTION_UP
      if (swipeBack) {
        when {
          dX < -buttonWidth ->
            buttonShowedState =
              ButtonState.RIGHT_VISIBLE
          dX > buttonWidth ->
            buttonShowedState =
              ButtonState.LEFT_VISIBLE
        }

        if (buttonShowedState != ButtonState.GONE) {
          setTouchDownListener(
            c,
            recyclerView,
            viewHolder,
            dX,
            dY,
            actionState,
            isCurrentlyActive
          )
          setItemsClickable(recyclerView, false)
        }
      }
      false
    }
  }

  @SuppressLint("ClickableViewAccessibility")
  private fun setTouchDownListener(
    c: Canvas,
    recyclerView: RecyclerView,
    viewHolder: RecyclerView.ViewHolder,
    dX: Float,
    dY: Float,
    actionState: Int,
    isCurrentlyActive: Boolean
  ) {
    recyclerView.setOnTouchListener { _, event ->
      if (event.action == MotionEvent.ACTION_DOWN && event.action != MotionEvent.ACTION_MOVE) {
        setTouchUpListener(
          c,
          recyclerView,
          viewHolder,
          dX,
          dY,
          actionState,
          isCurrentlyActive
        )
      }
      false
    }
  }

  @SuppressLint("ClickableViewAccessibility")
  private fun setTouchUpListener(
    c: Canvas,
    recyclerView: RecyclerView,
    viewHolder: RecyclerView.ViewHolder,
    @Suppress("UNUSED_PARAMETER") dX: Float,
    dY: Float,
    actionState: Int,
    isCurrentlyActive: Boolean
  ) {
    recyclerView.setOnTouchListener { _, event ->
      if (event.action == MotionEvent.ACTION_UP) {
        super@SwipeController.onChildDraw(
          c,
          recyclerView,
          viewHolder,
          0f,
          dY,
          actionState,
          isCurrentlyActive
        )
        recyclerView.setOnTouchListener { _, _ -> false }
        setItemsClickable(recyclerView, true)
        swipeBack = false

        if (buttonInstance != null && buttonInstance?.contains(event.x, event.y) == true) {
          when (buttonShowedState) {
            ButtonState.LEFT_VISIBLE -> buttonsActions.onLeftClicked(viewHolder.adapterPosition)
            ButtonState.RIGHT_VISIBLE -> buttonsActions.onRightClicked(viewHolder.adapterPosition)
            else -> Unit
          }
        }
        buttonShowedState = ButtonState.GONE
        currentViewHolder = null
      }
      false
    }
  }

  private fun setItemsClickable(recyclerView: RecyclerView, isClickable: Boolean) {
    for (i in 0 until recyclerView.childCount) {
      recyclerView.getChildAt(i).isClickable = isClickable
    }
  }

  private fun drawButtons(c: Canvas, viewHolder: RecyclerView.ViewHolder) {
    val buttonWidthWithoutPadding = buttonWidth - 16
    val corners = 2f

    val itemView = viewHolder.itemView
    val p = Paint()

    val leftButton = RectF(
      itemView.left.toFloat() + 4,
      itemView.top.toFloat() + 8,
      itemView.left + buttonWidthWithoutPadding,
      itemView.bottom.toFloat() - 8
    )
    p.color = ContextCompat.getColor(itemView.context, R.color.colorPrimary)
    c.drawRoundRect(leftButton, corners, corners, p)
    drawText("REFRESH", c, leftButton, p)

    val rightButton = RectF(
      itemView.right - buttonWidthWithoutPadding,
      itemView.top.toFloat() + 8,
      itemView.right.toFloat() - 4,
      itemView.bottom.toFloat() - 8
    )
    p.color = ContextCompat.getColor(itemView.context, R.color.colorAccent)
    c.drawRoundRect(rightButton, corners, corners, p)
    drawText("DELETE", c, rightButton, p)

    buttonInstance = null
    if (buttonShowedState == ButtonState.LEFT_VISIBLE) {
      buttonInstance = leftButton
    } else if (buttonShowedState == ButtonState.RIGHT_VISIBLE) {
      buttonInstance = rightButton
    }
  }

  private fun drawText(text: String, c: Canvas, button: RectF, p: Paint) {
    val textSize = 20f
    p.run {
      color = Color.WHITE
      isAntiAlias = true
      this.textSize = textSize
    }

    val textWidth = p.measureText(text)
    c.drawText(text, button.centerX() - textWidth / 2, button.centerY() + textSize / 2, p)
  }

  fun onDraw(c: Canvas) {
    currentViewHolder?.let { drawButtons(c, it) }
  }
}
