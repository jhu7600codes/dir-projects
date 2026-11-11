package com.vanbank.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.vanbank.app.ui.navigation.VanBankNavHost
import com.vanbank.app.ui.theme.VanBankTheme
import com.vanbank.app.ui.theme.VbBackground

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val container = (application as VanBankApplication).container

        setContent {
            VanBankTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = VbBackground) {
                    VanBankNavHost(container)
                }
            }
        }
    }
}
