package com.antifraude.common.repository;

import com.antifraude.common.entity.Canal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CanalRepository extends JpaRepository<Canal, Long> {
    Optional<Canal> findByCodigo(String codigo);
    Optional<Canal> findByNombre(String nombre);
}
