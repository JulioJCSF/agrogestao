# AgroGestão — Fase 1: Glossário de Domínio

Versão 1.5

Define os termos de negócio do projeto sem ambiguidade. É o documento de referência para requisitos, regras de negócio, modelagem de dados, nomenclatura de código e casos de teste.

**Regra de uso:** termo definido aqui tem um único significado em todo o projeto. Se um documento precisar de outro sentido, o termo é novo e entra neste glossário — não se reaproveita um existente.

**Documentos relacionados**

- `agrogestao-fase-0.md` — dores, decisões e riscos que originam estes termos
- `agrogestao-validacoes-indice.md` — status das decisões em aberto citadas
- `agrogestao-padroes-registro.md` — identificadores e templates

---

## 1. Pessoas e papéis

### Produtor

Agricultor familiar associado ao sindicato, cuja produção é registrada no sistema. **Não é usuário do sistema** nesta versão: não possui credenciais nem acessa a aplicação. É entidade cadastrada e beneficiário final da informação.

### Associado

Sinônimo de produtor no contexto do sindicato. Usar "produtor" nos documentos técnicos; "associado" apenas quando o texto tratar da relação com o sindicato.

### Operador

Pessoa do sindicato que opera o sistema e registra os dados em nome dos produtores. Possui credenciais. Papel exercido pela operadora titular e, na ausência dela, pelo suplente.

### Sindicato

Sindicato dos Trabalhadores Rurais de Redenção-CE. Titular da conta institucional no sistema e emissor do aval nos relatórios.

---

## 2. Estrutura produtiva

### Propriedade

Área rural onde o produtor cultiva. Um produtor pode ter mais de uma propriedade; uma propriedade pertence a um único produtor.

### Cultura

Espécie cultivada, como entidade de catálogo: milho, feijão, fava, banana, castanha, caju, mandioca, urucum, hortaliça. Define o ciclo padrão da espécie.

**Não confundir com Plantio.** "Custo por cultura" é expressão informal e significa custo por plantio daquela cultura no período apurado.

### Plantio

Instância concreta de cultivo: uma cultura, em uma propriedade, em uma área, com data de início e previsão de colheita. É a **unidade à qual despesas, diárias e produção se vinculam**, e sobre a qual o resultado é apurado.

> **Decisão tomada.** Os documentos anteriores tratavam "cultura" como se fosse a unidade de custo. A separação é necessária por três motivos: o mesmo produtor planta milho em safras diferentes e os custos não podem se misturar; culturas de ciclo curto apuram por mês, o que exige uma instância por período; e o rateio de despesa é proporcional à **área**, que é atributo do cultivo concreto e não da espécie.
>
>
> **Nome confirmado:** "Plantio". Alternativas consideradas e descartadas — "canteiro" (usado pela interlocutora, mas sugere horta pequena), "roçado" (regional, confunde com propriedade), "talhão" (técnico demais para o operador) e "safra" (já tem significado próprio neste glossário).

### Ciclo produtivo

Intervalo entre o início do plantio e a colheita.

- **Ciclo curto** — colheita recorrente em intervalos de semanas ou meses. Exemplo: hortaliças.
- **Ciclo longo** — uma colheita por ano, associada a uma safra. Exemplo: milho.

O ciclo determina o período de apuração do resultado (ver seção 4).

### Safra

Período anual de produção de uma cultura de ciclo longo. **Entressafra** é o período fora dele.

Safra e entressafra afetam o preço de mercado: na safra o preço cai pela oferta, na entressafra sobe. O sistema não controla safra como entidade própria nesta versão; o termo aparece na interpretação do resultado.

### Produção

Quantidade colhida em um plantio, com unidade de medida e data. Um plantio pode ter mais de um registro de produção (culturas de ciclo curto, com colheitas sucessivas).

---

## 3. Registros financeiros

### Despesa

Saída de dinheiro: semente, adubo, defensivo, combustível, diária paga a terceiro. Tem valor, data e categoria.

**É sempre desembolso efetivo.** Valor atribuído sem saída de dinheiro não é despesa — ver Diária familiar.

Uma despesa pode servir a **mais de um plantio** — o combustível do trator que trabalhou em dois canteiros. Nesse caso é rateada (ver Rateio).

### Rateio

Divisão de uma despesa entre os plantios que ela atendeu, **proporcional à área de cada um** (`DA01`).

O percentual é calculado e **gravado no momento do lançamento**, não recalculado na consulta. Corrigir a área de um plantio depois não altera rateios já realizados — sem isso, um relatório reemitido meses adiante deixaria de bater com o que foi entregue ao produtor.

### Diária

Registro de um dia de trabalho em um plantio. Duas naturezas, com tratamento distinto:

- **Diária paga** — trabalho de terceiro, remunerado. É desembolso e portanto **também é despesa**.
- **Diária familiar** — trabalho do produtor ou do grupo familiar, sem pagamento. **Não é desembolso.** Tem valor atribuído, não pago.

A interlocutora do sindicato pediu campo próprio para diárias no relatório. O sindicato não realiza esse levantamento hoje — o registro nasce com o sistema.

### Valor de referência da diária

Valor monetário atribuído a um dia de trabalho familiar, correspondente à diária praticada na região. Cadastrado pelo operador, **com vigência por período**.

O valor vigente na data é **gravado no registro de diária** no momento do lançamento. Atualizar a referência não altera registros passados (`DA07`).

> Decisão da equipe, não ratificada pela interlocutora. Conferir na apresentação do protótipo.

### Receita

Entrada de dinheiro. No MVP decorre de venda; o termo é mantido separado porque pode haver entrada não originada de venda registrada.

### Venda

Transação de saída de produto: produto, quantidade, valor, data e cliente. Gera receita. Não movimenta estoque — não há estoque registrado (`M03`); a quantidade vendida apenas reduz o saldo calculado do plantio.

### Estoque

Saldo calculado, não registrado: a produção registrada de um plantio menos as quantidades vendidas que apontam para ele (`ITEM_VENDA`). Não existe entidade de estoque no modelo (`M03`).

O saldo é informativo: não bloqueia venda, apenas sinaliza divergência (`RN29`).

---

## 4. Apuração

### Custo de desembolso

Soma das despesas de um plantio no período apurado. **É o custo principal do sistema** — corresponde à forma como o produtor pensa o custo, conforme validado em VAL01 (`DA03`, alternativa (a)).

### Custo com mão de obra familiar

Custo de desembolso acrescido do valor atribuído às diárias familiares. Apurado e exibido como **informação complementar**, nunca substituindo o custo de desembolso.

A distinção existe porque as duas visões têm usos diferentes: o custo de desembolso é o que o produtor reconhece; o custo com mão de obra revela quando uma cultura remunera abaixo de uma diária. Apresentar só o segundo impõe um conceito que o produtor não usa; apresentar só o primeiro esconde a distorção.

### Período de apuração

Intervalo sobre o qual o resultado é calculado. **Derivado do ciclo da cultura, não do calendário** (`DA05`):

- Plantio de ciclo longo — apura por safra
- Plantio de ciclo curto — apura por mês

### Resultado

Receita do plantio menos custo de desembolso, no período de apuração. É o termo técnico do sistema.

**No relatório impresso usa-se "Lucro"**, por ser o termo empregado pela interlocutora do sindicato ao especificar o documento. São o mesmo número com nomes diferentes: "resultado" na modelagem e no código, "lucro" na interface com o produtor.

### Termos a evitar

| Termo | Por quê | Usar |
|---|---|---|
| Gasto | Ambíguo entre despesa e custo | Despesa ou custo |
| Rentabilidade | Sugere percentual sobre investimento, que o sistema não calcula | Resultado |
| Lucro | Reservado ao relatório impresso | Resultado |
| Faturamento | Não pertence ao vocabulário do produtor | Receita |

---

## 5. Documentos e dados

### Relatório

Documento gerado pelo sistema para entrega ao produtor, contendo dados pessoais, culturas produzidas, despesas, diárias e resultado do período. **Único canal de entrega de informação ao beneficiário final.**

Tem dupla finalidade: análise de custo e resultado, e — com timbre e assinaturas — comprovação de atividade rural. O sistema produz o conteúdo; a validade institucional é conferida no papel.

### Aval do sindicato

Conjunto de elementos físicos que conferem validade institucional ao relatório impresso (`DA06`): timbre do sindicato no formulário, assinatura do presidente e assinatura do agricultor.

Não há assinatura digital, certificação ou mecanismo eletrônico de integridade nesta versão.

### Data do fato

Data em que o evento ocorreu — o dia do plantio, da despesa, da colheita, da venda.

### Data de coleta

Data em que o operador registrou o evento no sistema.

As duas são registradas separadamente. Como não há anotação prévia e o dado vem da memória do produtor no atendimento (`R5`), a distância entre as duas datas é um indicador da confiabilidade do registro.

### Valor declarado

Qualificação obrigatória dos dados do relatório: os valores são informados pelo produtor, não comprovados por documento fiscal. Deve constar no documento gerado, como mitigação de `R3` e `R5`.

---

## Termos com definição pendente

Nenhum termo em aberto.

`DA01` e `DA07` foram decididas em VAL03 e estão definidas nas seções 3 e 4. O critério de valoração da diária é decisão da equipe, ainda não ratificada pela interlocutora.
