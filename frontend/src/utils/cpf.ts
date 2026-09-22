export function apenasDigitosCpf(valor: string): string {
  return valor.replace(/\D/g, '').slice(0, 11)
}

export function formatarCpf(valor: string): string {
  const digitos = apenasDigitosCpf(valor)
  const partes: string[] = []
  if (digitos.length > 0) partes.push(digitos.slice(0, 3))
  if (digitos.length > 3) partes.push(digitos.slice(3, 6))
  if (digitos.length > 6) partes.push(digitos.slice(6, 9))
  let saida = partes.join('.')
  if (digitos.length > 9) {
    saida += `-${digitos.slice(9, 11)}`
  }
  return saida
}

export function cpfCompleto(valor: string): boolean {
  return apenasDigitosCpf(valor).length === 11
}
