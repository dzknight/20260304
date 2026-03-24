package com.example.community.repository;

import com.example.community.domain.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostRepository extends JpaRepository<Post, Long> {
    @Query(
        value = """
            select p
            from Post p
            join p.author a
            where :q is null or :q = '' or
              lower(p.title) like lower(concat('%', :q, '%')) or
              lower(p.content) like lower(concat('%', :q, '%')) or
              lower(a.username) like lower(concat('%', :q, '%')) or
              lower(a.nickname) like lower(concat('%', :q, '%'))
            """,
        countQuery = """
            select count(p)
            from Post p
            join p.author a
            where :q is null or :q = '' or
              lower(p.title) like lower(concat('%', :q, '%')) or
              lower(p.content) like lower(concat('%', :q, '%')) or
              lower(a.username) like lower(concat('%', :q, '%')) or
              lower(a.nickname) like lower(concat('%', :q, '%'))
            """
    )
    Page<Post> search(@Param("q") String q, Pageable pageable);
}

