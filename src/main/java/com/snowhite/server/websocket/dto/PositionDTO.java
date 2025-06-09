package com.snowhite.server.websocket.dto;

public record PositionDTO(
        int row,
        int column
) {
    public static PositionDTO of(int row, int column) {
        return new PositionDTO(row, column);
    }
}
