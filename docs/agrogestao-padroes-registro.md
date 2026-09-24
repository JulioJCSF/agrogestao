# AgroGestão — Padrões de Registro

Versão 1.1

Define os identificadores, templates e regras de redação usados no registro de dores, decisões em aberto e rodadas de validação do projeto. Documento de referência: muda pouco, consultado sempre que um novo item é registrado.

**Documentos relacionados**

- `agrogestao-validacoes-indice.md` — status transversal dos itens e histórico das rodadas
- `agrogestao-valnn.md` — registro de cada rodada, um arquivo por rodada
- `agrogestao-fase-0.md` — seção 0.4 (consolidação de dores) e seção 0.5 (protocolo de validação)
- `projeto-extensionista-agrogestao.md` — documento oficial do projeto

---

## 1. Identificadores

| Prefixo | Significado | Formato | Onde nasce |
|---|---|---|---|
| `D` | Dor levantada pela comunidade | `Dnn` | Fase 0 — diagnóstico |
| `DA` | Decisão em aberto | `DAnn` | Qualquer fase, quando falta definição de negócio |
| `P` | Pendência de fase | `Pnn` | Fase em que foi identificada |
| `VAL` | Rodada de validação com a autoridade ratificadora | `VALnn` | Fase 0 em diante |
| `RF` / `RNF` | Requisito funcional / não-funcional | `RFnn` / `RNFnn` | Fase 1 |
| `RN` | Regra de negócio | `RNnn` | Fase 1 |
| `CT` | Caso de teste | `CTnn` | Fase 3 |

Numeração sequencial, sem reaproveitamento. Item descartado permanece registrado com status `Descartada` — nunca é removido, e seu número nunca é reutilizado.

### Cadeia de rastreabilidade

```
D (dor)  →  DA (decisão em aberto)  →  VAL (validação)  →  RN (regra)  →  CT (caso de teste)
```

Toda `RN` deve ser rastreável até ao menos uma `D`. Regra sem dor de origem é sinal de que a equipe está implementando preferência técnica, não necessidade da comunidade.

---

## 2. Template — Registro de Dor (`Dnn`)

```
### Dnn — [título curto da dor]

| Campo | Conteúdo |
|---|---|
| Quem sente | Produtor / sindicato / técnico operador |
| Origem | Entrevista, data e interlocutor |
| Situação | Priorizada / Secundária / Descartada |
| Atendimento no MVP | Integral / Parcial / Por consequência / Fora de escopo |

**Descrição**
[O problema em uma ou duas frases, na linguagem de quem relatou.]

**Impacto no escopo**
[O que essa dor exige do sistema. Se não exige nada além do já previsto, dizer isso.]

**Decisões em aberto derivadas**
[Lista de DAnn, ou "nenhuma".]
```

### Regra de preenchimento

O campo **Origem** distingue o que foi dito pela comunidade do que foi interpretado pela equipe. Se a formulação é da equipe, registrar como inferência e submeter a validação antes de tratar como fato.

---

## 3. Template — Decisão em Aberto (`DAnn`)

```
### DAnn — [título curto]

| Campo | Conteúdo |
|---|---|
| Origem | Dnn |
| Tipo | Regra de negócio / requisito / conformidade externa |
| Bloqueia | O que não pode avançar enquanto estiver aberta |
| Rodada | VALnn em que foi submetida |
| Status | Aberta / Submetida / Decidida / Prejudicada |

**Pergunta central**
[A dúvida em uma frase.]

**Alternativas**
1. [Alternativa] — consequência
2. [Alternativa] — consequência

**Decisão**
[Preencher após a rodada. Registrar a alternativa escolhida e a justificativa dada pelo ratificador.]
```

### Regra de sequenciamento

Decisões interdependentes não vão na mesma rodada. Se a resposta de `DAx` altera as alternativas de `DAy`, `DAy` espera a rodada seguinte. Perguntar as duas juntas produz resposta inconsistente.

---

## 4. Template — Rodada de Validação (`VALnn`)

```
## VALnn — [tema da rodada]

| Campo | Conteúdo |
|---|---|
| Data de envio | |
| Data de resposta | |
| Interlocutor | |
| Canal | WhatsApp |
| Itens submetidos | Dnn, DAnn, Pnn |
| Status | Preparada / Enviada / Respondida / Reformulada / Encerrada |
| Evidência | Caminho do export ou captura anexada ao repositório |

### Mensagens enviadas
[Texto exato, na ordem de envio. Cada mensagem identifica o item que submete.]

### Respostas recebidas
[Transcrição literal. Não parafrasear.]

### Decisões resultantes
| Item | Decisão | Consequência |
|---|---|---|

### Itens não resolvidos
[O que ficou sem resposta explícita e será reformulado na próxima rodada.]
```

---

## 5. Regras de redação das mensagens

Aplicam-se a toda mensagem enviada à autoridade ratificadora.

1. **Cenário, nunca regra técnica.** Submeter situação concreta com nomes, quantidades e valores plausíveis. Nunca enviar `RN07 — Origem: RF03 — Exceção:`.
2. **Pergunta fechada ou de escolha.** Terminar em alternativas, não em "o que você acha?". Pergunta aberta por mensagem gera resposta genérica.
3. **Uma pergunta por mensagem.** Bloco com três perguntas costuma receber resposta para uma só.
4. **Sem jargão.** Evitar: MVP, entidade, rastreabilidade, escopo, requisito, rateio, ratificar.
5. **Sem sugerir a resposta.** Não indicar qual alternativa a equipe prefere. Isso contamina a validação.
6. **Perguntar como é, não como deveria ser.** Quando a dúvida envolve prática do produtor, perguntar o que ele faz hoje. A equipe decide depois o que o sistema faz com isso.
7. **A regra numerada nasce depois.** A `RN` é redigida pela equipe a partir da resposta, e não submetida a ela.

### Critério de item ratificado

Item está ratificado quando há **resposta explícita à pergunta feita**. Não ratificam:

- Silêncio
- Concordância genérica ("tá bom", "pode ser", "perfeito")
- Resposta que não corresponde à pergunta enviada

Nesses casos o item retorna como `Reformulada` na rodada seguinte, com a pergunta **reescrita** — não com a mesma pergunta reenviada.

### Justificativa do protocolo

Regra em formato técnico submetida por mensagem tende a receber aprovação por cortesia, não validação real. O erro decorrente disso só apareceria na oficina de capacitação, na semana 12, quando o custo de correção é máximo.
