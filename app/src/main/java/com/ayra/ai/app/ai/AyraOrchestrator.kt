package com.ayra.ai.app.ai

import android.content.Context
import com.ayra.ai.app.ai.providers.AyraError
import com.ayra.ai.app.ai.providers.AyraStreamEvent
import com.ayra.ai.app.ai.providers.ChatRequest
import com.ayra.ai.app.ai.providers.ProviderPool
import com.ayra.ai.app.ai.providers.ProviderPoolMode
import com.ayra.ai.app.automation.DeviceAutomationController
import com.ayra.ai.app.automation.ProposedAction
import com.ayra.ai.app.data.ChatMessage
import com.ayra.ai.app.voice.CrisisDetector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

sealed class AyraEvent {
    data class ActionRequired(val action: ProposedAction) : AyraEvent()
    data class Status(val message: String) : AyraEvent()
    data class Token(val text: String) : AyraEvent()
    data class Complete(val response: AiResponse) : AyraEvent()
    data class Error(val error: AyraError) : AyraEvent()
}

/**
 * Section 2: Unified Orchestrator pipeline for Ayra AI.
 * Single entry point ensuring:
 * 1. Crisis / Safety checks run first.
 * 2. Device hardware automation triggers ActionGate confirmation card.
 * 3. Tool intents classified with zero false positives.
 * 4. General questions routed through ProviderPool with streaming tokens.
 * 5. Complete, non-template answers.
 */
class AyraOrchestrator(private val context: Context) {

    private val providerPool = ProviderPool(context)
    private val automationController = DeviceAutomationController(context)
    private val unifiedAiEngine = UnifiedAiEngine(context)

    fun handle(
        prompt: String,
        persona: String = "AI_ASSISTANT",
        tier: String = "auto",
        history: List<ChatMessage> = emptyList(),
        imageBase64: String? = null,
        documentText: String? = null,
        documentName: String? = null
    ): Flow<AyraEvent> = flow {
        val trimmed = prompt.trim()
        if (trimmed.isEmpty()) return@flow

        // 1. Safety & Crisis Pre-checks (Blocking, deterministic, immediate)
        val crisisResult = CrisisDetector.evaluate(trimmed)
        if (crisisResult.isCrisis && crisisResult.supportiveMessage != null) {
            emit(AyraEvent.Complete(AiResponse(text = crisisResult.supportiveMessage, modelUsed = "Ayra Safety Guardian 🛡️")))
            return@flow
        }

        // 2. Scheduled Task / Device Automation Pre-checks -> ActionGate Confirmation
        val proposedAction = automationController.proposeAction(trimmed)
        if (proposedAction != null) {
            emit(AyraEvent.ActionRequired(proposedAction))
            return@flow
        }

        // 3. Tool Intent Classification (Strict)
        val toolIntent = AyraIntentClassifier.classifyIntent(trimmed)

        when (toolIntent) {
            AyraToolIntent.PDF -> {
                emit(AyraEvent.Status("Generating PDF Document..."))
                val pdfResp = unifiedAiEngine.generateUnifiedResponse(
                    prompt = trimmed,
                    personaMode = persona,
                    modelTier = tier,
                    attachedDocumentText = documentText,
                    attachedDocumentName = documentName,
                    attachedImageBase64 = imageBase64
                )
                emit(AyraEvent.Complete(pdfResp))
                return@flow
            }
            AyraToolIntent.TERMINAL -> {
                val cmd = trimmed.removePrefix("/terminal").removePrefix("terminal:").trim()
                val isBengali = trimmed.any { it in '\u0980'..'\u09FF' }
                val reply = if (isBengali) {
                    "🛡️ **Ayra Cyber Security Educational Terminal & Defensive Lab** সক্রিয় হয়েছে। অনুমোদিত ডায়াগনস্টিক চালান:"
                } else {
                    "🛡️ **Ayra Cyber Security Educational Terminal & Defensive Lab** active. Executing diagnostics:"
                }
                emit(AyraEvent.Complete(
                    AiResponse(
                        text = reply,
                        craftType = "CYBER_TERMINAL",
                        craftPayload = cmd,
                        modelUsed = "Ayra Terminal Engine"
                    )
                ))
                return@flow
            }
            AyraToolIntent.IMAGE -> {
                emit(AyraEvent.Status("Synthesizing image prompt..."))
                val imgResp = unifiedAiEngine.generateUnifiedResponse(
                    prompt = trimmed,
                    personaMode = persona,
                    modelTier = tier,
                    attachedImageBase64 = imageBase64
                )
                emit(AyraEvent.Complete(imgResp))
                return@flow
            }
            AyraToolIntent.DEEP_RESEARCH -> {
                emit(AyraEvent.Status("Conducting grounded multi-source research..."))
                val researchResp = unifiedAiEngine.generateUnifiedResponse(
                    prompt = trimmed,
                    personaMode = persona,
                    modelTier = tier,
                    attachedDocumentText = documentText,
                    attachedImageBase64 = imageBase64
                )
                emit(AyraEvent.Complete(researchResp))
                return@flow
            }
            AyraToolIntent.CHAT -> {
                // Main CHAT & Coding execution through ProviderPool
                val poolMode = when (tier) {
                    "omniroute", "quality" -> ProviderPoolMode.BEST
                    "fast" -> ProviderPoolMode.FAST
                    else -> ProviderPoolMode.AUTO
                }

                val request = ChatRequest(
                    prompt = trimmed,
                    persona = persona,
                    mode = poolMode,
                    history = history,
                    imageBase64 = imageBase64,
                    documentText = documentText
                )

                providerPool.streamChat(request).collect { streamEvent ->
                    when (streamEvent) {
                        is AyraStreamEvent.Thinking -> {
                            emit(AyraEvent.Status("Thinking with ${streamEvent.model}..."))
                        }
                        is AyraStreamEvent.Token -> {
                            emit(AyraEvent.Token(streamEvent.text))
                        }
                        is AyraStreamEvent.Draft -> {
                            emit(AyraEvent.Status("Consensus draft received from ${streamEvent.model}"))
                        }
                        is AyraStreamEvent.Complete -> {
                            emit(AyraEvent.Complete(
                                AiResponse(
                                    text = streamEvent.response.text,
                                    modelUsed = streamEvent.response.modelUsed
                                )
                            ))
                        }
                        is AyraStreamEvent.Error -> {
                            emit(AyraEvent.Error(streamEvent.error))
                        }
                    }
                }
            }
        }
    }.flowOn(Dispatchers.IO)
}
