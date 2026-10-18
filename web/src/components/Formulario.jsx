import { createContext, useContext, useState } from 'react'
const FormContext = createContext({ erros: {}, mensagem: '', limpar: () => {} })
export function Formulario({
  onSubmit,
  aliases = {},
  children,
  'data-testid': testId = 'formulario',
  ...props
}) {
  const [erros, setErros] = useState({})
  const [mensagem, setMensagem] = useState('')
  async function submit(event) {
    event.preventDefault()
    setErros({})
    setMensagem('')
    try {
      await onSubmit(event)
    } catch (cause) {
      setMensagem(cause.message)
      setErros(
        Object.fromEntries(
          (cause.campos || []).map(({ campo, mensagem: text }) => [
            aliases[campo] || campo,
            text,
          ]),
        ),
      )
    }
  }
  return (
    <FormContext.Provider value={{ erros, mensagem }}>
      <form {...props} onSubmit={submit} data-testid={testId}>
        {mensagem && (
          <p
            className="form-error"
            role="alert"
            data-testid={`${testId}-error`}
          >
            {mensagem}
          </p>
        )}
        {children}
      </form>
    </FormContext.Provider>
  )
}
export function useErrosFormulario() {
  return useContext(FormContext)
}
