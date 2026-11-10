package com.ayra.ai.app.ui.components

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ayra.ai.app.ai.providers.AiProviderType
import com.ayra.ai.app.ai.providers.ExternalAiProviders
import com.ayra.ai.app.data.AyraPreferences
import com.ayra.ai.app.search.TavilySearchEngine
import com.ayra.ai.app.voice.ElevenLabsVoiceEngine
import kotlinx.coroutines.launch

@Composable
fun ProviderKeysSection(
    context: Context,
    modifier: Modifier = Modifier
) {
    val providers = remember {
        listOf(
            Triple(AiProviderType.GEMINI, "Google AI Studio", "https://aistudio.google.com/app/apikey"),
            Triple(AiProviderType.OPENAI, "OpenAI API", "https://platform.openai.com/api-keys"),
            Triple(AiProviderType.ANTHROPIC, "Anthropic Console", "https://console.anthropic.com/settings/keys"),
            Triple(AiProviderType.DEEPSEEK, "DeepSeek Platform", "https://platform.deepseek.com/api_keys"),
            Triple(AiProviderType.OPENROUTER, "OpenRouter (All Models)", "https://openrouter.ai/keys"),
            Triple(AiProviderType.XAI, "xAI Console", "https://console.x.ai/"),
            Triple(AiProviderType.MISTRAL, "Mistral Console", "https://console.mistral.ai/api-keys/"),
            Triple(AiProviderType.PERPLEXITY, "Perplexity AI", "https://www.perplexity.ai/settings/api"),
            Triple(AiProviderType.LOCAL_OLLAMA, "Local Ollama / LM Studio", "http://localhost:11434")
        )
    }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(
            text = "AI Providers & API Keys",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "All API keys are encrypted on-device via Android Keystore (AES-256-GCM). They are never sent to third-party telemetry servers or stored in plaintext.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Intelligent Routing & AI Memory Control
        AiRoutingAndMemoryCard(context = context)

        Text(
            text = "Core LLM Providers",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 4.dp)
        )

        providers.forEach { (provider, label, portalUrl) ->
            SingleProviderKeyCard(
                context = context,
                provider = provider,
                portalLabel = label,
                portalUrl = portalUrl
            )
        }

        Text(
            text = "Search & Voice Integrations",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 8.dp)
        )

        // Tavily Live Web Search
        SpecializedIntegrationKeyCard(
            context = context,
            title = "Tavily Search Engine",
            description = "Provides live web grounding with real citations for news, facts, and research.",
            portalLabel = "Tavily Platform",
            portalUrl = "https://app.tavily.com",
            icon = Icons.Default.Language,
            initialKey = AyraPreferences.getTavilySearchKey(context),
            placeholder = "tvly-...",
            onSave = { AyraPreferences.setTavilySearchKey(context, it) },
            onClear = { AyraPreferences.setTavilySearchKey(context, "") },
            onTest = { key -> TavilySearchEngine(context).testConnection(key) }
        )

        // ElevenLabs Neural Voice
        SpecializedIntegrationKeyCard(
            context = context,
            title = "ElevenLabs Neural Voice",
            description = "High-fidelity AI voice synthesis with natural human cadence and tone.",
            portalLabel = "ElevenLabs Console",
            portalUrl = "https://elevenlabs.io",
            icon = Icons.Default.RecordVoiceOver,
            initialKey = AyraPreferences.getElevenLabsKey(context),
            placeholder = "sk_...",
            onSave = { AyraPreferences.setElevenLabsKey(context, it) },
            onClear = { AyraPreferences.setElevenLabsKey(context, "") },
            onTest = { key -> ElevenLabsVoiceEngine(context).testConnection(key) }
        )

        // Qwen 3.8 27B Uncensored (from Wassimyounes01/qwen38-uncensored)
        SpecializedIntegrationKeyCard(
            context = context,
            title = "Qwen 3.8 27B Uncensored (Abliterated)",
            description = "Weight-level abliterated 27B weights from Wassimyounes01/qwen38-uncensored. Answers all world questions without refusal.",
            portalLabel = "HuggingFace GGUF",
            portalUrl = "https://huggingface.co/orcarouter/Qwen3.8-27B-Uncensored-GGUF",
            icon = Icons.Default.AutoAwesome,
            initialKey = AyraPreferences.getCustomEndpointUrl(context),
            placeholder = "http://10.0.2.2:11434 (Local Ollama / LAN)",
            onSave = { AyraPreferences.setCustomEndpointUrl(context, it) },
            onClear = { AyraPreferences.setCustomEndpointUrl(context, "") },
            onTest = { endpoint -> com.ayra.ai.app.ai.qwen.Qwen38UncensoredEngine(context).testConnection(endpoint) }
        )
    }
}

@Composable
fun AiRoutingAndMemoryCard(context: Context) {
    var selectedMode by remember { mutableStateOf(AyraPreferences.getRoutingMode(context)) }
    var isMemoryEnabled by remember { mutableStateOf(AyraPreferences.isMemoryEnabled(context)) }
    var userInstructions by remember { mutableStateOf(AyraPreferences.getUserCustomInstructions(context)) }
    var rememberedFactsCount by remember { mutableStateOf(AyraPreferences.getRememberedFacts(context).size) }
    var statusMessage by remember { mutableStateOf("") }

    val routingModes = remember {
        listOf(
            Triple("auto", "Auto ⚡", "Adaptive selection"),
            Triple("quality", "Best Quality 👑", "Gemini 2.5 Pro / Claude 3.7"),
            Triple("fast", "Ultra Fast 🚀", "Gemini 2.5 Flash / 4o Mini"),
            Triple("qwen_uncensored", "Qwen 3.8 🔓", "Abliterated 27B — Zero Refusals"),
            Triple("reasoning", "Deep Reasoning 🧠", "DeepSeek R1 / o3-mini"),
            Triple("coding", "Coding & STEM 💻", "Claude 3.7 / Codestral"),
            Triple("local", "Private LAN 🏠", "Local Ollama / LM Studio")
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "⚡ Intelligent Routing & Memory Controls",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = "Active Routing Mode:",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                routingModes.take(3).forEach { (id, label, _) ->
                    val isSelected = selectedMode == id
                    OutlinedButton(
                        onClick = {
                            selectedMode = id
                            AyraPreferences.setRoutingMode(context, id)
                            statusMessage = "Routing set to $label ✓"
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = if (isSelected) ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer) else ButtonDefaults.outlinedButtonColors(),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(label, fontSize = 11.sp, maxLines = 1)
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                routingModes.drop(3).forEach { (id, label, _) ->
                    val isSelected = selectedMode == id
                    OutlinedButton(
                        onClick = {
                            selectedMode = id
                            AyraPreferences.setRoutingMode(context, id)
                            statusMessage = "Routing set to $label ✓"
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = if (isSelected) ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer) else ButtonDefaults.outlinedButtonColors(),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(label, fontSize = 11.sp, maxLines = 1)
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // AI Memory Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "🧠 Autonomous AI Memory",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (isMemoryEnabled) "Active ($rememberedFactsCount facts remembered)" else "Disabled (Privacy mode)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (rememberedFactsCount > 0) {
                        TextButton(
                            onClick = {
                                AyraPreferences.clearAllMemory(context)
                                rememberedFactsCount = 0
                                statusMessage = "All AI memory wiped clean ✓"
                            }
                        ) {
                            Text("Wipe", color = Color(0xFFFF5252), fontSize = 11.sp)
                        }
                    }

                    Button(
                        onClick = {
                            isMemoryEnabled = !isMemoryEnabled
                            AyraPreferences.setMemoryEnabled(context, isMemoryEnabled)
                            statusMessage = if (isMemoryEnabled) "AI Memory Enabled ✓" else "AI Memory Disabled ✓"
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isMemoryEnabled) MaterialTheme.colorScheme.primary else Color.Gray
                        )
                    ) {
                        Text(if (isMemoryEnabled) "Enabled" else "Disabled", fontSize = 11.sp)
                    }
                }
            }

            // Custom Instructions Input
            OutlinedTextField(
                value = userInstructions,
                onValueChange = {
                    userInstructions = it
                    AyraPreferences.setUserCustomInstructions(context, it)
                },
                label = { Text("Custom Instructions for Ayra (optional)", fontSize = 11.sp) },
                placeholder = { Text("e.g. Always respond in Bengali; prioritize concise code", fontSize = 11.sp) },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 2,
                textStyle = MaterialTheme.typography.bodySmall
            )

            if (statusMessage.isNotBlank()) {
                Text(
                    text = statusMessage,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun SingleProviderKeyCard(
    context: Context,
    provider: AiProviderType,
    portalLabel: String,
    portalUrl: String
) {
    val providersEngine = remember { ExternalAiProviders(context) }
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current

    var keyInput by remember {
        mutableStateOf(
            if (provider == AiProviderType.LOCAL_OLLAMA) {
                val url = AyraPreferences.getCustomEndpointUrl(context)
                if (url.isNotBlank()) url else providersEngine.getProviderKey(provider)
            } else {
                providersEngine.getProviderKey(provider)
            }
        )
    }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isTesting by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }

    val isConfigured = keyInput.isNotBlank() || (provider == AiProviderType.LOCAL_OLLAMA && AyraPreferences.getCustomEndpointUrl(context).isNotBlank())

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(
                if (isConfigured) Color(0xFF10B981).copy(alpha = 0.4f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
            )
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Key,
                        contentDescription = null,
                        tint = if (isConfigured) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = provider.displayName,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isConfigured) Color(0xFF10B981).copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = if (isConfigured) "Configured ✓" else "Not set",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = if (isConfigured) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = keyInput,
                onValueChange = { keyInput = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        if (provider == AiProviderType.LOCAL_OLLAMA) "http://10.0.2.2:11434/v1" else "Enter ${provider.displayName} API Key",
                        style = MaterialTheme.typography.bodySmall
                    )
                },
                singleLine = true,
                visualTransformation = if (isPasswordVisible || provider == AiProviderType.LOCAL_OLLAMA) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    if (provider != AiProviderType.LOCAL_OLLAMA) {
                        IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                            Icon(
                                imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Toggle visibility",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                ),
                textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
            )

            Text(
                text = "Get key at: $portalLabel ($portalUrl)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 4.dp, start = 4.dp)
            )

            AnimatedVisibility(visible = testResult != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .background(
                            if (isError) Color(0xFFEF4444).copy(alpha = 0.1f) else Color(0xFF10B981).copy(alpha = 0.1f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isError) Icons.Default.ErrorOutline else Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = if (isError) Color(0xFFEF4444) else Color(0xFF10B981),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = testResult ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isError) Color(0xFFEF4444) else Color(0xFF10B981)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = {
                        clipboardManager.getText()?.text?.let {
                            if (it.isNotBlank()) keyInput = it.trim()
                        }
                    }
                ) {
                    Text("Paste", style = MaterialTheme.typography.labelMedium)
                }

                if (keyInput.isNotBlank()) {
                    TextButton(
                        onClick = {
                            keyInput = ""
                            saveKey(context, provider, "")
                            testResult = null
                        }
                    ) {
                        Text("Clear", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error)
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                OutlinedButton(
                    onClick = {
                        scope.launch {
                            isTesting = true
                            testResult = null
                            isError = false
                            try {
                                val latency = providersEngine.testProviderConnection(provider, keyInput.trim())
                                testResult = "✓ Connected successfully in ${latency}ms!"
                                isError = false
                            } catch (e: Exception) {
                                testResult = "Connection failed: ${e.message}"
                                isError = true
                            } finally {
                                isTesting = false
                            }
                        }
                    },
                    enabled = !isTesting && (keyInput.isNotBlank() || provider == AiProviderType.LOCAL_OLLAMA),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    if (isTesting) {
                        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text("Test", style = MaterialTheme.typography.labelMedium)
                }

                Spacer(modifier = Modifier.width(6.dp))

                Button(
                    onClick = {
                        saveKey(context, provider, keyInput.trim())
                        testResult = "Key saved securely to Android Keystore ✓"
                        isError = false
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Save", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
fun SpecializedIntegrationKeyCard(
    context: Context,
    title: String,
    description: String,
    portalLabel: String,
    portalUrl: String,
    icon: ImageVector,
    initialKey: String,
    placeholder: String,
    onSave: (String) -> Unit,
    onClear: () -> Unit,
    onTest: suspend (String) -> Long
) {
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current

    var keyInput by remember { mutableStateOf(initialKey) }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isTesting by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }

    val isConfigured = keyInput.isNotBlank()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(
                if (isConfigured) Color(0xFF10B981).copy(alpha = 0.4f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
            )
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isConfigured) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isConfigured) Color(0xFF10B981).copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = if (isConfigured) "Configured ✓" else "Not set",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = if (isConfigured) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = keyInput,
                onValueChange = { keyInput = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(placeholder, style = MaterialTheme.typography.bodySmall) },
                singleLine = true,
                visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                        Icon(
                            imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = "Toggle visibility",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                ),
                textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
            )

            Text(
                text = "Get key at: $portalLabel ($portalUrl)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 4.dp, start = 4.dp)
            )

            AnimatedVisibility(visible = testResult != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .background(
                            if (isError) Color(0xFFEF4444).copy(alpha = 0.1f) else Color(0xFF10B981).copy(alpha = 0.1f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isError) Icons.Default.ErrorOutline else Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = if (isError) Color(0xFFEF4444) else Color(0xFF10B981),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = testResult ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isError) Color(0xFFEF4444) else Color(0xFF10B981)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = {
                        clipboardManager.getText()?.text?.let {
                            if (it.isNotBlank()) keyInput = it.trim()
                        }
                    }
                ) {
                    Text("Paste", style = MaterialTheme.typography.labelMedium)
                }

                if (keyInput.isNotBlank()) {
                    TextButton(
                        onClick = {
                            keyInput = ""
                            onClear()
                            testResult = null
                        }
                    ) {
                        Text("Clear", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error)
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                OutlinedButton(
                    onClick = {
                        scope.launch {
                            isTesting = true
                            testResult = null
                            isError = false
                            try {
                                val latency = onTest(keyInput.trim())
                                testResult = "✓ Connected successfully in ${latency}ms!"
                                isError = false
                            } catch (e: Exception) {
                                testResult = "Connection failed: ${e.message}"
                                isError = true
                            } finally {
                                isTesting = false
                            }
                        }
                    },
                    enabled = !isTesting && keyInput.isNotBlank(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    if (isTesting) {
                        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text("Test", style = MaterialTheme.typography.labelMedium)
                }

                Spacer(modifier = Modifier.width(6.dp))

                Button(
                    onClick = {
                        onSave(keyInput.trim())
                        testResult = "Key saved securely to Android Keystore ✓"
                        isError = false
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Save", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

private fun saveKey(context: Context, provider: AiProviderType, key: String) {
    when (provider) {
        AiProviderType.GEMINI -> AyraPreferences.setCustomGeminiKey(context, key)
        AiProviderType.OPENAI -> AyraPreferences.setOpenAiKey(context, key)
        AiProviderType.ANTHROPIC -> AyraPreferences.setAnthropicKey(context, key)
        AiProviderType.DEEPSEEK -> AyraPreferences.setDeepSeekKey(context, key)
        AiProviderType.XAI -> AyraPreferences.setXAiKey(context, key)
        AiProviderType.MISTRAL -> AyraPreferences.setMistralKey(context, key)
        AiProviderType.PERPLEXITY -> AyraPreferences.setPerplexityKey(context, key)
        AiProviderType.OPENROUTER -> AyraPreferences.setCustomOpenRouterKey(context, key)
        AiProviderType.LOCAL_OLLAMA -> AyraPreferences.setCustomEndpointUrl(context, key)
        AiProviderType.KEYLESS_COMMUNITY -> {}
    }
}
