// Geração de dados sintéticos (RNF10) sem dependência: serve aos testes e ao popular:dev.

/** Gerador determinístico (mulberry32): a mesma semente devolve a mesma sequência em [0, 1). */
export function criarAleatorio(semente) {
  let estado = semente >>> 0
  return () => {
    estado = (estado + 0x6d2b79f5) >>> 0
    let t = estado
    t = Math.imul(t ^ (t >>> 15), t | 1)
    t ^= t + Math.imul(t ^ (t >>> 7), t | 61)
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296
  }
}

/** CPF sintético válido com prefixo 999 (RNF10), com os dígitos sorteados pelo gerador. */
export function cpfSintetico(aleatorio) {
  const base = [9, 9, 9]
  while (base.length < 9) base.push(Math.floor(aleatorio() * 10))
  for (const tamanho of [9, 10]) {
    const soma = base
      .slice(0, tamanho)
      .reduce((total, digito, i) => total + digito * (tamanho + 1 - i), 0)
    const resto = (soma * 10) % 11
    base.push(resto === 10 ? 0 : resto)
  }
  return base.join('')
}
