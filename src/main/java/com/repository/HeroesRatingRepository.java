package com.repository;

import com.domain.HeroesRatingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Set;

@Repository
public interface HeroesRatingRepository extends JpaRepository<HeroesRatingEntity, Long> {

    public HeroesRatingEntity getByHeroNameAndChatID(String heroName, Long chatId);

    @Query(value = "select rating.hero_name from rating where rating = 1  group by (hero_name)" +
            " order by count(rating.rating) desc limit 10", nativeQuery = true)
    public Set<String> getTopHeroesForQuickReply();

    @Query(value = "SELECT count(rating) FROM internship.rating where hero_name = :heroName and rating = true", nativeQuery = true)
    public Long getLikesForHero(@Param("heroName") String heroName);

    @Query(value = "SELECT count(rating) FROM internship.rating where hero_name = :heroName  and rating = false", nativeQuery = true)
    public Long getDisLikesForHero(@Param("heroName") String heroName);
}
