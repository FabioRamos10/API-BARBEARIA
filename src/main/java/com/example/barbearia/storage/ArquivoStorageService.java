package com.example.barbearia.storage;

import com.example.barbearia.exception.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

@Service
public class ArquivoStorageService {

    private static final Set<String> TIPOS_IMAGEM = Set.of("image/jpeg", "image/png", "image/webp", "image/gif");
    private static final Set<String> TIPOS_DOCUMENTO = Set.of("application/pdf");
    private static final Set<String> TIPOS_CHAT = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/gif", "application/pdf"
    );
    private static final Set<String> TIPOS_PUBLICACAO = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/gif", "application/pdf"
    );

    private final Path baseDir;
    private final String publicBaseUrl;

    public ArquivoStorageService(
            @Value("${app.upload.dir:uploads}") String uploadDir,
            @Value("${app.api.public-url:http://localhost:8080}") String publicBaseUrl
    ) throws IOException {
        this.baseDir = Path.of(uploadDir).toAbsolutePath().normalize();
        this.publicBaseUrl = publicBaseUrl.endsWith("/") ? publicBaseUrl.substring(0, publicBaseUrl.length() - 1) : publicBaseUrl;
        Files.createDirectories(baseDir.resolve("comprovantes"));
        Files.createDirectories(baseDir.resolve("publicacoes"));
        Files.createDirectories(baseDir.resolve("chat"));
    }

    public record ArquivoSalvo(String storagePath, String urlPublica, String contentType, String nomeOriginal) {
    }

    public ArquivoSalvo salvarComprovante(UUID pagamentoId, MultipartFile file) {
        validar(file, TIPOS_IMAGEM, TIPOS_DOCUMENTO);
        String ext = extensao(file.getContentType());
        Path destino = baseDir.resolve("comprovantes").resolve(pagamentoId + ext);
        copiar(file, destino);
        return new ArquivoSalvo(destino.toString(), null, file.getContentType(), file.getOriginalFilename());
    }

    public ArquivoSalvo salvarPublicacao(UUID publicacaoId, MultipartFile file) {
        validar(file, TIPOS_PUBLICACAO, Set.of());
        return salvarEmSubpasta("publicacoes", publicacaoId, file);
    }

    public ArquivoSalvo salvarChat(UUID conversaId, MultipartFile file) {
        validar(file, TIPOS_CHAT, Set.of());
        return salvarEmSubpasta("chat", conversaId, file);
    }

    private ArquivoSalvo salvarEmSubpasta(String pasta, UUID ownerId, MultipartFile file) {
        String ext = extensao(file.getContentType());
        String nomeArquivo = UUID.randomUUID() + ext;
        Path destino = baseDir.resolve(pasta).resolve(ownerId.toString()).resolve(nomeArquivo);
        try {
            Files.createDirectories(destino.getParent());
        } catch (IOException e) {
            throw new ApiException("Falha ao preparar pasta de upload", HttpStatus.INTERNAL_SERVER_ERROR);
        }
        copiar(file, destino);
        String url = publicBaseUrl + "/media/" + pasta + "/" + ownerId + "/" + nomeArquivo;
        return new ArquivoSalvo(
                destino.toString(),
                url,
                file.getContentType(),
                file.getOriginalFilename() != null ? file.getOriginalFilename() : nomeArquivo
        );
    }

    public Path resolverMidia(String categoria, UUID ownerId, String nomeArquivo) {
        Path destino = baseDir.resolve(categoria).resolve(ownerId.toString()).resolve(nomeArquivo).normalize();
        if (!destino.startsWith(baseDir)) {
            throw new ApiException("Acesso negado ao arquivo", HttpStatus.FORBIDDEN);
        }
        if (!Files.exists(destino)) {
            throw new ApiException("Arquivo não encontrado", HttpStatus.NOT_FOUND);
        }
        return destino;
    }

    public Path resolver(String path) {
        if (!StringUtils.hasText(path)) {
            throw new ApiException("Arquivo não encontrado", HttpStatus.NOT_FOUND);
        }
        Path p = Path.of(path).normalize();
        if (!p.startsWith(baseDir)) {
            throw new ApiException("Acesso negado ao arquivo", HttpStatus.FORBIDDEN);
        }
        if (!Files.exists(p)) {
            throw new ApiException("Arquivo não encontrado", HttpStatus.NOT_FOUND);
        }
        return p;
    }

    private static void validar(MultipartFile file, Set<String> imagens, Set<String> docs) {
        if (file == null || file.isEmpty()) {
            throw new ApiException("Arquivo é obrigatório", HttpStatus.BAD_REQUEST);
        }
        String contentType = file.getContentType();
        Set<String> permitidos = new java.util.HashSet<>(imagens);
        permitidos.addAll(docs);
        if (contentType == null || !permitidos.contains(contentType)) {
            throw new ApiException("Formato inválido. Use JPG, PNG, WEBP, GIF ou PDF.", HttpStatus.BAD_REQUEST);
        }
    }

    private static String extensao(String contentType) {
        return switch (contentType) {
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            case "image/gif" -> ".gif";
            case "application/pdf" -> ".pdf";
            default -> ".jpg";
        };
    }

    private static void copiar(MultipartFile file, Path destino) {
        try {
            Files.copy(file.getInputStream(), destino, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new ApiException("Falha ao salvar arquivo", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
