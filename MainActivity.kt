package com.geocart.flow

import android.os.Bundle
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.UUID

data class PeriodRecord(
    val id: String = UUID.randomUUID().toString(),
    val start: String,
    val end: String = "",
    val flow: String = "Medium",
    val symptoms: String = "",
    val notes: String = ""
)
data class SexRecord(
    val id: String = UUID.randomUUID().toString(),
    val date: String,
    val method: String = "Withdrawal",
    val notes: String = ""
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { FlowApp() }
    }
}

private fun loadPeriods(ctx: Context): MutableList<PeriodRecord> {
    val raw = ctx.getSharedPreferences("flow_records", 0).getString("periods", "[]") ?: "[]"
    val arr = JSONArray(raw)
    return MutableList(arr.length()) { i ->
        val o = arr.getJSONObject(i)
        PeriodRecord(o.optString("id"), o.optString("start"), o.optString("end"),
            o.optString("flow", "Medium"), o.optString("symptoms"), o.optString("notes"))
    }
}
private fun loadSex(ctx: Context): MutableList<SexRecord> {
    val raw = ctx.getSharedPreferences("flow_records", 0).getString("sex", "[]") ?: "[]"
    val arr = JSONArray(raw)
    return MutableList(arr.length()) { i ->
        val o = arr.getJSONObject(i)
        SexRecord(o.optString("id"), o.optString("date"), o.optString("method", "Withdrawal"), o.optString("notes"))
    }
}
private fun savePeriods(ctx: Context, records: List<PeriodRecord>) {
    val arr = JSONArray()
    records.forEach { r -> arr.put(JSONObject().put("id", r.id).put("start", r.start).put("end", r.end)
        .put("flow", r.flow).put("symptoms", r.symptoms).put("notes", r.notes)) }
    ctx.getSharedPreferences("flow_records", 0).edit().putString("periods", arr.toString()).apply()
}
private fun saveSex(ctx: Context, records: List<SexRecord>) {
    val arr = JSONArray()
    records.forEach { r -> arr.put(JSONObject().put("id", r.id).put("date", r.date).put("method", r.method).put("notes", r.notes)) }
    ctx.getSharedPreferences("flow_records", 0).edit().putString("sex", arr.toString()).apply()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlowApp() {
    val ctx = LocalContext.current
    var periods by remember { mutableStateOf(loadPeriods(ctx).sortedByDescending { it.start }) }
    var sexRecords by remember { mutableStateOf(loadSex(ctx).sortedByDescending { it.date }) }
    var tab by remember { mutableStateOf(0) }
    var start by remember { mutableStateOf(LocalDate.now().toString()) }
    var end by remember { mutableStateOf("") }
    var flow by remember { mutableStateOf("Medium") }
    var symptoms by remember { mutableStateOf("") }
    var periodNotes by remember { mutableStateOf("") }
    var sexDate by remember { mutableStateOf(LocalDate.now().toString()) }
    var method by remember { mutableStateOf("Withdrawal") }
    var sexNotes by remember { mutableStateOf("") }
    var cycleLength by remember { mutableStateOf("28") }
    var message by remember { mutableStateOf("") }

    fun persistPeriods(list: List<PeriodRecord>) {
        periods = list.sortedByDescending { it.start }; savePeriods(ctx, periods)
    }
    fun persistSex(list: List<SexRecord>) {
        sexRecords = list.sortedByDescending { it.date }; saveSex(ctx, sexRecords)
    }

    MaterialTheme(colorScheme = lightColorScheme(primary = androidx.compose.ui.graphics.Color(0xFFC02672),
        secondary = androidx.compose.ui.graphics.Color(0xFF7C3AED))) {
        Scaffold(topBar = { TopAppBar(title = { Text("Flow · Period & conception tracker") }) }) { pad ->
            Column(Modifier.padding(pad).fillMaxSize()) {
                TabRow(selectedTabIndex = tab) {
                    listOf("Overview", "Periods", "Conception", "Backup").forEachIndexed { i, label ->
                        Tab(selected = tab == i, onClick = { tab = i }, text = { Text(label) })
                    }
                }
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    when (tab) {
                        0 -> {
                            Text("Your cycle overview", style = MaterialTheme.typography.headlineSmall)
                            Text("Records stay on this phone. Shared cloud sync is not enabled.")
                            OutlinedTextField(value = cycleLength, onValueChange = { cycleLength = it.filter(Char::isDigit).take(2) },
                                label = { Text("Typical cycle length in days") }, modifier = Modifier.fillMaxWidth())
                            val latest = periods.maxByOrNull { it.start }
                            if (latest != null) {
                                Text("Last recorded period: ${latest.start}", style = MaterialTheme.typography.titleMedium)
                                val n = cycleLength.toLongOrNull()
                                if (n != null && n in 21..45) {
                                    val predicted = runCatching { LocalDate.parse(latest.start).plusDays(n) }.getOrNull()
                                    predicted?.let {
                                        Text("Estimated next period: $it")
                                        Text("Rough fertile-window estimate: ${it.minusDays(19)} to ${it.minusDays(13)}")
                                    }
                                } else Text("Enter a cycle length between 21 and 45 days to show rough estimates.")
                            } else Text("No period recorded yet.")
                            Text("Estimates are not confirmation of ovulation. Irregular cycles make calendar predictions less reliable.")
                            Button(onClick = { tab = 1 }, modifier = Modifier.fillMaxWidth()) { Text("Log a period") }
                            Button(onClick = { tab = 2 }, modifier = Modifier.fillMaxWidth()) { Text("Log sex / conception notes") }
                        }
                        1 -> {
                            Text("Log a period", style = MaterialTheme.typography.headlineSmall)
                            OutlinedTextField(value = start, onValueChange = { start = it }, label = { Text("Start date (YYYY-MM-DD)") }, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = end, onValueChange = { end = it }, label = { Text("End date (optional)") }, modifier = Modifier.fillMaxWidth())
                            var expanded by remember { mutableStateOf(false) }
                            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                                OutlinedTextField(value = flow, onValueChange = {}, readOnly = true, label = { Text("Flow") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                                    modifier = Modifier.menuAnchor().fillMaxWidth())
                                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                    listOf("Spotting", "Light", "Medium", "Heavy").forEach { item ->
                                        DropdownMenuItem(text = { Text(item) }, onClick = { flow = item; expanded = false })
                                    }
                                }
                            }
                            OutlinedTextField(value = symptoms, onValueChange = { symptoms = it }, label = { Text("Symptoms (e.g. cramps)") }, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = periodNotes, onValueChange = { periodNotes = it }, label = { Text("Notes (optional)") }, modifier = Modifier.fillMaxWidth())
                            Button(onClick = {
                                val valid = runCatching { LocalDate.parse(start); if (end.isNotBlank()) LocalDate.parse(end) }.isSuccess
                                if (!valid) message = "Enter dates in YYYY-MM-DD format."
                                else if (end.isNotBlank() && end < start) message = "End date must not be before start date."
                                else {
                                    persistPeriods(periods + PeriodRecord(start = start, end = end, flow = flow, symptoms = symptoms, notes = periodNotes))
                                    message = "Period saved on this phone."; symptoms = ""; periodNotes = ""; end = ""
                                }
                            }, modifier = Modifier.fillMaxWidth()) { Text("Save period") }
                            Text(message)
                            Text("Period history", style = MaterialTheme.typography.titleLarge)
                            periods.forEach { p ->
                                Card(Modifier.fillMaxWidth()) {
                                    Column(Modifier.padding(12.dp)) {
                                        Text("${p.start}${if (p.end.isNotBlank()) " – ${p.end}" else ""}", style = MaterialTheme.typography.titleMedium)
                                        Text("Flow: ${p.flow} · Symptoms: ${p.symptoms.ifBlank { "Not recorded" }}")
                                        if (p.notes.isNotBlank()) Text(p.notes)
                                        TextButton(onClick = { persistPeriods(periods.filterNot { it.id == p.id }); message = "Period removed." }) { Text("Delete record") }
                                    }
                                }
                            }
                        }
                        2 -> {
                            Text("Conception log", style = MaterialTheme.typography.headlineSmall)
                            OutlinedTextField(value = sexDate, onValueChange = { sexDate = it }, label = { Text("Date (YYYY-MM-DD)") }, modifier = Modifier.fillMaxWidth())
                            var methodExpanded by remember { mutableStateOf(false) }
                            ExposedDropdownMenuBox(expanded = methodExpanded, onExpandedChange = { methodExpanded = !methodExpanded }) {
                                OutlinedTextField(value = method, onValueChange = {}, readOnly = true, label = { Text("Contraception / ejaculation") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(methodExpanded) },
                                    modifier = Modifier.menuAnchor().fillMaxWidth())
                                ExposedDropdownMenu(expanded = methodExpanded, onDismissRequest = { methodExpanded = false }) {
                                    listOf("Trying to conceive", "Withdrawal", "Condom", "Other contraception", "No ejaculation", "Prefer not to say").forEach { item ->
                                        DropdownMenuItem(text = { Text(item) }, onClick = { method = item; methodExpanded = false })
                                    }
                                }
                            }
                            OutlinedTextField(value = sexNotes, onValueChange = { sexNotes = it }, label = { Text("Notes (optional)") }, modifier = Modifier.fillMaxWidth())
                            Button(onClick = {
                                if (runCatching { LocalDate.parse(sexDate) }.isFailure) message = "Enter date in YYYY-MM-DD format."
                                else {
                                    persistSex(sexRecords + SexRecord(date = sexDate, method = method, notes = sexNotes))
                                    sexNotes = ""; message = "Entry saved on this phone."
                                }
                            }, modifier = Modifier.fillMaxWidth()) { Text("Save entry") }
                            Text(message)
                            sexRecords.forEach { s ->
                                Card(Modifier.fillMaxWidth()) {
                                    Column(Modifier.padding(12.dp)) {
                                        Text(s.date, style = MaterialTheme.typography.titleMedium)
                                        Text(s.method)
                                        if (s.notes.isNotBlank()) Text(s.notes)
                                        TextButton(onClick = { persistSex(sexRecords.filterNot { it.id == s.id }); message = "Entry removed." }) { Text("Delete record") }
                                    }
                                }
                            }
                        }
                        else -> {
                            Text("Backup & privacy", style = MaterialTheme.typography.headlineSmall)
                            Text("Export a JSON backup to keep a copy of your records. Backup files contain sensitive information.")
                            Button(onClick = {
                                val root = JSONObject()
                                root.put("app", "Flow")
                                root.put("version", "0.2.0")
                                val pa = JSONArray()
                                periods.forEach { p -> pa.put(JSONObject().put("id", p.id).put("start", p.start).put("end", p.end).put("flow", p.flow).put("symptoms", p.symptoms).put("notes", p.notes)) }
                                val sa = JSONArray()
                                sexRecords.forEach { s -> sa.put(JSONObject().put("id", s.id).put("date", s.date).put("method", s.method).put("notes", s.notes)) }
                                root.put("periods", pa); root.put("sex", sa)
                                val file = java.io.File(ctx.cacheDir, "flow-backup.json")
                                file.writeText(root.toString(2))
                                val uri = androidx.core.content.FileProvider.getUriForFile(ctx, ctx.packageName + ".provider", file)
                                val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                    type = "application/json"; putExtra(android.content.Intent.EXTRA_STREAM, uri)
                                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                ctx.startActivity(android.content.Intent.createChooser(intent, "Save or share Flow backup"))
                            }, modifier = Modifier.fillMaxWidth()) { Text("Export backup") }
                            Text("This export uses Android's share sheet. Save it somewhere private; do not send it unless you intend to share all included data.")
                            Text("Data is stored locally. Clearing app data or uninstalling may erase it.")
                            Text("Cloud sync, invitation codes, reminders, and app lock are not implemented in this version.")
                        }
                    }
                }
            }
        }
    }
}
