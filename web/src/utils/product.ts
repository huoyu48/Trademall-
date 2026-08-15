/**
 * 商城商品视觉映射：分类 → emoji + 渐变封面色。
 * 无真实图片资源时，用分类 emoji 生成统一且精美的占位封面。
 */
export const CATEGORY_STYLE: Record<string, { emoji: string; gradient: string }> = {
  '数码电子': { emoji: '💻', gradient: 'linear-gradient(135deg,#6366f1,#a855f7)' },
  '手机配件': { emoji: '📱', gradient: 'linear-gradient(135deg,#0ea5e9,#06b6d4)' },
  '办公设备': { emoji: '🖨️', gradient: 'linear-gradient(135deg,#f59e0b,#f97316)' },
  '智能家居': { emoji: '🏠', gradient: 'linear-gradient(135deg,#10b981,#34d399)' },
  '影音娱乐': { emoji: '🎧', gradient: 'linear-gradient(135deg,#ef4444,#ec4899)' }
}

export function categoryStyle(categoryName?: string): { emoji: string; gradient: string } {
  return CATEGORY_STYLE[categoryName || ''] || { emoji: '🛒', gradient: 'linear-gradient(135deg,#64748b,#94a3b8)' }
}

/** 销量格式化：1w 以上转成 “1.2万” */
export function formatSales(sales?: number | null): string {
  if (sales == null) return '0'
  if (sales >= 10000) return (sales / 10000).toFixed(1) + '万'
  return String(sales)
}
