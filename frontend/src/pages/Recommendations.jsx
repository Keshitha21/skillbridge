import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { PageHeading, StatusBadge, EmptyState, Spinner } from '../components'
import { get } from '../api'

/** U2 / F5 — published projects that match the logged-in student's skills. */
export default function Recommendations() {
  const [rows, setRows] = useState(null)

  useEffect(() => { get('/api/recommendations/projects').then(setRows).catch(() => setRows([])) }, [])

  if (rows === null) return <Spinner />

  return (
    <>
      <PageHeading title="Recommended for you" lead="Published projects that overlap with your skills (U2 / F5)." />
      {rows.length === 0
        ? <EmptyState icon="🎯" text="No matches yet — add more skills on your profile and new projects will surface here." />
        : <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
          {rows.map(p => (
            <Link to={`/projects/${p.id}`} key={p.id} className="panel block transition hover:-translate-y-px hover:shadow-card">
              <div className="mb-2 flex items-start justify-between gap-3">
                <h3 className="font-display font-bold text-ink">{p.title}</h3>
                <StatusBadge value={p.status} />
              </div>
              <p className="mb-3 line-clamp-2 text-sm text-muted">{p.description}</p>
              <div className="flex flex-wrap gap-1.5">
                {(p.skillNames || []).slice(0, 4).map(s => <span className="tag" key={s}>{s}</span>)}
              </div>
              <div className="mt-3 text-xs text-muted">👤 {p.ownerName} · {p.memberCount}/{p.teamSizeMax} members</div>
            </Link>
          ))}
        </div>}
    </>
  )
}
