import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { PageHeading, Panel, StatusBadge, EmptyState, Spinner } from '../components'
import { useAuth } from '../auth'
import { get } from '../api'

export default function Dashboard() {
  const { user } = useAuth()
  const [myProjects, setMyProjects] = useState([])
  const [tasks, setTasks] = useState([])
  const [notifs, setNotifs] = useState([])
  const [requests, setRequests] = useState([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    Promise.allSettled([
      get('/api/projects?mine=true'),
      get('/api/tasks/mine'),
      get('/api/notifications'),
      get('/api/requests'),
    ]).then(([p, t, n, r]) => {
      if (p.status === 'fulfilled') setMyProjects(p.value)
      if (t.status === 'fulfilled') setTasks(t.value)
      if (n.status === 'fulfilled') setNotifs(n.value.items || [])
      if (r.status === 'fulfilled') setRequests(r.value)
      setLoading(false)
    })
  }, [])

  if (loading) return <Spinner />

  const live = tasks.filter(t => ['TODO', 'IN_PROGRESS', 'BLOCKED', 'REOPENED'].includes(t.status))
  const ready = tasks.filter(t => t.status === 'READY_FOR_REVIEW')
  const unread = notifs.filter(n => !n.read)
  const incomingPending = requests.filter(r => r.audienceName === 'INCOMING' && r.status === 'PENDING')

  const stat = (label, value, link, accent) => (
    <Link to={link} className="panel block transition hover:-translate-y-px hover:shadow-card">
      <p className="text-[11px] font-bold uppercase tracking-wider text-[#7a8599]">{label}</p>
      <p className={`mt-1 font-display text-3xl font-extrabold ${accent || 'text-ink'}`}>{value}</p>
    </Link>
  )

  return (
    <>
      <PageHeading title={`Hey ${user.fullName.split(' ')[0]} 👋`}
        lead="Here's what's happening on SkillBridge."
        action={<div className="flex gap-2">
          <Link className="btn-primary" to="/create-project">+ New project</Link>
          <Link className="btn-secondary" to="/teammates">Find teammates</Link>
        </div>} />

      <div className="mb-8 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        {stat('My projects', myProjects.length, '/projects?mine=true')}
        {stat('Active tasks', live.length, '/tasks', 'text-brand')}
        {stat('Awaiting review', ready.length, '/tasks', ready.length ? 'text-[#a96a16]' : 'text-ink')}
        {stat('Notifications', unread.length, '/notifications', unread.length ? 'text-[#c34d4d]' : 'text-ink')}
      </div>

      <div className="grid gap-5 lg:grid-cols-[1.4fr_1fr]">
        <Panel title="My projects">
          {myProjects.length === 0
            ? <EmptyState text="No projects yet — create your first one or explore open projects." />
            : (
              <div className="table-wrap overflow-x-auto">
                <table className="table-el">
                  <thead><tr><th>Project</th><th>Status</th><th>My role</th><th></th></tr></thead>
                  <tbody>
                    {myProjects.map(p => (
                      <tr key={p.id}>
                        <td><Link to={`/projects/${p.id}`} className="font-semibold">{p.title}</Link><span className="ml-2 text-xs text-muted">{p.category}</span></td>
                        <td><StatusBadge value={p.status} /></td>
                        <td><StatusBadge value={p.myRole || 'MEMBER'} /></td>
                        <td className="text-right"><Link className="btn-ghost btn-sm" to={`/projects/${p.id}`}>Open</Link></td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
        </Panel>

        <div className="grid gap-5">
          <Panel title="Task deadlines">
            {live.length === 0
              ? <EmptyState icon="✅" text="Nothing due — nice." />
              : <ul className="divide-y divide-line">
                {live.slice(0, 4).map(t => (
                  <li key={t.id} className="py-2 text-sm">
                    <Link to={`/tasks?project=${t.projectId}`} className="font-semibold no-underline">{t.title}</Link>
                    <div className="text-xs text-muted">{t.projectTitle}</div>
                  </li>
                ))}
              </ul>}
          </Panel>
          <Panel title="Latest notifications">
            {notifs.length === 0
              ? <EmptyState icon="🔔" text="Nothing new." />
              : <ul className="divide-y divide-line">
                {unread.slice(0, 4).map(n => (
                  <li key={n.id} className="py-2 text-sm">
                    <strong>{n.title}</strong>
                    <div className="text-xs text-muted">{n.body}</div>
                  </li>
                ))}
                {unread.length === 0 && <li className="py-2 text-sm text-muted">All caught up.</li>}
              </ul>}
          </Panel>
          {incomingPending.length > 0 && (
            <Panel title="Pending requests">
              <ul className="divide-y divide-line">
                {incomingPending.slice(0, 3).map(r => (
                  <li key={r.id} className="py-2 text-sm">
                    <Link to="/requests" className="font-semibold no-underline">{r.requesterName}</Link> wants to join{' '}
                    <span className="text-muted">{r.projectTitle}</span>
                  </li>
                ))}
              </ul>
              <Link className="btn-ghost btn-sm mt-2" to="/requests">Review requests →</Link>
            </Panel>
          )}
        </div>
      </div>
    </>
  )
}
