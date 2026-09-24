# AgroGestão — VAL03: Valoração da diária, rateio de despesa e coleta do formulário

Registro de rodada de validação. Templates e regras de redação em `agrogestao-padroes-registro.md`. Status consolidado dos itens em `agrogestao-validacoes-indice.md`.

---

## VAL03 — Valoração da diária, rateio de despesa e coleta do formulário

| Campo | Conteúdo |
|---|---|
| Data de envio | *a preencher* |
| Data de resposta | *a preencher* |
| Interlocutor | Cristina — presidente do sindicato |
| Canal | WhatsApp |
| Itens submetidos | DA07, DA01, coleta do formulário, grafia do nome |
| Status | Encerrada |
| Evidência | *a anexar* |

### Nota de sequenciamento — exceção deliberada

`DA07` e `DA01` são interdependentes: a valoração da diária define se ela integra o montante a ratear. Pelo padrão de registro, decisões interdependentes não vão na mesma rodada.

A regra é quebrada aqui de forma consciente, por dois motivos: são apenas duas perguntas de decisão, e a ordem das mensagens resolve a dependência — `DA07` é submetida antes, e o cenário de `DA01` é redigido sem depender da resposta dela. Separar em duas rodadas custaria uma semana de espera sem ganho de qualidade.

Registrado como exceção para não ser lido como descuido em revisão posterior.

### Mensagens a enviar

**Abertura**

> Oi, Cristina. Duas perguntas sobre o formulário e uma coisinha pra confirmar.

---

**Mensagem 1** — *item DA07*

> No formulário que vocês já emitem tem o campo "Diárias".
>
> Quando o sindicato preenche esse campo, que valor vocês colocam ali? É o valor da diária que se paga na região, é um valor que o próprio agricultor informa, ou vocês têm alguma tabela de referência?

---

**Mensagem 2** — *item DA01*

> Uma situação comum: seu Antônio plantou feijão num canteiro e milho em outro, na mesma propriedade. Ele gastou R$ 200 de combustível no trator, e esse trator trabalhou nos dois canteiros.
>
> Na hora de dizer quanto custou o feijão e quanto custou o milho, esses R$ 200 devem ser:
>
> **(a)** divididos na metade, R$ 100 pra cada
>
> **(b)** divididos conforme o tamanho de cada canteiro — se o milho ocupa o dobro do feijão, o milho fica com o dobro do gasto
>
> **(c)** lançados inteiros num só, do jeito que o produtor achar
>
> Qual dessas três é mais parecida com o que faz sentido pro produtor?

---

**Mensagem 3** — *coleta do formulário*

> Aquele formulário de comprovação que vocês emitem: consegue me mandar uma foto de um já preenchido, ou mesmo em branco?
>
> Pergunto porque a gente vai montar o relatório do sistema no mesmo formato, e assim não corremos o risco de faltar campo ou colocar em ordem diferente da que vocês usam.

---

**Mensagem 4** — *grafia do nome*

> Última e rapidinha: o nome da titular é escrito Sinta ou Sintia? Quero deixar certo na documentação.

---

**Encerramento**

> Obrigado, Cristina. Com isso a gente fecha a parte de regras e começa a montar o sistema.

### Respostas recebidas

Transcrição literal.

**Mensagem 1 — DA07**

> Não, fazemos esse tipo de levantamento de produção.

**Mensagem 2 — DA01**

> B

**Mensagens 3 e 4** — sem resposta nesta rodada.

### Interpretação item a item

**DA01 — decidida, alternativa (b).** A despesa compartilhada é rateada proporcionalmente à área ocupada por cada plantio.

Consequência no modelo de dados: despesa **não** pertence a um único plantio. A relação é muitos-para-muitos, com percentual por plantio, e o rateio passa a ser regra de negócio explícita.

*Decisão de modelagem decorrente, tomada pela equipe:* o percentual é calculado no momento do lançamento e **gravado**, não recalculado na consulta. Se a área de um plantio for corrigida depois, os rateios já realizados não se alteram. Sem isso, um relatório entregue ao produtor em março deixaria de bater com o mesmo relatório reemitido em junho — inaceitável para um documento destinado a comprovação.

**DA07 — não ratificada.** A resposta não corresponde à pergunta enviada. Perguntamos que valor o sindicato coloca no campo "Diárias"; a resposta informa que o sindicato não realiza esse levantamento.

Pelo critério do protocolo, o item não está ratificado. Reformular, porém, não resolveria: não há prática a capturar, e a interlocutora não tem motivo para ter critério formado sobre valoração de trabalho não remunerado.

**Decisão da equipe, não submetida a validação:** a diária familiar é valorada por **valor de referência da diária da região**, cadastrado pelo operador com vigência por período. O valor vigente na data é **gravado no registro** no momento do lançamento, de modo que atualizações futuras da referência não alterem registros passados.

Justificativa: não exige que o produtor estime valor, mantém comparabilidade entre produtores e preserva a coerência histórica dos relatórios já emitidos.

**Formulário e grafia do nome — sem resposta.** Ver nota de reinterpretação abaixo quanto ao formulário.

### Nota de reinterpretação — o formulário do sindicato

Em alinhamento presencial posterior a esta rodada, ficou esclarecido que a lista de campos fornecida pela interlocutora em VAL02 ("Formato do documento: dados pessoais, endereço, profissão...") **não descreve um documento que o sindicato já emite**. É a especificação do que ela deseja que o relatório do sistema contenha.

A resposta de `DA07` nesta rodada é coerente com isso: o sindicato não levanta diárias hoje: o campo é desejado no relatório futuro, não herdado de um documento existente.

Consequências:

1. **A pendência de coleta do exemplar do formulário deixa de existir.** Não há exemplar a obter. O risco de o entregável divergir de um documento real desaparece.
2. **A justificativa de `DA03` muda.** O registro de diárias entrou no MVP por aderência ao formulário existente. Esse argumento cai. A decisão permanece, sustentada agora em dois outros: a interlocutora pediu o campo explicitamente, e afirmou em VAL02 que precificar exige somar a mão de obra do grupo familiar.
3. **A justificativa de `DA05` muda, a decisão não.** A apuração por ciclo produtivo não decorre do formulário: decorre da cultura. Milho apura por safra, hortaliça por mês, independentemente do formato do documento.
4. **`DA05` não precisa ser reaberta quanto ao emissor.** Quem assina documento institucional do sindicato é a presidente, o que decorre do cargo. `DA06` já fechou esse ponto em VAL02.

Nota registrada em `agrogestao-val02.md` como correção datada, sem reescrita da interpretação original daquela rodada.

### Decisões resultantes

| Item | Decisão | Consequência |
|---|---|---|
| DA01 | Alternativa (b) — rateio proporcional à área | Despesa passa a alcançar vários plantios, com percentual por plantio gravado no lançamento. Exige tabela associativa no modelo |
| DA07 | Não ratificada — decidida pela equipe | Valor de referência da diária da região, com vigência por período, gravado no registro. Entidade própria no modelo de dados |
| Formulário | Reinterpretado | Não existe documento a replicar; a lista de VAL02 é especificação do relatório a construir |
| Grafia do nome | Sem resposta | Permanece pendente |

### Itens não resolvidos

- Grafia do nome da operadora titular — "Sinta" em VAL01, "Sintia" em VAL02
- Validação, junto à interlocutora, do critério de valoração da diária adotado pela equipe. Não bloqueia a modelagem; cabe como conferência na apresentação do protótipo

---

### Desdobramento previsto

**DA07 — valoração da diária familiar.** As três respostas possíveis levam a modelagens distintas:

- **Diária da região** — valor de referência único, com vigência temporal. Entidade própria no banco, atualizável sem alterar registros passados.
- **Valor informado pelo agricultor** — atributo do próprio registro de diária. Mais simples, mas produz valores heterogêneos entre produtores e compromete a comparação.
- **Tabela do sindicato** — entidade de referência mantida pelo operador, com histórico de versões.

**DA01 — rateio.** A resposta define a cardinalidade entre despesa e plantio no modelo de dados:

- **Resposta (c)** — despesa pertence a um único plantio. Relação simples, um-para-muitos.
- **Respostas (a) ou (b)** — uma despesa pode alcançar vários plantios, com percentual por plantio. Exige tabela associativa e torna o rateio regra de negócio explícita.

Esta é a decisão de maior impacto sobre o DER, e é por isso que o modelo físico não deve ser desenhado antes do retorno desta rodada.
