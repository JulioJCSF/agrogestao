# AgroGestão — Fase 1: Regras de Negócio

Versão 1.2

Define como o sistema se comporta nos casos concretos. Cada regra aponta para o requisito que a origina; cada caso de teste da Fase 3 apontará de volta para uma regra daqui.

**Diferença entre RF e RN:** o requisito diz o que o sistema faz; a regra diz o que acontece quando os dados não são os ideais. A maior parte do valor deste documento está nas exceções.

**Documentos relacionados**

- `agrogestao-fase-1-requisitos.md` — os requisitos citados na coluna Origem
- `agrogestao-fase-1-glossario.md` — significado dos termos
- `agrogestao-fase-1-mer.md` — entidades e decisões de modelagem

---

## 1. Plantio

### RN01 — Área obrigatória e positiva
Um plantio só pode ser registrado com área maior que zero.
**Origem:** RF11, DA01 — a área é a base do rateio; plantio sem área torna o rateio indefinido.

### RN02 — Data de início não futura
A data de início de um plantio não pode ser posterior à data corrente.
**Origem:** RF11, R5

### RN03 — Encerramento posterior ao início
A data de encerramento não pode ser anterior à data de início.
**Origem:** RF13

### RN04 — Lançamento em plantio encerrado
Nenhum lançamento — produção, despesa, diária ou item de venda — pode ter data do fato posterior à data de encerramento do plantio.
**Tratamento:** rejeitar com HTTP 422 e mensagem indicando a data de encerramento.
**Origem:** RF13

### RN05 — Alteração de área não retroage
Alterar a área de um plantio não recalcula rateios já gravados.
**Origem:** RF21, DA01 — um relatório entregue ao produtor não pode divergir do mesmo relatório reemitido meses depois.

---

## 2. Rateio de despesa

### RN06 — Vínculo mínimo
Toda despesa deve estar vinculada a pelo menos um plantio.
**Origem:** RF19

### RN07 — Cálculo do rateio
O valor rateado a cada plantio é proporcional à sua área, em relação à soma das áreas de todos os plantios vinculados àquela despesa.

```
percentual(plantio) = área(plantio) / Σ áreas dos plantios vinculados
valor rateado       = valor total da despesa × percentual
```

**Origem:** RF20, DA01

### RN08 — Despesa em plantio único
Despesa vinculada a um só plantio recebe 100% e valor rateado igual ao valor total. Não é caso especial: é o cálculo da RN07 com um único termo.
**Origem:** RF20

### RN09 — Fechamento de centavos
A soma dos valores rateados deve ser exatamente igual ao valor total da despesa. Quando a divisão gerar dízima, a diferença residual é somada ao plantio de maior área; havendo empate, ao de menor identificador.
**Origem:** RF20, RF21
**Exemplo:** R$ 100,00 entre três plantios de área igual → 33,33 + 33,33 + 33,34.

> Sem esta regra, a soma dos custos dos plantios não bate com a soma das despesas, e a divergência aparece justamente no relatório entregue ao produtor.

### RN10 — Gravação no lançamento
Percentual e valor rateado são calculados uma vez, no lançamento, e gravados. Consultas leem o valor gravado; nunca recalculam.
**Origem:** RF21, RN05

### RN11 — Exclusão de despesa
Excluir uma despesa remove todos os seus rateios. Despesa já incluída em relatório emitido não pode ser excluída, apenas estornada: a própria despesa é marcada como estornada (`despesa.estornada` no DER), sem lançamento de ajuste à parte. O relatório já emitido não muda (`RN34`).
**Origem:** RF45, DA05

---

## 3. Diária e valor de referência

### RN12 — Natureza obrigatória
Toda diária deve ter natureza declarada: paga ou familiar.
**Origem:** RF24

### RN13 — Valoração da diária familiar
À diária familiar aplica-se o valor de referência **vigente na data do fato**, copiado para o registro no momento do lançamento.
**Origem:** RF26, DA07

### RN14 — Ausência de referência vigente
Não havendo valor de referência vigente para a data do fato, o lançamento de diária familiar é rejeitado, com mensagem orientando o cadastro da referência.
**Origem:** RF25, RF26

### RN15 — Valor da diária paga
A diária paga tem valor informado no lançamento, não herdado da referência — é desembolso efetivo, de valor conhecido.
**Origem:** RF24

### RN16 — Geração de despesa
Diária de natureza paga gera automaticamente uma despesa vinculada, de categoria "diária", com rateio integral ao plantio da diária. A despesa gerada não é editável diretamente: altera-se a diária, e a despesa acompanha.
**Origem:** RF27, RF28, M01

### RN17 — Vigências não se sobrepõem
Dois valores de referência não podem ter períodos de vigência sobrepostos.
**Origem:** RF25

### RN18 — Atualização de referência não retroage
Cadastrar ou alterar valor de referência não altera diárias já lançadas.
**Origem:** RF26, DA07

---

## 4. Apuração

### RN19 — Custo de desembolso
Custo de desembolso de um plantio no período é a soma dos valores rateados das despesas cuja data do fato caia no período.
**Origem:** RF34

### RN20 — Vedação de dupla contagem
Diárias **não** entram no custo de desembolso. A diária paga já participa pela despesa que gerou (RN16); a familiar não é desembolso.
**Origem:** RF39, M01

> É o erro mais perigoso do sistema: somar diárias nas duas pontas dobra o custo sem produzir exceção, sem quebrar nada e sem sinal visível.

### RN21 — Custo com mão de obra familiar
Custo de desembolso acrescido do valor das diárias familiares do período. Apurado e exibido como informação complementar, **nunca** substituindo o custo de desembolso.
**Origem:** RF37, DA03

### RN22 — Receita do plantio
Soma dos valores dos itens de venda que referenciam o plantio, com data do fato no período.
**Origem:** RF35

### RN23 — Resultado
Resultado = receita − custo de desembolso.
**Origem:** RF36

### RN24 — Período de apuração
O período deriva do tipo de ciclo efetivo do plantio: ciclo longo apura por safra, ciclo curto por mês.
**Origem:** RF38, DA05

### RN25 — Produção e venda são grandezas distintas — **DECIDIDA**
O relatório apresenta **quantidade produzida** e **quantidade vendida** como números separados. O resultado financeiro é sempre apurado e exibido, inclusive quando negativo.
**Origem:** RF36, DA04

> Metade dos associados produz para subsistência: terão custo e nenhuma venda, e o resultado sairá negativo. Isso não é distorção — é o que aconteceu. Separar produção de venda torna a leitura evidente: 500 kg produzidos, 0 kg vendidos, resultado negativo porque nada foi comercializado.
>
> A alternativa considerada era omitir o resultado nesses casos. Descartada: omitir valor corretamente apurado é decisão editorial do sistema, e o número negativo se explica sozinho quando as duas quantidades aparecem lado a lado.

---

## 5. Venda

### RN26 — Venda com ao menos um item
Uma venda deve conter no mínimo um item.
**Origem:** RF30

### RN27 — Item aponta para plantio
Todo item de venda deve indicar o plantio de origem.
**Origem:** RF31, M02

### RN28 — Sugestão de plantio
Havendo um único plantio aberto da cultura informada, o sistema o sugere como origem. Havendo mais de um, o operador escolhe; o sistema não decide.
**Origem:** RF32, R5

### RN29 — Saldo não bloqueia venda
O saldo disponível é informativo. O sistema **não** impede venda de quantidade superior ao saldo — apenas sinaliza a divergência.
**Origem:** RF33, M03, R5

> A produção é registrada de memória. Bloquear por saldo significaria impedir o registro de uma venda que de fato aconteceu.

### RN30 — Coerência de unidade
A unidade de medida do item de venda deve ser a mesma da produção registrada para aquele plantio.
**Origem:** RF31

---

## 6. Relatório e datas

### RN31 — Conteúdo mínimo do relatório
Todo relatório emitido contém: identificação do produtor, período coberto, data de emissão, culturas produzidas, despesas, diárias, resultado e as ressalvas das RN32 e RN33.
**Origem:** RF40 a RF44

### RN32 — Ressalva de valor declarado
Todo relatório indica que os valores foram declarados pelo produtor e não são comprovados por documento fiscal.
**Origem:** RF42, R3, R5

### RN33 — Ressalva de finalidade
Todo relatório indica que não constitui garantia de aceitação para qualquer finalidade externa.
**Origem:** RF43, R3

### RN34 — Imutabilidade da emissão
O conteúdo apurado no momento da emissão é gravado. Reemitir o mesmo período gera **novo** registro de emissão; o anterior permanece consultável.
**Origem:** RF45, RF46, DA05

### RN35 — Data do fato não futura
Nenhum lançamento aceita data do fato posterior à data corrente.
**Origem:** RF17, R5

### RN36 — Data de coleta atribuída pelo sistema
A data de coleta é a data em que o lançamento foi gravado. Não é informada nem editável pelo operador.
**Origem:** RF17, R5

### RN37 — Consentimento como condição para emissão — **DECIDIDA**
O lançamento de produção, despesa, diária e venda é permitido para produtor sem consentimento registrado, com sinalização da pendência. A **emissão de relatório** é bloqueada até que o consentimento seja registrado.
**Origem:** RF50, RNF09

> O atendimento não trava se o produtor comparecer sem poder assinar na hora, e nenhum documento sai sem autorização. A alternativa de bloquear o próprio lançamento foi descartada por inviabilizar o atendimento presencial; a de não bloquear nada, por esvaziar o consentimento.

---

## 7. Efeitos sobre o relatório

A RN25 acrescenta ao conteúdo do relatório a distinção entre quantidade produzida e quantidade vendida por plantio. O requisito RF40 descreve o conteúdo em termos gerais e comporta o acréscimo, mas a distinção deve constar do leiaute do documento.

---

## Rastreabilidade

| Origem | Regras |
|---|---|
| `DA04` — metade comercializa | RN25 |
| `DA01` — rateio proporcional à área | RN01, RN05, RN07, RN08, RN09, RN10 |
| `DA03` — custo de desembolso | RN20, RN21 |
| `DA05` — apuração por ciclo, emissão datada | RN11, RN24, RN34 |
| `DA07` — valoração da diária | RN13, RN14, RN18 |
| `M01` — diária paga gera despesa | RN16, RN20 |
| `M02` — venda com itens | RN27 |
| `M03` — saldo calculado | RN29 |
| `R3` — valor probatório | RN32, RN33 |
| `R5` — dado vindo de memória | RN28, RN29, RN35, RN36 |

---

## Pendências desta fase

| # | Pendência | Bloqueia |
|---|---|---|
| P15 | Refletir no leiaute do relatório a distinção entre quantidade produzida e vendida (RN25) | Não |

As regras RN25 e RN37 foram decididas nesta versão. Nenhuma regra segue pendente.
