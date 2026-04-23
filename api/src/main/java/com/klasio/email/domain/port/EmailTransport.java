package com.klasio.email.domain.port;

import com.klasio.email.domain.model.OutboundEmail;

public interface EmailTransport {
    void send(OutboundEmail email);
}
