package org.cemetery.app
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import org.cemetery.app.core.CemeteryApp
import org.cemetery.app.core.LocalHostActivity
import dagger.hilt.android.AndroidEntryPoint
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { CompositionLocalProvider(LocalHostActivity provides this, androidx.activity.compose.LocalActivityResultRegistryOwner provides this, androidx.activity.compose.LocalOnBackPressedDispatcherOwner provides this) { CemeteryApp() } }
    }
}
