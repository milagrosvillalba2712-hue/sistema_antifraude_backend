package com.antifraude.drools;

import com.antifraude.common.entity.SujetoRiesgo;
import com.antifraude.common.repository.PaisRiesgoRepository;
import com.antifraude.common.repository.SujetoRiesgoAliasRepository;
import com.antifraude.common.repository.SujetoRiesgoDocumentoRepository;
import com.antifraude.common.repository.SujetoRiesgoRepository;
import com.antifraude.drools.fact.CoincidenciaListaFact;
import com.antifraude.licensing.Empresa;
import com.antifraude.lists.ElementoListaControlCliente;
import com.antifraude.lists.ElementoListaControlClienteRepository;
import com.antifraude.lists.ListaControlCliente;
import com.antifraude.lists.ListaControlService;
import com.antifraude.transactions.Transaccion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ListScreeningServiceFuzzyTest {

    private SujetoRiesgoRepository sujetoRepo;
    private SujetoRiesgoAliasRepository aliasRepo;
    private SujetoRiesgoDocumentoRepository docRepo;
    private PaisRiesgoRepository paisRepo;
    private ListaControlService listaControlService;
    private ElementoListaControlClienteRepository elementoRepo;
    private ListScreeningService service;

    @BeforeEach
    void setUp() {
        sujetoRepo = mock(SujetoRiesgoRepository.class);
        aliasRepo = mock(SujetoRiesgoAliasRepository.class);
        docRepo = mock(SujetoRiesgoDocumentoRepository.class);
        paisRepo = mock(PaisRiesgoRepository.class);
        listaControlService = mock(ListaControlService.class);
        elementoRepo = mock(ElementoListaControlClienteRepository.class);

        service = new ListScreeningService(sujetoRepo, aliasRepo, docRepo, paisRepo,
                listaControlService, elementoRepo);
        ReflectionTestUtils.setField(service, "nameFuzzyEnabled", true);
        ReflectionTestUtils.setField(service, "nameFuzzyThreshold", 70);

        // Comportamiento por defecto: sin coincidencias exactas ni candidatos.
        when(sujetoRepo.findByNombreNormalizadoAndActivoTrue(anyString())).thenReturn(List.of());
        when(aliasRepo.findByAliasNormalizadoAndActivoTrue(anyString())).thenReturn(List.of());
        when(sujetoRepo.findByActivoTrue()).thenReturn(List.of());
        when(aliasRepo.findByActivoTrue()).thenReturn(List.of());
        when(docRepo.findByNumeroDocumentoAndActivoTrue(anyString())).thenReturn(List.of());
        when(paisRepo.findActiveByPaisCodigoIso(anyString())).thenReturn(List.of());
        when(listaControlService.buscarCoincidencias(any(), any())).thenReturn(List.of());
        when(elementoRepo.buscarActivosNoWhitelist(any(), any())).thenReturn(List.of());
    }

    private Transaccion txConNombre(String nombreRemitente, String nombreBeneficiario) {
        Transaccion t = new Transaccion();
        t.setRemitenteNombreCompleto(nombreRemitente);
        t.setBeneficiarioNombreCompleto(nombreBeneficiario);
        return t;
    }

    private SujetoRiesgo sujeto(Long id, String nombreNormalizado) {
        SujetoRiesgo s = new SujetoRiesgo();
        s.setId(id);
        s.setNombreNormalizado(nombreNormalizado);
        s.setNombreOriginal(nombreNormalizado);
        s.setTipoSujeto("PERSONA");
        s.setCategoria("NEGRA");
        s.setSeveridad("CRITICA");
        return s;
    }

    @Test
    void fuzzySobreNombreDeSujetoGeneraCoincidenciaDifusa() {
        SujetoRiesgo s = sujeto(1L, "MARIA LOPEZ GOMEZ");
        when(sujetoRepo.findByActivoTrue()).thenReturn(List.of(s));

        List<CoincidenciaListaFact> matches = service.screen(txConNombre("Maria Lopez Gomes", null));

        assertThat(matches).hasSize(1);
        CoincidenciaListaFact match = matches.get(0);
        assertThat(match.getScoreMatch()).isGreaterThan(BigDecimal.valueOf(70))
                .isLessThan(BigDecimal.valueOf(100));
        assertThat(match.getDescripcion()).contains("difusa");
        assertThat(match.getSujetoRiesgoId()).isEqualTo(1L);
    }

    @Test
    void coincidenciaExactaNoSeDuplicaPorFuzzy() {
        SujetoRiesgo s = sujeto(2L, "JUAN PEREZ");
        when(sujetoRepo.findByNombreNormalizadoAndActivoTrue("JUAN PEREZ")).thenReturn(List.of(s));
        when(sujetoRepo.findByActivoTrue()).thenReturn(List.of(s));

        List<CoincidenciaListaFact> matches = service.screen(txConNombre("Juan Perez", null));

        assertThat(matches).hasSize(1);
        assertThat(matches.get(0).getScoreMatch()).isEqualByComparingTo(BigDecimal.valueOf(100));
        assertThat(matches.get(0).getDescripcion()).contains("exacta");
    }

    @Test
    void nombrePorDebajoDelUmbralNoCoincide() {
        SujetoRiesgo s = sujeto(3L, "ROBERTO SANCHEZ VERA");
        when(sujetoRepo.findByActivoTrue()).thenReturn(List.of(s));

        List<CoincidenciaListaFact> matches = service.screen(txConNombre("Maria Lopez Gomez", null));

        assertThat(matches).isEmpty();
    }

    @Test
    void fuzzyEnListaDeControlDelClienteGeneraCoincidencia() {
        UUID empresaId = UUID.randomUUID();
        Empresa empresa = mock(Empresa.class);
        when(empresa.getId()).thenReturn(empresaId);

        ListaControlCliente lista = new ListaControlCliente();
        lista.setTipoLista(ListaControlCliente.TipoListaControl.BLACKLIST);
        lista.setCodigo("LCB");

        ElementoListaControlCliente elemento = new ElementoListaControlCliente();
        elemento.setId(10L);
        elemento.setLista(lista);
        elemento.setTipoEntidad(ElementoListaControlCliente.TipoEntidadControl.PERSONA);
        elemento.setTipoIdentificador(ElementoListaControlCliente.TipoIdentificadorControl.NOMBRE);
        elemento.setValorOriginal("Maria Lopez Gomez");
        elemento.setValorNormalizado("MARIA LOPEZ GOMEZ");
        elemento.setSeveridad("Crítica");

        when(elementoRepo.buscarActivosNoWhitelist(empresaId, ElementoListaControlCliente.TipoIdentificadorControl.NOMBRE))
                .thenReturn(List.of(elemento));

        Transaccion t = txConNombre(null, "Maria Lopez Gomes");
        t.setEmpresa(empresa);

        List<CoincidenciaListaFact> matches = service.screen(t);

        assertThat(matches).hasSize(1);
        CoincidenciaListaFact match = matches.get(0);
        assertThat(match.getScoreMatch()).isGreaterThan(BigDecimal.valueOf(70))
                .isLessThan(BigDecimal.valueOf(100));
        assertThat(match.getDescripcion()).contains("difusa");
        assertThat(match.getCampoEvaluado()).isEqualTo("NOMBRE");
    }

    @Test
    void fuzzyDeshabilitadoNoGeneraCoincidenciasDifusas() {
        ReflectionTestUtils.setField(service, "nameFuzzyEnabled", false);
        SujetoRiesgo s = sujeto(1L, "MARIA LOPEZ GOMEZ");
        when(sujetoRepo.findByActivoTrue()).thenReturn(List.of(s));

        List<CoincidenciaListaFact> matches = service.screen(txConNombre("Maria Lopez Gomes", null));

        assertThat(matches).isEmpty();
    }
}
