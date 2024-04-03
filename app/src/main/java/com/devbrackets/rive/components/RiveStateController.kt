package com.devbrackets.rive.components

import android.util.Log
import androidx.compose.runtime.Stable
import app.rive.runtime.kotlin.RiveAnimationView
import app.rive.runtime.kotlin.controllers.RiveFileController
import app.rive.runtime.kotlin.core.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Provides a mechanism for controlling a [RiveAnimationView] that is more Compose friendly
 * and more resilient to requesting state changes when the [RiveAnimationView] isn't quite
 * ready for changes.
 */
@Stable
@Suppress("MemberVisibilityCanBePrivate", "unused")
class RiveStateController {
    private val riveListener = RiveListener()
    private val animationView = MutableStateFlow<RiveAnimationView?>(null)

    private var isDetached: Boolean = false
    private val stateChangedListeners = mutableListOf<StateChangedListener>()

    fun attachView(view: RiveAnimationView) {
        val existingView = animationView.value

        if (existingView === view) return
        existingView?.let(::detachView)

        isDetached = false

        animationView.tryEmit(view)
        view.controller.registerListener(riveListener)
        view.controller.addEventListener(riveListener)
    }

    fun detachView() {
        animationView.value?.let(::detachView)
        isDetached = true
        animationView.tryEmit(null)
    }

    private fun detachView(view: RiveAnimationView) {
        view.controller.unregisterListener(riveListener)
        view.controller.removeEventListener(riveListener)
    }

    /**
     * Determines if a state machine has been loaded and is actively playing.
     *
     * Checking for a state machine being loaded is necessary due to Rive saying that
     * it is playing when no state machine has been loaded.
     */
    suspend fun isLoadedAndPlaying() = withAnimationViewOr(false) { isLoadedAndPlaying }

    suspend fun play(stateMachineName: String) = withAnimationViewOr(false) {
        if (!isLoadedAndPlaying) {
            play(animationName = stateMachineName, isStateMachine = true, settleInitialState = false)
        }
    }

    suspend fun fireState(stateMachineName: String, inputName: String): Boolean = withAnimationViewOr(false) {
        setState<SMITrigger>(stateMachineName, inputName) {
            fireState(stateMachineName, inputName)
        }
    }

    suspend fun setState(stateMachineName: String, inputName: String, value: Boolean): Boolean = withAnimationViewOr(false) {
        setState<SMIBoolean>(stateMachineName, inputName) {
            setBooleanState(stateMachineName, inputName, value)
        }
    }

    suspend fun setState(stateMachineName: String, inputName: String, value: Float): Boolean = withAnimationViewOr(false) {
        setState<SMINumber>(stateMachineName, inputName) {
            setNumberState(stateMachineName, inputName, value)
        }
    }

    /**
     * Awaits for the [stateName] in the state machine with [stateMachineName] to be reached.
     *
     * @param stateMachineName The name of the state machine to observe for the [stateName]
     * @param stateName The name of the state to await
     * @param timeoutMillis The maximum amount of time to wait for the [stateName] to be reached
     * @return `true` if the [stateName] was reached
     */
    suspend fun awaitState(stateMachineName: String, stateName: String, timeoutMillis: Long = 200): Boolean {
        val stateReached = AtomicBoolean(false)
        val listener = StateChangedListener { machineName, name ->
            if (!stateReached.get() && stateMachineName == machineName && stateName == name) {
                stateReached.set(true)
            }
        }

        registerStateChangedListener(listener)
        getWithTimeout(timeoutMillis = timeoutMillis) { if (stateReached.get()) stateReached.get() else null }
        unRegisterStateChangedListener(listener)

        return stateReached.get()
    }

    /**
     * Registers a new [StateChangedListener] to be informed of state changes. This should be
     * paired with [unRegisterStateChangedListener].
     *
     * NOTE:
     * We use a callback (listener) mechanism here instead of exposing a `State<>` because we need to
     * monitor all state changes. The Compose runtime will only emit the latest state when a snapshot (frame)
     * boundary is reached, meaning that we can potentially loose rive states.
     *
     * @param listener The [StateChangedListener] to inform of state changes from Rive
     */
    fun registerStateChangedListener(listener: StateChangedListener) {
        stateChangedListeners.add(listener)
    }

    /**
     * Un-Registers a [StateChangedListener] that was previously registered with [registerStateChangedListener].
     *
     * @param listener The [StateChangedListener] to unregister
     */
    fun unRegisterStateChangedListener(listener: StateChangedListener) {
        stateChangedListeners.remove(listener)
    }

    private suspend inline fun <T> withAnimationViewOr(default: T, crossinline action: suspend RiveAnimationView.() -> T): T {
        if (isDetached) {
            Log.w(TAG, "Attempting to run withAnimationViewOr on detached RiveController")
            return default
        }

        animationView.tryNonNull(500.milliseconds)?.let {
            return action(it)
        }

        Log.w(TAG, "withAnimationViewOr timedOut")
        return default
    }

    private suspend inline fun <reified T: SMIInput> RiveAnimationView.setState(stateMachineName: String, inputName: String, onSet: () -> Unit): Boolean {
        val stateMachine = getWithTimeout(timeoutMillis = 200) { stateMachines.find { it.name == stateMachineName } }
        if (stateMachine == null) {
            Log.w(TAG, "Attempting to set state for input \"$inputName\" on non-existent stateMachine \"$stateMachineName\"")
            return false
        }

        if (inputName in stateMachine.inputNames && stateMachine.input(inputName) is T) {
            onSet()
            return true
        }

        return false
    }

    private suspend inline fun <T> getWithTimeout(timeoutMillis: Long = 200, crossinline getter: suspend () -> T?): T? {
        val timeoutEnd = System.currentTimeMillis() + timeoutMillis
        while (System.currentTimeMillis() < timeoutEnd) {
            getter()?.let {
                return it
            }

            // NOTE:
            // It would be better if we could be notified of availability, however Rive doesn't currently inform us of
            // stateMachine additions/removals so we instead use a polling mechanism
            delay(8) // 8.34 ms represents a frame in 120-fps
        }

        return null
    }

    /**
     * Attempts to get the first non-null [T] from the flow within the specified [timeout]. If no
     * emission, or only null emissions, are received then a `null` value will be returned.
     */
    private suspend fun <T> Flow<T?>.tryNonNull(timeout: Duration): T? {
        return filter { it != null }.timeout(timeout).catch { emit(null) }.firstOrNull()
    }

    /**
     * Determines if a state machine has been loaded and is actively playing.
     *
     * Checking for a state machine being loaded is necessary due to Rive saying that
     * it is playing when no state machine has been loaded.
     */
    private val RiveAnimationView.isLoadedAndPlaying: Boolean get() = stateMachines.isNotEmpty() && isPlaying

    fun interface StateChangedListener {
        fun onStateChanged(stateMachineName: String, stateName: String)
    }

    private inner class RiveListener: RiveFileController.Listener, RiveFileController.RiveEventListener  {
        override fun notifyAdvance(elapsed: Float) {}
        override fun notifyLoop(animation: PlayableInstance) {}
        override fun notifyPause(animation: PlayableInstance) {}
        override fun notifyPlay(animation: PlayableInstance) {}

        override fun notifyStateChanged(stateMachineName: String, stateName: String) {
            stateChangedListeners.forEach { it.onStateChanged(stateMachineName, stateName) }
            Log.i(TAG, "notifyStateChanged($stateMachineName, $stateName)")
        }

        override fun notifyStop(animation: PlayableInstance) {}
        override fun notifyEvent(event: RiveEvent) {}
    }

    companion object  {
        private const val TAG = "RiveStateController"
    }
}