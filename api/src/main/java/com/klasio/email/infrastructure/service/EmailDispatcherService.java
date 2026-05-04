package com.klasio.email.infrastructure.service;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.klasio.email.application.EmailRecipient;
import com.klasio.email.application.EmailService;
import com.klasio.email.application.EmailType;
import com.klasio.email.domain.model.*;
import com.klasio.email.domain.port.EmailTransport;
import com.klasio.email.domain.port.TemplateRenderer;
import com.klasio.email.domain.port.TenantContextPort;
import com.klasio.email.infrastructure.config.EmailProperties;
import com.klasio.email.infrastructure.config.FrontendProperties;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class EmailDispatcherService implements EmailService {

    private final EmailTransport transport;
    private final TemplateRenderer renderer;
    private final EmailProperties props;
    private final FrontendProperties frontendProps;
    private final LoadingCache<UUID, TenantContext> tenantCache;

    public EmailDispatcherService(EmailTransport transport,
                                  TemplateRenderer renderer,
                                  TenantContextPort tenantContextPort,
                                  EmailProperties props,
                                  FrontendProperties frontendProps) {
        this.transport = transport;
        this.renderer = renderer;
        this.props = props;
        this.frontendProps = frontendProps;
        this.tenantCache = Caffeine.newBuilder()
                .maximumSize(500)
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .build(tenantContextPort::findById);
    }

    @Override
    public void send(EmailType type, EmailRecipient to, UUID tenantId, Map<String, Object> params) {
        Set<String> missing = new HashSet<>(type.requiredKeys());
        missing.removeAll(params.keySet());
        if (!missing.isEmpty()) {
            throw new IllegalArgumentException(
                    "Missing required params for " + type + ": " + missing);
        }

        try {
            MDC.put("emailType", type.name());
            MDC.put("tenantId", tenantId.toString());
            MDC.put("recipientEmailHash", sha256First8(to.email()));

            TenantContext tenant = tenantCache.get(tenantId);
            EmailSender from = new EmailSender(
                    props.from().address(),
                    tenant.name() + props.from().nameSuffix());
            String idempotencyKey = UUID.randomUUID().toString();

            Locale locale = Locale.forLanguageTag(tenant.language() != null ? tenant.language() : "en");
            ZoneId zone = ZoneId.of(tenant.timezone() != null ? tenant.timezone() : "UTC");
            Map<String, Object> model = formatTemporalParams(params, zone, locale);
            model.put("tenantName", tenant.name());
            model.put("tenantSlug", tenant.slug());
            model.put("tenantLogoUrl", tenant.logoUrl());
            // Resolve the {tenantSlug} placeholder so emails contain the real
            // per-tenant URL instead of the unsubstituted template.
            String urlTemplate = frontendProps.urlTemplate();
            String tenantBaseUrl = urlTemplate.contains("{tenantSlug}")
                    ? urlTemplate.replace("{tenantSlug}", tenant.slug())
                    : urlTemplate;
            model.put("loginUrl", tenantBaseUrl + "/login");

            RenderedTemplate rendered = renderer.render(type.templateRef(), locale, model);
            OutboundEmail outbound = new OutboundEmail(type, to, from,
                    rendered.subject(), rendered.htmlBody(), rendered.textBody(),
                    idempotencyKey);
            transport.send(outbound);

        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("[EMAIL] Failed to dispatch type={} tenantId={}: {}",
                    type, tenantId, e.getMessage(), e);
        } finally {
            MDC.remove("emailType");
            MDC.remove("tenantId");
            MDC.remove("recipientEmailHash");
        }
    }

    private static Map<String, Object> formatTemporalParams(Map<String, Object> model,
                                                             ZoneId zone, Locale locale) {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy h:mm a", Locale.ENGLISH)
                .withZone(zone);
        DateTimeFormatter df = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.ENGLISH);
        Map<String, Object> result = new HashMap<>(model);
        result.replaceAll((key, value) -> {
            if (value instanceof Instant i) return dtf.format(i);
            if (value instanceof LocalDateTime ldt) return dtf.format(ldt.atZone(zone));
            if (value instanceof LocalDate ld) return df.format(ld);
            return value;
        });
        return result;
    }

    private static String sha256First8(String input) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 4; i++) sb.append(String.format("%02x", hash[i]));
            return sb.toString();
        } catch (Exception e) {
            return "unknown";
        }
    }
}
