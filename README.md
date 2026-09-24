# AgroGestão

O **AgroGestão** é um sistema web voltado ao apoio da gestão da agricultura familiar, com foco na organização de informações relacionadas à produção, custos, estoque, vendas e resultados financeiros.

O projeto é desenvolvido como uma iniciativa extensionista e busca oferecer uma solução simples, acessível e adequada à realidade de pequenos produtores rurais e agricultores familiares.

## Objetivo

O objetivo do AgroGestão é disponibilizar uma plataforma digital que auxilie no registro e acompanhamento das principais atividades de gestão da produção rural, permitindo maior organização das informações e melhor visualização de custos, receitas, produção e vendas.

## Público-alvo

O sistema tem como público principal:

- Agricultores familiares;
- Pequenos produtores rurais;
- Pequenos horticultores;
- Produtores de frutas, hortaliças, raízes e grãos;
- Trabalhadores rurais e associados vinculados à comunidade parceira.

O projeto é desenvolvido em parceria com o **Sindicato dos Trabalhadores Rurais de Redenção - CE**.

## Principais funcionalidades previstas

O MVP do AgroGestão contempla, de forma geral:

- Autenticação e controle de acesso;
- Cadastro de produtores;
- Cadastro de propriedades;
- Cadastro de culturas e plantios;
- Registro da produção;
- Controle básico de estoque;
- Registro de despesas e receitas;
- Registro de diárias e custos de produção;
- Registro de vendas;
- Acompanhamento de resultados financeiros;
- Dashboard com indicadores;
- Geração de relatórios de produção, custos e atividade rural.

## Tecnologias

### Back-end

- Java;
- Spring Boot;
- Spring Security;
- JWT;
- Spring Data JPA / Hibernate;
- Bean Validation;
- Swagger / OpenAPI;
- Maven.

### Front-end

- React;
- Vite;
- HeroUI;
- React Router;
- Axios;
- Context API e Hooks.

### Banco de dados

- PostgreSQL;
- Flyway para versionamento do schema.

### Infraestrutura e qualidade

- Docker;
- Docker Compose;
- Git e GitHub;
- JUnit;
- Playwright.

## Estrutura do projeto

```text
agrogestao/
├── api/        # API REST desenvolvida com Spring Boot
├── web/        # Aplicação web do AgroGestão
└── README.md
```

## Regras de negócio importantes

Algumas regras foram definidas e refinadas durante as etapas de validação com a comunidade parceira:

- despesas podem ser associadas diretamente a um plantio ou compartilhadas entre diferentes plantios;
- despesas compartilhadas devem ser rateadas proporcionalmente à área utilizada por cada plantio;
- o percentual utilizado no rateio deve ser preservado para manter a consistência histórica dos cálculos;
- o registro de diárias integra o controle de custos da produção;
- vendas devem atualizar o estoque;
- os cálculos e relatórios devem considerar o ciclo produtivo das culturas;
- relatórios podem auxiliar na organização de informações sobre atividade rural, mas não representam garantia de aceitação por órgãos externos.

## Protótipo

O protótipo das telas do AgroGestão pode ser acessado no link abaixo:

[Visualizar protótipo no v0](https://v0.app/jggnobregu7-9226s-projects/chat/prototipagem-do-agrogestao-fQVvjI5ARhQ)

## Documentação do projeto

Os documentos, materiais de apoio, evidências de validação e demais arquivos relacionados ao projeto estão disponíveis no Google Drive:

[Acessar pasta do AgroGestão no Google Drive](https://drive.google.com/drive/folders/1CU-Jia17ymNc_5XILVbP35IHriFsZ9oh?usp=drive_link)

## Escopo do MVP

O projeto prioriza as funcionalidades essenciais identificadas durante o levantamento e validação de requisitos.

Funcionalidades mais avançadas, como integração meteorológica, marketplace, IoT, inteligência artificial, previsão de demanda e funcionalidades offline, são consideradas possíveis evoluções futuras e não fazem parte do escopo inicial obrigatório.

## Desenvolvimento

O desenvolvimento do sistema deverá ocorrer de forma incremental, priorizando entregas pequenas e validação contínua das funcionalidades.

As principais etapas previstas são:

1. Estruturação do ambiente e infraestrutura;
2. Modelagem de dados e API;
3. Implementação da autenticação;
4. Desenvolvimento dos cadastros principais;
5. Implementação dos módulos de produção e financeiro;
6. Implementação de estoque e vendas;
7. Desenvolvimento do dashboard;
8. Geração de relatórios;
9. Integração e testes;
10. Validação do MVP com a comunidade parceira.

## Status

> Projeto em desenvolvimento.
