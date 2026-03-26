package com.klasio.programclass.application.port.input;

import com.klasio.programclass.application.dto.ClassDetail;

import java.util.UUID;

public interface GetClassDetailUseCase {
    ClassDetail execute(UUID tenantId, UUID classId);
}
