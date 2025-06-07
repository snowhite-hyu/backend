package com.snowhite.server.domain.entity;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.snowhite.server.domain.enums.CardType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Entity
@Table(name = "cards")
@Getter
@AllArgsConstructor
@NoArgsConstructor
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "type"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = StartCard.class, name = "START"),
        @JsonSubTypes.Type(value = PathCard.class, name = "PATH"),
        @JsonSubTypes.Type(value = DestinationCard.class, name = "DESTINATION"),
        @JsonSubTypes.Type(value = ActionCard.class, name = "ACTION")
})
public class Card implements Serializable {

    @Id
    private Integer id;

    @Column(name = "card_name", nullable = false)
    private String name;

    @Column(name = "card_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private CardType type;
}