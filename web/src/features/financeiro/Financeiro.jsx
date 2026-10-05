import { useState } from 'react'
import { useOutletContext, useSearchParams } from 'react-router'
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
  Empty,
  PageHeading,
  SectionHeader,
  Status,
  Tabs,
} from '../../components/ui.jsx'

export function Financeiro() {
  const { data } = useData()
  const [params, setParams] = useSearchParams()
  const tab = params.get('tab') || 'despesas'
  const setTab = (value) => setParams({ tab: value })
  const [producerId, setProducerId] = useState('')
  const [from, setFrom] = useState('')
  const [to, setTo] = useState('')
  const { openModal } = useOutletContext()
  const { canWrite } = usePerfil()
  const inPeriod = (date) => (!from || date >= from) && (!to || date <= to)
  const expenses = data.expenses.filter(
    (row) =>
      inPeriod(row.date) && (!producerId || row.producerId === producerId),
  )
  const dailies = data.dailies.filter(
    (row) =>
      inPeriod(row.date) &&
      (!producerId ||
        byId(data.plantings, row.plantingId)?.producerId === producerId),
  )
  const plants = data.plantings.filter(
    (row) => !producerId || row.producerId === producerId,
  )
  return (
    <>
      <PageHeading
        eyebrow="Controle financeiro"
        title="Financeiro"
        description="Acompanhe despesas, diárias e resultados dos plantios."
        action={
          canWrite && (
            <Action
              testId="add-financial"
              onClick={() => openModal(tab === 'diarias' ? 'daily' : 'expense')}
            >
              {tab === 'diarias' ? 'Registrar diária' : 'Lançar movimentação'}
            </Action>
          )
        }
      />
      <Tabs
        active={tab}
        setActive={setTab}
        tabs={[
          { key: 'despesas', label: 'Despesas' },
          { key: 'diarias', label: 'Diárias' },
          { key: 'resultados', label: 'Resultados' },
        ]}
      />
      <div className="filter-row">
        <label>
          Produtor{' '}
          <select
            data-testid="finance-producer-filter"
            value={producerId}
            onChange={(event) => setProducerId(event.target.value)}
          >
            <option value="">Todos os produtores</option>
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
            data-testid="finance-from"
            value={from}
            onChange={(event) => setFrom(event.target.value)}
          />
        </label>
        <label>
          Até{' '}
          <input
            type="date"
            lang="pt-BR"
            data-testid="finance-to"
            value={to}
            onChange={(event) => setTo(event.target.value)}
          />
        </label>
      </div>
      {tab === 'despesas' && (
        <section className="panel list-panel">
          <SectionHeader
            title="Despesas registradas"
            subtitle="Custos associados aos ciclos produtivos"
          />
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Descrição</th>
                  <th>Produtor</th>
                  <th>Categoria</th>
                  <th>Data</th>
                  <th>Rateio</th>
                  <th>Valor</th>
                </tr>
              </thead>
              <tbody>
                {expenses.map((row) => (
                  <tr key={row.id} data-testid={`expense-row-${row.id}`}>
                    <td>
                      <strong>{row.description}</strong>
                      {row.system && (
                        <small className="table-sub">Gerada por diária</small>
                      )}
                    </td>
                    <td>{byId(data.producers, row.producerId)?.name}</td>
                    <td>{byId(data.categories, row.categoryId)?.name}</td>
                    <td>{dateText(row.date)}</td>
                    <td>
                      <details>
                        <summary data-testid={`expense-allocation-${row.id}`}>
                          {row.allocations?.length || 0} plantio(s)
                        </summary>
                        <div className="allocation-detail">
                          {row.allocations?.map((part) => (
                            <div key={part.plantingId}>
                              {rotuloPlantio(
                                data,
                                byId(data.plantings, part.plantingId),
                              )}
                              : {part.percent}% · {money(part.amount)}
                            </div>
                          ))}
                        </div>
                      </details>
                    </td>
                    <td>
                      <strong>{money(row.amount)}</strong>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          {!expenses.length && <Empty />}
        </section>
      )}
      {tab === 'diarias' && (
        <section className="panel list-panel">
          <SectionHeader
            title="Diárias registradas"
            subtitle="Valor aplicado em cada plantio"
          />
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Plantio</th>
                  <th>Natureza</th>
                  <th>Data</th>
                  <th>Quantidade</th>
                  <th>Valor unitário</th>
                  <th>Total</th>
                </tr>
              </thead>
              <tbody>
                {dailies.map((row) => (
                  <tr key={row.id} data-testid={`daily-row-${row.id}`}>
                    <td>
                      {rotuloPlantio(
                        data,
                        byId(data.plantings, row.plantingId),
                      )}
                    </td>
                    <td>
                      <Status tone={row.nature === 'Paga' ? 'gold' : 'green'}>
                        {row.nature}
                      </Status>
                    </td>
                    <td>{dateText(row.date)}</td>
                    <td>{row.days}</td>
                    <td>{money(row.unitValue)}</td>
                    <td>
                      <strong>{money(row.amount)}</strong>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          {!dailies.length && <Empty />}
        </section>
      )}
      {tab === 'resultados' && (
        <>
          {(from || to) && (
            <p className="notice">
              A apuração por período será exibida quando a API fornecer o
              resultado para o intervalo escolhido.
            </p>
          )}
          {!from && !to && (
            <>
              {producerId && (
                <p className="notice">
                  O resumo do produtor será exibido quando a API retornar a
                  apuração filtrada.
                </p>
              )}
              {!producerId && (
                <div className="mini-metrics">
                  <div>
                    <span>Receitas</span>
                    <strong>{money(data.resultSummary?.revenue)}</strong>
                  </div>
                  <div>
                    <span>Despesas e diárias pagas</span>
                    <strong>{money(data.resultSummary?.cost)}</strong>
                  </div>
                  <div>
                    <span>Resultado</span>
                    <strong>{money(data.resultSummary?.result)}</strong>
                  </div>
                </div>
              )}
              <section className="panel list-panel">
                <SectionHeader
                  title="Resultado por plantio"
                  subtitle="Quantidade produzida e vendida são indicadores distintos"
                />
                <div className="table-wrap">
                  <table>
                    <thead>
                      <tr>
                        <th>Plantio</th>
                        <th>Produzido</th>
                        <th>Vendido</th>
                        <th>Receita</th>
                        <th>Custo</th>
                        <th>Resultado</th>
                      </tr>
                    </thead>
                    <tbody>
                      {plants.map((row) => {
                        const result = resultadoPlantio(data, row.id)
                        return (
                          <tr key={row.id} data-testid={`result-row-${row.id}`}>
                            <td>
                              <strong>{rotuloPlantio(data, row)}</strong>
                            </td>
                            <td>
                              {number(quantidadeProduzida(data, row.id), 3)}{' '}
                              {byId(data.cultures, row.cultureId)?.unit}
                            </td>
                            <td>
                              {number(quantidadeVendida(data, row.id), 3)}{' '}
                              {byId(data.cultures, row.cultureId)?.unit}
                            </td>
                            <td>{money(result.revenue)}</td>
                            <td>{money(result.cost)}</td>
                            <td>
                              <strong
                                className={result.result < 0 ? 'negative' : ''}
                              >
                                {money(result.result)}
                              </strong>
                            </td>
                          </tr>
                        )
                      })}
                    </tbody>
                  </table>
                </div>
              </section>
            </>
          )}
        </>
      )}
    </>
  )
}
