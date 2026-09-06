package ch.babyguess.scoring;

import java.math.BigDecimal;

public record WeightedCategory(boolean enabled, BigDecimal weight) {

    public WeightedCategory {
        if (weight == null || weight.signum() < 0) {
            throw new IllegalArgumentException("Category weight must be zero or greater");
        }
    }
}
