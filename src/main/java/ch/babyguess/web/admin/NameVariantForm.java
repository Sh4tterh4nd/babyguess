package ch.babyguess.web.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class NameVariantForm {

    @NotBlank(message = "{validation.required}")
    @Size(max = 160, message = "{validation.name.size}")
    private String variant;

    public String getVariant() { return variant; }
    public void setVariant(String variant) { this.variant = variant; }
}
