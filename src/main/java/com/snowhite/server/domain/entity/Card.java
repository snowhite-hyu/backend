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
public class Card implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "card_name", nullable = false)
    private String name;

    @Column(name = "card_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private CardType type;
}