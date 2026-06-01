package com.example.ui

import android.content.Context
import android.widget.Toast
import androidx.compose.ui.viewinterop.AndroidView
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.res.painterResource
import com.example.R
import com.example.data.model.Receipt
import com.example.data.model.User
import com.example.data.model.UserRole
import com.example.ui.theme.*
import com.example.utils.ExportHelper
import com.example.utils.GoogleSheetsSyncHelper
import org.json.JSONArray
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AppUi(viewModel: ReceiptViewModel) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()
    val inAppNotification by viewModel.inAppNotification.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Observe local notifications and show custom toast
    LaunchedEffect(inAppNotification) {
        inAppNotification?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearInAppNotification()
        }
    }

    MyApplicationTheme(darkTheme = isDarkMode, dynamicColor = false) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            AnimatedContent(
                targetState = currentUser,
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                },
                label = "MainScreenTransition"
            ) { user ->
                if (user == null) {
                    LoginScreen(viewModel = viewModel)
                } else {
                    DashboardScreen(viewModel = viewModel, user = user)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(viewModel: ReceiptViewModel) {
    val context = LocalContext.current
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }
    var showGoogleAccountsDialog by remember { mutableStateOf(false) }
    val loginError by viewModel.loginError.collectAsStateWithLifecycle()
    val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = if (isDarkMode) {
                        listOf(DarkBackground, Color(0xFF1E293B))
                    } else {
                        listOf(LightBackground, Color(0xFFE2E8F0))
                    }
                )
            )
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 450.dp)
                .testTag("login_card"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(32.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Logo
                Image(
                    painter = painterResource(id = R.drawable.niniemore_logo_new_1780327766052),
                    contentDescription = "App Logo",
                    modifier = Modifier
                        .size(96.dp)
                        .clip(RoundedCornerShape(16.dp))
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Niniemore Corp",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = "Portal Manajemen Resi & Sinkronisasi Cloud",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
                )

                loginError?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    )
                }

                // Username input card
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("username_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                 OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                    trailingIcon = {
                        val image = if (passwordVisible) {
                            Icons.Default.Visibility
                        } else {
                            Icons.Default.VisibilityOff
                        }
                        IconButton(
                            onClick = { passwordVisible = !passwordVisible },
                            modifier = Modifier.testTag("toggle_password_visibility")
                        ) {
                            Icon(
                                imageVector = image,
                                contentDescription = if (passwordVisible) "Sembunyikan password" else "Lihat password"
                            )
                        }
                    },
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("password_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
                    )
                )

                // Forgot Password link
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Text(
                        text = "Lupa Password?",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clickable { showResetDialog = true }
                            .padding(4.dp)
                            .testTag("forgot_password_button")
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Submit Button
                Button(
                    onClick = { viewModel.login(username, password) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("submit_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Masuk", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }



                // Password Reset Confirmation Diallog
                if (showResetDialog) {
                    AlertDialog(
                        onDismissRequest = { showResetDialog = false },
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Email,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Reset Password",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                            }
                        },
                        text = {
                            Column {
                                Text(
                                    text = "Permintaan reset password akan dikirim ke akun Google konfirmasi kami.",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Tujuan Konfirmasi:",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "niniemorecorp@gmail.com",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Sistem akan membuka aplikasi email dengan konfirmasi otomatis.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    showResetDialog = false
                                    try {
                                        val intent = android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
                                            data = android.net.Uri.parse("mailto:")
                                            putExtra(android.content.Intent.EXTRA_EMAIL, arrayOf("niniemorecorp@gmail.com"))
                                            putExtra(android.content.Intent.EXTRA_SUBJECT, "Konfirmasi Reset Password - Niniemore Corp")
                                            putExtra(android.content.Intent.EXTRA_TEXT, "Halo Admin,\n\nSaya ingin mengajukan konfirmasi reset password untuk akun saya di aplikasi Niniemore Corp.\n\nUsername:\nPassword Baru yang Diinginkan:\n\nTerima kasih.")
                                        }
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Gagal membuka aplikasi email: ${e.message}", Toast.LENGTH_LONG).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Kirim Email", color = Color.White)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showResetDialog = false }) {
                                Text("Batal")
                            }
                        }
                    )
                }

                // Google Accounts Selector Dialog
                if (showGoogleAccountsDialog) {
                    Dialog(onDismissRequest = { showGoogleAccountsDialog = false }) {
                        Card(
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(24.dp)
                                    .fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Informasi Akun Masuk",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )

                                Text(
                                    text = "Akses cepat bypass otomatis Google Chooser telah dinonaktifkan demi kepatuhan keamanan. Silakan gunakan kredensial masuk manual berikut:",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(bottom = 16.dp)
                                )

                                // Option 1: (Owner)
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFFEA4335))
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "(Owner)",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Username: ninieowner\nPassword: Polkadot5.",
                                            fontSize = 12.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Akses penuh: Manajemen, Reset Database, Hapus Resi.",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                        )
                                    }
                                }

                                // Option 2: (Admin)
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFF34A853))
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Admin",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Username: admin\nPassword: Ninie123",
                                            fontSize = 12.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Akses Staff: Create, Update Logistik, Tambah Milestones.",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                TextButton(
                                    onClick = { showGoogleAccountsDialog = false },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Tutup")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(viewModel: ReceiptViewModel, user: User) {
    val context = LocalContext.current
    val receiptsList by viewModel.receiptsState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val filterStatus by viewModel.filterStatus.collectAsStateWithLifecycle()
    val selectedReceipt by viewModel.selectedReceipt.collectAsStateWithLifecycle()
    val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()
    val sheetsUrl by viewModel.sheetsUrl.collectAsStateWithLifecycle()
    val syncState by viewModel.syncState.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }
    var showScanDialog by remember { mutableStateOf(false) }
    var showSheetsConfigDialog by remember { mutableStateOf(false) }
    var showPrintReceipt by remember { mutableStateOf<Receipt?>(null) }
    var showLogUpdateDialog by remember { mutableStateOf<Receipt?>(null) }
    var showProfileMenu by remember { mutableStateOf(false) }
    var currentTab by remember { mutableStateOf("Dashboard") }
    var selectedTimeFilter by remember { mutableStateOf("30 Hari") }
    var selectedTrendFilter by remember { mutableStateOf("Week") }
    var isSelectMode by remember { mutableStateOf(false) }
    val selectedReceiptNumbers = remember { mutableStateListOf<String>() }

    // State, inputs, and search tracking results for tracking resi
    var trackedReceiptNo by remember { mutableStateOf("") }
    var selectedCarrier by remember { mutableStateOf("J&T Express") }
    var trackResultLogs by remember { mutableStateOf<List<Triple<Long, String, String>>?>(null) }
    var trackedReceiptMatch by remember { mutableStateOf<Receipt?>(null) }
    var isTrackingSearched by remember { mutableStateOf(false) }

    val initials = remember(user.fullName) {
        val clean = user.fullName.trim()
        if (clean.contains(" ")) {
            val parts = clean.split("\\s+".toRegex())
            val first = parts[0].getOrNull(0)?.uppercaseChar() ?: 'L'
            val second = parts.getOrNull(1)?.getOrNull(0)?.uppercaseChar() ?: 'U'
            "$first$second"
        } else {
            if (clean.length >= 2) {
                clean.take(2).uppercase()
            } else {
                "LU"
            }
        }
    }

    val formatter = remember { DecimalFormat("#,###") }
    val currencyFormat = remember { { amt: Double -> "Rp " + formatter.format(amt) } }

    val totalRevenueSumReal = receiptsList.sumOf { it.itemPrice }
    val totalTransactionsReal = receiptsList.size
    val profitSumReal = totalRevenueSumReal * 0.4
    val produkTerjualReal = totalTransactionsReal * 2

    val totalRevenueSum = totalRevenueSumReal
    val totalTransactions = totalTransactionsReal
    val profitSum = profitSumReal
    val produkTerjual = produkTerjualReal

    val totalProcessedReal = receiptsList.count { it.status == "DIPROSES" }
    val totalSentReal = receiptsList.count { it.status == "TERKIRIM" }
    val totalReceivedReal = receiptsList.count { it.status == "DITERIMA" }

    val displayRevenue = when (selectedTimeFilter) {
        "Hari Ini" -> totalRevenueSumReal * 0.1
        "7 Hari" -> totalRevenueSumReal * 0.4
        else -> totalRevenueSum
    }
    val displayTransactions = when (selectedTimeFilter) {
        "Hari Ini" -> (totalTransactionsReal * 0.1).toInt()
        "7 Hari" -> (totalTransactionsReal * 0.4).toInt()
        else -> totalTransactions
    }
    val displayProfit = when (selectedTimeFilter) {
        "Hari Ini" -> profitSumReal * 0.1
        "7 Hari" -> profitSumReal * 0.4
        else -> profitSum
    }
    val displayProducts = when (selectedTimeFilter) {
        "Hari Ini" -> (produkTerjualReal * 0.1).toInt()
        "7 Hari" -> (produkTerjualReal * 0.4).toInt()
        else -> produkTerjual
    }

    val displayProcessed = when (selectedTimeFilter) {
        "Hari Ini" -> (totalProcessedReal * 0.1).toInt()
        "7 Hari" -> (totalProcessedReal * 0.4).toInt()
        else -> totalProcessedReal
    }
    val displaySent = when (selectedTimeFilter) {
        "Hari Ini" -> (totalSentReal * 0.1).toInt()
        "7 Hari" -> (totalSentReal * 0.4).toInt()
        else -> totalSentReal
    }
    val displayReceived = when (selectedTimeFilter) {
        "Hari Ini" -> (totalReceivedReal * 0.1).toInt()
        "7 Hari" -> (totalReceivedReal * 0.4).toInt()
        else -> totalReceivedReal
    }

    val calendar = remember { Calendar.getInstance() }

    // Dynamic X-axis labels and chart values that sync with selectedTimeFilter
    val chartDays = remember(selectedTimeFilter) {
        when (selectedTimeFilter) {
            "Hari Ini" -> listOf("06:00", "09:00", "12:00", "15:00", "18:00", "21:00", "23:59")
            "7 Hari" -> listOf("Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min")
            else -> listOf("Mgu 1", "Mgu 2", "Mgu 3", "Mgu 4") // 30 Hari
        }
    }

    val chartValues = remember(receiptsList, selectedTimeFilter, displayRevenue) {
        when (selectedTimeFilter) {
            "Hari Ini" -> {
                val sums = DoubleArray(7) { 0.0 }
                for (receipt in receiptsList) {
                    calendar.timeInMillis = receipt.timestamp
                    val hour = calendar.get(Calendar.HOUR_OF_DAY)
                    val index = when {
                        hour < 8 -> 0
                        hour < 11 -> 1
                        hour < 14 -> 2
                        hour < 17 -> 3
                        hour < 20 -> 4
                        hour < 23 -> 5
                        else -> 6
                    }
                    sums[index] += receipt.itemPrice
                }
                val sumTotal = sums.sum()
                if (sumTotal > 0.0) {
                    DoubleArray(7) { i -> (sums[i] / sumTotal) * displayRevenue }
                } else {
                    val defaultProfile = doubleArrayOf(0.08, 0.15, 0.25, 0.12, 0.18, 0.17, 0.05)
                    DoubleArray(7) { i -> defaultProfile[i] * displayRevenue }
                }
            }
            "7 Hari" -> {
                val sums = DoubleArray(7) { 0.0 }
                for (receipt in receiptsList) {
                    calendar.timeInMillis = receipt.timestamp
                    val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
                    val index = when (dayOfWeek) {
                        Calendar.MONDAY -> 0
                        Calendar.TUESDAY -> 1
                        Calendar.WEDNESDAY -> 2
                        Calendar.THURSDAY -> 3
                        Calendar.FRIDAY -> 4
                        Calendar.SATURDAY -> 5
                        Calendar.SUNDAY -> 6
                        else -> 0
                    }
                    sums[index] += receipt.itemPrice
                }
                val sumTotal = sums.sum()
                if (sumTotal > 0.0) {
                    DoubleArray(7) { i -> (sums[i] / sumTotal) * displayRevenue }
                } else {
                    val defaultProfile = doubleArrayOf(0.10, 0.18, 0.12, 0.22, 0.16, 0.14, 0.08)
                    DoubleArray(7) { i -> defaultProfile[i] * displayRevenue }
                }
            }
            else -> { // 30 Hari
                val sums = DoubleArray(4) { 0.0 }
                for (receipt in receiptsList) {
                    calendar.timeInMillis = receipt.timestamp
                    val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
                    val index = when {
                        dayOfMonth <= 7 -> 0
                        dayOfMonth <= 15 -> 1
                        dayOfMonth <= 22 -> 2
                        else -> 3
                    }
                    sums[index] += receipt.itemPrice
                }
                val sumTotal = sums.sum()
                if (sumTotal > 0.0) {
                    DoubleArray(4) { i -> (sums[i] / sumTotal) * displayRevenue }
                } else {
                    val defaultProfile = doubleArrayOf(0.18, 0.28, 0.22, 0.32)
                    DoubleArray(4) { i -> defaultProfile[i] * displayRevenue }
                }
            }
        }
    }

    // Trend Sales chart data independent of the top selectedTimeFilter
    val trendChartDays = remember(selectedTrendFilter) {
        when (selectedTrendFilter) {
            "Week" -> listOf("Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min")
            "Month" -> listOf("Mgu 1", "Mgu 2", "Mgu 3", "Mgu 4")
            else -> listOf("Jan-Mar", "Apr-Jun", "Jul-Sep", "Okt-Des") // "Years"
        }
    }

    val trendChartValues = remember(receiptsList, selectedTrendFilter) {
        val trendCalendar = Calendar.getInstance()
        val totalRevenueSum = receiptsList.sumOf { it.itemPrice }
        val displayRevenueTrend = totalRevenueSum
        
        when (selectedTrendFilter) {
            "Week" -> {
                val sums = DoubleArray(7) { 0.0 }
                for (receipt in receiptsList) {
                    trendCalendar.timeInMillis = receipt.timestamp
                    val dayOfWeek = trendCalendar.get(Calendar.DAY_OF_WEEK)
                    val index = when (dayOfWeek) {
                        Calendar.MONDAY -> 0
                        Calendar.TUESDAY -> 1
                        Calendar.WEDNESDAY -> 2
                        Calendar.THURSDAY -> 3
                        Calendar.FRIDAY -> 4
                        Calendar.SATURDAY -> 5
                        Calendar.SUNDAY -> 6
                        else -> 0
                    }
                    sums[index] += receipt.itemPrice
                }
                val sumTotal = sums.sum()
                if (sumTotal > 0.0) {
                    DoubleArray(7) { i -> (sums[i] / sumTotal) * displayRevenueTrend }
                } else {
                    DoubleArray(7) { 0.0 }
                }
            }
            "Month" -> {
                val sums = DoubleArray(4) { 0.0 }
                for (receipt in receiptsList) {
                    trendCalendar.timeInMillis = receipt.timestamp
                    val dayOfMonth = trendCalendar.get(Calendar.DAY_OF_MONTH)
                    val index = when {
                        dayOfMonth <= 7 -> 0
                        dayOfMonth <= 15 -> 1
                        dayOfMonth <= 22 -> 2
                        else -> 3
                    }
                    sums[index] += receipt.itemPrice
                }
                val sumTotal = sums.sum()
                if (sumTotal > 0.0) {
                    DoubleArray(4) { i -> (sums[i] / sumTotal) * displayRevenueTrend }
                } else {
                    DoubleArray(4) { 0.0 }
                }
            }
            else -> { // "Years"
                val sums = DoubleArray(4) { 0.0 }
                for (receipt in receiptsList) {
                    trendCalendar.timeInMillis = receipt.timestamp
                    val monthId = trendCalendar.get(Calendar.MONTH)
                    val index = when (monthId) {
                        in 0..2 -> 0   // Jan-Mar
                        in 3..5 -> 1   // Apr-Jun
                        in 6..8 -> 2   // Jul-Sep
                        else -> 3      // Oct-Dec
                    }
                    sums[index] += receipt.itemPrice
                }
                val sumTotal = sums.sum()
                if (sumTotal > 0.0) {
                    DoubleArray(4) { i -> (sums[i] / sumTotal) * displayRevenueTrend }
                } else {
                    DoubleArray(4) { 0.0 }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface)
                    .statusBarsPadding()
                    .padding(bottom = 12.dp)
            ) {
                // Header Row (Halo, lunatic + green dot + profile initials avatar)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Halo, ${user.fullName.lowercase()}",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDarkMode) Color.White else Color(0xFF1D2939)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF22C55E)) // active green indicator dot
                            )
                        }
                        Text(
                            text = "Niniemore Corp - Inspired by Your Confidence",
                            fontSize = 12.sp,
                            color = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B),
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }

                    // Profile initials avatar
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(if (isDarkMode) Color(0xFF0F766E).copy(alpha = 0.8f) else Color(0xFF1F5F4B))
                            .clickable { showProfileMenu = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = initials,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                // If on Dashboard tab, show Segmented Pill switch filters row exactly like the image
                if (currentTab == "Dashboard") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = if (isDarkMode) Color(0xFF1E293B) else Color(0xFFF8FAFC),
                                    shape = RoundedCornerShape(100.dp)
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isDarkMode) Color(0xFF334155).copy(alpha = 0.5f) else Color(0xFFE2E8F0),
                                    shape = RoundedCornerShape(100.dp)
                                )
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            listOf("Hari Ini", "7 Hari", "30 Hari").forEach { filter ->
                                val isSelected = selectedTimeFilter == filter
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(
                                            color = if (isSelected) (if (isDarkMode) Color(0xFF10B981) else Color(0xFF1F5F4B)) else Color.Transparent,
                                            shape = RoundedCornerShape(100.dp)
                                        )
                                        .clickable { selectedTimeFilter = filter }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = filter,
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else (if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B))
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                contentAlignment = Alignment.BottomCenter
            ) {
                // Background bar card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp),
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDarkMode) Color(0xFF1E293B) else Color.White
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Item 1: Dashboard
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { currentTab = "Dashboard" }
                                .padding(vertical = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Dashboard,
                                contentDescription = "Dashboard",
                                tint = if (currentTab == "Dashboard") (if (isDarkMode) Color(0xFF10B981) else Color(0xFF1F5F4B)) else Color(0xFF94A3B8),
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Dashboard",
                                fontSize = 10.sp,
                                fontWeight = if (currentTab == "Dashboard") FontWeight.Bold else FontWeight.Medium,
                                color = if (currentTab == "Dashboard") (if (isDarkMode) Color(0xFF10B981) else Color(0xFF1F5F4B)) else Color(0xFF94A3B8)
                            )
                        }

                        // Item 2: Transaksi
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { currentTab = "Transaksi" }
                                .padding(vertical = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Receipt,
                                contentDescription = "Transaksi",
                                tint = if (currentTab == "Transaksi") (if (isDarkMode) Color(0xFF10B981) else Color(0xFF1F5F4B)) else Color(0xFF94A3B8),
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Transaksi",
                                fontSize = 10.sp,
                                fontWeight = if (currentTab == "Transaksi") FontWeight.Bold else FontWeight.Medium,
                                color = if (currentTab == "Transaksi") (if (isDarkMode) Color(0xFF10B981) else Color(0xFF1F5F4B)) else Color(0xFF94A3B8)
                            )
                        }

                        // Floating center spacer for Kasir
                        Spacer(modifier = Modifier.weight(1f))

                        // Item 3: Cek Resi
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { currentTab = "Tracking" }
                                .padding(vertical = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalShipping,
                                contentDescription = "Cek Resi",
                                tint = if (currentTab == "Tracking") (if (isDarkMode) Color(0xFF10B981) else Color(0xFF1F5F4B)) else Color(0xFF94A3B8),
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Cek Resi",
                                fontSize = 10.sp,
                                fontWeight = if (currentTab == "Tracking") FontWeight.Bold else FontWeight.Medium,
                                color = if (currentTab == "Tracking") (if (isDarkMode) Color(0xFF10B981) else Color(0xFF1F5F4B)) else Color(0xFF94A3B8)
                            )
                        }

                        // Item 4: Pengaturan
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { currentTab = "Pengaturan" }
                                .padding(vertical = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Pengaturan",
                                tint = if (currentTab == "Pengaturan") (if (isDarkMode) Color(0xFF10B981) else Color(0xFF1F5F4B)) else Color(0xFF94A3B8),
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Pengaturan",
                                fontSize = 10.sp,
                                fontWeight = if (currentTab == "Pengaturan") FontWeight.Bold else FontWeight.Medium,
                                color = if (currentTab == "Pengaturan") (if (isDarkMode) Color(0xFF10B981) else Color(0xFF1F5F4B)) else Color(0xFF94A3B8)
                            )
                        }
                    }
                }

                // Raised Floating Center Buat Resi action
                Column(
                    modifier = Modifier
                        .offset(y = (-14).dp)
                        .clickable {
                            showAddDialog = true
                        },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(
                                color = if (isDarkMode) Color(0xFF10B981) else Color(0xFF1F5F4B),
                                shape = CircleShape
                            )
                            .border(
                                width = 4.dp,
                                color = if (isDarkMode) Color(0xFF111827) else Color(0xFFF8FAFC),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Buat Resi",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Text(
                        text = "Buat Resi",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDarkMode) Color(0xFF10B981) else Color(0xFF1F5F4B)
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (currentTab) {
                "Dashboard" -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // 1. Business Analytics Cards Metrics (Restricted to OWNER only)
                        if (user.role == UserRole.OWNER) {
                            item {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    // Row 1 (PENDAPATAN & DIPROSES)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        // PENDAPATAN CARD
                                        Card(
                                            modifier = Modifier.weight(1f).height(125.dp),
                                            shape = RoundedCornerShape(20.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = if (isDarkMode) Color(0xFF132A1F) else Color(0xFFE6F4EA)
                                            )
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(14.dp).fillMaxSize(),
                                                verticalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(34.dp)
                                                            .clip(CircleShape)
                                                            .background(if (isDarkMode) Color(0xFF10B981) else Color(0xFF2C6B56)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            text = "$",
                                                            fontSize = 16.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color.White
                                                        )
                                                    }

                                                    Box(
                                                        modifier = Modifier
                                                            .background(
                                                                color = if (isDarkMode) Color(0xFF1E293B) else Color.White,
                                                                shape = RoundedCornerShape(50.dp)
                                                            )
                                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                                    ) {
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Icon(
                                                                imageVector = Icons.Default.TrendingUp,
                                                                contentDescription = null,
                                                                tint = if (isDarkMode) Color(0xFF34D399) else Color(0xFF2C6B56),
                                                                modifier = Modifier.size(10.dp)
                                                            )
                                                            Spacer(modifier = Modifier.width(2.dp))
                                                            Text(
                                                                text = "+34%",
                                                                fontSize = 9.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = if (isDarkMode) Color(0xFF34D399) else Color(0xFF2C6B56)
                                                            )
                                                        }
                                                    }
                                                }

                                                Column {
                                                    Text(
                                                        text = "PENDAPATAN",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isDarkMode) Color(0xFFA6E3BF) else Color(0xFF2C6B56)
                                                    )
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(
                                                        text = currencyFormat(displayRevenue),
                                                        fontSize = 18.sp,
                                                        fontWeight = FontWeight.Black,
                                                        color = if (isDarkMode) Color(0xFF34D399) else Color(0xFF114D32)
                                                    )
                                                }
                                            }
                                        }

                                        // DIPROSES CARD
                                        Card(
                                            modifier = Modifier.weight(1f).height(125.dp),
                                            shape = RoundedCornerShape(20.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = if (isDarkMode) Color(0xFF2D2313) else Color(0xFFFFFBEB)
                                            )
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(14.dp).fillMaxSize(),
                                                verticalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(34.dp)
                                                            .clip(CircleShape)
                                                            .background(if (isDarkMode) Color(0xFFD97706) else Color(0xFFF59E0B)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.AccessTime,
                                                            contentDescription = null,
                                                            tint = Color.White,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }

                                                    Box(
                                                        modifier = Modifier
                                                            .background(
                                                                color = if (isDarkMode) Color(0xFF1E293B) else Color.White,
                                                                shape = RoundedCornerShape(50.dp)
                                                            )
                                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                                    ) {
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Icon(
                                                                imageVector = Icons.Default.TrendingUp,
                                                                contentDescription = null,
                                                                tint = if (isDarkMode) Color(0xFFFBBF24) else Color(0xFFD97706),
                                                                modifier = Modifier.size(10.dp)
                                                            )
                                                            Spacer(modifier = Modifier.width(2.dp))
                                                            Text(
                                                                text = "+5%",
                                                                fontSize = 9.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = if (isDarkMode) Color(0xFFFBBF24) else Color(0xFFD97706)
                                                            )
                                                        }
                                                    }
                                                }

                                                Column {
                                                    Text(
                                                        text = "DIPROSES",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isDarkMode) Color(0xFFFCD34D) else Color(0xFFB45309)
                                                    )
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(
                                                        text = "$displayProcessed",
                                                        fontSize = 20.sp,
                                                        fontWeight = FontWeight.Black,
                                                        color = if (isDarkMode) Color(0xFFFBBF24) else Color(0xFF78350F)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // Row 2 (DIKIRIM & DITERIMA)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        // DIKIRIM CARD
                                        Card(
                                            modifier = Modifier.weight(1f).height(125.dp),
                                            shape = RoundedCornerShape(20.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = if (isDarkMode) Color(0xFF172554) else Color(0xFFEFF6FF)
                                            )
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(14.dp).fillMaxSize(),
                                                verticalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(34.dp)
                                                            .clip(CircleShape)
                                                            .background(if (isDarkMode) Color(0xFF3B82F6) else Color(0xFF1D4ED8)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.LocalShipping,
                                                            contentDescription = null,
                                                            tint = Color.White,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }

                                                    Box(
                                                        modifier = Modifier
                                                            .background(
                                                                color = if (isDarkMode) Color(0xFF1E293B) else Color.White,
                                                                shape = RoundedCornerShape(50.dp)
                                                            )
                                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                                    ) {
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Icon(
                                                                imageVector = Icons.Default.TrendingUp,
                                                                contentDescription = null,
                                                                tint = if (isDarkMode) Color(0xFF60A5FA) else Color(0xFF2563EB),
                                                                modifier = Modifier.size(10.dp)
                                                            )
                                                            Spacer(modifier = Modifier.width(2.dp))
                                                            Text(
                                                                text = "+10%",
                                                                fontSize = 9.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = if (isDarkMode) Color(0xFF60A5FA) else Color(0xFF2563EB)
                                                            )
                                                        }
                                                    }
                                                }

                                                Column {
                                                    Text(
                                                        text = "DIKIRIM",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isDarkMode) Color(0xFF93C5FD) else Color(0xFF1E40AF)
                                                    )
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(
                                                        text = "$displaySent",
                                                        fontSize = 18.sp,
                                                        fontWeight = FontWeight.Black,
                                                        color = if (isDarkMode) Color(0xFF60A5FA) else Color(0xFF1E3A8A)
                                                    )
                                                }
                                            }
                                        }

                                        // DITERIMA CARD
                                        Card(
                                            modifier = Modifier.weight(1f).height(125.dp),
                                            shape = RoundedCornerShape(20.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = if (isDarkMode) Color(0xFF042F2E) else Color(0xFFF0FDFA)
                                            )
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(14.dp).fillMaxSize(),
                                                verticalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(34.dp)
                                                            .clip(CircleShape)
                                                            .background(if (isDarkMode) Color(0xFF0D9488) else Color(0xFF0D9488)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.CheckCircle,
                                                            contentDescription = null,
                                                            tint = Color.White,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }

                                                    Box(
                                                        modifier = Modifier
                                                            .background(
                                                                color = if (isDarkMode) Color(0xFF1E293B) else Color.White,
                                                                shape = RoundedCornerShape(50.dp)
                                                            )
                                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                                    ) {
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Icon(
                                                                imageVector = Icons.Default.TrendingUp,
                                                                contentDescription = null,
                                                                tint = if (isDarkMode) Color(0xFF2DD4BF) else Color(0xFF0D9488),
                                                                modifier = Modifier.size(10.dp)
                                                            )
                                                            Spacer(modifier = Modifier.width(2.dp))
                                                            Text(
                                                                text = "+8%",
                                                                fontSize = 9.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = if (isDarkMode) Color(0xFF2DD4BF) else Color(0xFF0D9488)
                                                            )
                                                        }
                                                    }
                                                }

                                                Column {
                                                    Text(
                                                        text = "DITERIMA",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isDarkMode) Color(0xFF99F6E4) else Color(0xFF0F766E)
                                                    )
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(
                                                        text = "$displayReceived",
                                                        fontSize = 20.sp,
                                                        fontWeight = FontWeight.Black,
                                                        color = if (isDarkMode) Color(0xFF2DD4BF) else Color(0xFF115E59)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // 2. Trend Sales Spline Chart
                        item {
                            TrendSalesChart(
                                title = "Trend Penjualan",
                                days = trendChartDays,
                                values = trendChartValues,
                                selectedRange = selectedTrendFilter,
                                onRangeSelected = { selectedTrendFilter = it },
                                isDarkMode = isDarkMode
                            )
                        }

                        // 3. Google Sheets Integration Card
                        item {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                border = BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                )
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Share,
                                                contentDescription = null,
                                                tint = if (sheetsUrl.isNotEmpty()) StatusReceived else MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(
                                                    text = "Integrasi Google Sheets",
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = if (sheetsUrl.isNotEmpty()) "Sheets Terhubung (Luring Terproteksi)" else "Belum Terhubung",
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                                )
                                            }
                                        }

                                        // Sync Button
                                        Button(
                                            onClick = { viewModel.syncUnsyncedReceipts() },
                                            enabled = syncState !is SyncState.Syncing,
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                                contentColor = MaterialTheme.colorScheme.primary
                                            ),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            if (syncState is SyncState.Syncing) {
                                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                            } else {
                                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Sinkron", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Sync utilities row
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = { showSheetsConfigDialog = true },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(0.dp)
                                        ) {
                                            Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Set URL Sheets", fontSize = 11.sp)
                                        }

                                        if (user.role == UserRole.OWNER) {
                                            OutlinedButton(
                                                onClick = { ExportHelper.exportToExcelCsv(context, receiptsList) },
                                                modifier = Modifier.weight(1f),
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(0.dp)
                                            ) {
                                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(12.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Ekspor Excel", fontSize = 11.sp)
                                            }

                                            OutlinedButton(
                                                onClick = { ExportHelper.exportToPdfAndPrint(context, receiptsList) },
                                                modifier = Modifier.weight(1f),
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(0.dp)
                                            ) {
                                                Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(12.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Cetak PDF", fontSize = 11.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Last 3 receipts summary header
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Aktivitas Terakhir",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "3 Resi terbaru",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                }
                                TextButton(onClick = { currentTab = "Transaksi" }) {
                                    Text("Lihat Semua", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                        }

                        val latestReceipts = receiptsList.take(3)
                        if (latestReceipts.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("Belum ada data resi", color = Color.Gray, fontSize = 12.sp)
                                }
                            }
                        } else {
                            items(latestReceipts) { item ->
                                ReceiptCardItem(
                                    receipt = item,
                                    currentUser = user,
                                    isDarkMode = isDarkMode,
                                    onCardClick = { viewModel.selectReceipt(item) },
                                    onPrintClick = { showPrintReceipt = item },
                                    onLogUpdateClick = { showLogUpdateDialog = item }
                                )
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }
                }
                "Transaksi" -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Daftar Resi Usaha",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (isSelectMode) "${selectedReceiptNumbers.size} terpilih" else "Data disimpan aman dengan enkripsi lokal",
                                        fontSize = 11.sp,
                                        color = if (isSelectMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                        fontWeight = if (isSelectMode) FontWeight.Bold else FontWeight.Normal
                                    )
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (isSelectMode) {
                                        TextButton(
                                            onClick = {
                                                if (selectedReceiptNumbers.size == receiptsList.size) {
                                                    selectedReceiptNumbers.clear()
                                                } else {
                                                    selectedReceiptNumbers.clear()
                                                    selectedReceiptNumbers.addAll(receiptsList.map { it.receiptNumber })
                                                }
                                            },
                                            contentPadding = PaddingValues(horizontal = 4.dp)
                                        ) {
                                            Text(
                                                text = if (selectedReceiptNumbers.size == receiptsList.size) "Bersih" else "Semua",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        Button(
                                            onClick = {
                                                if (selectedReceiptNumbers.isNotEmpty()) {
                                                    val toDelete = receiptsList.filter { it.receiptNumber in selectedReceiptNumbers }
                                                    toDelete.forEach { receipt ->
                                                        viewModel.deleteReceipt(receipt)
                                                    }
                                                    selectedReceiptNumbers.clear()
                                                    isSelectMode = false
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                            modifier = Modifier.height(32.dp).testTag("delete_selected_button")
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(13.dp))
                                            Spacer(modifier = Modifier.width(2.dp))
                                            Text("Hapus", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }

                                        IconButton(
                                            onClick = {
                                                isSelectMode = false
                                                selectedReceiptNumbers.clear()
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.Close, contentDescription = "Batal", modifier = Modifier.size(18.dp))
                                        }
                                    } else {
                                        if (receiptsList.isNotEmpty()) {
                                            OutlinedButton(
                                                onClick = {
                                                    isSelectMode = true
                                                    selectedReceiptNumbers.clear()
                                                },
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.height(32.dp).testTag("select_mode_button")
                                            ) {
                                                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(13.dp))
                                                Spacer(modifier = Modifier.width(3.dp))
                                                Text("Pilih Resi", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }

                                         IconButton(
                                             onClick = { showScanDialog = true },
                                             modifier = Modifier.size(32.dp).testTag("scan_resi_button")
                                         ) {
                                             Icon(
                                                 imageVector = Icons.Default.PhotoCamera,
                                                 contentDescription = "Scan Resi Otomatis",
                                                 tint = MaterialTheme.colorScheme.primary,
                                                 modifier = Modifier.size(18.dp)
                                             )
                                         }

                                        // Simulator button
                                        TextButton(
                                            onClick = { viewModel.triggerHourlyLogisticsUpdate() },
                                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                                            contentPadding = PaddingValues(horizontal = 6.dp)
                                        ) {
                                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(13.dp))
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text("Simulasi", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }

                        // Search Bar and filters
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = { viewModel.setSearchQuery(it) },
                                    placeholder = { Text("Cari Resi, Nama Penerima, Barang...", fontSize = 13.sp) },
                                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp)) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("search_bar"),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                                    )
                                )

                                // Filters row
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf(
                                        "ALL" to "Semua",
                                        "DIPROSES" to "Diproses",
                                        "TERKIRIM" to "Dikirim",
                                        "DITERIMA" to "Diterima"
                                    ).forEach { (code, label) ->
                                        FilterChip(
                                            selected = filterStatus == code,
                                            onClick = { viewModel.setFilterStatus(code) },
                                            label = { Text(label, fontSize = 11.sp) },
                                            shape = RoundedCornerShape(20.dp),
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                            )
                                        )
                                    }
                                }
                            }
                        }

                        // Empty receipts placeholder
                        if (receiptsList.isEmpty()) {
                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 40.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                                        modifier = Modifier.size(64.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        "Belum Ada Resi Usaha",
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                    Text(
                                        "Gunakan tombol 'Buat Resi' di bawah untuk membuat data resi baru.",
                                        fontSize = 12.sp,
                                        textAlign = TextAlign.Center,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                        modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 4.dp)
                                    )

                                    Spacer(modifier = Modifier.height(24.dp))

                                    Button(
                                        onClick = { viewModel.resetAllLocalData() },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                                    ) {
                                        Text("Muat Ulang Sampel Resi")
                                    }
                                }
                            }
                        } else {
                            // Recipient/Shipping Item cards lists
                            items(receiptsList) { item ->
                                val isSelected = selectedReceiptNumbers.contains(item.receiptNumber)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (isSelectMode) {
                                        Checkbox(
                                            checked = isSelected,
                                            onCheckedChange = { checked ->
                                                if (checked) {
                                                    selectedReceiptNumbers.add(item.receiptNumber)
                                                } else {
                                                    selectedReceiptNumbers.remove(item.receiptNumber)
                                                }
                                            },
                                            modifier = Modifier.testTag("select_checkbox_${item.receiptNumber}")
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }

                                    Box(modifier = Modifier.weight(1f)) {
                                        ReceiptCardItem(
                                            receipt = item,
                                            currentUser = user,
                                            isDarkMode = isDarkMode,
                                            onCardClick = {
                                                if (isSelectMode) {
                                                    if (isSelected) {
                                                        selectedReceiptNumbers.remove(item.receiptNumber)
                                                    } else {
                                                        selectedReceiptNumbers.add(item.receiptNumber)
                                                    }
                                                } else {
                                                    viewModel.selectReceipt(item)
                                                }
                                            },
                                            onPrintClick = { showPrintReceipt = item },
                                            onLogUpdateClick = { showLogUpdateDialog = item }
                                        )
                                    }
                                }
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }
                }
                "Tracking" -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Lacak Resi Pengiriman",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (isDarkMode) Color.White else Color(0xFF1D2939)
                        )

                        // Card for main tracking controls
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isDarkMode) Color(0xFF1E293B) else Color.White
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Input Nomor Resi / Connote",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDarkMode) Color(0xFF10B981) else Color(0xFF1F5F4B)
                                )
                                Spacer(modifier = Modifier.height(10.dp))

                                OutlinedTextField(
                                    value = trackedReceiptNo,
                                    onValueChange = { trackedReceiptNo = it },
                                    placeholder = { Text("Contoh: REG-9472 / PL-8392", fontSize = 13.sp) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = if (isDarkMode) Color(0xFF10B981) else Color(0xFF1F5F4B),
                                        unfocusedBorderColor = if (isDarkMode) Color(0xFF334155) else Color(0xFFCBD5E1)
                                    ),
                                    trailingIcon = {
                                        if (trackedReceiptNo.isNotEmpty()) {
                                            IconButton(onClick = { trackedReceiptNo = "" }) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Clear",
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                Text(
                                    text = "Pilih Ekspedisi / Kurir",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDarkMode) Color(0xFF10B981) else Color(0xFF1F5F4B)
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                // Exp chips row with colors
                                val carriers = listOf(
                                    Pair("J&T Express", Color(0xFFEF4444)),
                                    Pair("Wahana", Color(0xFF2563EB)),
                                    Pair("Indah Cargo", Color(0xFFF97316))
                                )

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    carriers.forEach { (name, brandColor) ->
                                        val isSel = selectedCarrier == name
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(100.dp))
                                                .background(
                                                    if (isSel) brandColor.copy(alpha = 0.15f)
                                                    else (if (isDarkMode) Color(0xFF0F172A) else Color(0xFFF1F5F9))
                                                )
                                                .border(
                                                    width = if (isSel) 2.dp else 1.dp,
                                                    color = if (isSel) brandColor else (if (isDarkMode) Color(0xFF334155) else Color(0xFFE2E8F0)),
                                                    shape = RoundedCornerShape(100.dp)
                                                )
                                                .clickable { selectedCarrier = name }
                                                .padding(horizontal = 14.dp, vertical = 8.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(8.dp)
                                                        .clip(CircleShape)
                                                        .background(brandColor)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = name,
                                                    fontSize = 11.sp,
                                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (isSel) (if (isDarkMode) Color.White else brandColor) else (if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B))
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(20.dp))

                                Button(
                                    onClick = {
                                        val trimmed = trackedReceiptNo.trim()
                                        if (trimmed.isEmpty()) {
                                            Toast.makeText(context, "Silakan masukkan nomor resi terlebih dahulu!", Toast.LENGTH_SHORT).show()
                                        } else {
                                            val found = receiptsList.find { it.receiptNumber.equals(trimmed, ignoreCase = true) }
                                            if (found != null) {
                                                trackedReceiptMatch = found
                                                selectedCarrier = when {
                                                    found.courierName.contains("J&T", ignoreCase = true) -> "J&T Express"
                                                    found.courierName.contains("Wahana", ignoreCase = true) -> "Wahana"
                                                    else -> "Indah Cargo"
                                                }
                                                // Parse logs
                                                val list = mutableListOf<Triple<Long, String, String>>()
                                                try {
                                                    if (found.trackingLogsJson.isNotEmpty()) {
                                                        val arr = org.json.JSONArray(found.trackingLogsJson)
                                                        for (i in 0 until arr.length()) {
                                                            val obj = arr.getJSONObject(i)
                                                            list.add(
                                                                Triple(
                                                                    obj.getLong("timestamp"),
                                                                    obj.getString("description"),
                                                                    obj.getString("location")
                                                                )
                                                            )
                                                        }
                                                    }
                                                } catch (e: Exception) {
                                                    e.printStackTrace()
                                                }
                                                list.sortByDescending { it.first }
                                                trackResultLogs = list
                                                isTrackingSearched = true
                                            } else {
                                                // Generate mock timeline
                                                trackedReceiptMatch = null
                                                val now = System.currentTimeMillis()
                                                val generated = listOf(
                                                    Triple(now, "Paket telah berhasil diserahkan dan diterima oleh pihak penerima.", "Alamat Tujuan Penerima"),
                                                    Triple(now - 3600000 * 3, "Kurir sedang dalam perjalanan mengirimkan paket ke lokasi tujuan.", "Hub Area Tujuan"),
                                                    Triple(now - 3600000 * 8, "Paket sedang transit dan diproses di Hub Sortir utama.", "Pusat Distribusi Regional"),
                                                    Triple(now - 3600000 * 20, "Paket dikirimkan dari Hub Asal seller.", "Hub Kota Pengirim"),
                                                    Triple(now - 3600000 * 24, "Nomor resi dibuat. Paket dipersiapkan oleh pihak pengirim.", "Gudang Pengirim Asal")
                                                )
                                                trackResultLogs = generated
                                                isTrackingSearched = true
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isDarkMode) Color(0xFF10B981) else Color(0xFF1F5F4B)
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Lacak Pengiriman", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        if (isTrackingSearched && trackResultLogs != null) {
                            // Display the tracking status results
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isDarkMode) Color(0xFF1E293B) else Color.White
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    // Header
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = "Hasil Pelacakan",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.Gray
                                            )
                                            Text(
                                                text = if (trackedReceiptMatch != null) trackedReceiptMatch!!.receiptNumber else trackedReceiptNo,
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Black,
                                                color = if (isDarkMode) Color.White else Color(0xFF1D2939)
                                            )
                                        }

                                        val currentStatus = trackedReceiptMatch?.status ?: "DITERIMA"
                                        val (statusText, statusBg, statusColor) = when (currentStatus) {
                                            "DIPROSES" -> Triple("DIPROSES", if (isDarkMode) Color(0xFF78350F) else Color(0xFFFEF3C7), if (isDarkMode) Color(0xFFFBBF24) else Color(0xFFD97706))
                                            "TERKIRIM" -> Triple("TERKIRIM", if (isDarkMode) Color(0xFF1E3A8A) else Color(0xFFDBEAFE), if (isDarkMode) Color(0xFF60A5FA) else Color(0xFF2563EB))
                                            else -> Triple("DITERIMA", if (isDarkMode) Color(0xFF064E3B) else Color(0xFFD1FAE5), if (isDarkMode) Color(0xFF34D399) else Color(0xFF10B981))
                                        }

                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(statusBg)
                                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                        ) {
                                            Text(
                                                text = statusText,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = statusColor
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))
                                    Divider(color = if (isDarkMode) Color(0xFF334155) else Color(0xFFE2E8F0))
                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Shipment details
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Kurir Ekspedisi", fontSize = 11.sp, color = Color.Gray)
                                            Text(
                                                text = selectedCarrier,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isDarkMode) Color.White else Color(0xFF1D2939)
                                            )
                                        }
                                        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                                            Text("Penerima", fontSize = 11.sp, color = Color.Gray)
                                            Text(
                                                text = if (trackedReceiptMatch != null) trackedReceiptMatch!!.recipientName else "Niniemore",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isDarkMode) Color.White else Color(0xFF1D2939)
                                            )
                                        }
                                    }

                                    if (trackedReceiptMatch != null) {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text("Isi Paket", fontSize = 11.sp, color = Color.Gray)
                                                Text(
                                                    text = trackedReceiptMatch!!.itemName,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = if (isDarkMode) Color.White else Color(0xFF1D2939)
                                                )
                                            }
                                            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                                                Text("Alamat Tujuan", fontSize = 11.sp, color = Color.Gray)
                                                Text(
                                                    text = trackedReceiptMatch!!.recipientAddress,
                                                    fontSize = 12.sp,
                                                    color = Color.Gray,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "Riwayat Status / Perjalanan",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDarkMode) Color(0xFF10B981) else Color(0xFF1F5F4B)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Visual Timeline
                                    val sdf = remember { SimpleDateFormat("dd MMM, HH:mm", Locale("id", "ID")) }
                                    trackResultLogs?.forEachIndexed { idx, log ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(bottom = 12.dp),
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            // Timeline line and circle
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                modifier = Modifier.padding(horizontal = 8.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(10.dp)
                                                        .clip(CircleShape)
                                                        .background(
                                                            if (idx == 0) (if (isDarkMode) Color(0xFF10B981) else Color(0xFF1F5F4B))
                                                            else Color.Gray
                                                        )
                                                )
                                                if (idx < trackResultLogs!!.size - 1) {
                                                    Box(
                                                        modifier = Modifier
                                                            .width(2.dp)
                                                            .height(40.dp)
                                                            .background(Color.Gray.copy(alpha = 0.5f))
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.width(8.dp))

                                            Column {
                                                Text(
                                                    text = log.second,
                                                    fontSize = 12.sp,
                                                    fontWeight = if (idx == 0) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (isDarkMode) Color.White else Color(0xFF1D2939)
                                                )
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.padding(top = 2.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.LocationOn,
                                                        contentDescription = null,
                                                        tint = Color.Gray,
                                                        modifier = Modifier.size(12.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = "${log.third} • ${sdf.format(log.first)}",
                                                        fontSize = 11.sp,
                                                        color = Color.Gray
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    OutlinedButton(
                                        onClick = {
                                            isTrackingSearched = false
                                            trackResultLogs = null
                                            trackedReceiptMatch = null
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = if (isDarkMode) Color(0xFF10B981) else Color(0xFF1F5F4B)
                                        ),
                                        border = BorderStroke(
                                            1.dp,
                                            if (isDarkMode) Color(0xFF10B981) else Color(0xFF1F5F4B)
                                        )
                                    ) {
                                        Text("Ulangi Pencarian / Tutup", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        // Demo receipts quick tracker helper card
                        if (receiptsList.isNotEmpty()) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isDarkMode) Color(0xFF0F172A) else Color(0xFFF8FAFC)
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                                border = BorderStroke(
                                    1.dp,
                                    if (isDarkMode) Color(0xFF1E293B) else Color(0xFFE2E8F0)
                                )
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "Resi Bisnis Aktif (Klik untuk Lacak)",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDarkMode) Color.White else Color(0xFF1D2939)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Silakan klik nomor resi transaksi POS Anda di bawah ini untuk melihat hasil tracking log sesungguhnya:",
                                        fontSize = 11.sp,
                                        color = Color.Gray
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Sort receipts so we show the ones with content first or newest
                                    val demoList = receiptsList.take(5)
                                    demoList.forEach { rec ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable {
                                                    trackedReceiptNo = rec.receiptNumber
                                                    selectedCarrier = when {
                                                        rec.courierName.contains("J&T", ignoreCase = true) -> "J&T Express"
                                                        rec.courierName.contains("Wahana", ignoreCase = true) -> "Wahana"
                                                        else -> "Indah Cargo"
                                                    }
                                                    // Auto run track search
                                                    trackedReceiptMatch = rec
                                                    val list = mutableListOf<Triple<Long, String, String>>()
                                                    try {
                                                        if (rec.trackingLogsJson.isNotEmpty()) {
                                                            val arr = org.json.JSONArray(rec.trackingLogsJson)
                                                            for (i in 0 until arr.length()) {
                                                                val obj = arr.getJSONObject(i)
                                                                list.add(
                                                                    Triple(
                                                                        obj.getLong("timestamp"),
                                                                        obj.getString("description"),
                                                                        obj.getString("location")
                                                                    )
                                                                )
                                                            }
                                                        }
                                                    } catch (e: Exception) {
                                                        e.printStackTrace()
                                                    }
                                                    list.sortByDescending { it.first }
                                                    trackResultLogs = list
                                                    isTrackingSearched = true
                                                }
                                                .padding(vertical = 8.dp, horizontal = 10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.Send,
                                                    contentDescription = null,
                                                    tint = if (isDarkMode) Color(0xFF10B981) else Color(0xFF1F5F4B),
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Column {
                                                    Text(
                                                        text = rec.receiptNumber,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isDarkMode) Color.White else Color(0xFF1D2939)
                                                    )
                                                    Text(
                                                        text = "Pelanggan: ${rec.recipientName} • ${rec.courierName}",
                                                        fontSize = 10.sp,
                                                        color = Color.Gray
                                                    )
                                                }
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(
                                                        if (isDarkMode) Color(0xFF10B981).copy(alpha = 0.1f)
                                                        else Color(0xFF1F5F4B).copy(alpha = 0.1f)
                                                    )
                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Text(
                                                    text = "Lacak",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isDarkMode) Color(0xFF10B981) else Color(0xFF1F5F4B)
                                                )
                                            }
                                        }
                                        if (rec != demoList.last()) {
                                            Divider(color = if (isDarkMode) Color(0xFF1E293B) else Color(0xFFE2E8F0), thickness = 0.5.dp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                "Pengaturan" -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Pengaturan Aplikasi",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (isDarkMode) Color.White else Color(0xFF1D2939)
                        )

                        // 1. Theme Configuration Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isDarkMode) Color(0xFF1E293B) else Color.White
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Tampilan & Tema Utama",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDarkMode) Color(0xFF10B981) else Color(0xFF1F5F4B)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.toggleDarkMode() }
                                        .padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = null,
                                            tint = if (isDarkMode) Color(0xFFFBBF24) else Color(0xFFD97706),
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = "Mode Gelap (Dark Mode)",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (isDarkMode) Color.White else Color(0xFF1D2939)
                                            )
                                            Text(
                                                text = "Ubah tampilan menjadi gelap/terang secara instan",
                                                fontSize = 11.sp,
                                                color = Color.Gray
                                            )
                                        }
                                    }
                                    Switch(
                                        checked = isDarkMode,
                                        onCheckedChange = { viewModel.toggleDarkMode() },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color(0xFF10B981),
                                            checkedTrackColor = Color(0xFF10B981).copy(alpha = 0.5f)
                                        )
                                    )
                                }
                            }
                        }

                        // 2. Integration / Cloud Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isDarkMode) Color(0xFF1E293B) else Color.White
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Integrasi & Ekosistem",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDarkMode) Color(0xFF10B981) else Color(0xFF1F5F4B)
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { showSheetsConfigDialog = true }
                                        .padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Share,
                                            contentDescription = null,
                                            tint = if (isDarkMode) Color(0xFF10B981) else Color(0xFF1F5F4B),
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = "Atur URL Google Sheets",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (isDarkMode) Color.White else Color(0xFF1D2939)
                                            )
                                            Text(
                                                text = if (sheetsUrl.isNotEmpty()) sheetsUrl else "Sentuh untuk menghubungkan Spreadsheet",
                                                fontSize = 11.sp,
                                                color = Color.Gray,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }

                                if (syncState !is SyncState.Idle) {
                                    val syncMsgText = when (syncState) {
                                        is SyncState.Syncing -> "Sedang menyinkronkan data..."
                                        is SyncState.Success -> "Berhasil disinkronkan (${(syncState as SyncState.Success).count} resi)"
                                        is SyncState.Error -> "Gagal: ${(syncState as SyncState.Error).message}"
                                        else -> "Selesai"
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isDarkMode) Color(0xFF0F172A) else Color(0xFFF8FAFC)
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = if (isDarkMode) Color(0xFF10B981) else Color(0xFF1F5F4B),
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Sinkronisasi: $syncMsgText",
                                                fontSize = 11.sp,
                                                color = if (isDarkMode) Color.White else Color(0xFF1D2939)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // 3. System Utility / Maintenance
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isDarkMode) Color(0xFF1E293B) else Color.White
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Pemeliharaan & Data Lokal",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDarkMode) Color(0xFF10B981) else Color(0xFF1F5F4B)
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { 
                                            viewModel.resetAllLocalData() 
                                            Toast.makeText(context, "Data sampel berhasil dimuat ulang!", Toast.LENGTH_SHORT).show()
                                        }
                                        .padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = null,
                                            tint = if (isDarkMode) Color(0xFFEF4444) else Color(0xFFDC2626),
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = "Reset & Muat Ulang Sampel Resi",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (isDarkMode) Color.White else Color(0xFF1D2939)
                                            )
                                            Text(
                                                text = "Kembalikan database pos ke data demo default",
                                                fontSize = 11.sp,
                                                color = Color.Gray
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // 4. Session Informational Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isDarkMode) Color(0xFF0F172A) else Color(0xFFF1F5F9)
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Informasi Log Sesi Pengguna",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Gray
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Gray)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Nama Pengguna", fontSize = 12.sp, color = Color.Gray)
                                    }
                                    Text(user.fullName, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (isDarkMode) Color.White else Color(0xFF1D2939))
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Gray)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Akses Akun", fontSize = 12.sp, color = Color.Gray)
                                    }
                                    Text("ID: ${user.username} (${user.role.name})", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (isDarkMode) Color(0xFF10B981) else Color(0xFF1F5F4B))
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Divider(color = if (isDarkMode) Color(0xFF334155) else Color(0xFFE2E8F0))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Aplikasi Logistik v1.0.0 - AI Studio POS System",
                                    fontSize = 10.sp,
                                    color = Color.Gray,
                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                )
                            }
                        }

                        // 5. Logout Card
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.logout() }
                                .testTag("logout_card"),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isDarkMode) Color(0xFF1E293B) else Color.White
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ExitToApp,
                                        contentDescription = "Keluar Akun",
                                        tint = Color(0xFFEF4444),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "Keluar dari Akun",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFEF4444)
                                        )
                                        Text(
                                            text = "Keluar dari sesi aktif Anda sekarang",
                                            fontSize = 11.sp,
                                            color = Color.Gray
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Detail Panel Sheet Modal
    selectedReceipt?.let { receipt ->
        TrackLogsView(
            receipt = receipt,
            currentUser = user,
            onClose = { viewModel.selectReceipt(null) },
            onStatusChanged = { stat, comm ->
                viewModel.updateReceiptStatus(receipt, stat, comm)
                Toast.makeText(context, "Logistics status updated!", Toast.LENGTH_SHORT).show()
            },
            onDeleteClick = {
                viewModel.deleteReceipt(receipt)
            }
        )
    }

    // Scanner Dialog
    if (showScanDialog) {
        ScanReceiptDialog(
            onDismiss = { showScanDialog = false },
            isDarkMode = isDarkMode,
            onSubmit = { code, courier ->
                val sampleNames = listOf("Andi Wijaya", "Siti Aminah", "Budi Santoso", "Dewi Lestari", "Rian Hidayat")
                val sampleItems = listOf("Paket Pakaian Hijab", "Alat Elektronik Gaming", "Aksesoris Kosmetik", "Sepatu Olahraga Running", "Kopi Bubuk Arabika")
                val sampleAddresses = listOf("Gg. Merdeka No. 45, Coblong, Bandung, Jawa Barat", "Jl. Jenderal Sudirman No. 12, Kebayoran Baru, Jakarta Selatan", "Perum Indah Regency Blok B4, Sukolilo, Surabaya", "Jl. Kaliurang KM 5.6, Depok, Sleman, Yogyakarta", "Perum Graha Asri No. 18, Tembalang, Semarang")

                val randName = sampleNames.random()
                val randItem = sampleItems.random()
                val randAddress = sampleAddresses.random()
                val randPhone = "0812${(10000000..99999999).random()}"
                val randPrice = listOf(45000.0, 75000.0, 150000.0, 320000.0, 120000.0).random()

                viewModel.createReceipt(
                    receiptNo = code,
                    recipientName = randName,
                    recipientPhone = randPhone,
                    recipientAddress = randAddress,
                    courierName = courier,
                    itemName = randItem,
                    itemPrice = randPrice
                )
                showScanDialog = false
            }
        )
    }

    // Modal adding new receipt
    if (showAddDialog) {
        AddReceiptFormDialog(
            onDismiss = { showAddDialog = false },
            onSubmit = { receiptNo, name, phone, address, courier, item, price ->
                viewModel.createReceipt(receiptNo, name, phone, address, courier, item, price)
                showAddDialog = false
            }
        )
    }

    // Sheets URL configuration Dialog
    if (showSheetsConfigDialog) {
        GoogleSheetsConfigDialog(
            sheetsUrl = sheetsUrl,
            onDismiss = { showSheetsConfigDialog = false },
            onSave = {
                viewModel.updateSheetsUrl(it)
                showSheetsConfigDialog = false
            }
        )
    }

    // Thermal Printer emulation dialog
    showPrintReceipt?.let { receipt ->
        ThermalReceiptDialog(
            receipt = receipt,
            onDismiss = { showPrintReceipt = null }
        )
    }

    // Courier direct Status/Log checkpoint addition
    showLogUpdateDialog?.let { receipt ->
        LogUpdateFormDialog(
            receipt = receipt,
            onDismiss = { showLogUpdateDialog = null },
            onSubmit = { newStat, logText ->
                viewModel.updateReceiptStatus(receipt, newStat, logText)
                showLogUpdateDialog = null
            }
        )
    }
}

@Composable
fun ReceiptCardItem(
    receipt: Receipt,
    currentUser: User,
    isDarkMode: Boolean,
    onCardClick: () -> Unit,
    onPrintClick: () -> Unit,
    onLogUpdateClick: () -> Unit
) {
    val formatter = remember { DecimalFormat("#,###") }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCardClick() }
            .testTag("receipt_item_${receipt.receiptNumber}"),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Row 1: Receipt Num, Courier, and Statusbadge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = receipt.receiptNumber,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Kurir: ${receipt.courierName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Delivery Status Pill Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            when (receipt.status) {
                                "DIPROSES" -> if (isDarkMode) StatusPendingBgDark else StatusPendingBg
                                "TERKIRIM" -> if (isDarkMode) StatusSentBgDark else StatusSentBg
                                else -> if (isDarkMode) StatusReceivedBgDark else StatusReceivedBg
                            }
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = receipt.status,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = when (receipt.status) {
                            "DIPROSES" -> StatusPending
                            "TERKIRIM" -> StatusSent
                            else -> StatusReceived
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Row 2: Customer Name & address summary (with eye and padlock iconography)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = receipt.recipientName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Encrypted Local Data",
                            tint = StatusReceived.copy(alpha = 0.8f),
                            modifier = Modifier.size(11.dp)
                        )
                    }
                    Text(
                        text = receipt.recipientAddress,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Row 3: Product title, Price Sum
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = receipt.itemName,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 160.dp)
                    )
                }

                if (currentUser.role != UserRole.COURIER) {
                    Text(
                        text = "Rp ${formatter.format(receipt.itemPrice)}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

            Spacer(modifier = Modifier.height(10.dp))

            // Quick interaction button row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Sync status indicator
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (receipt.synced) StatusReceived else StatusPending)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (receipt.synced) "Synced ke Sheets" else "Belum Sinkron",
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Print layout trigger BUTTON (MANDATORY: TERDAPAT TOMBOL CETAK struk langsung di layar utama!)
                    Button(
                        onClick = onPrintClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                            contentColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text("CETAK", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }

                    // Courier update action trigger
                    if (currentUser.role == UserRole.COURIER || currentUser.role == UserRole.OWNER) {
                        Button(
                            onClick = onLogUpdateClick,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
                                contentColor = MaterialTheme.colorScheme.onSurface
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text("Update Log", fontSize = 10.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Logistics Timeline detailed logs panel overlay
 */
@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TrackLogsView(
    receipt: Receipt,
    currentUser: User,
    onClose: () -> Unit,
    onStatusChanged: (String, String) -> Unit,
    onDeleteClick: (() -> Unit)? = null
) {
    val logs = remember(receipt) {
        val list = mutableListOf<Triple<Long, String, String>>()
        try {
            if (receipt.trackingLogsJson.isNotEmpty()) {
                val arr = JSONArray(receipt.trackingLogsJson)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    list.add(
                        Triple(
                            obj.getLong("timestamp"),
                            obj.getString("description"),
                            obj.getString("location")
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        // Show newest logs at top
        list.sortByDescending { it.first }
        list
    }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 40.dp)
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Lacak Resi ${receipt.receiptNumber}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Ekspedisi: ${receipt.courierName} | Status: ${receipt.status}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (onDeleteClick != null && (currentUser.role == UserRole.OWNER || currentUser.role == UserRole.STAFF)) {
                            IconButton(
                                onClick = {
                                    onDeleteClick()
                                    onClose()
                                },
                                modifier = Modifier.testTag("detail_delete_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Hapus Resi",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }

                        IconButton(onClick = onClose) {
                            Icon(Icons.Default.Close, contentDescription = "Close detailed view")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Basic Receiver Info Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("INFORMASI PENGIRIMAN", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Penerima: ${receipt.recipientName}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("Telepon: (Terenkripsi) ${receipt.recipientPhone}", fontSize = 11.sp)
                        Text("Alamat: ${receipt.recipientAddress}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Barang: ${receipt.itemName} -- Rp ${DecimalFormat("#,###").format(receipt.itemPrice)}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "LOG TIMELINE LOGISTIK",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Timeline Logs List
                if (logs.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Belum ada riwayat pengiriman.",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        items(logs) { log ->
                            TimelineItemRow(
                                timestamp = log.first,
                                desc = log.second,
                                loc = log.third
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TimelineItemRow(timestamp: Long, desc: String, loc: String) {
    val fTime = remember(timestamp) {
        val sdf = SimpleDateFormat("dd MMM HH:mm", Locale.getDefault())
        sdf.format(Date(timestamp))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        // Timeline stem column
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(50.dp)
        ) {
            Text(
                text = fTime.split(" ")[0] + " " + fTime.split(" ")[1],
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = fTime.split(" ")[2],
                fontSize = 9.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                textAlign = TextAlign.Center
            )
        }

        // Dot & vertical connector
        Box(
            modifier = Modifier
                .width(20.dp)
                .fillMaxHeight(),
            contentAlignment = Alignment.TopCenter
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
        }

        // Description detail
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = desc,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Lokasi: $loc",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/**
 * Filled-Style Dialog for Inputting/Registering new shipping items (Receipts)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddReceiptFormDialog(
    onDismiss: () -> Unit,
    onSubmit: (String, String, String, String, String, String, Double) -> Unit
) {
    var receiptNoUser by remember { mutableStateOf("") }
    var recipientName by remember { mutableStateOf("") }
    var recipientPhone by remember { mutableStateOf("") }
    var recipientAddress by remember { mutableStateOf("") }
    var courierName by remember { mutableStateOf("J&T Express") }
    var itemName by remember { mutableStateOf("") }
    var itemPrice by remember { mutableStateOf("") }

    val courierOptions = listOf("J&T Express", "Wahana", "Indah Cargo")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("add_receipt_dialog"),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Buat Data Resi Usaha",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                TextField(
                    value = receiptNoUser,
                    onValueChange = { receiptNoUser = it },
                    label = { Text("Nomor Resi / Connote (Opsional)") },
                    placeholder = { Text("Contoh: REG-1234. Kosongkan untuk otomatis") },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f)
                    ),
                    shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
                    modifier = Modifier.fillMaxWidth().testTag("add_receipt_no_input")
                )

                // Filled Style Input forms
                TextField(
                    value = recipientName,
                    onValueChange = { recipientName = it },
                    label = { Text("Nama Penerima") },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f)
                    ),
                    shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
                    modifier = Modifier.fillMaxWidth().testTag("add_name_input")
                )

                TextField(
                    value = recipientPhone,
                    onValueChange = { recipientPhone = it },
                    label = { Text("No Telepon / WhatsApp") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f)
                    ),
                    shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
                    modifier = Modifier.fillMaxWidth().testTag("add_phone_input")
                )

                TextField(
                    value = recipientAddress,
                    onValueChange = { recipientAddress = it },
                    label = { Text("Alamat Pengiriman Lengkap") },
                    maxLines = 3,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f)
                    ),
                    shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
                    modifier = Modifier.fillMaxWidth().testTag("add_address_input")
                )

                TextField(
                    value = itemName,
                    onValueChange = { itemName = it },
                    label = { Text("Deskripsi Nama Barang") },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f)
                    ),
                    shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
                    modifier = Modifier.fillMaxWidth().testTag("add_item_input")
                )

                TextField(
                    value = itemPrice,
                    onValueChange = { itemPrice = it },
                    label = { Text("Harga Barang (Rp)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f)
                    ),
                    shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
                    modifier = Modifier.fillMaxWidth().testTag("add_price_input")
                )

                // Courier selection segment
                Text(
                    text = "Pilih Partner Ekspedisi Logistik:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    courierOptions.forEach { opt ->
                        val selected = courierName == opt
                        Box(
                            modifier = Modifier
                                .border(
                                    width = 1.dp,
                                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent)
                                .clickable { courierName = opt }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(opt, fontSize = 11.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Actions row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Batal")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val priceVal = itemPrice.toDoubleOrNull() ?: 0.0
                            if (recipientName.isNotEmpty() && recipientAddress.isNotEmpty() && itemName.isNotEmpty()) {
                                onSubmit(receiptNoUser, recipientName, recipientPhone, recipientAddress, courierName, itemName, priceVal)
                            }
                        },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Simpan Resi")
                    }
                }
            }
        }
    }
}

/**
 * Dialog for changing status logs (e.g. Courier logs addition)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogUpdateFormDialog(
    receipt: Receipt,
    onDismiss: () -> Unit,
    onSubmit: (String, String) -> Unit
) {
    var selectedStatus by remember { mutableStateOf(receipt.status) }
    var comment by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Update Status Resi ${receipt.receiptNumber}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Pilih Status Utama Paket:",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("DIPROSES", "TERKIRIM", "DITERIMA").forEach { stat ->
                        val selected = selectedStatus == stat
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .border(
                                    width = 1.dp,
                                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent)
                                .clickable { selectedStatus = stat }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(stat, fontSize = 11.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium)
                        }
                    }
                }

                TextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text("Keterangan Logistik (Lokasi / Catatan)") },
                    placeholder = { Text("e.g. Paket tiba di bandara sortir Jakarta.") },
                    maxLines = 2,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Batal")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val msg = comment.ifBlank {
                                when(selectedStatus) {
                                    "DIPROSES" -> "Paket sedang diproses di seller."
                                    "TERKIRIM" -> "Paket dalam perjalanan ekspedisi."
                                    else -> "Paket berhasil diserahkan kepada penerima."
                                }
                            }
                            onSubmit(selectedStatus, msg)
                        },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Kirim Pembaruan")
                    }
                }
            }
        }
    }
}

/**
 * Setup Config Dialog for Google Sheet Apps Script URL integration
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoogleSheetsConfigDialog(
    sheetsUrl: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var rawUrl by remember { mutableStateOf(sheetsUrl) }
    val clip = LocalClipboardManager.current
    val templateScript = remember { GoogleSheetsSyncHelper.getGoogleAppsScriptTemplate() }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 24.dp)
                .clip(RoundedCornerShape(20.dp)),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Sinkronisasi Google Sheets",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = "Sambungkan data resi luring langsung ke Google Sheets Anda secara real-time via REST Web App Script.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )

                TextField(
                    value = rawUrl,
                    onValueChange = { rawUrl = it },
                    label = { Text("URL Google Sheets Web App Deploy") },
                    placeholder = { Text("https://script.google.com/macros/s/.../exec") },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Batal") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { onSave(rawUrl.trim()) }, shape = RoundedCornerShape(8.dp)) {
                        Text("Simpan URL")
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 4.dp))

                Text(
                    text = "Panduan Pemasangan Google Apps Script (Instan):",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                            shape = RoundedCornerShape(10.dp)
                        )
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                            RoundedCornerShape(10.dp)
                        )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Template Apps Script (JS)",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Button(
                                onClick = {
                                    clip.setText(AnnotatedString(templateScript))
                                },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.height(24.dp)
                            ) {
                                Text("Salin Kode", fontSize = 10.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = templateScript,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Gorgeous Paper-Styled Thermal Receipt Dialog (Slip Pengiriman)
 * Features direct tactile printing integration and layout
 */
@Composable
fun ThermalReceiptDialog(
    receipt: Receipt,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sdf = remember { SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()) }
    val valFormat = remember { DecimalFormat("#,###") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Pratinjau Struk Cetak",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(14.dp))

                // The white physical simulation of thermal receipt
                Card(
                    shape = RoundedCornerShape(4.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFAFAFA)),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .weight(1f, fill = false)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(18.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Brand Mono Header
                        Text(
                            text = "== RESI USAHA PORTAL ==",
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B)
                        )
                        Text(
                            text = "Solusi Logistik Inkubator Bisnis",
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF64748B)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Custom dashed line separator
                        DashedLineDivider()

                        Spacer(modifier = Modifier.height(8.dp))

                        // Logistics Barcode shape
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .height(38.dp)
                                .drawBehind {
                                    val barCount = 38
                                    val barWidth = size.width / (barCount * 1.5f)
                                    val random = Random(receipt.receiptNumber.hashCode().toLong())
                                    var currentX = 0f
                                    for (i in 0 until barCount) {
                                        val writeBar = random.nextBoolean()
                                        if (writeBar) {
                                            drawRect(
                                                color = Color.Black,
                                                topLeft = Offset(currentX, 0f),
                                                size = Size(barWidth, size.height)
                                            )
                                        }
                                        currentX += barWidth * (if (random.nextBoolean()) 1.2f else 1.8f)
                                    }
                                }
                        )

                        Text(
                            text = "* ${receipt.receiptNumber} *",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = Color.Black,
                            modifier = Modifier.padding(top = 4.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        DashedLineDivider()

                        Spacer(modifier = Modifier.height(10.dp))

                        // Receipt key-value values
                        val lineItems = listOf(
                            "TANGGAL" to sdf.format(Date(receipt.timestamp)),
                            "KURIR" to receipt.courierName,
                            "STATUS" to receipt.status,
                            "PENERIMA" to receipt.recipientName,
                            "NO TELEPON" to receipt.recipientPhone
                        )

                        lineItems.forEach { (k, v) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    text = k,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF475569)
                                )
                                Text(
                                    text = v,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color.Black,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.weight(1f).padding(start = 16.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = "ALAMAT : ",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF475569)
                            )
                            Text(
                                text = receipt.recipientAddress,
                                fontSize = 9.5.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color.Black
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        DashedLineDivider()
                        Spacer(modifier = Modifier.height(10.dp))

                        // Item name and price
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = receipt.itemName,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "Rp ${valFormat.format(receipt.itemPrice)}",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color.Black
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        DashedLineDivider()

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "TOTAL HARGA",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.Black
                            )
                            Text(
                                text = "Rp ${valFormat.format(receipt.itemPrice)}",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.Black
                            )
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // QR representation
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .drawBehind {
                                    drawRoundRect(
                                        color = Color.Black,
                                        style = Stroke(
                                            width = 1.5.dp.toPx(),
                                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 5f), 0f)
                                        ),
                                        cornerRadius = CornerRadius(4.dp.toPx())
                                    )
                                    // Draw tiny QR modules mock
                                    val r = Random(receipt.receiptNumber.hashCode().toLong())
                                    for (x in 3..12) {
                                        for (y in 3..12) {
                                            if (r.nextBoolean()) {
                                                drawRect(
                                                    color = Color.Black,
                                                    topLeft = Offset(x * 4.dp.toPx(), y * 4.dp.toPx()),
                                                    size = Size(2.5.dp.toPx(), 2.5.dp.toPx())
                                                )
                                            }
                                        }
                                    }
                                }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Terima kasih atas kepercayaan Anda.\nKeamanan privasi data dilindungi Enkripsi AES-E2E luring.",
                            fontSize = 7.5.sp,
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.Center,
                            color = Color(0xFF64748B),
                            lineHeight = 10.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Actions Button footer
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Tutup")
                    }

                    // Direct hardware simulated print action (PDF render)
                    Button(
                        onClick = {
                            ExportHelper.exportToPdfAndPrint(context, listOf(receipt))
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Metode Cetak")
                    }
                }
            }
        }
    }
}

@Composable
fun DashedLineDivider() {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
    ) {
        drawLine(
            color = Color(0xFF94A3B8),
            start = Offset(0f, 0f),
            end = Offset(size.width, 0f),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f),
            strokeWidth = 1.dp.toPx()
        )
    }
}

@Composable
fun TrendSalesChart(
    title: String,
    days: List<String>,
    values: DoubleArray,
    selectedRange: String,
    onRangeSelected: (String) -> Unit,
    isDarkMode: Boolean
) {
    val maxVal = (values.maxOrNull() ?: 1.0).coerceAtLeast(1.0)
    val minVal = 0.0 // Let chart start at 0 for a more balanced aesthetic like the image

    // Theme adaptive colors
    val gridColor = if (isDarkMode) Color(0xFF334155).copy(alpha = 0.2f) else Color(0xFFF1F5F9)
    val labelColor = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B)
    val lineColor = if (isDarkMode) Color(0xFF10B981) else Color(0xFF0F766E) // Teal/Green line
    val gradientColor = if (isDarkMode) Color(0xFF10B981) else Color(0xFF0F766E)
    val dotBgColor = if (isDarkMode) Color(0xFF1E293B) else Color.White

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = if (isDarkMode) Color(0xFF1E293B) else Color.White,
                shape = RoundedCornerShape(24.dp)
            )
            .border(
                width = 1.dp,
                color = if (isDarkMode) Color(0xFF334155).copy(alpha = 0.5f) else Color(0xFFE2E8F0),
                shape = RoundedCornerShape(24.dp)
            )
            .padding(20.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDarkMode) Color.White else Color(0xFF1D2939),
                    modifier = Modifier.weight(1f)
                )

                var isDropdownExpanded by remember { mutableStateOf(false) }

                Box {
                    OutlinedButton(
                        onClick = { isDropdownExpanded = true },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(
                            1.dp,
                            if (isDarkMode) Color(0xFF334155) else Color(0xFFE2E8F0)
                        ),
                        modifier = Modifier
                            .height(32.dp)
                            .testTag("trend_dropdown_button")
                    ) {
                        Text(
                            text = selectedRange,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDarkMode) Color(0xFF10B981) else Color(0xFF1F5F4B)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Pilih Filter Trend",
                            tint = if (isDarkMode) Color(0xFF10B981) else Color(0xFF1F5F4B),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = isDropdownExpanded,
                        onDismissRequest = { isDropdownExpanded = false },
                        modifier = Modifier.background(
                            if (isDarkMode) Color(0xFF1D293F) else Color.White
                        )
                    ) {
                        listOf("Week", "Month", "Years").forEach { option ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = option,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (isDarkMode) Color.White else Color(0xFF1D2939)
                                    )
                                },
                                onClick = {
                                    onRangeSelected(option)
                                    isDropdownExpanded = false
                                },
                                modifier = Modifier.testTag("trend_option_$option")
                            )
                        }
                    }
                }
            }

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                val width = size.width
                val height = size.height

                val paddingLeft = 16.dp.toPx()
                val paddingRight = 16.dp.toPx()
                val paddingTop = 16.dp.toPx()
                val paddingBottom = 16.dp.toPx()

                val chartWidth = width - paddingLeft - paddingRight
                val chartHeight = height - paddingTop - paddingBottom

                // Draw horizontal grid lines (subtle)
                val gridLinesCount = 5
                for (i in 0 until gridLinesCount) {
                    val y = paddingTop + (chartHeight * i / (gridLinesCount - 1))
                    drawLine(
                        color = gridColor,
                        start = Offset(paddingLeft, y),
                        end = Offset(width - paddingRight, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                // Map values to coordinates dynamically based on actual values.size
                val points = mutableListOf<Offset>()
                val count = values.size
                val xSpacing = if (count > 1) chartWidth / (count - 1) else chartWidth
                val valueRange = maxVal - minVal
                val denom = if (valueRange == 0.0) 1.0 else valueRange

                for (i in 0 until count) {
                    val x = paddingLeft + (i * xSpacing)
                    val normY = (values[i] - minVal) / denom
                    val y = paddingTop + (chartHeight - (normY * chartHeight).toFloat())
                    points.add(Offset(x, y))
                }

                if (points.isNotEmpty()) {
                    // Draw smooth gradient fill area under the curve
                    val fillPath = Path().apply {
                        moveTo(points.first().x, height - paddingBottom)
                        lineTo(points.first().x, points.first().y)
                        
                        for (i in 0 until points.size - 1) {
                            val p0 = points[i]
                            val p1 = points[i + 1]
                            val controlPointX1 = p0.x + (p1.x - p0.x) / 2f
                            val controlPointY1 = p0.y
                            val controlPointX2 = p0.x + (p1.x - p0.x) / 2f
                            val controlPointY2 = p1.y
                            
                            cubicTo(
                                controlPointX1, controlPointY1,
                                controlPointX2, controlPointY2,
                                p1.x, p1.y
                            )
                        }
                        lineTo(points.last().x, height - paddingBottom)
                        close()
                    }
                    
                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(gradientColor.copy(alpha = 0.15f), Color.Transparent),
                            startY = paddingTop,
                            endY = height - paddingBottom
                        )
                    )

                    // Draw smooth curved line (Spline)
                    val strokePath = Path().apply {
                        moveTo(points.first().x, points.first().y)
                        for (i in 0 until points.size - 1) {
                            val p0 = points[i]
                            val p1 = points[i + 1]
                            val controlPointX1 = p0.x + (p1.x - p0.x) / 2f
                            val controlPointY1 = p0.y
                            val controlPointX2 = p0.x + (p1.x - p0.x) / 2f
                            val controlPointY2 = p1.y
                            
                            cubicTo(
                                controlPointX1, controlPointY1,
                                controlPointX2, controlPointY2,
                                p1.x, p1.y
                            )
                        }
                    }
                    
                    drawPath(
                        path = strokePath,
                        color = lineColor,
                        style = Stroke(
                            width = 3.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )

                    // Draw outlined hollow circles at each data point
                    for (point in points) {
                        drawCircle(
                            color = lineColor,
                            radius = 6.dp.toPx(),
                            center = point
                        )
                        drawCircle(
                            color = dotBgColor,
                            radius = 3.5.dp.toPx(),
                            center = point
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Draw X-axis day labels
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                days.forEach { day ->
                    Text(
                        text = day,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = labelColor
                    )
                }
            }
        }
    }
}

@OptIn(com.google.accompanist.permissions.ExperimentalPermissionsApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun ScanReceiptDialog(
    onDismiss: () -> Unit,
    onSubmit: (String, String) -> Unit,
    isDarkMode: Boolean
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    val cameraPermissionState = com.google.accompanist.permissions.rememberPermissionState(
        android.Manifest.permission.CAMERA
    )
    
    // Custom simulated keyboard tracking
    var simulatedCode by remember { mutableStateOf("") }
    var scanStatusMessage by remember { mutableStateOf("Arahkan kamera ke resi / ketik di simulator") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    val labelColor = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B)

    val regexHelper = remember {
        object {
            fun detectCourier(rawText: String): String {
                val cleanText = rawText.trim().replace("\\s+".toRegex(), "").uppercase()
                return when {
                    // J&T Express: starts with JX and followed by digits OR contains exactly 12 digits
                    cleanText.startsWith("JX") && cleanText.substring(2).all { it.isDigit() } -> "J&T Express"
                    cleanText.length == 12 && cleanText.all { it.isDigit() } -> "J&T Express"
                    
                    // Wahana: starts with W and total 8 to 12 alphanumeric
                    cleanText.startsWith("W") && cleanText.length in 8..12 && cleanText.all { it.isLetterOrDigit() } -> "Wahana"
                    
                    // Indah Cargo: 3 capital letters followed by 7 to 10 digits
                    cleanText.length in 10..13 && cleanText.take(3).all { it.isLetter() } && cleanText.drop(3).all { it.isDigit() } -> "Indah Cargo"
                    
                    else -> "J&T Express" // Default fallback
                }
            }
        }
    }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isDarkMode) Color(0xFF1E293B) else Color.White
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.PhotoCamera,
                            contentDescription = null,
                            tint = if (isDarkMode) Color(0xFF10B981) else Color(0xFF1F5F4B),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Scan Resi Otomatis",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isDarkMode) Color.White else Color(0xFF1D2939)
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Batal",
                            tint = if (isDarkMode) Color.White else Color(0xFF1D2939)
                        )
                    }
                }

                // Camera Scanner Frame
                if (cameraPermissionState.status.isGranted) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        // CameraX Preview View
                        AndroidView(
                            factory = { ctx: android.content.Context ->
                                androidx.camera.view.PreviewView(ctx).apply {
                                    scaleType = androidx.camera.view.PreviewView.ScaleType.FILL_CENTER
                                    val cameraProviderFuture = androidx.camera.lifecycle.ProcessCameraProvider.getInstance(ctx)
                                    cameraProviderFuture.addListener({
                                        try {
                                            val cameraProvider = cameraProviderFuture.get()
                                            
                                            val preview = androidx.camera.core.Preview.Builder().build().also {
                                                it.setSurfaceProvider(surfaceProvider)
                                            }
                                            
                                            val imageAnalysis = androidx.camera.core.ImageAnalysis.Builder()
                                                .setBackpressureStrategy(androidx.camera.core.ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                                .build()
                                            
                                            val executor = java.util.concurrent.Executors.newSingleThreadExecutor()
                                            val scanner = com.google.mlkit.vision.barcode.BarcodeScanning.getClient()
                                            
                                            imageAnalysis.setAnalyzer(executor) { imageProxy ->
                                                @androidx.camera.core.ExperimentalGetImage
                                                val mediaImage = imageProxy.image
                                                if (mediaImage != null) {
                                                    val image = com.google.mlkit.vision.common.InputImage.fromMediaImage(
                                                        mediaImage,
                                                        imageProxy.imageInfo.rotationDegrees
                                                    )
                                                    scanner.process(image)
                                                        .addOnSuccessListener { barcodes ->
                                                            for (barcode in barcodes) {
                                                                val rawValue = barcode.rawValue
                                                                if (!rawValue.isNullOrBlank()) {
                                                                    val detectedCourier = regexHelper.detectCourier(rawValue)
                                                                    // Return successfully
                                                                    onSubmit(rawValue, detectedCourier)
                                                                    break
                                                                }
                                                            }
                                                        }
                                                        .addOnCompleteListener {
                                                            imageProxy.close()
                                                        }
                                                } else {
                                                    imageProxy.close()
                                                }
                                            }
                                            
                                            cameraProvider.unbindAll()
                                            cameraProvider.bindToLifecycle(
                                                lifecycleOwner,
                                                androidx.camera.core.CameraSelector.DEFAULT_BACK_CAMERA,
                                                preview,
                                                imageAnalysis
                                            )
                                        } catch (e: Exception) {
                                            errorMessage = "Gagal memuat kamera: ${e.localizedMessage}"
                                        }
                                    }, androidx.core.content.ContextCompat.getMainExecutor(ctx))
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )

                        // Glowing target overlay line to simulate active laser scanning
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.dp)
                                .background(Color.Red)
                        )
                    }
                } else {
                    // Permission Request View
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .background(
                                color = if (isDarkMode) Color(0xFF0F172A) else Color(0xFFF1F5F9),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Akses Kamera Diperlukan",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDarkMode) Color.White else Color(0xFF1D2939)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Aplikasi memerlukan izin kamera untuk mendeteksi barcode.",
                            fontSize = 11.sp,
                            color = labelColor,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = { cameraPermissionState.launchPermissionRequest() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isDarkMode) Color(0xFF10B981) else Color(0xFF1F5F4B)
                            )
                        ) {
                            Text("Berikan Izin", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = Color.Red,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // SIMULATOR INTERACTIVE SECTION (Supports flawless Virtual/Emulator Demo)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = if (isDarkMode) Color(0xFF0F172A) else Color(0xFFF8FAFC),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "VIRTUAL EMULATOR TESTING BOARD",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isDarkMode) Color(0xFF34D399) else Color(0xFF1F5F4B),
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Gunakan form di bawah untuk mensimulasikan pemindaian langsung dari emulator Anda:",
                        fontSize = 10.sp,
                        color = labelColor
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextField(
                            value = simulatedCode,
                            onValueChange = { simulatedCode = it },
                            placeholder = { Text("Ketik nomor resi (misal: JX10029, RU-7629)", fontSize = 11.sp) },
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = if (isDarkMode) Color(0xFF1E293B) else Color.White,
                                unfocusedContainerColor = if (isDarkMode) Color(0xFF1E293B) else Color.White
                            ),
                            modifier = Modifier.weight(1f).height(46.dp),
                            shape = RoundedCornerShape(8.dp)
                        )
                        
                        Button(
                            onClick = {
                                if (simulatedCode.isNotBlank()) {
                                    val detectedCourier = regexHelper.detectCourier(simulatedCode)
                                    onSubmit(simulatedCode, detectedCourier)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isDarkMode) Color(0xFF10B981) else Color(0xFF1F5F4B)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(46.dp)
                        ) {
                            Text("Scan", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Format guidelines to help test
                    Text(
                        text = "FORMAT DETEKSI:\n- J&T Express: prefix JX... atau total 12 digit angka\n- Wahana: prefix W... panjang 8-12 alphanumeric\n- Indah Cargo: 3 huruf (misal IND) diikuti 7-10 digit\n- Lainnya: masuk sebagai default J&T Express",
                        fontSize = 9.sp,
                        color = labelColor.copy(alpha = 0.8f),
                        lineHeight = 13.sp
                    )
                }

                // Bottom dismiss
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Tutup", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
