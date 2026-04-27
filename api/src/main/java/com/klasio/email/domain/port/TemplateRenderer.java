package com.klasio.email.domain.port;

import com.klasio.email.domain.model.RenderedTemplate;
import java.util.Locale;
import java.util.Map;

public interface TemplateRenderer {
    RenderedTemplate render(String templatePath, Locale locale, Map<String, Object> model);
}
