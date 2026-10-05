import { useState } from 'react'
import { useOutletContext } from 'react-router'
import { dateText, money, useData } from '../../contexts/DataContext.jsx'
import { usePerfil } from '../../contexts/AuthContext.jsx'
import {
  Action,
  PageHeading,
  SectionHeader,
  Status,
  Tabs,
} from '../../components/ui.jsx'

export function Configuracoes() {
  const { data, save } = useData()
  const { openModal, notify, confirm } = useOutletContext()
  const { canWrite, isAdmin } = usePerfil()
  const [tab, setTab] = useState('culturas')
  return (
    <>
      <PageHeading
        eyebrow="Preferências do sistema"
        title="Configurações"
        description="Mantenha os catálogos usados nos cadastros e lançamentos."
        action={
          (canWrite || (tab === 'usuarios' && isAdmin)) && (
            <Action
              testId="add-setting"
              onClick={() =>
                openModal(
                  {
                    culturas: 'culture',
                    categorias: 'category',
                    referencias: 'reference',
                    usuarios: 'user',
                  }[tab],
                )
              }
            >
              {
                {
                  culturas: 'Nova cultura',
                  categorias: 'Nova categoria',
                  referencias: 'Novo valor',
                  usuarios: 'Novo usuário',
                }[tab]
              }
            </Action>
          )
        }
      />
      <Tabs
        active={tab}
        setActive={setTab}
        tabs={[
          { key: 'culturas', label: 'Culturas' },
          { key: 'categorias', label: 'Categorias de despesa' },
          { key: 'referencias', label: 'Valor da diária' },
          ...(isAdmin ? [{ key: 'usuarios', label: 'Usuários' }] : []),
        ]}
      />
      <section className="panel list-panel">
        {tab === 'culturas' && (
          <>
            <SectionHeader
              title="Catálogo de culturas"
              subtitle="Ciclo e unidade sugeridos no cadastro do plantio"
            />
            <div className="table-wrap">
              <table>
                <thead>
                  <tr>
                    <th>Cultura</th>
                    <th>Ciclo</th>
                    <th>Unidade</th>
                    <th />
                  </tr>
                </thead>
                <tbody>
                  {data.cultures.map((row) => (
                    <tr key={row.id} data-testid={`culture-row-${row.id}`}>
                      <td>
                        <strong>{row.name}</strong>
                      </td>
                      <td>{row.cycle}</td>
                      <td>{row.unit}</td>
                      <td>
                        {canWrite && (
                          <button
                            type="button"
                            className="text-button"
                            data-testid={`edit-culture-${row.id}`}
                            onClick={() => openModal('culture', row)}
                          >
                            Editar
                          </button>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </>
        )}
        {tab === 'categorias' && (
          <>
            <SectionHeader
              title="Categorias de despesa"
              subtitle="Categorias de sistema não podem ser editadas"
            />
            <div className="table-wrap">
              <table>
                <thead>
                  <tr>
                    <th>Categoria</th>
                    <th>Tipo</th>
                    <th />
                  </tr>
                </thead>
                <tbody>
                  {data.categories.map((row) => (
                    <tr key={row.id} data-testid={`category-row-${row.id}`}>
                      <td>
                        <strong>{row.name}</strong>
                      </td>
                      <td>
                        <Status tone={row.system ? 'blue' : 'green'}>
                          {row.system ? 'Sistema' : 'Personalizada'}
                        </Status>
                      </td>
                      <td>
                        {canWrite && !row.system && (
                          <button
                            type="button"
                            className="text-button"
                            data-testid={`edit-category-${row.id}`}
                            onClick={() => openModal('category', row)}
                          >
                            Editar
                          </button>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </>
        )}
        {tab === 'referencias' && (
          <>
            <SectionHeader
              title="Valores de referência da diária"
              subtitle="Valores e início da vigência"
            />
            <div className="table-wrap">
              <table>
                <thead>
                  <tr>
                    <th>Valor</th>
                    <th>Vigente a partir de</th>
                    <th />
                  </tr>
                </thead>
                <tbody>
                  {data.references.map((row) => (
                    <tr key={row.id} data-testid={`reference-row-${row.id}`}>
                      <td>
                        <strong>{money(row.value)}</strong>
                      </td>
                      <td>{dateText(row.startsAt)}</td>
                      <td>
                        {canWrite && (
                          <button
                            type="button"
                            className="text-button"
                            data-testid={`edit-reference-${row.id}`}
                            onClick={() => openModal('reference', row)}
                          >
                            Editar
                          </button>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </>
        )}
        {tab === 'usuarios' && isAdmin && (
          <>
            <SectionHeader
              title="Usuários do sistema"
              subtitle="Acesso institucional e permissões"
            />
            <div className="table-wrap">
              <table>
                <thead>
                  <tr>
                    <th>Nome</th>
                    <th>Login</th>
                    <th>Perfil</th>
                    <th>Situação</th>
                    <th />
                  </tr>
                </thead>
                <tbody>
                  {data.users.map((row) => (
                    <tr key={row.id} data-testid={`user-row-${row.id}`}>
                      <td>
                        <strong>{row.nome}</strong>
                      </td>
                      <td>{row.login}</td>
                      <td>{row.perfil}</td>
                      <td>
                        <Status tone={row.ativo ? 'green' : 'gray'}>
                          {row.ativo ? 'Ativo' : 'Inativo'}
                        </Status>
                      </td>
                      <td className="row-actions">
                        <button
                          type="button"
                          data-testid={`edit-user-${row.id}`}
                          onClick={() => openModal('user', row)}
                        >
                          Editar perfil
                        </button>
                        <button
                          type="button"
                          data-testid={`toggle-user-${row.id}`}
                          onClick={() =>
                            confirm({
                              title: row.ativo
                                ? 'Desativar usuário?'
                                : 'Reativar usuário?',
                              description: `${row.nome} ${row.ativo ? 'perderá o acesso' : 'voltará a ter acesso'} ao sistema.`,
                              action: row.ativo ? 'Desativar' : 'Reativar',
                              onConfirm: async () => {
                                await save('users', {
                                  ...row,
                                  ativo: !row.ativo,
                                })
                                notify('Situação do usuário atualizada.')
                              },
                            })
                          }
                        >
                          {row.ativo ? 'Desativar' : 'Reativar'}
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </>
        )}
      </section>
    </>
  )
}
