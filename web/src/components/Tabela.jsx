import { Empty } from './ui.jsx'
import { Aviso, Carregando } from './Feedback.jsx'
/** Paginação começa em zero, igual ao contrato {itens, pagina, tamanho, total}. */
export function Tabela({
  pagina,
  colunas,
  busca = '',
  onBuscaChange,
  onPageChange,
  carregando = false,
  erro,
  rowKey = (row) => row.id,
  'data-testid': testId = 'tabela',
}) {
  const { itens = [], pagina: atual = 0, tamanho = 20, total = 0 } = pagina
  const pages = Math.max(1, Math.ceil(total / tamanho))
  return (
    <section className="panel list-panel" data-testid={testId}>
      {onBuscaChange && (
        <label className="search-field">
          Buscar
          <input
            value={busca}
            data-testid={`${testId}-busca`}
            onChange={(event) => onBuscaChange(event.target.value)}
          />
        </label>
      )}
      {carregando ? (
        <Carregando data-testid={`${testId}-carregando`} />
      ) : erro ? (
        <Aviso tipo="erro" data-testid={`${testId}-erro`}>
          {erro}
        </Aviso>
      ) : itens.length ? (
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                {colunas.map((col) => (
                  <th key={col.campo} scope="col">
                    {col.titulo}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              {itens.map((row) => (
                <tr
                  key={rowKey(row)}
                  data-testid={`${testId}-linha-${rowKey(row)}`}
                >
                  {colunas.map((col) => (
                    <td key={col.campo}>
                      {col.render ? col.render(row) : row[col.campo]}
                    </td>
                  ))}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ) : (
        <Empty data-testid={`${testId}-vazio`} />
      )}
      <nav className="pagination" aria-label="Paginação">
        <span>
          {total} registros · Página {atual + 1} de {pages}
        </span>
        <button
          type="button"
          data-testid={`${testId}-anterior`}
          disabled={carregando || atual === 0}
          onClick={() => onPageChange(atual - 1)}
        >
          Anterior
        </button>
        <button
          type="button"
          data-testid={`${testId}-proxima`}
          disabled={carregando || atual + 1 >= pages}
          onClick={() => onPageChange(atual + 1)}
        >
          Próxima
        </button>
      </nav>
    </section>
  )
}
