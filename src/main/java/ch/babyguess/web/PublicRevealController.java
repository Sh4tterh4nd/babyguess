package ch.babyguess.web;

import java.util.Locale;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PublicRevealController {

    private final PublicRevealService revealService;

    public PublicRevealController(PublicRevealService revealService) {
        this.revealService = revealService;
    }

    @GetMapping("/results")
    String results(Model model, Locale locale) {
        try {
            model.addAttribute("reveal", revealService.view(locale));
            return "reveal";
        } catch (RevealNotPublishedException exception) {
            return "redirect:/";
        }
    }
}
