const formatadorHora = new Intl.DateTimeFormat('pt-BR', {
  hour: '2-digit',
  minute: '2-digit',
})

const formatadorDataHora = new Intl.DateTimeFormat('pt-BR', {
  day: '2-digit',
  month: '2-digit',
  year: 'numeric',
  hour: '2-digit',
  minute: '2-digit',
})

export function paraData(valorIso: string): Date {
  // O backend serializa LocalDateTime em UTC (sem sufixo). Adicionamos 'Z' quando não houver
  // timezone explícita para o navegador não interpretar como horário local.
  const temTimezone = /[zZ]|[+-]\d{2}:?\d{2}$/.test(valorIso)
  return new Date(temTimezone ? valorIso : `${valorIso}Z`)
}

export function formatarHora(valorIso: string): string {
  return formatadorHora.format(paraData(valorIso))
}

export function formatarDataHora(valorIso: string): string {
  return formatadorDataHora.format(paraData(valorIso))
}

export function formatarCronometro(segundosRestantes: number): string {
  const seguros = Math.max(0, Math.floor(segundosRestantes))
  const minutos = Math.floor(seguros / 60)
  const segundos = seguros % 60
  return `${String(minutos).padStart(2, '0')}:${String(segundos).padStart(2, '0')}`
}
