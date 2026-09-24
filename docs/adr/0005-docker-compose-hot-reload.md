# ADR-0005 — Docker Compose desde o início, com hot-reload

**Status:** Substituída por ADR-0009
**Data:** 2026-09-24

## Contexto
A equipe tem mais de cinco desenvolvedores, com disponibilidade parcial, trabalhando em paralelo no mesmo monorepo. Diferenças de ambiente entre as máquinas custam tempo que o cronograma de 15 semanas não tem. O time inteiro usa Windows.

## Decisão
Front-end, back-end e banco de dados totalmente conteinerizados desde o início do desenvolvimento, orquestrados por Docker Compose, com volumes montados para hot-reload.

## Alternativas consideradas
- Ambiente instalado diretamente em cada máquina — sem garantia de consistência entre as máquinas da equipe

## Consequências
- Todas as máquinas sobem o mesmo ambiente com o mesmo comando.
- **O hot-reload só funciona com o repositório dentro do WSL2. Fora dele, é necessário polling, com custo de CPU.** Bind mount a partir de `C:\` ou `F:\` não propaga eventos de arquivo para o container e é lento em I/O.
- Piora: exige Docker e WSL2 configurados em toda máquina antes de o desenvolvedor começar a trabalhar.
