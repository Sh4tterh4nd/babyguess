package ch.babyguess.web.admin;

import ch.babyguess.branding.BrandingAssetType;
import ch.babyguess.branding.BrandingUploadException;
import ch.babyguess.event.StaleEventConfigurationException;
import ch.babyguess.branding.BrandingService;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AdminBrandingController {

    private final BrandingService brandingService;

    public AdminBrandingController(BrandingService brandingService) {
        this.brandingService = brandingService;
    }

    @GetMapping("/admin/branding")
    String branding(Model model) {
        model.addAttribute("branding", brandingService.presentation());
        return "admin/branding";
    }

    @PostMapping("/admin/branding/logo")
    String replaceLogo(
            @RequestParam long version,
            @RequestParam("file") MultipartFile file,
            RedirectAttributes redirectAttributes) {
        return replace(version, BrandingAssetType.LOGO, file, redirectAttributes);
    }

    @PostMapping("/admin/branding/background")
    String replaceBackground(
            @RequestParam long version,
            @RequestParam("file") MultipartFile file,
            RedirectAttributes redirectAttributes) {
        return replace(version, BrandingAssetType.BACKGROUND, file, redirectAttributes);
    }

    @PostMapping("/admin/branding/logo/remove")
    String removeLogo(@RequestParam long version, RedirectAttributes redirectAttributes) {
        return remove(version, BrandingAssetType.LOGO, redirectAttributes);
    }

    @PostMapping("/admin/branding/background/remove")
    String removeBackground(@RequestParam long version, RedirectAttributes redirectAttributes) {
        return remove(version, BrandingAssetType.BACKGROUND, redirectAttributes);
    }

    private String replace(
            long version,
            BrandingAssetType type,
            MultipartFile file,
            RedirectAttributes redirectAttributes) {
        try {
            brandingService.replace(version, type, file);
            redirectAttributes.addFlashAttribute("brandingSaved", type.name());
        } catch (BrandingUploadException exception) {
            redirectAttributes.addFlashAttribute("brandingError", exception.getProblem().name());
        } catch (StaleEventConfigurationException | OptimisticLockingFailureException exception) {
            redirectAttributes.addFlashAttribute("brandingError", "STALE");
        }
        return "redirect:/admin/branding";
    }

    private String remove(
            long version,
            BrandingAssetType type,
            RedirectAttributes redirectAttributes) {
        try {
            brandingService.remove(version, type);
            redirectAttributes.addFlashAttribute("brandingRemoved", type.name());
        } catch (StaleEventConfigurationException | OptimisticLockingFailureException exception) {
            redirectAttributes.addFlashAttribute("brandingError", "STALE");
        }
        return "redirect:/admin/branding";
    }
}
