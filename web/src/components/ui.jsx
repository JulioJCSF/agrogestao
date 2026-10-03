import { Button } from '@heroui/react'
import { useNavigate } from 'react-router'
import {
  ArrowRight,
  ArrowUpRight,
  ClipboardList,
  FileText,
  Plus,
  Sprout,
} from 'lucide-react'
import {
  byId,
  dateText,
  number,
  plantingLabel,
} from '../contexts/DataContext.jsx'

export function Action({
  children,
  onClick,
  icon: Icon = Plus,
  testId,
  light = false,
}) {
  return (
    <Button
      className={light ? 'secondary-button action' : 'primary-button action'}
      onPress={onClick}
      data-testid={testId}
    >
      {Icon && <Icon size={16} strokeWidth={2.2} />}
      {children}
    </Button>
  )
}

export function Empty({
  title = 'Nenhum registro encontrado',
  detail = 'Cadastre uma informação para começar.',
}) {
  return (
    <div className="empty">
      <ClipboardList size={28} />
      <strong>{title}</strong>
      <span>{detail}</span>
    </div>
  )
}

export function Pagination({ total, page, size = 10, onPageChange }) {
  const pages = Math.max(1, Math.ceil(total / size))
  if (pages <= 1) return null
  return (
    <div className="pagination" aria-label="Paginação">
      <span>
        {Math.min((page - 1) * size + 1, total)}–{Math.min(page * size, total)}{' '}
        de {total}
      </span>
      <button
        type="button"
        data-testid="previous-page"
        disabled={page <= 1}
        onClick={() => onPageChange(page - 1)}
      >
        Anterior
      </button>
      <span>
        Página {page} de {pages}
      </span>
      <button
        type="button"
        data-testid="next-page"
        disabled={page >= pages}
        onClick={() => onPageChange(page + 1)}
      >
        Próxima
      </button>
    </div>
  )
}

export function SectionHeader({ title, subtitle, action }) {
  return (
    <div className="section-heading">
      <div>
        <h2>{title}</h2>
        {subtitle && <p>{subtitle}</p>}
      </div>
      {action}
    </div>
  )
}

export function PageHeading({ eyebrow, title, description, action }) {
  return (
    <div className="page-heading">
      <div>
        {eyebrow && (
          <span className="eyebrow">
            <span className="dot" />
            {eyebrow}
          </span>
        )}
        <h1>{title}</h1>
        <p>{description}</p>
      </div>
      {action}
    </div>
  )
}

export function Tabs({ tabs, active, setActive }) {
  return (
    <div className="tabs" role="tablist">
      {tabs.map((tab) => (
        <button
          key={tab.key}
          type="button"
          role="tab"
          aria-selected={active === tab.key}
          data-testid={`tab-${tab.key}`}
          className={active === tab.key ? 'active' : ''}
          onClick={() => setActive(tab.key)}
        >
          {tab.label}
        </button>
      ))}
    </div>
  )
}

export function Status({ children, tone = 'green' }) {
  return (
    <span className={`status status-${tone}`}>
      <span className="dot" />
      {children}
    </span>
  )
}

export function Metric({
  icon: Icon,
  label,
  value,
  detail,
  tone = 'green',
  onClick,
}) {
  return (
    <button
      type="button"
      className="metric-card"
      onClick={onClick}
      data-testid={`metric-${label.toLowerCase().replaceAll(' ', '-')}`}
    >
      <span className={`metric-icon ${tone}`}>
        <Icon size={20} />
      </span>
      <ArrowUpRight className="metric-arrow" size={17} />
      <span className="metric-label">{label}</span>
      <strong>{value}</strong>
      <span className="metric-detail">{detail}</span>
    </button>
  )
}

export function BackLink({ to, label }) {
  const navigate = useNavigate()
  return (
    <button
      type="button"
      className="back-link"
      data-testid="back-link"
      onClick={() => navigate(to)}
    >
      ← {label}
    </button>
  )
}

export function PlantingListRow({ row, data }) {
  const navigate = useNavigate()
  return (
    <button
      type="button"
      className="stack-row stack-link"
      data-testid={`open-planting-${row.id}`}
      onClick={() => navigate(`/plantios/${row.id}`)}
    >
      <span className="list-icon gold">
        <Sprout size={18} />
      </span>
      <span>
        <strong>{plantingLabel(data, row)}</strong>
        <small>
          {number(row.area, 3)} ha · Início {dateText(row.startedAt)}
        </small>
      </span>
      <Status tone={row.status === 'Em andamento' ? 'green' : 'gray'}>
        {row.status}
      </Status>
      <ArrowRight size={16} />
    </button>
  )
}

export function OpenPlantingButton({ id }) {
  const navigate = useNavigate()
  return (
    <button
      type="button"
      className="text-button"
      data-testid={`open-planting-${id}`}
      onClick={() => navigate(`/plantios/${id}`)}
    >
      Ver detalhes <ArrowRight size={14} />
    </button>
  )
}

export function ReportRow({ row, data }) {
  const navigate = useNavigate()
  return (
    <button
      type="button"
      className="stack-row stack-link"
      data-testid={`open-report-${row.id}`}
      onClick={() => navigate(`/relatorios/${row.id}`)}
    >
      <span className="list-icon blue">
        <FileText size={18} />
      </span>
      <span>
        <strong>
          {row.snapshot?.producer?.name ||
            byId(data.producers, row.producerId)?.name}
        </strong>
        <small>
          Período {dateText(row.from)} a {dateText(row.to)}
        </small>
      </span>
      <span className="muted">Emitido em {dateText(row.issuedAt)}</span>
      <ArrowRight size={16} />
    </button>
  )
}

export function NotFound() {
  const navigate = useNavigate()
  return (
    <div className="not-found">
      <Sprout size={40} />
      <h1>Página não encontrada</h1>
      <p>O endereço solicitado não existe ou o registro não está disponível.</p>
      <button
        type="button"
        className="primary-button"
        data-testid="go-home"
        onClick={() => navigate('/')}
      >
        Voltar para visão geral
      </button>
    </div>
  )
}
