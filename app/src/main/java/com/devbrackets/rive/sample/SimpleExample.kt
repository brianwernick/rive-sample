package com.devbrackets.rive.sample

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.devbrackets.rive.components.ExampleAnimation
import com.devbrackets.rive.components.ExampleAnimationType

@Composable
fun SimpleExample() {
    val animationType = remember { mutableStateOf<ExampleAnimationType>(ExampleAnimationType.Beginner) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding(),
    ) {
        ExampleAnimation(
            type = animationType,
            modifier = Modifier
                .fillMaxSize()
                .align(Alignment.Center)
        )

        Button(
            onClick = {
                animationType.value = animationType.value.next()
            },
            modifier = Modifier
                .padding(16.dp)
                .align(Alignment.BottomEnd)
        ) {
            Text(text = "Next Type")
        }
    }
}

/**
 * Cycle to the "next" type
 */
private fun ExampleAnimationType.next(): ExampleAnimationType {
    return when (this) {
        is ExampleAnimationType.Beginner -> ExampleAnimationType.Intermediate
        is ExampleAnimationType.Intermediate -> ExampleAnimationType.Advanced
        is ExampleAnimationType.Advanced -> ExampleAnimationType.Beginner
    }
}