package com.antifraude.drools.similarity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NameSimilarityTest {

    @Test
    void coincidenciaExactaDevuelve100() {
        assertThat(NameSimilarity.similarity("Juan Perez Gonzalez", "Juan Perez Gonzalez")).isEqualTo(100.0);
        assertThat(NameSimilarity.similarity("juan pérez gonzález", "JUAN PEREZ GONZALEZ")).isEqualTo(100.0);
    }

    @Test
    void tipografiaCercanaSuperaUmbralTipico() {
        double sim = NameSimilarity.similarity("Juan Perez Gozalez", "Juan Perez Gonzalez");
        assertThat(sim).isGreaterThan(80.0).isLessThan(100.0);
    }

    @Test
    void nombresDistintosQuedanPorDebajoDelUmbral() {
        double sim = NameSimilarity.similarity("Maria Lopez Gomez", "Roberto Sanchez Vera");
        assertThat(sim).isLessThan(50.0);
    }

    @Test
    void nuloOBlancoDevuelveCero() {
        assertThat(NameSimilarity.similarity(null, "JUAN")).isEqualTo(0.0);
        assertThat(NameSimilarity.similarity("JUAN", null)).isEqualTo(0.0);
        assertThat(NameSimilarity.similarity("   ", "   ")).isEqualTo(0.0);
    }

    @Test
    void normalizacionIgnoraAcentosYPuntuacion() {
        assertThat(NameSimilarity.normalize("José María, S.A.")).isEqualTo("JOSE MARIA S A");
    }
}
