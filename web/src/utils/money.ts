export function centToYuan(c?: number | null): string {
  if (c == null) return '0.00'
  return (c / 100).toFixed(2)
}

export function yuanToCent(y: number | string): number {
  return Math.round(Number(y) * 100)
}
