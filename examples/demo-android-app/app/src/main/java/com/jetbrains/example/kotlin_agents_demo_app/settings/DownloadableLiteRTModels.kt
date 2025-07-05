package com.jetbrains.example.kotlin_agents_demo_app.settings

import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.llm.LiteRTModels

data class DownloadableLiteRTModel(
    val llModel: LLModel, // Contains id, displayName, capabilities from LiteRTModels
    val downloadUrl: String,
    val fileName: String, // Expected filename after download, e.g., "gemma_2b_it_cpu.task"
    val expectedSizeInBytes: Long? = null // Optional, for display or verification
)

object SupportedLiteRTModels {
    // Hugging Face base URL for litert-community models (adjust if models are elsewhere)
    // Example URL structure: https://huggingface.co/google/gemma-3n-E2B-it-litert-preview/resolve/main/gemma-3n-E2B-it-int4.task

    val gemma3nE2B: DownloadableLiteRTModel = DownloadableLiteRTModel(
        llModel = LiteRTModels.Gemma3n.E2B,
        // Ensure this URL is valid and points to the actual .task file
        downloadUrl = "https://huggingface.co/google/gemma-3n-E2B-it-litert-preview/resolve/main/gemma-3n-E2B-it-int4.task",
        fileName = "gemma-3n-E2B-it-int4.task",
        expectedSizeInBytes = 3136226711L // From model_allowlist.json example
    )

    val gemma3nE4B: DownloadableLiteRTModel = DownloadableLiteRTModel(
        llModel = LiteRTModels.Gemma3n.E4B,
        downloadUrl = "https://huggingface.co/google/gemma-3n-E4B-it-litert-preview/resolve/main/gemma-3n-E4B-it-int4.task",
        fileName = "gemma-3n-E4B-it-int4.task",
        expectedSizeInBytes = 4405655031L // From model_allowlist.json example
    )

    // Add other models here, e.g., smaller Gemma 1B if available and suitable for testing
    // val gemma1B: DownloadableLiteRTModel = DownloadableLiteRTModel(
    //     llModel = LiteRTModels.Gemma1B, // Assuming Gemma1B is defined in LiteRTModels
    //     downloadUrl = "https://huggingface.co/litert-community/Gemma3-1B-IT/resolve/main/Gemma3-1B-IT_multi-prefill-seq_q4_ekv2048.task",
    //     fileName = "Gemma3-1B-IT_q4.task",
    //     expectedSizeInBytes = 554661246L
    // )


    val all: List<DownloadableLiteRTModel> = listOf(
        gemma3nE2B,
        gemma3nE4B
        // gemma1B
    )
}
