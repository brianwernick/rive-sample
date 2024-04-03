package com.devbrackets.rive.components

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.devbrackets.rive.R

/**
 * Serves as an example on how to integrate the [RiveStateController] with the
 * [RiveAnimation].
 *
 * [ExampleAnimationType] represents the visual presentation you want, in this example
 * this is a 1:1 mapping of the animation `stateName` however in more complex animations
 * this may represent a combination of inputs.
 *
 * [ExampleAnimationState] represents the resulting state of the animation, in this example
 * this is also a 1:1 mapping with the [ExampleAnimationType] however there may be cases
 * where you want to emit intermediate states beyond just the starting/ending [ExampleAnimationType]
 */
@Composable
internal fun ExampleAnimation(
    type: State<ExampleAnimationType>,
    modifier: Modifier = Modifier,
    onStateChanged: (ExampleAnimationState) -> Unit = {},
) {
    val stateController = remember { RiveStateController() }

    RiveAnimation(
        animation = remember {
            RiveAnimationData.Resource(
                // NOTE: this resource was pulled from the rive-android sample app on April 3, 2024
                // All ownership and rights belong to Rive
                // https://github.com/rive-app/rive-android/blob/master/app/src/main/res/raw/skills.riv
                resId = R.raw.skills,
                artboardName = ART_BOARD,
                stateMachineName = STATE_MACHINE,
                autoplay = false // applyState handles playing
            )
        },
        modifier = modifier,
        stateController = stateController
    )

   SyncState(
       type = type,
       controller = stateController,
       onStateChanged = onStateChanged
   )
}

/**
 * Ensures that incoming [type] changes are applied to the [RiveAnimation] via the
 * [controller] and any state changes in the [RiveAnimation] itself notifies [onStateChanged]
 */
@Composable
private fun SyncState(
    type: State<ExampleAnimationType>,
    controller: RiveStateController,
    onStateChanged: (ExampleAnimationState) -> Unit,
) {
    // Needed otherwise the DisposableEffect would only ever reference the first onStateChanged
    val stateCallback = rememberUpdatedState(onStateChanged)
    val previousState = remember { mutableStateOf(ExampleAnimationState.UNKNOWN) }

    LaunchedEffect(type.value, controller) {
        controller.applyState(type.value)
    }

    // Informs the onStateChanged of state changes
    DisposableEffect(controller) {
        val stateChangedListener = RiveStateController.StateChangedListener { _, stateName ->
            val state = ExampleAnimationState.entries.firstOrNull { it.stateName == stateName } ?: ExampleAnimationState.UNKNOWN
            if (state != ExampleAnimationState.UNKNOWN && state != previousState.value) {
                stateCallback.value(state)
                previousState.value = state
            }
        }

        controller.registerStateChangedListener(stateChangedListener)

        onDispose {
            controller.unRegisterStateChangedListener(stateChangedListener)
        }
    }
}

/**
 * Applies the [state] to the [RiveStateController]
 */
private suspend fun RiveStateController.applyState(state: ExampleAnimationType) {
    // Wait until the state machines are loaded
    isLoadedAndPlaying()

    // We need to call play before we can set the state otherwise Rive just plays the default state
    play(STATE_MACHINE)

    when (state) {
        is ExampleAnimationType.Beginner -> {
            setState(STATE_MACHINE, AnimationInput.Level.name, 0f)
        }
        is ExampleAnimationType.Intermediate -> {
            setState(STATE_MACHINE, AnimationInput.Level.name, 1f)
        }
        is ExampleAnimationType.Advanced -> {
            setState(STATE_MACHINE, AnimationInput.Level.name, 2f)
        }
    }
}

private const val ART_BOARD = "New Artboard"
private const val STATE_MACHINE = "Designer's Test"

@Immutable
enum class ExampleAnimationState(val stateName: String) {
    Beginner("Beginner"),
    Intermediate("Intermediate"),
    Advanced("Advanced"),
    UNKNOWN("")
}

@Immutable
sealed interface ExampleAnimationType {
    @Immutable
    data object Beginner: ExampleAnimationType

    @Immutable
    data object Intermediate: ExampleAnimationType

    @Immutable
    data object Advanced: ExampleAnimationType
}

@JvmInline
@Immutable
private value class AnimationInput private constructor(val name: String) {
    companion object {
        val Level = AnimationInput("Level")
    }
}