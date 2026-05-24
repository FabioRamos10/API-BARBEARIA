package com.example.barbearia.service;

import com.example.barbearia.domain.Avaliacao;
import com.example.barbearia.domain.Publicacao;
import com.example.barbearia.domain.PublicacaoMidia;
import com.example.barbearia.domain.TipoMidiaPublicacao;
import com.example.barbearia.domain.TipoPublicacao;
import com.example.barbearia.domain.User;
import com.example.barbearia.dto.PublicacaoCreateDTO;
import com.example.barbearia.dto.PublicacaoResponseDTO;
import com.example.barbearia.exception.ApiException;
import com.example.barbearia.repository.AvaliacaoRepository;
import com.example.barbearia.repository.PublicacaoRepository;
import com.example.barbearia.security.AuthzHelper;
import com.example.barbearia.storage.ArquivoStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PublicacaoService {

    private final PublicacaoRepository publicacaoRepository;
    private final AvaliacaoRepository avaliacaoRepository;
    private final UserService userService;
    private final ArquivoStorageService arquivoStorageService;

    @Transactional(readOnly = true)
    public List<PublicacaoResponseDTO> listarPublicadas() {
        return publicacaoRepository.findByPublicadoTrueOrderByPublicadoEmDesc().stream()
                .map(PublicacaoResponseDTO::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PublicacaoResponseDTO> listarPorTipo(TipoPublicacao tipo) {
        return publicacaoRepository.findByPublicadoTrueAndTipoOrderByPublicadoEmDesc(tipo).stream()
                .map(PublicacaoResponseDTO::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PublicacaoResponseDTO> listarTodasAdmin() {
        return publicacaoRepository.findAllWithMidias().stream().map(PublicacaoResponseDTO::from).toList();
    }

    @Transactional
    public PublicacaoResponseDTO criar(PublicacaoCreateDTO dto, Authentication auth) {
        return criarComArquivos(dto, null, List.of(), auth);
    }

    @Transactional
    public PublicacaoResponseDTO criarComArquivos(
            PublicacaoCreateDTO dto,
            MultipartFile capa,
            List<MultipartFile> anexos,
            Authentication auth
    ) {
        User admin = userService.requireByEmail(AuthzHelper.email(auth));
        Publicacao pub = montarPublicacao(dto, admin.getId());
        pub = publicacaoRepository.save(pub);
        anexarArquivos(pub, capa, anexos != null ? anexos : List.of());
        return PublicacaoResponseDTO.from(publicacaoRepository.save(pub));
    }

    @Transactional
    public PublicacaoResponseDTO atualizar(UUID id, PublicacaoCreateDTO dto) {
        return atualizarComArquivos(id, dto, null, List.of());
    }

    @Transactional
    public PublicacaoResponseDTO atualizarComArquivos(
            UUID id,
            PublicacaoCreateDTO dto,
            MultipartFile capa,
            List<MultipartFile> anexos
    ) {
        Publicacao pub = publicacaoRepository.findById(id)
                .orElseThrow(() -> new ApiException("Publicação não encontrada", HttpStatus.NOT_FOUND));
        pub.setTitulo(dto.titulo());
        pub.setConteudo(dto.conteudo());
        pub.setTipo(dto.tipo());
        if (dto.imagemUrl() != null) {
            pub.setImagemUrl(dto.imagemUrl());
        }
        if (dto.publicado() != null) {
            pub.setPublicado(dto.publicado());
        }
        if (dto.avaliacaoId() != null) {
            pub.setAvaliacao(avaliacaoRepository.findById(dto.avaliacaoId())
                    .orElseThrow(() -> new ApiException("Avaliação não encontrada", HttpStatus.NOT_FOUND)));
        }
        anexarArquivos(pub, capa, anexos != null ? anexos : List.of());
        return PublicacaoResponseDTO.from(publicacaoRepository.save(pub));
    }

    @Transactional
    public void excluir(UUID id) {
        publicacaoRepository.deleteById(id);
    }

    private Publicacao montarPublicacao(PublicacaoCreateDTO dto, UUID autorId) {
        Avaliacao avaliacao = null;
        if (dto.avaliacaoId() != null) {
            avaliacao = avaliacaoRepository.findById(dto.avaliacaoId())
                    .orElseThrow(() -> new ApiException("Avaliação não encontrada", HttpStatus.NOT_FOUND));
        }
        return Publicacao.builder()
                .titulo(dto.titulo())
                .conteudo(dto.conteudo())
                .tipo(dto.tipo())
                .imagemUrl(dto.imagemUrl())
                .avaliacao(avaliacao)
                .publicado(dto.publicado() == null || dto.publicado())
                .autorUserId(autorId)
                .build();
    }

    private void anexarArquivos(Publicacao pub, MultipartFile capa, List<MultipartFile> anexos) {
        int ordem = pub.getMidias() != null ? pub.getMidias().size() : 0;
        List<MultipartFile> todos = new ArrayList<>();
        if (capa != null && !capa.isEmpty()) {
            todos.add(capa);
        }
        todos.addAll(anexos.stream().filter(f -> f != null && !f.isEmpty()).toList());

        boolean primeira = capa == null || capa.isEmpty();
        for (MultipartFile file : todos) {
            var salvo = arquivoStorageService.salvarPublicacao(pub.getId(), file);
            TipoMidiaPublicacao tipo = (!primeira || pub.getImagemUrl() != null)
                    ? TipoMidiaPublicacao.GALERIA
                    : TipoMidiaPublicacao.CAPA;
            if (tipo == TipoMidiaPublicacao.CAPA) {
                pub.setImagemUrl(salvo.urlPublica());
                primeira = false;
            }
            pub.getMidias().add(PublicacaoMidia.builder()
                    .publicacao(pub)
                    .tipo(tipo)
                    .storagePath(salvo.storagePath())
                    .urlPublica(salvo.urlPublica())
                    .contentType(salvo.contentType())
                    .nomeArquivo(salvo.nomeOriginal())
                    .ordem(ordem++)
                    .build());
        }
    }
}
