package com.example.barbearia.mail;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PixPayloadServiceTest {

    @Test
    void chaveEmvUsaCpf06587872174() {
        PixPayloadService service = new PixPayloadService();
        ReflectionTestUtils.setField(service, "pixChave", "06587872174");
        ReflectionTestUtils.setField(service, "pixTipoChave", "CPF");
        ReflectionTestUtils.setField(service, "pixNome", "Street Barber");
        ReflectionTestUtils.setField(service, "pixCidade", "GOIANIA");

        assertThat(service.getChaveExibicao()).isEqualTo("06587872174");
        assertThat(service.getPixChave()).isEqualTo("06587872174");

        var pix = service.gerar(new BigDecimal("35.00"), UUID.randomUUID());
        String chaveNoPayload = extrairChavePix(pix.copiaCola());
        assertThat(chaveNoPayload).isEqualTo("06587872174");
        assertThat(chaveNoPayload).doesNotContain("+55");
        assertThat(pix.copiaCola()).contains("540535.00");
    }

    @Test
    void chaveEmvUsaTelefoneQuandoConfigurado() {
        PixPayloadService service = new PixPayloadService();
        ReflectionTestUtils.setField(service, "pixChave", "65987872174");
        ReflectionTestUtils.setField(service, "pixTipoChave", "TELEFONE");
        ReflectionTestUtils.setField(service, "pixNome", "Street Barber");
        ReflectionTestUtils.setField(service, "pixCidade", "GOIANIA");

        assertThat(service.getPixChave()).isEqualTo("+5565987872174");

        var pix = service.gerar(new BigDecimal("10.00"), UUID.randomUUID());
        assertThat(extrairChavePix(pix.copiaCola())).isEqualTo("+5565987872174");
    }

    private static String extrairChavePix(String payload) {
        int i = 0;
        while (i + 4 <= payload.length()) {
            String tag = payload.substring(i, i + 2);
            int len = Integer.parseInt(payload.substring(i + 2, i + 4));
            i += 4;
            String value = payload.substring(i, i + len);
            i += len;
            if ("26".equals(tag)) {
                return extrairSubcampo01(value);
            }
        }
        throw new IllegalArgumentException("merchant account 26 não encontrado");
    }

    private static String extrairSubcampo01(String merchantAccount) {
        int j = 0;
        while (j + 4 <= merchantAccount.length()) {
            String tag = merchantAccount.substring(j, j + 2);
            int len = Integer.parseInt(merchantAccount.substring(j + 2, j + 4));
            j += 4;
            String value = merchantAccount.substring(j, j + len);
            j += len;
            if ("01".equals(tag)) {
                return value;
            }
        }
        throw new IllegalArgumentException("subcampo 01 não encontrado");
    }
}
