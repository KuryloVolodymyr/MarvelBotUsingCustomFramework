package com.domain;

import org.hibernate.annotations.GeneratorType;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "user")
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column
    private Long chatID;

    @Column
    private String firstName;

    @Column
    private String lastName;

    @Column
    private String state;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "user")
    private List<UserRequestEntity> requests = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<HeroesRatingEntity> ratings;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<UserComicsEntity> comics;



    public UserEntity(Long chatID) {
        this.chatID = chatID;
        this.state = "default";
    }

    public UserEntity(Long chatID, String firstName, String lastName) {
        this.chatID = chatID;
        this.state = "default";
        this.firstName = firstName;
        this.lastName = lastName;
    }

    public UserEntity(Long chatID, String state, String firstName, String lastName) {
        this.chatID = chatID;
        this.state = state;
        this.firstName = firstName;
        this.lastName = lastName;
    }

    public UserEntity() {
        super();
    }


    public String getFirstName() {
        return firstName;
    }

    public List<UserRequestEntity> getRequests() {
        return requests;
    }

    public List<HeroesRatingEntity> getRatings() {
        return ratings;
    }

    public Long getChatID() {
        return chatID;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public void setRequests(List<UserRequestEntity> requests) {
        this.requests = requests;
    }

    public void setRatings(List<HeroesRatingEntity> ratings) {
        this.ratings = ratings;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getLastName() {
        return lastName;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public List<UserComicsEntity> getComics() {
        return comics;
    }

    public void setComics(List<UserComicsEntity> comics) {
        this.comics = comics;
    }
}
