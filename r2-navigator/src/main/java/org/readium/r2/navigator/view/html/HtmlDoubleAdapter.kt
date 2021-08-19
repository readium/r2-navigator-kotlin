package org.readium.r2.navigator.view.html

import android.content.Context
import android.view.View
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import org.readium.r2.navigator.view.ResourceAdapter
import org.readium.r2.navigator.view.SpreadAdapter
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Metadata
import kotlin.coroutines.CoroutineContext

internal class HtmlDoubleAdapter : SpreadAdapter, CoroutineScope {

    override val coroutineContext: CoroutineContext = Dispatchers.Main

    override fun makeSpread(links: List<Link>, metadata: Metadata): Boolean {
        TODO("Not yet implemented")
    }

    override fun createView(context: Context): View {
        TODO("Not yet implemented")
    }

    override fun bindSpread(spread: List<Link>, view: View): Job {
        TODO("Not yet implemented")
    }

    override fun scrollTo(locations: Locator.Locations, view: View) {

    }

    override fun resourceAdapters(view: View): List<ResourceAdapter> {
        TODO("Not yet implemented")
    }
}