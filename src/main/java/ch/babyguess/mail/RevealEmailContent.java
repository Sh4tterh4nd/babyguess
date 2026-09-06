package ch.babyguess.mail;

import java.net.URI;
import java.util.List;

public record RevealEmailContent(
        String actualName,
        List<String> actualDetails,
        List<String> categoryResults,
        URI leaderboardLink) {
}
