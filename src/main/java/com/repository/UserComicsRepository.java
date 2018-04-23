package com.repository;

import com.domain.UserComicsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface UserComicsRepository extends JpaRepository<UserComicsEntity, Long> {

    public UserComicsEntity getByChatIdAndComicsId(Long chatId, Long comicsId);

    @Transactional
    @Modifying
    @Query(value = "delete FROM user_comics where id = :id", nativeQuery = true)
    public void deleteComics(@Param("id") Long id);
}
