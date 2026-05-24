package com.example.barbearia.service.chatbot;

import com.example.barbearia.dto.chatbot.ChatbotRequestDTO;
import com.example.barbearia.dto.chatbot.ChatbotResponseDTO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class ChatbotService {

    private static final Pattern NAO_LETRAS = Pattern.compile("[^a-z0-9\\s]");

    private record Faq(String id, String categoria, List<String> gatilhos, String resposta, List<String> sugestoes) {
    }

    private static final List<Faq> FAQS = List.of(
            faq("saudacao", "GERAL",
                    List.of("ola", "oi", "bom dia", "boa tarde", "boa noite", "ajuda", "menu", "inicio", "começar", "comecar"),
                    """
                    Olá! Sou o assistente virtual da Street Barber.
                    Posso explicar cadastro, login, agendamentos, cancelamentos, atrasos, perfis, e-mails e relatórios.
                    Toque em uma sugestão abaixo ou digite sua pergunta com suas palavras.""",
                    List.of("Como agendar?", "Como cancelar?", "Esqueci minha senha")),
            faq("cadastro", "CADASTRO",
                    List.of("cadastr", "registr", "criar conta", "nova conta", "sign up", "me cadastrar", "criar usuario"),
                    """
                    Para ser cliente: faça cadastro com e-mail, senha e perfil CLIENTE (tela de registro ou POST /auth/register).
                    E-mail e telefone não podem já existir no sistema.
                    Barbeiro e recepcionista só o administrador cria.""",
                    List.of("Como agendar?", "Quais perfis existem?")),
            faq("login", "LOGIN",
                    List.of("login", "entrar", "acessar", "logar", "token", "jwt", "autentic"),
                    """
                    Entre com e-mail e senha. O sistema devolve um token JWT.
                    Nas próximas requisições use: Authorization: Bearer <seu_token>.
                    Conta desativada não entra — fale com a barbearia.""",
                    List.of("Esqueci minha senha", "Como me cadastrar?")),
            faq("senha", "SENHA",
                    List.of("senha", "recuper", "redefin", "esqueci", "forgot", "reset", "esqueci minha senha", "trocar senha"),
                    """
                    Recuperação de senha:
                    1) Informe seu e-mail em "Esqueci minha senha".
                    2) Abra o link recebido no e-mail.
                    3) Defina a nova senha (mínimo 6 caracteres).
                    Se não chegou o e-mail, veja a pasta spam.""",
                    List.of("Como me cadastrar?", "Não recebi o e-mail")),
            faq("agendar", "AGENDAMENTO",
                    List.of("agendar", "marcar", "horario", "reserva", "novo agendamento", "como agendar", "fazer agendamento", "marcar horario"),
                    """
                    Como agendar:
                    1) Faça login como cliente.
                    2) Escolha barbeiro, serviço e data/hora.
                    3) Confirme o agendamento.
                    Regras: entre 08h e 18h, data futura, sem conflito na agenda do barbeiro.
                    Você recebe e-mail de confirmação; barbeiro e equipe também são avisados.""",
                    List.of("Como cancelar?", "Informar atraso", "Ver meus agendamentos")),
            faq("listar_agendamento", "AGENDAMENTO",
                    List.of("meus agendamento", "listar agendamento", "ver agendamento", "consultar horario", "minha agenda"),
                    """
                    Ver agendamentos:
                    • Cliente: lista os seus horários na área de agendamentos.
                    • Barbeiro: vê a agenda do profissional.
                    • Admin/recepção: visão geral de todos.""",
                    List.of("Como agendar?", "Como cancelar?")),
            faq("cancelar", "CANCELAMENTO",
                    List.of("cancel", "desmarcar", "remover agendamento", "como cancelar", "cancelar horario", "desmarcar horario"),
                    """
                    Como cancelar:
                    Abra o agendamento e use cancelar (ou DELETE /agendamentos/{id} na API).
                    Cliente cancela o próprio horário; barbeiro cancela os da sua agenda; admin/recepção podem cancelar qualquer um.
                    Não dá para cancelar agendamento já concluído.
                    Todos recebem e-mail de cancelamento.""",
                    List.of("Como agendar?", "Informar atraso")),
            faq("atraso", "ATRASO",
                    List.of("atras", "demor", "vou chegar", "minutos", "informar atraso", "vou me atrasar", "chegar atrasado"),
                    """
                    Informar atraso (cliente):
                    No agendamento, informe quantos minutos (1 a 180) e o motivo.
                    Equipe e barbeiro recebem e-mail e alerta no sistema.
                    Podem confirmar o atraso (reajuste na agenda conforme os minutos informados) e conversar com você por mensagens.""",
                    List.of("Como cancelar?", "Horário de funcionamento")),
            faq("servicos", "SERVICOS",
                    List.of("servico", "corte", "barba", "preco", "valor", "duracao", "quanto custa"),
                    """
                    Serviços: corte, barba, combos etc., cada um com preço e duração.
                    A duração define quanto tempo o horário ocupa na agenda.
                    Cadastro de serviços: apenas administrador.""",
                    List.of("Como agendar?", "Horário de funcionamento")),
            faq("barbeiro", "BARBEIRO",
                    List.of("barbeiro", "profissional", "cabeleireiro", "barbeiros"),
                    """
                    Barbeiros aparecem na lista de profissionais ativos para você escolher ao agendar.
                    O barbeiro vê a própria agenda e pode atualizar status do atendimento.
                    Novo barbeiro com login: criado pelo admin.""",
                    List.of("Como agendar?", "Status do agendamento")),
            faq("perfis", "PERFIS",
                    List.of("perfil", "role", "permis", "tipos de usuario", "quais perfis", "diferenca cliente"),
                    """
                    Perfis do sistema:
                    • CLIENTE — agenda e cancela os próprios horários.
                    • BARBEIRO — agenda do profissional e status.
                    • RECEPCIONISTA — visão geral e relatórios.
                    • ADMIN — equipe, serviços e relatórios.""",
                    List.of("Como me cadastrar?", "Relatórios")),
            faq("admin", "ADMIN",
                    List.of("admin", "administrador", "gestao", "equipe", "desativar usuario", "criar barbeiro"),
                    """
                    Administrador gerencia a equipe: listar, criar barbeiro/recepcionista e ativar ou desativar usuários.
                    Usuário desativado não faz login.""",
                    List.of("Criar barbeiro", "Relatórios")),
            faq("email", "EMAIL",
                    List.of("email", "e-mail", "notific", "template", "mensagem", "nao recebi", "spam", "lixo"),
                    """
                    Enviamos e-mails automáticos (tema preto e verde neon): boas-vindas, senha, confirmação, lembrete, cancelamento, status e atraso.
                    Não chegou? Veja spam/lixo eletrônico e confira se o e-mail da conta está correto.""",
                    List.of("Esqueci minha senha", "Como agendar?")),
            faq("horario", "HORARIO",
                    List.of("funcionamento", "abre", "fecha", "horario loja", "que horas", "08", "18", "aberto"),
                    """
                    Funcionamos das 08:00 às 18:00.
                    Agendamentos só nesse intervalo e não podem terminar depois do fechamento.""",
                    List.of("Como agendar?", "Conflito de horário")),
            faq("status", "STATUS",
                    List.of("status", "confirmado", "concluido", "faltou", "andamento", "agendado"),
                    """
                    Status: AGENDADO, CONFIRMADO, EM_ANDAMENTO, CONCLUIDO, CANCELADO, FALTOU.
                    Barbeiro, recepção ou admin atualizam conforme o atendimento avança.""",
                    List.of("Como cancelar?", "Relatórios")),
            faq("relatorio", "RELATORIO",
                    List.of("relatorio", "faturamento", "resumo", "estatistica", "vendas"),
                    """
                    Relatórios (admin e recepcionista): resumo por período, por barbeiro e faturamento.
                    Use datas no formato ano-mês-dia.""",
                    List.of("Perfis do sistema", "Quem é admin?")),
            faq("validacao", "VALIDACAO",
                    List.of("duplicad", "ja cadastrad", "email existe", "telefone repetido", "cpf"),
                    """
                    O sistema bloqueia e-mail ou telefone já usado por outra conta.
                    CPF duplicado também não é permitido no cadastro de cliente.""",
                    List.of("Como me cadastrar?", "Erro ao cadastrar")),
            faq("erro", "ERRO",
                    List.of("401", "403", "404", "409", "erro", "nao autorizado", "negado", "nao funciona", "deu erro"),
                    """
                    Erros comuns:
                    • Sem login ou token inválido — entre de novo.
                    • Sem permissão — seu perfil não pode essa ação.
                    • Não encontrado — id ou rota incorretos.
                    • Conflito — e-mail, telefone ou horário já ocupado.""",
                    List.of("Como fazer login?", "Como agendar?")),
            faq("contato", "GERAL",
                    List.of("contato", "telefone barbearia", "endereco", "onde fica", "falar com", "atendimento humano"),
                    """
                    Para assuntos específicos da sua conta ou horário, use os dados de contato da barbearia no app ou ligue ao estabelecimento.
                    Posso ajudar com o uso do sistema — diga o que você quer fazer.""",
                    List.of("Como agendar?", "Horário de funcionamento")),
            faq("api", "API",
                    List.of("api", "endpoint", "localhost", "8080", "backend", "integracao"),
                    """
                    A API roda em http://localhost:8080 (desenvolvimento).
                    Principais rotas: /auth, /agendamentos, /servicos, /barbeiros, /chatbot/mensagem.""",
                    List.of("Como agendar?", "Como fazer login?")),
            faq("pagamento", "PAGAMENTO",
                    List.of("pagamento", "pagar", "pix", "cartao", "dinheiro", "comprovante", "qr code", "maquininha"),
                    """
                    Pagamentos:
                    • Ao concluir o atendimento, PIX gera QR/copia-e-cola no perfil do cliente.
                    • Cliente pode enviar comprovante; equipe confirma no sistema.
                    • Cartão/dinheiro: recepção, barbeiro ou admin registram e marcam como pago.""",
                    List.of("Como agendar?", "Como cancelar?")),
            faq("comissao", "COMISSAO",
                    List.of("comissao", "comissões", "folha", "pagar barbeiro"),
                    """
                    Comissões: cada atendimento concluído gera comissão do barbeiro no mês.
                    Admin vê folhas por barbeiro/mês, altera status (a pagar, em andamento, pago) e exporta PDF.""",
                    List.of("Relatórios", "Perfis do sistema")),
            faq("chat", "CHAT",
                    List.of("chat", "conversa", "mensagem", "bate papo", "falar com", "anexo", "foto no chat"),
                    """
                    Chat interno: lista de usuários, abra conversa e envie texto ou anexe foto/PDF.
                    Tempo real via WebSocket (/ws). Alertas quando chegar mensagem nova.""",
                    List.of("Como agendar?", "Informar atraso")),
            faq("sobre_nos", "SOBRE_NOS",
                    List.of("sobre nos", "sobre nós", "noticia", "notícias", "jornal", "elogio", "contratacao", "novidade"),
                    """
                    Sobre Nós: feed público com notícias, contratações, elogios e destaques.
                    Admin publica texto e fotos. Avaliações podem virar publicação em destaque.""",
                    List.of("Como agendar?", "Avaliações")),
            faq("avaliacao", "AVALIACAO",
                    List.of("avaliar", "avaliação", "nota", "estrela", "feedback", "comentario cliente"),
                    """
                    Após atendimento concluído o cliente pode avaliar (nota 1–5 e comentário).
                    Admin pode destacar elogios na seção Sobre Nós.""",
                    List.of("Sobre nós", "Como agendar?")),
            faq("alerta", "ALERTA",
                    List.of("alerta", "notificacao sistema", "sininho", "avisos"),
                    """
                    Alertas no app: atrasos, chat, agendamentos. Lista em /alertas e marque como lido.""",
                    List.of("Informar atraso", "Chat"))
    );

    /** Perguntas exatas dos botões de sugestão → id da FAQ */
    private static final Map<String, String> ALIAS_SUGESTOES = Map.ofEntries(
            Map.entry("como agendar", "agendar"),
            Map.entry("como cancelar", "cancelar"),
            Map.entry("esqueci minha senha", "senha"),
            Map.entry("informar atraso", "atraso"),
            Map.entry("como me cadastrar", "cadastro"),
            Map.entry("nao recebi o e-mail", "email"),
            Map.entry("nao recebi o email", "email"),
            Map.entry("ver meus agendamentos", "listar_agendamento"),
            Map.entry("quais perfis existem", "perfis"),
            Map.entry("horario de funcionamento", "horario"),
            Map.entry("criar barbeiro", "admin"),
            Map.entry("relatorios", "relatorio"),
            Map.entry("como fazer login", "login"),
            Map.entry("formas de pagamento", "pagamento"),
            Map.entry("sobre nos", "sobre_nos"),
            Map.entry("sobre nós", "sobre_nos")
    );

    public ChatbotResponseDTO responder(ChatbotRequestDTO request) {
        String perguntaNorm = normalizar(request.mensagem());
        if (!StringUtils.hasText(perguntaNorm)) {
            return respostaFaq(buscarPorId("saudacao"), 1.0);
        }

        Faq porAlias = resolverAlias(perguntaNorm);
        if (porAlias != null) {
            return respostaFaq(porAlias, 1.0);
        }

        List<FaqScore> ranqueadas = FAQS.stream()
                .map(f -> new FaqScore(f, pontuar(f, perguntaNorm)))
                .filter(fs -> fs.score > 0.05)
                .sorted(Comparator.comparingDouble((FaqScore fs) -> fs.score).reversed())
                .toList();

        if (!ranqueadas.isEmpty() && ranqueadas.get(0).score >= 0.20) {
            return respostaFaq(ranqueadas.get(0).faq, Math.min(1.0, ranqueadas.get(0).score));
        }

        if (!ranqueadas.isEmpty()) {
            return respostaCombinada(request.mensagem(), ranqueadas);
        }

        return respostaGenericaInteligente(request.mensagem(), perguntaNorm);
    }

    private record FaqScore(Faq faq, double score) {
    }

    private ChatbotResponseDTO respostaCombinada(String perguntaOriginal, List<FaqScore> ranqueadas) {
        Faq principal = ranqueadas.get(0).faq;
        StringBuilder sb = new StringBuilder();
        sb.append("Sobre sua pergunta");
        if (StringUtils.hasText(perguntaOriginal)) {
            sb.append(" (\"").append(extrairTrecho(perguntaOriginal)).append("\")");
        }
        sb.append(":\n\n");
        sb.append(principal.resposta().trim());
        if (ranqueadas.size() > 1 && ranqueadas.get(1).score >= 0.10) {
            sb.append("\n\nTambém relacionado:\n").append(ranqueadas.get(1).faq.resposta().trim());
        }
        sb.append("\n\nSe ainda tiver dúvida, reformule ou escolha um tema abaixo.");
        List<String> sugestoes = ranqueadas.stream()
                .limit(3)
                .flatMap(fs -> fs.faq.sugestoes().stream())
                .distinct()
                .limit(4)
                .collect(Collectors.toList());
        return new ChatbotResponseDTO(sb.toString(), principal.categoria(), ranqueadas.get(0).score, sugestoes);
    }

    private Faq resolverAlias(String perguntaNorm) {
        if (ALIAS_SUGESTOES.containsKey(perguntaNorm)) {
            return buscarPorId(ALIAS_SUGESTOES.get(perguntaNorm));
        }
        for (var entry : ALIAS_SUGESTOES.entrySet()) {
            if (perguntaNorm.contains(entry.getKey()) || entry.getKey().contains(perguntaNorm)) {
                return buscarPorId(entry.getValue());
            }
        }
        return null;
    }

    private ChatbotResponseDTO respostaGenericaInteligente(String original, String perguntaNorm) {
        List<String> dicas = new ArrayList<>();
        if (contemAlgum(perguntaNorm, "agendar", "marcar", "horario")) {
            dicas.add("Para agendar: login como cliente → escolha barbeiro, serviço e horário (08h–18h).");
        }
        if (contemAlgum(perguntaNorm, "cancel", "desmarc")) {
            dicas.add("Para cancelar: abra seu agendamento e use a opção cancelar.");
        }
        if (contemAlgum(perguntaNorm, "senha", "login", "entrar")) {
            dicas.add("Login: e-mail + senha. Esqueceu a senha? Use recuperação por e-mail.");
        }
        if (contemAlgum(perguntaNorm, "pagamento", "pagar", "pix", "cartao", "comprovante")) {
            dicas.add("PIX: ao concluir o corte, o QR aparece no seu perfil; envie o comprovante e a equipe confirma.");
        }
        if (contemAlgum(perguntaNorm, "preco", "valor", "quanto", "custa")) {
            dicas.add("Preços dos serviços aparecem na lista ao agendar.");
        }
        if (contemAlgum(perguntaNorm, "plano", "mensal", "assinatura", "pacote")) {
            dicas.add("No momento não trabalhamos com planos mensais no sistema — agende serviços avulsos pelo app.");
        }
        if (contemAlgum(perguntaNorm, "chat", "mensagem", "foto", "anexo")) {
            dicas.add("No Chat você conversa com qualquer usuário e pode enviar fotos ou PDF.");
        }
        if (contemAlgum(perguntaNorm, "noticia", "sobre", "elogio", "contrat")) {
            dicas.add("Veja Sobre Nós no app: notícias, novidades e elogios publicados pelo admin.");
        }
        if (contemAlgum(perguntaNorm, "comissao", "relatorio", "faturamento")) {
            dicas.add("Relatórios e comissões ficam no painel admin/recepção.");
        }
        if (contemAlgum(perguntaNorm, "confirmar", "atraso", "15")) {
            dicas.add("A equipe pode confirmar seu atraso e reajustar a agenda pelos minutos que você informou.");
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Entendi sua pergunta");
        if (StringUtils.hasText(original)) {
            sb.append(" sobre \"").append(original.trim()).append("\"");
        }
        sb.append(".\n\n");
        if (!dicas.isEmpty()) {
            sb.append(String.join("\n\n", dicas)).append("\n\n");
        }
        sb.append("""
                Sou o assistente da Street Barber. Posso ajudar com agendamentos, pagamentos PIX, chat, atrasos, \
                avaliações, Sobre Nós, comissões e relatórios — pergunte do seu jeito.
                """);
        sb.append("\n\nTambém pode tocar em uma sugestão abaixo.");

        return new ChatbotResponseDTO(
                sb.toString().trim(),
                "GERAL",
                0.55,
                List.of("Como agendar?", "Formas de pagamento", "Informar atraso", "Sobre nós")
        );
    }

    private static boolean contemAlgum(String texto, String... termos) {
        for (String t : termos) {
            if (texto.contains(t)) {
                return true;
            }
        }
        return false;
    }

    private static String extrairTrecho(String s) {
        if (s == null) {
            return "";
        }
        return s.length() > 80 ? s.substring(0, 77) + "..." : s;
    }

    private ChatbotResponseDTO respostaFaq(Faq faq, double confianca) {
        return new ChatbotResponseDTO(
                faq.resposta().trim(),
                faq.categoria(),
                confianca,
                faq.sugestoes()
        );
    }

    private Faq buscarPorId(String id) {
        return FAQS.stream().filter(f -> f.id().equals(id)).findFirst().orElse(FAQS.get(0));
    }

    private static Faq faq(String id, String categoria, List<String> gatilhos, String resposta, List<String> sugestoes) {
        return new Faq(id, categoria, gatilhos, resposta, sugestoes);
    }

    /** Pontuação pelo melhor gatilho (não média), para "Como agendar?" funcionar */
    private static double pontuar(Faq faq, String pergunta) {
        double melhor = 0;
        for (String gatilho : faq.gatilhos()) {
            String g = normalizar(gatilho);
            if (g.isEmpty()) {
                continue;
            }
            if (pergunta.equals(g)) {
                return 1.0;
            }
            if (pergunta.contains(g) || g.contains(pergunta)) {
                melhor = Math.max(melhor, 0.85 + Math.min(0.15, g.length() / 50.0));
                continue;
            }
            int tokensMatch = 0;
            for (String token : pergunta.split("\\s+")) {
                if (token.length() >= 3 && g.contains(token)) {
                    tokensMatch++;
                }
            }
            if (tokensMatch > 0) {
                melhor = Math.max(melhor, 0.4 + (tokensMatch * 0.15));
            }
        }
        return melhor;
    }

    private static String normalizar(String texto) {
        if (texto == null) {
            return "";
        }
        String semAcento = Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return NAO_LETRAS.matcher(semAcento.toLowerCase(Locale.ROOT))
                .replaceAll(" ")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
