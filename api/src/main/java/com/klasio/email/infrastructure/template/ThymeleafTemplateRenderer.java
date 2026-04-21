package com.klasio.email.infrastructure.template;

import com.klasio.email.domain.model.RenderedTemplate;
import com.klasio.email.domain.port.TemplateRenderer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Component
public class ThymeleafTemplateRenderer implements TemplateRenderer {

    private final TemplateEngine htmlEngine;
    private final TemplateEngine textEngine;

    public ThymeleafTemplateRenderer(
            @Qualifier("emailHtmlEngine") TemplateEngine htmlEngine,
            @Qualifier("emailTextEngine") TemplateEngine textEngine) {
        this.htmlEngine = htmlEngine;
        this.textEngine = textEngine;
    }

    @Override
    public RenderedTemplate render(String templatePath, Map<String, Object> model) {
        Context ctx = new Context(Locale.ENGLISH, model);
        // Use the fragment-selector overload so the suffix is applied only to the
        // template name and not to the "name :: fragment" composite string.
        String subject = htmlEngine.process(templatePath, Set.of("subject"), ctx).strip();
        String htmlBody = htmlEngine.process(templatePath, ctx);
        String textBody = textEngine.process(templatePath, ctx);
        return new RenderedTemplate(subject, htmlBody, textBody);
    }
}
