package com.werewolfengine.ai.api;

/**
 * AI decision with observability source (PRD §4.5.4: LLM primary, Mock fallback).
 */
public record DecisionResult(
        PlayerIntent intent,
        Source source,
        String modelId
) {
    public enum Source {
        /** LangChain4j response parsed and legal. */
        LLM,
        /** LLM disabled or unavailable; Mock only. */
        MOCK_ONLY,
        /** LLM failed or illegal; Mock §4.5.4 fallback. */
        MOCK_FALLBACK
    }

    public static final String MOCK_ONLY_MODEL = "mock-only";
    public static final String MOCK_FALLBACK_MODEL = "mock-fallback";
}
