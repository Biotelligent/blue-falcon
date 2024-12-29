package com.example.bluefalconcomposemultiplatform.di

import dev.bluefalcon.ApplicationContext
import dev.bluefalcon.BlueFalcon

actual class AppModule {
    actual val blueFalcon: BlueFalcon
        get() = BlueFalcon(ApplicationContext(), "4775DD3A-3BB7-4E23-9E4E-91A20D43E491")
}