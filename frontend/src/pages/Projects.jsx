import { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { PageHeading, EmptyState, StatusBadge, Spinner, Field } from '../components'
import { get } from '../api'

export default function Projects() {
  const [params] = useSearchParams()
  const [rows, setRows] = useState(null)
  const [filters, setFilters] = useState({ q: '', category: '', skillId: '', status: '', mine: params.get('mine') === 'true' })
  const [skills, setSkills] = useState([])

  useEffect(() => { get('/api/skills').then(setSkills).catch(() => {}) }, [])

  useEffect(() => {
    const qs = new URLSearchParams()
    if (filters.q) qs.set('q', filters.q)
    if (filters.category) qs.set('category', filters.category)
    if (filters.skillId) qs.set('skillId', filters.skillId)
    if (filters.status) qs.set('status', filters.status)
    if (filters.mine) qs.set('mine', 'true')
    const timer = setTimeout(() => {
      get(`/api/projects?${qs}`).then(setRows).catch(() => setRows([]))
    }, 250)
    return () => clearTimeout(timer)
  }, [filters, params])

  return (
    <>
      <PageHeading title={filters.mine ? 'My projects' : 'Discover projects'}
        lead="Search by keyword, category or a required skill — U2 / F3."
        action={<Link className="btn-primary" to="/create-project">+ Create project</Link>} />

      <div className="panel mb-6 grid gap-3 md:grid-cols-4">
        <input className="field-input" placeholder="Search projects…" value={filters.q}
               onChange={(e) => setFilters({ ...filters, q: e.target.value })} />
        <select className="field-input" value={filters.category} onChange={(e) => setFilters({ ...filters, category: e.target.value })}>
          <option value="">All categories</option>
          <option>Web Development</option><option>Backend</option><option>Data & AI</option>
          <option>Mobile</option><option>Learning</option>
        </select>
        <select className="field-input" value={filters.skillId} onChange={(e) => setFilters({ ...filters, skillId: e.target.value })}>
          <option value="">Any required skill</option>
          {skills.map(s => <option key={s.id} value={s.id}>{s.name}</option>)}
        </select>
        <select className="field-input" value={filters.status} onChange={(e) => setFilters({ ...filters, status: e.target.value })}>
          <option value="">Active projects</option>
          <option value="PUBLISHED">Published only</option>
          <option value="COMPLETED">Completed</option>
          <option value="DRAFT">Include drafts</option>
          <option value="PAUSED">Paused</option>
        </select>
        <label className="flex items-center gap-2 text-sm font-semibold text-[#3b4860]">
          <input type="checkbox" className="h-4 w-4 accent-[#3659d9]" checked={filters.mine}
                 onChange={(e) => setFilters({ ...filters, mine: e.target.checked })} />
          Only projects I am part of
        </label>
      </div>

      {rows === null ? <Spinner /> : rows.length === 0
        ? <EmptyState text="No projects match these filters. Try different keywords." />
        : <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
          {rows.map(p => (
            <Link to={`/projects/${p.id}`} key={p.id} className="panel block transition hover:-translate-y-px hover:shadow-card">
              <div className="mb-2 flex items-start justify-between gap-3">
                <h3 className="font-display font-bold text-ink">{p.title}</h3>
                <StatusBadge value={p.status} />
              </div>
              <p className="mb-3 line-clamp-2 text-sm text-muted">{p.description}</p>
              <div className="flex flex-wrap gap-1.5">
                {(p.skillNames || []).slice(0, 3).map(s => <span className="tag" key={s}>{s}</span>)}
              </div>
              <div className="mt-3 flex items-center justify-between text-xs text-muted">
                <span>👤 {p.ownerName} · {p.memberCount}/{p.teamSizeMax} members</span>
                <span>{p.targetDeadline || 'no deadline'}</span>
              </div>
            </Link>
          ))}
        </div>}
    </>
  )
}
