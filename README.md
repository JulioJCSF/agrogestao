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
- não há estoque registrado: o saldo disponível de cada plantio é calculado (produção menos vendas) e é apenas informativo — não bloqueia a venda;
- os cálculos e relatórios devem considerar o ciclo produtivo das culturas;
- relatórios podem auxiliar na organização de informações sobre atividade rural, mas não representam garantia de aceitação por órgãos externos.

## Protótipo

O protótipo das telas do AgroGestão pode ser acessado no link abaixo:

[Visualizar protótipo no v0](https://v0.app/jggnobregu7-9226s-projects/chat/prototipagem-do-agrogestao-fQVvjI5ARhQ)

### Executar as telas localmente

Com Node.js 22 e npm instalados:

```bash
cd web
npm install
npm run dev
```

Abra o endereço mostrado pelo Vite. Na tela de login, **Explorar prévia com dados fictícios** permite navegar pelas telas e examinar os formulários sem a API. A prévia é somente visual: não salva, altera nem exclui registros. A sessão de prévia fica no `sessionStorage`; os dados fictícios são arquivos estáticos em `web/src/services/mockData.js`.

Para usar a prévia estática, copie `web/.env.example` para `web/.env.local` e inicie o Vite. Com `VITE_USAR_MOCK=true`, o botão **Explorar prévia** mostra inclusive a interface de administração, sem autenticar ninguém. Para solicitar dados reais, use `VITE_USAR_MOCK=false` e reinicie o Vite. O login e as operações reais dependem da API; as telas usam `web/src/services/registroService.js`, e o cliente Axios usa `/api/v1`, com proxy local para `http://localhost:8080`.

O contrato do MVP está publicado no Swagger pela F13: DTOs, exemplos, paginação e endpoints. Login, propriedades, culturas e categorias de despesa têm implementação real; os demais módulos respondem `501 NAO_IMPLEMENTADO` até suas próprias issues. O front usa os nomes e URLs desse contrato e mostra a indisponibilidade sem substituir uma sessão real pela prévia. Cálculos de resultado, saldo, rateio e relatório pertencem à API. Os números da prévia são exemplos estáticos.

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

## Ambiente de desenvolvimento

O ambiente é híbrido: só o PostgreSQL roda em container, pelo Docker Compose; a API roda direto no Windows. Ver `docs/adr/0009-ambiente-desenvolvimento-hibrido.md`.

### Pré-requisitos

- Docker Desktop
- JDK 21 (Temurin)
- Node 22 LTS

Para conferir as versões instaladas:

```bash
docker --version
docker compose version
java -version
node --version
```

### Primeiro uso

Na raiz do repositório:

```bash
cp .env.example .env
docker compose up -d db
```

No PowerShell, use `Copy-Item .env.example .env` no lugar do `cp`.

Depois de copiar, preencha `JWT_SEGREDO` no `.env`: é a chave que assina os tokens de login, não tem valor padrão e precisa de pelo menos 32 caracteres. A API não sobe sem ela. Para gerar uma: `openssl rand -base64 48` (no Git Bash).

### Rodar a API

A API lê o `.env` da raiz; não é preciso exportar variáveis.

- Git Bash, a partir de `api/`: `./mvnw spring-boot:run`
- PowerShell, a partir de `api/`: `.\mvnw.cmd spring-boot:run`
- IDE: executar a classe `ApiApplication`. No VS Code, abra a pasta `api/` (e não a raiz do repositório); caso contrário o `.env` não é carregado e a API falha com `password authentication failed for user "${DB_USER}"`.

### Hot-reload

O DevTools reinicia a API quando uma classe é recompilada — salvar o `.java` não basta, a IDE precisa compilar.

- **VS Code** (Extension Pack for Java): compila ao salvar, sem configuração adicional.
- **IntelliJ IDEA**: em *Settings → Build, Execution, Deployment → Compiler*, ativar **Build project automatically**; em *Settings → Advanced Settings*, ativar **Allow auto-make to start even if developed application is currently running**.

### Formatação do Java

O código Java segue o Google Java Format, verificado pelo Spotless. O `verify` falha se houver arquivo fora do padrão.

Antes de abrir PR, formate o código:

- Git Bash, a partir de `api/`: `./mvnw spotless:apply`
- PowerShell, a partir de `api/`: `.\mvnw.cmd spotless:apply`

Para só conferir, sem alterar nada, use `spotless:check` no lugar de `spotless:apply`.

### Swagger

Com a API no ar: http://localhost:8080/swagger-ui.html

Só o login e o próprio Swagger são públicos; os demais endpoints exigem token. Para logar com o administrador inicial:

1. Em **Autenticação**, abra `POST /api/v1/auth/login`, clique em **Try it out** e envie o `ADMIN_LOGIN` e a `ADMIN_SENHA` do seu `.env`:
   ```json
   { "login": "admin", "senha": "admin_dev" }
   ```
2. Copie o valor de `token` da resposta.
3. Clique em **Authorize** (no topo da página), cole o token e confirme. Não digite `Bearer`; o Swagger acrescenta.
4. Confira com `GET /api/v1/auth/me`, que devolve o usuário logado.

O token vale `JWT_EXPIRACAO_MINUTOS` (60 por padrão). Quando expirar, a API responde 401 e é preciso logar de novo.

### Testes da API

A partir de `api/`:

- Unitários: `./mvnw test` — não usam o Compose nem exigem Docker.
- Verificação completa: `./mvnw verify` — unitários, integração e Spotless. Exige Docker Desktop no ar, mas não o banco do Compose.
- Integração específica: `./mvnw verify -Dit.test=AuthIT`.
- Formatação: `./mvnw spotless:apply`.

No PowerShell, use `.\mvnw.cmd` e coloque parâmetros `-D` entre aspas.

Surefire executa classes `*Test`; Failsafe executa `*IT` no `verify`. A base `IntegracaoTest` importa `PostgresTesteConfig`, usa `@ServiceConnection` e a imagem `postgres:17-alpine`. O perfil `test` define os placeholders sintéticos do admin e ignora o `.env`. Os testes não alteram o banco local.

### Perfil dev e massa sintética

A partir de `api/`: `./mvnw spring-boot:run -Dspring-boot.run.profiles=dev` (PowerShell: `.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"`).

O perfil `dev` carrega `db/dev/dados-sinteticos.sql` depois das migrations, com um inicializador idempotente: três produtores claramente fictícios, CPFs gerados com dígitos verificadores, propriedades e plantios do catálogo. O perfil padrão e os testes comuns não executam esse inicializador.

**Limitação da F12 (#86):** a migration antiga `V20261005_1020` já contém exemplos, usuários de exemplo e referências em todos os perfis. Ela foi preservada conforme orientação desta entrega. A massa adicional fica em `dev`; a separação integral da carga antiga ainda exige decisão, e #86 permanece aberta. Não há nova migration nem alteração de checksum.

Para zerar somente o banco local de desenvolvimento, pare a API e execute `docker compose down -v` na raiz, depois `docker compose up -d db`. O comando apaga o volume local e todos os registros nele.

### Verificar o front-end

Em `web/`, com Node 22:

```bash
npm ci
npm run lint
npm run build
npm run smoke
npm run test:contrato
npx playwright install chromium
npm test
```

Para usar o Chrome já instalado no Windows, defina `$env:PLAYWRIGHT_CHANNEL='chrome'` antes de `npm test`.

`npm run test:api-real` verifica o login e o Bearer no navegador contra a API real, com `VITE_USAR_MOCK=false`. Antes, gere o JAR com `./mvnw verify`. O comando cria PostgreSQL temporário sem volume, gera credenciais sintéticas, sobe a API na porta 18080 e encerra ambos ao terminar. O log fica em `api/target/browser-api.log`. O teste não usa o banco do Compose nem dados reais.

A página de exemplo dos componentes está em `/tests/components.html` durante o desenvolvimento. Ela não aparece no menu e não é incluída no build de produção. Os componentes reutilizáveis estão em `web/src/components/`.

As issues com label `qa` (#32, #46, #57) e a validação com o sindicato (#36) foram excluídas do escopo por orientação. Veja [o registro da sprint 2](docs/agrogestao-sprint-2.md).

### Conflito na porta 5432

Se já houver um PostgreSQL instalado no Windows usando a porta 5432, mude `DB_PORT` no `.env` (por exemplo, `DB_PORT=5433`) e suba o banco de novo.

### Parar o banco

```bash
docker compose down      # para o container; os dados são preservados
docker compose down -v   # para e apaga o volume; o banco é zerado
```
