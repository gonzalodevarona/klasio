package com.klasio.email.domain.model;

import java.util.UUID;

public record TenantContext(UUID id, String slug, String name) {}
