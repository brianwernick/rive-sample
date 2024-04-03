package com.devbrackets.rive.sample

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.devbrackets.rive.components.ExampleAnimation
import com.devbrackets.rive.components.ExampleAnimationType

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
