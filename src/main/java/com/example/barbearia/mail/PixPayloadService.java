package com.example.barbearia.mail;



import org.springframework.beans.factory.annotation.Value;

import org.springframework.stereotype.Service;

import org.springframework.util.StringUtils;



import java.math.BigDecimal;

import java.net.URLEncoder;

import java.nio.charset.StandardCharsets;

import java.text.Normalizer;

import java.util.Locale;

import java.util.UUID;



@Service

public class PixPayloadService {



    @Value("${app.pix.chave:06587872174}")

    private String pixChave;



    @Value("${app.pix.tipo-chave:CPF}")

    private String pixTipoChave;



    @Value("${app.pix.nome:Street Barber}")

    private String pixNome;



    @Value("${app.pix.cidade:GOIANIA}")

    private String pixCidade;



    public PixTipoChave getTipoChave() {

        return PixTipoChave.fromConfig(pixTipoChave);

    }



    /** Chave como o cliente deve ver/copiar. */

    public String getChaveExibicao() {

        return switch (getTipoChave()) {

            case CPF -> normalizarCpf(pixChave);

            case TELEFONE -> formatarTelefoneLocal(pixChave);

            case EMAIL -> pixChave.trim().toLowerCase(Locale.ROOT);

            case ALEATORIA -> pixChave.trim();

        };

    }



    public String getPixChave() {

        return normalizarChaveEmv(pixChave);

    }



    public String getRotuloTipoChave() {

        return switch (getTipoChave()) {

            case CPF -> "CPF";

            case TELEFONE -> "telefone";

            case EMAIL -> "e-mail";

            case ALEATORIA -> "chave aleatória";

        };

    }



    /** Lê o valor (tag 54) do payload PIX copia e cola. */

    public static BigDecimal extrairValorDoPayload(String copiaCola) {

        if (!StringUtils.hasText(copiaCola)) {

            return null;

        }

        String payload = copiaCola.trim();

        int i = 0;

        while (i + 4 <= payload.length()) {

            String tag = payload.substring(i, i + 2);

            int len;

            try {

                len = Integer.parseInt(payload.substring(i + 2, i + 4));

            } catch (NumberFormatException e) {

                return null;

            }

            i += 4;

            if (i + len > payload.length()) {

                return null;

            }

            String value = payload.substring(i, i + len);

            i += len;

            if ("54".equals(tag)) {

                try {

                    return new BigDecimal(value);

                } catch (NumberFormatException e) {

                    return null;

                }

            }

        }

        return null;

    }



    public PixDados gerar(BigDecimal valor, UUID referencia) {

        String chaveEmv = normalizarChaveEmv(pixChave);

        String txid = "AG" + referencia.toString().replace("-", "").substring(0, 20);

        String payload = montarPayload(valor, txid, chaveEmv);

        String qrUrl = "https://api.qrserver.com/v1/create-qr-code/?size=320x320&data="

                + URLEncoder.encode(payload, StandardCharsets.UTF_8);

        return new PixDados(getChaveExibicao(), payload, qrUrl);

    }



    private String normalizarChaveEmv(String raw) {

        return switch (getTipoChave()) {

            case CPF -> normalizarCpf(raw);

            case TELEFONE -> normalizarTelefoneEmv(raw);

            case EMAIL -> raw.trim().toLowerCase(Locale.ROOT);

            case ALEATORIA -> raw.trim();

        };

    }



    /** CPF: 11 dígitos, sem pontuação (padrão PIX). */

    private static String normalizarCpf(String raw) {

        String digits = apenasDigitos(raw);

        if (digits.length() != 11) {

            throw new IllegalArgumentException("Chave PIX CPF deve ter 11 dígitos");

        }

        return digits;

    }



    /** Telefone: +55 + DDD + número. */

    private static String normalizarTelefoneEmv(String raw) {

        String digits = apenasDigitos(raw);

        if (digits.startsWith("55") && digits.length() >= 12) {

            return "+" + digits;

        }

        if (digits.startsWith("0") && digits.length() == 11) {

            return "+55" + digits.substring(1);

        }

        if (digits.length() == 10 || digits.length() == 11) {

            String semZero = digits.startsWith("0") ? digits.substring(1) : digits;

            return "+55" + semZero;

        }

        return raw.trim();

    }



    private static String formatarTelefoneLocal(String raw) {

        String digits = apenasDigitos(raw);

        if (digits.startsWith("55") && digits.length() >= 12) {

            return "0" + digits.substring(2);

        }

        if (digits.length() == 10 || digits.length() == 11) {

            return digits.startsWith("0") ? digits : "0" + digits;

        }

        return raw.trim();

    }



    private static String apenasDigitos(String s) {

        if (!StringUtils.hasText(s)) {

            return "";

        }

        return s.replaceAll("\\D", "");

    }



    private String montarPayload(BigDecimal valor, String txid, String chaveEmv) {

        String nome = sanitizar(pixNome, 25);

        String cidade = sanitizar(pixCidade, 15);

        String valorStr = String.format(Locale.US, "%.2f", valor);



        String merchantAccount = tlv("00", "br.gov.bcb.pix") + tlv("01", chaveEmv);

        String merchantAccountGui = tlv("26", merchantAccount);



        String payloadSemCrc = tlv("00", "01")

                + merchantAccountGui

                + tlv("52", "0000")

                + tlv("53", "986")

                + tlv("54", valorStr)

                + tlv("58", "BR")

                + tlv("59", nome)

                + tlv("60", cidade)

                + tlv("62", tlv("05", txid))

                + "6304";



        String crc = crc16(payloadSemCrc);

        return payloadSemCrc + crc;

    }



    private static String tlv(String id, String value) {

        if (!StringUtils.hasText(value)) {

            return "";

        }

        return id + String.format("%02d", value.length()) + value;

    }



    private static String sanitizar(String s, int max) {

        String n = Normalizer.normalize(s, Normalizer.Form.NFD)

                .replaceAll("\\p{M}", "")

                .replaceAll("[^a-zA-Z0-9 ]", "")

                .toUpperCase(Locale.ROOT)

                .trim();

        return n.length() > max ? n.substring(0, max) : n;

    }



    private static String crc16(String payload) {

        int crc = 0xFFFF;

        for (int i = 0; i < payload.length(); i++) {

            crc ^= payload.charAt(i) << 8;

            for (int j = 0; j < 8; j++) {

                if ((crc & 0x8000) != 0) {

                    crc = (crc << 1) ^ 0x1021;

                } else {

                    crc <<= 1;

                }

            }

        }

        return String.format("%04X", crc & 0xFFFF);

    }



    public record PixDados(String chave, String copiaCola, String qrCodeUrl) {

    }

}


