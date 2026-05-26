package com.chatapp.server.repository;

import com.chatapp.server.model.Message;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MessageRepository extends JpaRepository<Message, Long> {

    @Query("""
            select m from Message m
            where m.timestamp >= :since
              and m.recipient is null
            order by m.timestamp asc
            """)
    List<Message> findGroupHistory(@Param("since") long since);

    @Query("""
            select m from Message m
            where m.timestamp >= :since
              and m.recipient is not null
              and (m.sender = :username or m.recipient = :username)
            order by m.timestamp asc
            """)
    List<Message> findPeerHistory(@Param("username") String username, @Param("since") long since);

    @Query("""
            select m from Message m
            where m.timestamp >= :since
              and (m.recipient is null or m.sender = :username or m.recipient = :username)
            order by m.timestamp asc
            """)
    List<Message> findAllVisibleHistory(@Param("username") String username, @Param("since") long since);

    @Query("""
            select m from Message m
            where m.timestamp < :cutoff
            order by m.timestamp asc
            """)
    List<Message> findExpired(@Param("cutoff") long cutoff);

    Optional<Message> findByFileId(String fileId);
}
