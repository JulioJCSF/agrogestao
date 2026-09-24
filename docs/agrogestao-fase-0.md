# AgroGestão — Fase 0: Documentos de Pré-Projeto

Versão 2.3 — elaborada a partir das entrevistas iniciais com o Sindicato dos Trabalhadores Rurais de Redenção-CE.

**Histórico de versões**

| Versão | Alteração |
|---|---|
| 1.0 | Elaboração inicial dos documentos 0.1, 0.2 e 0.3 |
| 1.1 | Consolidação e priorização das dores (P01). Inclusão da seção 0.4 — Registro de Consolidação de Dores. Ajuste do item "Problema que resolve". Atualização da tabela de pendências. |
| 1.2 | Fechamento de P03. Identificação da autoridade ratificadora no sindicato. Definição do canal e do protocolo de validação. Inclusão da seção 0.5 — Protocolo de Validação. |
| 1.3 | Resultado de VAL01. D2 descartada, D3 confirmada, D5 registrada. Priorização de D1 revertida a status não ratificado. Inclusão de DA04 e dos riscos R1 e R2. |
| 1.4 | Encerramento de VAL01. DA03 decidida (custo de desembolso). DA02 decidida com ressalva legal: finalidade previdenciária do relatório. P02 fechada. Inclusão de DA05, DA06 e dos riscos R3 e R4. |
| 1.5 | Correção: inclusão da descrição completa dos riscos R3 e R4, citados na v1.4 mas não redigidos. |
| 1.6 | Correção de atribuição em DA02: a adoção do relatório para fim previdenciário é decisão da equipe, não declaração da interlocutora. DA06 ampliada para tratar do aval institucional como condição de validade. |
| 1.7 | Resultado de VAL02. D1 e D3 reconhecidas como complementares — encerrada a disputa por dor principal. D4 refutada. DA04, DA05 (apuração) e DA06 decididas. DA03 reaberta: registro de diárias entra no MVP. Inclusão de DA07 e do risco R5. R2 reduzido a médio. |
| 1.8 | Revisão completa de consistência: Termo de Abertura e Documento de Visão alinhados à refutação de D4 (ausência de registro prévio) e ao achado sobre formação de preço. Diárias e apuração por ciclo incluídas no escopo do MVP. |
| 1.9 | Decisão sobre R3: relatório será desenvolvido; uso previdenciário é responsabilidade do sindicato. Consulta ao orientador deixa de ser bloqueante. DA05 desvinculada de R3. |
| 2.0 | Resultado de VAL03. DA01 decidida (rateio proporcional à área). DA07 decidida pela equipe. Reinterpretação do formulário do sindicato: não existe documento a replicar — justificativas de DA03 e DA05 revistas, decisões mantidas. |
| 2.1 | Revisão de consistência pós-VAL03: remoção das referências remanescentes ao formulário do sindicato como documento existente. |
| 2.2 | Atualização de P04: convertida em RNF09 e na pendência P11 da Fase 1. Correção do texto de pendências: P02 consta como fechada, conforme a tabela. |
| 2.3 | Stack: Docker Compose passa a orquestrar só o banco em desenvolvimento (ADR-0009). |

---

## 0.1 Termo de Abertura

### Objetivo do projeto

Desenvolver e validar uma plataforma web de apoio à gestão da agricultura familiar, operada pelo Sindicato dos Trabalhadores Rurais de Redenção-CE, que permita registrar produção, custos, estoque e vendas dos produtores associados e devolver a eles informação organizada sobre o resultado econômico de cada cultura.

### Problema que resolve

Os produtores não mantêm registro das suas atividades. Não há caderno, planilha ou anotação sistemática: a informação existe apenas na memória de quem produziu. Sem registro não há apuração de custo, e sem custo apurado o produtor não sabe se o que recebe pela produção cobre o que gastou para produzi-la.

O preço de venda é formado pelo mercado e varia entre safra e entressafra — o produtor não o arbitra. Conhecer o custo, portanto, não muda o preço praticado: muda a decisão de **o que plantar** e **quando vender**, e revela qual cultura efetivamente compensa.

Soma-se a isso a ausência de documento que comprove a produção e a renda, necessário quando o produtor precisa demonstrar exercício de atividade rural.

**Dor em priorização:** a equipe propôs o desconhecimento do custo real de produção por cultura (D1) como dor principal. Em VAL01 o sindicato confirmou D1 como necessidade real, mas não ratificou sua prioridade, e confirmou também a falta de comprovação documental de produção e renda (D3). A escolha entre D1 e D3 foi submetida em VAL02. O detalhamento e o impacto de cada dor no escopo estão na seção 0.4.

### Escopo macro

**Dentro do escopo (MVP)**

- Autenticação de conta institucional do sindicato, com perfis de acesso
- Cadastro de produtores e propriedades
- Cadastro de culturas e ciclos produtivos
- Registro de produção
- Registro de despesas vinculadas à cultura e de receitas
- Registro de diárias de mão de obra, inclusive familiar
- Registro de vendas
- Apuração de resultado por cultura, seguindo o ciclo produtivo
- Controle básico de estoque
- Dashboard com indicadores essenciais
- Relatório exportável em formato aderente ao formulário emitido pelo sindicato, destinado à entrega ao produtor

**Fora do escopo desta versão**

- Acesso direto do produtor ao sistema (conta individual)
- Aplicativo móvel ou PWA
- Funcionamento offline
- Marketplace
- Integração meteorológica
- Sensores e Internet das Coisas
- Inteligência artificial e recomendações automáticas
- Mapa de propriedades e pontos de comercialização
- Módulo para cooperativas
- Previsão de demanda

### Prazo e marcos principais

Duração total: 15 semanas letivas, conforme cronograma da seção 12 do documento do projeto.

| Marco | Semana | Entrega |
|---|---|---|
| M1 | 2 | Dores consolidadas e priorizadas junto ao sindicato |
| M2 | 4 | Requisitos definidos e protótipo validado |
| M3 | 7 | Back-end funcional |
| M4 | 9 | Interface funcional |
| M5 | 11 | MVP integrado e testado |
| M6 | 12 | Oficina de capacitação realizada |
| M7 | 13 | Indicadores de impacto coletados |
| M8 | 15 | Relatório final e apresentação |

### Responsável pela decisão final

A decisão sobre escopo, prioridade e viabilidade cabe à **equipe de desenvolvimento**, em comunicação contínua com o **professor orientador**, que acompanha o projeto e detém veto acadêmico sobre decisões que comprometam os objetivos da disciplina extensionista.

O sindicato levanta as necessidades e valida se as regras implementadas correspondem à realidade do produtor, mas não define prioridade técnica nem prazo.

---

## 0.2 Documento de Visão

### Público-alvo

O projeto tem dois públicos distintos, e a diferença entre eles orienta todas as decisões de interface.

**Usuário operador — técnico ou funcionário do sindicato**

É quem efetivamente opera o sistema. Trabalha no notebook do próprio sindicato, na sede, com conexão WiFi disponível. Registra dados em nome dos produtores associados, a partir das informações que eles trazem. Possui familiaridade tecnológica intermediária.

**Beneficiário final — produtor rural / agricultor familiar**

Não acessa o sistema diretamente nesta versão. Fornece as informações ao técnico e recebe de volta os relatórios gerados. É para ele que a informação precisa fazer sentido, ainda que não seja ele quem opera a ferramenta.

Perfis de produtores atendidos: agricultores familiares, pequenos horticultores, produtores de frutas, hortaliças, raízes e grãos, associados ao sindicato parceiro.

### Diferencial em relação às soluções existentes

O AgroGestão não concorre com nenhum software atualmente em uso. A situação atual é a ausência de qualquer registro: nem sistema, nem caderno. A informação vive na memória do produtor e em conversas informais na comunidade.

O diferencial está em três pontos:

1. **Custo real por cultura.** Hoje não há apuração de custo. O sistema produz esse número e permite comparar culturas entre si, sustentando a decisão de o que plantar e quando vender. Não atua sobre o preço, que é formado pelo mercado.
2. **Criação do histórico.** Não se trata de consolidar registros dispersos: não existe registro prévio. O sistema é a primeira fonte de histórico de produção e despesa do produtor, recuperável por período e por cultura — e é isso que viabiliza o documento comprobatório.
3. **Gratuidade e acompanhamento.** A ferramenta é entregue sem custo e acompanhada de capacitação presencial, o que a diferencia de soluções comerciais voltadas ao agronegócio de médio e grande porte.

### Restrições conhecidas

**De prazo**

- 15 semanas letivas, sem possibilidade de extensão
- Equipe composta por estudantes com disponibilidade parcial

**De tecnologia**

- Stack definida e já dominada pela equipe: React com Vite e HeroUI no front-end; Java com Spring Boot no back-end; PostgreSQL com Flyway; Docker Compose orquestrando o banco de dados em desenvolvimento (ADR-0009)
- Sem orçamento para infraestrutura paga; hospedagem em produção a definir

**De ambiente de uso**

- Acesso exclusivamente por notebook na sede do sindicato, com WiFi disponível
- Interface deve ser projetada para desktop; responsividade para telas menores não é requisito do MVP
- Funcionamento offline não é necessário nesta versão

**De operação**

- O sistema é operado por um intermediário, não pelo beneficiário final
- O volume de dados depende da disponibilidade de uma única pessoa para realizar os registros

**De dados**

- O sindicato passará a armazenar dados financeiros de terceiros, o que exige consentimento formal simples dos produtores participantes

---

## 0.3 Matriz de Stakeholders

| Stakeholder | Papel no projeto | Classificação | Responsabilidade |
|---|---|---|---|
| Equipe de desenvolvimento | Executor | **Decide** | Define viabilidade, prioridade técnica e prazo; implementa e testa |
| Julio | Analista, padrões e qualidade | **Decide** | Ponto focal de análise, definição de padrões técnicos, documentação e controle de qualidade |
| Professor orientador | Orientação acadêmica | **Consultado / veto** | Acompanha a execução, orienta decisões e pode vetar rumos que comprometam os objetivos da disciplina |
| Cristina — presidente do sindicato | Autoridade ratificadora | **Ratifica** | Valida e ratifica as regras de negócio propostas pela equipe; responde pelo sindicato na decisão final |
| Sindicato dos Trabalhadores Rurais de Redenção-CE | Comunidade parceira | **Levanta e valida** | Informa as principais dores e valida se as regras implementadas correspondem à realidade do produtor |
| Técnico/funcionário do sindicato | Usuário operador | **Valida** | Valida usabilidade e viabilidade operacional do fluxo de registro |
| Sintia (titular) e Cesariano Rodrigues Fernandes (suplente) | Operadores designados (diretoria) | **Valida** | Responsáveis pelo lançamento das informações no sistema. Grafia do primeiro nome a confirmar — "Sinta da Silva Costa" em VAL01, "Sintia" em VAL02 |
| Produtores associados | Beneficiários finais | **Consultado / informado** | Fornecem informações no diagnóstico, validam a utilidade dos relatórios recebidos |
| Coordenação do curso | Institucional | **Informado** | Recebe resultados e relatório final |

### Canais e frequência

- **Equipe ↔ Cristina (ratificação):** WhatsApp, em rodadas de validação registradas conforme o protocolo da seção 0.5
- **Sindicato ↔ equipe:** comunicação livre e contínua, sem intermediários
- **Equipe ↔ professor:** acompanhamento periódico, com validação nos marcos
- **Equipe ↔ produtores:** contato concentrado nas fases de diagnóstico, capacitação e avaliação

### Ponto de atenção

O técnico do sindicato acumula os papéis de usuário operador e de validador operacional. Isso concentra risco: indisponibilidade ou desligamento dessa pessoa interrompe tanto o uso quanto a validação. Recomenda-se identificar um segundo operador desde a fase de capacitação.

---

## 0.4 Registro de Consolidação de Dores

### Origem

Todas as dores registradas nesta seção foram levantadas diretamente pelo Sindicato dos Trabalhadores Rurais de Redenção-CE durante as entrevistas iniciais. Nenhuma decorre de inferência da equipe de desenvolvimento.

### Dores levantadas

| ID | Dor | Quem sente | Situação |
|---|---|---|---|
| D1 | Desconhecimento do custo real de produção por cultura | Produtor | Confirmada — atendida em conjunto com D3 |
| D2 | Ausência de dados consolidados sobre os associados | Sindicato | **Descartada** — refutada em VAL01 |
| D3 | Falta de comprovação documental de produção e renda | Produtor | Confirmada — atendida em conjunto com D1 |
| D4 | Perda de histórico por extravio ou deterioração dos cadernos | Produtor | **Refutada em VAL02** — não há hábito de anotação |
| D5 | Dificuldade de manter jovens na produção agrícola | Sindicato / comunidade | Registrada — fora do escopo de software |
| D6 | Ausência de critério de precificação que considere o custo | Produtor | Confirmada em VAL02 — decorrente de D1 |

**Atualização após VAL01.** A priorização original da equipe (D1 como dor principal, demais secundárias) não foi ratificada. D2 foi refutada pela presidente do sindicato, que declarou já possuir clareza sobre os números de associados por comunidade — a dor era inferência da equipe. D3 foi confirmada. D5 surgiu espontaneamente e com ênfase, mas não é endereçável por software.

**Atualização após VAL02.** A escolha entre D1 e D3 foi submetida e **recusada pela interlocutora**, que afirmou serem as duas complementares. A especificação de relatório que ela forneceu confirma essa leitura: o documento desejado reúne produção, despesas, diárias e lucro — ou seja, apuração de custo e comprovação de atividade rural são o mesmo artefato com dois usos.

Não há, portanto, dor principal a ratificar. O MVP tem núcleo único: registro de produção e despesas alimentando um relatório que serve como análise de custo e, com timbre e assinatura, como comprovante.

D4 foi refutada: não há hábito de anotação em caderno. O sistema não digitaliza registro existente — cria o primeiro (ver `R5`). D6 foi registrada a partir da mesma resposta.

### Justificativa da priorização proposta pela equipe

D1 foi proposta como dor principal por dois motivos:

1. É a dor com maior impacto econômico direto sobre o beneficiário final. Sem custo apurado, o produtor não tem base para precificar nem para decidir o que plantar.
2. É a única cuja solução exige regra de negócio nova. As demais são resolvidas por consequência do registro estruturado dos dados.

**Status após VAL01:** proposta não ratificada. O terceiro argumento originalmente registrado — de que atender D1 atenderia parcialmente D2 — caiu com o descarte de D2. D3, confirmada na mesma rodada, disputa a posição de dor principal, e a decisão foi submetida em VAL02.

### Impacto de cada dor no escopo do MVP

**D1 — custo real por cultura.** Define o núcleo funcional do MVP. Exige registro de despesa vinculada ao plantio, apuração de custo por ciclo e relatório de resultado. `DA01` (rateio) e `DA03` (mão de obra familiar) foram decididas — ver tabela de decisões abaixo.

**D2 — dados consolidados dos associados.** Descartada. O sindicato declarou já possuir clareza sobre os números de agricultores por comunidade. Nenhuma funcionalidade do MVP se justifica por esta dor.

**D3 — comprovação documental.** Confirmada. A finalidade relatada em VAL01 é previdenciária: comprovação de exercício de atividade rural junto ao INSS, no ato da solicitação de benefício. A hipótese original da equipe (vínculo com crédito rural) não se confirmou.

A interlocutora não declarou que o relatório do sistema será adotado para esse fim — declarou que a necessidade existe, que não há modelo formal exigido e que o aval do sindicato é condição de validade do documento. **Empregar o relatório nessa função é decisão da equipe, ainda não submetida a validação.**

Caso essa decisão seja tomada, o módulo de relatórios deixa de ser entregável de gestão e passa a documento com finalidade probatória, o que impõe requisitos de contemporaneidade, retenção de longo prazo, aval institucional identificável e integridade (`DA05`, `DA06`).

Sob a premissa de R2, D3 alcança base maior de beneficiários que D1: rentabilidade serve a quem comercializa, comprovação de atividade rural serve a todos os associados, inclusive os de subsistência pura. A escolha entre D1 e D3 como dor principal foi submetida em VAL02.

**D4 — perda de histórico.** Refutada em VAL02. A interlocutora indica que não há costume de anotar produção e gastos. A dor não é perder o registro, é não haver registro. O sistema passa a ser a primeira fonte de histórico, alimentada pela memória do produtor no momento do atendimento — o que transfere o problema para a qualidade do dado de entrada (`R5`).

**D6 — ausência de critério de precificação.** Confirmada em VAL02. A interlocutora afirma que o produtor não recebe orientação de que precificar exige somar as despesas com a mão de obra do grupo familiar. Registre-se, porém, que o preço é formado pelo mercado e varia entre safra e entressafra: conhecer o custo não altera o preço praticado, altera a decisão de **o que plantar** e **quando vender**. A formulação do Documento de Visão sobre comparação entre preço e custo precisa ser revista nesse sentido.

**D5 — permanência dos jovens no campo.** Fora do escopo de software. Registrada por ter sido levantada espontaneamente e com ênfase pela presidente do sindicato. Não gera requisito, mas exige alinhamento explícito de expectativa (ver risco R1).

### Riscos identificados em VAL01

**R1 — Desalinhamento de expectativa (alto).** A resposta do sindicato associa a solução a orientação técnica, maquinário, insumos e retenção de jovens no campo — nenhum deles endereçável por software. Mitigação: comunicar explicitamente o limite do sistema ainda na Fase 1, conforme mensagem de encerramento de VAL02.

**R2 — Premissa de comercialização (alto).** O sindicato afirma que o agricultor familiar ainda produz majoritariamente para subsistência. Se a maior parte da produção não é vendida, o módulo de vendas terá baixo volume e o relatório de rentabilidade se aplica a uma fração menor dos associados do que o projeto assume. Mitigação: dimensionar a proporção que comercializa (`DA04`) antes de fechar os requisitos.

*Status após VAL02:* **reduzido a impacto médio.** Metade dos associados comercializa, por comércio local, feira e CEASA. A apuração de rentabilidade atinge metade da base, não uma fração marginal. Módulo de vendas mantido no MVP.

**R3 — Expectativa indevida sobre o valor probatório do relatório (alto).** Em VAL01 o sindicato afirmou que, tendo aval da entidade, o documento "certamente será aceito" como prova de exercício rural pelo INSS. A legislação e a jurisprudência não sustentam essa leitura: desde a Lei 13.843/2019 a comprovação do segurado especial se dá por autodeclaração ratificada por bases governamentais, e a declaração sindical é tratada como início de prova material, complementada por outros elementos — não como prova plena.

O risco recai sobre o beneficiário final: produtor que confie no relatório como garantia e tenha benefício negado sofre prejuízo concreto. É o único risco do projeto cujo dano atinge terceiro fora da equipe.

*Mitigação:*

1. Ressalva impressa no próprio relatório gerado: valores declarados pelo produtor, documento não constitui garantia de aceitação.
2. Vedação de linguagem de garantia em qualquer comunicação com sindicato ou produtores, inclusive na oficina de capacitação.
3. Comunicação explícita à presidente do sindicato sobre a natureza do documento, já que é ela quem o entrega ao produtor e hoje considera que o aval institucional assegura a aceitação.
4. Consulta ao professor orientador — ação pendente, **sem data definida e sem efeito bloqueante sobre o cronograma**.

*Decisão da equipe:* o relatório será desenvolvido. O uso do documento para fim previdenciário é decisão do sindicato, que responde por ela. O risco permanece aberto e monitorado porque a mitigação protege o beneficiário final, não a equipe — os itens 1 a 3 valem independentemente de quem assume a responsabilidade formal.

Fundamentação completa em `agrogestao-val01.md`, seção "Verificação legal sobre o uso previdenciário (DA02)".

**R4 — Operação difusa entre diretores (médio).** O sindicato indicou seis diretores habilitados a alimentar o sistema. Operação distribuída sem responsável definido produz registro inconsistente, multiplica o esforço de capacitação e dilui a responsabilidade pela qualidade do dado. Diretoria é cargo com mandato, sujeito a renovação.

*Mitigação:* concentrar o lançamento nos dois operadores indicados nominalmente (P02); demais diretores com acesso de consulta, não de lançamento.

*Status após VAL02:* titular e suplente definidos — Sintia (titular, por estar mais presente no atendimento diário) e Cesariano (suplente).

### Risco identificado em VAL02

**R5 — Ausência de registro prévio na origem do dado (alto).** VAL02 refutou D4: os produtores não têm o hábito de anotar produção e gastos. O projeto assumia que o sistema digitalizaria registros existentes em caderno. Não há caderno. O dado entra a partir da memória do produtor no momento do atendimento, o que compromete precisão, completude e periodicidade — e afeta tanto a apuração de custo quanto o valor probatório do relatório, que depende de contemporaneidade.

*Mitigação:* reduzir ao mínimo o conjunto de campos obrigatórios; registrar a data de coleta separada da data do fato; indicar no relatório que os valores são declarados pelo produtor; avaliar, na oficina com produtores, a entrega de um instrumento simples de anotação para uso entre atendimentos.

### Decisões transferidas para a Fase 1

| # | Decisão em aberto | Fase |
|---|---|---|
| DA01 | Critério de rateio de despesas entre plantios que compartilham despesa | **Decidida em VAL03** — proporcional à área; percentual gravado no lançamento |
| DA02 | Formato externo para relatórios | **Decidida em VAL01** — finalidade previdenciária (INSS), sem modelo formal citado |
| DA03 | O que constitui "custo" para o produtor | **Revista em VAL02** — custo de desembolso descreve a prática atual; diárias entram no MVP por pedido explícito da interlocutora e pela relação entre precificação e mão de obra familiar |
| DA04 | Proporção de associados que efetivamente comercializam a produção | **Decidida em VAL02** — metade comercializa (comércio local, feira, CEASA) |
| DA05 | Contemporaneidade, periodicidade de emissão e retenção do relatório | **Decidida** — apuração segue o ciclo da cultura (não o formulário); emissão datada e retenção de longo prazo como requisito próprio |
| DA06 | Aval institucional, identificação do emissor e integridade do documento | **Decidida em VAL02** — timbre do sindicato, assinatura do presidente e do agricultor; aval físico, sem requisito digital |
| DA07 | Critério de valoração da diária de mão de obra familiar | **Decidida pela equipe em VAL03** — valor de referência da região, com vigência; não ratificada pela interlocutora |

### Ratificação

Este registro consolida o entendimento da equipe sobre o levantamento realizado. A ratificação cabe a Cristina, presidente do sindicato, conforme definido em P03 e detalhado no protocolo da seção 0.5. A priorização de D1 como dor principal deve ser submetida na primeira rodada de validação (VAL01).

---

## 0.5 Protocolo de Validação

### Autoridade ratificadora

Cristina, presidente do Sindicato dos Trabalhadores Rurais de Redenção-CE, é a responsável pela ratificação das regras de negócio e pela decisão final em nome do sindicato.

### Canal

WhatsApp, por ser o canal de uso corrente e de resposta mais rápida. O canal é adequado à agilidade do projeto, mas não constitui registro rastreável por si só — daí o protocolo abaixo.

### Formato das rodadas

Cada rodada de validação recebe identificador sequencial (VAL01, VAL02, ...) e é registrada com:

| Campo | Conteúdo |
|---|---|
| ID | VALnn |
| Data de envio | — |
| Regras submetidas | IDs das RN ou decisões em aberto (DAnn) tratadas |
| Conteúdo enviado | Texto exato encaminhado, em linguagem de negócio |
| Resposta recebida | Transcrição da resposta de Cristina |
| Decisão resultante | Regra confirmada, ajustada ou rejeitada |
| Evidência | Export ou captura da conversa, anexada ao repositório do projeto |

### Regra de redação

As regras **não** são submetidas em formato técnico. A validação é feita por cenário concreto, com nomes, quantidades e valores plausíveis, terminando em pergunta fechada ou de escolha entre alternativas.

Exemplo de submissão adequada para DA01:

> "Um produtor plantou feijão e milho na mesma área. Gastou R$ 200 de combustível no trator, que serviu para as duas culturas. Esse gasto deve ser dividido meio a meio, ou proporcional ao tamanho de cada plantio?"

A regra de negócio numerada é redigida pela equipe **a partir** da resposta, não submetida a ela.

Justificativa: regra em formato técnico submetida por mensagem tende a receber aprovação por cortesia, não validação real. O erro decorrente disso só apareceria na oficina de capacitação, na semana 12, quando o custo de correção é máximo.

### Critério de regra ratificada

Uma regra é considerada ratificada quando há resposta explícita de Cristina à pergunta correspondente. Silêncio, resposta genérica de concordância ("tá bom", "pode ser") ou ausência de retorno **não** ratificam a regra — nesses casos a rodada é repetida com a pergunta reformulada.

### Ponto de atenção

A concentração da ratificação em uma única pessoa reproduz, no nível decisório, o mesmo risco já apontado na seção 0.3 para o técnico operador. Indisponibilidade prolongada de Cristina bloqueia a validação de regras de negócio. Mitigação: submeter regras em lotes por marco, evitando dependência de resposta pontual, e registrar em VAL01 quem a substitui na ausência.

---

## Pendências desta fase

| # | Pendência | Responsável | Prazo | Status | Bloqueia Fase 1 |
|---|---|---|---|---|---|
| P01 | Consolidação e priorização da dor principal apontada pelo sindicato | Sindicato + equipe | Semana 1–2 | **Fechada** — ver seção 0.4 | — |
| P02 | Identificação nominal do técnico operador e de um suplente | Sindicato | Semana 2 | **Fechada em VAL02** — Sintia titular, Cesariano suplente. Grafia do nome a confirmar | Não |
| P03 | Definição do responsável pela decisão final no sindicato (diretoria ou representante) | Sindicato | Semana 2 | **Fechada** — Cristina, presidente; ver seção 0.5 | — |
| P04 | Definição do modelo de consentimento dos produtores para guarda de dados financeiros | Equipe + sindicato | Semana 4 | **Convertida** — virou `RNF09` e a pendência `P11` (modelo do termo de consentimento), ambas em `agrogestao-fase-1-requisitos.md` | Não — vira RNF na Fase 1 |
| P05 | Definição do ambiente de hospedagem em produção | Equipe | Semana 7 | Aberta | Não |

P01, P02 e P03 estão fechadas. P04 foi convertida em `RNF09` e na pendência `P11`, ambas em `agrogestao-fase-1-requisitos.md`. **Nenhuma pendência bloqueia o início da Fase 1.** P05 segue aberta e rastreada, com prazo na semana 7.

A ratificação de D1 por Cristina ocorre na rodada VAL01, em paralelo ao início da Fase 1 — não é pré-requisito para começar o levantamento de requisitos.

---

## Impactos no documento do projeto

As decisões desta fase entram em conflito com trechos do documento atual. Ajustes a realizar em etapa posterior:

| Seção | Situação atual | Ajuste necessário |
|---|---|---|
| 7 | Prevê aplicação responsiva com acesso por celular e tablet | Reposicionar para desktop-first; mobile passa a evolução futura |
| 8.1 | Prevê cadastro e login de produtores | Substituir por conta institucional com perfis; produtor vira entidade cadastrada |
| 8.8 | Relatórios listados como funcionalidade comum | Elevar prioridade: é o único canal de entrega de informação ao produtor |
| 9 | Estoque e financeiro no mesmo nível de prioridade | Priorizar despesa vinculada à cultura e rentabilidade |
| 11.4 | Capacitação voltada a produtores | Redirecionar para o técnico operador, mantendo oficina de apresentação aos produtores |
| 13 | Indicadores medem uso do sistema pelos produtores | Reformular: medir mudança de comportamento do produtor, não uso direto |
| 14 | Lista dificuldade de acesso à internet como risco | Substituir por risco de dependência de operador único |
| 15 | Não menciona acesso direto do produtor | Incluir conta individual do produtor como evolução futura |
