package tti

import android.app.Activity
import android.content.Context
import android.view.ActionMode
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import org.readium.r2.navigator.IR2Activity
import org.readium.r2.navigator.R2BasicWebView
import org.readium.r2.streamer.fetcher.Fetcher

open class NavigatorExtension{
    var epubActivityClass:Class<out Activity>? = null
    open fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
        return null
    }
    open fun addJavascriptInterface(view:WebView){

    }
    open fun onServerStart(appContext: Context){

    }

    //返回值表示是否由扩展处理数据请求
    open fun serveEpub(book:String,fetcher: Fetcher): Boolean {
        return false
    }

    open fun urlForEpub(publicationFileName: String, href: String?):String?{
        return null
    }

    open fun onCreateReader(epubActivity: Activity) {

    }

    open fun onPageLoaded(epubActivity: IR2Activity, webView: R2BasicWebView) {

    }

    open fun onActionModeStarted(epubActivity: Activity,mode: ActionMode?){

    }

    companion object{
        val allExtension = mutableListOf<NavigatorExtension>()

        fun addExtension(e:NavigatorExtension){
            allExtension.add(e)
        }
        fun getEpubActivityClass(): Class<out Activity>? {
            for(e in allExtension){
                if(e.epubActivityClass!=null){
                    return e.epubActivityClass
                }
            }
            return null
        }
    }
}

interface NavigatorInterface{
    fun addHighlight(highlight: org.readium.r2.navigator.epub.Highlight)
    fun addAnnotation(highlight: org.readium.r2.navigator.epub.Highlight, annotation: String)
}