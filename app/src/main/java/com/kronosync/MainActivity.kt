package com.kronosync

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import dagger.hilt.android.AndroidEntryPoint
import com.kronosync.ui.theme.KronoSyncTheme
import com.kronosync.ui.navigation.KronoNavHost

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            KronoSyncTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    KronoNavHost()
                }
            }
        }
    }
}

@Preview
@Composable
fun PreviewApp() {
    KronoSyncTheme {
        Text("KronoSync")
    }
}
