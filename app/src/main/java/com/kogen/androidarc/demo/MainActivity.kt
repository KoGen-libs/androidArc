package com.kogen.androidarc.demo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.navigation.compose.rememberNavController
import com.kogen.androidarc.demo.navigation.AppNavHost

/**
 * Demo host for androidArc's Notes screens: two features (list, details), both built the same
 * way real app screens would be - `BaseMviViewModel` + `ScreenContainerWrapper` from the
 * `androidArc` library, wired through KoGen DI and KoGen Navigation code-gen.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                val navController = rememberNavController()
                AppNavHost(navController = navController)
            }
        }
    }
}
