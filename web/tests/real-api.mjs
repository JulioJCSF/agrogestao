import { spawn, execFile } from 'node:child_process'
import { promisify } from 'node:util'
import { randomUUID } from 'node:crypto'
import { createWriteStream } from 'node:fs'
import { resolve } from 'node:path'
const exec = promisify(execFile)
const name = `agrogestao-sprint2-${randomUUID().slice(0, 8)}`
const password = randomUUID()
const apiPort = 18080
let apiProcess
try {
  await exec('docker', [
    'run',
    '--rm',
    '-d',
    '--name',
    name,
    '-e',
    `POSTGRES_PASSWORD=${password}`,
    '-p',
    '127.0.0.1::5432',
    'postgres:17-alpine',
  ])
  const { stdout } = await exec('docker', ['port', name, '5432'])
  const dbPort = stdout.trim().split(':').at(-1)
  const log = createWriteStream(resolve('../api/target/browser-api.log'))
  const env = {
    ...process.env,
    DB_HOST: '127.0.0.1',
    DB_PORT: dbPort,
    DB_NAME: 'postgres',
    DB_USER: 'postgres',
    DB_PASSWORD: password,
    DB_DRIVER_NAME: 'org.postgresql.Driver',
    DB_SCHEMA: 'public',
    ADMIN_LOGIN: 'admin-browser-sintetico',
    ADMIN_SENHA: password,
    JWT_SEGREDO: randomUUID() + randomUUID(),
    SPRING_CONFIG_IMPORT: '',
  }
  apiProcess = spawn(
    'java',
    [
      '-jar',
      resolve('../api/target/api-0.0.1-SNAPSHOT.jar'),
      `--server.port=${apiPort}`,
      '--spring.profiles.active=dev',
    ],
    { cwd: resolve('../api'), env, stdio: ['ignore', 'pipe', 'pipe'] },
  )
  apiProcess.stdout.pipe(log)
  apiProcess.stderr.pipe(log)
  let ready = false
  for (let attempt = 0; attempt < 90; attempt++) {
    if (apiProcess.exitCode !== null)
      throw new Error('A API encerrou. Consulte api/target/browser-api.log.')
    try {
      const response = await fetch(`http://127.0.0.1:${apiPort}/v3/api-docs`)
      if (response.ok) {
        ready = true
        break
      }
    } catch {
      // A API pode ainda estar aplicando as migrations.
    }
    await new Promise((done) => setTimeout(done, 1000))
  }
  if (!ready) throw new Error('A API não ficou pronta em 90 segundos.')
  const verifyEnv = {
    ...process.env,
    AGROGESTAO_API_REAL: 'true',
    AGROGESTAO_API_PROXY: `http://127.0.0.1:${apiPort}`,
    AGROGESTAO_TEST_LOGIN: env.ADMIN_LOGIN,
    AGROGESTAO_TEST_SENHA: password,
  }
  const tests = spawn(
    process.execPath,
    ['node_modules/@playwright/test/cli.js', 'test'],
    { env: verifyEnv, stdio: 'inherit' },
  )
  process.exitCode = await new Promise((done) =>
    tests.on('exit', (code) => done(code ?? 1)),
  )
} finally {
  if (apiProcess) {
    if (process.platform === 'win32') {
      // O java.exe do PATH pode ser um launcher que mantém outra JVM como filha.
      await exec('taskkill', [
        '/PID',
        String(apiProcess.pid),
        '/T',
        '/F',
      ]).catch(() => {})
    } else apiProcess.kill()
  }
  await exec('docker', ['stop', name]).catch(() => {})
}
