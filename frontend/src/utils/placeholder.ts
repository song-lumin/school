const CATEGORY_COLORS: Record<string, string> = {
  coral: '#d88770',
  blue: '#7899a5',
  yellow: '#c3a668',
  green: '#81a78e'
}

const CATEGORY_ICON_PATHS: Record<string, string> = {
  coral: 'M7 18c-1.1 0-1.99.9-1.99 2S5.9 22 7 22s2-.9 2-2-.9-2-2-2zM1 2v2h2l3.6 7.59-1.35 2.45c-.16.28-.25.61-.25.96 0 1.1.9 2 2 2h12v-2H7.42c-.14 0-.25-.11-.25-.25l.03-.12.9-1.63h7.45c.75 0 1.41-.41 1.75-1.03l3.58-6.49c.08-.14.12-.31.12-.48 0-.55-.45-1-1-1H5.21l-.94-2H1zm16 16c-1.1 0-1.99.9-1.99 2s.89 2 1.99 2 2-.9 2-2-.9-2-2-2z',
  blue: 'M17 10.5V7c0-.55-.45-1-1-1H4c-.55 0-1 .45-1 1v10c0 .55.45 1 1 1h12c.55 0 1-.45 1-1v-3.5l4 4v-11l-4 4z',
  yellow: 'M18 2H6c-1.1 0-2 .9-2 2v16c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zM6 4h5v8l-2.5-1.5L6 12V4z',
  green: 'M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7zm0 9.5c-1.38 0-2.5-1.12-2.5-2.5s1.12-2.5 2.5-2.5 2.5 1.12 2.5 2.5-1.12 2.5-2.5 2.5z'
}

function categoryTone(category: string): 'coral' | 'blue' | 'yellow' | 'green' {
  if (category.includes('证件') || category.includes('卡')) return 'coral'
  if (category.includes('电子')) return 'blue'
  if (category.includes('书')) return 'yellow'
  return 'green'
}

export function categoryTile(category: string): string {
  const tone = categoryTone(category)
  const color = CATEGORY_COLORS[tone]
  const iconPath = CATEGORY_ICON_PATHS[tone]

  const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="200" height="150" viewBox="0 0 200 150"><rect width="200" height="150" fill="${color}"/><path d="${iconPath}" fill="rgba(255,255,255,0.85)" transform="translate(88, 63) scale(1.4)"/></svg>`
  return `data:image/svg+xml;charset=utf-8,${encodeURIComponent(svg)}`
}
