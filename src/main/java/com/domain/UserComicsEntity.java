package com.domain;

import javax.persistence.*;

@Entity
@Table(name = "userComics")
public class UserComicsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column
    private Long chatId;

    @Column
    private Long comicsId;

    @Column
    private String comicsName;

    @Column
    private String comicsInfoUrl;

    @Column
    private String imageUrl;

    @ManyToOne
    @JoinColumn(name = "userId")
    private UserEntity user;

    public UserComicsEntity(Long comicsId, String comicsName, String comicsInfoUrl, String imageUrl, Long chatId) {
        this.comicsId = comicsId;
        this.comicsName = comicsName;
        this.comicsInfoUrl = comicsInfoUrl;
        this.imageUrl = imageUrl;
        this.chatId = chatId;
    }

    public UserComicsEntity() {

    }

    public Long getId() {
        return id;
    }

    public UserEntity getUser() {
        return user;
    }

    public Long getComicsId() {
        return comicsId;
    }

    public String getComicsInfoUrl() {
        return comicsInfoUrl;
    }

    public String getComicsName() {
        return comicsName;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setUser(UserEntity user) {
        this.user = user;
    }

    public void setComicsId(Long comicsId) {
        this.comicsId = comicsId;
    }

    public void setComicsInfoUrl(String comicsInfoUrl) {
        this.comicsInfoUrl = comicsInfoUrl;
    }

    public void setComicsName(String comicsName) {
        this.comicsName = comicsName;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public Long getChatId() {
        return chatId;
    }

    public void setChatId(Long chatId) {
        this.chatId = chatId;
    }
}
