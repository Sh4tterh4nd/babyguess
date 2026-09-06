package ch.babyguess.branding;

import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;

public record BrandingAssetContent(Resource resource, MediaType mediaType) {
}
