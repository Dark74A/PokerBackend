package com.example.backend.repositories;

import com.example.backend.projections.SessionProjection;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SessionProjectionRepository
        extends MongoRepository<SessionProjection, String> {

    @Query("{ '$or': [ { 'hostId': ?0 }, { 'players.userId': ?0 } ] }")
    List<SessionProjection> findByHostIdOrParticipant(String userId);

}