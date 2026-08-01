package com.borgorninja.androidguardian.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.borgorninja.androidguardian.ui.theme.ConsoleBackground
import com.borgorninja.androidguardian.ui.theme.ConsoleMuted
import com.borgorninja.androidguardian.ui.theme.ConsoleStderr
import com.borgorninja.androidguardian.ui.theme.ConsoleStdout
import com.borgorninja.androidguardian.ui.theme.ConsoleTextStyle
import com.borgorninja.androidguardian.ui.viewmodel.ConsoleEntry

/**
 * Auto-scrolling terminal view. Auto-scroll only fires when the user is already
 * near the bottom, so scrolling up to inspect earlier output isn't fought by
 * incoming lines during a long compile.
 */
@Composable
fun ConsoleLog(
    entries: List<ConsoleEntry>,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    LaunchedEffect(entries.size) {
        if (entries.isNotEmpty()) {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val isNearBottom = lastVisible >= entries.size - 3
            if (isNearBottom) listState.animateScrollToItem(entries.size - 1)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ConsoleBackground)
            .padding(12.dp)
    ) {
        if (entries.isEmpty()) {
            Text(
                text = "Console idle. Select an app and tap \"Optimize\" to begin.",
                style = ConsoleTextStyle,
                color = ConsoleMuted
            )
        } else {
            LazyColumn(state = listState) {
                items(entries) { entry ->
                    Text(
                        text = entry.text,
                        style = ConsoleTextStyle,
                        color = if (entry.isError) ConsoleStderr else ConsoleStdout
                    )
                }
            }
        }
    }
}
