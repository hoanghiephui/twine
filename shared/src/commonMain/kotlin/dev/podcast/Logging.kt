package dev.podcast

import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity

fun configureLogger(isDebug: Boolean = false) {
    Logger.setTag("CoinDex")
    Logger.setMinSeverity(
        if (isDebug) Severity.Verbose else Severity.Error
    )
}