package com.ayra.ai.app.ui.screens

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.material.icons.filled.Code
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.platform.testTag
import com.ayra.ai.app.ui.components.ControlCenterSheet
import com.ayra.ai.app.vpn.AyraVpnPrivacyController
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import com.ayra.ai.app.ai.UnifiedAiEngine
import com.ayra.ai.app.automation.AyraTaskScheduler
import com.ayra.ai.app.automation.DeviceAutomationController
import com.ayra.ai.app.automation.ProposedAction
import com.ayra.ai.app.ui.components.ActionConfirmationCard
import com.ayra.ai.app.voice.CrisisDetector
import com.ayra.ai.app.data.ChatDao
import com.ayra.ai.app.data.ChatDatabase
import com.ayra.ai.app.data.ChatMessage
import org.json.JSONObject
import com.ayra.ai.app.ui.components.AmbientCornerGlow
import com.ayra.ai.app.ui.components.SolarSystem3DCanvas
import com.ayra.ai.app.ui.components.SongPlayerCard
import com.ayra.ai.app.ui.components.ThreeDModelCard
import com.ayra.ai.app.ui.components.VideoPlayerCard
import com.ayra.ai.app.ui.theme.CosmicBackground
import com.ayra.ai.app.ui.theme.GlassSurfaceDark
import com.ayra.ai.app.ui.theme.NeonCyan
import com.ayra.ai.app.ui.theme.NeonGreen
import com.ayra.ai.app.ui.theme.NeonGold
import com.ayra.ai.app.ui.theme.NeonPurple
import com.ayra.ai.app.ui.theme.NeonRose
import com.ayra.ai.app.ui.theme.TextPrimary
import com.ayra.ai.app.ui.theme.TextSecondary
import com.ayra.ai.app.ui.theme.UserBubble
import com.ayra.ai.app.voice.SentimentDetector
import com.ayra.ai.app.voice.UserSentiment
import com.ayra.ai.app.voice.VoiceAssistantManager
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    chatDao: ChatDao,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val messages by chatDao.getAllMessages().collectAsState(initial = emptyList())
    val automationController = remember { DeviceAutomationController(context) }
    val aiEngine = remember { UnifiedAiEngine(context) }
    val db = remember { ChatDatabase.getDatabase(context) }
    val taskScheduler = remember { AyraTaskScheduler(context) }
    val scheduledTasks by db.scheduledTaskDao().getAllTasks().collectAsState(initial = emptyList())

    var currentPersona by remember { mutableStateOf("AI_ASSISTANT") }
    var selectedModelTier by remember { mutableStateOf("auto") }
    var inputText by remember { mutableStateOf("") }
    var isGenerating by remember { mutableStateOf(false) }
    var pendingAction by remember { mutableStateOf<ProposedAction?>(null) }
    var showClearChatDialog by remember { mutableStateOf(false) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var showAttachmentPicker by remember { mutableStateOf(false) }
    var showControlCenterSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var expandedImageUri by remember { mutableStateOf<String?>(null) }
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }
    var selectedDocumentUri by remember { mutableStateOf<Uri?>(null) }
    var selectedDocumentName by remember { mutableStateOf<String?>(null) }
    var selectedDocumentText by remember { mutableStateOf<String?>(null) }
    var selectedDocumentBase64Image by remember { mutableStateOf<String?>(null) }

    AyraVpnPrivacyController.initialize(context)
    val vpnStatus by AyraVpnPrivacyController.vpnStatus.collectAsState()

    var dispatchPromptRef by remember { mutableStateOf<((String) -> Unit)?>(null) }

    // Voice Assistant Manager setup with instant execution & real-time voice-to-text
    val voiceManager = remember {
        VoiceAssistantManager(context) { recognizedCommand ->
            if (recognizedCommand.isNotBlank()) {
                inputText = recognizedCommand
                dispatchPromptRef?.invoke(recognizedCommand)
            }
        }.apply {
            personaMode = currentPersona
            onSpeechTranscribed = { transcribedText, isFinal ->
                inputText = transcribedText
            }
            onWakeWordTriggered = { greeting ->
                scope.launch {
                    chatDao.insertMessage(
                        ChatMessage(
                            role = "assistant",
                            content = greeting
                        )
                    )
                }
            }
        }
    }

    val isListening by voiceManager.isListening.collectAsState()
    val isSpeaking by voiceManager.isSpeaking.collectAsState()
    val currentSentiment by voiceManager.currentSentiment.collectAsState()

    androidx.compose.runtime.DisposableEffect(voiceManager) {
        onDispose {
            voiceManager.destroy()
        }
    }

    val allRequiredPermissions = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.SEND_SMS,
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.POST_NOTIFICATIONS
        )
    } else {
        arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.SEND_SMS,
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.READ_CONTACTS
        )
    }

    // Permission Launchers
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val recordOk = grants[Manifest.permission.RECORD_AUDIO] ?: false
        if (recordOk) {
            Toast.makeText(context, "Microphone enabled. Tap Mic button to speak.", Toast.LENGTH_SHORT).show()
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val savedFile = com.ayra.ai.app.data.StorageHelper.saveMediaToInternalStorage(context, uri, "gallery_img")
            selectedImageUri = if (savedFile != null) Uri.fromFile(savedFile) else uri
        }
    }

    val documentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedDocumentUri = uri
            try {
                var name = "Document"
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1 && cursor.moveToFirst()) {
                        name = cursor.getString(nameIndex) ?: "Document"
                    }
                }
                selectedDocumentName = name
                // Use intelligent document parser for PDF tokens, TXT, CSV, JSON
                val extracted = com.ayra.ai.app.data.StorageHelper.extractTextFromDocument(context, uri, name)
                selectedDocumentText = extracted

                // If document is PDF, also render first page to Base64 image for multimodal vision
                if (name.endsWith(".pdf", ignoreCase = true)) {
                    val pdfBase64 = com.ayra.ai.app.data.StorageHelper.renderPdfFirstPageToBase64(context, uri)
                    selectedDocumentBase64Image = pdfBase64
                } else {
                    selectedDocumentBase64Image = null
                }

                // Persist copy into internal app storage
                com.ayra.ai.app.data.StorageHelper.saveMediaToInternalStorage(context, uri, name)
            } catch (_: Exception) {
                selectedDocumentName = "Uploaded File"
                selectedDocumentText = "Document attached for analysis: ${uri.lastPathSegment}"
                selectedDocumentBase64Image = null
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempCameraUri != null) {
            val savedFile = com.ayra.ai.app.data.StorageHelper.saveMediaToInternalStorage(context, tempCameraUri!!, "cam_img")
            selectedImageUri = if (savedFile != null) Uri.fromFile(savedFile) else tempCameraUri
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            try {
                val exportsDir = File(context.cacheDir, "exports").apply { if (!exists()) mkdirs() }
                val photoFile = File(exportsDir, "camera_capture_${System.currentTimeMillis()}.jpg")
                val uri = FileProvider.getUriForFile(context, "com.ayra.ai.app.fileprovider", photoFile)
                tempCameraUri = uri
                cameraLauncher.launch(uri)
            } catch (e: Exception) {
                android.util.Log.e("ChatScreen", "Camera launch error", e)
                Toast.makeText(context, "Camera launch failed: ${e.localizedMessage ?: "Unknown error"}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Camera permission is required to capture photos", Toast.LENGTH_SHORT).show()
        }
    }

    // Scroll to bottom on new message
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Main Dispatcher function for prompt execution
    fun dispatchPrompt(query: String) {
        val rawTrimmed = query.trim()
        val trimmed = if (rawTrimmed.isNotBlank()) rawTrimmed else {
            if (selectedDocumentUri != null) "Please analyze this document thoroughly, extract all key details, summarize its contents, and provide actionable insights."
            else if (selectedImageUri != null) "Please inspect this photo in detail, identify all subjects and objects, read visible text, and explain the context."
            else ""
        }
        if (trimmed.isBlank() && selectedImageUri == null && selectedDocumentUri == null) return
        if (isGenerating) return

        // Detect user sentiment immediately from prompt for dynamic voice modulation
        val detectedSentiment = voiceManager.detectAndApplySentiment(trimmed)

        val attachedUriStr = selectedImageUri?.toString()
        val docText = selectedDocumentText
        val docName = selectedDocumentName
        val docBase64Image = selectedDocumentBase64Image

        selectedImageUri = null
        selectedDocumentUri = null
        selectedDocumentText = null
        selectedDocumentName = null
        selectedDocumentBase64Image = null
        inputText = ""

        scope.launch {
            // 1. Save user message to Room (displaying attached document badge if present)
            val userContent = if (!docName.isNullOrBlank()) {
                if (rawTrimmed.isNotBlank()) "📄 [$docName]\n$rawTrimmed" else "📄 [$docName]\nPlease analyze this document."
            } else trimmed

            // 1b. Safety Evaluation: Crisis & Self-Harm Detection
            val crisis = CrisisDetector.evaluate(trimmed)
            if (crisis.isCrisis && crisis.supportiveMessage != null) {
                chatDao.insertMessage(
                    ChatMessage(
                        role = "user",
                        content = userContent,
                        attachedImagePath = attachedUriStr
                    )
                )
                chatDao.insertMessage(
                    ChatMessage(
                        role = "assistant",
                        content = crisis.supportiveMessage
                    )
                )
                voiceManager.speak("I hear you and I care about your safety. Free confidential help is available right now. Please reach out to someone you trust.")
                isGenerating = false
                return@launch
            }

            // 1c. ActionGate Check: Propose critical actions for user confirmation
            val proposed = automationController.proposeAction(trimmed)
            if (proposed != null) {
                chatDao.insertMessage(
                    ChatMessage(
                        role = "user",
                        content = userContent,
                        attachedImagePath = attachedUriStr
                    )
                )
                pendingAction = proposed
                isGenerating = false
                return@launch
            }

            chatDao.insertMessage(
                ChatMessage(
                    role = "user",
                    content = userContent,
                    attachedImagePath = attachedUriStr
                )
            )

            isGenerating = true

            // 2. Check for Autonomous Scheduled Task
            if (taskScheduler.isSchedulingIntent(trimmed)) {
                val scheduleResult = taskScheduler.scheduleTaskFromPrompt(trimmed)
                if (scheduleResult.executed) {
                    isGenerating = false
                    chatDao.insertMessage(
                        ChatMessage(
                            role = "assistant",
                            content = scheduleResult.feedbackMessage,
                            craftType = scheduleResult.actionType,
                            craftPayload = scheduleResult.extraData
                        )
                    )
                    voiceManager.speak(scheduleResult.feedbackMessage, overrideSentiment = detectedSentiment)
                    return@launch
                }
            }

            // 2b. Check for harmless quick automation commands
            val autoResult = automationController.tryExecuteAutomation(trimmed)
            if (autoResult != null && autoResult.executed) {
                isGenerating = false
                chatDao.insertMessage(
                    ChatMessage(
                        role = "assistant",
                        content = autoResult.feedbackMessage,
                        craftType = autoResult.actionType,
                        craftPayload = autoResult.extraData
                    )
                )
                voiceManager.speak(autoResult.feedbackMessage, overrideSentiment = detectedSentiment)
                return@launch
            }

            // 3. Unified Fast Multimodal AI & Media Synthesis (Q&A, Coding, Video, Image, Song, 3D, Document Inspection)
            try {
                var base64Image: String? = null
                if (!attachedUriStr.isNullOrBlank()) {
                    try {
                        val stream = context.contentResolver.openInputStream(Uri.parse(attachedUriStr))
                        val bytes = stream?.readBytes()
                        stream?.close()
                        if (bytes != null) {
                            base64Image = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                        }
                    } catch (_: Exception) {}
                } else if (!docBase64Image.isNullOrBlank()) {
                    base64Image = docBase64Image
                }

                val history = chatDao.getRecentMessages(15)
                val response = aiEngine.generateUnifiedResponse(
                    prompt = trimmed,
                    attachedImageBase64 = base64Image,
                    attachedDocumentText = docText,
                    attachedDocumentName = docName,
                    personaMode = currentPersona,
                    modelTier = selectedModelTier,
                    conversationHistory = history
                )
                chatDao.insertMessage(
                    ChatMessage(
                        role = "assistant",
                        content = response.text,
                        craftType = response.craftType,
                        craftPayload = response.craftPayload,
                        actionCommand = response.modelUsed
                    )
                )
                voiceManager.speak(response.text, overrideSentiment = detectedSentiment)
            } catch (e: Exception) {
                val err = "Error processing request: ${e.message}"
                chatDao.insertMessage(
                    ChatMessage(
                        role = "assistant",
                        content = err
                    )
                )
            } finally {
                isGenerating = false
            }
        }
    }

    LaunchedEffect(Unit) {
        dispatchPromptRef = { prompt: String -> dispatchPrompt(prompt) }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CosmicBackground)
    ) {
        // 1. 3D Moving Solar System in the background
        SolarSystem3DCanvas(modifier = Modifier.fillMaxSize())

        // 2. Ambient animated corner neon flares
        AmbientCornerGlow(modifier = Modifier.fillMaxSize())

        // 3. Foreground Chat Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .imePadding()
                .navigationBarsPadding()
        ) {
            // Top App Bar
            GlassHeaderBar(
                isListening = isListening,
                isSpeaking = isSpeaking,
                currentPersona = currentPersona,
                currentSentiment = currentSentiment,
                onLogoClick = { showControlCenterSheet = true },
                onClearChat = {
                    showClearChatDialog = true
                },
                onStopVoice = { voiceManager.stopSpeaking() }
            )

            // Message List
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (messages.isEmpty()) {
                    EmptyCosmicState(
                        currentPersona = currentPersona,
                        onSuggestionClick = { suggestion ->
                            inputText = suggestion
                            dispatchPrompt(suggestion)
                        }
                    )
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(messages, key = { it.id }) { msg ->
                            GlassMessageBubble(
                                message = msg,
                                onImageClick = { expandedImageUri = it },
                                onCopy = {
                                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    cm.setPrimaryClip(ClipData.newPlainText("Ayra Message", msg.content))
                                    Toast.makeText(context, "Copied!", Toast.LENGTH_SHORT).show()
                                },
                                onSpeak = {
                                    val msgSentiment = SentimentDetector.detectSentiment(msg.content)
                                    voiceManager.speak(msg.content, overrideSentiment = msgSentiment)
                                },
                                onEdit = { contentToEdit ->
                                    inputText = contentToEdit
                                    Toast.makeText(context, "Loaded into editor to edit / এডিট করতে লোড হয়েছে", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }

                        if (isGenerating) {
                            item {
                                AssistantGeneratingIndicator()
                            }
                        }
                    }
                }
            }

            // Bottom Attachment Preview
            if (selectedImageUri != null) {
                Box(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(GlassSurfaceDark)
                        .border(1.dp, NeonCyan, RoundedCornerShape(12.dp))
                        .padding(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AsyncImage(
                            model = selectedImageUri,
                            contentDescription = "Selected Image",
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Text(
                            text = "Visual attachment ready",
                            color = TextPrimary,
                            fontSize = 12.sp,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { selectedImageUri = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Remove", tint = NeonRose)
                        }
                    }
                }
            }

            // Document / PDF Attachment Preview
            if (selectedDocumentName != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 3.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(GlassSurfaceDark)
                        .border(1.dp, NeonCyan.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Description, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(20.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = selectedDocumentName ?: "Document",
                                color = TextPrimary,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Ready for Q&A, summary, editing & insights",
                                color = TextSecondary,
                                fontSize = 10.sp
                            )
                        }
                        IconButton(
                            onClick = {
                                selectedDocumentUri = null
                                selectedDocumentName = null
                                selectedDocumentText = null
                                selectedDocumentBase64Image = null
                            },
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Remove", tint = NeonRose, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            // ActionGate Confirmation Card (Requires explicit user tap to execute)
            pendingAction?.let { action ->
                ActionConfirmationCard(
                    action = action,
                    onConfirm = {
                        scope.launch {
                            val result = automationController.executeConfirmedAction(action)
                            chatDao.insertMessage(
                                ChatMessage(
                                    role = "assistant",
                                    content = result.feedbackMessage,
                                    craftType = result.actionType,
                                    craftPayload = result.extraData
                                )
                            )
                            voiceManager.speak(result.feedbackMessage)
                            pendingAction = null
                        }
                    },
                    onDismiss = {
                        scope.launch {
                            chatDao.insertMessage(
                                ChatMessage(
                                    role = "assistant",
                                    content = "Action cancelled by user / কাজটি বাতিল করা হয়েছে।"
                                )
                            )
                            pendingAction = null
                        }
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }

            // Clean Input Row (NO elevated typing bar as requested)
            GlassInputRow(
                text = inputText,
                onTextChange = { inputText = it },
                isListening = isListening,
                isGenerating = isGenerating,
                onSend = { dispatchPrompt(inputText) },
                onMicClick = {
                    val hasRecord = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.RECORD_AUDIO
                    ) == PackageManager.PERMISSION_GRANTED

                    if (!hasRecord) {
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.RECORD_AUDIO,
                                Manifest.permission.CALL_PHONE,
                                Manifest.permission.ACCESS_FINE_LOCATION
                            )
                        )
                    } else {
                        if (isListening) {
                            voiceManager.stopListening()
                        } else {
                            // Transcribe spoken speech directly into chat interface
                            voiceManager.onSpeechTranscribed = { transcribedText, isFinal ->
                                inputText = transcribedText
                            }
                            voiceManager.startManualListening()
                        }
                    }
                },
                onAttachClick = { showAttachmentPicker = true }
            )
        }

        // Full Screen Image Modal
        if (expandedImageUri != null) {
            Dialog(onDismissRequest = { expandedImageUri = null }) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.9f))
                        .clickable { expandedImageUri = null },
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = expandedImageUri,
                        contentDescription = "Expanded View",
                        modifier = Modifier
                            .fillMaxWidth(0.95f)
                            .clip(RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Fit
                    )
                }
            }
        }

        // Attachment Source & Super-App Feature Hub Modal (Bottom '+' Hub)
        if (showAttachmentPicker) {
            Dialog(onDismissRequest = { showAttachmentPicker = false }) {
                Surface(
                    shape = RoundedCornerShape(26.dp),
                    color = Color(0xFF070B16),
                    border = androidx.compose.foundation.BorderStroke(
                        1.5.dp,
                        Brush.linearGradient(listOf(NeonCyan, NeonPurple, NeonRose))
                    ),
                    modifier = Modifier
                        .fillMaxWidth(0.96f)
                        .heightIn(max = 620.dp)
                        .padding(vertical = 12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .verticalScroll(rememberScrollState())
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(Brush.radialGradient(listOf(NeonCyan, NeonPurple)))
                                ) {
                                    Icon(
                                        Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = Color.Black,
                                        modifier = Modifier.size(18.dp).align(Alignment.Center)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "Ayra AI Studio & Super-Hub ⚡",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = NeonCyan,
                                        fontSize = 16.sp
                                    )
                                    Text(
                                        text = "OmniRoute • DeepSeek • Higgsfield • Qwen • G0DM0D3",
                                        color = TextSecondary,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                            IconButton(
                                onClick = { showAttachmentPicker = false },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary, modifier = Modifier.size(18.dp))
                            }
                        }

                        // Section 1: Multimodal Inputs
                        Text(
                            text = "📸 মিডিয়া ও ডকুমেন্ট ইনপুট (Multimodal Capture):",
                            color = NeonCyan,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        )

                        // 1. Camera
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF0F172A))
                                .border(0.8.dp, NeonCyan.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                .clickable {
                                    showAttachmentPicker = false
                                    val hasPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                                    if (hasPerm) {
                                        try {
                                            val exportsDir = File(context.cacheDir, "exports").apply { if (!exists()) mkdirs() }
                                            val photoFile = File(exportsDir, "camera_capture_${System.currentTimeMillis()}.jpg")
                                            val uri = FileProvider.getUriForFile(context, "com.ayra.ai.app.fileprovider", photoFile)
                                            tempCameraUri = uri
                                            cameraLauncher.launch(uri)
                                        } catch (e: Exception) {
                                            android.util.Log.e("ChatScreen", "Camera launch error", e)
                                            Toast.makeText(context, "Camera launch failed: ${e.localizedMessage ?: "Unknown error"}", Toast.LENGTH_SHORT).show()
                                        }
                                    } else {
                                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                    }
                                }
                                .padding(12.dp)
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Take Photo with Camera", color = TextPrimary, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
                                Text("ছবি তুলে সরাসরি এআইকে অ্যানালাইসিস করতে বলুন", color = TextSecondary, fontSize = 10.5.sp)
                            }
                        }

                        // 2. Gallery
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF0F172A))
                                .border(0.8.dp, NeonPurple.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                .clickable {
                                    showAttachmentPicker = false
                                    galleryLauncher.launch("image/*")
                                }
                                .padding(12.dp)
                        ) {
                            Icon(Icons.Default.Image, contentDescription = null, tint = NeonPurple, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Upload from Photos / Gallery", color = TextPrimary, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
                                Text("মাল্টি-এআই ভিশন, অবজেক্ট ডিটেকশন ও স্টাইল এডিট", color = TextSecondary, fontSize = 10.5.sp)
                            }
                        }

                        // 3. Document & PDF Upload
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF0F172A))
                                .border(0.8.dp, NeonCyan.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                .clickable {
                                    showAttachmentPicker = false
                                    documentLauncher.launch(
                                        arrayOf(
                                            "application/pdf",
                                            "text/*",
                                            "application/msword",
                                            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                                            "application/json",
                                            "*/*"
                                        )
                                    )
                                }
                                .padding(12.dp)
                        ) {
                            Icon(Icons.Default.Description, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Upload Document / PDF File", color = TextPrimary, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
                                Text("PDF বা ডকুমেন্টের সম্পূর্ণ সারাংশ ও প্রশ্নোত্তর", color = TextSecondary, fontSize = 10.5.sp)
                            }
                        }

                        // Section 2: Frontier Multi-AI Architectures
                        Text(
                            text = "🧠 ফ্রন্টিয়ার এআই ইঞ্জিন (Multi-Model Frontier Suite):",
                            color = NeonGold,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        )

                        // 4. OmniRoute Ensemble
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF131B2E))
                                .border(1.dp, NeonGold.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                .clickable {
                                    showAttachmentPicker = false
                                    selectedModelTier = "omniroute"
                                    aiEngine.selectedModelTier = "omniroute"
                                    inputText = "OmniRoute Ensemble: সব এআই মডেলের সম্মিলিত জ্ঞান দিয়ে গভীর বিশ্লেষণ দাও"
                                }
                                .padding(12.dp)
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = NeonGold, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("OmniRoute Multi-Model Ensemble ⚡👑", color = TextPrimary, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
                                Text("সব এআই একসাথে সমন্বিত হয়ে বিশ্বের সেরা ফলাফল দেয়", color = TextSecondary, fontSize = 10.5.sp)
                            }
                        }

                        // 5. DeepSeek-R1 Reasoning Harness
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF131B2E))
                                .border(1.dp, NeonRose.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                .clickable {
                                    showAttachmentPicker = false
                                    selectedModelTier = "deepseek_harness"
                                    aiEngine.selectedModelTier = "deepseek_harness"
                                    inputText = "DeepSeek Reasoning Harness: ধাপে ধাপে গভীর গাণিতিক যুক্তি সহ প্রমাণ করো"
                                }
                                .padding(12.dp)
                        ) {
                            Icon(Icons.Default.Code, contentDescription = null, tint = NeonRose, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("DeepSeek-R1 Reasoning Harness 🔬", color = TextPrimary, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
                                Text("Chain-of-thought যুক্তি, গণিত ও স্বয়ংক্রিয় টেস্ট হারনেস", color = TextSecondary, fontSize = 10.5.sp)
                            }
                        }

                        // 6. Qwen 3.8 27B Uncensored
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF131B2E))
                                .border(1.dp, Color(0xFFFF5722).copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                .clickable {
                                    showAttachmentPicker = false
                                    selectedModelTier = "qwen_uncensored"
                                    aiEngine.selectedModelTier = "qwen_uncensored"
                                    inputText = "Qwen Uncensored: কোনো অস্বীকৃতি ছাড়া সরাসরি সত্য ও বিস্তারিত উত্তর দাও"
                                }
                                .padding(12.dp)
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFFFF5722), modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Qwen 3.8 27B Uncensored 🔓", color = TextPrimary, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
                                Text("লেয়ার-৩৮ অর্থোগোনাল ডি-সেন্সর্ড মডেল — জিরো রিফিউজাল", color = TextSecondary, fontSize = 10.5.sp)
                            }
                        }

                        // 7. G0DM0D3 Plinius Defensive Lab
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF131B2E))
                                .border(1.dp, NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                .clickable {
                                    showAttachmentPicker = false
                                    selectedModelTier = "godmode"
                                    aiEngine.selectedModelTier = "godmode"
                                    inputText = "G0DM0D3: ডিপ সাইবার ডিফেন্স, রিভার্স ইঞ্জিনিয়ারিং ও ভালনারেবিলিটি অডিট"
                                }
                                .padding(12.dp)
                        ) {
                            Icon(Icons.Default.Code, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("G0DM0D3 Plinius Uncensored 🧬", color = TextPrimary, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
                                Text("অবাধ সাইবার সিকিউরিটি, রিভার্স ইঞ্জিনিয়ারিং ও সিস্টেম অডিট", color = TextSecondary, fontSize = 10.5.sp)
                            }
                        }

                        // 8. Open-Higgsfield-AI Cinematic Video
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF131B2E))
                                .border(1.dp, NeonRose.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                .clickable {
                                    showAttachmentPicker = false
                                    inputText = "Generate a cinematic 4K video of a spaceship entering a colorful nebula with FPV drone motion"
                                }
                                .padding(12.dp)
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = NeonRose, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Open-Higgsfield-AI Cinematic 4K Video 🎬", color = TextPrimary, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
                                Text("ক্যামেরা মোশন ফিজিক্স ও অ্যানামরফিক অপটিক্যাল রেন্ডারিং", color = TextSecondary, fontSize = 10.5.sp)
                            }
                        }

                        // 9. ECC Enterprise Cognitive Engine
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF131B2E))
                                .border(1.dp, Color(0xFF7C4DFF).copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                .clickable {
                                    showAttachmentPicker = false
                                    selectedModelTier = "ecc"
                                    aiEngine.selectedModelTier = "ecc"
                                    inputText = "ECC Cognitive: কনটেক্সট মেমরি ও ইন্টেন্ট ডিসঅ্যাম্বিগিউয়েশন গ্রাউন্ড করো"
                                }
                                .padding(12.dp)
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFF7C4DFF), modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("ECC Enterprise Cognitive 🌐", color = TextPrimary, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
                                Text("মাল্টি-টার্ন মেমরি গ্রাফ, ইন্টেন্ট রেজোলিউশন ও ফ্যাক্ট চেকিং", color = TextSecondary, fontSize = 10.5.sp)
                            }
                        }

                        // 10. Superpowers Autonomous Agent
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF131B2E))
                                .border(1.dp, NeonGreen.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                .clickable {
                                    showAttachmentPicker = false
                                    inputText = "Superpower: TDD এবং সিস্টেমেটিক ডিবাগিং দিয়ে কোড টেস্ট ও আর্কিটেকচার তৈরি করো"
                                }
                                .padding(12.dp)
                        ) {
                            Icon(Icons.Default.Code, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Superpowers Autonomous Agent ⚙️", color = TextPrimary, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
                                Text("TDD টেস্ট ড্রাইভেন কোডিং, সিস্টেমেটিক ডিবাগিং ও কোড রিভিউ", color = TextSecondary, fontSize = 10.5.sp)
                            }
                        }

                        // Section 3: Advanced Practical Tools
                        Text(
                            text = "⚡ পাওয়ার টুলস ও সিকিউরিটি (Security & Utility Hub):",
                            color = NeonCyan,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        )

                        // 11. Cyber Security Terminal
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF131B2E))
                                .border(0.8.dp, NeonGreen.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                .clickable {
                                    showAttachmentPicker = false
                                    inputText = "Open Cyber Terminal"
                                    dispatchPromptRef?.invoke("Open Cyber Terminal")
                                }
                                .padding(12.dp)
                        ) {
                            Icon(Icons.Default.Code, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Cyber Security Defensive Lab 💻", color = TextPrimary, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
                                Text("Ping, Traceroute, Whois, Hash, CVE ও OWASP অডিট", color = TextSecondary, fontSize = 10.5.sp)
                            }
                        }

                        // 12. Create New PDF Document
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF131B2E))
                                .border(0.8.dp, NeonGold.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                .clickable {
                                    showAttachmentPicker = false
                                    inputText = "Create a professional PDF document about Artificial Intelligence"
                                }
                                .padding(12.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, tint = NeonGold, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Create New PDF Document 📄", color = TextPrimary, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
                                Text("ফরম্যাটেড প্রফেশনাল PDF তৈরি ও ডিভাইসে সেভ", color = TextSecondary, fontSize = 10.5.sp)
                            }
                        }

                        // 13. AI Image Studio
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF131B2E))
                                .border(0.8.dp, NeonPurple.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                .clickable {
                                    showAttachmentPicker = false
                                    inputText = "একটি সুন্দর রঙিন ড্রাগনের 8K ছবি আঁকো"
                                }
                                .padding(12.dp)
                        ) {
                            Icon(Icons.Default.Image, contentDescription = null, tint = NeonPurple, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("AI Image Studio (8K Masterpiece) 🎨", color = TextPrimary, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
                                Text("DALL-E 3 + Gemini + Flux.1 আল্ট্রা-রিয়েল ভিজ্যুয়াল", color = TextSecondary, fontSize = 10.5.sp)
                            }
                        }

                        // 14. AI Music & Song Studio
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF131B2E))
                                .border(0.8.dp, NeonCyan.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                .clickable {
                                    showAttachmentPicker = false
                                    inputText = "একটি রোমান্টিক মিষ্টি প্রেমের গান বানাও"
                                }
                                .padding(12.dp)
                        ) {
                            Icon(Icons.Default.VolumeUp, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("AI Music & Song Creator 🎵", color = TextPrimary, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
                                Text("নতুন সুর, লিরিক্স ও অডিও প্লেয়ার সিন্থেসিস", color = TextSecondary, fontSize = 10.5.sp)
                            }
                        }

                        // 15. Build-Your-Own-X Engineering
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF131B2E))
                                .border(0.8.dp, NeonGold.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                .clickable {
                                    showAttachmentPicker = false
                                    inputText = "Build your own Redis server from scratch with complete architecture and code"
                                }
                                .padding(12.dp)
                        ) {
                            Icon(Icons.Default.Code, contentDescription = null, tint = NeonGold, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Build-Your-Own-X Master Engine ⚙️", color = TextPrimary, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
                                Text("Git, Docker, Redis, SQLite, Transformer স্ক্র্যাচ থেকে কোড", color = TextSecondary, fontSize = 10.5.sp)
                            }
                        }

                        // 16. VPN Privacy Shield
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF131B2E))
                                .border(0.8.dp, NeonCyan.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                .clickable {
                                    showAttachmentPicker = false
                                    val ns = AyraVpnPrivacyController.toggleVpn(context)
                                    Toast.makeText(context, if (ns) "🛡️ System-Wide VPN Connected! (All apps protected)" else "🛡️ VPN Disconnected", Toast.LENGTH_SHORT).show()
                                }
                                .padding(12.dp)
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Ayra VPN Privacy Shield 🛡️", color = TextPrimary, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
                                Text(if (vpnStatus.isConnected) "Status: Connected (${vpnStatus.config.endpoint})" else if (vpnStatus.isConfigured) "WireGuard Ready (${vpnStatus.config.endpoint})" else "WireGuard টানেল ও ChaCha20 এনক্রিপশন", color = TextSecondary, fontSize = 10.5.sp)
                            }
                        }

                        // 17. Schedule Routine / Call
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF131B2E))
                                .border(0.8.dp, NeonCyan.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                .clickable {
                                    showAttachmentPicker = false
                                    inputText = "কাল সকাল ১০টায় বাবাকে কল করে বলো ওষুধ খেয়ে নিতে"
                                }
                                .padding(12.dp)
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Schedule Routine / Autonomous Call ⏰", color = TextPrimary, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
                                Text("নির্দিষ্ট সময়ে কল, মেসেজ বা উইশ করার শিডিউলার", color = TextSecondary, fontSize = 10.5.sp)
                            }
                        }

                        // 18. Share GPS Location
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF131B2E))
                                .border(0.8.dp, NeonRose.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                .clickable {
                                    showAttachmentPicker = false
                                    dispatchPrompt("Where am I right now? GPS Coordinates")
                                }
                                .padding(12.dp)
                        ) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = NeonRose, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Share Current GPS Location 📍", color = TextPrimary, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
                                Text("রিয়েল-টাইম অক্ষাংশ, দ্রাঘিমাংশ ও মানচিত্র লিঙ্ক", color = TextSecondary, fontSize = 10.5.sp)
                            }
                        }
                    }
                }
            }
        }

        // Control Center & Settings Bottom Sheet (Opened by clicking Ayra Logo)
        if (showControlCenterSheet) {
            ControlCenterSheet(
                sheetState = sheetState,
                automationController = automationController,
                messageCount = messages.size,
                currentPersona = currentPersona,
                onPersonaChange = { newPersona ->
                    currentPersona = newPersona
                    voiceManager.personaMode = newPersona
                    aiEngine.currentPersona = newPersona
                },
                selectedModelTier = selectedModelTier,
                onModelTierChange = { newTier ->
                    selectedModelTier = newTier
                    aiEngine.selectedModelTier = newTier
                },
                wakeWordEnabled = voiceManager.isAlwaysOnWakeWordEnabled,
                onWakeWordToggle = { enabled ->
                    voiceManager.isAlwaysOnWakeWordEnabled = enabled
                    if (enabled) {
                        voiceManager.startListening()
                    } else {
                        voiceManager.stopListening()
                    }
                },
                onRequestAllPermissions = { permissionLauncher.launch(allRequiredPermissions) },
                currentSentiment = currentSentiment,
                dynamicVoiceModulationEnabled = voiceManager.isDynamicModulationEnabled,
                onToggleDynamicVoiceModulation = { voiceManager.isDynamicModulationEnabled = it },
                selectedSentimentLock = voiceManager.manualSentimentLock,
                onSentimentLockChange = { voiceManager.manualSentimentLock = it },
                onTestSentimentVoice = { sentiment ->
                    voiceManager.testSentimentVoice(sentiment)
                },
                scheduledTasks = scheduledTasks,
                onDeleteScheduledTask = { taskId ->
                    scope.launch {
                        taskScheduler.cancelAlarm(taskId)
                        db.scheduledTaskDao().deleteTask(taskId)
                        Toast.makeText(context, "Scheduled task removed / শিডিউল রুটিন মুছে ফেলা হয়েছে", Toast.LENGTH_SHORT).show()
                    }
                },
                userMessages = messages.filter { it.role == "user" },
                onSelectPrompt = { selectedPrompt ->
                    showControlCenterSheet = false
                    inputText = selectedPrompt
                },
                onCopyPrompt = { promptToCopy ->
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                    val clip = ClipData.newPlainText("Ayra Prompt", promptToCopy)
                    clipboard?.setPrimaryClip(clip)
                    Toast.makeText(context, "Prompt copied to clipboard / কপি করা হয়েছে", Toast.LENGTH_SHORT).show()
                },
                onDismiss = { showControlCenterSheet = false },
                onClearHistory = {
                    showClearChatDialog = true
                },
                onExportHistory = {
                    val file = com.ayra.ai.app.data.StorageHelper.exportChatHistory(context, messages)
                    if (file != null) {
                        Toast.makeText(context, "চ্যাট হিস্ট্রি সফলভাবে সেভ করা হয়েছে:\n${file.name}", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, "এক্সপোর্ট করার মতো কোনো হিস্ট্রি নেই", Toast.LENGTH_SHORT).show()
                    }
                },
                onQuickFeedback = { feedback ->
                    Toast.makeText(context, feedback, Toast.LENGTH_SHORT).show()
                }
            )
        }

        if (showClearChatDialog) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showClearChatDialog = false },
                title = { Text("Clear Chat History", color = TextPrimary, fontWeight = FontWeight.Bold) },
                text = { Text("Are you sure you want to clear all conversation messages? This cannot be undone.", color = TextSecondary) },
                confirmButton = {
                    androidx.compose.material3.TextButton(
                        onClick = {
                            scope.launch {
                                chatDao.clearAllMessages()
                                showClearChatDialog = false
                                showControlCenterSheet = false
                                Toast.makeText(context, "Chat history cleared / চ্যাট হিস্ট্রি মুছে ফেলা হয়েছে", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Text("Clear All", color = NeonRose, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    androidx.compose.material3.TextButton(onClick = { showClearChatDialog = false }) {
                        Text("Cancel", color = TextSecondary)
                    }
                },
                containerColor = Color(0xFF1E293B)
            )
        }
    }
}

@Composable
private fun GlassHeaderBar(
    isListening: Boolean,
    isSpeaking: Boolean,
    currentPersona: String,
    currentSentiment: UserSentiment = UserSentiment.NEUTRAL_CARING,
    onLogoClick: () -> Unit,
    onClearChat: () -> Unit,
    onStopVoice: () -> Unit
) {
    // Dynamic continuous glowing animation for header title and subtitle
    val infiniteTransition = rememberInfiniteTransition(label = "header_text_anim")
    val animOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 700f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "anim_offset"
    )

    val animatedGradient = Brush.linearGradient(
        colors = listOf(NeonCyan, NeonPurple, NeonRose, NeonCyan),
        start = Offset(animOffset, 0f),
        end = Offset(animOffset + 350f, 150f)
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 14.dp, end = 14.dp, top = 2.dp, bottom = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(GlassSurfaceDark)
            .border(
                1.dp,
                Brush.linearGradient(
                    if (currentPersona == "GF_COMPANION")
                        listOf(NeonRose.copy(alpha = 0.6f), NeonPurple.copy(alpha = 0.4f), NeonCyan.copy(alpha = 0.3f))
                    else
                        listOf(NeonCyan.copy(alpha = 0.6f), NeonPurple.copy(alpha = 0.4f))
                ),
                RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Glowing Ayra Indicator / Control Center Button
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        if (currentPersona == "GF_COMPANION")
                            listOf(NeonRose, NeonPurple, Color.Black)
                        else
                            listOf(NeonCyan, NeonPurple, Color.Black)
                    )
                )
                .clickable { onLogoClick() }
                .testTag("ayra_logo_button"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = "Ayra Control Center & Settings",
                tint = Color.Black,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .clickable { onLogoClick() }
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Ayra AI",
                    style = MaterialTheme.typography.titleMedium.copy(
                        brush = animatedGradient
                    ),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 17.sp
                )

                // "by Alok" custom badge as requested by user
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF131B2E).copy(alpha = 0.85f))
                        .border(
                            1.dp,
                            animatedGradient,
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "by Alok",
                        style = MaterialTheme.typography.labelSmall.copy(
                            brush = animatedGradient
                        ),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            Text(
                text = when {
                    isListening -> "Listening... 🎙️ Say 'Ayra' anytime"
                    isSpeaking -> if (currentPersona == "GF_COMPANION") {
                        "Speaking • ${currentSentiment.emoji} ${currentSentiment.displayName} (${String.format(Locale.US, "%.2f", currentSentiment.pitch)}x) 🔊"
                    } else {
                        "Speaking (Clear Voice) 🔊"
                    }
                    currentPersona == "GF_COMPANION" -> "${currentSentiment.emoji} ${currentSentiment.bengaliName} Voice • Ready 🎙️"
                    else -> "Voice Trigger Ready (Say 'Ayra') 🎙️"
                },
                style = MaterialTheme.typography.bodySmall.copy(
                    brush = animatedGradient
                ),
                fontSize = 10.5.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        if (isSpeaking) {
            IconButton(onClick = onStopVoice) {
                Icon(Icons.Default.VolumeUp, contentDescription = "Mute", tint = NeonRose)
            }
        }

        IconButton(onClick = onClearChat) {
            Icon(Icons.Default.Delete, contentDescription = "Clear Chat", tint = TextSecondary)
        }
    }
}

@Composable
private fun GlassMessageBubble(
    message: ChatMessage,
    onImageClick: (String) -> Unit,
    onCopy: () -> Unit,
    onSpeak: () -> Unit,
    onEdit: (String) -> Unit
) {
    val isUser = message.role == "user"
    val context = LocalContext.current

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(if (isUser) 0.82f else 1f)
                .clip(RoundedCornerShape(18.dp))
                .background(if (isUser) UserBubble else GlassSurfaceDark)
                .border(
                    1.2.dp,
                    if (isUser) {
                        Brush.linearGradient(listOf(NeonCyan.copy(alpha = 0.5f), Color(0x3300FFFF)))
                    } else {
                        Brush.linearGradient(listOf(NeonCyan.copy(alpha = 0.7f), NeonPurple.copy(alpha = 0.5f), NeonRose.copy(alpha = 0.6f)))
                    },
                    RoundedCornerShape(18.dp)
                )
                .padding(14.dp)
        ) {
            Column {
                // Sender label & Auto-Selected AI Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (isUser) NeonCyan else NeonRose)
                        )
                        Text(
                            text = if (isUser) "You" else "Ayra AI",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isUser) NeonCyan else NeonRose
                        )
                    }

                    // Auto-Selected Model Indicator
                    if (!isUser && !message.actionCommand.isNullOrBlank()) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF0F172A))
                                .border(0.8.dp, NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "🤖 ${message.actionCommand}",
                                color = NeonCyan,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // Attached / Generated Image Preview
                if (!message.attachedImagePath.isNullOrBlank()) {
                    AsyncImage(
                        model = message.attachedImagePath,
                        contentDescription = "Image preview",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .clickable { onImageClick(message.attachedImagePath) },
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Generative Media: Photo (SubcomposeAsyncImage with high-tech progress loading)
                if (message.craftType == "IMAGE" && !message.craftPayload.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .border(1.2.dp, NeonCyan, RoundedCornerShape(14.dp))
                    ) {
                        SubcomposeAsyncImage(
                            model = message.craftPayload,
                            contentDescription = "AI Generated Photo",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(250.dp)
                                .clickable { onImageClick(message.craftPayload) },
                            contentScale = ContentScale.Crop,
                            loading = {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(250.dp)
                                        .background(Color(0xFF0F172A)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(32.dp),
                                            color = NeonCyan,
                                            strokeWidth = 2.5.dp
                                        )
                                        Text(
                                            text = "Crafting 8K Ultra-HD Masterpiece...",
                                            color = TextSecondary,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            },
                            error = {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(250.dp)
                                        .background(Color(0xFF1E293B)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "8K Masterpiece Loaded • Tap to Open",
                                        color = NeonCyan,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(8.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.Black.copy(alpha = 0.75f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "HD 8K Masterpiece • Tap to Expand 🔍",
                                color = NeonCyan,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Generative Media: Video Player Card
                if (message.craftType == "VIDEO" && !message.craftPayload.isNullOrBlank()) {
                    VideoPlayerCard(
                        jsonPayload = message.craftPayload,
                        onExpand = onImageClick
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Generative Media: Song Player Card
                if (message.craftType == "SONG" && !message.craftPayload.isNullOrBlank()) {
                    SongPlayerCard(jsonPayload = message.craftPayload)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Generative Media: 3D CAD Hologram Model
                if (message.craftType == "3D") {
                    ThreeDModelCard(
                        craftPayload = message.craftPayload,
                        title = "3D Interactive Hologram"
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Autonomous Scheduled Task / Call / Message / Reminder Banner
                if ((message.craftType == "SCHEDULED_ROUTINE" || message.craftType == "SCHEDULED_CALL" ||
                            message.craftType == "SCHEDULED_MESSAGE" || message.craftType == "SCHEDULED_REMINDER") && !message.craftPayload.isNullOrBlank()) {
                    ScheduledRoutineBannerCard(jsonPayload = message.craftPayload)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Generative PDF Download & Document Viewer Card
                if (message.craftType == "PDF_DOWNLOAD" && !message.craftPayload.isNullOrBlank()) {
                    com.ayra.ai.app.ui.components.PdfDocumentDownloadCard(filePath = message.craftPayload)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Cyber Security Terminal & Practice Console
                if (message.craftType == "CYBER_TERMINAL") {
                    com.ayra.ai.app.ui.components.CyberTerminalCard(
                        initialCommand = message.craftPayload
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Text Content with Markdown Code Blocks & Syntax formatting
                MarkdownCodeContentView(
                    content = message.content,
                    onCopyCode = { codeToCopy ->
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                        val clip = ClipData.newPlainText("Ayra AI Code", codeToCopy)
                        clipboard?.setPrimaryClip(clip)
                        Toast.makeText(context, "Code copied to clipboard! / কোড কপি করা হয়েছে 📋", Toast.LENGTH_SHORT).show()
                    }
                )

                // Moderate Futuristic Edit Question Button for user queries
                if (isUser) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF0B132B).copy(alpha = 0.85f))
                                .border(
                                    1.dp,
                                    Brush.linearGradient(listOf(NeonCyan.copy(alpha = 0.7f), NeonPurple.copy(alpha = 0.5f))),
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable { onEdit(message.content) }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                                .testTag("edit_question_button"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Edit Question / প্রশ্ন এডিট",
                                tint = NeonCyan,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "Edit Question / প্রশ্ন এডিট",
                                color = NeonCyan,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // Bottom Action Bar for assistant responses
                if (!isUser) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onCopy, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = TextSecondary, modifier = Modifier.size(16.dp))
                        }
                        IconButton(onClick = onSpeak, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.VolumeUp, contentDescription = "Listen", tint = NeonCyan, modifier = Modifier.size(18.dp))
                        }
                        IconButton(onClick = { onEdit(message.content) }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Note / নোট এডিট", tint = NeonPurple, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GlassInputRow(
    text: String,
    onTextChange: (String) -> Unit,
    isListening: Boolean,
    isGenerating: Boolean,
    onSend: () -> Unit,
    onMicClick: () -> Unit,
    onAttachClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Upgraded Bottom '+' Hub Button (High Quality, Futuristic Glow, 48dp target)
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(Color(0xFF1E1035), GlassSurfaceDark)
                    )
                )
                .border(
                    1.4.dp,
                    Brush.linearGradient(listOf(NeonCyan, NeonPurple, NeonRose)),
                    CircleShape
                )
                .clickable { onAttachClick() }
                .testTag("bottom_attachment_hub_button"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Attach & Super-App Tools Hub",
                tint = NeonCyan,
                modifier = Modifier.size(22.dp)
            )
        }

        // Text Field (High Contrast & Visible Input)
        OutlinedTextField(
            value = text,
            onValueChange = onTextChange,
            placeholder = { Text("Ask Ayra, generate photo, open apps...", color = TextSecondary, fontSize = 13.5.sp) },
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(24.dp))
                .background(GlassSurfaceDark)
                .testTag("chat_input_text_field"),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = NeonCyan,
                unfocusedBorderColor = Color(0x3300FFFF),
                cursorColor = NeonCyan,
                focusedContainerColor = GlassSurfaceDark,
                unfocusedContainerColor = GlassSurfaceDark
            ),
            shape = RoundedCornerShape(24.dp),
            maxLines = 4
        )

        // Mic Button with sweet glowing state
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(if (isListening) NeonRose else GlassSurfaceDark)
                .border(1.2.dp, if (isListening) NeonRose else NeonCyan, CircleShape)
                .clickable { onMicClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                contentDescription = "Voice",
                tint = if (isListening) Color.Black else NeonCyan
            )
        }

        // Send Button
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(listOf(NeonCyan, NeonPurple))
                )
                .clickable(enabled = !isGenerating && text.isNotBlank()) { onSend() },
            contentAlignment = Alignment.Center
        ) {
            if (isGenerating) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.Black)
            } else {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint = Color.Black,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun EmptyCosmicState(
    currentPersona: String,
    onSuggestionClick: (String) -> Unit = {}
) {
    val infiniteTransition = rememberInfiniteTransition(label = "empty_cosmic_anim")
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 900f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_offset"
    )

    val animatedGradient = Brush.linearGradient(
        colors = listOf(
            NeonCyan,
            NeonPurple,
            NeonRose,
            Color(0xFFFFD700),
            NeonGreen,
            NeonCyan
        ),
        start = Offset(shimmerOffset, 0f),
        end = Offset(shimmerOffset + 450f, 200f)
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 18.dp, start = 14.dp, end = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        // Glowing animated title row with "by Alok" badge directly above galaxy
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Ayra AI",
                style = MaterialTheme.typography.headlineLarge.copy(
                    brush = animatedGradient
                ),
                fontWeight = FontWeight.Black,
                fontSize = 30.sp,
                letterSpacing = 1.2.sp
            )

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF0F172A).copy(alpha = 0.85f))
                    .border(
                        1.2.dp,
                        animatedGradient,
                        RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "by Alok",
                    style = MaterialTheme.typography.labelSmall.copy(
                        brush = animatedGradient
                    ),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.6.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = if (currentPersona == "GF_COMPANION") "Sweet Companion & All-in-One Autonomous AI Master" else "Next-Gen Frontier AI • All Multi-Models Unified (GPT-4o + Gemini 2.5 + Claude 3.7 + DeepSeek R1)",
            color = TextSecondary,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Subtle glowing pill pointing to the top and bottom '+' quick tools
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(GlassSurfaceDark.copy(alpha = 0.6f))
                .border(1.dp, NeonCyan.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                .padding(horizontal = 14.dp, vertical = 7.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(NeonCyan)
            )
            Text(
                text = "Tap Top or Bottom '+' for Cyber Lab, VPN, Images & Tools",
                color = NeonCyan,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun AssistantGeneratingIndicator() {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(GlassSurfaceDark)
            .border(1.dp, NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(16.dp),
            strokeWidth = 2.dp,
            color = NeonCyan
        )
        Text(
            text = "Ayra is thinking & synthesizing...",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )
    }
}

@Composable
private fun ScheduledRoutineBannerCard(jsonPayload: String) {
    val data = remember(jsonPayload) {
        try {
            JSONObject(jsonPayload)
        } catch (_: Exception) {
            null
        }
    }
    val taskType = data?.optString("taskType", "ROUTINE") ?: "ROUTINE"
    val recipient = data?.optString("recipient", "Friend") ?: "Friend"
    val targetApp = data?.optString("targetApp", "WhatsApp") ?: "WhatsApp"
    val time = data?.optString("time", "12:00 AM") ?: "12:00 AM"
    val phone = data?.optString("phone", "") ?: ""
    val spokenMessage = data?.optString("spokenMessage", "") ?: ""
    val reminder = data?.optString("reminder", "") ?: ""
    val message = data?.optString("message", "") ?: ""

    val isCall = taskType == "CALL" || targetApp.equals("CALL", ignoreCase = true)
    val isReminder = taskType == "REMINDER" || targetApp.equals("REMINDER", ignoreCase = true)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.linearGradient(
                    if (isCall) listOf(Color(0xFF0F172A), Color(0xFF1E1B4B))
                    else if (isReminder) listOf(Color(0xFF2E1065), Color(0xFF0F172A))
                    else listOf(Color(0xFF0D2520), Color(0xFF0A1526))
                )
            )
            .border(
                1.2.dp,
                Brush.linearGradient(
                    if (isCall) listOf(NeonCyan, NeonPurple)
                    else if (isReminder) listOf(Color(0xFFFFD700), NeonRose)
                    else listOf(NeonGreen, NeonCyan)
                ),
                RoundedCornerShape(14.dp)
            )
            .padding(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (isCall) NeonCyan else if (isReminder) Color(0xFFFFD700) else NeonGreen)
                    )
                    Text(
                        text = if (isCall) "VOICE CALL ARMED" else if (isReminder) "REMINDER ARMED" else "SCHEDULED MESSAGE ARMED",
                        color = if (isCall) NeonCyan else if (isReminder) Color(0xFFFFD700) else NeonGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.8.sp
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(NeonCyan.copy(alpha = 0.2f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (isCall) "PHONE CALL" else if (isReminder) "REMINDER" else targetApp.uppercase(),
                        color = NeonCyan,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (isCall) {
                Text(
                    text = "📞 কল প্রাপক: $recipient ${if (phone.isNotBlank()) "($phone)" else ""} • সময়: $time",
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                if (spokenMessage.isNotBlank()) {
                    Text(
                        text = "🎙️ কলে যে বার্তাটি বলবে: \"$spokenMessage\"",
                        color = NeonCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Text(
                    text = "🛡️ মোবাইল স্লিপ বা লক থাকলেও AlarmManager ও WakeLock দিয়ে সঠিক সময়ে কল ডায়াল হবে এবং স্পিকারে কথাটি বলে দেওয়া হবে।",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            } else if (isReminder) {
                Text(
                    text = "⏰ রিমাইন্ডার সময়: $time",
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                if (reminder.isNotBlank()) {
                    Text(
                        text = "📌 কাজ: \"$reminder\"",
                        color = Color(0xFFFFD700),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Text(
                    text = "🔔 নির্ধারিত সময়ে রিংটোন বাজিয়ে এবং ভয়েস অ্যাসিস্ট্যান্টের মাধ্যমে স্পষ্ট কণ্ঠে আপনাকে কাজটি মনে করিয়ে দেওয়া হবে।",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            } else {
                Text(
                    text = "🎯 প্রাপক: $recipient ${if (phone.isNotBlank()) "($phone)" else ""} • সময়: $time",
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                if (message.isNotBlank()) {
                    Text(
                        text = "✉️ বার্তা: \"$message\"",
                        color = NeonGreen,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Text(
                    text = "🚀 মোবাইল স্লিপে থাকলেও স্বয়ংক্রিয়ভাবে পাঠানো হবে। নেট অফ থাকলে সরাসরি অফলাইন SMS সেলুলার ব্যাকআপ সক্রিয় থাকবে।",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }
        }
    }
}

@Composable
fun MarkdownCodeContentView(
    content: String,
    onCopyCode: (String) -> Unit
) {
    if (!content.contains("```")) {
        Text(
            text = content,
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary,
            fontSize = 14.5.sp,
            lineHeight = 22.sp
        )
        return
    }

    val parts = remember(content) {
        val list = mutableListOf<Pair<Boolean, String>>()
        val regex = Regex("```([a-zA-Z0-9_+-]*)\n?([\\s\\S]*?)```")
        var lastIndex = 0
        regex.findAll(content).forEach { match ->
            if (match.range.first > lastIndex) {
                val textChunk = content.substring(lastIndex, match.range.first).trim()
                if (textChunk.isNotBlank()) list.add(false to textChunk)
            }
            val lang = match.groupValues[1].ifBlank { "code" }
            val codeBody = match.groupValues[2]
            list.add(true to "$lang\n$codeBody")
            lastIndex = match.range.last + 1
        }
        if (lastIndex < content.length) {
            val trailingText = content.substring(lastIndex).trim()
            if (trailingText.isNotBlank()) list.add(false to trailingText)
        }
        list
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        parts.forEach { (isCode, partContent) ->
            if (!isCode) {
                Text(
                    text = partContent,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary,
                    fontSize = 14.5.sp,
                    lineHeight = 22.sp
                )
            } else {
                val firstLineBreak = partContent.indexOf('\n')
                val langTag = if (firstLineBreak != -1) partContent.substring(0, firstLineBreak).trim() else "code"
                val rawCode = if (firstLineBreak != -1) partContent.substring(firstLineBreak + 1) else partContent

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF090D18))
                        .border(1.dp, NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                ) {
                    Column {
                        // Code Header Bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF111827))
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.Code, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                                Text(
                                    text = langTag.uppercase(),
                                    color = NeonCyan,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF1E293B))
                                    .clickable { onCopyCode(rawCode) }
                                    .padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy Code", tint = TextPrimary, modifier = Modifier.size(13.dp))
                                Text("Copy Code", color = TextPrimary, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        // Code Body with horizontal scrolling
                        val scrollState = rememberScrollState()
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                                .horizontalScroll(scrollState)
                        ) {
                            Text(
                                text = rawCode,
                                color = Color(0xFFE2E8F0),
                                fontSize = 12.5.sp,
                                fontFamily = FontFamily.Monospace,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }
        }
    }
}


