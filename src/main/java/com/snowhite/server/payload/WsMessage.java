package com.snowhite.server.payload;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@JsonPropertyOrder({"type", "payload"})
public class WsMessage<T> {
    private String type;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private T payload;

    public static <T> WsMessage<T> onSuccess(String type, T payload) {
        return new WsMessage<>(type, payload);
    }
}
