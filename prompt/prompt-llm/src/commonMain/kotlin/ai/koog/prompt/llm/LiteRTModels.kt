package ai.koog.prompt.llm

/**
 * Represents a collection of predefined LiteRT models.
 */
public object LiteRTModels {

    /**
     * Google's Gemma 3n models for LiteRT.
     */
    public object Gemma3n {
        /**
         * Gemma-3n E2B (English, 2 Billion parameters)
         * A 2B parameter model from the Gemma-3n family, optimized for LiteRT.
         * Supports text generation and vision capabilities (multimodal).
         */
        public val E2B: LLModel = LLModel(
            provider = LLMProvider.LiteRT,
            id = "gemma-3n-e2b",
            displayName = "Gemma-3n E2B",
            capabilities = listOf(
                LLMCapability.Text,
                LLMCapability.Vision, // Assuming Gemma-3n supports vision
                LLMCapability.Temperature, // Common parameter
                LLMCapability.TopK // Common parameter
            )
        )

        /**
         * Gemma-3n E4B (English, 4 Billion parameters)
         * A 4B parameter model from the Gemma-3n family, optimized for LiteRT.
         * Supports text generation and vision capabilities (multimodal).
         */
        public val E4B: LLModel = LLModel(
            provider = LLMProvider.LiteRT,
            id = "gemma-3n-e4b",
            displayName = "Gemma-3n E4B",
            capabilities = listOf(
                LLMCapability.Text,
                LLMCapability.Vision, // Assuming Gemma-3n supports vision
                LLMCapability.Temperature, // Common parameter
                LLMCapability.TopK // Common parameter
            )
        )
    }
}
