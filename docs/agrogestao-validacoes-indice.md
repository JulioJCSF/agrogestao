# AgroGestão — Índice de Validações

Controle das rodadas de validação com a autoridade ratificadora do sindicato. Este arquivo concentra o **status transversal**; o registro de cada rodada fica em arquivo próprio.

**Autoridade ratificadora:** Cristina — presidente do Sindicato dos Trabalhadores Rurais de Redenção-CE
**Canal:** WhatsApp, com alinhamento presencial pontual

**Documentos relacionados**

- `agrogestao-padroes-registro.md` — identificadores, templates e regras de redação
- `agrogestao-fase-0.md` — dores, decisões em aberto e **registro único dos riscos**
- `agrogestao-val01.md`, `agrogestao-val02.md`, ... — uma rodada por arquivo

---

## Regra de manutenção

Rodada encerrada não é editada. Correção de entendimento posterior entra na rodada seguinte ou em nota de correção datada, nunca reescreve a interpretação anterior — o valor do registro está em mostrar o que se sabia em cada momento.

Este índice é o único arquivo com status mutável. Riscos vivem apenas na Fase 0; arquivos de rodada referenciam, não copiam.

---

## Rodadas

| ID | Tema | Itens | Status | Arquivo |
|---|---|---|---|---|
| VAL01 | Ratificação da dor principal e definição de custo | D1, D2, D3, D4, DA02, DA03, P02 | **Encerrada** | `agrogestao-val01.md` |
| VAL02 | Escolha da dor principal, comercialização e uso previdenciário | D1, D3, D4, DA04, DA05, DA06, P02 | **Encerrada** | `agrogestao-val02.md` |
| VAL03 | Valoração da diária, rateio de despesa e coleta do formulário | DA01, DA07 | **Encerrada** | `agrogestao-val03.md` |

---

## Itens em circulação

### Dores

| ID | Descrição | Rodada | Status |
|---|---|---|---|
| D1 | Desconhecimento do custo real de produção por cultura | VAL01, VAL02 | Confirmada — atendida junto com D3 |
| D2 | Ausência de dados consolidados sobre os associados | VAL01 | **Descartada** — refutada |
| D3 | Falta de comprovação documental de produção e renda | VAL01, VAL02 | Confirmada — atendida junto com D1 |
| D4 | Perda de histórico por extravio dos cadernos | VAL01, VAL02 | **Refutada** — não há hábito de anotação |
| D5 | Dificuldade de manter jovens na produção agrícola | VAL01 | Registrada — fora do escopo de software |
| D6 | Ausência de critério de precificação que considere o custo | VAL02 | Confirmada |

### Decisões em aberto

| ID | Descrição | Rodada | Status |
|---|---|---|---|
| DA01 | Critério de rateio de despesas entre plantios que compartilham despesa | VAL03 | **Decidida** — proporcional à área |
| DA02 | Formato externo exigido para o relatório | VAL01 | **Decidida** |
| DA03 | Inclusão de mão de obra familiar no custo | VAL01, VAL02 | **Revista** — diárias entram no MVP |
| DA04 | Proporção de associados que comercializam | VAL02 | **Decidida** — metade |
| DA05 | Contemporaneidade, periodicidade e retenção do relatório | VAL02 | **Decidida** — apuração por ciclo; emissão datada e retenção de longo prazo como requisito próprio |
| DA06 | Aval institucional e integridade do documento | VAL02 | **Decidida** — aval físico |
| DA07 | Critério de valoração da diária de mão de obra familiar | VAL03 | **Decidida pela equipe** — não ratificada |

### Pendências de fase

| ID | Descrição | Rodada | Status |
|---|---|---|---|
| P02 | Identificação nominal do operador e do suplente | VAL01, VAL02 | **Encerrada** — Sintia titular, Cesariano suplente |

---

## Decisões consolidadas

Decisões já tomadas, com efeito sobre o escopo. Não voltam a rodada salvo fato novo.

| Item | Decisão | Efeito |
|---|---|---|
| D2 | Descartada | Dashboard institucional fora do escopo, inclusive como evolução futura |
| D4 | Refutada | Sistema é a primeira fonte de histórico, não digitalização de caderno. Origem de R5 |
| D1 e D3 | Complementares, não concorrentes | Núcleo único de MVP: registro de produção e despesas alimentando relatório de dupla finalidade — análise de custo e comprovante de atividade rural |
| DA02 | Não há modelo formal exigido. A necessidade externa relatada é comprovação de atividade rural junto ao INSS, e o aval do sindicato é apontado pela interlocutora como condição de validade | Adoção do relatório para essa finalidade é decisão da equipe, condicionada ao retorno do orientador (R3) |
| DA03 | Custo de desembolso descreve a prática atual do produtor; o formulário do sindicato contém campo de diárias | Registro de diárias entra no MVP. Origem de DA07 |
| DA04 | Metade dos associados comercializa — comércio local, feira e CEASA | R2 reduzido a médio; módulo de vendas mantido |
| DA05 | Apuração segue o ciclo da cultura: ciclo longo por safra, ciclo curto por mês. Emissão datada e retenção de longo prazo adotadas por decisão da equipe | Período de apuração derivado do ciclo produtivo, não do calendário. Retenção não depende mais de R3 |
| DA06 | Timbre do sindicato, assinatura do presidente e do agricultor | Aval físico; sem requisito de assinatura digital ou integridade eletrônica |
| P02 | Sintia titular, Cesariano suplente | Operação concentrada; demais diretores com acesso de consulta (mitigação de R4) |
| DA01 | Despesa compartilhada é rateada proporcionalmente à área de cada plantio | Relação despesa↔plantio é muitos-para-muitos, com percentual gravado no lançamento. Rateio vira regra de negócio explícita |
| DA07 | Valor de referência da diária da região, cadastrado pelo operador com vigência por período, gravado no registro no lançamento | Entidade de referência no modelo. **Decisão da equipe, não ratificada** — conferir com a interlocutora na apresentação do protótipo |
| Formulário do sindicato | Não existe documento a replicar. A lista de campos de VAL02 é especificação do relatório desejado | Pendência de coleta encerrada. Justificativas de DA03 e DA05 revistas; decisões mantidas |

---

## Riscos

Registro único em `agrogestao-fase-0.md`, seção 0.4. Referência rápida:

| ID | Risco | Impacto | Origem |
|---|---|---|---|
| R1 | Desalinhamento de expectativa quanto ao alcance do software | Alto | VAL01 |
| R2 | Premissa de comercialização | Médio — reduzido em VAL02 | VAL01 |
| R3 | Expectativa indevida sobre o valor probatório do relatório | Alto — aberto e monitorado | VAL01 |
| R4 | Operação difusa entre seis diretores | Médio — mitigado em VAL02 | VAL01 |
| R5 | Ausência de registro prévio na origem do dado | Alto | VAL02 |

---

## Pendências de coleta

- Confirmação da grafia do nome da operadora titular — "Sinta" em VAL01, "Sintia" em VAL02. Não bloqueia nada; resolver antes de constar em documento formal.

A coleta do exemplar do formulário foi **encerrada sem objeto**: não existe documento emitido a replicar.

---

## Decisão sobre R3

O relatório será desenvolvido. O uso do documento para fim previdenciário é decisão do sindicato, que responde por ela. `DA05` deixa de estar condicionada a esse retorno: emissão datada e retenção de longo prazo passam a ser requisito próprio do módulo, justificado pela qualidade do registro.

A consulta ao professor orientador permanece como ação pendente, sem data e **sem efeito bloqueante**. As mitigações que protegem o beneficiário final seguem válidas e independem dessa consulta: ressalva impressa no relatório, vedação de linguagem de garantia e comunicação explícita à presidente do sindicato sobre a natureza do documento.

---

## Próxima ação

Frentes abertas:

1. Comunicação à presidente sobre a natureza do relatório (mitigação de R3)
2. Conferência do critério de valoração da diária com a interlocutora, na apresentação do protótipo
