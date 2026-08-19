package com.antifraude.licensing;

import com.antifraude.exception.BusinessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import com.antifraude.security.tenant.RlsContextService;

import java.lang.reflect.Method;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/**
 * Tests unitarios del calculo de proximaEjecucion y de la validacion de
 * frecuencia usados por el sistema de jobs locales (Fase 2). No levanta
 * Spring: invoca por reflexion los metodos privados puros del runner y del
 * controller de jobs.
 */
class LicensingJobRunnerProximaTest {

    private static final OffsetDateTime BASE = OffsetDateTime.of(2026, 8, 15, 12, 0, 0, 0, ZoneOffset.UTC);

    private static final LicensingJobRunner RUNNER = new LicensingJobRunner(
            mock(JdbcTemplate.class), mock(RlsContextService.class), null, new ObjectMapper(), null, List.of());
    private static final AdminEmpresaJobsController CONTROLLER = new AdminEmpresaJobsController(
            mock(JdbcTemplate.class), null, null, new ObjectMapper());

    @Test
    void frecuenciaEnMinutosSumaMinutos() throws Exception {
        assertThat(calcularProxima(BASE, detalle("frecuenciaValor", 5, "frecuenciaUnidad", "MINUTOS")))
                .isEqualTo(BASE.plusMinutes(5));
    }

    @Test
    void frecuenciaEnHorasSumaHoras() throws Exception {
        assertThat(calcularProxima(BASE, detalle("frecuenciaValor", 2, "frecuenciaUnidad", "HORAS")))
                .isEqualTo(BASE.plusHours(2));
    }

    @Test
    void unidadPorDefectoEsHoras() throws Exception {
        assertThat(calcularProxima(BASE, detalle("frecuenciaValor", 1)))
                .isEqualTo(BASE.plusHours(1));
    }

    @Test
    void valorInvalidoSeAjustaAMinimoUno() throws Exception {
        assertThat(calcularProxima(BASE, detalle("frecuenciaValor", 0, "frecuenciaUnidad", "HORAS")))
                .isEqualTo(BASE.plusHours(1));
    }

    @Test
    void diasSinHoraSumaDias() throws Exception {
        assertThat(calcularProxima(BASE, detalle("frecuenciaValor", 1, "frecuenciaUnidad", "DIAS")))
                .isEqualTo(BASE.plusDays(1));
    }

    @Test
    void diasConHoraPasadaMueveAlDiaSiguiente() throws Exception {
        assertThat(calcularProxima(BASE, detalle(
                "frecuenciaValor", 1, "frecuenciaUnidad", "DIAS", "hora", "08:00")))
                .isEqualTo(OffsetDateTime.of(2026, 8, 16, 8, 0, 0, 0, ZoneOffset.UTC));
    }

    @Test
    void diasConHoraFuturaSeEjecutaHoy() throws Exception {
        assertThat(calcularProxima(BASE, detalle(
                "frecuenciaValor", 1, "frecuenciaUnidad", "DIAS", "hora", "18:30")))
                .isEqualTo(OffsetDateTime.of(2026, 8, 15, 18, 30, 0, 0, ZoneOffset.UTC));
    }

    @Test
    void cronValidoCalculaSiguienteOcurrencia() throws Exception {
        OffsetDateTime proxima = calcularProxima(BASE, detalle("cron", "0 30 3 * * *"));
        assertThat(proxima.toLocalTime()).isEqualTo(LocalTime.of(3, 30));
        assertThat(proxima.isAfter(BASE)).isTrue();
    }

    @Test
    void cronInvalidoCaeEnUnaHora() throws Exception {
        assertThat(calcularProxima(BASE, detalle("cron", "esto-no-es-cron")))
                .isEqualTo(BASE.plusHours(1));
    }

    @Test
    void sinProximaEjecucionEstaVencido() throws Exception {
        assertThat(debeEjecutar(RUNNER, Map.of())).isTrue();
    }

    @Test
    void proximaFuturaNoEstaVencido() throws Exception {
        assertThat(debeEjecutar(RUNNER, detalle("proximaEjecucion", OffsetDateTime.now().plusMinutes(10).toString())))
                .isFalse();
    }

    @Test
    void proximaVencidaSiEstaVencido() throws Exception {
        assertThat(debeEjecutar(RUNNER, detalle("proximaEjecucion", OffsetDateTime.now().minusMinutes(1).toString())))
                .isTrue();
    }

    @Test
    void proximaIlegibleSeTrataComoVencido() throws Exception {
        assertThat(debeEjecutar(RUNNER, detalle("proximaEjecucion", "fecha-ilegible"))).isTrue();
    }

    @Test
    void frecuenciaValidaConCronNoExigeValor() throws Exception {
        validarFrecuencia(Map.of("cron", "0 */5 * * * *"));
    }

    @Test
    void frecuenciaValidaConValorYUnidad() throws Exception {
        validarFrecuencia(Map.of("valor", 6, "unidad", "HORAS"));
    }

    @Test
    void cronInvalidoLanzaBusinessException() {
        assertThatThrownBy(() -> validarFrecuencia(Map.of("cron", "0 0")))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo("CRON_INVALIDO");
    }

    @Test
    void valorNoPositivoLanzaBusinessException() {
        assertThatThrownBy(() -> validarFrecuencia(Map.of("valor", 0, "unidad", "HORAS")))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo("FRECUENCIA_INVALIDA");
    }

    @Test
    void unidadDesconocidaLanzaBusinessException() {
        assertThatThrownBy(() -> validarFrecuencia(Map.of("valor", 1, "unidad", "MESES")))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo("FRECUENCIA_INVALIDA");
    }

    private static OffsetDateTime calcularProxima(OffsetDateTime ahora, Map<String, Object> detalle) throws Exception {
        Method method = LicensingJobRunner.class.getDeclaredMethod("calcularProxima", OffsetDateTime.class, Map.class);
        method.setAccessible(true);
        return (OffsetDateTime) method.invoke(RUNNER, ahora, detalle);
    }

    private static boolean debeEjecutar(LicensingJobRunner runner, Map<String, Object> detalle) throws Exception {
        Method method = LicensingJobRunner.class.getDeclaredMethod("debeEjecutar", Map.class);
        method.setAccessible(true);
        return (boolean) method.invoke(RUNNER, detalle);
    }

private static void validarFrecuencia(Map<?, ?> frecuencia) {
        try {
            Method method = AdminEmpresaJobsController.class.getDeclaredMethod("validarFrecuencia", Map.class);
            method.setAccessible(true);
            method.invoke(CONTROLLER, frecuencia);
        } catch (ReflectiveOperationException exception) {
            if (exception.getCause() instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new IllegalStateException(exception);
        }
    }

    private static Map<String, Object> detalle(Object... values) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < values.length; i += 2) {
            map.put(String.valueOf(values[i]), values[i + 1]);
        }
        return map;
    }
}
