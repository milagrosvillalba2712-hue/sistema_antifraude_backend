package com.antifraude.common.repository;

import com.antifraude.common.entity.ClientePEP;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClientePEPRepository extends JpaRepository<ClientePEP, Long> {
    @Query(value = "SELECT * FROM cliente_pep WHERE false", nativeQuery = true)
    List<ClientePEP> findByNumeroDocumento(@Param("documento") String documento);
}
