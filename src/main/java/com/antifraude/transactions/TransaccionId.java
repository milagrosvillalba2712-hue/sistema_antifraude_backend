package com.antifraude.transactions;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransaccionId implements Serializable {
    private Long id;
    private LocalDateTime fechaTransaccion;
}
