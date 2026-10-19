import {
  displayCurrency,
  readCurrency,
  displayDate,
  readDate,
  formatCpf,
} from './formularioValores.js'

export function Field({
  name,
  label,
  value,
  onChange,
  type = 'text',
  required = false,
  options,
  hint,
  error,
  min,
  max,
  step,
  disabled,
  'data-testid': testId,
  ...props
}) {
  const id = props.id || `field-${name}`
  const errorId = `${id}-error`
  const display =
    type === 'currency'
      ? displayCurrency(value)
      : type === 'date'
        ? displayDate(value)
        : type === 'cpf'
          ? formatCpf(value)
          : (value ?? '')
  function change(event) {
    const raw = event.target.value
    if (type === 'date') {
      const iso = readDate(raw)
      event.target.setCustomValidity(
        raw && (!iso || (min && iso < min) || (max && iso > max))
          ? 'Informe uma data válida no formato dd/mm/aaaa.'
          : '',
      )
      onChange(iso || raw)
    } else if (type === 'currency') onChange(readCurrency(raw))
    else if (type === 'cpf') onChange(raw.replace(/\D/g, '').slice(0, 11))
    else onChange(raw)
  }
  const common = {
    ...props,
    id,
    name,
    value: display,
    onChange: change,
    required,
    disabled,
    'data-testid': testId || id,
    'aria-invalid': Boolean(error),
    'aria-describedby': error ? errorId : undefined,
  }
  return (
    <label className="field" htmlFor={id}>
      <span>
        {label}
        {required && <b aria-hidden="true"> *</b>}
      </span>
      {options ? (
        <select {...common}>
          <option value="">Selecione</option>
          {options.map((option) => (
            <option key={option.value} value={option.value}>
              {option.label}
            </option>
          ))}
        </select>
      ) : (
        <input
          {...common}
          type={['currency', 'date', 'cpf'].includes(type) ? 'text' : type}
          inputMode={
            ['currency', 'cpf'].includes(type)
              ? 'numeric'
              : type === 'date'
                ? 'numeric'
                : undefined
          }
          min={type === 'number' ? min : undefined}
          max={type === 'number' ? max : undefined}
          step={step}
          placeholder={
            type === 'date'
              ? 'dd/mm/aaaa'
              : type === 'cpf'
                ? '000.000.000-00'
                : `Informe ${label.toLowerCase()}`
          }
        />
      )}
      {hint && <small>{hint}</small>}
      {error && (
        <small
          id={errorId}
          className="form-error"
          role="alert"
          data-testid={`${testId || id}-error`}
        >
          {error}
        </small>
      )}
    </label>
  )
}
export function CampoMoeda(props) {
  return <Field {...props} type="currency" />
}
export function CampoData(props) {
  return <Field {...props} type="date" />
}
export function CampoCpf(props) {
  return <Field {...props} type="cpf" />
}
