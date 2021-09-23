# Changelog

All notable changes to this project will be documented in this file.

**Warning:** Features marked as *alpha* may change or be removed in a future release without notice. Use with caution.

<!-- ## [Unreleased] -->

## [2.1.0]

### Added

* The EPUB navigator is now able to navigate to a `Locator` using its `text` context. This is useful for search results or highlights missing precise locations.
* Get or clear the current user selection of the navigators implementing `SelectableNavigator`.
* (*alpha*) Support for the [Decorator API](https://github.com/readium/architecture/pull/160) to draw user interface elements over a publication's content.
    * This can be used to render highlights over a text selection, for example.
    * For now, only the EPUB navigator implements `DecorableNavigator`, for reflowable publications. You can implement custom decoration styles with `HtmlDecorationTemplate`.
* Customize the EPUB selection context menu by providing a custom `ActionMode.Callback` implementation with `EpubNavigatorFragment.Configuration.selectionActionModeCallback`.
    * This is an alternative to overriding `Activity.onActionModeStarted()` which does not seem to work anymore with Android 12.
* (*alpha*) A new audiobook navigator based on Android's [`MediaSession`](https://developer.android.com/guide/topics/media-apps/working-with-a-media-session).
    * It supports out-of-the-box media style notifications and background playback.
    * ExoPlayer is used by default for the actual playback, but you can use a custom player by implementing `MediaPlayer`.

### Changed

* Upgraded to Kotlin 1.5.31 and Gradle 7.1.1
* The order of precedence of `Locator` locations in the reflowable EPUB navigator is: `text`, HTML ID, then `progression`. The navigator will now fallback on less precise locations in case of failure.

### Fixed

* When restoring a `Locator`, The PDF navigator now falls back on `locations.position` if the `page=` fragment identifier is missing.


## [2.0.0]

### Fixed

* Scrolling to an EPUB ID (e.g. from the table of contents) when the target spans several screens.


## [2.0.0-beta.2]

### Changed

* `R2EpubActivity` and `R2AudiobookActivity` require a new `baseUrl` `Intent` extra. You need to set it to the base URL returned by `Server.addPublication()` from the Streamer.

### Fixed

* [#217](https://github.com/readium/r2-testapp-kotlin/issues/217) Interactive HTML elements are not bypassed anymore when handling touch gestures.
  * Scripts using `preventDefault()` are now taken into account and do not trigger a tap event anymore.
* [#150](https://github.com/readium/r2-navigator-kotlin/issues/150) External links are opened in a Chrome Custom Tab instead of the navigator's web view.
* [#52](https://github.com/readium/r2-navigator-kotlin/issues/52) Memory leak in EPUB web views. This fixes ongoing media playback when closing an EPUB.


## [2.0.0-beta.1]

### Added

* Support for [display cutouts](https://developer.android.com/guide/topics/display-cutout) (screen notches).
    * **IMPORTANT**: You need to remove any `setPadding()` statement from your app in `UserSettings.kt`, if you copied it from the test app.
    * If you embed a navigator fragment (e.g. `EpubNavigatorFragment`) yourself, you need to opt-in by [specifying the `layoutInDisplayCutoutMode`](https://developer.android.com/guide/topics/display-cutout#choose_how_your_app_handles_cutout_areas) of the host `Activity`.
    * `R2EpubActivity` and `R2CbzActivity` automatically apply `LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES` to their window's `layoutInDisplayCutoutMode`.
    * `PdfNavigatorFragment` is not yet compatible with display cutouts, because of limitations from the underlying PDF viewer.
* Customize EPUB vertical padding by overriding the `r2.navigator.epub.vertical_padding` dimension.
    * Follow [Android's convention for alternative resources](https://developer.android.com/guide/topics/resources/providing-resources#AlternativeResources) to specify different paddings for landscape (`values-land`) or large screens.

### Changed

* Upgraded to Kotlin 1.4.10.
* All `utils.js` functions were moved under a `readium.` namespace. You will need to update your code if you were calling them manually.

### Fixed

* EPUBs declaring multiple languages were laid out from right to left if the first language had an RTL reading
progression. Now if no reading progression is set, the `effectiveReadingProgression` will be LTR.
* [#152](https://github.com/readium/r2-navigator-kotlin/issues/152) Panning through a zoomed-in fixed layout EPUB (contributed by [@johanpoirier](https://github.com/readium/r2-navigator-kotlin/pull/172)).
* [#146](https://github.com/readium/r2-navigator-kotlin/issues/146) Various reflowable EPUB columns shift issues.
* Restoring the last EPUB location after configuration changes (e.g. screen rotation).
* Edge taps to turn pages when the app runs in a multi-windows environment.


## [2.0.0-alpha.2]

### Added

* Support for the new `Publication` model using the [Content Protection](https://readium.org/architecture/proposals/006-content-protection) for DRM rights and the [Fetcher](https://readium.org/architecture/proposals/002-composite-fetcher-api) for resource access.
* (*alpha*) New `Fragment` implementations as an alternative to the legacy `Activity` ones (contributed by [@johanpoirier](https://github.com/readium/r2-navigator-kotlin/pull/148)).
  * The fragments are chromeless, to let you customize the reading UX.
  * To create the fragments use the matching factory such as `EpubNavigatorFragment.createFactory()`, as showcased in `R2EpubActivity`.
  * At the moment, highlights and TTS are not yet supported in the new EPUB navigator `Fragment`.
  * [This is now the recommended way to integrate Readium](https://github.com/readium/r2-navigator-kotlin/issues/115) in your applications.

### Changed

* `currentLocator` is now a `StateFlow` instead of `LiveData`, to better support chromeless navigators such as an audiobook navigator.
  * If you were observing `currentLocator` in a UI context, you can continue to do so with `currentLocator.asLiveData()`.
* Improvements to the PDF navigator:
  * The navigator doesn't require PDF publications to be served from an HTTP server anymore. A side effect is that the navigator is now able to open larger PDF files.
  * `PdfNavigatorFragment.Listener::onResourceLoadFailed()` can be used to report fatal errors to the user, such as when trying to open a PDF document that is too large for the available memory.
  * A dedicated `PdfNavigatorFragment.createFactory()` was added, which deprecates the use of `NavigatorFragmentFactory`.

### Fixed

* Prevent switching to the next resource by mistake when scrolling through an EPUB resource in scroll mode.


## [2.0.0-alpha.1]

### Added

* The [position](https://github.com/readium/architecture/tree/master/models/locators/positions) is now reported in the locators for EPUB, CBZ and PDF.
* (*alpha*) [PDF navigator](https://github.com/readium/r2-navigator-kotlin/pull/130).
  * Supports both single PDF and LCP protected PDF.
  * As a proof of concept, [it is implemented using `Fragment` instead of `Activity`](https://github.com/readium/r2-navigator-kotlin/issues/115). `R2PdfActivity` showcases how to use the `PdfNavigatorFragment` with the new `NavigatorFragmentFactory`.
  * The navigator is based on [AndroidPdfViewer](https://github.com/barteksc/AndroidPdfViewer), which may increase the size of your apps. Please open an issue if this is a problem for you, as we are considering different solutions to fix this in a future release.

### Changed

* [Upgraded to Readium CSS 1.0.0-beta.1.](https://github.com/readium/r2-navigator-kotlin/pull/134)
  * Two new fonts are available: AccessibleDfa and IA Writer Duospace.
  * The file structure now follows strictly the one from [ReadiumCSS's `dist/`](https://github.com/readium/readium-css/tree/master/css/dist), for easy upgrades and custom builds replacement.

### Deprecated

* `Navigator.currentLocation` and `NavigatorDelegate.locationDidChange()` are deprecated in favor of a unified `Navigator.currentLocator`, which is observable thanks to `LiveData`.

### Fixed

* **Important:** [Publications parsed from large manifests could crash the application](https://github.com/readium/r2-testapp-kotlin/issues/286) when starting a reading activity. To fix this, **`Publication` must not be put in an `Intent` extra anymore**. Instead, [use the new `Intent` extensions provided by Readium](https://github.com/readium/r2-testapp-kotlin/pull/303). This solution is a crutch until [we move away from `Activity` in the Navigator](https://github.com/readium/r2-navigator-kotlin/issues/115) and let reading apps handle the lifecycle of `Publication` themselves.
* [Crash when opening a publication with a space in its filename](https://github.com/readium/r2-navigator-kotlin/pull/136).
* [Jumping to an EPUB location from the search](https://github.com/readium/r2-navigator-kotlin/pull/111).
* The `AndroidManifest.xml` is not forcing anymore `allowBackup` and `supportsRtl`, to let reading apps manage these features themselves (contributed by [@twaddington](https://github.com/readium/r2-navigator-kotlin/pull/118)).


[unreleased]: https://github.com/readium/r2-navigator-kotlin/compare/master...HEAD
[2.0.0-alpha.1]: https://github.com/readium/r2-navigator-kotlin/compare/1.1.6...2.0.0-alpha.1
[2.0.0-alpha.2]: https://github.com/readium/r2-navigator-kotlin/compare/2.0.0-alpha.1...2.0.0-alpha.2
[2.0.0-beta.1]: https://github.com/readium/r2-navigator-kotlin/compare/2.0.0-alpha.2...2.0.0-beta.1
[2.0.0-beta.2]: https://github.com/readium/r2-navigator-kotlin/compare/2.0.0-beta.1...2.0.0-beta.2
[2.0.0]: https://github.com/readium/r2-navigator-kotlin/compare/2.0.0-beta.2...2.0.0
[2.1.0]: https://github.com/readium/r2-navigator-kotlin/compare/2.0.0...2.1.0

