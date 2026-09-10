export function cleanName(name: string): string {
  return name.trim().replace(/\s+/g, ' ')
}

export function normalizeName(name: string): string {
  return cleanName(name).toLocaleLowerCase('pt-BR')
}
