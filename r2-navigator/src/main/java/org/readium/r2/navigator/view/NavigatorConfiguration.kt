package org.readium.r2.navigator.view

import android.graphics.Bitmap
import android.util.Size
import org.readium.r2.navigator.view.layout.DefaultLayoutPolicy
import org.readium.r2.navigator.view.layout.LayoutPolicy

data class NavigatorConfiguration(
    val errorBitmap: (Size) -> Bitmap = { Bitmap.createBitmap(it.width, it.height, Bitmap.Config.ALPHA_8) },
    val emptyBitmap: (Size) -> Bitmap = { Bitmap.createBitmap(it.width, it.height, Bitmap.Config.ALPHA_8) },
    val spreadAdapters: List<SpreadAdapter> = emptyList(),
    val ignoreDefaultSpreadAdapters: Boolean = false,
    val layoutPolicy: LayoutPolicy = DefaultLayoutPolicy
)
