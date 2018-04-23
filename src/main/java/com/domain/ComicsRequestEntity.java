package com.domain;

import javax.persistence.*;

@Entity
@Table(name = "comics_request")
public class ComicsRequestEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column
    private Long chatId;

    @Column
    private String heroName;

    @Column
    private String lastRequest;

    public ComicsRequestEntity() {

    }

    public ComicsRequestEntity(Long chatId, String heroName, String lastRequest) {
        this.chatId = chatId;
        this.heroName = heroName;
        this.lastRequest = lastRequest;
    }

    public Long getChatId() {
        return chatId;
    }


    public Long getId() {
        return id;
    }

    public String getHeroName() {
        return heroName;
    }

    public String getLastRequest() {
        return lastRequest;
    }

    public void setChatId(Long chatId) {
        this.chatId = chatId;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setHeroName(String heroName) {
        this.heroName = heroName;
    }

    public void setLastRequest(String lastRequest) {
        this.lastRequest = lastRequest;
    }
}
