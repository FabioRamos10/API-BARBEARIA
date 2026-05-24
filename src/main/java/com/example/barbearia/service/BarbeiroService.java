package com.example.barbearia.service;

import com.example.barbearia.domain.Barbeiro;
import com.example.barbearia.dto.BarbeiroResponseDTO;
import com.example.barbearia.dto.BarbeiroUpdateDTO;
import com.example.barbearia.exception.ApiException;
import com.example.barbearia.repository.BarbeiroRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BarbeiroService {

    private final BarbeiroRepository barbeiroRepository;
    private final ContatoValidacaoService contatoValidacaoService;

    @Transactional(readOnly = true)
    public Barbeiro findById(UUID id) {
        return barbeiroRepository.findById(id)
                .orElseThrow(() -> new ApiException("Barbeiro não encontrado", HttpStatus.NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public Barbeiro findEntityByUserEmail(String email) {
        return barbeiroRepository.findByUserEmail(email)
                .orElseThrow(() -> new ApiException("Perfil de barbeiro não encontrado para este usuário", HttpStatus.NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public BarbeiroResponseDTO getMe(String email) {
        return BarbeiroResponseDTO.from(findEntityByUserEmail(email));
    }

    @Transactional
    public BarbeiroResponseDTO updateMe(String email, BarbeiroUpdateDTO dto) {
        Barbeiro barbeiro = findEntityByUserEmail(email);
        applyUpdate(barbeiro, dto);
        return BarbeiroResponseDTO.from(barbeiroRepository.save(barbeiro));
    }

    @Transactional
    public BarbeiroResponseDTO atualizar(UUID id, BarbeiroUpdateDTO dto) {
        Barbeiro barbeiro = findById(id);
        applyUpdate(barbeiro, dto);
        return BarbeiroResponseDTO.from(barbeiroRepository.save(barbeiro));
    }

    @Transactional(readOnly = true)
    public List<BarbeiroResponseDTO> findAllAtivosDtos() {
        return barbeiroRepository.findByAtivoTrue().stream()
                .map(BarbeiroResponseDTO::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<BarbeiroResponseDTO> findAllDtos() {
        return barbeiroRepository.findAll().stream()
                .map(BarbeiroResponseDTO::from)
                .collect(Collectors.toList());
    }

    private void applyUpdate(Barbeiro barbeiro, BarbeiroUpdateDTO dto) {
        if (dto.telefone() != null) {
            contatoValidacaoService.validarTelefoneDisponivel(dto.telefone(), null, barbeiro.getId());
            barbeiro.setTelefone(contatoValidacaoService.normalizarTelefone(dto.telefone()));
        }
        if (dto.percentualComissao() != null) {
            barbeiro.setPercentualComissao(dto.percentualComissao());
        }
        if (dto.especialidades() != null) {
            barbeiro.setEspecialidades(dto.especialidades());
        }
    }
}
