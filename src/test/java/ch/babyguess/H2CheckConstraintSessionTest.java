package ch.babyguess;

import static org.assertj.core.api.Assertions.assertThatCode;

import java.sql.DriverManager;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class H2CheckConstraintSessionTest {

    @Test
    void checkConstraintCanBeEvaluatedByAnotherConnection() throws Exception {
        var databaseName = "check-constraint-" + UUID.randomUUID();
        var url = "jdbc:h2:mem:" + databaseName + ";DB_CLOSE_DELAY=-1";

        // Keep the database alive while the connection that compiled the constraint is
        // closed. H2 2.4.240 incorrectly keeps that closed session in the expression.
        try (var sentinel = DriverManager.getConnection(url);
                var schemaConnection = DriverManager.getConnection(url)) {
            try (var statement = schemaConnection.createStatement()) {
                statement.execute("""
                        CREATE TABLE prediction (
                            predicted_sex VARCHAR(16)
                                CHECK (predicted_sex IN ('GIRL', 'BOY'))
                        )
                        """);
            }

            schemaConnection.close();

            assertThatCode(() -> {
                        try (var writeConnection = DriverManager.getConnection(url);
                                var statement = writeConnection.prepareStatement(
                                        "INSERT INTO prediction (predicted_sex) VALUES (?)")) {
                            statement.setString(1, "GIRL");
                            statement.executeUpdate();
                        }
                    })
                    .doesNotThrowAnyException();
        }
    }
}
