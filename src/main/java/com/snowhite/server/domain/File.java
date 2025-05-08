package com.snowhite.server.domain;

import com.snowhite.server.domain.common.BaseEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "files")
public class File extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name = "file_name", nullable = false)
    private String name;

    @Column(name = "file_extension", nullable = false)
    private String extension;

    @Column(name = "file_size", nullable = false)
    private long size;

    @Column(name = "file_path", nullable = false)
    private String path;


}
