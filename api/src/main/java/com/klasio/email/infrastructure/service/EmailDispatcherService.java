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
import com.klasio.email.infrastructure.config.BrevoProperties;
import com.klasio.email.infrastructure.config.EmailProperties;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class EmailDispatcherService implements EmailService {

    private final EmailTransport transport;
    private final TemplateRenderer renderer;
    private final EmailProperties props;
    private final BrevoProperties brevoProps;
    private final LoadingCache<UUID, TenantContext> tenantCache;
    private final Set<EmailType> warnedMissingTemplate = ConcurrentHashMap.newKeySet();

    public EmailDispatcherService(EmailTransport transport,
                                  TemplateRenderer renderer,
                                  TenantContextPort tenantContextPort,
                                  EmailProperties props,
                                  BrevoProperties brevoProps) {
        this.transport = transport;
        this.renderer = renderer;
        this.props = props;
        this.brevoProps = brevoProps;
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

            OutboundEmail outbound = buildOutbound(type, to, from, tenant, params, idempotencyKey);
            transport.send(outbound);

        } catch (IllegalArgumentException e) {
            throw e; // propagate missing-param errors (fail fast)
        } catch (Exception e) {
            log.error("[EMAIL] Failed to dispatch type={} tenantId={}: {}",
                    type, tenantId, e.getMessage(), e);
        } finally {
            MDC.remove("emailType");
            MDC.remove("tenantId");
            MDC.remove("recipientEmailHash");
        }
    }

    private OutboundEmail buildOutbound(EmailType type, EmailRecipient to, EmailSender from,
                                        TenantContext tenant, Map<String, Object> params,
                                        String idempotencyKey) {
        if (type.source() == EmailType.Source.IN_REPO) {
            Map<String, Object> model = new HashMap<>(params);
            model.put("tenantName", tenant.name());
            model.put("tenantSlug", tenant.slug());
            RenderedTemplate rendered = renderer.render(type.templateRef(), model);
            return new OutboundEmail(type, to, from,
                    rendered.subject(), rendered.htmlBody(), rendered.textBody(),
                    null, null, idempotencyKey);
        }

        Long templateId = brevoProps.templateIds().get(type.templateRef());
        if (templateId == null || templateId == 0L) {
            if (warnedMissingTemplate.add(type)) {
                log.warn("[EMAIL] No Brevo template ID configured for {}, falling back to missing-template-fallback", type);
            }
            Map<String, Object> model = new HashMap<>(params);
            model.put("tenantName", tenant.name());
            model.put("emailTypeName", type.name());
            RenderedTemplate fallback = renderer.render("missing-template-fallback", model);
            return new OutboundEmail(type, to, from,
                    fallback.subject(), fallback.htmlBody(), fallback.textBody(),
                    null, null, idempotencyKey);
        }

        Map<String, Object> brevoParams = new HashMap<>(params);
        brevoParams.put("tenantName", tenant.name());
        return new OutboundEmail(type, to, from,
                null, null, null, templateId, brevoParams, idempotencyKey);
    }

    private static String sha256First8(String input) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(input.getBytes());
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 4; i++) sb.append(String.format("%02x", hash[i]));
            return sb.toString();
        } catch (Exception e) {
            return "unknown";
        }
    }
}
