package link.danb.launcher.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp

@Composable
fun TabButtonGroup(iconButtons: @Composable () -> Unit) {
    Card(Modifier.padding(horizontal = 4.dp), RoundedCornerShape(28.dp)) {
        Row(modifier = Modifier.padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
            iconButtons()
        }
    }
}

@Composable
fun TabButton(icon: Painter, name: String, isChecked: Boolean, onClick: () -> Unit) {
    FilledIconToggleButton(checked = isChecked, onCheckedChange = { _ -> onClick() }) {
        Icon(painter = icon, contentDescription = name)
    }
}
