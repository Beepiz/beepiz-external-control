package com.example.beepizcontrol

interface BeepizControlUi {

    /**
     * Must enable the UI control that allows the user to request start of Beepiz monitoring,
     * and return/resume when the user acts on it (e.g clicking the button).
     */
    suspend fun awaitStartMonitoringRequest()

    /**
     * Must enable the UI control that allows the user to request stop of Beepiz monitoring,
     * and return/resume when the user acts on it (e.g clicking the button plus confirming).
     */
    suspend fun awaitStopMonitoringRequest()
}
