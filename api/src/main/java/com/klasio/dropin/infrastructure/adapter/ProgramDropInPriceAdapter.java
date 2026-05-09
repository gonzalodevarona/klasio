package com.klasio.dropin.infrastructure.adapter;

import com.klasio.dropin.domain.port.DropInPriceLookupPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class ProgramDropInPriceAdapter implements DropInPriceLookupPort {

    private final JdbcTemplate jdbc;

    public ProgramDropInPriceAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<BigDecimal> findPrice(UUID tenantId, UUID programId) {
        List<BigDecimal> results = jdbc.query(
                "SELECT drop_in_price FROM programs WHERE id = ? AND tenant_id = ?",
                (rs, rowNum) -> rs.getBigDecimal("drop_in_price"),
                programId, tenantId);
        if (results.isEmpty()) return Optional.empty();
        return Optional.ofNullable(results.get(0));  // null if drop_in_price IS NULL in DB
    }
}
