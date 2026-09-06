package ch.babyguess.scoring;

import java.time.LocalDate;

public record ActualBaby(String name, Sex sex, LocalDate birthDate, Integer birthWeightGrams) {

    public ActualBaby {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("The actual name is required");
        }
        if (birthWeightGrams != null && birthWeightGrams <= 0) {
            throw new IllegalArgumentException("Actual birth weight must be positive");
        }
    }
}
