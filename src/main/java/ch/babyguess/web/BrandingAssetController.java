package ch.babyguess.web;

import ch.babyguess.branding.BrandingAssetContent;
import ch.babyguess.branding.BrandingAssetType;
import ch.babyguess.branding.BrandingService;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class BrandingAssetController {

    private final BrandingService brandingService;

    public BrandingAssetController(BrandingService brandingService) {
        this.brandingService = brandingService;
    }

    @GetMapping("/branding/logo")
    @ResponseBody
    ResponseEntity<Resource> logo() {
        return response(brandingService.activeAsset(BrandingAssetType.LOGO));
    }

    @GetMapping("/branding/background")
    @ResponseBody
    ResponseEntity<Resource> background() {
        return response(brandingService.activeAsset(BrandingAssetType.BACKGROUND));
    }

    private ResponseEntity<Resource> response(BrandingAssetContent asset) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noCache())
                .contentType(asset.mediaType())
                .body(asset.resource());
    }
}
