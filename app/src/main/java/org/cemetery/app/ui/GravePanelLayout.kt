package org.cemetery.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity

/** Leave reading space above the fixed Close footer on short windows with large text. */
internal fun gravePanelFraction(heightDp:Int,fontScale:Float):Float =
    if(heightDp<480 && fontScale>=1.5f).60f else .44f

@Composable internal fun gravePanelFraction():Float =
    gravePanelFraction(LocalConfiguration.current.screenHeightDp,LocalDensity.current.fontScale)
