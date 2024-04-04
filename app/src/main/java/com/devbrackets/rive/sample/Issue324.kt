package com.devbrackets.rive.sample

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.devbrackets.rive.components.ExampleAnimation
import com.devbrackets.rive.components.ExampleAnimationType
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.random.Random

/**
 * Use for [Issue 324](https://github.com/rive-app/rive-android/issues/324)
 *
 * Represents an [AnimatedContent] that contains `RiveAnimations` on each screen
 * which is a simplified reproduction of the application code that is triggering
 * seg fault crashes
 */
@Composable
fun Issue324() {
    // NOTE: In our app we only have 2 screens that use the same Rive asset, but for
    // reproduction (and simplicity) we just allow it to increment
    val screen = remember { mutableIntStateOf(0) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
    ) {
        AnimatedContent(
            targetState = screen.intValue,
            modifier = Modifier.fillMaxSize(),
            label = "Issue324Content"
        ) { _ ->
            IssueScreen(
                modifier = Modifier.fillMaxSize()
            )
        }

        Button(
            onClick = {
                screen.intValue++
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Text(text = "Next Screen")
        }
    }
}

@Composable
private fun IssueScreen(
    modifier: Modifier = Modifier
) {
    val type = remember { mutableStateOf<ExampleAnimationType>(ExampleAnimationType.Beginner) }

    Box(
        modifier = modifier
    ) {
        ExampleAnimation(
            type = type,
            modifier = Modifier
                .fillMaxSize()
                .align(Alignment.Center)
        )
    }
}

/**
 * Our engineering team was able to encounter another crash which gave us a better
 * picture on timing of when it occurs. What we previously believed was that this occurred
 * during the transition from one screen to another and the tombstone gave me a false sense
 * that this was accurate. However this is a bit incorrect, the crash does occur during the
 * transition from one screen to another however it seems that this happens during the first
 * screen which performs a size animation on the `RiveAnimationView` which seems to be triggering
 * repeated construction of new surfaces (my assumption).
 *
 * This Version replicates that animation, though with multiple instances to hopefully speed
 * up reproduction
 */
@Composable
@OptIn(ExperimentalLayoutApi::class)
fun Issue324V2() {
    val type = remember { mutableStateOf<ExampleAnimationType>(ExampleAnimationType.Beginner) }

    val targetState = remember { mutableFloatStateOf(1f) }
    val exampleCount = remember { mutableIntStateOf(0) }

    val transition = updateTransition(targetState = targetState.floatValue, label = "Size Transition")
    val animationSize = transition.animateValue(
        typeConverter = DpSize.VectorConverter,
        transitionSpec = {
            tween(durationMillis = 250)
        },
        label = "Animation Size",
        targetValueByState = { state ->
            DpSize(width = 135.dp, height = 237.dp) * state
        }
    )

    FlowRow(
        modifier = Modifier.fillMaxSize()
    ) {
        repeat(exampleCount.intValue) {
            ExampleAnimation(
                type = type,
                modifier = Modifier.size(animationSize.value)
            )
        }
    }

    RandomizeSizeAndCount(sizeMultiplier = targetState, examples = exampleCount)
}

/**
 * Randomly updates the number of [examples] (1 to 20) and the [sizeMultiplier] (.8 to 1)
 * to push Compose and Rive to the breaking point (native crashes)
 */
@Composable
private fun RandomizeSizeAndCount(
    sizeMultiplier: MutableFloatState,
    examples: MutableIntState
) {
    val sizeMultiplierIndex = remember { mutableIntStateOf(0) }

    LaunchedEffect(true) {
        while (isActive) {
            sizeMultiplierIndex.intValue = (sizeMultiplierIndex.intValue + 1) % 3
            sizeMultiplier.floatValue = when (sizeMultiplierIndex.intValue) {
                0 -> 1f
                1 -> 0.9f
                else -> 0.8f
            }
            delay(500)
        }
    }

    LaunchedEffect(true) {
        while (isActive) {
            examples.intValue = Random.nextInt(19) + 1
            delay(900)
        }
    }
}

private val DpSize.Companion.VectorConverter: TwoWayConverter<DpSize, AnimationVector2D>
    get() = DpSizeToVector

private val DpSizeToVector: TwoWayConverter<DpSize, AnimationVector2D> =
    TwoWayConverter(
        convertToVector = { AnimationVector2D(it.width.value, it.height.value) },
        convertFromVector = { DpSize(Dp(it.v1), Dp(it.v2)) }
    )