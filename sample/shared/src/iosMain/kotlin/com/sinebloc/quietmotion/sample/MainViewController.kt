package com.sinebloc.quietmotion.sample

import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController

/** The single entry point the Xcode project in sample/iosApp hosts. */
fun MainViewController(): UIViewController = ComposeUIViewController { App() }
