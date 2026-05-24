package com.example.barbearia.service;

import com.example.barbearia.domain.Agendamento;
import com.example.barbearia.domain.Mensagem;
import com.example.barbearia.domain.Role;
import com.example.barbearia.domain.StatusAgendamento;
import com.example.barbearia.domain.StatusMensagem;
import com.example.barbearia.domain.TipoMensagem;
import com.example.barbearia.domain.User;
import com.example.barbearia.mail.EmailTemplateEngine;
import com.example.barbearia.mail.MailDeliveryService;
import com.example.barbearia.repository.AgendamentoRepository;
import com.example.barbearia.repository.MensagemRepository;
import com.example.barbearia.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificacaoService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final int CONTEUDO_MAX = 500;

    public static final String PREFIX_NOVO = "[NOVO]";
    public static final String PREFIX_LEMBRETE = "[LEMBRETE]";
    public static final String PREFIX_CANCELADO = "[CANCELADO]";
    public static final String PREFIX_STATUS = "[STATUS]";
    public static final String PREFIX_AVISO_BARBEIRO_NOVO = "[AVISO_BARBEIRO_NOVO]";
    public static final String PREFIX_AVISO_BARBEIRO_CANCEL = "[AVISO_BARBEIRO_CANCEL]";
    public static final String PREFIX_AVISO_BARBEIRO_STATUS = "[AVISO_BARBEIRO_STATUS]";
    public static final String PREFIX_AVISO_STAFF_NOVO = "[AVISO_STAFF_NOVO]";
    public static final String PREFIX_AVISO_STAFF_CANCEL = "[AVISO_STAFF_CANCEL]";
    public static final String PREFIX_ATRASO_CLIENTE = "[ATRASO_CLIENTE]";
    public static final String PREFIX_AVISO_EQUIPE_ATRASO = "[AVISO_EQUIPE_ATRASO]";
    public static final String PREFIX_PIX_CLIENTE = "[PIX_PAGAMENTO]";
    public static final String PREFIX_COMPROVANTE_EQUIPE = "[COMPROVANTE_PIX]";
    public static final String PREFIX_ATRASO_RESPOSTA = "[ATRASO_RESPOSTA]";
    public static final String PREFIX_REAGENDAMENTO_ATRASO = "[REAGENDO_ATRASO]";
    public static final String PREFIX_ATRASO_CHAT = "[ATRASO_CHAT]";

    private final MensagemRepository mensagemRepository;
    private final AgendamentoRepository agendamentoRepository;
    private final UserRepository userRepository;
    private final EmailTemplateEngine emailTemplateEngine;
    private final MailDeliveryService mailDeliveryService;
    private final AgendamentoDetalheHelper agendamentoDetalheHelper;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void notificarAgendamentoCriado(UUID agendamentoId) {
        agendamentoRepository.findByIdComDetalhes(agendamentoId).ifPresent(a -> {
            if (mensagemRepository.existsByAgendamento_IdAndConteudoStartingWith(a.getId(), PREFIX_NOVO)) {
                return;
            }
            String email = emailCliente(a);
            if (!StringUtils.hasText(email)) {
                return;
            }
            String corpo = truncar(PREFIX_NOVO + " Agendamento confirmado. "
                    + a.getServico().getNome() + " com " + a.getBarbeiro().getNome()
                    + " em " + FMT.format(a.getInicio()) + ".");
            salvarMensagem(a, email, corpo);
            notificarCopiaBarbeiroNovoAgendamento(a);
            notificarEquipeGestaoNovoAgendamento(a);
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void notificarAgendamentoCancelado(UUID agendamentoId) {
        agendamentoRepository.findByIdComDetalhes(agendamentoId).ifPresent(a -> {
            if (mensagemRepository.existsByAgendamento_IdAndConteudoStartingWith(a.getId(), PREFIX_CANCELADO)) {
                return;
            }
            String email = emailCliente(a);
            if (!StringUtils.hasText(email)) {
                return;
            }
            String corpo = truncar(PREFIX_CANCELADO + " Seu agendamento de "
                    + FMT.format(a.getInicio()) + " (" + a.getServico().getNome() + ") foi cancelado.");
            salvarMensagem(a, email, corpo);
            notificarCopiaBarbeiroCancelamento(a);
            notificarEquipeGestaoCancelamento(a);
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void notificarAtraso(UUID agendamentoId, int minutos, String motivo) {
        agendamentoRepository.findByIdComDetalhes(agendamentoId).ifPresent(a -> {
            try {
            String prefixCliente = PREFIX_ATRASO_CLIENTE + minutos;
            if (!mensagemRepository.existsByAgendamento_IdAndConteudoStartingWith(a.getId(), prefixCliente)) {
                String emailCliente = emailCliente(a);
                if (StringUtils.hasText(emailCliente)) {
                    String corpoCliente = truncar(prefixCliente + " Registramos seu aviso de atraso de " + minutos
                            + " min. Motivo: " + motivo + ". Horário: " + FMT.format(a.getInicio()) + ".");
                    salvarMensagem(a, emailCliente, corpoCliente);
                }
            }

            String prefixEquipe = PREFIX_AVISO_EQUIPE_ATRASO + minutos;
            if (mensagemRepository.existsByAgendamento_IdAndConteudoStartingWith(a.getId(), prefixEquipe)) {
                return;
            }
            String corpoEquipe = truncar(prefixEquipe + " Cliente " + agendamentoDetalheHelper.nomeCliente(a)
                    + " avisa atraso de " + minutos + " min para " + FMT.format(a.getInicio())
                    + " com " + agendamentoDetalheHelper.nomeBarbeiro(a) + ". Motivo: " + motivo + ".");

            Set<String> destinos = new HashSet<>();
            String emailBarbeiro = emailBarbeiroUsuario(a);
            if (StringUtils.hasText(emailBarbeiro)) {
                destinos.add(emailBarbeiro.toLowerCase());
            }
            for (User u : userRepository.findByRoleInAndAtivoTrue(List.of(Role.ADMIN, Role.RECEPCIONISTA))) {
                if (StringUtils.hasText(u.getEmail())) {
                    destinos.add(u.getEmail().trim().toLowerCase());
                }
            }
            for (String destino : destinos) {
                if (mensagemRepository.existsByAgendamento_IdAndDestinatarioIgnoreCaseAndConteudoStartingWith(
                        a.getId(), destino, prefixEquipe)) {
                    continue;
                }
                salvarMensagem(a, destino, corpoEquipe);
            }
            } catch (Exception ignored) {
            }
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void notificarPagamentoPixCliente(
            UUID agendamentoId,
            java.math.BigDecimal valor,
            String copiaCola,
            String qrUrl
    ) {
        agendamentoRepository.findByIdComDetalhes(agendamentoId).ifPresent(a -> {
            if (mensagemRepository.existsByAgendamento_IdAndConteudoStartingWith(a.getId(), PREFIX_PIX_CLIENTE)) {
                return;
            }
            String email = emailCliente(a);
            if (!StringUtils.hasText(email)) {
                return;
            }
            java.math.BigDecimal total = valor != null && valor.signum() > 0
                    ? valor
                    : agendamentoDetalheHelper.valorTotal(a);
            String corpo = truncar(PREFIX_PIX_CLIENTE + "||" + total.toPlainString() + "||" + qrUrl + "||" + copiaCola);
            salvarMensagem(a, email, corpo);
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void notificarComprovantePixEnviado(UUID agendamentoId) {
        agendamentoRepository.findByIdComDetalhes(agendamentoId).ifPresent(a -> {
            if (mensagemRepository.existsByAgendamento_IdAndConteudoStartingWith(a.getId(), PREFIX_COMPROVANTE_EQUIPE)) {
                return;
            }
            String corpo = truncar(PREFIX_COMPROVANTE_EQUIPE + " Cliente " + a.getCliente().getNome()
                    + " enviou comprovante PIX do atendimento em " + FMT.format(a.getInicio())
                    + " (" + a.getServico().getNome() + "). Confirme o pagamento no sistema.");

            Set<String> destinos = new HashSet<>();
            String emailBarbeiro = emailBarbeiroUsuario(a);
            if (StringUtils.hasText(emailBarbeiro)) {
                destinos.add(emailBarbeiro.toLowerCase());
            }
            for (String email : emailsGestao()) {
                destinos.add(email.toLowerCase());
            }
            for (String destino : destinos) {
                if (mensagemRepository.existsByAgendamento_IdAndDestinatarioIgnoreCaseAndConteudoStartingWith(
                        a.getId(), destino, PREFIX_COMPROVANTE_EQUIPE)) {
                    continue;
                }
                salvarMensagem(a, destino, corpo);
            }
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void notificarReagendamentoAtraso(
            UUID agendamentoId,
            int minutosReajuste,
            String novoHorario,
            String nomeClienteAtraso
    ) {
        agendamentoRepository.findByIdComDetalhes(agendamentoId).ifPresent(a -> {
            String email = emailCliente(a);
            if (!StringUtils.hasText(email)) {
                return;
            }
            String corpo = truncar(PREFIX_REAGENDAMENTO_ATRASO + "||" + minutosReajuste + "||"
                    + novoHorario + "||" + nomeClienteAtraso);
            salvarMensagem(a, email, corpo);
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void notificarRespostaAtraso(UUID agendamentoId, String mensagem) {
        agendamentoRepository.findByIdComDetalhes(agendamentoId).ifPresent(a -> {
            String prefix = PREFIX_ATRASO_RESPOSTA;
            if (mensagemRepository.existsByAgendamento_IdAndConteudoStartingWith(a.getId(), prefix)) {
                return;
            }
            String email = emailCliente(a);
            if (!StringUtils.hasText(email)) {
                return;
            }
            salvarMensagem(a, email, truncar(prefix + " " + mensagem));
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void notificarMensagemAtraso(UUID agendamentoId, String autorNome, String texto) {
        agendamentoRepository.findByIdComDetalhes(agendamentoId).ifPresent(a -> {
            String corpo = truncar(PREFIX_ATRASO_CHAT + " " + autorNome + ": " + texto);

            String emailCliente = emailCliente(a);
            if (StringUtils.hasText(emailCliente)) {
                salvarMensagem(a, emailCliente, corpo);
            }

            Set<String> destinos = new HashSet<>();
            String emailBarbeiro = emailBarbeiroUsuario(a);
            if (StringUtils.hasText(emailBarbeiro)) {
                destinos.add(emailBarbeiro.toLowerCase());
            }
            for (String email : emailsGestao()) {
                destinos.add(email.toLowerCase());
            }
            if (StringUtils.hasText(emailCliente)) {
                destinos.remove(emailCliente.toLowerCase());
            }

            for (String destino : destinos) {
                if (mensagemRepository.existsByAgendamento_IdAndDestinatarioIgnoreCaseAndConteudoStartingWith(
                        a.getId(), destino, PREFIX_ATRASO_CHAT)) {
                    continue;
                }
                salvarMensagem(a, destino, corpo);
            }
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void notificarMudancaStatus(UUID agendamentoId, StatusAgendamento novoStatus) {
        if (novoStatus == StatusAgendamento.CANCELADO) {
            notificarAgendamentoCancelado(agendamentoId);
            return;
        }
        agendamentoRepository.findByIdComDetalhes(agendamentoId).ifPresent(a -> {
            String prefix = PREFIX_STATUS + novoStatus.name();
            if (mensagemRepository.existsByAgendamento_IdAndConteudoStartingWith(a.getId(), prefix)) {
                return;
            }
            String email = emailCliente(a);
            if (!StringUtils.hasText(email)) {
                return;
            }
            String corpo = truncar(prefix + " Seu agendamento em " + FMT.format(a.getInicio())
                    + " agora está: " + novoStatus + ".");
            salvarMensagem(a, email, corpo);
            notificarCopiaBarbeiroMudancaStatus(a, novoStatus);
        });
    }

    @Transactional
    public void dispararLembretesProximas24Horas() {
        LocalDateTime agora = LocalDateTime.now();
        LocalDateTime janelaInicio = agora.plusHours(23);
        LocalDateTime janelaFim = agora.plusHours(25);
        List<StatusAgendamento> status = Arrays.asList(StatusAgendamento.AGENDADO, StatusAgendamento.CONFIRMADO);

        List<Agendamento> candidatos = agendamentoRepository.findParaLembreteNaJanela(status, janelaInicio, janelaFim);
        for (Agendamento a : candidatos) {
            if (mensagemRepository.existsByAgendamento_IdAndConteudoStartingWith(a.getId(), PREFIX_LEMBRETE)) {
                continue;
            }
            String email = emailCliente(a);
            if (!StringUtils.hasText(email)) {
                continue;
            }
            String corpo = truncar(PREFIX_LEMBRETE + " Lembrete: " + a.getServico().getNome() + " com "
                    + a.getBarbeiro().getNome() + " em " + FMT.format(a.getInicio()) + ".");
            salvarMensagem(a, email, corpo);
        }
    }

    @Transactional
    public void processarMensagensPendentes(int limite) {
        var page = mensagemRepository.findByStatusOrderByCreatedAtAsc(
                StatusMensagem.PENDENTE,
                PageRequest.of(0, limite)
        );
        for (Mensagem m : page) {
            enviarOuMarcarFalha(m);
        }
    }

    private void salvarMensagem(Agendamento agendamento, String destinatario, String conteudo) {
        agendamentoDetalheHelper.enriquecer(agendamento);
        Mensagem msg = Mensagem.builder()
                .agendamentoId(agendamento.getId())
                .agendamento(agendamento)
                .tipo(TipoMensagem.EMAIL)
                .status(StatusMensagem.PENDENTE)
                .conteudo(conteudo)
                .destinatario(destinatario.trim())
                .build();
        mensagemRepository.save(msg);
        enviarOuMarcarFalha(msg);
    }

    private void enviarOuMarcarFalha(Mensagem m) {
        if (m.getStatus() != StatusMensagem.PENDENTE) {
            return;
        }
        if (!mailDeliveryService.isMailConfigured()) {
            m.setStatus(StatusMensagem.FALHA);
            m.setErro("JavaMailSender indisponível (configure spring.mail.host).");
            mensagemRepository.save(m);
            return;
        }
        try {
            String textoPlano = m.getConteudo();
            UUID agendamentoId = m.getAgendamentoId();
            if (agendamentoId == null && m.getAgendamento() != null) {
                agendamentoId = m.getAgendamento().getId();
            }
            var agOpt = agendamentoId != null
                    ? agendamentoRepository.findByIdComDetalhes(agendamentoId)
                    : java.util.Optional.<Agendamento>empty();
            String html;
            if (agOpt.isPresent()) {
                html = emailTemplateEngine.renderHtml(m, agOpt.get());
            } else {
                html = "<html><body style=\"margin:0;background:#050508;color:#eafff0;font-family:system-ui,sans-serif;padding:20px;\"><pre style=\"white-space:pre-wrap;\">"
                        + escapeHtmlSimple(textoPlano) + "</pre></body></html>";
            }
            mailDeliveryService.enviar(m.getDestinatario(), assuntoPara(m.getConteudo()), textoPlano, html);
            m.setStatus(StatusMensagem.ENVIADA);
            m.setEnviadoEm(LocalDateTime.now());
            m.setErro(null);
        } catch (Exception ex) {
            m.setStatus(StatusMensagem.FALHA);
            m.setErro(truncarErro(ex.getMessage()));
        }
        mensagemRepository.save(m);
    }

    private static String assuntoPara(String conteudo) {
        if (conteudo.startsWith(PREFIX_LEMBRETE)) {
            return "Lembrete — Barbearia";
        }
        if (conteudo.startsWith(PREFIX_CANCELADO)) {
            return "Agendamento cancelado — Barbearia";
        }
        if (conteudo.startsWith(PREFIX_NOVO)) {
            return "Novo agendamento — Barbearia";
        }
        if (conteudo.startsWith(PREFIX_STATUS)) {
            return "Atualização do agendamento — Barbearia";
        }
        if (conteudo.startsWith(PREFIX_AVISO_BARBEIRO_NOVO)) {
            return "Cópia: novo horário — Barbearia";
        }
        if (conteudo.startsWith(PREFIX_AVISO_BARBEIRO_CANCEL)) {
            return "Cópia: cancelamento — Barbearia";
        }
        if (conteudo.startsWith(PREFIX_AVISO_BARBEIRO_STATUS)) {
            return "Cópia: status — Barbearia";
        }
        if (conteudo.startsWith(PREFIX_AVISO_STAFF_NOVO)) {
            return "Novo agendamento na barbearia — Gestão";
        }
        if (conteudo.startsWith(PREFIX_AVISO_STAFF_CANCEL)) {
            return "Cancelamento — Gestão";
        }
        if (conteudo.startsWith(PREFIX_ATRASO_CLIENTE)) {
            return "Atraso registrado — Barbearia";
        }
        if (conteudo.startsWith(PREFIX_AVISO_EQUIPE_ATRASO)) {
            return "Cliente avisou atraso — Barbearia";
        }
        if (conteudo.startsWith(PREFIX_PIX_CLIENTE)) {
            return "Pagamento PIX — Barbearia";
        }
        if (conteudo.startsWith(PREFIX_COMPROVANTE_EQUIPE)) {
            return "Comprovante PIX recebido — Barbearia";
        }
        if (conteudo.startsWith(PREFIX_ATRASO_RESPOSTA)) {
            return "Atualização sobre seu atraso — Barbearia";
        }
        if (conteudo.startsWith(PREFIX_REAGENDAMENTO_ATRASO)) {
            return "Horário reajustado — Barbearia";
        }
        if (conteudo.startsWith(PREFIX_ATRASO_CHAT)) {
            return "Mensagem sobre atraso — Barbearia";
        }
        return "Barbearia";
    }

    private void notificarEquipeGestaoNovoAgendamento(Agendamento a) {
        for (String email : emailsGestao()) {
            if (email.equalsIgnoreCase(emailCliente(a)) || email.equalsIgnoreCase(emailBarbeiroUsuario(a))) {
                continue;
            }
            if (mensagemRepository.existsByAgendamento_IdAndDestinatarioIgnoreCaseAndConteudoStartingWith(
                    a.getId(), email, PREFIX_AVISO_STAFF_NOVO)) {
                continue;
            }
            String corpo = truncar(PREFIX_AVISO_STAFF_NOVO + " Novo agendamento: " + a.getCliente().getNome()
                    + " — " + a.getServico().getNome() + " com " + a.getBarbeiro().getNome()
                    + " em " + FMT.format(a.getInicio()) + ".");
            salvarMensagem(a, email, corpo);
        }
    }

    private void notificarEquipeGestaoCancelamento(Agendamento a) {
        for (String email : emailsGestao()) {
            if (email.equalsIgnoreCase(emailCliente(a)) || email.equalsIgnoreCase(emailBarbeiroUsuario(a))) {
                continue;
            }
            if (mensagemRepository.existsByAgendamento_IdAndDestinatarioIgnoreCaseAndConteudoStartingWith(
                    a.getId(), email, PREFIX_AVISO_STAFF_CANCEL)) {
                continue;
            }
            String corpo = truncar(PREFIX_AVISO_STAFF_CANCEL + " Cancelamento: " + a.getCliente().getNome()
                    + " — " + a.getServico().getNome() + " em " + FMT.format(a.getInicio()) + ".");
            salvarMensagem(a, email, corpo);
        }
    }

    private List<String> emailsGestao() {
        return userRepository.findByRoleInAndAtivoTrue(List.of(Role.ADMIN, Role.RECEPCIONISTA)).stream()
                .map(User::getEmail)
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .toList();
    }

    private void notificarCopiaBarbeiroNovoAgendamento(Agendamento a) {
        if (mensagemRepository.existsByAgendamento_IdAndConteudoStartingWith(a.getId(), PREFIX_AVISO_BARBEIRO_NOVO)) {
            return;
        }
        String emailB = emailBarbeiroUsuario(a);
        if (!StringUtils.hasText(emailB)) {
            return;
        }
        if (emailClienteIgualBarbeiro(a, emailB)) {
            return;
        }
        String corpo = truncar(PREFIX_AVISO_BARBEIRO_NOVO + " Novo agendamento na sua agenda: "
                + a.getCliente().getNome() + " — " + a.getServico().getNome() + " em " + FMT.format(a.getInicio()) + ".");
        salvarMensagem(a, emailB, corpo);
    }

    private void notificarCopiaBarbeiroCancelamento(Agendamento a) {
        if (mensagemRepository.existsByAgendamento_IdAndConteudoStartingWith(a.getId(), PREFIX_AVISO_BARBEIRO_CANCEL)) {
            return;
        }
        String emailB = emailBarbeiroUsuario(a);
        if (!StringUtils.hasText(emailB)) {
            return;
        }
        if (emailClienteIgualBarbeiro(a, emailB)) {
            return;
        }
        String corpo = truncar(PREFIX_AVISO_BARBEIRO_CANCEL + " Cancelamento na sua agenda: "
                + a.getCliente().getNome() + " — " + FMT.format(a.getInicio()) + ".");
        salvarMensagem(a, emailB, corpo);
    }

    private void notificarCopiaBarbeiroMudancaStatus(Agendamento a, StatusAgendamento novoStatus) {
        String prefixo = PREFIX_AVISO_BARBEIRO_STATUS + novoStatus.name();
        if (mensagemRepository.existsByAgendamento_IdAndConteudoStartingWith(a.getId(), prefixo)) {
            return;
        }
        String emailB = emailBarbeiroUsuario(a);
        if (!StringUtils.hasText(emailB)) {
            return;
        }
        if (emailClienteIgualBarbeiro(a, emailB)) {
            return;
        }
        String corpo = truncar(prefixo + " Status na sua agenda: " + novoStatus + " — "
                + a.getCliente().getNome() + " em " + FMT.format(a.getInicio()) + ".");
        salvarMensagem(a, emailB, corpo);
    }

    private static String emailBarbeiroUsuario(Agendamento a) {
        if (a.getBarbeiro() == null || a.getBarbeiro().getUser() == null) {
            return null;
        }
        String e = a.getBarbeiro().getUser().getEmail();
        return e == null ? null : e.trim();
    }

    private static boolean emailClienteIgualBarbeiro(Agendamento a, String emailBarbeiro) {
        String c = emailCliente(a);
        return c != null && c.equalsIgnoreCase(emailBarbeiro.trim());
    }

    private static String escapeHtmlSimple(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private static String emailCliente(Agendamento a) {
        if (a.getCliente() == null || a.getCliente().getEmail() == null) {
            return null;
        }
        return a.getCliente().getEmail().trim();
    }

    private static String truncar(String texto) {
        if (texto == null) {
            return "";
        }
        return texto.length() <= CONTEUDO_MAX ? texto : texto.substring(0, CONTEUDO_MAX);
    }

    private static String truncarErro(String msg) {
        if (msg == null) {
            return "Erro desconhecido";
        }
        return msg.length() <= 500 ? msg : msg.substring(0, 500);
    }
}
