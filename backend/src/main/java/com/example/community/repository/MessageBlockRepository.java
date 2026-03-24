package com.example.community.repository;

import com.example.community.domain.Member;
import com.example.community.domain.MessageBlock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MessageBlockRepository extends JpaRepository<MessageBlock, Long> {
    boolean existsByOwnerAndBlockedMember(Member owner, Member blockedMember);

    void deleteByOwnerAndBlockedMember(Member owner, Member blockedMember);

    List<MessageBlock> findByOwnerOrderByCreatedAtDesc(Member owner);
}
