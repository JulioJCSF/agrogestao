import { useState } from 'react'
import {
  NavLink,
  Navigate,
  Outlet,
  Route,
  Routes,
  useLocation,
  useNavigate,
} from 'react-router'
import {
  ArrowRight,
  Bell,
  Check,
  ChevronDown,
  CircleHelp,
  FileText,
  Home,
  Leaf,
  LogOut,
  Menu,
  Search,
  Settings,
  Sprout,
  Store,
  Users,
  Wallet,
  X,
} from 'lucide-react'
import { RecordModal } from './components/RecordModal.jsx'
import { Empty, NotFound } from './components/ui.jsx'
import { useData } from './contexts/DataContext.jsx'
import { useAuth } from './contexts/AuthContext.jsx'
import { Dashboard } from './features/dashboard/Dashboard.jsx'
import { Producers, ProducerDetail } from './features/produtores/Producers.jsx'
import {
  ProductionPage,
  PlantingDetail,
} from './features/producao/Production.jsx'
import { Finance } from './features/financeiro/Finance.jsx'
import { Sales } from './features/vendas/Sales.jsx'
import { Reports, ReportDetail } from './features/relatorios/Reports.jsx'
import { Config } from './features/configuracoes/Config.jsx'
import { mockEnabled } from './services/records.js'

const navItems = [
  { to: '/', label: 'Visão geral', icon: Home },
  { to: '/produtores', label: 'Produtores', icon: Users },
  { to: '/producao?tab=plantios', label: 'Plantios', icon: Sprout },
  { section: 'LANÇAMENTOS' },
  { to: '/producao?tab=colheitas', label: 'Produção', icon: Sprout },
  { to: '/financeiro?tab=despesas', label: 'Despesas', icon: Wallet },
  { to: '/financeiro?tab=diarias', label: 'Diárias', icon: Wallet },
  { to: '/vendas', label: 'Vendas', icon: Store },
  { to: '/relatorios', label: 'Relatórios', icon: FileText },
]

function navIsActive(to, location) {
  const [path, query] = to.split('?')
  const pathMatches =
    location.pathname === path ||
    (path === '/produtores' && location.pathname.startsWith('/produtores/')) ||
    (path === '/producao' && location.pathname.startsWith('/plantios/'))
  if (!pathMatches) return false
  if (!query) return true
  const currentTab =
    new URLSearchParams(location.search).get('tab') ||
    (path === '/financeiro' ? 'despesas' : 'plantios')
  return currentTab === query.split('=')[1]
}

function AppLayout() {
  const { session, logout } = useAuth()
  const { loading: dataLoading, error: dataError } = useData()
  const navigate = useNavigate()
  const location = useLocation()
  const [modal, setModal] = useState(null)
  const [toast, setToast] = useState('')
  const [mobileOpen, setMobileOpen] = useState(false)
  const [profileOpen, setProfileOpen] = useState(false)
  const [helpOpen, setHelpOpen] = useState(false)
  const [searchOpen, setSearchOpen] = useState(false)
  const [pending, setPending] = useState(null)
  const [confirmError, setConfirmError] = useState('')
  const [confirmBusy, setConfirmBusy] = useState(false)
  const name = session.user?.nome || 'Operador'
  const initials = name
    .split(' ')
    .map((part) => part[0])
    .slice(0, 2)
    .join('')
    .toUpperCase()
  const firstName = name.split(' ')[0]
  const today = new Date().toLocaleDateString('pt-BR', {
    weekday: 'long',
    day: 'numeric',
    month: 'long',
    year: 'numeric',
  })
  const notify = (message) => {
    setToast(message)
    window.setTimeout(() => setToast(''), 4500)
  }
  const openModal = (type, item, defaults) => setModal({ type, item, defaults })
  const confirm = (request) => {
    setConfirmError('')
    setPending(request)
  }
  const runConfirm = async () => {
    setConfirmBusy(true)
    setConfirmError('')
    try {
      await pending.onConfirm()
      setPending(null)
    } catch (cause) {
      setConfirmError(cause.message)
    } finally {
      setConfirmBusy(false)
    }
  }

  return (
    <div className="app-shell">
      <aside className={`sidebar ${mobileOpen ? 'sidebar-open' : ''}`}>
        <NavLink
          to="/"
          className="brand"
          onClick={() => setMobileOpen(false)}
          data-testid="brand-home"
        >
          <span className="brand-mark">
            <Leaf size={24} />
          </span>
          <span>
            <strong>AgroGestão</strong>
            <small>SINDICATO RURAL</small>
          </span>
        </NavLink>
        <div className="sidebar-label">MENU PRINCIPAL</div>
        <nav aria-label="Menu principal">
          {navItems.map(({ to, label, icon: Icon, section }) =>
            section ? (
              <div key={section} className="sidebar-label nav-section">
                {section}
              </div>
            ) : (
              <NavLink
                end={to === '/'}
                key={to}
                to={to}
                data-testid={`nav-${to === '/' ? 'inicio' : to.replace(/^\//, '').replace('?tab=', '-')}`}
                onClick={() => setMobileOpen(false)}
                className={() =>
                  `nav-link ${navIsActive(to, location) ? 'active' : ''}`
                }
              >
                <Icon size={18} />
                <span>{label}</span>
              </NavLink>
            ),
          )}
        </nav>
        <div className="sidebar-spacer" />
        <button
          type="button"
          className="sidebar-settings"
          onClick={() => {
            navigate('/configuracoes')
            setMobileOpen(false)
          }}
          data-testid="nav-configuracoes"
        >
          <Settings size={18} /> Configurações
        </button>
        <div className="help-card">
          <span className="help-icon">
            <CircleHelp size={20} />
          </span>
          <strong>Precisa de ajuda?</strong>
          <p>Consulte o guia de operação do AgroGestão.</p>
          <button
            type="button"
            data-testid="open-help"
            onClick={() => setHelpOpen(true)}
          >
            Abrir guia <ArrowRight size={14} />
          </button>
        </div>
      </aside>
      {mobileOpen && (
        <button
          type="button"
          className="mobile-scrim"
          aria-label="Fechar menu"
          data-testid="close-menu"
          onClick={() => setMobileOpen(false)}
        />
      )}
      <div className="main-shell">
        <header className="topbar">
          <button
            type="button"
            className="icon-button mobile-menu"
            data-testid="open-menu"
            aria-label="Abrir menu"
            onClick={() => setMobileOpen(true)}
          >
            <Menu size={23} />
          </button>
          <div className="topbar-greeting">
            <span>{today.charAt(0).toUpperCase() + today.slice(1)}</span>
            <strong>Bom dia, {firstName}</strong>
          </div>
          <div className="topbar-actions">
            <button
              type="button"
              className="icon-button"
              aria-label="Pesquisar produtores"
              data-testid="global-search"
              onClick={() => setSearchOpen(true)}
            >
              <Search size={20} />
            </button>
            <button
              type="button"
              className="icon-button notification"
              aria-label="Notificações"
              data-testid="notifications"
              onClick={() => notify('Você não tem novas notificações.')}
            >
              <Bell size={20} />
              <i />
            </button>
            <span className="topbar-divider" />
            <div className="profile-wrap">
              <button
                type="button"
                className="profile-button"
                data-testid="profile-menu"
                onClick={() => setProfileOpen(!profileOpen)}
              >
                <span className="avatar">{initials}</span>
                <span className="profile-copy">
                  <strong>{name}</strong>
                  <small>
                    {session.user?.perfil === 'ADMIN'
                      ? 'Administrador'
                      : session.user?.perfil === 'CONSULTA'
                        ? 'Consulta'
                        : 'Operador'}
                  </small>
                </span>
                <ChevronDown size={17} />
              </button>
              {profileOpen && (
                <div className="profile-popover">
                  <button
                    type="button"
                    data-testid="profile-settings"
                    onClick={() => {
                      navigate('/configuracoes')
                      setProfileOpen(false)
                    }}
                  >
                    <Settings size={16} /> Configurações
                  </button>
                  <button
                    type="button"
                    data-testid="logout"
                    onClick={() => {
                      logout()
                      navigate('/login')
                    }}
                  >
                    <LogOut size={16} /> Sair
                  </button>
                </div>
              )}
            </div>
          </div>
        </header>
        {session.demo && (
          <div className="demo-banner">
            Prévia visual com dados fictícios. As operações de cadastro,
            alteração e exclusão dependem da API.
          </div>
        )}
        {dataLoading && (
          <div className="demo-banner" role="status">
            Carregando dados...
          </div>
        )}
        {dataError && (
          <div className="demo-banner error-banner" role="alert">
            {dataError}
          </div>
        )}
        <main className="content">
          <Outlet context={{ openModal, notify, confirm }} />
        </main>
      </div>
      {modal && (
        <RecordModal
          key={`${modal.type}-${modal.item?.id || 'new'}`}
          modal={modal}
          onClose={() => setModal(null)}
          notify={notify}
          openModal={openModal}
        />
      )}
      {toast && (
        <div className="toast" role="status">
          <Check size={17} />
          {toast}
          <button
            type="button"
            aria-label="Fechar aviso"
            data-testid="close-toast"
            onClick={() => setToast('')}
          >
            <X size={16} />
          </button>
        </div>
      )}
      {helpOpen && (
        <div
          className="modal-backdrop"
          role="presentation"
          onMouseDown={(event) => {
            if (event.target === event.currentTarget) setHelpOpen(false)
          }}
        >
          <section
            className="record-modal help-modal"
            role="dialog"
            aria-modal="true"
          >
            <div className="modal-heading">
              <div>
                <h2>Guia rápido</h2>
                <p>Comece pelos cadastros e siga o ciclo produtivo.</p>
              </div>
              <button
                type="button"
                className="icon-button"
                aria-label="Fechar"
                data-testid="close-help"
                onClick={() => setHelpOpen(false)}
              >
                <X size={20} />
              </button>
            </div>
            <ol>
              <li>
                Cadastre o produtor e registre o consentimento para relatórios.
              </li>
              <li>
                Adicione uma propriedade e um plantio na área de Produção.
              </li>
              <li>Registre colheitas, despesas, diárias e vendas.</li>
              <li>Confira os resultados e emita um relatório do período.</li>
            </ol>
          </section>
        </div>
      )}
      {searchOpen && <GlobalSearch onClose={() => setSearchOpen(false)} />}
      {pending && (
        <div className="modal-backdrop" role="presentation">
          <section
            className="record-modal confirm-modal"
            role="alertdialog"
            aria-modal="true"
            aria-labelledby="confirm-title"
          >
            <div className="modal-heading">
              <div>
                <h2 id="confirm-title">{pending.title}</h2>
                <p>{pending.description}</p>
              </div>
            </div>
            {confirmError && (
              <p className="form-error confirm-error" role="alert">
                {confirmError}
              </p>
            )}
            <div className="modal-actions">
              <button
                type="button"
                className="secondary-button"
                data-testid="cancel-confirm"
                onClick={() => setPending(null)}
              >
                Cancelar
              </button>
              <button
                type="button"
                className="primary-button"
                data-testid="accept-confirm"
                disabled={confirmBusy}
                onClick={runConfirm}
              >
                {confirmBusy ? 'Aguarde...' : pending.action}
              </button>
            </div>
          </section>
        </div>
      )}
    </div>
  )
}

function GlobalSearch({ onClose }) {
  const { data } = useData()
  const navigate = useNavigate()
  const [query, setQuery] = useState('')
  const matches = data.producers
    .filter(
      (row) =>
        row.name.toLowerCase().includes(query.toLowerCase()) ||
        (row.cpf || '').includes(query),
    )
    .slice(0, 6)
  return (
    <div
      className="modal-backdrop search-backdrop"
      role="presentation"
      onMouseDown={(event) => {
        if (event.target === event.currentTarget) onClose()
      }}
    >
      <section
        className="search-dialog"
        role="dialog"
        aria-modal="true"
        aria-label="Pesquisar"
      >
        <div className="search-box">
          <Search size={21} />
          <input
            autoFocus
            data-testid="search-input"
            placeholder="Buscar produtor pelo nome ou CPF"
            value={query}
            onChange={(event) => setQuery(event.target.value)}
          />
          <button
            type="button"
            data-testid="close-search"
            aria-label="Fechar"
            onClick={onClose}
          >
            <X size={18} />
          </button>
        </div>
        <div className="search-results">
          {matches.map((row) => (
            <button
              key={row.id}
              type="button"
              data-testid={`search-result-${row.id}`}
              onClick={() => {
                navigate(`/produtores/${row.id}`)
                onClose()
              }}
            >
              <span className="small-avatar">
                {row.name.slice(0, 2).toUpperCase()}
              </span>
              <span>
                <strong>{row.name}</strong>
                <small>{row.community}</small>
              </span>
              <ArrowRight size={16} />
            </button>
          ))}
          {!matches.length && <Empty title="Nenhum produtor encontrado" />}
        </div>
      </section>
    </div>
  )
}

function Login() {
  const { session, login, enterDemo } = useAuth()
  const navigate = useNavigate()
  const [loginName, setLoginName] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  if (session) return <Navigate to="/" replace />
  async function submit(event) {
    event.preventDefault()
    setLoading(true)
    setError('')
    try {
      await login(loginName, password)
      navigate('/')
    } catch (cause) {
      setError(cause.message)
    } finally {
      setLoading(false)
    }
  }
  return (
    <div className="login-page">
      <div className="login-panel">
        <div className="brand login-brand">
          <span className="brand-mark">
            <Leaf size={25} />
          </span>
          <span>
            <strong>AgroGestão</strong>
            <small>SINDICATO RURAL</small>
          </span>
        </div>
        <div className="login-intro">
          <span className="eyebrow">
            <span className="dot" />
            GESTÃO DA AGRICULTURA FAMILIAR
          </span>
          <h1>Bem-vindo de volta</h1>
          <p>
            Organize a produção, acompanhe os resultados e valorize o trabalho
            no campo.
          </p>
        </div>
        <form className="login-form" onSubmit={submit}>
          {mockEnabled && (
            <p className="notice">
              Modo prévia: use “Explorar prévia” para visualizar as telas com
              dados fictícios. O login requer a API.
            </p>
          )}
          <label className="field">
            <span>Login</span>
            <input
              data-testid="login-name"
              value={loginName}
              onChange={(event) => setLoginName(event.target.value)}
              required
              autoComplete="username"
              placeholder="Seu usuário"
            />
          </label>
          <label className="field">
            <span>Senha</span>
            <input
              data-testid="login-password"
              type="password"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              required
              autoComplete="current-password"
              placeholder="Sua senha"
            />
          </label>
          {error && (
            <p role="alert" className="form-error">
              {error}
            </p>
          )}
          <button
            type="submit"
            className="primary-button login-submit"
            data-testid="login-submit"
            disabled={loading}
          >
            {loading ? 'Entrando...' : 'Entrar no sistema'}{' '}
            <ArrowRight size={17} />
          </button>
        </form>
        <div className="demo-entry">
          <span>Quer conhecer as telas?</span>
          <button
            type="button"
            data-testid="enter-demo"
            onClick={() => {
              enterDemo()
              navigate('/')
            }}
          >
            Explorar prévia com dados fictícios
          </button>
        </div>
      </div>
      <div className="login-art">
        <div className="login-art-content">
          <span className="art-icon">
            <Sprout size={34} />
          </span>
          <h2>Da terra aos resultados.</h2>
          <p>Informações claras para apoiar cada etapa da produção rural.</p>
        </div>
      </div>
    </div>
  )
}

export function App() {
  const { session } = useAuth()
  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route
        element={session ? <AppLayout /> : <Navigate to="/login" replace />}
      >
        <Route index element={<Dashboard />} />
        <Route path="produtores" element={<Producers />} />
        <Route path="produtores/:id" element={<ProducerDetail />} />
        <Route path="producao" element={<ProductionPage />} />
        <Route path="plantios/:id" element={<PlantingDetail />} />
        <Route path="financeiro" element={<Finance />} />
        <Route path="vendas" element={<Sales />} />
        <Route path="relatorios" element={<Reports />} />
        <Route path="relatorios/:id" element={<ReportDetail />} />
        <Route path="configuracoes" element={<Config />} />
        <Route path="*" element={<NotFound />} />
      </Route>
    </Routes>
  )
}
