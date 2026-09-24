# ADR-0009 — Ambiente de desenvolvimento híbrido

**Status:** Aceita
**Data:** 2026-09-24

Substitui a ADR-0005.

## Contexto
A ADR-0005 definiu front-end, back-end e banco totalmente conteinerizados, com hot-reload por volumes montados. O time inteiro usa Windows. Nesse cenário, o bind mount a partir do filesystem do Windows não propaga eventos de alteração de arquivo para o container e é lento em I/O: o hot-reload deixa de funcionar e o build e a instalação de dependências ficam várias vezes mais lentos. Fazer o modelo funcionar exigiria instalar o Ubuntu no WSL2 em todas as máquinas, com o repositório dentro dele, ou usar polling, com custo de CPU constante. Nenhuma das duas saídas se paga num cronograma de 15 semanas com equipe de disponibilidade parcial.

## Decisão
Em desenvolvimento, só o PostgreSQL roda em container, pelo Docker Compose. A API roda direto no Windows, pela IDE ou pelo Maven Wrapper, e o front-end pelo `npm run dev`.

## Alternativas consideradas
- Manter a ADR-0005 com o repositório dentro do WSL2 — exige instalar e manter um Ubuntu em cada máquina da equipe, além de mudar o fluxo de trabalho de todos (clone, IDE e terminal dentro do WSL)
- Manter a ADR-0005 com polling de arquivos — funciona a partir do filesystem do Windows, mas com alto custo de CPU e I/O ainda lento

## Consequências
- Cada máquina precisa de Docker Desktop, JDK 21 e Node 22.
- Hot-reload nativo: DevTools no back-end, HMR do Vite no front-end.
- As versões ficam fixadas no projeto — Maven Wrapper, `maven-enforcer-plugin` com Java 21 ou superior, `.nvmrc` e `engines` no `package.json` — e documentadas no README.
- Piora: a diferença de ambiente entre as máquinas passa a ser contida pelas versões fixadas, não eliminada. Um JDK ou Node instalado fora da versão pode gerar comportamento que não se reproduz em outra máquina.
- O banco continua idêntico em todas as máquinas, e os testes de integração usam a mesma imagem via Testcontainers.
