import { useNavigate, useOutletContext } from 'react-router'
import { ArrowRight, Sprout, TrendingUp, Users, Wallet } from 'lucide-react'
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
  Metric,
  PageHeading,
  SectionHeader,
  Status,
} from '../../components/ui.jsx'

export function Dashboard() {
  const { data } = useData()
  const navigate = useNavigate()
  const { openModal } = useOutletContext()
  const { canWrite } = usePerfil()
  const dashboard = data.dashboard || {
    producerCount: 0,
    activePlantingCount: 0,
    revenue: 0,
    result: 0,
    bars: [],
    cultures: [],
  }
  const bars = dashboard.bars || []
  const cultureResults = dashboard.cultures || []
  const max = Math.max(1, ...bars.flatMap((row) => [row.revenue, row.expense]))
  return (
    <>
      <PageHeading
        eyebrow="Dados atualizados hoje"
        title="Visão geral"
        description="Acompanhe a atividade dos produtores e os resultados da comunidade."
        action={
          canWrite && (
            <Action testId="new-record" onClick={() => openModal('record')}>
              Novo registro
            </Action>
          )
        }
      />
      <div className="metric-grid">
        <Metric
          icon={Users}
          label="Produtores cadastrados"
          value={number(dashboard.producerCount)}
          detail="Cadastros ativos"
          onClick={() => navigate('/produtores')}
        />
        <Metric
          icon={Sprout}
          label="Culturas em andamento"
          value={number(dashboard.activePlantingCount)}
          detail="Plantios ativos"
          tone="gold"
          onClick={() => navigate('/producao')}
        />
        <Metric
          icon={Wallet}
          label="Receita no período"
          value={money(dashboard.revenue)}
          detail="Vendas registradas"
          tone="blue"
          onClick={() => navigate('/vendas')}
        />
        <Metric
          icon={TrendingUp}
          label="Resultado apurado"
          value={money(dashboard.result)}
          detail="Receitas menos despesas"
          tone="coral"
          onClick={() => navigate('/financeiro?tab=resultados')}
        />
      </div>
      <div className="dashboard-grid">
        <section className="panel chart-panel">
          <SectionHeader
            title="Movimentação financeira"
            subtitle="Receitas e despesas nos últimos 6 meses"
          />
          <div
            className="bar-chart"
            role="img"
            aria-label="Receitas e despesas por mês"
          >
            {bars.map((row) => (
              <div className="bar-group" key={row.key}>
                <div className="bars">
                  <span
                    className="bar income"
                    style={{
                      height: `${Math.max(3, (row.revenue / max) * 100)}%`,
                    }}
                    title={`${row.label}: receitas ${money(row.revenue)}`}
                  />
                  <span
                    className="bar expense"
                    style={{
                      height: `${Math.max(3, (row.expense / max) * 100)}%`,
                    }}
                    title={`${row.label}: despesas ${money(row.expense)}`}
                  />
                </div>
                <span>{row.label}</span>
              </div>
            ))}
          </div>
          <div className="chart-legend">
            <span>
              <i className="legend-income" />
              Receitas
            </span>
            <span>
              <i className="legend-expense" />
              Despesas
            </span>
          </div>
        </section>
        <section className="panel culture-panel">
          <SectionHeader
            title="Resultados por cultura"
            subtitle="Produção comercializada no período"
          />
          <div className="culture-bars">
            {cultureResults.map(({ id, name, value, width }, index) => (
              <div key={id}>
                <div>
                  <strong>{name}</strong>
                  <span>{money(value)}</span>
                </div>
                <div className="progress">
                  <span
                    className={`progress-${index}`}
                    style={{
                      width: `${width}%`,
                    }}
                  />
                </div>
              </div>
            ))}
          </div>
        </section>
      </div>
      <section className="panel recent-panel">
        <SectionHeader
          title="Produtores recentes"
          subtitle="Últimos cadastros e atualizações"
          action={
            <button
              className="text-button"
              type="button"
              data-testid="see-all-producers"
              onClick={() => navigate('/produtores')}
            >
              Ver todos <ArrowRight size={15} />
            </button>
          }
        />
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Produtor</th>
                <th>Cultura principal</th>
                <th>Atualização</th>
                <th>Status</th>
              </tr>
            </thead>
            <tbody>
              {data.producers.slice(0, 4).map((producer) => {
                const plant = data.plantings.find(
                  (row) => row.producerId === producer.id,
                )
                const property = data.properties.find(
                  (row) => row.producerId === producer.id,
                )
                return (
                  <tr
                    key={producer.id}
                    data-testid={`producer-row-${producer.id}`}
                    onClick={() => navigate(`/produtores/${producer.id}`)}
                    tabIndex={0}
                    onKeyDown={(event) => {
                      if (event.key === 'Enter')
                        navigate(`/produtores/${producer.id}`)
                    }}
                  >
                    <td>
                      <span className="person-cell">
                        <span className="small-avatar">
                          {producer.name
                            .split(' ')
                            .map((part) => part[0])
                            .slice(0, 2)
                            .join('')}
                        </span>
                        <span>
                          <strong>{producer.name}</strong>
                          <small>{property?.name || producer.community}</small>
                        </span>
                      </span>
                    </td>
                    <td>
                      {byId(data.cultures, plant?.cultureId)?.name || '—'}
                    </td>
                    <td>{dateText(plant?.startedAt)}</td>
                    <td>
                      <Status
                        tone={
                          plant?.status === 'Em andamento' ? 'green' : 'gold'
                        }
                      >
                        {plant?.status || 'Sem plantio'}
                      </Status>
                    </td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        </div>
      </section>
    </>
  )
}
