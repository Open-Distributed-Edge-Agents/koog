package ai.koog.prompt.llm

public object LiteRTModels {
    public object Gemma {
        public val GEMMA_3_1B: LLModel = LLModel(
            id = "gemma-3-1b-it",
            provider = LLMProvider.LiteRT,
            family = "Gemma",
            capabilities = setOf(
                LLMCapability.Completion,
                LLMCapability.Vision,
            ),
        )

        public val GEMMA_3N_E2B: LLModel = LLModel(
            id = "gemma-3n-e2b-it",
            provider = LLMProvider.LiteRT,
            family = "Gemma",
            capabilities = setOf(
                LLMCapability.Completion,
                LLMCapability.Vision,
            ),
        )

        public val GEMMA_3N_E4B: LLModel = LLModel(
            id = "gemma-3n-e4b-it",
            provider = LLMProvider.LiteRT,
            family = "Gemma",
            capabilities = setOf(
                LLMCapability.Completion,
                LLMCapability.Vision,
            ),
        )
    }
}
