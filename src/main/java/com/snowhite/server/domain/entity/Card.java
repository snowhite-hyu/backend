package com.snowhite.server.domain.entity;

import com.snowhite.server.domain.common.BaseEntity;
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
public class Card implements Serializable {

    public Card(int id, String name, CardType type) {
        this.id = id;
        this.name = name;
        this.type = type;
    }

    @Id
    private int id;

    @Column(name = "card_name", nullable = false)
    private String name;

    @Column(name = "card_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private CardType type;

    @Column(name = "up_open")
    private boolean upOpen;

    @Column(name = "down_open")
    private boolean downOpen;

    @Column(name = "left_open")
    private boolean leftOpen;

    @Column(name = "right_open")
    private boolean rightOpen;

    @Column(name = "middle_open")
    private boolean middleOpen;
}
