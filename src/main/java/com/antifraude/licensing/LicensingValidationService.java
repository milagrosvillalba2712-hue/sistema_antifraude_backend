package com.antifraude.licensing;

import com.antifraude.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Politica de validacion de licencias locales. Evalua vigencia del lease,
 * firma y conectividad (online vs offline).
 *
 * Resultados: OPERATIVO (lease vigente), SOLO_LECTURA (lease vencido pero
 * dentro del periodo de gracia, o sin conectividad con stock de gracia) y
 * BLOQUEADO (vencido sin gracia o revocado). Distingue explicitamente
 * "sin conectividad" de "vencida/revocada" (ADR-002).
 */
@Service
public class LicensingValidationService {

    private static final Logger log = LoggerFactory.getLogger(LicensingValidationService.class);

    private final LicenciaLocalRepository licenciaRepository;
    private final InstalacionLocalRepository instalacionRepository;
    private final LicenseCryptoService cryptoService;

    public LicensingValidationService(LicenciaLocalRepository licenciaRepository,
                                      InstalacionLocalRepository instalacionRepository,
                                      LicenseCryptoService cryptoService) {
        this.licenciaRepository = licenciaRepository;
        this.instalacionRepository = instalacionRepository;
        this.cryptoService = cryptoService;
    }

    @Transactional(readOnly = true)
    public ResultadoValidacion validar(UUID instalacionId, boolean online) {
        LicenciaLocal licencia = licenciaRepository.findTopByInstalacionIdOrderByEmitidaEnDesc(instalacionId)
                .orElseThrow(() -> new BusinessException("LICENCIA_NO_EMITIDA",
                        "No existe licencia emitida para la instalacion"));
        InstalacionLocal instalacion = instalacionRepository.findById(instalacionId)
                .orElseThrow(() -> new BusinessException("INSTALACION_DESCONOCIDA",
                        "Instalacion no registrada"));

        if (LicensingLocalService.INSTALACION_REVOCADA.equals(instalacion.getEstado())
                || LicensingLocalService.LICENCIA_REVOCADA.equals(licencia.getEstado())) {
            return ResultadoValidacion.bloqueado(licencia,
                    "REVOCADA", "Instalacion o licencia revocada");
        }

        boolean firmaValida = cryptoService.verificar(licencia.getLeasePayload(),
                licencia.getLeaseFirma(), instalacion.getFingerprintHash(),
                licencia.getKidFirma(), instalacion.getClavePublicaPem());
        if (!firmaValida) {
            return ResultadoValidacion.bloqueado(licencia,
                    "FIRMA_INVALIDA", "El lease no supero la verificacion criptografica");
        }

        OffsetDateTime limiteGracia = licencia.getVenceEn()
                .plusDays(licencia.getDiasGracia() != null ? licencia.getDiasGracia() : 0);

        if (OffsetDateTime.now().isBefore(licencia.getVenceEn())) {
            return ResultadoValidacion.operativo(licencia, online, firmaValida);
        }
        if (OffsetDateTime.now().isBefore(limiteGracia)) {
            return ResultadoValidacion.soloLectura(licencia, online, firmaValida);
        }
        return ResultadoValidacion.bloqueado(licencia,
                "SIN_GRACIA", "Lease vencido sin dias de gracia disponibles");
    }

    public static int frecuenciaDias(String planCodigo) {
        return switch (planCodigo) {
            case "BASICO" -> 30;
            case "ESTANDAR" -> 15;
            default -> 7;
        };
    }

    public enum Modo {
        OPERATIVO, SOLO_LECTURA, BLOQUEADO
    }

    public record ResultadoValidacion(UUID instalacionId, UUID licenciaId, Modo modo,
                                      String motivo, String detalle, boolean firmaValida,
                                      boolean online, OffsetDateTime venceEn,
                                      OffsetDateTime ultimaValidacionEn) {

        static ResultadoValidacion operativo(LicenciaLocal licencia, boolean online, boolean firmaValida) {
            return new ResultadoValidacion(licencia.getInstalacion().getId(), licencia.getId(),
                    Modo.OPERATIVO, "VIGENTE", "Lease vigente", firmaValida, online,
                    licencia.getVenceEn(), OffsetDateTime.now());
        }

        static ResultadoValidacion soloLectura(LicenciaLocal licencia, boolean online, boolean firmaValida) {
            return new ResultadoValidacion(licencia.getInstalacion().getId(), licencia.getId(),
                    Modo.SOLO_LECTURA, "EN_GRACIA",
                    online ? "Lease vencido dentro del periodo de gracia"
                            : "Sin conectividad dentro del periodo de gracia",
                    firmaValida, online, licencia.getVenceEn(), OffsetDateTime.now());
        }

        static ResultadoValidacion bloqueado(LicenciaLocal licencia, String motivo, String detalle) {
            return new ResultadoValidacion(licencia.getInstalacion().getId(), licencia.getId(),
                    Modo.BLOQUEADO, motivo, detalle, false, false,
                    licencia.getVenceEn(), OffsetDateTime.now());
        }
    }
}
