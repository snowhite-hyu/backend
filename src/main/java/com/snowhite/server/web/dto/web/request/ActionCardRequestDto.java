package com.snowhite.server.web.dto.web.request;

import com.snowhite.server.domain.enums.ActionCardType;
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
