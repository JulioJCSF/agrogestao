import { useState } from 'react'
import { useOutletContext, useParams, useSearchParams } from 'react-router'
import { Plus, Search, Sprout, Store, Users, Wallet } from 'lucide-react'
import {
  byId,
  dateText,
  money,
  number,
  rotuloPlantio,
  resultadoPlantio,
  quantidadeProduzida,
  quantidadeVendida,
  useData,
} from '../../contexts/DataContext.jsx'
import { usePerfil } from '../../contexts/AuthContext.jsx'
import {
  Action,
  BackLink,
  Empty,
  NotFound,
  BotaoAbrirPlantio,
  PageHeading,
  Pagination,
  SectionHeader,
  Status,
  Tabs,
} from '../../components/ui.jsx'

export function Producao() {
  const { data } = useData()
  const [params, setParams] = useSearchParams()
  const tab = params.get('tab') || 'plantios'
  const setTab = (value) => setParams({ tab: value })
  const { openModal } = useOutletContext()
  const { canWrite } = usePerfil()
  const [query, setQuery] = useState('')
  const [status, setStatus] = useState('Todos')
  const [producerId, setProducerId] = useState('')
  const [from, setFrom] = useState('')
  const [to, setTo] = useState('')
  const [page, setPage] = useState(1)
  const plantings = data.plantings.filter(
    (row) =>
      (status === 'Todos' || row.status === status) &&
      (!producerId || row.producerId === producerId) &&
      (!from || row.startedAt >= from) &&
      (!to || row.startedAt <= to) &&
      `${rotuloPlantio(data, row)} ${byId(data.producers, row.producerId)?.name}`
        .toLowerCase()
        .includes(query.toLowerCase()),
  )
  return (
    <>
      <PageHeading
        eyebrow="Ciclos produtivos"
        title="Produção"
        description="Gerencie plantios, registre colheitas e acompanhe cada safra."
        action={
          canWrite && (
            <Action
              testId={tab === 'plantios' ? 'add-planting' : 'add-production'}
              onClick={() =>
                openModal(tab === 'plantios' ? 'planting' : 'production')
              }
            >
              {tab === 'plantios' ? 'Novo plantio' : 'Registrar produção'}
            </Action>
          )
        }
      />
      <Tabs
        active={tab}
        setActive={setTab}
        tabs={[
          { key: 'plantios', label: 'Plantios' },
          { key: 'colheitas', label: 'Colheitas' },
        ]}
      />
      {tab === 'plantios' ? (
        <div className="panel list-panel">
          <div className="list-toolbar">
            <label className="search-field">
              <Search size={18} />
              <input
                data-testid="filter-plantings"
                value={query}
                onChange={(event) => {
                  setQuery(event.target.value)
                  setPage(1)
                }}
                placeholder="Buscar cultura, propriedade ou produtor"
              />
            </label>
            <select
              data-testid="filter-planting-status"
              value={status}
              onChange={(event) => {
                setStatus(event.target.value)
                setPage(1)
              }}
            >
              <option>Todos</option>
              <option>Em andamento</option>
              <option>Encerrado</option>
            </select>
          </div>
          <div className="filter-row">
            <label>
              Produtor{' '}
              <select
                data-testid="filter-planting-producer"
                value={producerId}
                onChange={(event) => {
                  setProducerId(event.target.value)
                  setPage(1)
                }}
              >
                <option value="">Todos</option>
                {data.producers.map((row) => (
                  <option key={row.id} value={row.id}>
                    {row.name}
                  </option>
                ))}
              </select>
            </label>
            <label>
              De{' '}
              <input
                type="date"
                lang="pt-BR"
                data-testid="filter-planting-from"
                value={from}
                onChange={(event) => {
                  setFrom(event.target.value)
                  setPage(1)
                }}
              />
            </label>
            <label>
              Até{' '}
              <input
                type="date"
                lang="pt-BR"
                data-testid="filter-planting-to"
                value={to}
                onChange={(event) => {
                  setTo(event.target.value)
                  setPage(1)
                }}
              />
            </label>
          </div>
          {plantings.length ? (
            <div className="table-wrap">
              <table>
                <thead>
                  <tr>
                    <th>Cultura e propriedade</th>
                    <th>Produtor</th>
                    <th>Área</th>
                    <th>Plantio</th>
                    <th>Previsão</th>
                    <th>Situação</th>
                    <th />
                  </tr>
                </thead>
                <tbody>
                  {plantings.slice((page - 1) * 10, page * 10).map((row) => (
                    <tr key={row.id} data-testid={`planting-row-${row.id}`}>
                      <td>
                        <strong>
                          {byId(data.cultures, row.cultureId)?.name}
                        </strong>
                        <small className="table-sub">
                          {byId(data.properties, row.propertyId)?.name}
                        </small>
                      </td>
                      <td>{byId(data.producers, row.producerId)?.name}</td>
                      <td>{number(row.area, 3)} ha</td>
                      <td>{dateText(row.startedAt)}</td>
                      <td>{dateText(row.expectedAt)}</td>
                      <td>
                        <Status
                          tone={
                            row.status === 'Em andamento' ? 'green' : 'gray'
                          }
                        >
                          {row.status}
                        </Status>
                      </td>
                      <td>
                        <BotaoAbrirPlantio id={row.id} />
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : (
            <Empty />
          )}
          <Pagination
            total={plantings.length}
            page={page}
            onPageChange={setPage}
          />
        </div>
      ) : (
        <div className="panel list-panel">
          <SectionHeader
            title="Colheitas registradas"
            subtitle="Quantidade produzida e data do fato"
          />
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Plantio</th>
                  <th>Produtor</th>
                  <th>Data</th>
                  <th>Quantidade</th>
                </tr>
              </thead>
              <tbody>
                {data.production.map((row) => {
                  const plant = byId(data.plantings, row.plantingId)
                  return (
                    <tr key={row.id} data-testid={`production-row-${row.id}`}>
                      <td>{rotuloPlantio(data, plant)}</td>
                      <td>{byId(data.producers, plant?.producerId)?.name}</td>
                      <td>{dateText(row.date)}</td>
                      <td>
                        <strong>
                          {number(row.amount, 3)} {row.unit}
                        </strong>
                      </td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </>
  )
}

export function DetalhePlantio() {
  const { id } = useParams()
  const { data } = useData()
  const { openModal } = useOutletContext()
  const { canWrite } = usePerfil()
  const plant = byId(data.plantings, id)
  if (!plant) return <NotFound />
  const result = resultadoPlantio(data, id)
  const productions = data.production.filter((row) => row.plantingId === id)
  const expenses = data.expenses.filter((row) =>
    row.allocations?.some((entry) => entry.plantingId === id),
  )
  const dailies = data.dailies.filter((row) => row.plantingId === id)
  const saleItems = data.sales.flatMap((sale) =>
    (sale.items || [])
      .filter((item) => item.plantingId === id)
      .map((item) => ({ ...item, sale })),
  )
  return (
    <>
      <BackLink to="/producao" label="Voltar para produção" />
      <PageHeading
        eyebrow={plant.status}
        title={byId(data.cultures, plant.cultureId)?.name || 'Plantio'}
        description={`${byId(data.properties, plant.propertyId)?.name} · ${byId(data.producers, plant.producerId)?.name} · ${number(plant.area, 3)} ha`}
        action={
          canWrite && (
            <div className="heading-actions">
              <Action
                testId="edit-planting"
                onClick={() => openModal('planting', plant)}
                icon={null}
                light
              >
                Editar plantio
              </Action>
              {plant.status === 'Em andamento' && (
                <button
                  type="button"
                  className="secondary-button"
                  data-testid="close-planting"
                  onClick={() => openModal('closePlanting', plant)}
                >
                  Encerrar plantio
                </button>
              )}
            </div>
          )
        }
      />
      <div className="mini-metrics">
        <div>
          <span>Quantidade produzida</span>
          <strong>
            {number(quantidadeProduzida(data, id), 3)}{' '}
            {byId(data.cultures, plant.cultureId)?.unit}
          </strong>
        </div>
        <div>
          <span>Quantidade vendida</span>
          <strong>
            {number(quantidadeVendida(data, id), 3)}{' '}
            {byId(data.cultures, plant.cultureId)?.unit}
          </strong>
        </div>
        <div>
          <span>Receita</span>
          <strong>{money(result.revenue)}</strong>
        </div>
        <div>
          <span>Custo de desembolso</span>
          <strong>{money(result.cost)}</strong>
        </div>
        <div>
          <span>Resultado</span>
          <strong className={result.result < 0 ? 'negative' : ''}>
            {money(result.result)}
          </strong>
        </div>
      </div>
      <p className="result-note">
        Mão de obra familiar: {money(result.familyLabor)} (valor complementar,
        separado do custo de desembolso).
      </p>
      <div className="detail-grid">
        <section className="panel">
          <SectionHeader
            title="Produção"
            subtitle="Colheitas deste plantio"
            action={
              canWrite && (
                <button
                  type="button"
                  className="text-button"
                  data-testid="add-production-planting"
                  onClick={() =>
                    openModal('production', null, { plantingId: id })
                  }
                >
                  <Plus size={16} /> Registrar
                </button>
              )
            }
          />
          {productions.length ? (
            <div className="stack-list">
              {productions.map((row) => (
                <div className="stack-row" key={row.id}>
                  <span className="list-icon">
                    <Sprout size={18} />
                  </span>
                  <span>
                    <strong>
                      {number(row.amount, 3)} {row.unit}
                    </strong>
                    <small>{dateText(row.date)}</small>
                  </span>
                </div>
              ))}
            </div>
          ) : (
            <Empty />
          )}
        </section>
        <section className="panel">
          <SectionHeader
            title="Despesas"
            subtitle="Valores rateados a este plantio"
            action={
              canWrite && (
                <button
                  type="button"
                  className="text-button"
                  data-testid="add-expense-planting"
                  onClick={() =>
                    openModal('expense', null, {
                      producerId: plant.producerId,
                      plantingId: id,
                    })
                  }
                >
                  <Plus size={16} /> Lançar
                </button>
              )
            }
          />
          {expenses.length ? (
            <div className="stack-list">
              {expenses.map((row) => (
                <div className="stack-row" key={row.id}>
                  <span className="list-icon gold">
                    <Wallet size={18} />
                  </span>
                  <span>
                    <strong>{row.description}</strong>
                    <small>
                      {dateText(row.date)} ·{' '}
                      {
                        row.allocations.find((entry) => entry.plantingId === id)
                          ?.percent
                      }
                      % do total
                    </small>
                  </span>
                  <b>
                    {money(
                      row.allocations.find((entry) => entry.plantingId === id)
                        ?.amount,
                    )}
                  </b>
                </div>
              ))}
            </div>
          ) : (
            <Empty />
          )}
        </section>
        <section className="panel">
          <SectionHeader
            title="Diárias"
            subtitle="Trabalho pago e familiar"
            action={
              canWrite && (
                <button
                  type="button"
                  className="text-button"
                  data-testid="add-daily-planting"
                  onClick={() => openModal('daily', null, { plantingId: id })}
                >
                  <Plus size={16} /> Registrar
                </button>
              )
            }
          />
          {dailies.length ? (
            <div className="stack-list">
              {dailies.map((row) => (
                <div className="stack-row" key={row.id}>
                  <span className="list-icon">
                    <Users size={18} />
                  </span>
                  <span>
                    <strong>
                      {row.days} diária(s) · {row.nature}
                    </strong>
                    <small>
                      {dateText(row.date)} · {money(row.unitValue)} por diária
                    </small>
                  </span>
                  <b>{money(row.amount)}</b>
                </div>
              ))}
            </div>
          ) : (
            <Empty />
          )}
        </section>
        <section className="panel">
          <SectionHeader
            title="Vendas"
            subtitle="Itens originados neste plantio"
            action={
              canWrite && (
                <button
                  type="button"
                  className="text-button"
                  data-testid="add-sale-planting"
                  onClick={() =>
                    openModal('sale', null, {
                      producerId: plant.producerId,
                      items: [
                        {
                          plantingId: id,
                          amount: '',
                          unit:
                            byId(data.cultures, plant.cultureId)?.unit || 'kg',
                          unitPrice: '',
                        },
                      ],
                    })
                  }
                >
                  <Plus size={16} /> Registrar
                </button>
              )
            }
          />
          {saleItems.length ? (
            <div className="stack-list">
              {saleItems.map((row, index) => (
                <div className="stack-row" key={`${row.sale.id}-${index}`}>
                  <span className="list-icon blue">
                    <Store size={18} />
                  </span>
                  <span>
                    <strong>
                      {number(row.amount, 3)} {row.unit}
                    </strong>
                    <small>
                      {dateText(row.sale.date)} ·{' '}
                      {byId(data.clients, row.sale.clientId)?.name}
                    </small>
                  </span>
                  <b>{money(row.amount * row.unitPrice)}</b>
                </div>
              ))}
            </div>
          ) : (
            <Empty />
          )}
        </section>
      </div>
    </>
  )
}
