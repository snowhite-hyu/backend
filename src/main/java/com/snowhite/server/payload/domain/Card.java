package com.snowhite.server.payload.domain;

import com.snowhite.server.payload.domain.common.BaseEntity;
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

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "image_id")
    private File file;
}
