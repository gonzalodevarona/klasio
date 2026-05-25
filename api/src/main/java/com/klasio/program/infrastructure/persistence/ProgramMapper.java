package com.klasio.program.infrastructure.persistence;

import com.klasio.program.domain.model.Program;
import com.klasio.program.domain.model.ProgramId;
import com.klasio.program.domain.model.ProgramStatus;
import org.springframework.stereotype.Component;

@Component
public class ProgramMapper {

    public Program toDomain(ProgramJpaEntity entity) {
        return Program.reconstitute(
                ProgramId.of(entity.getId()),
                entity.getTenantId(),
                entity.getName(),
                ProgramStatus.valueOf(entity.getStatus()),
                entity.getDropInPrice(),
                entity.getCreatedAt(),
                entity.getCreatedBy(),
                entity.getUpdatedAt(),
                entity.getUpdatedBy()
        );
    }

    public ProgramJpaEntity toEntity(Program program) {
        ProgramJpaEntity entity = new ProgramJpaEntity();
        entity.setId(program.getId().value());
        entity.setTenantId(program.getTenantId());
        entity.setName(program.getName());
        entity.setStatus(program.getStatus().name());
        entity.setDropInPrice(program.getDropInPrice());
        entity.setCreatedAt(program.getCreatedAt());
        entity.setCreatedBy(program.getCreatedBy());
        entity.setUpdatedAt(program.getUpdatedAt());
        entity.setUpdatedBy(program.getUpdatedBy());
        return entity;
    }
}
