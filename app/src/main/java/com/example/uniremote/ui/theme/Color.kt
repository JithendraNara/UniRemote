
package com.example.uniremote.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import android.os.Build
import androidx.compose.ui.platform.LocalContext

val LightColors = androidx.compose.material3.lightColorScheme(
	primary = Color(0xFF6750A4),
	onPrimary = Color.White,
	primaryContainer = Color(0xFFEADDFF),
	onPrimaryContainer = Color(0xFF21005D),
	secondary = Color(0xFF625B71),
	onSecondary = Color.White,
	secondaryContainer = Color(0xFFE8DEF8),
	onSecondaryContainer = Color(0xFF1D192B),
	background = Color(0xFFFFFBFE),
	onBackground = Color(0xFF1C1B1F),
	surface = Color(0xFFFFFBFE),
	onSurface = Color(0xFF1C1B1F),
	surfaceVariant = Color(0xFFE7E0EC),
	onSurfaceVariant = Color(0xFF49454F),
	outline = Color(0xFF79747E)
)

val DarkColors = androidx.compose.material3.darkColorScheme(
	primary = Color(0xFFD0BCFF),
	onPrimary = Color(0xFF381E72),
	primaryContainer = Color(0xFF4F378B),
	onPrimaryContainer = Color(0xFFEADDFF),
	secondary = Color(0xFFCCC2DC),
	onSecondary = Color(0xFF332D41),
	secondaryContainer = Color(0xFF4A4458),
	onSecondaryContainer = Color(0xFFE8DEF8),
	background = Color(0xFF1C1B1F),
	onBackground = Color(0xFFE6E1E5),
	surface = Color(0xFF1C1B1F),
	onSurface = Color(0xFFE6E1E5),
	surfaceVariant = Color(0xFF49454F),
	onSurfaceVariant = Color(0xFFCAC4D0),
	outline = Color(0xFF938F99)
)

@Composable
fun uniRemoteColorScheme(darkTheme: Boolean): ColorScheme {
	val context = LocalContext.current
	return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
		if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
	} else {
		if (darkTheme) DarkColors else LightColors
	}
}
