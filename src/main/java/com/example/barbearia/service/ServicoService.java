package com.example.barbearia.service;

import com.example.barbearia.domain.Servico;
import com.example.barbearia.dto.ServicoCreateDTO;
import com.example.barbearia.dto.ServicoResponseDTO;
import com.example.barbearia.dto.ServicoUpdateDTO;
import com.example.barbearia.exception.ApiException;
import com.example.barbearia.repository.ServicoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ServicoService {

    private final ServicoRepository servicoRepository;

    @Transactional(readOnly = true)
    public Servico findById(UUID id) {
        return servicoRepository.findById(id)
                .orElseThrow(() -> new ApiException("Serviço não encontrado", HttpStatus.NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public ServicoResponseDTO findDtoById(UUID id) {
        return ServicoResponseDTO.from(findById(id));
    }

    @Transactional(readOnly = true)
    public List<ServicoResponseDTO> findAllAtivosDtos() {
        return servicoRepository.findByAtivoTrue().stream()
                .map(ServicoResponseDTO::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ServicoResponseDTO> findByCategoriaDto(String categoria) {
        return servicoRepository.findByCategoriaAndAtivoTrue(categoria).stream()
                .map(ServicoResponseDTO::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public ServicoResponseDTO criar(ServicoCreateDTO dto) {
        Servico servico = Servico.builder()
                .nome(dto.nome())
                .descricao(dto.descricao())
                .preco(dto.preco())
                .duracaoMinutos(dto.duracaoMinutos())
                .categoria(dto.categoria())
                .ativo(true)
                .build();
        return ServicoResponseDTO.from(servicoRepository.save(servico));
    }

    @Transactional
    public ServicoResponseDTO atualizar(UUID id, ServicoUpdateDTO dto) {
        Servico servico = findById(id);
        if (dto.nome() != null) {
            servico.setNome(dto.nome());
        }
        if (dto.descricao() != null) {
            servico.setDescricao(dto.descricao());
        }
        if (dto.preco() != null) {
            if (dto.preco().signum() <= 0) {
                throw new ApiException("Preço deve ser maior que zero", HttpStatus.BAD_REQUEST);
            }
            servico.setPreco(dto.preco());
        }
        if (dto.duracaoMinutos() != null) {
            if (dto.duracaoMinutos() <= 0) {
                throw new ApiException("Duração deve ser maior que zero", HttpStatus.BAD_REQUEST);
            }
            servico.setDuracaoMinutos(dto.duracaoMinutos());
        }
        if (dto.categoria() != null) {
            servico.setCategoria(dto.categoria());
        }
        if (dto.ativo() != null) {
            servico.setAtivo(dto.ativo());
        }
        return ServicoResponseDTO.from(servicoRepository.save(servico));
    }
}
