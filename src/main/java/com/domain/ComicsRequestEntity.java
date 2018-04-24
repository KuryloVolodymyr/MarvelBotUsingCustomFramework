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
    private String characterName;

    @Column
    private String characterId;

    @Column
    private String lastRequest;

    public ComicsRequestEntity() {

    }

    public ComicsRequestEntity(Long chatId, String characterName, String characterId, String lastRequest) {
        this.chatId = chatId;
        this.characterName = characterName;
        this.lastRequest = lastRequest;
        this.characterId = characterId;
    }

    public Long getChatId() {
        return chatId;
    }


    public Long getId() {
        return id;
    }

    public String getCharacterName() {
        return characterName;
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

    public void setCharacterName(String characterName) {
        this.characterName = characterName;
    }

    public void setLastRequest(String lastRequest) {
        this.lastRequest = lastRequest;
    }

    public void setCharacterId(String characterId) {
        this.characterId = characterId;
    }

    public String getCharacterId() {
        return characterId;
    }
}
