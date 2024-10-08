package com.example.demo.note;

import com.example.demo.note.model.Note;
import com.example.demo.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NoteRepository extends JpaRepository<Note, String> {
    @Query("from Note  n where (lower(n.title) like lower(:query) or lower(n.content) like lower(:query)) and (n.user.id = :userId)")
    List<Note> findbyContentList(@Param("userId") Long userId, @Param("query") String query);

    List<Note> findByUser(User user);
}

