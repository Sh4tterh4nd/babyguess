package ch.babyguess;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:babyguess-test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.username=sa",
        "spring.datasource.password="
})
class BabyGuessApplicationTest {

    @Test
    void applicationContextStartsAndMigrationRuns() {
    }
}
