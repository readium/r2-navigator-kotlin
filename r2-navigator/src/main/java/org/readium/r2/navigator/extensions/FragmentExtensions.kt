package org.readium.r2.navigator.extensions

import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment

/** returns true if the resolved layout direction of the content view in this
 * activity is ViewCompat.LAYOUT_DIRECTION_RTL. Otherwise false. */
fun Fragment.layoutDirectionIsRTL(): Boolean {
    view?.let {
        (it.context as? AppCompatActivity)?.let { activity ->
            return ViewCompat.getLayoutDirection(activity.findViewById(android.R.id.content)) == ViewCompat.LAYOUT_DIRECTION_RTL
        }
    }

    return false
}