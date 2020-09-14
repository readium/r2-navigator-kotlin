/*
 * Copyright 2020 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.audio

import android.net.Uri
import com.google.android.exoplayer2.C.LENGTH_UNSET
import com.google.android.exoplayer2.C.RESULT_END_OF_INPUT
import com.google.android.exoplayer2.upstream.BaseDataSource
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DataSpec
import com.google.android.exoplayer2.upstream.TransferListener
import kotlinx.coroutines.runBlocking
import org.readium.r2.shared.fetcher.ResourceInputStream
import org.readium.r2.shared.publication.Publication
import java.io.IOException
import java.io.InputStream

/**
 * An ExoPlayer's [DataSource] which retrieves resources from a [Publication].
 */
internal class PublicationDataSource(private val publication: Publication) : BaseDataSource(/* isNetwork = */ false) {

    class Factory(private val publication: Publication, private val transferListener: TransferListener? = null) : DataSource.Factory {

        override fun createDataSource(): DataSource =
            PublicationDataSource(publication).apply {
                if (transferListener != null) {
                    addTransferListener(transferListener)
                }
            }

    }

    sealed class Exception(message: String, cause: Throwable?) : IOException(message, cause) {
        class NotOpened(message: String) : Exception(message, null)
        class NotFound(message: String) : Exception(message, null)
        class ReadFailed(uri: Uri, offset: Int, readLength: Int, cause: Throwable) : Exception("Failed to read $readLength bytes of URI $uri at offset $offset.", cause)
    }

    private data class OpenedResource(
        val inputStream: InputStream,
        val uri: Uri,
        var bytesRemaining: Long
    )

    private var openedResource: OpenedResource? = null

    override fun open(dataSpec: DataSpec): Long {
        close()

        val link = publication.linkWithHref(dataSpec.uri.toString())
            ?: throw Exception.NotFound("Can't find a [Link] for URI: ${dataSpec.uri}. Make sure you only request resources declared in the manifest.")

        val inputStream = ResourceInputStream(publication.get(link), autocloseResource = true)
            // Significantly improves performances, in particular with deflated ZIP entries.
            .buffered()

        inputStream.skip(dataSpec.position)

        val bytesRemaining =
            if (dataSpec.length == LENGTH_UNSET.toLong()) inputStream.available().toLong()
            else dataSpec.length

        openedResource = OpenedResource(inputStream, dataSpec.uri, bytesRemaining = bytesRemaining)
        return bytesRemaining
    }

    override fun read(target: ByteArray, offset: Int, length: Int): Int {
        val openedResource = openedResource ?: throw Exception.NotOpened("No opened resource to read from. Did you call open()?")

        when {
            (length <= 0) -> return 0
            (openedResource.bytesRemaining == 0.toLong()) -> return RESULT_END_OF_INPUT
        }

        val bytesRead = try {
            val bytesToRead = length.coerceAtMost(openedResource.bytesRemaining.toInt())
            openedResource.inputStream.read(target, offset, bytesToRead)
        } catch (e: Exception) {
            throw Exception.ReadFailed(uri = openedResource.uri, offset = offset, readLength = length, cause = e)
        }

        if (bytesRead == -1) {
            return RESULT_END_OF_INPUT
        }

        openedResource.bytesRemaining -= bytesRead
        return bytesRead
    }

    override fun getUri(): Uri? = openedResource?.uri

    override fun close() {
        openedResource?.run {
            runBlocking { inputStream.close() }
        }
        openedResource = null
    }

}
