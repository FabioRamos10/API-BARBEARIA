package com.example.barbearia.mail;

import com.example.barbearia.domain.Agendamento;
import com.example.barbearia.domain.Mensagem;
import com.example.barbearia.domain.StatusAgendamento;
import com.example.barbearia.repository.PagamentoRepository;
import com.example.barbearia.service.AgendamentoDetalheHelper;
import com.example.barbearia.service.NotificacaoService;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Monta HTML responsivo (tema escuro + verde neon + fundo estelar) para os e-mails da barbearia.
 */
@Component
public class EmailTemplateEngine {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final String layoutHtml;
    private final AgendamentoDetalheHelper agendamentoDetalheHelper;
    private final PixPayloadService pixPayloadService;
    private final PagamentoRepository pagamentoRepository;

    public EmailTemplateEngine(
            ResourceLoader resourceLoader,
            AgendamentoDetalheHelper agendamentoDetalheHelper,
            PixPayloadService pixPayloadService,
            PagamentoRepository pagamentoRepository
    ) {
        this.agendamentoDetalheHelper = agendamentoDetalheHelper;
        this.pixPayloadService = pixPayloadService;
        this.pagamentoRepository = pagamentoRepository;
        try (var in = resourceLoader.getResource("classpath:email/layout.html").getInputStream()) {
            this.layoutHtml = StreamUtils.copyToString(in, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao carregar email/layout.html", e);
        }
    }

    public String renderHtml(Mensagem mensagem, Agendamento ag) {
        String conteudo = mensagem.getConteudo();
        if (conteudo.startsWith(NotificacaoService.PREFIX_AVISO_BARBEIRO_NOVO)) {
            return fillLayout(
                    "Novo horário na sua agenda",
                    "Cópia para o profissional",
                    "Agenda",
                    accentNovo(),
                    corpoBarbeiroNovo(ag)
            );
        }
        if (conteudo.startsWith(NotificacaoService.PREFIX_AVISO_BARBEIRO_CANCEL)) {
            return fillLayout(
                    "Agendamento cancelado",
                    "Cópia para o profissional",
                    "Cancelado",
                    accentCancelado(),
                    corpoBarbeiroCancelado(ag)
            );
        }
        if (conteudo.startsWith(NotificacaoService.PREFIX_AVISO_BARBEIRO_STATUS)) {
            StatusAgendamento st = ag.getStatus();
            return fillLayout(
                    "Status atualizado",
                    "Cópia para o profissional",
                    st.name(),
                    accentStatus(st),
                    corpoBarbeiroStatus(ag, st)
            );
        }
        if (conteudo.startsWith(NotificacaoService.PREFIX_NOVO)) {
            return fillLayout(
                    "Novo agendamento",
                    "Seu horário está confirmado",
                    "Novo",
                    accentNovo(),
                    corpoNovo(ag)
            );
        }
        if (conteudo.startsWith(NotificacaoService.PREFIX_LEMBRETE)) {
            return fillLayout(
                    "Lembrete de agendamento",
                    "Estamos te esperando",
                    "Lembrete",
                    accentLembrete(),
                    corpoLembrete(ag)
            );
        }
        if (conteudo.startsWith(NotificacaoService.PREFIX_CANCELADO)) {
            return fillLayout(
                    "Agendamento cancelado",
                    "Seu horário foi liberado",
                    "Cancelado",
                    accentCancelado(),
                    corpoCancelado(ag)
            );
        }
        if (conteudo.startsWith(NotificacaoService.PREFIX_STATUS)) {
            StatusAgendamento st = ag.getStatus();
            return fillLayout(
                    "Atualização do agendamento",
                    "Status do seu horário",
                    st.name(),
                    accentStatus(st),
                    corpoStatus(ag, st)
            );
        }
        if (conteudo.startsWith(NotificacaoService.PREFIX_AVISO_STAFF_NOVO)) {
            return fillLayout(
                    "Novo agendamento",
                    "Cliente na agenda do profissional",
                    "Gestão",
                    accentNovo(),
                    corpoStaffGenerico(ag, "Um cliente foi vinculado a um horário na agenda do barbeiro.")
            );
        }
        if (conteudo.startsWith(NotificacaoService.PREFIX_AVISO_STAFF_CANCEL)) {
            return fillLayout(
                    "Cancelamento",
                    "Horário liberado na agenda",
                    "Gestão",
                    accentCancelado(),
                    corpoStaffGenerico(ag, "Um agendamento foi cancelado e a agenda foi atualizada.")
            );
        }
        if (conteudo.startsWith(NotificacaoService.PREFIX_ATRASO_CLIENTE)) {
            return fillLayout(
                    "Atraso registrado",
                    "Recebemos seu aviso",
                    "Atraso",
                    accentLembrete(),
                    corpoAtraso(ag, conteudo)
            );
        }
        if (conteudo.startsWith(NotificacaoService.PREFIX_AVISO_EQUIPE_ATRASO)) {
            return fillLayout(
                    "Cliente vai atrasar",
                    "Aviso para a equipe",
                    "Atraso",
                    accentLembrete(),
                    corpoAtraso(ag, conteudo)
            );
        }
        if (conteudo.startsWith(NotificacaoService.PREFIX_REAGENDAMENTO_ATRASO)) {
            return fillLayout(
                    "Horário reajustado",
                    "Ajuste na agenda da barbearia",
                    "Reagendado",
                    accentLembrete(),
                    corpoReagendamentoAtraso(ag, conteudo)
            );
        }
        if (conteudo.startsWith(NotificacaoService.PREFIX_ATRASO_RESPOSTA)) {
            return fillLayout(
                    "Atualização sobre atraso",
                    "Sua agenda foi reajustada",
                    "Atraso",
                    accentLembrete(),
                    corpoAtrasoChat(ag, conteudo)
            );
        }
        if (conteudo.startsWith(NotificacaoService.PREFIX_ATRASO_CHAT)) {
            return fillLayout(
                    "Mensagem sobre atraso",
                    "Nova mensagem na conversa",
                    "Chat",
                    accentLembrete(),
                    corpoAtrasoChat(ag, conteudo)
            );
        }
        if (conteudo.startsWith(NotificacaoService.PREFIX_PIX_CLIENTE)) {
            return fillLayout(
                    "Pagamento PIX",
                    "Seu atendimento foi finalizado",
                    "PIX",
                    accentNovo(),
                    corpoPagamentoPix(ag, conteudo)
            );
        }
        return fillLayout(
                "Barbearia",
                "Mensagem",
                "Aviso",
                accentNeutro(),
                "<p style=\"margin:0;\">" + escapeHtml(conteudo) + "</p>"
        );
    }

    public String renderBoasVindas(String nomeUsuario, String papelExibido) {
        String body = """
                <p style="margin:0 0 14px;color:#7dff9a !important;">Olá, <strong style="color:#39ff14 !important;">%s</strong>!</p>
                <p style="margin:0 0 14px;color:#7dff9a !important;">Sua conta foi criada com o perfil <strong style="color:#39ff14 !important;">%s</strong>.</p>
                <p style="margin:0;color:#7dff9a !important;">Você já pode acessar o sistema, conferir horários e receber avisos automáticos por e-mail.</p>
                """.formatted(escapeHtml(nomeUsuario), escapeHtml(papelExibido));
        return fillLayout(
                "Bem-vindo",
                "Conta ativa na barbearia",
                "Boas-vindas",
                accentNovo(),
                body
        );
    }

    public String renderRecuperacaoSenha(String nome, String linkRedefinicao, String tokenPuro) {
        String blocoAcao;
        if (linkRedefinicao != null && !linkRedefinicao.isBlank()) {
            blocoAcao = """
                    <p style="margin:0 0 18px;color:#7dff9a !important;">Clique no botão abaixo para criar uma nova senha (link expira em breve).</p>
                    <p style="margin:0 0 22px;text-align:center;">
                      <a href="%s" style="display:inline-block;padding:12px 22px;border-radius:10px;background-color:#39ff14 !important;color:#000000 !important;font-weight:700;text-decoration:none;font-size:15px;border:2px solid #39ff14;">Redefinir senha</a>
                    </p>
                    <p style="margin:0;font-size:12px;color:#39ff14 !important;">Se o botão não funcionar, copie e cole este endereço no navegador:<br/><span style="word-break:break-all;color:#7dff9a !important;">%s</span></p>
                    """.formatted(linkRedefinicao, escapeHtml(linkRedefinicao));
        } else {
            blocoAcao = """
                    <p style="margin:0 0 12px;">Use o token abaixo no corpo JSON de <code style="color:#39ff14;">POST /auth/reset-password</code> junto com <code style="color:#39ff14;">novaSenha</code>:</p>
                    <pre style="margin:0;padding:14px 16px;border-radius:10px;background:rgba(0,0,0,0.45);border:1px solid rgba(57,255,20,0.35);color:#c8ffd8;font-size:14px;letter-spacing:0.04em;overflow:auto;">%s</pre>
                    """.formatted(escapeHtml(tokenPuro));
        }
        String body = """
                <p style="margin:0 0 14px;">Olá, <strong style="color:#39ff14;">%s</strong>,</p>
                <p style="margin:0 0 18px;">recebemos um pedido para redefinir a senha da sua conta.</p>
                %s
                <p style="margin:18px 0 0;font-size:12px;color:#6f8a78;">Se você não solicitou, ignore este e-mail. Sua senha permanece a mesma.</p>
                """.formatted(escapeHtml(nome), blocoAcao);
        return fillLayout(
                "Redefinição de senha",
                "Recuperação de acesso",
                "Segurança",
                accentLembrete(),
                body
        );
    }

    private String fillLayout(String title, String headline, String badge, BadgeAccent accent, String bodyHtml) {
        return layoutHtml
                .replace("__TITLE__", escapeHtml(title))
                .replace("__HEADLINE__", escapeHtml(headline))
                .replace("__BADGE__", escapeHtml(badge))
                .replace("__BODY__", bodyHtml)
                .replace("__FOOTER_LINE__", "Neon desk · notificações automáticas");
    }

    private record BadgeAccent(String bg, String border, String glow) {
    }

    private static BadgeAccent accentNovo() {
        return new BadgeAccent(
                "rgba(57,255,20,0.22)",
                "rgba(57,255,20,0.55)",
                "rgba(57,255,20,0.45)"
        );
    }

    private static BadgeAccent accentLembrete() {
        return new BadgeAccent(
                "rgba(0,255,200,0.2)",
                "rgba(0,255,200,0.55)",
                "rgba(0,255,200,0.4)"
        );
    }

    private static BadgeAccent accentCancelado() {
        return new BadgeAccent(
                "#0a120a",
                "#39ff14",
                "#39ff14"
        );
    }

    private static BadgeAccent accentStatus(StatusAgendamento st) {
        return switch (st) {
            case CONCLUIDO -> new BadgeAccent(
                    "rgba(57,255,20,0.22)",
                    "rgba(57,255,20,0.55)",
                    "rgba(57,255,20,0.45)"
            );
            case FALTOU -> new BadgeAccent(
                    "rgba(255,200,80,0.22)",
                    "rgba(255,200,120,0.55)",
                    "rgba(255,180,0,0.35)"
            );
            case EM_ANDAMENTO -> new BadgeAccent(
                    "rgba(0,200,255,0.2)",
                    "rgba(120,230,255,0.55)",
                    "rgba(0,200,255,0.4)"
            );
            default -> new BadgeAccent(
                    "rgba(166,255,0,0.2)",
                    "rgba(200,255,120,0.5)",
                    "rgba(166,255,0,0.38)"
            );
        };
    }

    private static BadgeAccent accentNeutro() {
        return new BadgeAccent(
                "rgba(120,140,130,0.28)",
                "rgba(160,180,170,0.45)",
                "rgba(120,140,130,0.3)"
        );
    }

    private String corpoNovo(Agendamento a) {
        return blocoResumo(a, "Seu horário foi reservado com sucesso. Confira os detalhes abaixo.");
    }

    private String corpoLembrete(Agendamento a) {
        return blocoResumo(a, "Falta pouco para o seu atendimento. Não esqueça do horário.");
    }

    private String corpoCancelado(Agendamento a) {
        return blocoResumo(a, "O horário abaixo foi cancelado. Se não foi você, entre em contato com a barbearia.");
    }

    private String corpoStatus(Agendamento a, StatusAgendamento st) {
        return blocoResumo(a, "O status do seu agendamento foi atualizado para <strong style=\"color:#39ff14;\">"
                + escapeHtml(st.name()) + "</strong>.");
    }

    private String corpoBarbeiroNovo(Agendamento a) {
        return blocoResumo(a, "Um cliente acabou de reservar um horário com você. Confira os detalhes.");
    }

    private String corpoBarbeiroCancelado(Agendamento a) {
        return blocoResumo(a, "O cliente cancelou (ou a agenda cancelou) o horário abaixo.");
    }

    private String corpoBarbeiroStatus(Agendamento a, StatusAgendamento st) {
        return blocoResumo(a, "O status do agendamento foi atualizado para <strong style=\"color:#39ff14;\">"
                + escapeHtml(st.name()) + "</strong> — cópia para sua agenda.");
    }

    private String corpoStaffGenerico(Agendamento a, String intro) {
        return blocoResumo(a, intro);
    }

    private String corpoAtraso(Agendamento a, String conteudo) {
        String detalhe = conteudo.contains("Motivo:") ? conteudo.substring(conteudo.indexOf("Motivo:")) : conteudo;
        return blocoResumo(a, "Informação de atraso: <strong style=\"color:#39ff14;\">"
                + escapeHtml(detalhe) + "</strong>");
    }

    private String corpoPagamentoPix(Agendamento a, String conteudo) {
        PixDadosEmail pix = extrairDadosPix(conteudo);
        BigDecimal valor = resolverValorPix(a, pix);
        String valorFmt = formatarValor(valor);
        String chave = pixPayloadService.getChaveExibicao();
        String qrUrl = pix.qrUrl() != null && !pix.qrUrl().isBlank()
                ? pix.qrUrl()
                : "";
        String copia = pix.copiaCola() != null ? pix.copiaCola() : "";

        String blocoQr = qrUrl.isBlank()
                ? ""
                : """
                <div style="margin:24px 0;text-align:center;">
                  <p style="margin:0 0 12px;font-size:12px;letter-spacing:0.12em;text-transform:uppercase;color:#39ff14 !important;">Escaneie o QR Code</p>
                  <img src="%s" alt="QR Code PIX" width="260" height="260" style="display:block;margin:0 auto;border-radius:12px;border:2px solid #39ff14;background:#ffffff;padding:8px;" />
                </div>
                """.formatted(escapeHtml(qrUrl));

        return """
                <p style="margin:0 0 16px;color:#7dff9a !important;">Seu atendimento foi concluído. Para finalizar, realize o pagamento via PIX no valor de <strong style="color:#39ff14 !important;">%s</strong>.</p>
                %s
                <div style="margin:20px 0;padding:16px;border-radius:12px;border:1px solid rgba(57,255,20,0.45);background:rgba(0,0,0,0.35);text-align:center;">
                  <p style="margin:0 0 8px;font-size:11px;letter-spacing:0.1em;text-transform:uppercase;color:#39ff14 !important;">Chave PIX (%s)</p>
                  <p style="margin:0;font-size:22px;font-weight:700;color:#39ff14 !important;letter-spacing:0.06em;">%s</p>
                  <p style="margin:10px 0 0;font-size:12px;color:#7dff9a !important;">Use esta chave no app do banco se não conseguir ler o QR Code.</p>
                </div>
                %s
                <p style="margin:16px 0 0;font-size:13px;color:#7dff9a !important;">Depois do pagamento, envie o comprovante pelo aplicativo da barbearia.</p>
                """.formatted(
                escapeHtml(valorFmt),
                blocoQr,
                escapeHtml(pixPayloadService.getRotuloTipoChave()),
                escapeHtml(chave),
                blocoCopiaCola(copia)
        );
    }

    private String blocoCopiaCola(String copia) {
        if (copia == null || copia.isBlank()) {
            return "";
        }
        return """
                <div style="margin:16px 0;">
                  <p style="margin:0 0 8px;font-size:11px;letter-spacing:0.1em;text-transform:uppercase;color:#39ff14 !important;">PIX copia e cola</p>
                  <pre style="margin:0;padding:12px 14px;border-radius:10px;border:1px solid rgba(57,255,20,0.35);background:rgba(0,0,0,0.5);color:#c8ffd8;font-size:11px;line-height:1.45;white-space:pre-wrap;word-break:break-all;">%s</pre>
                </div>
                """.formatted(escapeHtml(copia));
    }

    private BigDecimal resolverValorPix(Agendamento a, PixDadosEmail pix) {
        if (a.getId() != null) {
            var pagOpt = pagamentoRepository.findByAgendamento_Id(a.getId());
            if (pagOpt.isPresent() && pagOpt.get().getValor() != null && pagOpt.get().getValor().signum() > 0) {
                return pagOpt.get().getValor();
            }
        }
        BigDecimal doPayload = PixPayloadService.extrairValorDoPayload(pix.copiaCola());
        if (doPayload != null && doPayload.signum() > 0) {
            return doPayload;
        }
        if (pix.valor() != null && !pix.valor().isBlank()) {
            try {
                BigDecimal parsed = new BigDecimal(pix.valor().trim().replace(',', '.'));
                if (parsed.signum() > 0) {
                    return parsed;
                }
            } catch (NumberFormatException ignored) {
            }
        }
        BigDecimal total = agendamentoDetalheHelper.valorTotal(a);
        return total.signum() > 0 ? total : BigDecimal.ZERO;
    }

    private static String formatarValor(BigDecimal valor) {
        return String.format(Locale.forLanguageTag("pt-BR"), "R$ %.2f", valor);
    }

    private PixDadosEmail extrairDadosPix(String conteudo) {
        if (conteudo.contains("||")) {
            String[] partes = conteudo.split("\\|\\|", 4);
            if (partes.length >= 4) {
                return new PixDadosEmail(partes[1], partes[2], partes[3]);
            }
        }
        String qrUrl = null;
        String copia = null;
        String valor = null;
        int qrIdx = conteudo.indexOf("QR: ");
        int codigoIdx = conteudo.indexOf(" | Codigo: ");
        if (qrIdx >= 0 && codigoIdx > qrIdx) {
            qrUrl = conteudo.substring(qrIdx + 4, codigoIdx).trim();
            int fimCodigo = conteudo.indexOf(" Depois ", codigoIdx);
            copia = fimCodigo > codigoIdx
                    ? conteudo.substring(codigoIdx + 10, fimCodigo).trim()
                    : conteudo.substring(codigoIdx + 10).trim();
        }
        int valorIdx = conteudo.indexOf("Valor R$ ");
        if (valorIdx >= 0) {
            int inicio = valorIdx + 9;
            int fimValor = conteudo.indexOf(". Pague", inicio);
            if (fimValor < 0) {
                fimValor = conteudo.indexOf(' ', inicio);
            }
            if (fimValor > inicio) {
                valor = conteudo.substring(inicio, fimValor).trim().replace(',', '.');
            }
        }
        return new PixDadosEmail(valor, qrUrl, copia);
    }

    private record PixDadosEmail(String valor, String qrUrl, String copiaCola) {
    }

    private String corpoReagendamentoAtraso(Agendamento a, String conteudo) {
        DadosReagendamento dados = extrairDadosReagendamento(conteudo);
        String novoHorario = dados.novoHorario() != null
                ? dados.novoHorario()
                : FMT.format(a.getInicio());
        int minutos = dados.minutos() != null ? dados.minutos() : 0;
        String clienteAtraso = dados.clienteAtraso() != null ? dados.clienteAtraso() : "outro cliente";

        return blocoResumo(a, """
                <p style="margin:0 0 12px;color:#7dff9a !important;">Informamos que houve um <strong style="color:#39ff14 !important;">atraso confirmado</strong> de outro atendimento na mesma agenda do seu barbeiro.</p>
                <p style="margin:0 0 12px;color:#7dff9a !important;">Por isso, <strong style="color:#39ff14 !important;">seu horário foi adiado em %d minuto%s</strong> para organizar a fila sem conflitos.</p>
                <p style="margin:0;color:#c8ffd8 !important;">Cliente com atraso: <strong style="color:#39ff14 !important;">%s</strong><br/>Seu novo horário: <strong style="color:#39ff14 !important;">%s</strong></p>
                """.formatted(
                minutos,
                minutos == 1 ? "" : "s",
                escapeHtml(clienteAtraso),
                escapeHtml(novoHorario)
        ));
    }

    private record DadosReagendamento(Integer minutos, String novoHorario, String clienteAtraso) {
    }

    private DadosReagendamento extrairDadosReagendamento(String conteudo) {
        if (conteudo.contains("||")) {
            String[] partes = conteudo.split("\\|\\|", 4);
            if (partes.length >= 4) {
                try {
                    return new DadosReagendamento(
                            Integer.parseInt(partes[1].trim()),
                            partes[2].trim(),
                            partes[3].trim()
                    );
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return new DadosReagendamento(null, null, null);
    }

    private String corpoAtrasoChat(Agendamento a, String conteudo) {
        String texto = conteudo;
        for (String prefix : new String[]{
                NotificacaoService.PREFIX_ATRASO_CHAT,
                NotificacaoService.PREFIX_ATRASO_RESPOSTA,
                NotificacaoService.PREFIX_REAGENDAMENTO_ATRASO
        }) {
            if (texto.startsWith(prefix)) {
                texto = texto.substring(prefix.length()).trim();
                break;
            }
        }
        return blocoResumo(a, "<p style=\"margin:0;color:#7dff9a !important;\">"
                + escapeHtml(texto) + "</p>");
    }

    private String blocoResumo(Agendamento a, String intro) {
        String inicio = FMT.format(a.getInicio());
        String fim = FMT.format(a.getFim());
        return """
                <p style="margin:0 0 16px;color:#7dff9a !important;">%s</p>
                %s
                """.formatted(intro, tabelaResumo(a, inicio, fim));
    }

    private String tabelaResumo(Agendamento a, String inicio, String fim) {
        agendamentoDetalheHelper.enriquecer(a);
        return """
                <table role="presentation" cellpadding="0" cellspacing="0" border="0" width="100%%" bgcolor="#000000" style="border-collapse:collapse;border:1px solid #39ff14;background-color:#000000 !important;">
                  %s
                  %s
                  %s
                  %s
                  %s
                </table>
                """.formatted(
                linha("Data e hora", escapeHtml(inicio) + " — " + escapeHtml(fim)),
                linha("Serviço", escapeHtml(agendamentoDetalheHelper.nomesServicos(a))),
                linha("Profissional", escapeHtml(agendamentoDetalheHelper.nomeBarbeiro(a))),
                linha("Cliente", escapeHtml(agendamentoDetalheHelper.nomeCliente(a))),
                a.getObservacoes() != null && !a.getObservacoes().isBlank()
                        ? linha("Observações", escapeHtml(a.getObservacoes()))
                        : ""
        );
    }

    private static String linha(String rotulo, String valor) {
        return """
                <tr bgcolor="#000000">
                  <td bgcolor="#000000" style="padding:12px 14px;border-bottom:1px solid #39ff14;font-size:12px;letter-spacing:0.08em;text-transform:uppercase;color:#39ff14 !important;background-color:#000000 !important;width:34%%;">%s</td>
                  <td bgcolor="#000000" style="padding:12px 14px;border-bottom:1px solid #39ff14;font-size:15px;color:#7dff9a !important;font-weight:600;background-color:#000000 !important;">%s</td>
                </tr>
                """.formatted(escapeHtml(rotulo), valor);
    }

    private static String escapeHtml(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
