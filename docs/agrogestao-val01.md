# AgroGestão — VAL01: Ratificação da dor principal e definição de custo

Registro de rodada de validação. Templates e regras de redação em `agrogestao-padroes-registro.md`. Status consolidado dos itens em `agrogestao-validacoes-indice.md`.

---

## VAL01 — Ratificação da dor principal e definição de custo

| Campo | Conteúdo |
|---|---|
| Data de envio | 09/09/2026 |
| Data de resposta | 10/09/2026 |
| Interlocutor | Cristina — presidente do sindicato |
| Canal | WhatsApp |
| Itens submetidos | D1, D2, D3, D4, DA02, DA03, P02 |
| Status | Encerrada |
| Evidência | *a anexar* |

### Nota de sequenciamento

`DA01` (critério de rateio) fica fora desta rodada de forma deliberada: a resposta a `DA03` altera as alternativas de `DA01`. Perguntar as duas juntas produziria resposta inconsistente.

### Mensagens a enviar

**Abertura**

> Oi, Cristina, tudo bem? Aqui é o pessoal do projeto do sistema de gestão, da faculdade.
>
> Antes de a gente começar a montar o sistema, queríamos confirmar algumas coisas com você, pra garantir que a ferramenta vai resolver o que realmente importa pros produtores. São quatro perguntas curtas. Pode responder no seu tempo.

---

**Mensagem 1** — *item D1*

> Nas nossas conversas, apareceram quatro dificuldades:
>
> 1. O produtor não sabe quanto custa produzir cada cultura
> 2. O sindicato não tem os dados dos associados reunidos em um lugar só
> 3. O produtor não tem um documento que comprove a produção e a renda dele
> 4. Os cadernos de anotação se perdem e o histórico some
>
> A gente entendeu que a número 1 é a mais importante, e planejou o sistema em cima dela. As outras três também são atendidas, mas em segundo plano.
>
> Na sua visão, está certo? Ou tem outra dessas quatro que é mais urgente pros produtores hoje?

---

**Mensagem 2** — *item DA03*

> Uma dúvida sobre como calcular o custo.
>
> Imagine um produtor que plantou um canteiro de coentro. Ele gastou R$ 80 em semente e adubo. Além disso, ele e a esposa trabalharam 10 dias na roça, sem pagar diária pra ninguém — o trabalho foi da família mesmo.
>
> Quando ele pensa em "quanto me custou esse coentro", ele considera:
>
> **(a)** só os R$ 80 que saíram do bolso
> **(b)** os R$ 80 mais o valor do trabalho da família, como se fosse uma diária
>
> Qual dessas duas é a forma como o produtor pensa?

---

**Mensagem 3** — *item DA02*

> O sistema vai gerar um relatório impresso pra entregar ao produtor, com a produção, os gastos e o resultado de cada cultura.
>
> Algum produtor já precisou levar um papel desse tipo pra banco, pro Pronaf, pra prefeitura ou pra algum programa do governo?
>
> Se sim, esse lugar exige um modelo específico de documento, ou aceita qualquer comprovante?

---

**Mensagem 4** — *item P02*

> Última: quem vai ficar responsável por lançar as informações no sistema aí no sindicato?
>
> E tem uma segunda pessoa que poderia aprender também, pra não ficar tudo na mão de uma pessoa só quando ela estiver de férias ou faltar?

---

**Encerramento**

> É isso. Obrigado, Cristina. Assim que a gente tiver as primeiras telas, mandamos pra você dar uma olhada antes de continuar.

### Respostas recebidas

Resposta à Mensagem 1, transcrição literal:

> 1- agricultor familiar ainda produz para própria substância, necessitando de orientação Técnica para alavancar a produção, assim também precificar os alimentos da agricultura familiar, assim também como identificar o quanto tem investido em cada produção. Grande necessidade de ferramentas agrícolas, maquinarios, insumos que possam facilitar a mão de obra que ainda se encontram de forma arcaica sendo uma das dificuldades de manter os jovens na produção agrícola.
>
> 2- O sindicato tem com clareza os números de agricultores e agricultoras no município por comunidade.
>
> 3- Não tem
>
> Então Jovens filho de agricultores tem muita dificuldade em se manter no campo, muitas saindo para capital em buscar de emprego, os até mesmos no comércio local ou trabalho doméstico. Etc

As Mensagens 2, 3 e 4 foram respondidas em retorno posterior, transcrição literal:

**Mensagem 2 — DA03**

> Ela pensa na A

**Mensagem 3 — DA02**

> Sim, para o inss para fins de comprovação de atividade rural, no ato da solicitação de benefício.
>
> Tendo aval do sindicato certamente será aceito como prova do exercício rural

**Mensagem 4 — P02**

> O sindicato possui uma diretoria com um quadro de seis diretores aptos e capaz de alimentar o sistema.

Em complemento, foram indicados nominalmente:

> Sinta da Silva Costa
> Cesariano Rodrigues Fernandes

### Interpretação item a item

**D1 — confirmada como necessidade, prioridade não ratificada.** A resposta menciona explicitamente precificar os alimentos e identificar quanto foi investido em cada produção. Isso corresponde a D1. Porém a pergunta feita foi qual das quatro é a mais urgente, e essa pergunta não foi respondida. Pelo critério do protocolo, D1 está confirmada, não ratificada como prioridade.

**D2 — refutada.** O sindicato declara ter clareza sobre os números de agricultores por comunidade. A dor era inferência da equipe e não se sustenta. Passa a `Descartada`; o número não será reutilizado.

**D3 — confirmada.** "Não tem" foi interpretado pela equipe como confirmação de que o produtor não possui documento comprobatório de produção e renda.

**D4 — sem resposta.** Silêncio não ratifica. Reformular em VAL02.

**D5 — nova.** A dificuldade de manter jovens no campo foi levantada espontaneamente e reforçada no fechamento da mensagem, o que sugere ser pauta central para a interlocutora. Não estava no levantamento anterior.

**DA03 — decidida, alternativa (a).** O produtor considera custo apenas o desembolso em dinheiro. Consequências: o registro de dias trabalhados **sai** do MVP; `DA01` reduz-se ao rateio de despesas em dinheiro; o valor do trabalho familiar pode figurar como informação complementar no relatório, sem entrar no cálculo principal.

**DA02 — decidida, com ressalva.** O uso externo existe e é específico: comprovação de exercício de atividade rural junto ao INSS, no ato da solicitação de benefício. Não há modelo formal citado; a interlocutora considera que o aval do sindicato torna o documento aceito.

Essa última afirmação **não pode ser assumida como verdadeira pela equipe**. Ver a seção de verificação legal abaixo.

> **Nota de correção — registrada em 12/09/2026.**
> A redação original desta seção e dos documentos derivados deu a entender que a interlocutora declarou que o relatório *será* usado para comprovação previdenciária. Ela não declarou adoção. O que respondeu foi:
> (i) a necessidade existe e a finalidade é o INSS, no ato da solicitação de benefício;
> (ii) não há modelo formal exigido;
> (iii) o aval do sindicato é apontado como **condição** de validade do documento.
> Empregar o relatório do sistema para esse fim é decisão da equipe, ainda não submetida. A correção não altera o conteúdo da resposta nem o risco R3 — apenas a atribuição do que foi dito e do que foi inferido. Por (iii), `DA06` deixa de tratar apenas de identificação do emissor e passa a tratar do aval institucional como requisito do documento.

**P02 — fechada.** Indicados nominalmente Sinta da Silva Costa e Cesariano Rodrigues Fernandes, dentro de uma diretoria de seis membros habilitados a operar o sistema. A ordem de indicação foi interpretada como titular e suplente, respectivamente — confirmar em VAL02.

### Verificação legal sobre o uso previdenciário (DA02)

Levantamento realizado pela equipe, não submetido à interlocutora. **A equipe não possui formação jurídica; este item deve ser levado ao professor orientador antes de virar requisito.**

Desde a Lei 13.843/2019, a comprovação da condição e do exercício da atividade do segurado especial passou a ser feita por autodeclaração, ratificada por consulta às bases de dados governamentais a que o INSS tem acesso; havendo ausência ou insuficiência de informação nessas bases, a comprovação pode ser complementada por prova documental contemporânea ao período informado.

A jurisprudência trata a declaração do sindicato de trabalhadores rurais como **início de prova material** — documento hábil a sinalizar a condição de rurícola, complementado por prova testemunhal para ampliar sua força probante. Não como prova plena.

**Consequências para o projeto:**

1. **O relatório compõe conjunto probatório; não garante benefício.** Posicionar a ferramenta como garantia de aceitação é incorreto e potencialmente danoso: um produtor que confie nisso e tenha benefício negado sofre prejuízo real. A comunicação ao sindicato e aos produtores precisa refletir isso.

2. **Contemporaneidade vira requisito de arquitetura.** Documentos valem para o período a que se referem. Um relatório emitido anos depois, cobrindo período passado, tem valor probatório reduzido. O que tem valor é o documento emitido durante o período, guardado ao longo do tempo. Isso impõe retenção de dados de longo prazo e emissão periódica datada — origem de `DA05`.

3. **O documento precisa se identificar.** Data de emissão, período coberto, identificação de quem emitiu pelo sindicato e integridade do conteúdo passam a ser requisitos do relatório — origem de `DA06`.

4. **Alcance de D3 supera o de D1.** Rentabilidade por cultura serve a quem comercializa. Comprovação de atividade rural serve a **todos** os associados, inclusive os de subsistência pura. Sob a premissa levantada em R2, D3 atinge uma base maior de beneficiários que D1. Isso pesa diretamente na escolha submetida em VAL02.

### Decisões resultantes

| Item | Decisão | Consequência |
|---|---|---|
| D1 | Confirmada como necessidade real | Mantida como núcleo do MVP; ratificação de prioridade pendente em VAL02 |
| D2 | Descartada | Dashboard institucional sai definitivamente do escopo, inclusive como evolução futura justificada por essa dor |
| D3 | Confirmada | Candidata a elevação de prioridade; vínculo com acesso a crédito a testar em VAL02 |
| D4 | Não resolvida | Reformular |
| D5 | Registrada, fora do escopo de software | Exige alinhamento explícito de expectativa com o sindicato |
| DA02 | Uso externo confirmado: comprovação de atividade rural junto ao INSS | Relatório muda de finalidade; ressalva legal registrada; origem de DA05 e DA06 |
| DA03 | Alternativa (a) — custo é desembolso em dinheiro | Registro de mão de obra sai do MVP; DA01 reduz escopo |
| P02 | Sinta da Silva Costa e Cesariano Rodrigues Fernandes | Pendência P02 fechada; risco R4 registrado |

### Itens não resolvidos

- Ratificação da prioridade entre D1 e D3
- D4 — sem resposta
- Confirmação de qual dos dois indicados é titular e qual é suplente

---

### Riscos identificados a partir desta resposta

Os riscos **R1** (desalinhamento de expectativa) e **R2** (premissa de comercialização) nasceram desta resposta. **R3** (expectativa indevida sobre valor probatório) e **R4** (operação difusa entre diretores) nasceram do retorno das mensagens 2 a 4.

Descrição completa e mitigações em `agrogestao-fase-0.md`, seção 0.4 — registro único, para evitar divergência entre cópias.

### Hipótese a testar

A resposta cita necessidade de maquinário e insumos. O acesso a esses bens normalmente passa por crédito rural — Pronaf e programas equivalentes — e crédito exige comprovação de produção e renda, que é exatamente D3.

Se a cadeia se confirmar, D3 deixa de ser dor secundária e passa a ser o elo entre o sistema e a agenda concreta do sindicato: o relatório vira instrumento de acesso a crédito, e não apenas de gestão. Isso alteraria a prioridade do módulo de relatórios e daria a `DA02` peso de requisito, não de verificação.

**Esta é hipótese da equipe, não afirmação da interlocutora.** Submetida em VAL02.

---

### Desdobramento previsto da mensagem 2

A alternativa **(b)** é economicamente mais correta: sem contabilizar a mão de obra familiar, uma cultura pode parecer lucrativa quando na verdade remunera abaixo de uma diária, e essa é exatamente a distorção que o sistema deveria expor.

A pergunta enviada, no entanto, **não** pede a resposta correta — pede como o produtor pensa hoje, conforme a regra 6 dos padrões de redação. Os dois desdobramentos:

- **Resposta (a).** O sistema registra as duas visões: custo de desembolso como principal, valor do trabalho familiar como informação complementar no relatório. Apresenta a distorção sem impor ao produtor um conceito que ele não usa.
- **Resposta (b).** O registro de dias trabalhados passa a ser requisito do MVP, com impacto direto no esforço de coleta pelo técnico operador.

A resposta não define apenas o cálculo: define se o sistema precisa de um módulo de registro de mão de obra.
