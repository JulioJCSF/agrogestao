import { useState } from 'react'
import { useOutletContext, useParams } from 'react-router'
import { CircleHelp, Printer, Search } from 'lucide-react'
import {
  byId,
  dateText,
  money,
  number,
  useData,
} from '../../contexts/DataContext.jsx'
import { usePerfil } from '../../contexts/AuthContext.jsx'
import {
  Action,
  BackLink,
  Empty,
  NotFound,
  PageHeading,
  Pagination,
  LinhaRelatorio,
} from '../../components/ui.jsx'

export function Relatorios() {
  const { data } = useData()
  const { openModal } = useOutletContext()
  const { canWrite } = usePerfil()
  const [query, setQuery] = useState('')
  const [page, setPage] = useState(1)
  const rows = data.reports.filter((row) =>
    (
      byId(data.producers, row.producerId)?.name ||
      row.snapshot?.producer?.name ||
      ''
    )
      .toLowerCase()
      .includes(query.toLowerCase()),
  )
  return (
    <>
      <PageHeading
        eyebrow="Documentos"
        title="Relatórios"
        description="Emita documentos com dados de produção, custos e atividade rural."
        action={
          canWrite && (
            <Action testId="new-report" onClick={() => openModal('report')}>
              Novo relatório
            </Action>
          )
        }
      />
      <div className="panel list-panel">
        <div className="list-toolbar">
          <label className="search-field">
            <Search size={18} />
            <input
              data-testid="filter-reports"
              value={query}
              onChange={(event) => {
                setQuery(event.target.value)
                setPage(1)
              }}
              placeholder="Buscar por produtor"
            />
          </label>
          <span className="count-label">{rows.length} emissões</span>
        </div>
        {rows.length ? (
          <div className="stack-list">
            {rows.slice((page - 1) * 10, page * 10).map((row) => (
              <LinhaRelatorio key={row.id} row={row} data={data} />
            ))}
          </div>
        ) : (
          <Empty
            title="Nenhum relatório emitido"
            detail="Selecione um produtor com consentimento e um período para emitir."
          />
        )}
        <Pagination total={rows.length} page={page} onPageChange={setPage} />
      </div>
      <div className="report-disclaimer">
        <CircleHelp size={19} />
        <span>
          O relatório organiza os registros da atividade rural. Sua emissão não
          garante aceitação pelo INSS ou por outro órgão.
        </span>
      </div>
    </>
  )
}

export function DetalheRelatorio() {
  const { id } = useParams()
  const { data } = useData()
  const report = byId(data.reports, id)
  if (!report?.snapshot) return <NotFound />
  const snapshot = report.snapshot
  const summary = snapshot.summary || {}
  return (
    <div className="report-page">
      <div className="report-actions">
        <BackLink to="/relatorios" label="Voltar para relatórios" />
        <button
          type="button"
          className="primary-button"
          data-testid="print-report"
          onClick={() => window.print()}
        >
          <Printer size={17} /> Imprimir relatório
        </button>
      </div>
      <article className="report-sheet">
        <div className="report-header">
          <div className="stamp-space">Espaço para timbre institucional</div>
          <div>
            <strong>AgroGestão</strong>
            <small>Sindicato dos Trabalhadores Rurais de Redenção – CE</small>
          </div>
        </div>
        <div className="report-title">
          <span>RELATÓRIO DE ATIVIDADE RURAL</span>
          <h1>Produção, despesas e resultado</h1>
          <p>
            Documento emitido em {dateText(report.issuedAt)} · Período de{' '}
            {dateText(report.from)} a {dateText(report.to)}
          </p>
        </div>
        <div className="report-info">
          <div>
            <span>Produtor</span>
            <strong>{snapshot.producer.name}</strong>
          </div>
          <div>
            <span>Comunidade</span>
            <strong>{snapshot.producer.community || 'Não informada'}</strong>
          </div>
          <div>
            <span>Propriedade(s)</span>
            <strong>
              {snapshot.properties.map((row) => row.name).join(', ') ||
                'Não informada'}
            </strong>
          </div>
          <div>
            <span>Consentimento</span>
            <strong>{dateText(snapshot.producer.consentAt)}</strong>
          </div>
        </div>
        <h2>Resumo da atividade</h2>
        <div className="report-summary">
          <div>
            <span>Quantidade produzida</span>
            <strong>{number(summary.produced, 3)}</strong>
          </div>
          <div>
            <span>Quantidade vendida</span>
            <strong>{number(summary.sold, 3)}</strong>
          </div>
          <div>
            <span>Receitas</span>
            <strong>{money(summary.revenue)}</strong>
          </div>
          <div>
            <span>Despesas e diárias pagas</span>
            <strong>{money(summary.cost)}</strong>
          </div>
          <div>
            <span>Lucro</span>
            <strong>{money(summary.profit)}</strong>
          </div>
        </div>
        <p className="report-labor">
          Mão de obra familiar (valor complementar):{' '}
          {money(summary.familyLabor)}.
        </p>
        <h2>Ciclos produtivos</h2>
        <table className="report-table">
          <thead>
            <tr>
              <th>Cultura</th>
              <th>Área</th>
              <th>Produzido</th>
              <th>Vendido</th>
              <th>Receita</th>
              <th>Despesa</th>
              <th>Diárias pagas</th>
            </tr>
          </thead>
          <tbody>
            {snapshot.plantings.map((row) => (
              <tr key={row.id}>
                <td>{row.culture}</td>
                <td>{number(row.area, 3)} ha</td>
                <td>{number(row.result.produced, 3)}</td>
                <td>{number(row.result.sold, 3)}</td>
                <td>{money(row.result.revenue)}</td>
                <td>{money(row.result.expense)}</td>
                <td>{money(row.result.paidLabor || 0)}</td>
              </tr>
            ))}
          </tbody>
        </table>
        <p className="report-caveat">
          Este documento organiza informações registradas no AgroGestão para
          acompanhamento do produtor. Não constitui garantia de aceitação pelo
          INSS ou por qualquer outro órgão. As quantidades produzida e vendida
          são apresentadas separadamente.
        </p>
        <div className="signature-grid">
          <div>
            <span />
            Assinatura do agricultor
          </div>
          <div>
            <span />
            Assinatura do presidente do sindicato
          </div>
        </div>
      </article>
    </div>
  )
}
