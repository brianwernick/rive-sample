package com.devbrackets.rive.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.devbrackets.rive.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * NOTE: currently the [ExampleAnimationType] and [ExampleAnimationState] don't
 * represent the current resource and won't work. This serves primarily as an
 * example of how it can be setup and will be updated at a future date with
 * a rive asset that matches.
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
                // NOTE: this resource was pulled from the rive-android sample app
                resId = R.raw.trailblaze,
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
 * Applies the [state] to the [RiveStateController] by setting the appropriate
 * inputs then firing the `"Continue"` input
 */
private suspend fun RiveStateController.applyState(state: ExampleAnimationType) {
    // Wait until the state machines are loaded
    isLoadedAndPlaying()

    // We need to call play before we can set the state otherwise Rive just plays the default state (Idle in this case)
    play(STATE_MACHINE)

    when (state) {
        is ExampleAnimationType.Idle -> {
            setState(STATE_MACHINE, AnimationInput.StartIdle.name, false)
            fireState(STATE_MACHINE, AnimationInput.Continue.name)
        }
        ExampleAnimationType.Streak -> {
            setState(STATE_MACHINE, AnimationInput.AddToStreak.name, true)
            setState(STATE_MACHINE, AnimationInput.StreakMilestone.name, false)
            fireState(STATE_MACHINE, AnimationInput.Continue.name)
        }
    }
}

private const val ART_BOARD = "Lesson Endstate"
private const val STATE_MACHINE = "endstate_complete"

@Immutable
enum class ExampleAnimationState(val stateName: String) {
    IDLE("Idle"),
    TRANSITION_TO_STREAK("NonMile_Transition to Streak"),
    SHOCKING("Shock"),
    IDLE_STREAK("Streak Idle"),
    UNKNOWN("")
}

@Immutable
sealed interface ExampleAnimationType {
    @Immutable
    data object Idle: ExampleAnimationType

    @Immutable
    data object Streak: ExampleAnimationType
}

@JvmInline
@Immutable
private value class AnimationInput private constructor(val name: String) {
    companion object {
        val StartIdle = AnimationInput("Start Idle")
        val Continue = AnimationInput("Continue")
        val AddToStreak = AnimationInput("Add to Streak")
        val StreakMilestone = AnimationInput("Streak Milestone")
    }
}

@Preview
@Composable
private fun PreviewAnimation() {
    val screen = remember { mutableIntStateOf(0) }

    AnimatedContent(
        targetState = screen.intValue,
        modifier = Modifier.fillMaxSize(),
        transitionSpec = {
            fadeIn(spring()) togetherWith fadeOut(spring())
        },
        label = "PreviewScreens"
    ) { targetState ->
        PreviewScreen(
            transition = targetState > 0
        )
    }

    LaunchedEffect("screen") {
        launch {
            delay(2_000)
            screen.intValue += 1
        }
    }
}

@Composable
private fun PreviewScreen(
    modifier: Modifier = Modifier,
    transition: Boolean = false,
) {
    val type = remember { mutableStateOf<ExampleAnimationType>(ExampleAnimationType.Idle) }

    Box(
        modifier = modifier
    ) {
        ExampleAnimation(
            type = type,
            modifier = Modifier
                .size(width = 135.dp, height = 237.dp)
                .align(Alignment.Center)
        )
    }

    LaunchedEffect("preview-screen", transition) {
        if (transition) {
            launch {
                delay(1_500)
                type.value = ExampleAnimationType.Streak
            }
        }
    }
}
