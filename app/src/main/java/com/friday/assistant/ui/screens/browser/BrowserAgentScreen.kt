package com.friday.assistant.ui.screens.browser

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.friday.assistant.domain.browser.FridayBrowserAgent
import com.friday.assistant.ui.components.GlassCard
import com.friday.assistant.ui.theme.FridayBgDark
import com.friday.assistant.ui.theme.FridayCyan
import com.friday.assistant.ui.theme.TextPrimary
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserAgentScreen(
    browserAgent: FridayBrowserAgent,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var summarizeUrl by remember { mutableStateOf("") }
    var summaryResult by remember { mutableStateOf("") }
    var isSummarizing by remember { mutableStateOf(false) }
    val cachedArticles by browserAgent.cachedArticles.collectAsState()
    val scope = rememberCoroutineScope()

    Scaffold(
        containerColor = FridayBgDark,
        topBar = {
            TopAppBar(
                title = { Text("Browser Research Agent", color = TextPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = FridayCyan)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = FridayBgDark)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .background(FridayBgDark)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "Autonomous web search, secure URL dispatch, and offline article extraction.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF78909C)
                )
            }

            // Search Section
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Live Web Research", style = MaterialTheme.typography.titleSmall, color = Color.White)
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("e.g. quantum computing news", color = Color(0xFF546E7A)) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF00E5FF),
                                unfocusedBorderColor = Color(0xFF1E3A5F),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            singleLine = true
                        )
                        Button(
                            onClick = {
                                if (searchQuery.isNotBlank()) {
                                    browserAgent.openSearch(searchQuery)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Search, contentDescription = null, tint = Color.Black)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Search in Chrome", color = Color.Black)
                        }
                    }
                }
            }

            // Web Extraction & Summarization
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Article Extractor & Summarizer", style = MaterialTheme.typography.titleSmall, color = Color.White)
                        OutlinedTextField(
                            value = summarizeUrl,
                            onValueChange = { summarizeUrl = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("https://en.wikipedia.org/wiki/Artificial_intelligence", color = Color(0xFF546E7A)) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF00E5FF),
                                unfocusedBorderColor = Color(0xFF1E3A5F),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            singleLine = true
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    if (summarizeUrl.isNotBlank()) {
                                        isSummarizing = true
                                        scope.launch {
                                            summaryResult = browserAgent.fetchAndSummarize(summarizeUrl)
                                            isSummarizing = false
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00B0FF)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Extract & Summarize", color = Color.Black)
                            }
                            IconButton(
                                onClick = { if (summarizeUrl.isNotBlank()) browserAgent.openUrl(summarizeUrl) }
                            ) {
                                Icon(Icons.Default.OpenInBrowser, contentDescription = "Open", tint = Color(0xFF00E5FF))
                            }
                        }

                        if (isSummarizing) {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = Color(0xFF00E5FF))
                        }

                        if (summaryResult.isNotBlank()) {
                            Text(
                                text = summaryResult,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFE0E0E0),
                                modifier = Modifier
                                    .background(Color(0xFF0F1E36))
                                    .padding(12.dp)
                            )
                        }
                    }
                }
            }

            // Cached Offline Articles
            item {
                Text(
                    text = "SAVED OFFLINE ARTICLES (${cachedArticles.size})",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFF00E5FF)
                )
            }

            if (cachedArticles.isEmpty()) {
                item {
                    Text(
                        text = "No offline articles cached yet. Summarize any web article to save it locally.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF546E7A)
                    )
                }
            } else {
                items(cachedArticles) { article ->
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(article.title, style = MaterialTheme.typography.titleSmall, color = Color(0xFF00E5FF))
                            Text(
                                SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(article.timestamp)),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF78909C)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(article.summary, style = MaterialTheme.typography.bodySmall, color = Color(0xFFECEFF1))
                        }
                    }
                }
            }
        }
    }
}
