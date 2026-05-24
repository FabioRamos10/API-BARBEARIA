package com.example.barbearia.service;

import com.example.barbearia.domain.ChatMensagem;
import com.example.barbearia.domain.Conversa;
import com.example.barbearia.domain.TipoAlerta;
import com.example.barbearia.domain.TipoConteudoMensagem;
import com.example.barbearia.domain.User;
import com.example.barbearia.dto.*;
import com.example.barbearia.exception.ApiException;
import com.example.barbearia.repository.ChatMensagemRepository;
import com.example.barbearia.repository.ConversaRepository;
import com.example.barbearia.repository.UserRepository;
import com.example.barbearia.security.AuthzHelper;
import com.example.barbearia.storage.ArquivoStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ConversaRepository conversaRepository;
    private final ChatMensagemRepository chatMensagemRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final AlertaService alertaService;
    private final ArquivoStorageService arquivoStorageService;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional(readOnly = true)
    public List<UsuarioChatDTO> listarUsuarios(Authentication auth) {
        User me = userService.requireByEmail(AuthzHelper.email(auth));
        return userRepository.findByAtivoTrueOrderByNomeAsc().stream()
                .filter(u -> !u.getId().equals(me.getId()))
                .map(u -> new UsuarioChatDTO(
                        u.getId(),
                        u.getNome(),
                        u.getEmail(),
                        u.getRole() != null ? u.getRole().name() : "CLIENTE"))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ConversaResponseDTO> listarConversas(Authentication auth) {
        User me = userService.requireByEmail(AuthzHelper.email(auth));
        return conversaRepository.findByParticipante(me.getId()).stream()
                .map(c -> toConversaDto(c, me.getId()))
                .toList();
    }

    @Transactional
    public ConversaResponseDTO abrirConversa(UUID outroUserId, Authentication auth) {
        User me = userService.requireByEmail(AuthzHelper.email(auth));
        User outro = userService.requireById(outroUserId);
        if (me.getId().equals(outro.getId())) {
            throw new ApiException("Não é possível conversar consigo mesmo", HttpStatus.BAD_REQUEST);
        }
        UUID[] par = Conversa.ordenarPar(me.getId(), outro.getId());
        Conversa conversa = conversaRepository.findByUsuarioMenorIdAndUsuarioMaiorId(par[0], par[1])
                .orElseGet(() -> conversaRepository.save(Conversa.builder()
                        .usuarioMenorId(par[0])
                        .usuarioMaiorId(par[1])
                        .build()));
        return toConversaDto(conversa, me.getId());
    }

    @Transactional(readOnly = true)
    public List<ChatMensagemResponseDTO> listarMensagens(UUID conversaId, Authentication auth) {
        Conversa conversa = requireParticipante(conversaId, auth);
        return chatMensagemRepository.findByConversa_IdOrderByEnviadaEmAsc(conversa.getId()).stream()
                .map(m -> toMensagemDto(m, conversa.getId()))
                .toList();
    }

    @Transactional
    public ChatMensagemResponseDTO enviar(UUID conversaId, ChatEnviarDTO dto, Authentication auth) {
        return enviarMensagem(conversaId, dto != null ? dto.texto() : null, null, auth);
    }

    @Transactional
    public ChatMensagemResponseDTO enviarComArquivo(
            UUID conversaId,
            String texto,
            MultipartFile arquivo,
            Authentication auth
    ) {
        return enviarMensagem(conversaId, texto, arquivo, auth);
    }

    private ChatMensagemResponseDTO enviarMensagem(
            UUID conversaId,
            String texto,
            MultipartFile arquivo,
            Authentication auth
    ) {
        User me = userService.requireByEmail(AuthzHelper.email(auth));
        Conversa conversa = requireParticipante(conversaId, auth);

        boolean temTexto = StringUtils.hasText(texto);
        boolean temArquivo = arquivo != null && !arquivo.isEmpty();
        if (!temTexto && !temArquivo) {
            throw new ApiException("Envie uma mensagem de texto ou um anexo", HttpStatus.BAD_REQUEST);
        }

        ChatMensagem.ChatMensagemBuilder builder = ChatMensagem.builder()
                .conversa(conversa)
                .remetenteUserId(me.getId())
                .lida(false);

        if (temArquivo) {
            var salvo = arquivoStorageService.salvarChat(conversa.getId(), arquivo);
            TipoConteudoMensagem tipo = salvo.contentType() != null && salvo.contentType().startsWith("image/")
                    ? TipoConteudoMensagem.IMAGEM
                    : TipoConteudoMensagem.ARQUIVO;
            builder.tipoConteudo(tipo)
                    .anexoPath(salvo.storagePath())
                    .anexoUrl(salvo.urlPublica())
                    .anexoContentType(salvo.contentType())
                    .anexoNome(salvo.nomeOriginal())
                    .texto(temTexto ? texto.trim() : null);
        } else {
            builder.tipoConteudo(TipoConteudoMensagem.TEXTO).texto(texto.trim());
        }

        ChatMensagem msg = chatMensagemRepository.save(builder.build());
        msg.setConversaId(conversa.getId());
        conversa.setUltimaMensagemEm(LocalDateTime.now());
        conversaRepository.save(conversa);

        ChatMensagemResponseDTO response = ChatMensagemResponseDTO.from(msg, me.getNome());
        messagingTemplate.convertAndSend("/topic/chat." + conversa.getId(), response);

        String preview = temTexto ? texto.trim() : (msg.getTipoConteudo() == TipoConteudoMensagem.IMAGEM
                ? "📷 Imagem enviada" : "📎 Arquivo enviado");
        UUID destinoId = conversa.getUsuarioMenorId().equals(me.getId())
                ? conversa.getUsuarioMaiorId()
                : conversa.getUsuarioMenorId();
        alertaService.notificarUsuario(
                destinoId,
                conversa.getId(),
                "Nova mensagem de " + me.getNome(),
                preview,
                TipoAlerta.CHAT
        );

        return response;
    }

    @Transactional
    public void marcarMensagensLidas(UUID conversaId, Authentication auth) {
        User me = userService.requireByEmail(AuthzHelper.email(auth));
        requireParticipante(conversaId, auth);
        chatMensagemRepository.findByConversa_IdOrderByEnviadaEmAsc(conversaId).stream()
                .filter(m -> !m.getRemetenteUserId().equals(me.getId()) && !m.isLida())
                .forEach(m -> {
                    m.setLida(true);
                    chatMensagemRepository.save(m);
                });
    }

    private Conversa requireParticipante(UUID conversaId, Authentication auth) {
        User me = userService.requireByEmail(AuthzHelper.email(auth));
        Conversa conversa = conversaRepository.findById(conversaId)
                .orElseThrow(() -> new ApiException("Conversa não encontrada", HttpStatus.NOT_FOUND));
        if (!conversa.getUsuarioMenorId().equals(me.getId()) && !conversa.getUsuarioMaiorId().equals(me.getId())) {
            throw new ApiException("Acesso negado", HttpStatus.FORBIDDEN);
        }
        return conversa;
    }

    private ChatMensagemResponseDTO toMensagemDto(ChatMensagem m, UUID conversaId) {
        if (m.getConversaId() == null) {
            m.setConversaId(conversaId);
        }
        String nome = userService.requireById(m.getRemetenteUserId()).getNome();
        return ChatMensagemResponseDTO.from(m, nome);
    }

    private ConversaResponseDTO toConversaDto(Conversa c, UUID meId) {
        UUID outroId = c.getUsuarioMenorId().equals(meId) ? c.getUsuarioMaiorId() : c.getUsuarioMenorId();
        User outro = userService.requireById(outroId);
        long naoLidas = chatMensagemRepository.countByConversa_IdAndRemetenteUserIdNotAndLidaFalse(c.getId(), meId);
        return new ConversaResponseDTO(
                c.getId(),
                outro.getId(),
                outro.getNome(),
                outro.getEmail(),
                outro.getRole() != null ? outro.getRole().name() : "CLIENTE",
                c.getUltimaMensagemEm(),
                naoLidas
        );
    }
}
