import { useState } from 'react'
import { X, Plus, Trash2 } from 'lucide-react'
import { useNavigate } from 'react-router'
import { byId, plantingLabel, useData } from '../contexts/DataContext.jsx'

const today = () => new Date().toISOString().slice(0, 10)

const titles = {
  record: ['Novo registro', 'Adicione uma nova informação ao AgroGestão.'],
  producer: [
    'Cadastrar produtor',
    'Registre os dados básicos e o consentimento.',
  ],
  property: ['Cadastrar propriedade', 'Associe a propriedade a um produtor.'],
  culture: ['Cadastrar cultura', 'Configure o ciclo e a unidade de medida.'],
  planting: ['Novo plantio', 'Acompanhe um ciclo produtivo.'],
  production: [
    'Registrar produção',
    'Informe a quantidade colhida e a data do fato.',
  ],
  expense: [
    'Lançar movimentação',
    'Registre uma despesa e o rateio entre plantios.',
  ],
  daily: ['Registrar diária', 'Informe a natureza e o valor aplicado.'],
  client: ['Cadastrar cliente', 'Registre um comprador ou canal de venda.'],
  sale: ['Registrar venda', 'Adicione os produtos comercializados.'],
  report: [
    'Novo relatório',
    'Emita um documento a partir dos registros atuais.',
  ],
  category: ['Nova categoria', 'Organize os tipos de despesa.'],
  reference: ['Novo valor de referência', 'Defina a vigência da diária.'],
  user: [
    'Cadastrar usuário',
    'Defina quem pode acessar o sistema e seu perfil.',
  ],
  closePlanting: [
    'Encerrar plantio',
    'Informe a data de encerramento do ciclo produtivo.',
  ],
}

function formatCpf(value) {
  const digits = String(value || '')
    .replace(/\D/g, '')
    .slice(0, 11)
  return digits
    .replace(/^(\d{3})(\d)/, '$1.$2')
    .replace(/^(\d{3})\.(\d{3})(\d)/, '$1.$2.$3')
    .replace(/\.(\d{3})(\d)/, '.$1-$2')
}

function displayCurrency(value) {
  if (value === '' || value == null) return ''
  const [integer, fraction = ''] = String(value).replace(',', '.').split('.')
  const grouped = (integer.replace(/\D/g, '') || '0').replace(
    /\B(?=(\d{3})+(?!\d))/g,
    '.',
  )
  return `R$ ${grouped},${fraction.padEnd(2, '0').slice(0, 2)}`
}

function readCurrency(value) {
  const digits = value.replace(/\D/g, '')
  if (!digits) return ''
  return `${digits.slice(0, -2) || '0'}.${digits.slice(-2).padStart(2, '0')}`
}

function Field({
  name,
  label,
  value,
  onChange,
  type = 'text',
  required = false,
  options,
  hint,
  min,
  step,
  disabled,
  error,
}) {
  const id = `field-${name}`
  return (
    <label className="field" htmlFor={id}>
      <span>
        {label}
        {required && <b aria-hidden="true"> *</b>}
      </span>
      {options ? (
        <select
          id={id}
          data-testid={id}
          value={value ?? ''}
          onChange={(event) => onChange(event.target.value)}
          required={required}
          disabled={disabled}
          aria-invalid={Boolean(error)}
        >
          <option value="">Selecione</option>
          {options.map((option) => (
            <option key={option.value} value={option.value}>
              {option.label}
            </option>
          ))}
        </select>
      ) : (
        <input
          id={id}
          data-testid={id}
          type={type === 'currency' ? 'text' : type}
          lang={type === 'date' ? 'pt-BR' : undefined}
          inputMode={type === 'currency' ? 'decimal' : undefined}
          value={type === 'currency' ? displayCurrency(value) : (value ?? '')}
          onChange={(event) =>
            onChange(
              type === 'currency'
                ? readCurrency(event.target.value)
                : event.target.value,
            )
          }
          required={required}
          min={min}
          step={step}
          disabled={disabled}
          aria-invalid={Boolean(error)}
          placeholder={`Informe ${label.toLowerCase()}`}
        />
      )}
      {hint && <small>{hint}</small>}
      {error && (
        <small className="form-error" role="alert">
          {error}
        </small>
      )}
    </label>
  )
}

function initialValues(type, item, defaults, data) {
  if (type === 'closePlanting') return { ...item, endedAt: today() }
  if (item)
    return type === 'producer'
      ? { ...item, cpf: formatCpf(item.cpf) }
      : { ...item }
  const firstProducer = defaults.producerId || data.producers[0]?.id || ''
  const firstPlanting = defaults.plantingId || data.plantings[0]?.id || ''
  const openPlantings = data.plantings.filter(
    (row) => row.producerId === firstProducer && row.status === 'Em andamento',
  )
  const base = {
    record: { kind: '', description: '', date: today() },
    producer: {
      name: '',
      cpf: '',
      phone: '',
      community: '',
      consentAt: '',
      status: 'Ativo',
    },
    property: { producerId: firstProducer, name: '', area: '', location: '' },
    culture: { name: '', cycle: 'Curto', unit: 'kg' },
    planting: {
      producerId: firstProducer,
      propertyId: '',
      cultureId: '',
      cycle: 'Curto',
      area: '',
      startedAt: today(),
      expectedAt: '',
      status: 'Em andamento',
    },
    production: {
      plantingId: firstPlanting,
      amount: '',
      unit: 'kg',
      date: today(),
    },
    expense: {
      producerId: firstProducer,
      description: '',
      categoryId: '',
      amount: '',
      date: today(),
      plantingIds: defaults.plantingId ? [defaults.plantingId] : [],
    },
    daily: {
      plantingId: firstPlanting,
      nature: 'Paga',
      days: 1,
      unitValue: data.references.at(-1)?.value || '',
      date: today(),
    },
    client: { name: '', channel: 'Feira' },
    sale: {
      producerId: firstProducer,
      clientId: '',
      date: today(),
      items: [
        {
          plantingId: openPlantings.length === 1 ? openPlantings[0].id : '',
          amount: '',
          unit: 'kg',
          unitPrice: '',
        },
      ],
    },
    report: { producerId: firstProducer, from: '', to: today() },
    category: { name: '', system: false },
    reference: { value: '', startsAt: today() },
    user: { nome: '', login: '', perfil: 'OPERADOR', ativo: true },
  }
  return { ...base[type], ...defaults }
}

export function RecordModal({ modal, onClose, notify, openModal }) {
  const { data, save, closePlanting } = useData()
  const navigate = useNavigate()
  const { type, item, defaults = {} } = modal
  const [form, setForm] = useState(() =>
    initialValues(type, item, defaults, data),
  )
  const [error, setError] = useState('')
  const [fieldErrors, setFieldErrors] = useState({})
  const [saving, setSaving] = useState(false)
  const set = (key, value) =>
    setForm((current) => ({ ...current, [key]: value }))
  const producerOptions = data.producers
    .filter((row) => row.status === 'Ativo')
    .map((row) => ({ value: row.id, label: row.name }))
  const propertyOptions = data.properties
    .filter((row) => row.producerId === form.producerId)
    .map((row) => ({ value: row.id, label: row.name }))
  const plantingOptions = data.plantings
    .filter((row) =>
      type === 'production' || type === 'daily'
        ? true
        : row.producerId === form.producerId,
    )
    .map((row) => ({ value: row.id, label: plantingLabel(data, row) }))
  const title =
    item && type !== 'closePlanting'
      ? `Editar ${titles[type][0].toLowerCase().replace(/^(cadastrar|registrar|lançar|novo|nova) /, '')}`
      : titles[type][0]

  async function saveForm(event) {
    event.preventDefault()
    setError('')
    setFieldErrors({})
    setSaving(true)
    try {
      if (type === 'record') {
        const map = {
          Produtor: 'producer',
          Produção: 'production',
          Despesa: 'expense',
          Venda: 'sale',
        }
        onClose()
        openModal(map[form.kind] || 'producer')
        return
      }
      if (type === 'closePlanting') {
        await closePlanting(item.id, form.endedAt)
        onClose()
        notify('Plantio encerrado com sucesso.')
        return
      }
      const fields = {
        producer: ['name', 'cpf', 'phone', 'community', 'consentAt'],
        property: ['producerId', 'name', 'area', 'location'],
        culture: ['name', 'cycle', 'unit'],
        planting: [
          'producerId',
          'propertyId',
          'cultureId',
          'area',
          'startedAt',
          'expectedAt',
        ],
        production: ['plantingId', 'amount', 'unit', 'date'],
        expense: [
          'producerId',
          'description',
          'categoryId',
          'amount',
          'date',
          'plantingIds',
        ],
        daily: ['plantingId', 'nature', 'days', 'unitValue', 'date'],
        client: ['name', 'channel'],
        sale: ['producerId', 'clientId', 'date', 'items'],
        report: ['producerId', 'from', 'to'],
        category: ['name'],
        reference: ['value', 'startsAt'],
        user: ['nome', 'login', 'perfil'],
      }[type]
      const value = Object.fromEntries(fields.map((key) => [key, form[key]]))
      if (item?.id) value.id = item.id
      if (type === 'producer')
        value.cpf = String(value.cpf || '').replace(/\D/g, '')
      const collection = {
        producer: 'producers',
        property: 'properties',
        culture: 'cultures',
        planting: 'plantings',
        production: 'production',
        expense: 'expenses',
        daily: 'dailies',
        client: 'clients',
        sale: 'sales',
        report: 'reports',
        category: 'categories',
        reference: 'references',
        user: 'users',
      }[type]
      const saved = await save(collection, value)
      onClose()
      notify(`${item ? 'Alteração salva' : 'Registro adicionado'} com sucesso.`)
      if (type === 'report') navigate(`/relatorios/${saved.id}`)
    } catch (cause) {
      setError(cause.message)
      setFieldErrors(
        Object.fromEntries(
          (cause.campos || []).map(({ campo, mensagem }) => [campo, mensagem]),
        ),
      )
    } finally {
      setSaving(false)
    }
  }

  const option = (list, label = 'name') =>
    list.map((row) => ({ value: row.id, label: row[label] }))
  const field = (name, label, props = {}) => (
    <Field
      name={name}
      label={label}
      value={form[name]}
      onChange={(value) => set(name, value)}
      error={fieldErrors[name]}
      {...props}
    />
  )
  const boolField = (name, label) => (
    <label className="check-field">
      <input
        data-testid={`field-${name}`}
        type="checkbox"
        checked={Boolean(form[name])}
        onChange={(event) => set(name, event.target.checked ? today() : '')}
      />
      <span>{label}</span>
    </label>
  )

  return (
    <div
      className="modal-backdrop"
      role="presentation"
      onMouseDown={(event) => {
        if (event.target === event.currentTarget) onClose()
      }}
    >
      <section
        className="record-modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby="modal-title"
      >
        <div className="modal-heading">
          <div>
            <h2 id="modal-title">{title}</h2>
            <p>{titles[type][1]}</p>
          </div>
          <button
            className="icon-button"
            type="button"
            data-testid="close-modal"
            aria-label="Fechar"
            onClick={onClose}
          >
            <X size={21} />
          </button>
        </div>
        <form onSubmit={saveForm}>
          <div className="modal-fields">
            {error && (
              <p className="form-error" role="alert" data-testid="form-error">
                {error}
              </p>
            )}
            {type === 'record' && (
              <>
                {field('kind', 'Tipo de registro', {
                  options: ['Produtor', 'Produção', 'Despesa', 'Venda'].map(
                    (row) => ({ value: row, label: row }),
                  ),
                  required: true,
                })}
                {field('description', 'Descrição')}
                {field('date', 'Data', { type: 'date' })}
              </>
            )}
            {type === 'producer' && (
              <>
                {field('name', 'Nome completo', { required: true })}
                <Field
                  name="cpf"
                  label="CPF"
                  value={form.cpf}
                  onChange={(value) => set('cpf', formatCpf(value))}
                  hint="Opcional. Somente números são armazenados."
                />
                <div className="form-grid">
                  {field('phone', 'Telefone')}
                  {field('community', 'Comunidade')}
                </div>
                {boolField(
                  'consentAt',
                  'Consentimento registrado para emissão de relatório',
                )}
              </>
            )}
            {type === 'property' && (
              <>
                {field('producerId', 'Produtor', {
                  options: producerOptions,
                  required: true,
                })}
                {field('name', 'Nome da propriedade', { required: true })}
                <div className="form-grid">
                  {field('area', 'Área total (ha)', {
                    type: 'number',
                    min: '0',
                    step: '0.001',
                  })}
                  {field('location', 'Localização')}
                </div>
              </>
            )}
            {type === 'culture' && (
              <>
                {field('name', 'Nome da cultura', { required: true })}
                <div className="form-grid">
                  {field('cycle', 'Tipo de ciclo', {
                    options: [
                      { value: 'Curto', label: 'Curto' },
                      { value: 'Longo', label: 'Longo' },
                    ],
                  })}
                  {field('unit', 'Unidade de medida', {
                    options: [
                      { value: 'kg', label: 'kg' },
                      { value: 'un', label: 'un' },
                      { value: 'saca', label: 'saca' },
                    ],
                  })}
                </div>
              </>
            )}
            {type === 'planting' && (
              <>
                {field('producerId', 'Produtor', {
                  options: producerOptions,
                  required: true,
                })}
                {field('propertyId', 'Propriedade', {
                  options: propertyOptions,
                  required: true,
                })}
                <Field
                  name="cultureId"
                  label="Cultura"
                  value={form.cultureId}
                  onChange={(value) =>
                    setForm((current) => ({
                      ...current,
                      cultureId: value,
                      cycle: byId(data.cultures, value)?.cycle || 'Curto',
                    }))
                  }
                  options={option(data.cultures)}
                  required
                />
                <div className="form-grid">
                  {field('cycle', 'Ciclo', {
                    options: [
                      { value: 'Curto', label: 'Curto' },
                      { value: 'Longo', label: 'Longo' },
                    ],
                  })}
                  {field('area', 'Área utilizada (ha)', {
                    type: 'number',
                    min: '0.001',
                    step: '0.001',
                    required: true,
                  })}
                </div>
                <div className="form-grid">
                  {field('startedAt', 'Data do plantio', {
                    type: 'date',
                    required: true,
                  })}
                  {field('expectedAt', 'Previsão de colheita', {
                    type: 'date',
                  })}
                </div>
                {item && (
                  <p className="notice">
                    Alterar a área não modifica rateios de despesas já
                    registrados.
                  </p>
                )}
              </>
            )}
            {type === 'production' && (
              <>
                {field('plantingId', 'Plantio', {
                  options: plantingOptions,
                  required: true,
                })}
                <div className="form-grid">
                  {field('amount', 'Quantidade produzida', {
                    type: 'number',
                    min: '0.001',
                    step: '0.001',
                    required: true,
                  })}
                  {field('unit', 'Unidade', {
                    options: [
                      { value: 'kg', label: 'kg' },
                      { value: 'un', label: 'un' },
                      { value: 'saca', label: 'saca' },
                    ],
                  })}
                </div>
                {field('date', 'Data da produção', {
                  type: 'date',
                  required: true,
                })}
              </>
            )}
            {type === 'expense' && (
              <>
                {field('producerId', 'Produtor', {
                  options: producerOptions,
                  required: true,
                })}
                {field('description', 'Descrição', { required: true })}
                <div className="form-grid">
                  {field('categoryId', 'Categoria', {
                    options: option(
                      data.categories.filter((row) => !row.system),
                    ),
                    required: true,
                  })}
                  {field('amount', 'Valor (R$)', {
                    type: 'currency',
                    min: '0.01',
                    step: '0.01',
                    required: true,
                  })}
                </div>
                {field('date', 'Data da despesa', {
                  type: 'date',
                  required: true,
                })}
                <fieldset className="selection-field">
                  <legend>Plantios envolvidos *</legend>
                  {plantingOptions.map((plant) => (
                    <label key={plant.value} className="check-field">
                      <input
                        type="checkbox"
                        data-testid={`planting-option-${plant.value}`}
                        checked={
                          form.plantingIds?.includes(plant.value) || false
                        }
                        onChange={(event) =>
                          set(
                            'plantingIds',
                            event.target.checked
                              ? [...(form.plantingIds || []), plant.value]
                              : form.plantingIds.filter(
                                  (id) => id !== plant.value,
                                ),
                          )
                        }
                      />
                      {plant.label}
                    </label>
                  ))}
                  <small>
                    A API fará o rateio proporcionalmente à área de cada plantio
                    e registrará o percentual.
                  </small>
                </fieldset>
              </>
            )}
            {type === 'daily' && (
              <>
                {field('plantingId', 'Plantio', {
                  options: plantingOptions,
                  required: true,
                })}
                <div className="form-grid">
                  {field('nature', 'Natureza', {
                    options: [
                      { value: 'Paga', label: 'Paga' },
                      { value: 'Familiar', label: 'Familiar' },
                    ],
                  })}
                  {field('days', 'Quantidade de diárias', {
                    type: 'number',
                    min: '1',
                    step: '1',
                    required: true,
                  })}
                </div>
                {form.nature === 'Paga' &&
                  field('unitValue', 'Valor unitário (R$)', {
                    type: 'currency',
                    min: '0',
                    step: '0.01',
                    required: true,
                  })}
                {field('date', 'Data do trabalho', {
                  type: 'date',
                  required: true,
                })}
                <p className="notice">O valor total será calculado pela API.</p>
              </>
            )}
            {type === 'client' && (
              <>
                {field('name', 'Nome do cliente', { required: true })}
                {field('channel', 'Canal', {
                  options: [
                    { value: 'Feira', label: 'Feira' },
                    { value: 'Comércio local', label: 'Comércio local' },
                    { value: 'Venda direta', label: 'Venda direta' },
                    { value: 'Outros', label: 'Outros' },
                  ],
                })}
              </>
            )}
            {type === 'sale' && (
              <>
                <Field
                  name="producerId"
                  label="Produtor"
                  value={form.producerId}
                  onChange={(value) => {
                    const plants = data.plantings.filter(
                      (row) =>
                        row.producerId === value &&
                        row.status === 'Em andamento',
                    )
                    setForm((current) => ({
                      ...current,
                      producerId: value,
                      items: [
                        {
                          plantingId: plants.length === 1 ? plants[0].id : '',
                          amount: '',
                          unit: 'kg',
                          unitPrice: '',
                        },
                      ],
                    }))
                  }}
                  options={producerOptions}
                  required
                />
                {field('clientId', 'Cliente', {
                  options: option(data.clients),
                  required: true,
                })}
                {field('date', 'Data da venda', {
                  type: 'date',
                  required: true,
                })}
                <div className="section-divider">Itens da venda</div>
                {form.items.map((row, index) => {
                  const plant = byId(data.plantings, row.plantingId)
                  const available = plant
                    ? data.stock.find((entry) => entry.plantingId === plant.id)
                        ?.available
                    : null
                  const change = (key, value) =>
                    set(
                      'items',
                      form.items.map((entry, position) =>
                        position === index ? { ...entry, [key]: value } : entry,
                      ),
                    )
                  return (
                    <div className="sale-item" key={index}>
                      <div className="sale-item-title">
                        <strong>Item {index + 1}</strong>
                        {form.items.length > 1 && (
                          <button
                            type="button"
                            data-testid={`remove-sale-item-${index}`}
                            className="text-button danger"
                            onClick={() =>
                              set(
                                'items',
                                form.items.filter(
                                  (_, position) => position !== index,
                                ),
                              )
                            }
                          >
                            <Trash2 size={15} /> Remover
                          </button>
                        )}
                      </div>
                      <Field
                        name={`sale-planting-${index}`}
                        label="Plantio"
                        value={row.plantingId}
                        onChange={(value) => change('plantingId', value)}
                        options={plantingOptions.filter(
                          (optionRow) =>
                            byId(data.plantings, optionRow.value)?.status ===
                            'Em andamento',
                        )}
                        required
                      />
                      {available != null && (
                        <small>
                          Saldo informado pela API:{' '}
                          {available.toLocaleString('pt-BR')}{' '}
                          {byId(data.cultures, plant.cultureId)?.unit || 'kg'}
                        </small>
                      )}
                      <div className="form-grid three">
                        <Field
                          name={`sale-amount-${index}`}
                          label="Quantidade"
                          value={row.amount}
                          onChange={(value) => change('amount', value)}
                          type="number"
                          min="0.001"
                          step="0.001"
                          required
                        />
                        <Field
                          name={`sale-unit-${index}`}
                          label="Unidade"
                          value={row.unit}
                          onChange={(value) => change('unit', value)}
                          options={[
                            { value: 'kg', label: 'kg' },
                            { value: 'un', label: 'un' },
                            { value: 'saca', label: 'saca' },
                          ]}
                        />
                        <Field
                          name={`sale-price-${index}`}
                          label="Valor unitário"
                          value={row.unitPrice}
                          onChange={(value) => change('unitPrice', value)}
                          type="currency"
                          min="0"
                          step="0.01"
                          required
                        />
                      </div>
                    </div>
                  )
                })}
                <button
                  type="button"
                  data-testid="add-sale-item"
                  className="text-button"
                  onClick={() =>
                    set('items', [
                      ...form.items,
                      { plantingId: '', amount: '', unit: 'kg', unitPrice: '' },
                    ])
                  }
                >
                  <Plus size={16} /> Adicionar item
                </button>
                <p className="notice">
                  O total da venda será calculado pela API.
                </p>
              </>
            )}
            {type === 'report' && (
              <>
                {field('producerId', 'Produtor', {
                  options: producerOptions,
                  required: true,
                })}
                <div className="form-grid">
                  {field('from', 'Início do período', {
                    type: 'date',
                    required: true,
                  })}
                  {field('to', 'Fim do período', {
                    type: 'date',
                    required: true,
                  })}
                </div>
                <p className="notice">
                  O relatório guarda uma cópia dos dados no momento da emissão
                  para manter o histórico.
                </p>
              </>
            )}
            {type === 'category' &&
              field('name', 'Nome da categoria', { required: true })}
            {type === 'user' && (
              <>
                {field('nome', 'Nome completo', { required: true })}
                {field('login', 'Login', { required: true })}
                {field('perfil', 'Perfil', {
                  options: [
                    { value: 'ADMIN', label: 'Administrador' },
                    { value: 'OPERADOR', label: 'Operador' },
                    { value: 'CONSULTA', label: 'Consulta' },
                  ],
                  required: true,
                })}
                <p className="notice">
                  A credencial inicial e as permissões são definidas pela API.
                </p>
              </>
            )}
            {type === 'reference' && (
              <>
                {field('value', 'Valor da diária (R$)', {
                  type: 'currency',
                  min: '0.01',
                  step: '0.01',
                  required: true,
                })}
                {field('startsAt', 'Início da vigência', {
                  type: 'date',
                  required: true,
                })}
              </>
            )}
            {type === 'closePlanting' && (
              <>
                {field('endedAt', 'Data de encerramento', {
                  type: 'date',
                  required: true,
                })}
                <p className="notice">
                  Após o encerramento, novas produções não poderão ser
                  registradas neste plantio.
                </p>
              </>
            )}
          </div>
          <div className="modal-actions">
            <button
              type="button"
              className="secondary-button"
              data-testid="cancel-modal"
              onClick={onClose}
            >
              Cancelar
            </button>
            <button
              type="submit"
              className="primary-button"
              data-testid="save-modal"
              disabled={saving}
            >
              {saving
                ? 'Salvando...'
                : type === 'report'
                  ? 'Emitir relatório'
                  : item
                    ? 'Salvar alterações'
                    : 'Salvar registro'}
            </button>
          </div>
        </form>
      </section>
    </div>
  )
}
