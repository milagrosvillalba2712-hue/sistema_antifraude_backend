package com.antifraude.licensing.catalog;

import com.antifraude.common.entity.Pais;
import com.antifraude.common.repository.PaisRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class CatalogoDestinoPais implements CatalogoDestino {

    private final PaisRepository repository;

    public CatalogoDestinoPais(PaisRepository repository) {
        this.repository = repository;
    }

    @Override
    public String codigoControlPlane() {
        return "PAISES_ISO";
    }

    @Override
    public String tabla() {
        return "pais";
    }

    @Override
    public boolean existe(String codigo) {
        return repository.existsByCodigoIso(codigo);
    }

    @Override
    public String codigoOf(Map<String, Object> item) {
        return Str.code(item, "codigo", "codigoIso", "codigo_iso");
    }

    @Override
    public void upsert(Map<String, Object> item) {
        String codigo = codigoOf(item);
        if (codigo == null || codigo.isBlank()) {
            return;
        }
        Pais pais = repository.findByCodigoIso(codigo).orElseGet(Pais::new);
        pais.setCodigoIso(codigo);
        pais.setNombre(String.valueOf(item.getOrDefault("nombre", codigo)));
        if (item.containsKey("codigoIso3") || item.containsKey("codigo_iso3")) {
            pais.setCodigoIso3(Str.string(item, "codigoIso3", "codigo_iso3"));
        }
        if (item.containsKey("continente")) {
            pais.setContinente(Str.string(item, "continente"));
        }
        pais.setActivo(true);
        repository.save(pais);
    }

    @Override
    public int desactivarAusentes(List<String> codigosVigentes) {
        int desactivados = 0;
        List<Pais> todos = repository.findAll();
        for (Pais pais : todos) {
            if (pais.getActivo() && !codigosVigentes.contains(pais.getCodigoIso())) {
                pais.setActivo(false);
                repository.save(pais);
                desactivados++;
            }
        }
        return desactivados;
    }
}
