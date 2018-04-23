package com.repository;

import com.domain.ComicsRequestEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ComicsRequestRepository extends JpaRepository<ComicsRequestEntity, Long> {

    public List<ComicsRequestEntity> getByChatId(Long chatId);
}
