package org.readium.r2.navigator.view

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager.VERTICAL
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.*
import org.readium.r2.navigator.view.layout.EffectiveReadingProgression

internal class ReadingView(context: Context, attributes: AttributeSet? = null) : View(context, attributes)  {
    private val recyclerView: RecyclerView = RecyclerView(context)
    private val layoutManager: LinearLayoutManager = LinearLayoutManager(context)
    private val pagerSnapHelper: PagerSnapHelper = PagerSnapHelper()

    init {
        recyclerView.layoutManager = layoutManager
        recyclerView.layoutDirection = LAYOUT_DIRECTION_LTR
        pagerSnapHelper.attachToRecyclerView(recyclerView)
    }

    fun setReadingProgression(progression: EffectiveReadingProgression) {
        this.layoutManager.reverseLayout = when (progression) {
            EffectiveReadingProgression.LTR, EffectiveReadingProgression.TTB -> false
            EffectiveReadingProgression.RTL, EffectiveReadingProgression.BTT -> true
        }

        this.layoutManager.orientation = if (progression.isHorizontal) HORIZONTAL else VERTICAL
        this.layoutManager.requestLayout()
    }

    fun setContinuous(continuous: Boolean) {
        this.pagerSnapHelper.attachToRecyclerView(if (continuous) null else recyclerView)
    }

    fun <VH: ViewHolder> setAdapter(adapter: Adapter<VH>) {
        this.recyclerView.adapter = adapter
    }

    fun scrollToPosition(position: Int) {
        return this.recyclerView.scrollToPosition(position)
    }

    fun findViewByPosition(position: Int): View? {
        return this.layoutManager.findViewByPosition(position)
    }

    fun findFirstVisiblePosition(): Int {
        return this.layoutManager.findFirstVisibleItemPosition()
    }

    fun setOnScrollListener(listener: () -> Unit) {
        this.recyclerView.addOnScrollListener(object : OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
            }

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                listener()
            }
        })
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        this.recyclerView.measure(widthMeasureSpec, heightMeasureSpec)
    }

    override fun onDraw(canvas: Canvas?) {
        this.recyclerView.draw(canvas)
    }
}