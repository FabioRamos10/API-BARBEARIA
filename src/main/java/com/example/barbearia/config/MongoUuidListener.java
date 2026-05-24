package com.example.barbearia.config;

import com.example.barbearia.domain.*;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.BeforeConvertEvent;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.UUID;

@Component
public class MongoUuidListener extends AbstractMongoEventListener<Object> {

    @Override
    public void onBeforeConvert(BeforeConvertEvent<Object> event) {
        Object source = event.getSource();
        if (source == null) {
            return;
        }
        assignIdIfNull(source);
        syncReferenceIds(source);
        touchTimestamps(source);
    }

    private void assignIdIfNull(Object source) {
        if (source instanceof User u && u.getId() == null) {
            u.setId(UUID.randomUUID());
        } else if (source instanceof Cliente c && c.getId() == null) {
            c.setId(UUID.randomUUID());
        } else if (source instanceof Barbeiro b && b.getId() == null) {
            b.setId(UUID.randomUUID());
        } else if (source instanceof Servico s && s.getId() == null) {
            s.setId(UUID.randomUUID());
        } else if (source instanceof Agendamento a && a.getId() == null) {
            a.setId(UUID.randomUUID());
        } else if (source instanceof Pagamento p && p.getId() == null) {
            p.setId(UUID.randomUUID());
        } else if (source instanceof PasswordResetToken t && t.getId() == null) {
            t.setId(UUID.randomUUID());
        } else if (source instanceof Mensagem m && m.getId() == null) {
            m.setId(UUID.randomUUID());
        } else if (source instanceof Conversa c && c.getId() == null) {
            c.setId(UUID.randomUUID());
        } else if (source instanceof ChatMensagem cm && cm.getId() == null) {
            cm.setId(UUID.randomUUID());
        } else if (source instanceof Avaliacao av && av.getId() == null) {
            av.setId(UUID.randomUUID());
        } else if (source instanceof AlertaSistema al && al.getId() == null) {
            al.setId(UUID.randomUUID());
        } else if (source instanceof AtrasoMensagem am && am.getId() == null) {
            am.setId(UUID.randomUUID());
        } else if (source instanceof ComissaoLancamento cl && cl.getId() == null) {
            cl.setId(UUID.randomUUID());
        } else if (source instanceof FolhaComissaoBarbeiro f && f.getId() == null) {
            f.setId(UUID.randomUUID());
        } else if (source instanceof Publicacao pub && pub.getId() == null) {
            pub.setId(UUID.randomUUID());
        }
        assignEmbeddedIds(source);
    }

    private void assignEmbeddedIds(Object source) {
        if (source instanceof Agendamento a && a.getItens() != null) {
            for (AgendamentoItem item : a.getItens()) {
                if (item.getId() == null) {
                    item.setId(UUID.randomUUID());
                }
                if (item.getServico() != null && item.getServicoId() == null) {
                    item.setServicoId(item.getServico().getId());
                }
            }
        }
        if (source instanceof Publicacao p && p.getMidias() != null) {
            for (PublicacaoMidia midia : p.getMidias()) {
                if (midia.getId() == null) {
                    midia.setId(UUID.randomUUID());
                }
            }
        }
    }

    private void syncReferenceIds(Object source) {
        if (source instanceof Cliente c && c.getUser() != null && c.getUserId() == null) {
            c.setUserId(c.getUser().getId());
        }
        if (source instanceof Barbeiro b && b.getUser() != null && b.getUserId() == null) {
            b.setUserId(b.getUser().getId());
        }
        if (source instanceof Agendamento a) {
            if (a.getCliente() != null && a.getClienteId() == null) {
                a.setClienteId(a.getCliente().getId());
            }
            if (a.getBarbeiro() != null && a.getBarbeiroId() == null) {
                a.setBarbeiroId(a.getBarbeiro().getId());
            }
            if (a.getServico() != null && a.getServicoId() == null) {
                a.setServicoId(a.getServico().getId());
            }
        }
        if (source instanceof Pagamento p && p.getAgendamento() != null && p.getAgendamentoId() == null) {
            p.setAgendamentoId(p.getAgendamento().getId());
        }
        if (source instanceof PasswordResetToken t && t.getUser() != null && t.getUserId() == null) {
            t.setUserId(t.getUser().getId());
        }
        if (source instanceof Mensagem m && m.getAgendamento() != null && m.getAgendamentoId() == null) {
            m.setAgendamentoId(m.getAgendamento().getId());
        }
        if (source instanceof ChatMensagem cm && cm.getConversa() != null && cm.getConversaId() == null) {
            cm.setConversaId(cm.getConversa().getId());
        }
        if (source instanceof Avaliacao av && av.getAgendamento() != null && av.getAgendamentoId() == null) {
            av.setAgendamentoId(av.getAgendamento().getId());
        }
        if (source instanceof AtrasoMensagem am && am.getAgendamento() != null && am.getAgendamentoId() == null) {
            am.setAgendamentoId(am.getAgendamento().getId());
        }
        if (source instanceof ComissaoLancamento cl) {
            if (cl.getAgendamento() != null && cl.getAgendamentoId() == null) {
                cl.setAgendamentoId(cl.getAgendamento().getId());
            }
            if (cl.getBarbeiro() != null && cl.getBarbeiroId() == null) {
                cl.setBarbeiroId(cl.getBarbeiro().getId());
            }
        }
        if (source instanceof FolhaComissaoBarbeiro f && f.getBarbeiro() != null && f.getBarbeiroId() == null) {
            f.setBarbeiroId(f.getBarbeiro().getId());
        }
        if (source instanceof Publicacao pub && pub.getAvaliacao() != null && pub.getAvaliacaoId() == null) {
            pub.setAvaliacaoId(pub.getAvaliacao().getId());
        }
    }

    private void touchTimestamps(Object source) {
        LocalDateTime now = LocalDateTime.now();
        if (source instanceof Agendamento a) {
            if (a.getCreatedAt() == null) {
                a.setCreatedAt(now);
            }
            a.setUpdatedAt(now);
        } else if (source instanceof Publicacao p) {
            if (p.getCreatedAt() == null) {
                p.setCreatedAt(now);
            }
            if (p.getPublicadoEm() == null) {
                p.setPublicadoEm(p.getCreatedAt() != null ? p.getCreatedAt() : now);
            }
        } else if (source instanceof Pagamento p) {
            if (p.getCreatedAt() == null) {
                p.setCreatedAt(now);
            }
        } else if (source instanceof Mensagem m) {
            if (m.getCreatedAt() == null) {
                m.setCreatedAt(now);
            }
        } else if (source instanceof Conversa c) {
            if (c.getCreatedAt() == null) {
                c.setCreatedAt(now);
            }
        } else if (source instanceof ChatMensagem cm) {
            if (cm.getEnviadaEm() == null) {
                cm.setEnviadaEm(now);
            }
        } else if (source instanceof Avaliacao av) {
            if (av.getCreatedAt() == null) {
                av.setCreatedAt(now);
            }
        } else if (source instanceof AlertaSistema al) {
            if (al.getCreatedAt() == null) {
                al.setCreatedAt(now);
            }
        } else if (source instanceof AtrasoMensagem am) {
            if (am.getCreatedAt() == null) {
                am.setCreatedAt(now);
            }
        } else if (source instanceof ComissaoLancamento cl) {
            if (cl.getCreatedAt() == null) {
                cl.setCreatedAt(now);
            }
            if (cl.getAnoMes() == null && cl.getCreatedAt() != null) {
                cl.setAnoMes(YearMonth.from(cl.getCreatedAt()).toString());
            }
        } else if (source instanceof FolhaComissaoBarbeiro f) {
            f.setUpdatedAt(now);
        }
    }
}
