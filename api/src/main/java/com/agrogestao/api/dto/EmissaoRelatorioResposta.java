package com.agrogestao.api.dto;

import com.agrogestao.api.model.*;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.time.*;

@Schema(
    description = "Resposta do contrato do MVP",
    example =
        "{\"id\":1,\"produtorId\":1,\"usuarioId\":1,\"dataEmissao\":\"2026-07-01T12:00:00Z\",\"periodoInicio\":\"2026-01-01\",\"periodoFim\":\"2026-06-30\",\"conteudo\":{\"produtor\":{\"id\":1,\"nome\":\"Cadastro Fictício\",\"documento\":\"99900000188\",\"endereco\":\"Endereço Fictício\",\"profissao\":\"Agricultor de demonstração\",\"contato\":\"Contato Fictício\",\"consentimentoEm\":\"2026-01-01\",\"ativo\":true,\"consentimentoPendente\":false},\"propriedades\":[{\"id\":1,\"produtorId\":1,\"nome\":\"Cadastro Fictício\",\"areaTotal\":5.25,\"localizacao\":null}],\"plantios\":[{\"plantioId\":1,\"culturaNome\":\"Cultura Fictícia\",\"propriedadeNome\":\"Propriedade Fictícia\",\"tipoCiclo\":\"CURTO\",\"periodoInicio\":\"2026-01-01\",\"periodoFim\":\"2026-06-30\",\"quantidadeProduzida\":250.5,\"quantidadeVendida\":100,\"unidadeMedida\":\"kg\",\"receita\":450,\"custoDesembolso\":100.25,\"valorMaoObraFamiliar\":80,\"custoComMaoObraFamiliar\":180.25,\"resultado\":349.75}],\"despesas\":[{\"id\":1,\"produtorId\":1,\"categoriaDespesaId\":2,\"diariaId\":null,\"descricao\":\"Despesa Fictícia\",\"valorTotal\":100.25,\"dataFato\":\"2026-03-01\",\"dataColeta\":\"2026-03-02\",\"estornada\":false,\"rateios\":[{\"plantioId\":1,\"percentual\":1,\"valorRateado\":100.25}]}],\"diarias\":[{\"id\":1,\"plantioId\":1,\"valorReferenciaDiariaId\":1,\"natureza\":\"PAGA\",\"quantidadeDias\":1.25,\"valorUnitario\":80,\"valorTotal\":100.25,\"dataFato\":\"2026-03-01\",\"dataColeta\":\"2026-03-02\"}],\"receita\":450,\"custoDesembolso\":100.25,\"resultado\":349.75,\"valorMaoObraFamiliar\":80,\"ressalvaValorDeclarado\":\"Valores declarados pelo produtor, não comprovados por documento fiscal.\",\"ressalvaFinalidade\":\"Este documento não constitui garantia de aceitação para qualquer finalidade externa.\"}}")
public record EmissaoRelatorioResposta(
    Long id,
    Long produtorId,
    Long usuarioId,
    Instant dataEmissao,
    LocalDate periodoInicio,
    LocalDate periodoFim,
    ConteudoRelatorioResposta conteudo) {}
