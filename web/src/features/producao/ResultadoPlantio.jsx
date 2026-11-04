import { useEffect, useState } from 'react'
import { dateText, money, number } from '../../contexts/DataContext.jsx'
import { consultarResultadoPlantio } from '../../services/registroService.js'
import { Aviso, Carregando } from '../../components/Feedback.jsx'
import { SectionHeader } from '../../components/ui.jsx'

export function ResultadoPlantio({ plantio }) {
  const [mes, setMes] = useState('')
  const [tentativa, setTentativa] = useState(0)
  const [consulta, setConsulta] = useState({ loading: true })
  useEffect(() => {
    let active = true
    setConsulta({ loading: true, mes })
    consultarResultadoPlantio(plantio.id, mes)
      .then((resultado) => {
        if (active) setConsulta({ resultado, mes })
      })
      .catch((cause) => {
        if (active) setConsulta({ error: cause.message, mes })
      })
    return () => {
      active = false
    }
  }, [plantio, mes, tentativa])
  const { resultado, error } = consulta
  const loading = consulta.loading || consulta.mes !== mes
  return (
    <section data-testid="planting-result-section">
      <SectionHeader
        title="Resultado"
        subtitle="Valores apurados pela API no período indicado"
      />
      {plantio.cycle === 'Curto' && (
        <label className="field">
          <span>Mês de apuração</span>
          <input
            type="month"
            data-testid="planting-result-month"
            value={mes || resultado?.periodoInicio.slice(0, 7) || ''}
            onChange={(event) => setMes(event.target.value)}
          />
        </label>
      )}
      {loading ? (
        <Carregando data-testid="planting-result-loading">
          Consultando resultado...
        </Carregando>
      ) : error ? (
        <>
          <Aviso tipo="erro" data-testid="planting-result-error">
            {error}
          </Aviso>
          <button
            type="button"
            className="secondary-button"
            data-testid="planting-result-retry"
            onClick={() => setTentativa((value) => value + 1)}
          >
            Tentar novamente
          </button>
        </>
      ) : (
        resultado && (
          <>
            <p>
              Período: {dateText(resultado.periodoInicio)} a{' '}
              {dateText(resultado.periodoFim)}
            </p>
            <div className="mini-metrics">
              <div>
                <span>Quantidade produzida</span>
                <strong data-testid="planting-produced">
                  {number(resultado.quantidadeProduzida, 3)}{' '}
                  {resultado.unidadeMedida}
                </strong>
              </div>
              <div>
                <span>Quantidade vendida</span>
                <strong data-testid="planting-sold">
                  {number(resultado.quantidadeVendida, 3)}{' '}
                  {resultado.unidadeMedida}
                </strong>
              </div>
              <div>
                <span>Receita</span>
                <strong>{money(resultado.receita)}</strong>
              </div>
              <div>
                <span>Custo de desembolso</span>
                <strong>{money(resultado.custoDesembolso)}</strong>
              </div>
              <div>
                <span>Resultado</span>
                <strong
                  data-testid="planting-result"
                  className={resultado.resultado < 0 ? 'negative' : ''}
                >
                  {money(resultado.resultado)}
                </strong>
              </div>
            </div>
            <p className="result-note">
              Informação complementar — Mão de obra familiar:{' '}
              {money(resultado.valorMaoObraFamiliar)}. Custo com mão de obra
              familiar: {money(resultado.custoComMaoObraFamiliar)}.
            </p>
          </>
        )
      )}
    </section>
  )
}
