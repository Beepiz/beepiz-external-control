# beepiz-external-control

Documentation and example to integrate Beepiz monitoring control from a third-party Android app.

Starting with versionCode 165, [the Beepiz Android app](https://play.google.com/store/apps/details?id=net.suivideflotte.dati)
allows third-party apps to do the following:
* Listen Beepiz monitoring state
* Listen if Beepiz requires to be opened for configuration
* Start monitoring
* Stop Monitoring

This is guarded by a "dangerous" permission that the controlling app must declare, and request on
API level 23 and newer.

## About the example application

The example application has been designed to provide leak-free binding with Beepiz, and is
capable of re-establishing the connection automatically after Beepiz process death or package
replacement (caused by update from the Play Store).

It relies on structured-concurrency from kotlinx.coroutines to avoid leaking memory, and the
ServiceConnection that we need to listen to Beepiz monitoring and config state.

Note that it uses Android Gradle Plugin 3.5.0-beta05, but you can downgrade it to 3.4.1 if needed
from the [root build.gradle].

## How to integrate Beepiz controlling in your app

The integration uses Android's built-in secure IPC (inter-process communication), permission
system and broadcast.

All the constants needed in this integration can be found in the [BeepizBindingConstants] file.

### 1. Declare permission usage

First, in the `AndroidManifest.xml` file of a library module in your project, or of the application
module of your app, add the following permission usage declaration under the opening `<manifest>`
tag:

```xml
<uses-permission android:name="net.suivideflotte.dati.permission.CONTROL_MONITORING" />
```

### 2. Request permission for API 23+

See constant `permission` in [BeepizBindingConstants].

The permission mentioned above is marked as dangerous to prevent abuse, so on API level 23
(Android 6) and newer, you'll need to request it at runtime, which will cause the system to ask the
user, and will allow him to revoke it at any time from the device settings (note that revoking a
permission kills the process, so in a process life, you don't have to handle permission being
revoked).

The example application includes an example on how to request a permission with ease in the
[MainActivity], with a single function call to `ensurePermissionOrFinishAndCancel`. In case you
don't want to finish your `Activity` and cancel the current coroutine but want the same ease of use,
you can use `ensurePermission` also available in this sample.

### 3. Bind to the service

See constants `bindAction` and `packageName` in [BeepizBindingConstants].

After ensuring the Beepiz app is installed, create an [Intent] with the action
`"bind_monitoring_state"`, and specify the [`package`][setPackage] of Beepiz (`net.suivideflotte.dati`).

You can then bind to the service using this `Intent` using [`bindService`][bindService] with the
flag `Context.BIND_AUTO_CREATE`.

It is your responsibility to handle `bindService` returning `false`, and managing the
`ServiceConnection` lifecycle to not leak it. The example app includes an example on how to do it
in a simple way at use-site, leveraging cancellable coroutines and basic language constructs like
while loops to recover from disconnections.

You can navigate the source from Android Studio using ctrl/cmd + click, starting from [MainActivity].
Most of the code of the example is reusable (`BeepizControlUiImpl` being an exception), feel free to
make copies into your project.

Android official [documentation on bound services can be found here](
https://developer.android.com/guide/components/bound-services#kotlin
).

### 4. Prepare receiving monitoring and configuration state changes

See constants `CURRENTLY_MONITORING`, `CURRENTLY_NOT_MONITORING`, `ARG1_REQUIRES_CONFIG` and
`ARG1_CONFIG_OK` in [BeepizBindingConstants].

Create a custom [Handler] subclass and override the `handleMessage` function/method.
**Dot not keep a reference to the [Message] instances after `handleMessage` returns** as they are
immediately recycled after it for all sort of events through your app (including user input events).

The `what` field/property of the messages incoming from Beepiz can currently have two values:
* `0` when Beepiz is **not* monitoring (`CURRENTLY_NOT_MONITORING`)
* `1` when Beepiz is currently monitoring (`CURRENTLY_MONITORING`)

The `arg1` field/property can also have two values:
* `0` when Beepiz config is OK (`ARG1_CONFIG_OK`). You can start Beepiz monitoring if not running.
* `1` when Beepiz requires to be launched for user configuration (`ARG1_REQUIRES_CONFIG`).

### 5. Register and unregister state changes

See constants `REGISTER_CLIENT` and `UNREGISTER_CLIENT` in [BeepizBindingConstants].

What is described here must happen when the `onServiceConnected` function/method of your
[ServiceConnection] subclass is called.

Create a [Messenger] instance for your **client**, passing an instance of your custom `Handler` subclass
that you have prepared in previous step.

Create a `Messenger` instance for the **service** from the `IBinder` received in `onServiceConnected`.

Create a register message by calling `Messenger.obtain()`, and apply the following configuration to
it:
1. Set `what` to `1` (`REGISTER_CLIENT`)
2. Set `replyTo` to the **client** `Messenger` instance you have setup before.
3. If the device API level is 22 or newer, call `setAsynchronous(true)` to reduce latency.

Send that register message to the **service** `Messenger` you created.
You'll receive current state in `handleMessage` from your custom `Handler` instance you passed to
your **client** `Messenger`, and all subsequent updates.

When you no longer need to listen to state changes from Beepiz, you need to unregister then unbind
the service.

To unregister, do exactly the same as for registering, but instead of `1`, set `what` to `0`
(`UNREGISTER_CLIENT`). You can then unbind the service using [unbindService].

Beware that all calls to `Messenger.send` can throw a `DeadObjectException` in case Beepiz is killed.
You need to catch it and treat it as a service disconnection. We recommend to unbind the service
when this happens, ensure the app is still installed and retry binding if so.

In the example, the service is simply unbound in the `finally` block, but you may have to do it
differently if you don't use coroutines.

### 6. Starting and stopping Beepiz monitoring

See constants `startAction` and `stopAction` in [BeepizBindingConstants].

When Beepiz does not require to be open for configuration, you can start monitoring directly from
your app by sending a broadcast with the [sendBroadcast] function/method. The `Intent` action needs
to be `"start_monitoring"`, and you have to specify the [`package`][setPackage] of Beepiz
(`net.suivideflotte.dati`).

When Beepiz monitoring is running, you can stop it by doing exactly the same as for starting it,
but passing with the `Intent` action `"stop_monitoring"` instead.

[MainActivity]: /example-app/src/main/java/com/example/beepizcontrol/MainActivity.kt
[root build.gradle]: /build.gradle
[BeepizBindingConstants]: /example-app/src/main/java/com/example/beepizcontrol/BeepizBindingConstants.kt
[Intent]: https://developer.android.com/reference/android/content/Intent.html
[setPackage]: https://developer.android.com/reference/android/content/Intent.html#setPackage(java.lang.String)
[bindService]: https://developer.android.com/reference/android/content/Context.html#bindService(android.content.Intent,%20android.content.ServiceConnection,%20int)
[Messenger]: https://developer.android.com/reference/android/os/Messenger.html
[Handler]: https://developer.android.com/reference/android/os/Handler.html
[ServiceConnection]: https://developer.android.com/reference/android/content/ServiceConnection.html
[unbindService]: https://developer.android.com/reference/android/content/Context.html#unbindService(android.content.ServiceConnection)
[sendBroadcast]: https://developer.android.com/reference/android/content/Context.html#sendBroadcast(android.content.Intent)
