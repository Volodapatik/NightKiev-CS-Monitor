package ua.nightkiev.csmonitor.ui

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonitorApp(viewModel: MonitorViewModel) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Головна", "Налаштування", "Логи")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Нічний Київ | Монітор", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { index, title ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = {
                            Icon(
                                imageVector = when (index) {
                                    0 -> Icons.Default.Home
                                    1 -> Icons.Default.Settings
                                    else -> Icons.Default.History
                                },
                                contentDescription = title
                            )
                        },
                        label = { Text(title) }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            when (selectedTab) {
                0 -> HomeScreen(viewModel = viewModel)
                1 -> SettingsScreen(viewModel = viewModel)
                2 -> LogsScreen(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun HomeScreen(viewModel: MonitorViewModel) {
    val info by viewModel.serverInfo.collectAsState()
    val isMonitoring by viewModel.isMonitoring.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.refreshOnce()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .size(120.dp)
                .background(
                    color = if (info.isOnline) Color(0xFF4CAF50) else Color(0xFFEF5350),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (info.isOnline) "ОНЛАЙН" else "ОФЛАЙН",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        }

        Text(
            text = if (info.isOnline) "Сервер відповідає" else "Сервер не відповідає",
            style = MaterialTheme.typography.titleMedium
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                InfoRow(label = "Карта", value = if (info.isOnline) info.mapName.ifEmpty { "—" } else "—")
                InfoRow(label = "Гравці", value = info.playersText)
                if (info.isOnline && info.pingMs > 0) {
                    InfoRow(label = "Пінг", value = "${info.pingMs} мс")
                }
                if (info.serverName.isNotEmpty()) {
                    InfoRow(label = "Назва", value = info.serverName)
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = {
                if (isMonitoring) {
                    viewModel.stopMonitoring()
                } else {
                    viewModel.startMonitoring()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isMonitoring) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.primary
                }
            )
        ) {
            Text(
                text = if (isMonitoring) "СТОП" else "СТАРТ",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        TextButton(onClick = { viewModel.refreshOnce() }) {
            Text("Оновити зараз")
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun SettingsScreen(viewModel: MonitorViewModel) {
    val ip by viewModel.ip.collectAsState()
    val port by viewModel.port.collectAsState()
    val interval by viewModel.interval.collectAsState()
    val attempts by viewModel.attempts.collectAsState()
    val autoStart by viewModel.autoStart.collectAsState()

    var saved by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OutlinedTextField(
            value = ip,
            onValueChange = { viewModel.updateIp(it) },
            label = { Text("IP-адреса") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        OutlinedTextField(
            value = port,
            onValueChange = { viewModel.updatePort(it) },
            label = { Text("Порт") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        OutlinedTextField(
            value = interval,
            onValueChange = { viewModel.updateInterval(it) },
            label = { Text("Інтервал перевірки (сек)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        OutlinedTextField(
            value = attempts,
            onValueChange = { viewModel.updateAttempts(it) },
            label = { Text("Кількість спроб") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Автозапуск після перезавантаження")
            Switch(
                checked = autoStart,
                onCheckedChange = { viewModel.updateAutoStart(it) }
            )
        }

        Button(
            onClick = {
                viewModel.saveSettings(ip, port, interval, attempts, autoStart)
                saved = true
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Зберегти")
        }

        if (saved) {
            Text(
                text = "Налаштування збережено",
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun LogsScreen(viewModel: MonitorViewModel) {
    val logs by viewModel.logs.collectAsState()
    val dateFormat = remember { SimpleDateFormat("dd.MM HH:mm:ss", Locale.getDefault()) }

    LaunchedEffect(Unit) {
        viewModel.refreshLogs()
    }

    if (logs.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("Логів поки немає")
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(logs) { entry ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = dateFormat.format(Date(entry.timestamp)),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(text = entry.message)
                    }
                }
            }
        }
    }
}
