package com.klasio.email.domain.port;

import com.klasio.email.domain.model.RenderedTemplate;
import java.util.Map;

public interface TemplateRenderer {
    RenderedTemplate render(String templatePath, Map<String, Object> model);
}
