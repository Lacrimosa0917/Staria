package com.lacrimosa.staria

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.lacrimosa.staria.ui.StariaApp
import com.lacrimosa.staria.ui.theme.StariaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            StariaTheme {
                StariaApp()
            }
        }
    }
}
