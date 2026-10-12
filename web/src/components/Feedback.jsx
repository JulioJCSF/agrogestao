import { useEffect, useId, useRef } from 'react'
export function Aviso({
  children,
  tipo = 'sucesso',
  onClose,
  'data-testid': testId = 'aviso',
  ...props
}) {
  return (
    <div
      {...props}
      className={tipo === 'erro' ? 'form-error' : 'notice-banner'}
      role={tipo === 'erro' ? 'alert' : 'status'}
      data-testid={testId}
    >
      {children}
      {onClose && (
        <button
          type="button"
          data-testid={`${testId}-fechar`}
          aria-label="Fechar aviso"
          onClick={onClose}
        >
          Fechar
        </button>
      )}
    </div>
  )
}
export function Carregando({
  children = 'Carregando dados...',
  'data-testid': testId = 'carregando',
  ...props
}) {
  return (
    <div {...props} role="status" aria-live="polite" data-testid={testId}>
      {children}
    </div>
  )
}
export function Confirmacao({
  title,
  description,
  action = 'Confirmar',
  busy = false,
  error,
  onConfirm,
  onCancel,
  'data-testid': testId = 'confirmacao',
}) {
  const heading = useId()
  const ref = useRef(null)
  useEffect(() => {
    const previous = document.activeElement
    ref.current?.querySelector('button')?.focus()
    return () => previous?.focus()
  }, [])
  function keyDown(event) {
    if (event.key === 'Escape' && !busy) onCancel()
    if (event.key !== 'Tab') return
    const buttons = [...ref.current.querySelectorAll('button:not(:disabled)')]
    if (!buttons.length) return
    const first = buttons[0],
      last = buttons.at(-1)
    if (event.shiftKey && document.activeElement === first) {
      event.preventDefault()
      last.focus()
    } else if (!event.shiftKey && document.activeElement === last) {
      event.preventDefault()
      first.focus()
    }
  }
  return (
    <div className="modal-backdrop" role="presentation">
      <section
        ref={ref}
        className="record-modal confirm-modal"
        role="alertdialog"
        aria-modal="true"
        aria-labelledby={heading}
        onKeyDown={keyDown}
        data-testid={testId}
      >
        <div className="modal-heading">
          <div>
            <h2 id={heading}>{title}</h2>
            <p>{description}</p>
          </div>
        </div>
        {error && (
          <Aviso tipo="erro" data-testid={`${testId}-erro`}>
            {error}
          </Aviso>
        )}
        <div className="modal-actions">
          <button
            type="button"
            className="secondary-button"
            data-testid="cancel-confirm"
            disabled={busy}
            onClick={onCancel}
          >
            Cancelar
          </button>
          <button
            type="button"
            className="primary-button"
            data-testid="accept-confirm"
            disabled={busy}
            onClick={onConfirm}
          >
            {busy ? 'Aguarde...' : action}
          </button>
        </div>
      </section>
    </div>
  )
}
