package com.antifraude.licensing.catalog;

import com.antifraude.common.entity.Moneda;
import com.antifraude.common.repository.MonedaRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class CatalogoDestinoMoneda implements CatalogoDestino {

    private final MonedaRepository repository;

    public CatalogoDestinoMoneda(MonedaRepository repository) {
        this.repository = repository;
    }

    @Override
    public String codigoControlPlane() {
        return "MONEDAS_ISO";
    }

    @Override
    public String tabla() {
        return "moneda";
    }

    @Override
    public boolean existe(String codigo) {
        return repository.findByCodigoIso(codigo).isPresent();
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
        Moneda moneda = repository.findByCodigoIso(codigo).orElseGet(Moneda::new);
        moneda.setCodigoIso(codigo);
        moneda.setNombre(String.valueOf(item.getOrDefault("nombre", codigo)));
        moneda.setActivo(true);
        repository.save(moneda);
    }

    @Override
    public int desactivarAusentes(List<String> codigosVigentes) {
        int desactivados = 0;
        List<Moneda> todas = repository.findAll();
        for (Moneda moneda : todas) {
            if (moneda.getActivo() && !codigosVigentes.contains(moneda.getCodigoIso())) {
                moneda.setActivo(false);
                repository.save(moneda);
                desactivados++;
            }
        }
        return desactivados;
    }
}
