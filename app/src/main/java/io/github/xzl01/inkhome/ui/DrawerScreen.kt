package io.github.xzl01.inkhome.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.xzl01.inkhome.data.AppEntry
import io.github.xzl01.inkhome.data.AppRepository
import io.github.xzl01.inkhome.ui.theme.InkBlack
import io.github.xzl01.inkhome.ui.theme.InkGray
import io.github.xzl01.inkhome.ui.theme.InkLine
import io.github.xzl01.inkhome.ui.theme.InkWhite
import kotlinx.coroutines.launch

private fun letterOf(label: String): String {
    val c = label.trim().firstOrNull() ?: return "#"
    val upper = c.uppercaseChar()
    return if (upper in 'A'..'Z') upper.toString() else "#"
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DrawerScreen(
    apps: List<AppEntry>,
    repo: AppRepository,
    colorIcons: Boolean,
    onBack: () -> Unit,
    onLaunch: (AppEntry) -> Unit,
    onLongPress: (AppEntry) -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }
    val filtered = remember(apps, query) {
        val q = query.trim()
        if (q.isEmpty()) apps else apps.filter {
            it.label.contains(q, ignoreCase = true) || it.packageName.contains(q, ignoreCase = true)
        }
    }
    val sorted = remember(filtered) {
        filtered.sortedWith(compareBy({ letterOf(it.label) }, { it.label.lowercase() }))
    }
    val letterStarts = remember(sorted) {
        val map = LinkedHashMap<String, Int>()
        sorted.forEachIndexed { index, app ->
            map.putIfAbsent(letterOf(app.label), index)
        }
        map
    }
    val letters = remember(letterStarts) {
        letterStarts.keys.sortedWith(compareBy({ it == "#" }, { it }))
    }
    val gridState: LazyGridState = rememberLazyGridState()
    val scope = rememberCoroutineScope()

    Box(Modifier.fillMaxSize().background(InkWhite)) {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "←",
                    color = InkBlack,
                    fontSize = 30.sp,
                    modifier = Modifier
                        .clickable(onClick = onBack)
                        .padding(end = 16.dp)
                )
                Text(
                    text = "应用",
                    color = InkBlack,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.weight(1f)
                )
                Text(text = "${filtered.size}", color = InkGray, fontSize = 14.sp)
            }
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 10.dp),
                singleLine = true,
                placeholder = { Text("搜索应用", color = InkGray, fontSize = 15.sp) },
                textStyle = TextStyle(color = InkBlack, fontSize = 16.sp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
            )
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                state = gridState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 12.dp, top = 8.dp, end = 40.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(sorted, key = { it.key }) { app ->
                    Column(
                        modifier = Modifier
                            .border(1.dp, InkLine)
                            .combinedClickable(
                                onClick = { onLaunch(app) },
                                onLongClick = { onLongPress(app) }
                            )
                            .padding(vertical = 12.dp, horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        AppIcon(entry = app, repo = repo, size = 48.dp, color = colorIcons)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = app.label,
                            color = InkBlack,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        if (query.isBlank() && letters.size > 1) {
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 4.dp),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                letters.forEach { letter ->
                    Text(
                        text = letter,
                        color = InkGray,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable {
                                val target = letterStarts[letter] ?: 0
                                scope.launch { gridState.scrollToItem(target) }
                            }
                            .padding(horizontal = 6.dp, vertical = 1.dp)
                    )
                }
            }
        }
    }
}
