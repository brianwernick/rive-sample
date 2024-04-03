package com.devbrackets.rive.components

import androidx.annotation.RawRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import app.rive.runtime.kotlin.RiveAnimationView
import app.rive.runtime.kotlin.core.Alignment
import app.rive.runtime.kotlin.core.Fit
import app.rive.runtime.kotlin.core.Loop

/**
 * Displays [Rive](https://rive.app/) animations
 *
 * NOTE:
 * Rive doesn't natively support Compose and is tracking the addition in https://github.com/rive-app/rive-android/issues/259
 */
@Composable
fun RiveAnimation(
    animation: RiveAnimationData,
    modifier: Modifier = Modifier,
    stateController: RiveStateController? = null,
) {
    // Rive does not work in Previews so we will draw a grey box instead for reference
    if (LocalInspectionMode.current) {
        Box(modifier = modifier.background(Color(0xFF555555), shape = RoundedCornerShape(8.dp)))
        return
    }

    AndroidView(
        factory = { context ->
            RiveAnimationView(context)
        },
        modifier = modifier,
        onRelease = { stateController?.detachView() }
    ) { animationView ->
        animationView.setAnimation(animation)
        stateController?.attachView(animationView)
    }
}

/**
 * Uses the [RiveAnimationData] [animation] to set the appropriate properties in the [RiveAnimationView]
 *
 * @param animation The [RiveAnimationData] to apply to the [RiveAnimationView]
 */
private fun RiveAnimationView.setAnimation(animation: RiveAnimationData) {
    when (animation) {
        is RiveAnimationData.Resource -> {
            setRiveResource(
                resId = animation.resId,
                artboardName = animation.artboardName,
                animationName = animation.animationName,
                stateMachineName = animation.stateMachineName,
                autoplay = animation.autoplay,
                fit = animation.fit,
                alignment = animation.alignment,
                loop = animation.loop,
            )
        }
    }
}

/**
 * Use to set the initial Rive animation properties. Currently we only set this by Resource id; add more as needed.
 *
 * Make sure all implementations are data classes so Compose can avoid unnecessary recompositions.
 */
@Immutable
sealed interface RiveAnimationData {
    val artboardName: String?
    val animationName: String?
    val stateMachineName: String?
    val autoplay: Boolean
    val fit: Fit
    val alignment: Alignment
    val loop: Loop

    @Immutable
    data class Resource(
        @RawRes val resId: Int,
        override val artboardName: String? = null,
        override val animationName: String? = null,
        override val stateMachineName: String? = null,
        override val autoplay: Boolean = true,
        override val fit: Fit = Fit.CONTAIN,
        override val alignment: Alignment = Alignment.CENTER,
        override val loop: Loop = Loop.AUTO,
    ) : RiveAnimationData
}