package com.domain;

import javax.persistence.*;

@Entity
@Table(name = "rating")
public class HeroesRatingEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column
    private Long chatID;

    @Column
    private String heroName;

    @Column
    private Boolean rating;

    @ManyToOne
    @JoinColumn(name = "userId")
    private UserEntity user;

    public HeroesRatingEntity(String heroName, Long chatID, Boolean rating) {
        this.heroName = heroName;
        this.chatID = chatID;
        this.rating = rating;
    }

    public HeroesRatingEntity() {
    }


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getHeroName() {
        return heroName;
    }

    public void setHeroName(String heroName) {
        this.heroName = heroName;
    }

    public Long getChatID() {
        return chatID;
    }

    public void setChatID(Long chatID) {
        this.chatID = chatID;
    }

    public Boolean getRating() {
        return rating;
    }

    public void setRating(Boolean rating) {
        this.rating = rating;
    }

    public UserEntity getUser() {
        return user;
    }

    public void setUser(UserEntity user) {
        this.user = user;
    }
}
