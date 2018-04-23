package com.domain;

import javax.persistence.*;

@Entity
@Table(name = "request")
public class UserRequestEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column
    private String heroName;

    @Column
    private Long heroId;

    @Column
    private Long chatId;

    @ManyToOne
    @JoinColumn(name = "userId")
    private UserEntity user;


    public UserRequestEntity(String heroName, Long heroId, Long chatId){
        this.heroId = heroId;
        this.heroName = heroName;
        this.chatId = chatId;
    }

    public UserRequestEntity(){}

    public String getHeroName() {
        return heroName;
    }

    public void setHeroName(String heroName) {
        this.heroName = heroName;
    }

    public Long getHeroId() {
        return heroId;
    }

    public void setHeroId(Long heroId) {
        this.heroId = heroId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public UserEntity getUser() {
        return user;
    }

    public void setUser(UserEntity user) {
        this.user = user;
    }

    public Long getChatId() {
        return chatId;
    }

    public void setChatId(Long chatId) {
        this.chatId = chatId;
    }
}
