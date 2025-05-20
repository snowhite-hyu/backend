package com.snowhite.server.domain.entity;

import com.snowhite.server.domain.common.BaseEntity;
import com.snowhite.server.domain.enums.CardType;
import jakarta.persistence.*;
import lombok.Getter;

import java.io.Serializable;

@Entity
@Table(name = "cards")
@Getter
public class Card extends BaseEntity implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
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

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "image_id")
    private File file;
}
