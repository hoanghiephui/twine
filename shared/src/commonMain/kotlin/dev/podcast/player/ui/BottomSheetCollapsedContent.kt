/*
 * Copyright 2025 CoinDex
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.podcast.player.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.unit.dp
import dev.sasikanth.rss.reader.feeds.ui.HomeBottomBarItem
import dev.sasikanth.rss.reader.resources.strings.LocalStrings
import dev.sasikanth.rss.reader.ui.AppTheme

@Composable
fun BottomSheetCollapsedContent(modifier: Modifier = Modifier) {
    val shadowColors =
        arrayOf(
            0.85f to AppTheme.colorScheme.tintedBackground,
            0.9f to AppTheme.colorScheme.tintedBackground.copy(alpha = 0.4f),
            1f to Color.Transparent
        )

    val allFeedsLabel = LocalStrings.current.allFeeds

    HomeBottomBarItem(
        selected = false,
        onClick = {},
        modifier =
            Modifier.clearAndSetSemantics {
                contentDescription = allFeedsLabel
                role = Role.Button
            }
                .drawWithCache {
                    onDrawBehind {
                        val brush =
                            Brush.horizontalGradient(
                                colorStops = shadowColors,
                            )
                        drawRect(
                            brush = brush,
                        )
                    }
                }
                .padding(end = 4.dp)
    )
}
