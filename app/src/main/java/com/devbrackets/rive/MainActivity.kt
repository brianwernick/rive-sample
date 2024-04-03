package com.devbrackets.rive

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.devbrackets.rive.sample.SimpleExample
import com.devbrackets.rive.ui.theme.RiveSampleTheme

class MainActivity: ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RiveSampleTheme {
                SimpleExample()
            }
        }
    }
}