

**PROJETO EXTENSIONISTA**

**AGROGESTÃO**  
**Sistema de Gestão e Apoio à Agricultura Familiar**

Proposta de projeto para curso de Tecnologia da Informação  
2026

Versão 3.1 — revisada após as rodadas de validação VAL01, VAL02 e VAL03 com o Sindicato dos Trabalhadores Rurais de Redenção-CE. Ajustes nas seções 2, 7, 8.5, 8.8, 9, 13 e 14, refletindo as decisões registradas em *AgroGestão — Fase 0* (v1.8) e nos registros de validação.

# **Sumário**

1\. Visão geral do projeto

2\. Contextualização e problema

3\. Justificativa

4\. Público-alvo e comunidade beneficiária

5\. Objetivos

6\. Relação com as ODS

7\. Solução proposta

8\. Funcionalidades do sistema

9\. Escopo do MVP

10\. Arquitetura e tecnologias

11\. Metodologia extensionista

12\. Cronograma sugerido

13\. Indicadores de impacto

14\. Riscos e estratégias de mitigação

15\. Possíveis evoluções

16\. Resultados esperados

17\. Considerações finais

# **1\. Visão geral do projeto**

O AgroGestão é uma proposta de projeto extensionista voltada à digitalização da gestão de pequenos produtores rurais, com foco prioritário na agricultura familiar. A iniciativa combina desenvolvimento de software, capacitação tecnológica e interação direta com uma comunidade beneficiária.

A proposta parte de um princípio central: a tecnologia deve ser utilizada para solucionar uma necessidade identificada na comunidade. Dessa forma, o sistema não será desenvolvido apenas como um produto acadêmico, mas como uma ferramenta de apoio à organização da produção, controle de custos, acompanhamento de vendas e análise de resultados.

# **2\. Contextualização e problema**

O diagnóstico realizado com o sindicato parceiro revelou uma situação mais severa do que a hipótese inicial do projeto. Não há registro a organizar: os produtores não mantêm cadernos, planilhas ou anotações sistemáticas da produção e dos gastos. A informação existe apenas na memória de quem produziu.

Sem registro não há apuração de custo. O produtor sabe quanto recebeu pela venda, mas não sabe quanto gastou para produzir determinada cultura nem qual apresenta melhor resultado econômico. O preço de venda, por sua vez, é formado pelo mercado e varia entre safra e entressafra — não é arbitrado pelo produtor. Conhecer o custo não altera o preço praticado: altera a decisão de o que plantar e quando vender.

Há ainda uma segunda carência, levantada pelo próprio sindicato: o produtor não dispõe de documento organizado que comprove sua produção e sua renda, necessário quando precisa demonstrar exercício de atividade rural.

O sistema, portanto, não digitaliza um registro existente. Ele cria o primeiro registro formal da atividade do produtor.

# **3\. Justificativa**

A agricultura familiar possui relevância econômica e social e depende, entre outros fatores, de uma gestão eficiente dos recursos disponíveis. A transformação digital pode contribuir para organizar informações que já são produzidas no dia a dia, mas que muitas vezes não são consolidadas de forma útil para a tomada de decisão.

Para os estudantes de TI, o projeto oferece a oportunidade de aplicar conhecimentos de engenharia de software, banco de dados, desenvolvimento web, segurança, APIs e visualização de informações em uma situação real. Para a comunidade, a expectativa é disponibilizar uma ferramenta gratuita e acompanhada de capacitação.

O caráter extensionista estará principalmente na interação com os produtores: levantamento das necessidades, validação da solução, capacitação dos usuários e avaliação dos resultados.

# **4\. Público-alvo e comunidade beneficiária**

O projeto possui dois públicos distintos, e a diferença entre eles orienta as decisões de interface.

**Usuário operador.** O sistema é operado por técnico ou funcionário do Sindicato dos Trabalhadores Rurais de Redenção-CE, que registra os dados em nome dos produtores associados, a partir das informações que eles trazem. Trabalha em notebook, na sede do sindicato, com conexão WiFi disponível.

**Beneficiários finais.** Os pequenos produtores rurais e agricultores familiares não acessam o sistema diretamente nesta versão. Fornecem as informações ao técnico e recebem de volta os relatórios gerados. É para eles que a informação precisa fazer sentido, ainda que não sejam eles quem operam a ferramenta.

Perfis de beneficiários atendidos:  
 ● Agricultores familiares;  
 ● Pequenos horticultores;  
 ● Produtores de frutas, hortaliças, raízes e grãos;  
 ● Associados e trabalhadores rurais vinculados ao sindicato parceiro.

O projeto será desenvolvido em parceria com o Sindicato dos Trabalhadores Rurais de Redenção-CE, que atua como comunidade beneficiária e ponto de contato com os produtores participantes. A parceria já estabelecida permite iniciar o diagnóstico com contato direto e recorrente, favorecendo o acompanhamento próximo previsto na metodologia extensionista.

# **5\. Objetivos**

## **5.1 Objetivo geral**

Desenvolver e validar uma plataforma digital de apoio à gestão da agricultura familiar, contribuindo para a organização da produção, do estoque, das receitas, das despesas e das vendas de pequenos produtores.

## **5.2 Objetivos específicos**

* Identificar as principais dificuldades de gestão enfrentadas pelos produtores parceiros;  
* Mapear os processos atualmente utilizados para registrar produção, custos e vendas;  
* Projetar uma solução digital adequada ao perfil dos usuários;  
* Desenvolver um MVP funcional com as principais necessidades identificadas;  
* Capacitar os produtores para utilização do sistema;  
* Avaliar a usabilidade e a percepção de valor da ferramenta;  
* Medir indicadores de impacto antes e depois da intervenção.

# **6\. Relação com as ODS**

A proposta possui aderência principalmente à ODS 2, podendo também contribuir para as ODS 8, 9 e 12\.

## **6.1 ODS 2 — Fome Zero e Agricultura Sustentável**

É a ODS principal. O projeto busca apoiar pequenos produtores e fortalecer práticas de gestão relacionadas à agricultura familiar, contribuindo indiretamente para maior eficiência e sustentabilidade da produção.

## **6.2 ODS 8 — Trabalho Decente e Crescimento Econômico**

A melhoria da organização financeira e produtiva pode apoiar pequenos empreendimentos rurais e sua capacidade de gestão.

## **6.3 ODS 9 — Indústria, Inovação e Infraestrutura**

A iniciativa promove o uso de tecnologia e inovação digital em um contexto de produção agrícola de pequena escala.

## **6.4 ODS 12 — Consumo e Produção Responsáveis**

O controle de produção, estoque e perdas pode contribuir para uma visão mais organizada do uso de recursos e do desperdício.

# **7\. Solução proposta**

O AgroGestão será uma aplicação web projetada para uso em desktop, acessada por notebook na sede do sindicato. A responsividade para telas menores não é requisito desta versão e foi reposicionada como evolução futura, conforme a seção 15.

O técnico operador terá um painel central com informações resumidas por produtor e por propriedade, e poderá registrar as atividades necessárias para acompanhar a operação dos associados. O produtor recebe o resultado por meio de relatórios exportáveis, que constituem o canal de devolução da informação.

Fluxo simplificado:

1. Acesso do técnico por conta institucional do sindicato;  
2. Cadastro do produtor e da propriedade;  
3. Cadastro de culturas e ciclos produtivos;  
4. Registro de produção e estoque;  
5. Registro de despesas vinculadas à cultura, de diárias de mão de obra e de receitas;  
6. Registro de vendas;  
7. Apuração do resultado por cultura, segundo o ciclo produtivo;  
8. Geração de indicadores e do relatório;  
9. Impressão, aval do sindicato e entrega do relatório ao produtor.

# **8\. Funcionalidades do sistema**

## **8.1 Autenticação e usuários**

* Conta institucional do sindicato, com perfis de acesso diferenciados;  
* Login e controle de acesso por perfil;  
* Cadastro de produtores como entidade do sistema, e não como usuário com credenciais;  
* Recuperação de senha, caso o tempo de desenvolvimento permita.

## **8.2 Propriedade**

* Cadastro da propriedade;  
* Área aproximada;  
* Localização em nível adequado à necessidade do projeto;  
* Informações básicas do produtor.

## **8.3 Culturas e produção**

* Cadastro da cultura;  
* Data de plantio;  
* Previsão de colheita;  
* Área utilizada;  
* Quantidade produzida;  
* Histórico de produção.

## **8.4 Estoque**

* Produtos disponíveis;  
* Quantidade;  
* Unidade de medida;  
* Entrada e saída de produtos;  
* Atualização automática após vendas.

## **8.5 Financeiro**

* Registro de despesas vinculadas à cultura;  
* Registro de diárias de mão de obra, incluindo a do grupo familiar;  
* Registro de receitas;  
* Categorias de custos;  
* Filtros por período;  
* Apuração de resultado segundo o ciclo da cultura — culturas de ciclo longo apuram por safra, de ciclo curto por mês.

## **8.6 Vendas**

* Cadastro de clientes;  
* Produto vendido;  
* Quantidade;  
* Valor;  
* Data da venda;  
* Atualização do estoque.

## **8.7 Dashboard**

* Produção por período;  
* Receita;  
* Despesas;  
* Resultado;  
* Produtos mais vendidos;  
* Indicadores por cultura.

## **8.8 Relatórios**

Como o produtor não acessa o sistema diretamente, o relatório é o **único canal de entrega de informação ao beneficiário final**. Isso eleva este módulo à condição de funcionalidade crítica do MVP: sem ele, o sistema organiza dados para o sindicato, mas não devolve valor ao produtor.

O relatório tem **dupla finalidade**, confirmada na validação com o sindicato. Serve como análise de custo e resultado por cultura e, quando impresso em papel timbrado e assinado, como documento de comprovação de atividade rural — finalidade relatada para uso junto ao INSS, no ato da solicitação de benefício.

Campos especificados pela presidente do sindicato para o relatório:

* Dados pessoais, endereço e profissão do agricultor;  
* Culturas produzidas;  
* Despesas;  
* Diárias;  
* Resultado mensal ou anual, conforme o ciclo da cultura;  
* Timbre do sindicato no formulário;  
* Assinatura do agricultor e do presidente do sindicato.

Funcionalidades do módulo:

* Relatório de produção e financeiro por cultura e por período;  
* Resultado por cultura apurado segundo o ciclo produtivo;  
* Histórico de vendas;  
* Exportação em formato aderente ao formulário do sindicato, adequado à impressão;  
* Identificação da data de emissão e do período coberto;  
* Indicação de que os valores são declarados pelo produtor;  
* Linguagem e apresentação compreensíveis por quem não opera o sistema.

**Ressalva.** O valor probatório do documento está sob avaliação do professor orientador. A declaração emitida por sindicato de trabalhadores rurais é tratada como início de prova material, não como prova plena. O sistema não deve ser apresentado, em nenhuma comunicação, como garantia de obtenção de benefício previdenciário.

# **9\. Escopo do MVP**

Para uma disciplina extensionista, recomenda-se limitar a primeira versão a funcionalidades essenciais.

A validação com o sindicato mostrou que as duas necessidades levantadas — conhecer o custo de produção e dispor de comprovante de atividade rural — não são concorrentes. O relatório especificado pela presidente reúne produção, despesas, diárias e resultado: é o mesmo artefato servindo a dois usos. O MVP tem, portanto, **núcleo único**: registro de produção e despesas alimentando um relatório de dupla finalidade.

**Prioridade alta — núcleo do MVP**

* Conta institucional do sindicato com perfis de acesso;  
* Cadastro de produtores e propriedades;  
* Cadastro de culturas e ciclos produtivos;  
* Registro de produção;  
* Registro de despesas vinculadas à cultura;  
* Registro de diárias de mão de obra, inclusive familiar;  
* Registro de receitas e vendas;  
* Apuração de resultado por cultura, segundo o ciclo produtivo;  
* Relatório exportável, aderente ao formulário do sindicato, para entrega ao produtor.

**Prioridade média**

* Controle básico de estoque;  
* Dashboard com indicadores essenciais.

O controle de estoque foi rebaixado em relação à proposta original: é útil, mas não endereça a dor priorizada. Caso o cronograma aperte, é o primeiro candidato a corte.

Funcionalidades como marketplace, integração meteorológica, Internet das Coisas e inteligência artificial devem ser tratadas como futuras evoluções, e não como requisitos obrigatórios do MVP.

# **10\. Arquitetura e tecnologias**

Arquitetura sugerida:

**Front-end**

* Build tool: Vite  
* Framework: React  
* Biblioteca de UI/estilização: HeroUI  
* Roteamento: React Router  
* Requisições HTTP: Axios  
* Gerenciamento de estado: Context API \+ hooks

**Back-end**

* Linguagem/Framework: Java com Spring Boot  
* API: REST  
* Autenticação/autorização: Spring Security e JWT  
* Validação: Bean Validation (Jakarta Validation)  
* Documentação da API: Swagger/OpenAPI (springdoc-openapi)  
* ORM: Spring Data JPA (Hibernate)

**Banco de dados**

* PostgreSQL  
* Versionamento de schema: Flyway

**Infraestrutura e qualidade**

* Controle de versão: Git e GitHub  
* Containerização: Docker e Docker Compose, com front-end, back-end e banco de dados  
   totalmente conteinerizados desde o início do desenvolvimento, utilizando volumes montados  
   para permitir hot-reload durante o ambiente de desenvolvimento  
* Testes automatizados : JUnit (back-end) e Playwright (testes end-to-end no front-end)

Arquitetura lógica: o usuário interage com o front-end desenvolvido em React (Vite), estilizado  
 com HeroUI. O front-end consome uma API REST desenvolvida em Spring Boot, autenticada via  
 JWT. O back-end aplica as regras de negócio, realiza validações e persiste os dados no  
 PostgreSQL, com controle de versionamento de schema via Flyway. Todo o ambiente —  
 front-end, back-end e banco de dados — é orquestrado desde o início do projeto por meio de  
 Docker Compose, com volumes configurados para hot-reload, garantindo consistência entre os  
 times de desenvolvimento e agilidade durante a fase de desenvolvimento em paralelo. A  
 qualidade do sistema é verificada por meio de testes automatizados: testes unitários no  
 back-end com JUnit e testes end-to-end no front-end com Playwright, cobrindo os principais  
 fluxos de uso do sistema.

# **11\. Metodologia extensionista**

## **11.1 Diagnóstico**

A primeira etapa será a aproximação com a comunidade. A equipe deverá conversar com os produtores e observar como informações de produção, custos e vendas são atualmente registradas.

Perguntas possíveis:

* Como você registra sua produção?  
* Como controla os gastos da propriedade?  
* Como sabe quanto lucrou com cada cultura?  
* Como controla produtos disponíveis para venda?  
* Quais tarefas de gestão são mais difíceis?  
* Você utiliza celular, computador ou planilhas para essa finalidade?  
* Que informação gostaria de visualizar e hoje não consegue?

## **11.2 Planejamento e prototipação**

Com os dados do diagnóstico, a equipe deverá priorizar os problemas mais relevantes. Em seguida, poderá criar protótipos de baixa ou média fidelidade e validar as telas com alguns usuários antes da implementação.

## **11.3 Desenvolvimento**

O desenvolvimento pode seguir uma abordagem incremental, entregando pequenas partes do sistema e validando-as com os usuários. A equipe deve manter o foco no MVP.

## **11.4 Capacitação**

Após uma versão utilizável, a capacitação ocorre em dois níveis distintos, coerentes com a divisão entre operador e beneficiário.

**Capacitação operacional — técnico do sindicato.** Oficina prática voltada a quem efetivamente opera o sistema, cobrindo cadastro de produtores e propriedades, registro de produção, lançamento de despesas vinculadas à cultura, vendas, geração e exportação de relatórios. Recomenda-se capacitar também um segundo operador, para mitigar o risco de dependência de pessoa única.

**Oficina de apresentação — produtores.** Encontro voltado aos beneficiários finais, com foco na leitura e interpretação do relatório recebido: o que significa o custo apurado, como comparar culturas e como usar a informação para decidir o que plantar e por quanto vender. Não envolve operação do sistema.

## **11.5 Avaliação**

Ao final, os produtores podem responder a um questionário de satisfação e realizar tarefas práticas no sistema. Os resultados devem ser comparados com os dados coletados na fase de diagnóstico.

# **12\. Cronograma sugerido**

| Etapa | Atividade | Período | Entrega |
| :---- | :---- | :---- | :---- |
| 1 | Contato e diagnóstico | Semanas 1–2 | Levantamento de necessidades |
| 2 | Requisitos e protótipo | Semanas 3–4 | Protótipo validado |
| 3 | Banco/API | Semanas 5–7 | Back-end funcional |
| 4 | Front-end | Semanas 7–9 | Interface funcional |
| 5 | Integração e testes | Semanas 10–11 | MVP integrado |
| 6 | Capacitação | Semana 12 | Oficina com usuários |
| 7 | Avaliação | Semana 13 | Indicadores de impacto |
| 8 | Documentação/apresentação | Semanas 14–15 | Relatório final |

# **13\. Indicadores de impacto**

Como o produtor não opera o sistema, indicadores de uso direto pelo produtor não são aplicáveis. Os indicadores foram reformulados em três grupos: uso operacional pelo sindicato, alcance junto aos produtores e mudança de comportamento do produtor — sendo este último o que efetivamente mede o impacto do projeto.

## **13.1 Uso operacional (sindicato)**

* Número de propriedades cadastradas;  
* Quantidade de culturas registradas;  
* Quantidade de registros de produção realizados;  
* Quantidade de registros financeiros realizados;  
* Quantidade de vendas registradas;  
* Percentual de tarefas de registro que o técnico realiza sem auxílio da equipe;  
* Número de operadores capacitados.

## **13.2 Alcance (produtores)**

* Número de produtores participantes;  
* Número de produtores com dados efetivamente registrados no sistema;  
* Número de relatórios entregues a produtores.

## **13.3 Mudança de comportamento (impacto)**

Medidos por comparação entre o diagnóstico inicial e a avaliação final:

* Percentual de produtores que passam a conhecer o custo de produção de ao menos uma cultura;  
* Percentual de produtores que identificam qual cultura apresenta melhor resultado econômico;  
* Percentual de produtores que declaram usar a informação do relatório para decidir o que plantar;  
* Percentual de produtores que declaram usar a informação para decidir quando vender, considerando safra e entressafra;  
* Percentual de produtores que passam a dispor de documento de comprovação de produção e renda.

O indicador de revisão de preço, previsto na versão anterior, foi retirado: a validação mostrou que o preço é formado pelo mercado e não é arbitrado pelo produtor. Medir alteração de preço mediria um comportamento que não está ao alcance dele.

## **13.4 Indicadores qualitativos**

* Percepção de utilidade do relatório pelo produtor;  
* Compreensão da informação apresentada;  
* Percepção de facilidade de uso pelo técnico operador;  
* Principais dificuldades encontradas na operação;  
* Sugestões dos produtores e do sindicato.

# **14\. Riscos e estratégias de mitigação**

| Risco | Impacto | Mitigação |
| :---- | :---- | :---- |
| Ausência de registro prévio na origem do dado | Alto | O produtor não anota produção nem gastos; o dado vem da memória. Reduzir campos obrigatórios ao mínimo, registrar data de coleta separada da data do fato, indicar no relatório que os valores são declarados, avaliar entrega de instrumento simples de anotação. |
| Expectativa indevida sobre o valor probatório do relatório | Alto | Submeter o tema ao professor orientador antes de firmar requisito; incluir ressalva no documento gerado; vedar linguagem de garantia em qualquer comunicação com sindicato e produtores. |
| Desalinhamento de expectativa quanto ao alcance do software | Alto | Comunicar explicitamente que o sistema não endereça orientação técnica, maquinário, insumos nem permanência de jovens no campo. |
| Dependência de operador único no sindicato | Alto | Operação concentrada em titular e suplente designados; demais diretores com acesso de consulta. Capacitar ambos e documentar o fluxo em manual. |
| Baixa adesão dos produtores ao fornecimento de dados | Alto | Envolver produtores desde o diagnóstico; manter o esforço de coleta baixo e demonstrar valor pelo relatório. |
| Escopo muito grande | Alto | MVP de núcleo único: registro de produção e despesas alimentando o relatório de dupla finalidade. |
| Premissa de comercialização | Médio | Metade dos associados comercializa; a apuração de resultado atinge parcela relevante da base. Módulo de vendas mantido. |
| Indisponibilidade da autoridade ratificadora | Médio | Submeter regras em lotes por marco, evitando dependência de resposta pontual. |
| Relatório não compreendido pelo produtor | Médio | Validar o formato com produtores antes da versão final; oficina de interpretação. |
| Baixa familiaridade tecnológica do operador | Médio | Interface simples, capacitação e suporte inicial. |

# **15\. Possíveis evoluções**

* Conta individual de acesso do produtor ao sistema;  
* Interface responsiva para tablet e celular;  
* Aplicativo móvel ou PWA;  
* Modo offline para áreas com conexão limitada;  
* Previsão e consulta de condições climáticas;  
* Integração com sensores de irrigação;  
* Mapa de propriedades e pontos de comercialização;  
* Marketplace para aproximar produtores e compradores;  
* Recomendações baseadas no histórico de produção;  
* Previsão de demanda;  
* Dashboard institucional consolidado para o sindicato;  
* Módulo para cooperativas;  
* Inteligência artificial para apoio à análise de dados.

As duas primeiras evoluções decorrem diretamente de decisões da Fase 0: o acesso do produtor e a responsividade foram deliberadamente retirados do MVP por restrição de ambiente de uso, e não por falta de relevância.

Essas funcionalidades devem ser consideradas somente após a validação do MVP. A prioridade do projeto extensionista deve ser resolver um problema real identificado com os produtores.

# **16\. Resultados esperados**

* Disponibilização de uma ferramenta digital gratuita para a comunidade participante;  
* Melhoria na organização dos registros de produção e vendas;  
* Maior visibilidade sobre custos e receitas;  
* Capacitação dos produtores no uso de tecnologias digitais;  
* Produção de indicadores que auxiliem a tomada de decisão;  
* Aproximação entre a universidade e a comunidade;  
* Aplicação prática dos conhecimentos adquiridos pelos estudantes de TI.

# **17\. Considerações finais**

O AgroGestão apresenta potencial para ser um projeto extensionista relevante por combinar  
 tecnologia, capacitação e necessidades concretas de uma comunidade. O principal diferencial  
 da proposta é colocar os produtores no centro do processo: o sistema deve ser definido a partir  
 dos problemas encontrados no diagnóstico, e não apenas a partir das preferências técnicas da  
 equipe.

A solução será desenvolvida com tecnologias já dominadas pela equipe de estudantes de TI,  
 como React, Spring Boot e PostgreSQL, permitindo a aplicação de conhecimentos de  
 desenvolvimento web, banco de dados, segurança, APIs e engenharia de software. Ao mesmo  
 tempo, a realização de entrevistas, oficinas e avaliação de impacto caracteriza a dimensão  
 extensionista.

O projeto conta com parceria já estabelecida com o Sindicato dos Trabalhadores Rurais de  
 Redenção-CE, o que permite iniciar o diagnóstico inicial com a comunidade parceira desde as  
 primeiras semanas do cronograma, sem a necessidade de uma etapa preliminar de busca por  
 parceiros.