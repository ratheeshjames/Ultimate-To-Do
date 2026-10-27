package com.example.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.ToneGenerator
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import com.example.domain.TaskDomainLogic
import com.example.model.TaskItem
import com.example.model.TaskList

object PlatformHelper {

    /**
     * Plays a pleasant short confirmation tone when `sound` setting is enabled,
     * replicating the Web Audio API 700Hz 120ms oscillator beep in the original HTML app.
     */
    fun playBeepIfEnabled(enabled: Boolean) {
        if (!enabled) return
        try {
            val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 75)
            toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 120)
        } catch (_: Exception) {
            // Ignore audio hardware unavailability in unit test / headless environments
        }
    }

    fun copyToClipboard(context: Context, label: String, text: String) {
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            clipboard?.setPrimaryClip(ClipData.newPlainText(label, text))
        } catch (_: Exception) {}
    }

    fun shareText(context: Context, title: String, content: String, mimeType: String = "text/plain") {
        try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_SUBJECT, title)
                putExtra(Intent.EXTRA_TEXT, content)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, title).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: Exception) {}
    }

    /**
     * Uses Android's native PrintManager framework to print a clean, formatted task summary document,
     * replacing the browser `window.print()` feature with its true Android equivalent.
     */
    fun printTaskList(
        context: Context,
        currentList: TaskList,
        tasks: List<TaskItem>,
        localeTag: String
    ) {
        try {
            val webView = WebView(context)
            val rowsHtml = tasks.joinToString("") { t ->
                val status = if (t.done) "☑" else "☐"
                val titleStyle = if (t.done) "text-decoration:line-through;color:#666;" else "font-weight:600;"
                val due = if (t.due.isNotBlank()) " · Due: ${TaskDomainLogic.formatDueDisplay(t.due, localeTag)}" else ""
                val cat = if (t.category.isNotBlank()) " · @${escapeHtml(t.category)}" else ""
                val tags = if (t.tags.isNotEmpty()) " · ${t.tags.joinToString(" ") { "#${escapeHtml(it)}" }}" else ""
                """
                <div style="padding:10px 8px;border-bottom:1px solid #ddd;font-family:sans-serif;">
                    <span style="font-size:16px;">$status</span>
                    <span style="font-size:15px;$titleStyle"> ${escapeHtml(t.title)}</span>
                    <span style="font-size:12px;color:#555;"> [${t.priority.label}]$due$cat$tags</span>
                </div>
                """.trimIndent()
            }
            val html = """
                <!DOCTYPE html>
                <html>
                <head><meta charset="utf-8"><title>${escapeHtml(currentList.name)} - Ultimate To-Do</title></head>
                <body style="padding:24px;font-family:sans-serif;color:#111;">
                    <h1 style="margin:0 0 8px 0;">${escapeHtml(currentList.name)} — Tasks</h1>
                    <p style="color:#555;margin:0 0 16px 0;">Total: ${tasks.size} · Completed: ${tasks.count { it.done }}</p>
                    $rowsHtml
                </body>
                </html>
            """.trimIndent()

            webView.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView, url: String) {
                    val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
                    val jobName = "UltimateToDo_${currentList.name}"
                    val printAdapter = view.createPrintDocumentAdapter(jobName)
                    printManager?.print(jobName, printAdapter, PrintAttributes.Builder().build())
                }
            }
            webView.loadDataWithBaseURL(null, html, "text/HTML", "UTF-8", null)
        } catch (_: Exception) {}
    }

    private fun escapeHtml(input: String): String {
        return input
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
    }
}
