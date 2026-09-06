package ch.babyguess.scoring;

import java.math.BigDecimal;

public record ToleratedCategory(boolean enabled, BigDecimal weight, int tolerance) {

    public ToleratedCategory {
        if (weight == null || weight.signum() < 0) {
            throw new IllegalArgumentException("Category weight must be zero or greater");
        }
        if (tolerance < 0) {
            throw new IllegalArgumentException("Category tolerance must be zero or greater");
        }
    }
}
