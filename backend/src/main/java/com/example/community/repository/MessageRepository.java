package com.example.community.repository;

import com.example.community.domain.Member;
import com.example.community.domain.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageRepository extends JpaRepository<Message, Long> {
    Page<Message> findByReceiverOrderByCreatedAtDesc(Member receiver, Pageable pageable);
    Page<Message> findBySenderOrderByCreatedAtDesc(Member sender, Pageable pageable);
    Page<Message> findByReceiverAndReadFalseOrderByCreatedAtDesc(Member receiver, Pageable pageable);
    long countByReceiverAndReadFalse(Member receiver);
}

