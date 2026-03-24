package com.example.community.repository;

import com.example.community.domain.Member;
import com.example.community.domain.Post;
import com.example.community.domain.PostLike;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PostLikeRepository extends JpaRepository<PostLike, Long> {
    Optional<PostLike> findByPostAndMember(Post post, Member member);
    boolean existsByPostAndMember(Post post, Member member);
    long countByPost(Post post);
    void deleteByPost(Post post);
}
