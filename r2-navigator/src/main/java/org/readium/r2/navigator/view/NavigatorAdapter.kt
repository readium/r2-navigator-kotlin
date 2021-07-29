package org.readium.r2.navigator.view

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Job
import org.readium.r2.navigator.view.layout.LayoutComputer
import org.readium.r2.navigator.view.layout.Spread
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.ReadingProgression
import org.readium.r2.shared.publication.indexOfFirstWithHref
import org.readium.r2.shared.publication.presentation.page

internal class NavigatorAdapter(
    private val context: Context,
    private val adapters: List<SpreadAdapter>,
    private val publication: Publication,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    class ViewHolder(
        val view: View,
        var loadingJob: Job? = null
    ) : RecyclerView.ViewHolder(view)

    private val spreads: List<Spread> =
        LayoutComputer(publication.metadata.effectiveReadingProgression == ReadingProgression.RTL)
            .compute(publication.readingOrder.map { it.properties.page })

    private val spreadAdapters: List<SpreadAdapter> =
            spreads.map { spread -> adapters.first { adapter -> adapter.supportsSpread(spread)} }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val adapter = adapters[viewType]
        val view = adapter.createView(context)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        spreadAdapters[position].bindSpread(spreads[position], holder.itemView)
        holder.itemView.tag = position
    }

    override fun getItemCount(): Int =
        spreadAdapters.size

    override fun getItemViewType(position: Int): Int =
        adapters.indexOfFirst { it === spreadAdapters[position] }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        super.onViewRecycled(holder)
        holder.itemView.tag = null
    }

    fun positionForHref(href: String): Int {
        val resourceIndex = publication.readingOrder.indexOfFirstWithHref(href)!!
        return spreads.indexOfFirst {
            (it is Spread.SinglePage && it.page == resourceIndex ||
                it is Spread.DoublePage && (it.left == resourceIndex || it.right == resourceIndex)) }
    }

    fun scrollTo(locations: Locator.Locations, view: View, position: Int) {
        spreadAdapters[position].scrollTo(locations, view)
    }
}