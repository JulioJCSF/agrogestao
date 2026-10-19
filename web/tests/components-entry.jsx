// Página de exemplo apenas para desenvolvimento e testes; não entra no build.
import { useState } from 'react'
import { createRoot } from 'react-dom/client'
import '../src/styles.css'
import {
  CampoMoeda,
  CampoData,
  CampoCpf,
} from '../src/components/CampoFormulario.jsx'
import {
  Formulario,
  useErrosFormulario,
} from '../src/components/Formulario.jsx'
import { Tabela } from '../src/components/Tabela.jsx'
import { Aviso, Confirmacao } from '../src/components/Feedback.jsx'
function Campos({ values, setValues }) {
  const { erros } = useErrosFormulario()
  const field = (name) => ({
    name,
    value: values[name],
    onChange: (value) =>
      setValues((current) => ({ ...current, [name]: value })),
    error: erros[name],
    required: true,
  })
  return (
    <>
      <CampoMoeda
        {...field('valor')}
        label="Valor"
        data-testid="example-money"
      />
      <CampoData
        {...field('dataFato')}
        label="Data do fato"
        data-testid="example-date"
      />
      <CampoCpf {...field('documento')} label="CPF" data-testid="example-cpf" />
    </>
  )
}
function Exemplos() {
  const [values, setValues] = useState({
    valor: '',
    dataFato: '',
    documento: '',
  })
  const [submitted, setSubmitted] = useState('')
  const [apiError, setApiError] = useState(false)
  const [busca, setBusca] = useState('')
  const [pagina, setPagina] = useState(0)
  const [pending, setPending] = useState(false)
  const [confirmed, setConfirmed] = useState(false)
  const [loading, setLoading] = useState(false)
  const rows = Array.from({ length: 25 }, (_, index) => ({
    id: index + 1,
    nome: `Produtor Fictício ${index + 1}`,
  })).filter((row) => row.nome.includes(busca))
  async function submit() {
    if (apiError) {
      const cause = new Error('Verifique os dados informados.')
      cause.campos = [{ campo: 'documento', mensagem: 'CPF inválido.' }]
      throw cause
    }
    setSubmitted(JSON.stringify(values))
  }
  return (
    <main style={{ padding: 40 }}>
      <h1>Exemplos dos componentes compartilhados</h1>
      <Formulario onSubmit={submit} data-testid="example-form">
        <Campos values={values} setValues={setValues} />
        <button type="submit" data-testid="example-submit">
          Enviar
        </button>
      </Formulario>
      <label>
        <input
          type="checkbox"
          data-testid="example-api-error"
          checked={apiError}
          onChange={(event) => setApiError(event.target.checked)}
        />
        Simular erro por campo
      </label>
      <output data-testid="example-submitted">{submitted}</output>
      <Tabela
        data-testid="example-table"
        pagina={{
          itens: rows.slice(pagina * 10, pagina * 10 + 10),
          pagina,
          tamanho: 10,
          total: rows.length,
        }}
        colunas={[{ campo: 'nome', titulo: 'Nome' }]}
        busca={busca}
        onBuscaChange={(value) => {
          setBusca(value)
          setPagina(0)
        }}
        onPageChange={setPagina}
        carregando={loading}
      />
      <button
        type="button"
        data-testid="example-loading"
        onClick={() => setLoading(!loading)}
      >
        Alternar carregamento
      </button>
      <Aviso data-testid="example-success">Cadastro salvo.</Aviso>
      <Aviso tipo="erro" data-testid="example-error">
        Operação recusada.
      </Aviso>
      <button
        type="button"
        data-testid="example-confirm"
        onClick={() => setPending(true)}
      >
        Excluir
      </button>
      {pending && (
        <Confirmacao
          data-testid="example-confirmation"
          title="Excluir registro?"
          description="Confirme a exclusão."
          onCancel={() => setPending(false)}
          onConfirm={() => {
            setConfirmed(true)
            setPending(false)
          }}
        />
      )}
      {confirmed && <output data-testid="example-confirmed">Confirmado</output>}
    </main>
  )
}
createRoot(document.getElementById('root')).render(<Exemplos />)
