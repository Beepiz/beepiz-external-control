package com.example.beepizcontrol

import android.os.Message
import android.os.Messenger

object BeepizBindingConstants {

    const val packageName = "net.suivideflotte.dati"

    const val permission = "$packageName.permission.CONTROL_MONITORING"

    const val bindAction = "bind_monitoring_state"

    /**
     * You need to put it in the [Message.what] sent to Beepiz for your app to listen the
     * monitoring state.
     *
     * **IMPORTANT:** You need to pass the same [Messenger] in [Message.replyTo]. Otherwise, your
     * request will be ignored.
     */
    const val REGISTER_CLIENT = 1

    /**
     * You need to put it in the [Message.what] sent to Beepiz once your app no longer needs to
     * listen the monitoring state.
     *
     * **IMPORTANT:** You need to pass the same [Messenger] in [Message.replyTo]. Otherwise, your
     * request will be ignored.
     */
    const val UNREGISTER_CLIENT = 0

    /**
     * Put in [Message.what] coming from Beepiz when monitoring is active.
     */
    const val CURRENTLY_MONITORING = 1

    /**
     * Put in [Message.what] coming from Beepiz when not monitoring.
     */
    const val CURRENTLY_NOT_MONITORING = 0

    /**
     * Put in [Message.arg1] coming from Beepiz if Beepiz need to be open to fix configuration.
     */
    const val ARG1_REQUIRES_CONFIG = 1

    /**
     * Put in [Message.arg1] coming from Beepiz if configuration is ok.
     */
    const val ARG1_CONFIG_OK = 0

    /**
     * Action to broadcast to Beepiz to start monitoring. Be sure to specify the Intent
     * targetPackage or the system will ignore it.
     */
    const val startAction = "start_monitoring"

    /**
     * Action to broadcast to Beepiz to stop monitoring. Be sure to specify the Intent
     * targetPackage or the system will ignore it.
     */
    const val stopAction = "stop_monitoring"
}
