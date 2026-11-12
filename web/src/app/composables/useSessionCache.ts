// Estado de módulo nos features guarda o que já foi carregado para a navegação SPA ser
// instantânea. Ao encerrar a sessão esse cache precisa sumir: outro usuário no mesmo navegador
// não pode ver, nem por um instante, os dados do anterior.
const resetters = new Set<() => void>()

export const onSessionCacheReset = (reset: () => void) => {
  resetters.add(reset)
}

export const resetSessionCache = () => {
  for (const reset of resetters) reset()
}
