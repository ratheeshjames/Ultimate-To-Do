package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.domain.TaskDomainLogic
import com.example.model.TaskItem
import com.example.ui.PomodoroState
import com.example.ui.theme.JetBrainsMonoFontFamily
import kotlin.random.Random

private data class ConfettiParticle(
    val startXFraction: Float,
    val velocityX: Float,
    val velocityY: Float,
    val color: Color,
    val sizeDp: Float,
    val rotationSpeed: Float
)

@Composable
fun ConfettiBurstOverlay(
    triggerCount: Int,
    modifier: Modifier = Modifier
) {
    if (triggerCount <= 0) return
    val progress = remember(triggerCount) { Animatable(0f) }
    val particles = remember(triggerCount) {
        val palette = listOf(
            Color(0xFF6366F1),
            Color(0xFF14B8A6),
            Color(0xFFF43F5E),
            Color(0xFFF59E0B),
            Color(0xFF22C55E),
            Color(0xFFA855F7)
        )
        List(26) {
            val rnd = Random(triggerCount * 100 + it)
            ConfettiParticle(
                startXFraction = 0.25f + rnd.nextFloat() * 0.5f,
                velocityX = (rnd.nextFloat() - 0.5f) * 520f,
                velocityY = 260f + rnd.nextFloat() * 380f,
                color = palette[it % palette.size],
                sizeDp = 7f + rnd.nextFloat() * 6f,
                rotationSpeed = 180f + rnd.nextFloat() * 360f
            )
        }
    }

    LaunchedEffect(triggerCount) {
        progress.snapTo(0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 950, easing = FastOutSlowInEasing)
        )
    }

    if (progress.value < 1f) {
        Canvas(modifier = modifier.fillMaxSize()) {
            val t = progress.value
            val alpha = (1f - t).coerceIn(0f, 1f)
            val startY = size.height * 0.32f
            particles.forEach { p ->
                val x = size.width * p.startXFraction + p.velocityX * t
                val y = startY + p.velocityY * t * t
                val pxSize = p.sizeDp.dp.toPx()
                rotate(degrees = p.rotationSpeed * t, pivot = Offset(x, y)) {
                    drawRect(
                        color = p.color.copy(alpha = alpha),
                        topLeft = Offset(x - pxSize / 2f, y - pxSize / 2f),
                        size = Size(pxSize, pxSize * 0.65f)
                    )
                }
            }
        }
    }
}

@Composable
fun CompletionRingGauge(
    percent: Int,
    modifier: Modifier = Modifier,
    size: Dp = 76.dp,
    strokeWidth: Dp = 9.dp
) {
    val animatedFraction by animateFloatAsState(
        targetValue = (percent.coerceIn(0, 100) / 100f),
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "ring_fraction"
    )
    val trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.14f)
    val activeColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = strokeWidth.toPx()
            val inset = strokePx / 2f
            val arcSize = Size(this.size.width - strokePx, this.size.height - strokePx)
            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
            drawArc(
                color = activeColor,
                startAngle = -90f,
                sweepAngle = 360f * animatedFraction,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
        }
        Text(
            text = "$percent%",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.testTag("completion_ring_text")
        )
    }
}

@Composable
fun FloatingPomodoroPill(
    pomodoroState: PomodoroState,
    activeTask: TaskItem?,
    onTogglePause: () -> Unit,
    onStop: () -> Unit,
    onOpenFocusScreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (pomodoroState.activeTaskId == null) return
    val minutes = (pomodoroState.remainingSeconds / 60).toString().padStart(2, '0')
    val seconds = (pomodoroState.remainingSeconds % 60).toString().padStart(2, '0')

    Surface(
        onClick = onOpenFocusScreen,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        tonalElevation = 8.dp,
        shadowElevation = 6.dp,
        modifier = modifier.testTag("floating_pomodoro_pill")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Timer,
                contentDescription = "Active Focus Session",
                modifier = Modifier.size(20.dp)
            )
            Column(modifier = Modifier.weight(1f, fill = false)) {
                Text(
                    text = "$minutes:$seconds",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold
                    )
                )
                if (activeTask != null) {
                    Text(
                        text = activeTask.title,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            IconButton(
                onClick = onTogglePause,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("pomodoro_pill_pause_button")
            ) {
                Icon(
                    imageVector = if (pomodoroState.isRunning) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (pomodoroState.isRunning) "Pause timer" else "Resume timer"
                )
            }
            IconButton(
                onClick = onStop,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("pomodoro_pill_stop_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Stop timer"
                )
            }
        }
    }
}

@Composable
fun OnboardingTourDialog(
    stepIndex: Int,
    onNext: () -> Unit,
    onSkip: () -> Unit
) {
    val steps = TaskDomainLogic.TOUR_STEPS
    if (stepIndex !in steps.indices) return
    val (title, body) = steps[stepIndex]
    val isLast = stepIndex == steps.lastIndex

    AlertDialog(
        onDismissRequest = onSkip,
        shape = RoundedCornerShape(28.dp),
        icon = {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        },
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Step ${stepIndex + 1} of ${steps.size}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall
                )
            }
        },
        text = {
            Text(
                text = body,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            Button(
                onClick = onNext,
                modifier = Modifier.testTag("tour_next_button")
            ) {
                Text(if (isLast) "Done" else "Next")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onSkip,
                modifier = Modifier.testTag("tour_skip_button")
            ) {
                Text("Skip")
            }
        }
    )
}

@Composable
fun SmartSyntaxHelpDialog(
    onDismiss: () -> Unit,
    onReplayTour: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        title = {
            Text(
                text = "Smart Syntax & Guide",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Type inline tokens when adding a task to set metadata automatically:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                SyntaxRow("!high  !med  !low", "Set task priority (or !h, !m, !l)")
                SyntaxRow("#tag", "Attach one or more tags (e.g. #work #urgent)")
                SyntaxRow("@category", "Assign a category (e.g. @home)")
                SyntaxRow("~30m", "Set time estimate in minutes")
                SyntaxRow("today / tomorrow / next week", "Set relative due date")
                SyntaxRow("daily / weekly / monthly", "Configure automatic recurrence")

                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Example:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Call mom tomorrow !high #family @home ~30m weekly",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier.testTag("help_close_button")
            ) {
                Text("Got it")
            }
        },
        dismissButton = {
            FilledTonalButton(
                onClick = onReplayTour,
                modifier = Modifier.testTag("replay_tour_button")
            ) {
                Text("Replay Tour")
            }
        }
    )
}

@Composable
private fun SyntaxRow(token: String, description: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = token,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
