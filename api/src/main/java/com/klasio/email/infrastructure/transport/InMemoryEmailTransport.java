package com.klasio.email.infrastructure.transport;

import com.klasio.email.application.EmailType;
import com.klasio.email.domain.model.OutboundEmail;
import com.klasio.email.domain.port.EmailTransport;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
@ConditionalOnProperty(name = "klasio.email.transport", havingValue = "inmemory")
public class InMemoryEmailTransport implements EmailTransport {

    private final List<OutboundEmail> recorded = new CopyOnWriteArrayList<>();

    @Override
    public void send(OutboundEmail email) {
        recorded.add(email);
    }

    public List<OutboundEmail> recordedFor(EmailType type) {
        return recorded.stream().filter(e -> e.type() == type).toList();
    }

    public List<OutboundEmail> all() {
        return List.copyOf(recorded);
    }

    public void clear() {
        recorded.clear();
    }
}
