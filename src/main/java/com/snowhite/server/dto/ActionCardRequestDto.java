package com.snowhite.server.dto;

import com.snowhite.server.domain.ActionCardType;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public class ActionCardRequestDto {
    private ActionCardType actionCardType;
    private Long playerId;
    private Integer locationX;
    private Integer locationY;
}
