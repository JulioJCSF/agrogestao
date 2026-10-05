import { useState } from 'react'
import { useNavigate, useOutletContext, useParams } from 'react-router'
import { ArrowRight, CircleHelp, Home, Plus, Search } from 'lucide-react'
import { byId, number, useData } from '../../contexts/DataContext.jsx'
import { usePerfil } from '../../contexts/AuthContext.jsx'
import {
  Action,
  BackLink,
  Empty,
  PageHeading,
  Pagination,
  LinhaPlantio,
  LinhaRelatorio,
  SectionHeader,
  Status,
  NotFound,
} from '../../components/ui.jsx'

export function Produtores() {
  const { data, save, remove } = useData()
  const navigate = useNavigate()
  const { openModal, notify, confirm } = useOutletContext()
  const { canWrite } = usePerfil()
  const [query, setQuery] = useState('')
  const [page, setPage] = useState(1)
  const rows = data.producers.filter(
    (row) =>
      row.name.toLowerCase().includes(query.toLowerCase()) ||
      (row.cpf || '').includes(query.replace(/\D/g, '')),
  )
  return (
    <>
      <PageHeading
        eyebrow="Cadastro rural"
        title="Produtores"
        description="Consulte os agricultores atendidos e acompanhe suas propriedades."
        action={
          canWrite && (
            <Action testId="add-producer" onClick={() => openModal('producer')}>
              Cadastrar produtor
            </Action>
          )
        }
      />
      <div className="panel list-panel">
        <div className="list-toolbar">
          <label className="search-field">
            <Search size={18} />
            <input
              data-testid="filter-producers"
              value={query}
              onChange={(event) => {
                setQuery(event.target.value)
                setPage(1)
              }}
              placeholder="Buscar por nome ou CPF"
            />
          </label>
          <span className="count-label">{rows.length} produtores</span>
        </div>
        {rows.length ? (
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Produtor</th>
                  <th>Comunidade</th>
                  <th>Propriedades</th>
                  <th>Consentimento</th>
                  <th>Status</th>
                  <th />
                </tr>
              </thead>
              <tbody>
                {rows.slice((page - 1) * 10, page * 10).map((row) => (
                  <tr key={row.id} data-testid={`producer-row-${row.id}`}>
                    <td>
                      <button
                        type="button"
                        className="person-cell row-link"
                        data-testid={`open-producer-${row.id}`}
                        onClick={() => navigate(`/produtores/${row.id}`)}
                      >
                        <span className="small-avatar">
                          {row.name
                            .split(' ')
                            .map((part) => part[0])
                            .slice(0, 2)
                            .join('')}
                        </span>
                        <span>
                          <strong>{row.name}</strong>
                          <small>{row.cpf || 'CPF não informado'}</small>
                        </span>
                      </button>
                    </td>
                    <td>{row.community || '—'}</td>
                    <td>
                      {
                        data.properties.filter(
                          (item) => item.producerId === row.id,
                        ).length
                      }
                    </td>
                    <td>
                      <Status tone={row.consentAt ? 'green' : 'gold'}>
                        {row.consentAt ? 'Registrado' : 'Pendente'}
                      </Status>
                    </td>
                    <td>
                      <Status tone={row.status === 'Ativo' ? 'green' : 'gray'}>
                        {row.status}
                      </Status>
                    </td>
                    <td>
                      {canWrite && (
                        <div className="row-actions">
                          <button
                            type="button"
                            data-testid={`edit-producer-${row.id}`}
                            onClick={() => openModal('producer', row)}
                          >
                            Editar
                          </button>
                          <button
                            type="button"
                            data-testid={`toggle-producer-${row.id}`}
                            onClick={() =>
                              confirm({
                                title:
                                  row.status === 'Ativo'
                                    ? 'Inativar produtor?'
                                    : 'Reativar produtor?',
                                description: `${row.name} ${row.status === 'Ativo' ? 'sairá da lista de produtores ativos' : 'voltará a ficar ativo'}. O histórico será preservado.`,
                                action:
                                  row.status === 'Ativo'
                                    ? 'Inativar'
                                    : 'Reativar',
                                onConfirm: async () => {
                                  await save('producers', {
                                    ...row,
                                    status:
                                      row.status === 'Ativo'
                                        ? 'Inativo'
                                        : 'Ativo',
                                  })
                                  notify('Situação do produtor atualizada.')
                                },
                              })
                            }
                          >
                            {row.status === 'Ativo' ? 'Inativar' : 'Reativar'}
                          </button>
                          <button
                            type="button"
                            className="danger"
                            data-testid={`delete-producer-${row.id}`}
                            onClick={() =>
                              confirm({
                                title: 'Excluir produtor?',
                                description: `Excluir ${row.name} permanentemente? Produtores com plantios devem ser inativados para preservar o histórico.`,
                                action: 'Excluir',
                                onConfirm: async () => {
                                  await remove('producers', row.id)
                                  notify('Produtor excluído.')
                                },
                              })
                            }
                          >
                            Excluir
                          </button>
                        </div>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <Empty
            title="Nenhum produtor encontrado"
            detail="Experimente outro nome ou CPF."
          />
        )}
        <Pagination total={rows.length} page={page} onPageChange={setPage} />
      </div>
    </>
  )
}

export function DetalheProdutor() {
  const { id } = useParams()
  const { data } = useData()
  const { openModal } = useOutletContext()
  const { canWrite } = usePerfil()
  const producer = byId(data.producers, id)
  if (!producer) return <NotFound />
  const properties = data.properties.filter((row) => row.producerId === id)
  const plantings = data.plantings.filter((row) => row.producerId === id)
  const reports = data.reports.filter((row) => row.producerId === id)
  return (
    <>
      <BackLink to="/produtores" label="Voltar para produtores" />
      <PageHeading
        eyebrow={producer.status}
        title={producer.name}
        description={`${producer.community || 'Comunidade não informada'} · ${producer.phone || 'Telefone não informado'}`}
        action={
          canWrite && (
            <Action
              testId="edit-producer-detail"
              onClick={() => openModal('producer', producer)}
              icon={null}
              light
            >
              Editar dados
            </Action>
          )
        }
      />
      {!producer.consentAt && (
        <div className="notice-banner">
          <CircleHelp size={19} />
          <span>
            <strong>Consentimento pendente.</strong> Registre o consentimento
            antes de emitir relatórios deste produtor.
          </span>
          {canWrite && (
            <button
              type="button"
              data-testid="record-consent"
              onClick={() => openModal('producer', producer)}
            >
              Registrar agora
            </button>
          )}
        </div>
      )}
      <div className="detail-grid">
        <section className="panel">
          <SectionHeader
            title="Propriedades"
            subtitle={`${properties.length} cadastradas`}
            action={
              canWrite && (
                <button
                  type="button"
                  className="text-button"
                  data-testid="add-property"
                  onClick={() =>
                    openModal('property', null, { producerId: id })
                  }
                >
                  <Plus size={16} /> Adicionar
                </button>
              )
            }
          />
          {properties.length ? (
            <div className="stack-list">
              {properties.map((row) => (
                <div className="stack-row" key={row.id}>
                  <span className="list-icon">
                    <Home size={18} />
                  </span>
                  <span>
                    <strong>{row.name}</strong>
                    <small>
                      {row.location || 'Localização não informada'} ·{' '}
                      {row.area
                        ? `${number(row.area, 3)} ha`
                        : 'Área não informada'}
                    </small>
                  </span>
                  {canWrite && (
                    <button
                      type="button"
                      data-testid={`edit-property-${row.id}`}
                      onClick={() => openModal('property', row)}
                    >
                      Editar
                    </button>
                  )}
                </div>
              ))}
            </div>
          ) : (
            <Empty />
          )}
        </section>
        <section className="panel">
          <SectionHeader
            title="Plantios"
            subtitle={`${plantings.length} ciclos produtivos`}
            action={
              canWrite && (
                <button
                  type="button"
                  className="text-button"
                  data-testid="add-planting-producer"
                  onClick={() =>
                    openModal('planting', null, { producerId: id })
                  }
                >
                  <Plus size={16} /> Novo plantio
                </button>
              )
            }
          />
          {plantings.length ? (
            <div className="stack-list">
              {plantings.map((row) => (
                <LinhaPlantio key={row.id} row={row} data={data} />
              ))}
            </div>
          ) : (
            <Empty />
          )}
        </section>
      </div>
      <section className="panel">
        <SectionHeader
          title="Relatórios emitidos"
          subtitle="Documentos gerados para este produtor"
          action={
            canWrite && (
              <button
                type="button"
                className="text-button"
                data-testid="new-report-producer"
                disabled={!producer.consentAt}
                onClick={() => openModal('report', null, { producerId: id })}
              >
                <Plus size={16} /> Novo relatório
              </button>
            )
          }
        />
        {reports.length ? (
          <div className="stack-list">
            {reports.map((row) => (
              <LinhaRelatorio key={row.id} row={row} data={data} />
            ))}
          </div>
        ) : (
          <Empty
            title="Nenhum relatório emitido"
            detail="Escolha um período para emitir o primeiro relatório."
          />
        )}
      </section>
    </>
  )
}
