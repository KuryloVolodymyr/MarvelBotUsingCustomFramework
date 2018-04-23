package com.repository;

import com.domain.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {

    public UserEntity getByChatID(Long chatId);

    public UserEntity findByChatID(Long chatId);

}
