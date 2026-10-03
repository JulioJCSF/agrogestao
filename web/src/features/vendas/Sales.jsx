import { useState } from 'react'
import { useOutletContext, useSearchParams } from 'react-router'
import {
  byId,
  dateText,
  money,
  number,
  plantingLabel,
  produced,
  saleTotal,
  sold,
  useData,
} from '../../contexts/DataContext.jsx'
import { usePerfil } from '../../contexts/AuthContext.jsx'
import {
  Action,
  PageHeading,
  SectionHeader,
  Status,
  Tabs,
} from '../../components/ui.jsx'

export function Sales() {
  const { data } = useData()
  const { openModal } = useOutletContext()
  const { canWrite } = usePerfil()
  const [params, setParams] = useSearchParams()
  const tab = params.get('tab') || 'vendas'
  const setTab = (value) => setParams({ tab: value })
  const [producerId, setProducerId] = useState('')
  const sales = data.sales.filter(
    (row) => !producerId || row.producerId === producerId,
  )
  const plantings = data.plantings.filter(
    (row) => !producerId || row.producerId === producerId,
  )
  return (
    <>
      <PageHeading
        eyebrow="Comercialização"
        title="Vendas"
        description="Registre compradores, acompanhe vendas e consulte o saldo dos plantios."
        action={
          canWrite && (
            <Action
              testId="register-sale"
              onClick={() => openModal(tab === 'clientes' ? 'client' : 'sale')}
            >
              {tab === 'clientes' ? 'Cadastrar cliente' : 'Registrar venda'}
            </Action>
          )
        }
      />
      <Tabs
        active={tab}
        setActive={setTab}
        tabs={[
          { key: 'vendas', label: 'Histórico de vendas' },
          { key: 'estoque', label: 'Estoque' },
          { key: 'clientes', label: 'Clientes' },
        ]}
      />
      {tab !== 'clientes' && (
        <div className="filter-row">
          <label>
            Produtor{' '}
            <select
              data-testid="sales-producer-filter"
              value={producerId}
              onChange={(event) => setProducerId(event.target.value)}
            >
              <option value="">Todos os produtores</option>
              {data.producers.map((row) => (
                <option value={row.id} key={row.id}>
                  {row.name}
                </option>
              ))}
            </select>
          </label>
        </div>
      )}
      {tab === 'vendas' && (
        <section className="panel list-panel">
          <SectionHeader
            title="Vendas registradas"
            subtitle={`${sales.length} operações no histórico`}
          />
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Data</th>
                  <th>Produtor</th>
                  <th>Comprador</th>
                  <th>Produtos</th>
                  <th>Total</th>
                </tr>
              </thead>
              <tbody>
                {sales.map((row) => (
                  <tr key={row.id} data-testid={`sale-row-${row.id}`}>
                    <td>{dateText(row.date)}</td>
                    <td>{byId(data.producers, row.producerId)?.name}</td>
                    <td>{byId(data.clients, row.clientId)?.name}</td>
                    <td>
                      <details>
                        <summary data-testid={`sale-items-${row.id}`}>
                          {row.items.length} item(ns)
                        </summary>
                        <div className="allocation-detail">
                          {row.items.map((item, index) => (
                            <div key={index}>
                              {plantingLabel(
                                data,
                                byId(data.plantings, item.plantingId),
                              )}
                              : {number(item.amount, 3)} {item.unit} ×{' '}
                              {money(item.unitPrice)}
                            </div>
                          ))}
                        </div>
                      </details>
                    </td>
                    <td>
                      <strong>{money(saleTotal(row))}</strong>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          {!sales.length && <Empty />}
        </section>
      )}
      {tab === 'estoque' && (
        <section className="panel list-panel">
          <SectionHeader
            title="Saldo informativo"
            subtitle="Saldo informado pelo serviço de estoque"
          />
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Cultura e plantio</th>
                  <th>Produtor</th>
                  <th>Produzido</th>
                  <th>Vendido</th>
                  <th>Disponível</th>
                </tr>
              </thead>
              <tbody>
                {plantings.map((row) => {
                  const balance = data.stock.find(
                    (item) => item.plantingId === row.id,
                  )?.available
                  return (
                    <tr key={row.id} data-testid={`stock-row-${row.id}`}>
                      <td>
                        <strong>{plantingLabel(data, row)}</strong>
                      </td>
                      <td>{byId(data.producers, row.producerId)?.name}</td>
                      <td>{number(produced(data, row.id), 3)}</td>
                      <td>{number(sold(data, row.id), 3)}</td>
                      <td>
                        <strong className={balance < 0 ? 'negative' : ''}>
                          {balance == null ? '—' : number(balance, 3)}{' '}
                          {byId(data.cultures, row.cultureId)?.unit}
                        </strong>
                      </td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </div>
          <p className="panel-note">
            Saldo informativo fornecido pela API. A validação da venda cabe ao
            backend.
          </p>
        </section>
      )}
      {tab === 'clientes' && (
        <section className="panel list-panel">
          <SectionHeader
            title="Clientes e compradores"
            subtitle="Canais de comercialização cadastrados"
          />
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Nome</th>
                  <th>Canal</th>
                  <th>Vendas</th>
                  <th>Total comprado</th>
                  <th />
                </tr>
              </thead>
              <tbody>
                {data.clients.map((row) => {
                  return (
                    <tr key={row.id} data-testid={`client-row-${row.id}`}>
                      <td>
                        <strong>{row.name}</strong>
                      </td>
                      <td>{row.channel}</td>
                      <td>{row.salesCount ?? '—'}</td>
                      <td>
                        {row.totalPurchased == null
                          ? '—'
                          : money(row.totalPurchased)}
                      </td>
                      <td>
                        {canWrite && (
                          <button
                            type="button"
                            className="text-button"
                            data-testid={`edit-client-${row.id}`}
                            onClick={() => openModal('client', row)}
                          >
                            Editar
                          </button>
                        )}
                      </td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </div>
        </section>
      )}
    </>
  )
}
