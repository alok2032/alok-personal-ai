package com.ayra.ai.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.collectAsState
import com.ayra.ai.app.vpn.AyraVpnPrivacyController
import com.ayra.ai.app.vpn.WireGuardConfig
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.RecordVoiceOver
import com.ayra.ai.app.data.ChatMessage
import com.ayra.ai.app.data.ScheduledTask
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.ayra.ai.app.data.AyraPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ayra.ai.app.ai.AiModelTier
import com.ayra.ai.app.automation.DeviceAutomationController
import com.ayra.ai.app.voice.UserSentiment
import java.util.Locale
import com.ayra.ai.app.ui.theme.GlassSurfaceDark
import com.ayra.ai.app.ui.theme.NeonCyan
import com.ayra.ai.app.ui.theme.NeonGreen
import com.ayra.ai.app.ui.theme.NeonPurple
import com.ayra.ai.app.ui.theme.NeonRose
import com.ayra.ai.app.ui.theme.TextPrimary
import com.ayra.ai.app.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ControlCenterSheet(
    sheetState: SheetState,
    automationController: DeviceAutomationController,
    messageCount: Int,
    currentPersona: String = "GF_COMPANION",
    onPersonaChange: (String) -> Unit = {},
    selectedModelTier: String = "auto",
    onModelTierChange: (String) -> Unit = {},
    wakeWordEnabled: Boolean = true,
    onWakeWordToggle: (Boolean) -> Unit = {},
    onRequestAllPermissions: () -> Unit = {},
    currentSentiment: UserSentiment = UserSentiment.NEUTRAL_CARING,
    dynamicVoiceModulationEnabled: Boolean = true,
    onToggleDynamicVoiceModulation: (Boolean) -> Unit = {},
    selectedSentimentLock: UserSentiment? = null,
    onSentimentLockChange: (UserSentiment?) -> Unit = {},
    onTestSentimentVoice: (UserSentiment) -> Unit = {},
    scheduledTasks: List<ScheduledTask> = emptyList(),
    onDeleteScheduledTask: (Long) -> Unit = {},
    userMessages: List<ChatMessage> = emptyList(),
    onSelectPrompt: (String) -> Unit = {},
    onCopyPrompt: (String) -> Unit = {},
    onDismiss: () -> Unit,
    onClearHistory: () -> Unit,
    onExportHistory: () -> Unit = {},
    onQuickFeedback: (String) -> Unit
) {
    val context = LocalContext.current
    var isFlashlightOn by remember { mutableStateOf(automationController.isFlashlightActive()) }
    var lockScreenWakeEnabled by remember { mutableStateOf(true) }
    var selectedLanguage by remember { mutableStateOf("Auto") }
    var geminiKeyInput by remember { mutableStateOf(AyraPreferences.getCustomGeminiKey(context)) }
    var isKeySectionExpanded by remember { mutableStateOf(false) }
    val vpnStatus by AyraVpnPrivacyController.vpnStatus.collectAsState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF0C101B),
        scrimColor = Color.Black.copy(alpha = 0.75f),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(42.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(NeonCyan.copy(alpha = 0.6f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(listOf(NeonCyan, NeonPurple, Color.Black))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Ayra Logo",
                        tint = Color.Black,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Ayra AI • Control Center",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "সম্পূর্ণ ডিভাইস কন্ট্রোল, AI মডেল নির্বাচন ও পারসোনা",
                        style = MaterialTheme.typography.bodySmall,
                        color = NeonCyan,
                        fontSize = 11.5.sp
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("close_control_center_button")
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                }
            }

            HorizontalDivider(color = Color(0x3300FFFF), thickness = 0.8.dp)

            // Section 0: All Device Permissions Grant Card
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(GlassSurfaceDark)
                    .border(1.2.dp, NeonGreen.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
                    .clickable {
                        onRequestAllPermissions()
                        onQuickFeedback("সব পারমিশন ডায়ালগ খোলা হয়েছে 🛡️")
                    }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(NeonGreen.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Grant All Permissions / সব পারমিশন অনুমোদন",
                        color = TextPrimary,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Audio, Calls, Camera, Location, Contacts ও Alarms সক্রিয় করুন",
                        color = NeonGreen,
                        fontSize = 10.5.sp
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(NeonGreen)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(text = "GRANT", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }

            HorizontalDivider(color = Color(0x2200FFFF), thickness = 0.8.dp)

            // Section 1: AI Model Selection (Speed vs Depth of Reasoning)
            Text(
                text = "⚡ AI Model Selection / স্পিড বনাম রিজনিং ডেপথ",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = NeonCyan,
                fontSize = 13.sp
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AiModelTier.entries.forEach { tier ->
                    val isSelected = selectedModelTier == tier.id
                    val tierColor = Color(tier.accentColorHex)

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isSelected) tierColor.copy(alpha = 0.16f) else GlassSurfaceDark)
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) tierColor else Color(0x2200FFFF),
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clickable {
                                onModelTierChange(tier.id)
                                onQuickFeedback("মডেল সক্রিয়: ${tier.displayName}")
                            }
                            .padding(12.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isSelected) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                                    contentDescription = null,
                                    tint = if (isSelected) tierColor else TextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = tier.displayName,
                                        color = if (isSelected) tierColor else TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = tier.providerName,
                                        color = TextSecondary,
                                        fontSize = 10.5.sp
                                    )
                                }

                                // Badges
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xFF1E283F))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(text = tier.speedLabel, color = NeonCyan, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(tierColor.copy(alpha = 0.2f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(text = tier.reasoningLabel, color = tierColor, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            Text(
                                text = tier.description,
                                color = TextSecondary,
                                fontSize = 11.sp,
                                lineHeight = 14.sp,
                                modifier = Modifier.padding(start = 28.dp)
                            )
                        }
                    }
                }
            }
            
            // Section 1b: AI Connection & Custom API Keys
            val effectiveGeminiKey = AyraPreferences.getEffectiveGeminiKey(context)
            val isGeminiConnected = effectiveGeminiKey.isNotBlank()

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (isGeminiConnected) Color(0x1A00FF7F) else GlassSurfaceDark)
                    .border(
                        1.dp,
                        if (isGeminiConnected) NeonGreen.copy(alpha = 0.5f) else Color(0x2200FFFF),
                        RoundedCornerShape(14.dp)
                    )
                    .padding(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.VpnKey,
                            contentDescription = null,
                            tint = if (isGeminiConnected) NeonGreen else NeonCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isGeminiConnected) "🟢 Gemini AI সরাসরি কানেক্টেড" else "🌐 লাইভ ওয়েব ও অটোনোমাস ইন্টেলিজেন্স সক্রিয়",
                                color = if (isGeminiConnected) NeonGreen else NeonCyan,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.5.sp
                            )
                            Text(
                                text = if (isGeminiConnected) "সরাসরি Gemini 2.0 Flash / Pro মডেল থেকে উত্তর জেনারেট হচ্ছে" else "কোনো কী ছাড়াই যে কোনো সাধারণ জ্ঞান ও প্রশ্নের নিখুঁত উত্তর দেবে",
                                color = TextSecondary,
                                fontSize = 10.5.sp
                            )
                        }

                        Button(
                            onClick = { isKeySectionExpanded = !isKeySectionExpanded },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E283F)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text(
                                text = if (isKeySectionExpanded) "হাইড" else "কী কনফিগার",
                                color = NeonCyan,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (isKeySectionExpanded) {
                        ProviderKeysSection(
                            context = context,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }

            HorizontalDivider(color = Color(0x2200FFFF), thickness = 0.8.dp)

            // Section 1c: Cyber Shield & WireGuard VPN Suite
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🛡️ Device Privacy & Security",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan,
                        fontSize = 13.sp,
                        modifier = Modifier.weight(1f)
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(GlassSurfaceDark)
                        .border(
                            1.2.dp,
                            Color(0x2200FFFF),
                            RoundedCornerShape(14.dp)
                        )
                        .padding(14.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(NeonCyan.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    tint = NeonCyan,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Device Privacy & Security",
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp
                                )
                                Text(
                                    text = "Transparent local execution & direct TLS AI requests",
                                    color = TextSecondary,
                                    fontSize = 10.5.sp
                                )
                            }
                        }

                        Text(
                            text = "• Local Storage: Chat histories and tasks remain in on-device SQLite database.\n• ActionGate: Device calls, SMS, and alarms require your explicit tap.\n• Direct Network: AI queries travel directly to your configured provider over secure TLS.",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            lineHeight = 16.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF0F172A))
                                .padding(10.dp)
                        )
                    }
                }
            }

            HorizontalDivider(color = Color(0x2200FFFF), thickness = 0.8.dp)

            // Section 2: AI Assistant Persona Selector (GF Companion vs Normal Assistant)
            Text(
                text = "💖 AI Persona / সহকারীর আচরণ ও ব্যক্তিত্ব",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = NeonRose,
                fontSize = 13.sp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // GF Mode Card
                val isGf = currentPersona == "GF_COMPANION"
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isGf) NeonRose.copy(alpha = 0.22f) else GlassSurfaceDark)
                        .border(
                            1.2.dp,
                            if (isGf) NeonRose else Color(0x33FF007F),
                            RoundedCornerShape(14.dp)
                        )
                        .clickable {
                            onPersonaChange("GF_COMPANION")
                            onQuickFeedback("💖 Sweet GF Companion Mode সক্রিয় করা হলো!")
                        }
                        .padding(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Favorite, contentDescription = null, tint = NeonRose, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Sweet GF Mode",
                                color = if (isGf) NeonRose else TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.5.sp
                            )
                        }
                        Text(
                            text = "মিষ্টি, যত্নশীল পার্সোনাল সঙ্গী। খোঁজখবর, আবদার, কেয়ারিং কথা ও গান শোনাবে।",
                            color = TextSecondary,
                            fontSize = 10.5.sp,
                            lineHeight = 14.sp
                        )
                    }
                }

                // Normal Assistant Mode Card
                val isNormal = currentPersona == "PROFESSIONAL"
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isNormal) NeonCyan.copy(alpha = 0.22f) else GlassSurfaceDark)
                        .border(
                            1.2.dp,
                            if (isNormal) NeonCyan else Color(0x3300FFFF),
                            RoundedCornerShape(14.dp)
                        )
                        .clickable {
                            onPersonaChange("PROFESSIONAL")
                            onQuickFeedback("💼 Professional Assistant Mode সক্রিয় করা হলো!")
                        }
                        .padding(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Settings, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Normal Assistant",
                                color = if (isNormal) NeonCyan else TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.5.sp
                            )
                        }
                        Text(
                            text = "ফরমাল ও সরাসরি প্রফেশনাল সহকারী। কোডিং, গবেষণা ও টু-দ্য-পয়েন্ট উত্তর।",
                            color = TextSecondary,
                            fontSize = 10.5.sp,
                            lineHeight = 14.sp
                        )
                    }
                }
            }

            HorizontalDivider(color = Color(0x2200FFFF), thickness = 0.8.dp)

            // Dynamic Voice Tone & Sentiment Modulation Section
            Text(
                text = "🎭 Dynamic Voice Tone & Sentiment / কণ্ঠের সুর ও আবেগ রূপান্তর",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = NeonRose,
                fontSize = 13.sp
            )

            // Dynamic Tone Toggle Row
            SettingToggleRow(
                icon = Icons.Default.RecordVoiceOver,
                title = "Dynamic Voice Modulation",
                subtitle = "ইউজারের আবেগ (ভালোবাসা, কষ্ট, আনন্দ, ক্ষোভ) অনুযায়ী কণ্ঠের সুর, পিচ ও গতি রূপান্তর করবে",
                checked = dynamicVoiceModulationEnabled,
                onCheckedChange = {
                    onToggleDynamicVoiceModulation(it)
                    onQuickFeedback(if (it) "🎭 Dynamic Voice Modulation সক্রিয়" else "স্বাভাবিক কণ্ঠে স্থিতিশীল করা হলো")
                }
            )

            // Active Sentiment Tone Status Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(currentSentiment.glowColorHex).copy(alpha = 0.12f))
                    .border(
                        1.2.dp,
                        Color(currentSentiment.glowColorHex).copy(alpha = 0.6f),
                        RoundedCornerShape(14.dp)
                    )
                    .padding(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = currentSentiment.emoji, fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = currentSentiment.displayName,
                                    color = Color(currentSentiment.glowColorHex),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp
                                )
                                Text(
                                    text = currentSentiment.bengaliName,
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        // Pitch & Rate pills
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF1E283F))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "Pitch ${String.format(Locale.US, "%.2f", currentSentiment.pitch)}x",
                                    color = NeonCyan,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF1E283F))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "Rate ${String.format(Locale.US, "%.2f", currentSentiment.speechRate)}x",
                                    color = NeonRose,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Text(
                        text = currentSentiment.description,
                        color = TextSecondary,
                        fontSize = 11.5.sp,
                        lineHeight = 15.sp
                    )

                    // Test Live Voice Button
                    Button(
                        onClick = {
                            onTestSentimentVoice(currentSentiment)
                            onQuickFeedback("🔊 টেস্টিং ভয়েস: ${currentSentiment.displayName}")
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(currentSentiment.glowColorHex).copy(alpha = 0.25f)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Default.VolumeUp,
                            contentDescription = null,
                            tint = Color(currentSentiment.glowColorHex),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Test This Voice Tone (কণ্ঠে শুনে দেখুন) 🔊",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Interactive Emotion Tone Presets Grid
            Text(
                text = "🎵 Test / Lock Specific Voice Tone (ভয়েস মুড পরীক্ষা বা লক করুন):",
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )

            // Auto Reset Button if locked
            if (selectedSentimentLock != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(NeonCyan.copy(alpha = 0.2f))
                        .clickable {
                            onSentimentLockChange(null)
                            onQuickFeedback("⚡ স্বয়ংক্রিয় আবেগ সনাক্তকরণ সক্রিয় করা হলো")
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "🔒 Locked to: ${selectedSentimentLock.displayName}",
                        color = NeonCyan,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Reset to Auto Dynamic ⚡",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Tone Presets Chips
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                UserSentiment.entries.chunked(2).forEach { rowSentiments ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowSentiments.forEach { sentiment ->
                            val isSelected = (selectedSentimentLock == sentiment) || (selectedSentimentLock == null && currentSentiment == sentiment)
                            val chipColor = Color(sentiment.glowColorHex)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) chipColor.copy(alpha = 0.22f) else GlassSurfaceDark)
                                    .border(
                                        1.dp,
                                        if (isSelected) chipColor else Color(0x2200FFFF),
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable {
                                        onSentimentLockChange(sentiment)
                                        onTestSentimentVoice(sentiment)
                                        onQuickFeedback("${sentiment.emoji} ${sentiment.displayName} লক ও টেস্ট করা হলো!")
                                    }
                                    .padding(horizontal = 10.dp, vertical = 8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(text = sentiment.emoji, fontSize = 14.sp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Column {
                                            Text(
                                                text = sentiment.displayName.substringBefore(" &"),
                                                color = if (isSelected) chipColor else TextPrimary,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "${String.format(Locale.US, "%.2f", sentiment.pitch)}x",
                                                color = TextSecondary,
                                                fontSize = 9.sp
                                            )
                                        }
                                    }
                                    Icon(
                                        Icons.Default.VolumeUp,
                                        contentDescription = "Test",
                                        tint = if (isSelected) chipColor else TextSecondary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = Color(0x2200FFFF), thickness = 0.8.dp)

            // Section 3: Instant Phone Controls
            Text(
                text = "⚡ Instant Phone Controls / সরাসরি মোবাইল কন্ট্রোল",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = NeonCyan,
                fontSize = 13.sp
            )

            // Control Buttons Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Flashlight Button
                QuickControlCard(
                    modifier = Modifier.weight(1f),
                    icon = if (isFlashlightOn) Icons.Default.FlashlightOn else Icons.Default.FlashlightOff,
                    title = "Flashlight",
                    status = if (isFlashlightOn) "ON 🔦" else "OFF",
                    isActive = isFlashlightOn,
                    activeColor = Color(0xFFFFD700),
                    onClick = {
                        val newState = !isFlashlightOn
                        automationController.toggleFlashlight(newState)
                        isFlashlightOn = newState
                        onQuickFeedback(if (newState) "🔦 Flashlight turned ON" else "🔦 Flashlight turned OFF")
                    }
                )

                // Volume Max Button
                QuickControlCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.VolumeUp,
                    title = "Max Sound",
                    status = "100% 🔊",
                    isActive = false,
                    activeColor = NeonCyan,
                    onClick = {
                        val msg = automationController.maxVolume()
                        onQuickFeedback(msg)
                    }
                )

                // Mute Sound Button
                QuickControlCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.VolumeMute,
                    title = "Mute Sound",
                    status = "Mute 🔇",
                    isActive = false,
                    activeColor = NeonRose,
                    onClick = {
                        val msg = automationController.muteVolume()
                        onQuickFeedback(msg)
                    }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Wi-Fi Shortcut
                QuickControlCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Wifi,
                    title = "Wi-Fi",
                    status = "Settings 📶",
                    isActive = false,
                    activeColor = NeonCyan,
                    onClick = {
                        automationController.openWifiSettings()
                        onQuickFeedback("📶 Opening Wi-Fi settings...")
                    }
                )

                // Bluetooth Shortcut
                QuickControlCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Bluetooth,
                    title = "Bluetooth",
                    status = "Settings ᛒ",
                    isActive = false,
                    activeColor = NeonPurple,
                    onClick = {
                        automationController.openBluetoothSettings()
                        onQuickFeedback("ᛒ Opening Bluetooth settings...")
                    }
                )

                // Camera Shortcut
                QuickControlCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.CameraAlt,
                    title = "Camera",
                    status = "Photo 📸",
                    isActive = false,
                    activeColor = NeonGreen,
                    onClick = {
                        automationController.openCamera()
                        onQuickFeedback("📸 Launching Camera...")
                    }
                )
            }

            // Section 3b: Perfectly Aligned Phone App Launcher Grid
            Text(
                text = "📱 Quick App Launcher / ফোন অ্যাপ লঞ্চার",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = NeonCyan,
                fontSize = 12.sp
            )

            // 4-column balanced grid for common installed phone apps
            val popularApps = listOf(
                Triple("YouTube", "▶️", "youtube"),
                Triple("WhatsApp", "💬", "whatsapp"),
                Triple("Facebook", "📘", "facebook"),
                Triple("Instagram", "📷", "instagram"),
                Triple("Spotify", "🎵", "spotify"),
                Triple("Chrome", "🌐", "chrome"),
                Triple("Maps", "🗺️", "maps"),
                Triple("Settings", "⚙️", "settings")
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                popularApps.chunked(4).forEach { rowApps ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowApps.forEach { (appName, emoji, commandKey) ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .defaultMinSize(minHeight = 70.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF131B2E))
                                    .border(1.dp, Color(0x3300FFFF), RoundedCornerShape(12.dp))
                                    .clickable {
                                        if (commandKey == "settings") {
                                            automationController.openSettings()
                                            onQuickFeedback("⚙️ Opening Settings...")
                                        } else {
                                            automationController.tryExecuteAutomation("open $commandKey")
                                            onQuickFeedback("🚀 Opening $appName...")
                                        }
                                    }
                                    .padding(vertical = 10.dp, horizontal = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(text = emoji, fontSize = 22.sp)
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = appName,
                                        color = TextPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Volume Fine Tuning
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(GlassSurfaceDark)
                    .border(1.dp, NeonCyan.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Media Volume Adjustment",
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = {
                            val res = automationController.adjustVolume(false)
                            onQuickFeedback(res)
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1E283F))
                    ) {
                        Icon(Icons.Default.VolumeDown, contentDescription = "Down", tint = NeonCyan, modifier = Modifier.size(18.dp))
                    }
                    IconButton(
                        onClick = {
                            val res = automationController.adjustVolume(true)
                            onQuickFeedback(res)
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1E283F))
                    ) {
                        Icon(Icons.Default.VolumeUp, contentDescription = "Up", tint = NeonCyan, modifier = Modifier.size(18.dp))
                    }
                }
            }

            HorizontalDivider(color = Color(0x2200FFFF), thickness = 0.8.dp)

            // Section 4: Voice & Lock Screen Automation
            Text(
                text = "🎙️ Voice Wake & Lock Screen / লক স্ক্রিন ও ভয়েস সেটিংস",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = NeonCyan,
                fontSize = 13.sp
            )

            // Lock Screen Wake Toggle
            SettingToggleRow(
                icon = Icons.Default.Lock,
                title = "Lock Screen Wake ('Ayra')",
                subtitle = "ফোন লক থাকলেও 'আয়রা' বা 'Ayra' বললেই সহকারী অন হবে",
                checked = lockScreenWakeEnabled,
                onCheckedChange = {
                    lockScreenWakeEnabled = it
                    onQuickFeedback(if (it) "Lock Screen Wake Enabled 🔓" else "Lock Screen Wake Disabled")
                }
            )

            // Wake Word Detection Toggle
            SettingToggleRow(
                icon = Icons.Default.Mic,
                title = "Always-On Voice Trigger",
                subtitle = "গুগল বা সিরি-র মত 'Ayra' ডাকলে স্বয়ংক্রিয় সক্রিয়",
                checked = wakeWordEnabled,
                onCheckedChange = {
                    onWakeWordToggle(it)
                    onQuickFeedback(if (it) "Voice Trigger Enabled 🎙️" else "Voice Trigger Disabled")
                }
            )

            HorizontalDivider(color = Color(0x2200FFFF), thickness = 0.8.dp)

            // Section 5: Multilingual Voice & AI Companion
            Text(
                text = "🗣️ Voice Language / কথা বলার ভাষা",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = NeonCyan,
                fontSize = 13.sp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "Auto" to "🌐 Auto",
                    "bn" to "বাংলা",
                    "hi" to "हिन्दी",
                    "en" to "English"
                ).forEach { (code, label) ->
                    val isSelected = selectedLanguage == code
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) NeonCyan.copy(alpha = 0.25f) else Color(0xFF131B2E))
                            .border(
                                1.dp,
                                if (isSelected) NeonCyan else Color(0x2200FFFF),
                                RoundedCornerShape(10.dp)
                            )
                            .clickable {
                                selectedLanguage = code
                                onQuickFeedback("Language preference: $label")
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) NeonCyan else TextSecondary,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            HorizontalDivider(color = Color(0x2200FFFF), thickness = 0.8.dp)

            // Section 5b: Autonomous Background Routines & Scheduled Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "⏰ Autonomous Scheduled Routines (${scheduledTasks.size})",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan,
                    fontSize = 13.sp
                )
                Text(
                    text = "AlarmManager RTC",
                    color = NeonGreen,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (scheduledTasks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(GlassSurfaceDark)
                        .border(1.dp, Color(0x2200FFFF), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "কোনো অ্যাক্টিভ শিডিউল নেই।",
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "💡 আপনি সরাসরি বলতে বা টাইপ করতে পারেন: \"রাত ১২টায় রাহুলকে হোয়াটসঅ্যাপে শুভ জন্মদিন উইশ করো\" — ফোন লক/স্লিপ থাকলেও Ayra সময়মতো পাঠিয়ে দেবে!",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    scheduledTasks.forEach { task ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(GlassSurfaceDark)
                                .border(
                                    1.dp,
                                    if (task.status == "PENDING") NeonCyan.copy(alpha = 0.6f) else NeonGreen.copy(alpha = 0.6f),
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = if (task.status == "PENDING") NeonCyan else NeonGreen,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = task.title,
                                        color = TextPrimary,
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color(0xFF1E293B))
                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                    ) {
                                        Text(
                                            text = task.targetApp,
                                            color = NeonCyan,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Text(
                                    text = "সময়: ${task.formattedTime} • স্ট্যাটাস: ${task.status}",
                                    color = if (task.status == "PENDING") NeonCyan else NeonGreen,
                                    fontSize = 10.5.sp
                                )
                                Text(
                                    text = task.messageText,
                                    color = TextSecondary,
                                    fontSize = 10.sp,
                                    maxLines = 1
                                )
                            }
                            IconButton(
                                onClick = { onDeleteScheduledTask(task.id) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Cancel Task",
                                    tint = NeonRose,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = Color(0x2200FFFF), thickness = 0.8.dp)

            // Section 5c: Search & Prompt History
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "🔍 Search & Prompt History (${userMessages.size})",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan,
                    fontSize = 13.sp
                )
                if (userMessages.isNotEmpty()) {
                    Text(
                        text = "Tap to re-run",
                        color = TextSecondary,
                        fontSize = 10.sp
                    )
                }
            }

            if (userMessages.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(GlassSurfaceDark)
                        .border(1.dp, Color(0x2200FFFF), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = "এখনো কোনো অতীত প্রশ্ন বা প্রম্পট সেভ করা নেই।",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    userMessages.takeLast(8).reversed().forEach { msg ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(GlassSurfaceDark)
                                .border(0.8.dp, Color(0x1F00FFFF), RoundedCornerShape(10.dp))
                                .clickable { onSelectPrompt(msg.content) }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = msg.content,
                                color = TextPrimary,
                                fontSize = 11.5.sp,
                                maxLines = 1,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { onCopyPrompt(msg.content) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy prompt",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            IconButton(
                                onClick = { onSelectPrompt(msg.content) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Re-run prompt",
                                    tint = NeonGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = Color(0x2200FFFF), thickness = 0.8.dp)

            // Section 6: Phone Storage & Data
            Text(
                text = "💾 Storage & Privacy / মোবাইলের মেমোরি ও ডেটা",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = NeonCyan,
                fontSize = 13.sp
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(GlassSurfaceDark)
                    .border(1.dp, Color(0x2200FFFF), RoundedCornerShape(12.dp))
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Storage, contentDescription = null, tint = NeonPurple)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Local SQLite Storage: $messageCount messages saved",
                        color = TextPrimary,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Phone Storage: /data/user/0/com.ayra.ai.app/databases/",
                        color = TextSecondary,
                        fontSize = 10.5.sp
                    )
                }
                IconButton(
                    onClick = onExportHistory,
                    modifier = Modifier.testTag("export_history_in_settings")
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Export History to Storage", tint = NeonCyan)
                }
                IconButton(
                    onClick = onClearHistory,
                    modifier = Modifier.testTag("clear_history_in_settings")
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Clear", tint = NeonRose)
                }
            }

            Spacer(modifier = Modifier.height(18.dp))
        }
    }
}

@Composable
private fun QuickControlCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    status: String,
    isActive: Boolean,
    activeColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (isActive) activeColor.copy(alpha = 0.2f) else GlassSurfaceDark)
            .border(
                1.dp,
                if (isActive) activeColor else Color(0x2200FFFF),
                RoundedCornerShape(14.dp)
            )
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (isActive) activeColor else NeonCyan,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = status,
                color = if (isActive) activeColor else TextSecondary,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun SettingToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(GlassSurfaceDark)
            .border(1.dp, Color(0x2200FFFF), RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (checked) NeonCyan else TextSecondary,
            modifier = Modifier.size(22.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                color = TextSecondary,
                fontSize = 11.sp
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.Black,
                checkedTrackColor = NeonCyan,
                uncheckedThumbColor = TextSecondary,
                uncheckedTrackColor = Color(0xFF1E283F)
            )
        )
    }
}
