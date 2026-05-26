import { createLogger, defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

/** Benign when backend restarts or the browser closes the WS during dev/HMR. */
function isBenignProxySocketError(err: NodeJS.ErrnoException): boolean {
  return err.code === 'ECONNRESET' || err.code === 'EPIPE' || err.code === 'ECONNABORTED'
}

function isBenignWsProxyLog(message: string): boolean {
  if (!message.includes('ws proxy socket error')) return false
  return (
    message.includes('ECONNABORTED') ||
    message.includes('ECONNRESET') ||
    message.includes('EPIPE')
  )
}

const logger = createLogger()
const logError = logger.error.bind(logger)
logger.error = (message, options) => {
  const text = typeof message === 'string' ? message : String(message)
  const err = options?.error as NodeJS.ErrnoException | undefined
  if (isBenignWsProxyLog(text) || (err && isBenignProxySocketError(err))) {
    return
  }
  logError(message, options)
}

export default defineConfig({
  customLogger: logger,
  plugins: [react()],
  server: {
    port: 3000,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      '/ws': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        ws: true,
        configure: (proxy) => {
          proxy.on('error', (err: NodeJS.ErrnoException) => {
            if (isBenignProxySocketError(err)) return
            console.error('[vite] ws proxy error:', err)
          })
        },
      },
    },
  },
})
