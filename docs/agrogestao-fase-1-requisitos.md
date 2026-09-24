# AgroGestão — Fase 1: Requisitos Funcionais e Não-Funcionais

Versão 1.2

Requisitos numerados e rastreáveis do sistema. Cada requisito funcional aponta para a dor que o origina; cada regra de negócio da Fase 1 apontará de volta para um requisito daqui.

**Documentos relacionados**

- `agrogestao-fase-1-glossario.md` — os termos usados aqui têm significado único, definido lá
- `agrogestao-fase-1-mer.md` — entidades e relacionamentos que sustentam estes requisitos
- `agrogestao-fase-0.md` — dores, decisões e riscos citados na coluna Origem
- `agrogestao-fase-2-padroes-tecnicos.md` — como implementar, não o que implementar

**Como ler**

- **Prioridade alta** — núcleo do MVP. Sem isso o sistema não entrega valor.
- **Prioridade média** — entra se o cronograma permitir. Primeiro candidato a corte.
- A coluna **Origem** rastreia até a dor (`Dnn`), a decisão validada (`DAnn`), o risco (`Rnn`) ou a decisão de modelagem (`Mnn`).

---

## 1. Autenticação e controle de acesso

| ID | Requisito | Prior. | Origem |
|---|---|---|---|
| RF01 | O sistema deve autenticar usuários por credenciais vinculadas à conta institucional do sindicato | Alta | Fase 0 |
| RF02 | O sistema deve suportar perfis de acesso distintos: lançamento e consulta | Alta | R4 |
| RF03 | O sistema deve registrar qual usuário criou cada lançamento | Alta | R4 |
| RF04 | O sistema deve encerrar a sessão após período de inatividade | Média | Fase 0 |
| RF05 | O sistema deve permitir ao administrador cadastrar e desativar usuários | Média | R4 |

O produtor **não** é usuário. Não há requisito de login, cadastro ou recuperação de senha para ele.

---

## 2. Produtor e propriedade

| ID | Requisito | Prior. | Origem |
|---|---|---|---|
| RF06 | O sistema deve permitir cadastrar produtor com nome, documento de identificação, endereço, profissão e contato | Alta | D3 |
| RF07 | O sistema deve permitir cadastrar uma ou mais propriedades por produtor, com identificação, área total e localização | Alta | D1 |
| RF08 | O sistema deve permitir consultar produtores por nome ou documento | Alta | Operação |
| RF09 | O sistema deve impedir a exclusão de produtor que possua lançamentos, permitindo apenas inativação | Alta | D3 |
| RF50 | O sistema deve registrar a obtenção do consentimento do produtor para guarda de seus dados financeiros, com a data em que foi assinado | Alta | P04, RNF09 |

RF06 inclui endereço e profissão porque constam da especificação do relatório. São atributos do produtor, lidos pelo documento.

RF09 existe por causa do uso probatório: apagar produtor apagaria o histórico que sustenta o comprovante.

RF50 registra o consentimento no momento do cadastro, antes de qualquer lançamento. O termo é assinado pelo produtor, que é quem autoriza a guarda dos próprios dados; o sindicato o recebe e arquiva. O sistema guarda apenas o fato e a data, não o documento.

---

## 3. Cultura e plantio

| ID | Requisito | Prior. | Origem |
|---|---|---|---|
| RF10 | O sistema deve manter catálogo de culturas com nome, tipo de ciclo padrão e unidade de medida | Alta | Glossário |
| RF11 | O sistema deve permitir registrar um plantio vinculado a uma cultura e a uma propriedade, com área utilizada, data de início e previsão de colheita | Alta | D1 |
| RF12 | O sistema deve permitir que o plantio sobreponha o tipo de ciclo padrão da cultura | Média | Glossário |
| RF13 | O sistema deve permitir encerrar um plantio, registrando a data de encerramento | Alta | D1 |
| RF14 | O sistema deve listar os plantios de um produtor, com filtro por período e por situação | Alta | Operação |

A área do plantio (RF11) é obrigatória: é a base do rateio de despesa (`DA01`).

---

## 4. Produção

| ID | Requisito | Prior. | Origem |
|---|---|---|---|
| RF15 | O sistema deve permitir registrar produção colhida em um plantio, com quantidade, unidade de medida e data do fato | Alta | D1, D3 |
| RF16 | O sistema deve permitir mais de um registro de produção por plantio | Alta | Ciclo curto |
| RF17 | O sistema deve registrar a data de coleta separadamente da data do fato em todo lançamento de evento | Alta | R5 |

RF17 vale para produção, despesa, diária e venda. Como o dado vem da memória do produtor no atendimento, a distância entre as duas datas indica a confiabilidade do registro.

---

## 5. Despesa e rateio

| ID | Requisito | Prior. | Origem |
|---|---|---|---|
| RF18 | O sistema deve permitir registrar despesa com descrição, categoria, valor total e data do fato | Alta | D1 |
| RF19 | O sistema deve permitir vincular uma despesa a um ou mais plantios | Alta | DA01 |
| RF20 | O sistema deve ratear a despesa entre os plantios vinculados proporcionalmente à área de cada um | Alta | DA01 |
| RF21 | O sistema deve gravar o percentual e o valor rateado no momento do lançamento | Alta | DA01 |
| RF22 | O sistema deve manter catálogo de categorias de despesa | Média | D1 |
| RF23 | O sistema deve listar as despesas de um plantio com o valor efetivamente rateado a ele | Alta | D1 |

RF21 é a proteção contra reemissão divergente: corrigir a área de um plantio depois não pode alterar rateios já realizados, ou um relatório entregue em março deixa de bater com o mesmo relatório reemitido em junho.

---

## 6. Diária e valor de referência

| ID | Requisito | Prior. | Origem |
|---|---|---|---|
| RF24 | O sistema deve permitir registrar diária em um plantio, com data do fato, quantidade de dias e natureza (paga ou familiar) | Alta | DA03 |
| RF25 | O sistema deve permitir cadastrar valor de referência da diária da região, com vigência por período | Alta | DA07 |
| RF26 | O sistema deve aplicar à diária familiar o valor de referência vigente na data do fato, gravando-o no registro | Alta | DA07 |
| RF27 | O sistema deve gerar automaticamente uma despesa vinculada ao plantio quando a diária for de natureza paga, com rateio integral àquele plantio | Alta | M01 |
| RF28 | O sistema não deve permitir lançamento manual de despesa referente a diária já registrada | Alta | M01 |

RF26 garante que atualizar a referência não altere registros passados.

RF27 e RF28 formam um par: a despesa da diária paga é gerada pelo sistema, nunca lançada à mão. Exigir dois lançamentos manuais garante que uma hora falte um dos lados, e o custo fica errado sem sinal visível.

---

## 7. Venda e cliente

| ID | Requisito | Prior. | Origem |
|---|---|---|---|
| RF29 | O sistema deve permitir cadastrar cliente com nome e tipo de canal | Alta | DA04 |
| RF30 | O sistema deve permitir registrar venda com cliente, data do fato e um ou mais itens | Alta | M02 |
| RF31 | Cada item de venda deve indicar o plantio de origem, a quantidade, a unidade e o valor | Alta | M02 |
| RF32 | O sistema deve sugerir o plantio de origem quando houver apenas um plantio aberto da cultura informada | Média | R5 |
| RF33 | O sistema deve exibir o saldo disponível de um plantio, calculado como produção registrada menos quantidade vendida | Média | M03 |

RF33 é consulta, não controle de estoque. Não há entidade de estoque no modelo (`M03`), e o saldo é informativo — o sistema **não** deve bloquear venda por saldo insuficiente, já que a produção é registrada de memória e o saldo pode não refletir a realidade.

---

## 8. Apuração de resultado

| ID | Requisito | Prior. | Origem |
|---|---|---|---|
| RF34 | O sistema deve apurar o custo de desembolso de um plantio como a soma das despesas rateadas a ele no período | Alta | DA03 |
| RF35 | O sistema deve apurar a receita de um plantio como a soma dos itens de venda que o referenciam no período | Alta | M02 |
| RF36 | O sistema deve apurar o resultado do plantio como receita menos custo de desembolso | Alta | D1 |
| RF37 | O sistema deve apurar, como informação complementar, o custo com mão de obra familiar — custo de desembolso acrescido do valor das diárias familiares | Alta | DA03 |
| RF38 | O período de apuração deve derivar do ciclo da cultura: safra para ciclo longo, mês para ciclo curto | Alta | DA05 |
| RF39 | O sistema não deve somar diárias ao custo de desembolso | Alta | M01 |

RF39 é a vedação de dupla contagem. Diária paga já entra pelo registro de despesa que o sistema gera (RF27); somá-la de novo dobraria o custo do plantio sem que nada parecesse quebrado.

---

## 9. Relatório

| ID | Requisito | Prior. | Origem |
|---|---|---|---|
| RF40 | O sistema deve gerar relatório por produtor contendo dados pessoais, endereço, profissão, culturas produzidas, despesas, diárias e resultado do período | Alta | D1, D3 |
| RF41 | O relatório deve identificar a data de emissão e o período coberto | Alta | DA05 |
| RF42 | O relatório deve indicar que os valores são declarados pelo produtor | Alta | R3, R5 |
| RF43 | O relatório deve conter ressalva de que não constitui garantia de aceitação para qualquer finalidade externa | Alta | R3 |
| RF44 | O relatório deve ser exportável em formato adequado à impressão, com espaço para timbre e para as assinaturas do presidente e do agricultor | Alta | DA06 |
| RF45 | O sistema deve registrar cada emissão de relatório, com data, período coberto, operador emissor e conteúdo apurado no momento | Alta | DA05 |
| RF46 | O sistema deve permitir consultar relatórios emitidos anteriormente a um produtor | Alta | DA05 |
| RF47 | O relatório deve usar o termo "Lucro" para o valor que o sistema chama de resultado | Média | Glossário |

RF45 e RF46 são o que dá sentido à retenção de longo prazo: sem eles o sistema não responde o que foi entregue ao produtor em determinada data.

RF43 não depende do desfecho da consulta ao orientador. Protege o beneficiário final independentemente de quem assume a responsabilidade formal pelo uso do documento.

---

## 10. Painel

| ID | Requisito | Prior. | Origem |
|---|---|---|---|
| RF48 | O sistema deve apresentar painel com totais de produção, despesa, receita e resultado por período | Média | Fase 0 |
| RF49 | O painel deve permitir comparar o resultado entre plantios de culturas diferentes | Média | D1 |

RF49 é o que sustenta a decisão de o que plantar — o uso real da informação de custo, já que o preço é formado pelo mercado e não pelo produtor.

---

## 11. Requisitos não-funcionais

| ID | Requisito | Origem |
|---|---|---|
| RNF01 | A interface deve ser projetada para uso em desktop, em notebook na sede do sindicato. Responsividade para telas menores não é requisito desta versão | Fase 0 |
| RNF02 | O sistema deve operar com conexão de internet disponível; funcionamento offline não é requisito | Fase 0 |
| RNF03 | Mensagens de erro exibidas ao operador devem ser escritas em português claro, sem jargão técnico | Perfil do operador |
| RNF04 | O sistema deve reduzir ao mínimo os campos obrigatórios de lançamento | R5 |
| RNF05 | Os dados devem ser retidos por prazo longo, sem expurgo automático de registros de produção, despesa ou emissão de relatório | DA05 |
| RNF06 | O acesso aos dados de um produtor deve exigir autenticação; não deve haver acesso anônimo a qualquer informação financeira | Fase 0 |
| RNF07 | O sistema deve registrar data de criação, data de atualização e autor em todo lançamento | R4 |
| RNF08 | Valores monetários devem ser armazenados com precisão decimal fixa, sem ponto flutuante | Fase 2 |
| RNF09 | O sindicato deve obter consentimento formal simples do produtor para guarda de seus dados financeiros | P04 |
| RNF10 | O repositório do projeto não deve conter dado real de produtor, em nenhuma hipótese — incluindo massa de teste, seed e captura de tela | Fase 2 |

RNF09 é a conversão de P04, pendência da Fase 0, em requisito. O modelo de termo de consentimento ainda precisa ser definido com o sindicato — pendência `P11`.

---

## 12. Fora de escopo desta versão

Registrado para evitar reabertura:

- Acesso do produtor ao sistema, com conta própria
- Aplicativo móvel, PWA e funcionamento offline
- Controle de estoque como entidade, com lote, perda ou consumo próprio
- Assinatura digital ou mecanismo eletrônico de integridade do relatório
- Dashboard institucional consolidado para o sindicato
- Marketplace, integração meteorológica, sensores e recomendações automáticas

---

## Pendências desta fase

| # | Pendência | Bloqueia |
|---|---|---|
| P10 | Redigir as regras de negócio (`RNnn`) a partir destes requisitos | **Fechada** — RN01 a RN37 redigidas em `agrogestao-fase-1-regras-negocio.md` |
| P11 | Redigir o modelo de termo de consentimento a ser assinado pelo produtor (RNF09, RF50) | Não |
| P12 | Nome da entidade `PLANTIO` | **Fechada** — mantido "plantio" |
