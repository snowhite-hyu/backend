package com.snowhite.server.domain.entity;

import com.snowhite.server.domain.common.BaseEntity;
import com.snowhite.server.domain.enums.CardType;
import jakarta.persistence.*;

@Entity
@Table(name = "cards")
public class Card extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name = "card_name", nullable = false)
    private String name;

    @Column(name = "card_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private CardType type;

    @Column(name = "upOpen")
    private boolean upOpen;

    @Column(name = "upOpen")
    private boolean downOpen;

    @Column(name = "upOpen")
    private boolean leftOpen;

    @Column(name = "upOpen")
    private boolean rightOpen;

    @Column(name = "upOpen")
    private boolean middleOpen;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "image_id")
    private File file;
}
