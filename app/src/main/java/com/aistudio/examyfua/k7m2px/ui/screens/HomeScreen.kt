package com.aistudio.examyfua.k7m2px.ui.screens

import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddTask
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aistudio.examyfua.k7m2px.data.local.AnnouncementCacheEntity
import com.aistudio.examyfua.k7m2px.data.local.FlashcardEntity
import com.aistudio.examyfua.k7m2px.data.local.PomodoroSessionEntity
import com.aistudio.examyfua.k7m2px.data.local.TaskEntity
import com.aistudio.examyfua.k7m2px.domain.model.AnnouncementPriority
import com.aistudio.examyfua.k7m2px.domain.model.StudentProfile
import com.aistudio.examyfua.k7m2px.domain.model.SubjectStudyStat
import com.aistudio.examyfua.k7m2px.ui.components.EmptyStateCard
import com.aistudio.examyfua.k7m2px.ui.components.StudentAvatar
import com.aistudio.examyfua.k7m2px.ui.theme.YfuaTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    profile: StudentProfile?,
    tasks: List<TaskEntity>,
    flashcards: List<FlashcardEntity>,
    pomodoroSessions: List<PomodoroSessionEntity>,
    subjectStats: List<SubjectStudyStat>,
    announcements: List<AnnouncementCacheEntity>,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onNavigateToPlanning: () -> Unit,
    onNavigateToAi: () -> Unit,
    onShareSummary: suspend () -> String
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val pendingTasksToday by remember(tasks) {
        derivedStateOf {
            val endOfToday = System.currentTimeMillis() + 86_400_000L
            tasks.count { !it.isCompleted && it.dueDateEpochMs <= endOfToday }
        }
    }
    val dueFlashcards by remember(flashcards) {
        derivedStateOf {
            val now = System.currentTimeMillis()
            flashcards.count { it.nextReviewAtEpochMs <= now }
        }
    }
    val todayFocusMinutes by remember(pomodoroSessions) {
        derivedStateOf {
            val since = System.currentTimeMillis() - 86_400_000L
            pomodoroSessions.filter { it.completedAtEpochMs >= since }.sumOf { it.focusMinutes }
        }
    }
    val relevantAnnouncements by remember(announcements, profile) {
        derivedStateOf {
            announcements.filter { ann ->
                !ann.archived &&
                    ann.pinned &&
                    (ann.targetInstitution == "ALL" || profile?.institution.equals(ann.targetInstitution, ignoreCase = true)) &&
                    (ann.targetLevel == "ALL" || ann.targetLevel == profile?.level?.displayName) &&
                    (ann.targetSpecialty == "ALL" || ann.targetSpecialty == profile?.specialty?.displayName)
            }
        }
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item(key = "home_identity") {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, YfuaTheme.colors.cardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StudentAvatar(
                            initials = profile?.initials ?: "YF",
                            size = 56.dp,
                            photoPath = profile?.photoUri?.takeIf { it.isNotBlank() }
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (profile?.fullName.isNullOrBlank()) "Bienvenue sur YFUA" else "Bonjour, ${profile!!.fullName}",
                                style = MaterialTheme.typography.titleLarge,
                                color = YfuaTheme.colors.headline,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = profile?.specialty?.displayName ?: "Filière non configurée",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = listOfNotNull(
                                    profile?.level?.displayName,
                                    profile?.institution?.takeIf { it.isNotBlank() }
                                ).joinToString(" • "),
                                style = MaterialTheme.typography.bodySmall,
                                color = YfuaTheme.colors.bodySlate,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(
                            onClick = {
                                scope.launch {
                                    val text = onShareSummary()
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, text)
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Partager mon bilan YFUA"))
                                }
                            },
                            modifier = Modifier.testTag("home_share_summary_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Share,
                                contentDescription = "Partager le bilan d'étude",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            item(key = "home_xp_streak") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.25f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("EXPÉRIENCE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
                            Text(
                                text = "${profile?.xpPoints ?: 0} XP",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Surface(
                        color = YfuaTheme.colors.successContainer,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, YfuaTheme.colors.success.copy(alpha = 0.3f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.LocalFireDepartment,
                                contentDescription = "Série d'étude",
                                tint = YfuaTheme.colors.success,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("SÉRIE ACTIVE", style = MaterialTheme.typography.labelSmall, color = YfuaTheme.colors.success)
                                Text(
                                    text = "${profile?.streakDays ?: 0} jour(s)",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = YfuaTheme.colors.onSuccessContainer,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            item(key = "home_kpis") {
                Text("Résumé du jour", style = MaterialTheme.typography.titleLarge, color = YfuaTheme.colors.headline)
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    DailyMetricTile("À faire", "$pendingTasksToday", MaterialTheme.colorScheme.secondary, Modifier.weight(1f))
                    DailyMetricTile("Focus (24h)", "${todayFocusMinutes}m", YfuaTheme.colors.success, Modifier.weight(1f))
                    DailyMetricTile("Flashcards", "$dueFlashcards", MaterialTheme.colorScheme.tertiary, Modifier.weight(1f))
                }
            }

            item(key = "home_quick_actions") {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = onNavigateToAi,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f).height(48.dp).testTag("home_quick_ai_button")
                    ) {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Assistant IA", fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = onNavigateToPlanning,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f).height(48.dp).testTag("home_quick_planning_button")
                    ) {
                        Icon(Icons.Filled.AddTask, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Planning", color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            item(key = "home_announcements_header") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Campaign, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Communiqués officiels", style = MaterialTheme.typography.titleLarge, color = YfuaTheme.colors.headline)
                }
            }

            if (relevantAnnouncements.isEmpty()) {
                item(key = "home_announcements_empty") {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, YfuaTheme.colors.cardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Aucun communiqué actif pour votre profil.",
                            style = MaterialTheme.typography.bodySmall,
                            color = YfuaTheme.colors.bodySlate,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
            } else {
                items(relevantAnnouncements, key = { "ann_${it.id}" }) { announcement ->
                    val prio = AnnouncementPriority.safeValueOf(announcement.priority)
                    val container = when (prio) {
                        AnnouncementPriority.URGENT -> MaterialTheme.colorScheme.errorContainer
                        AnnouncementPriority.IMPORTANT -> MaterialTheme.colorScheme.tertiaryContainer
                        AnnouncementPriority.INFORMATION -> MaterialTheme.colorScheme.secondaryContainer
                    }
                    val accent = when (prio) {
                        AnnouncementPriority.URGENT -> MaterialTheme.colorScheme.error
                        AnnouncementPriority.IMPORTANT -> MaterialTheme.colorScheme.tertiary
                        AnnouncementPriority.INFORMATION -> MaterialTheme.colorScheme.secondary
                    }
                    Surface(
                        color = container,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, accent.copy(alpha = 0.45f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = announcement.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = accent,
                                    modifier = Modifier.weight(1f)
                                )
                                Surface(color = accent, shape = RoundedCornerShape(6.dp)) {
                                    Text(
                                        text = prio.label,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = announcement.body,
                                style = MaterialTheme.typography.bodyMedium,
                                color = YfuaTheme.colors.headline
                            )
                        }
                    }
                }
            }

            item(key = "home_chart") {
                Text("Temps d'étude par matière", style = MaterialTheme.typography.titleLarge, color = YfuaTheme.colors.headline)
                Spacer(modifier = Modifier.height(8.dp))
                AnimatedContent(targetState = subjectStats.isEmpty(), label = "home_chart_content") { isEmpty ->
                    if (isEmpty) {
                        EmptyStateCard(
                            icon = Icons.Filled.Style,
                            title = "Aucune session enregistrée",
                            description = "Planifiez vos cours et examens ou sollicitez l'assistant IA pour organiser vos révisions.",
                            actionLabel = "Ouvrir le Planning",
                            onActionClick = onNavigateToPlanning
                        )
                    } else {
                        SubjectBreakdownChart(stats = subjectStats.take(5))
                    }
                }
            }
        }
    }
}

@Composable
private fun DailyMetricTile(label: String, value: String, accent: Color, modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, YfuaTheme.colors.cardBorder),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.headlineSmall, color = accent, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.bodySmall, color = YfuaTheme.colors.bodySlate)
        }
    }
}

@Composable
private fun SubjectBreakdownChart(stats: List<SubjectStudyStat>) {
    val maxMinutes = (stats.maxOfOrNull { it.totalMinutes } ?: 1).coerceAtLeast(1)
    val trackColor = MaterialTheme.colorScheme.primaryContainer
    val barColor = MaterialTheme.colorScheme.primary

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, YfuaTheme.colors.cardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            stats.forEach { stat ->
                val ratio = stat.totalMinutes.toFloat() / maxMinutes.toFloat()
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            text = stat.subject,
                            style = MaterialTheme.typography.bodyMedium,
                            color = YfuaTheme.colors.headline,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Text("${stat.totalMinutes} min (${stat.sessionCount}x)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                    Box(modifier = Modifier.fillMaxWidth().height(10.dp)) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawRoundRect(trackColor, size = size, cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx()))
                            drawRoundRect(
                                barColor,
                                topLeft = Offset.Zero,
                                size = Size(size.width * ratio.coerceIn(0.05f, 1f), size.height),
                                cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                            )
                        }
                    }
                }
            }
        }
    }
}
