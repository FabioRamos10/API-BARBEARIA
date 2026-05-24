package com.example.barbearia.controller;

import com.example.barbearia.dto.StaffUserCreateDTO;
import com.example.barbearia.dto.StaffUserResponseDTO;
import com.example.barbearia.dto.StaffUserStatusDTO;
import com.example.barbearia.dto.StaffUserSummaryDTO;
import com.example.barbearia.service.AdminUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/admin/usuarios")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final AdminUserService adminUserService;

    @GetMapping
    public ResponseEntity<List<StaffUserSummaryDTO>> listar() {
        return ResponseEntity.ok(adminUserService.listarEquipe());
    }

    @PostMapping
    public ResponseEntity<StaffUserResponseDTO> criar(@RequestBody @Valid StaffUserCreateDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminUserService.criarUsuarioStaff(dto));
    }

    @PatchMapping("/{userId}/status")
    public ResponseEntity<StaffUserSummaryDTO> alterarStatus(
            @PathVariable UUID userId,
            @RequestBody @Valid StaffUserStatusDTO dto
    ) {
        return ResponseEntity.ok(adminUserService.alterarStatus(userId, dto));
    }
}
