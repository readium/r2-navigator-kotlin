package org.readium.r2.navigator.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager.VERTICAL
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.HORIZONTAL
import org.readium.r2.navigator.view.layout.EffectiveReadingProgression

internal class ReadingView(context: Context, attributes: AttributeSet) : View(context, attributes)  {
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

    fun <VH: RecyclerView.ViewHolder> setAdapter(adapter: RecyclerView.Adapter<VH>) {
        this.recyclerView.adapter = adapter
    }

    fun scrollToPosition(position: Int) {
        return this.recyclerView.scrollToPosition(position)
    }

    fun findViewByPosition(position: Int): View? {
        return this.layoutManager.findViewByPosition(position)
    }
}