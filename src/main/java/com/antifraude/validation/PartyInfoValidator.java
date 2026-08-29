package com.antifraude.validation;

import com.antifraude.dto.TransaccionRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Locale;

public class PartyInfoValidator implements ConstraintValidator<ValidPartyInfo, TransaccionRequest> {

    @Override
    public boolean isValid(TransaccionRequest request, ConstraintValidatorContext context) {
        if (request == null) {
            return true;
        }
        boolean ok = true;
        ok &= validateParte(context, request.personaRemitenteId(),
                request.tipoPersonaRemitente(), request.nombreCompletoRemitente(),
                "tipoPersonaRemitente", "nombreCompletoRemitente", "remitente");
        ok &= validateParte(context, request.personaBeneficiarioId(),
                request.tipoPersonaBeneficiario(), request.nombreCompletoBeneficiario(),
                "tipoPersonaBeneficiario", "nombreCompletoBeneficiario", "beneficiario");
        return ok;
    }

    private boolean validateParte(ConstraintValidatorContext context, Long personaId,
                                  String tipoPersona, String nombreCompleto,
                                  String tipoField, String nombreField, String parte) {
        if (personaId != null) {
            return true;
        }
        boolean ok = true;
        String tipo = tipoPersona == null ? null : tipoPersona.trim().toUpperCase(Locale.ROOT);
        if (tipo == null || tipo.isEmpty()) {
            context.buildConstraintViolationWithTemplate(
                            "El tipo de persona del " + parte + " es obligatorio")
                    .addPropertyNode(tipoField).addConstraintViolation();
            ok = false;
        } else if (!"FISICA".equals(tipo) && !"JURIDICA".equals(tipo)) {
            context.buildConstraintViolationWithTemplate(
                            "El tipo de persona del " + parte + " debe ser FISICA o JURIDICA")
                    .addPropertyNode(tipoField).addConstraintViolation();
            ok = false;
        }
        String nombre = nombreCompleto == null ? null : nombreCompleto.trim();
        if (nombre == null || nombre.isEmpty()) {
            context.buildConstraintViolationWithTemplate(
                            "La identidad completa del " + parte + " es obligatoria")
                    .addPropertyNode(nombreField).addConstraintViolation();
            ok = false;
        } else if ("FISICA".equals(tipo) && nombre.split("\\s+").length < 2) {
            context.buildConstraintViolationWithTemplate(
                            "La identidad del " + parte + " (persona fisica) debe incluir nombre y apellido")
                    .addPropertyNode(nombreField).addConstraintViolation();
            ok = false;
        }
        return ok;
    }
}
