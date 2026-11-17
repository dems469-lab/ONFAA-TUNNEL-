package com.onfaa.tunnel.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.onfaa.tunnel.ui.theme.OnfaaTheme

data class OnfaaNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

@Composable
fun OnfaaBottomBar(
    items: List<OnfaaNavItem>,
    selectedRoute: String?,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = OnfaaTheme.colors
    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            palette.border.copy(alpha = 0f),
                            palette.primary.copy(alpha = 0.35f),
                            palette.border.copy(alpha = 0f)
                        )
                    )
                )
        )
        NavigationBar(
            containerColor = palette.navBackground,
            tonalElevation = 0.dp
        ) {
            items.forEach { item ->
                val selected = selectedRoute == item.route
                NavigationBarItem(
                    selected = selected,
                    onClick = { onNavigate(item.route) },
                    icon = {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.label,
                            modifier = Modifier.height(22.dp)
                        )
                    },
                    label = {
                        Text(
                            text = item.label,
                            style = MaterialTheme.typography.labelSmall
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = palette.primary,
                        selectedTextColor = palette.primary,
                        indicatorColor = palette.navSelected,
                        unselectedIconColor = palette.textMuted,
                        unselectedTextColor = palette.textMuted
                    )
                )
            }
        }
    }
}
