package org.readium.r2.navigator.controller

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import org.readium.r2.navigator.epub.Highlight
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import kotlin.properties.ObservableProperty

interface NavigatorController {

    sealed class Event {

        object HighlightActivated

    }

    val currentLocator: Flow<Locator>

    val highlights: ObservableProperty<Highlight>

    val events: Channel<Event>

    fun loadPublication(publication: Publication)


}