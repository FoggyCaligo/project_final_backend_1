package com.today.fridge.recommendation.service;

import org.springframework.stereotype.Service;

@Service
public class HybridRankingService {

    private static final double RULE_WEIGHT = 0.35;
    private static final double SEMANTIC_WEIGHT = 0.20;
    private static final double TAG_WEIGHT = 0.45;

    public double calculateHybridScore(
            double ruleScore,
            double semanticScore,
            double tagScore
    ) {
        return
                ruleScore * RULE_WEIGHT
              + semanticScore * 100.0 * SEMANTIC_WEIGHT
              + tagScore * TAG_WEIGHT;
    }

    public double toSemanticScore(double distance) {
        return 1.0 / (1.0 + distance);
    }
}