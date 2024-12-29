package com.example.bluefalconcomposemultiplatform.di

import android.content.Context
import dev.bluefalcon.ApplicationContext
import dev.bluefalcon.BlueFalcon

actual class AppModule(
    private val context: Context
) {
    actual val blueFalcon: BlueFalcon
        get() = BlueFalcon(context as ApplicationContext, "4775DD3A-3BB7-4E23-9E4E-91A20D43E491")
}