import axios from 'axios'

const api = axios.create({
  baseURL: 'http://localhost:8080',
  timeout: 15000
})

export function getOverview() {
  return api.get('/api/migration/overview')
}

export function getPhaseStats() {
  return api.get('/api/migration/phase-stats')
}

export function getTrack() {
  return api.get('/api/migration/track')
}

export function getStopovers() {
  return api.get('/api/migration/stopovers')
}

export function getHourlyActivity() {
  return api.get('/api/migration/hourly-activity')
}

export function getScatterData() {
  return api.get('/api/migration/scatter')
}
