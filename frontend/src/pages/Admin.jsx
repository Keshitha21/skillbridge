import { useEffect, useState } from 'react'
import { PageHeading, Panel, StatusBadge, Field, EmptyState, useToast, Spinner } from '../components'
import { get, post, put, del } from '../api'

const TABS = ['Accounts', 'Skills', 'Reports', 'Audit log', 'Stats']

/** U9 / F15 — admin console: accounts, skill catalogue, report review, audit trail and stats. */
export default function Admin() {
  const toast = useToast()
  const [tab, setTab] = useState('Accounts')

  return (
    <>
      <PageHeading title="Admin console" lead="Moderation and platform management (U9 / F15)." />
      <div className="mb-6 flex flex-wrap gap-2">
        {TABS.map(t => (
          <button key={t} className={`${tab === t ? 'btn-primary' : 'btn-secondary'} btn-sm`} onClick={() => setTab(t)}>{t}</button>
        ))}
      </div>
      {tab === 'Accounts' && <Accounts toast={toast} />}
      {tab === 'Skills' && <SkillsManager toast={toast} />}
      {tab === 'Reports' && <Reports toast={toast} />}
      {tab === 'Audit log' && <AuditLog />}
      {tab === 'Stats' && <Stats />}
    </>
  )
}

/* ---------------- accounts ---------------- */
function Accounts({ toast }) {
  const [users, setUsers] = useState(null)
  const [q, setQ] = useState('')
  const [reason, setReason] = useState('Following the platform review policy.')

  const load = () => get(`/api/admin/users${q ? `?q=${encodeURIComponent(q)}` : ''}`).then(setUsers).catch(() => setUsers([]))
  useEffect(() => { load() }, [q])

  if (users === null) return <Spinner />

  const setStatus = async (u, status) => {
    try {
      await post(`/api/admin/users/${u.id}/status`, { status, reason })
      toast(`${u.fullName} is now ${status.toLowerCase()}`)
      load()
    } catch (ex) { toast({ text: ex.message }) }
  }

  return (
    <>
      <input className="field-input mb-4 max-w-sm" placeholder="Search users…" value={q} onChange={(e) => setQ(e.target.value)} />
      <Panel title={`Accounts (${users.length})`}>
        <div className="overflow-x-auto">
          <table className="table-el">
            <thead><tr><th>Name</th><th>Email</th><th>Role</th><th>Status</th><th></th></tr></thead>
            <tbody>
              {users.map(u => (
                <tr key={u.id}>
                  <td><strong>{u.fullName}</strong></td>
                  <td className="text-muted">{u.email}</td>
                  <td><StatusBadge value={u.role} /></td>
                  <td><StatusBadge value={u.status} /></td>
                  <td className="text-right">
                    {u.role !== 'ADMIN' && <>
                      {u.status === 'ACTIVE'
                        ? <button className="btn-danger btn-sm" onClick={() => setStatus(u, 'SUSPENDED')}>Suspend</button>
                        : <button className="btn-secondary btn-sm" onClick={() => setStatus(u, 'ACTIVE')}>Reactivate</button>}
                    </>}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        <Field label="Reason noted in the audit log">
          <input className="field-input max-w-md" value={reason} onChange={(e) => setReason(e.target.value)} />
        </Field>
      </Panel>
    </>
  )
}

/* ---------------- skill categories & skills ---------------- */
function SkillsManager({ toast }) {
  const [categories, setCategories] = useState([])
  const [skills, setSkills] = useState([])
  const [catForm, setCatForm] = useState({ name: '', description: '' })
  const [skillForm, setSkillForm] = useState({ name: '', description: '', categoryId: '' })

  const load = () => {
    Promise.allSettled([get('/api/admin/skill-categories'), get('/api/admin/skills')])
      .then(([c, s]) => {
        if (c.status === 'fulfilled') setCategories(c.value)
        if (s.status === 'fulfilled') setSkills(s.value)
      })
  }
  useEffect(() => { load() }, [])

  const addCategory = async (e) => {
    e.preventDefault()
    try { await post('/api/admin/skill-categories', catForm); setCatForm({ name: '', description: '' }); toast('Category added 🗂️'); load() }
    catch (ex) { toast({ text: ex.message }) }
  }
  const removeCategory = async (id) => {
    try { await del(`/api/admin/skill-categories/${id}`); toast('Category deleted'); load() }
    catch (ex) { toast({ text: ex.message }) }
  }
  const addSkill = async (e) => {
    e.preventDefault()
    try {
      await post('/api/admin/skills', { ...skillForm, categoryId: Number(skillForm.categoryId) })
      setSkillForm({ name: '', description: '', categoryId: '' }); toast('Skill added 🎓'); load()
    } catch (ex) { toast({ text: ex.message }) }
  }
  const removeSkill = async (id) => {
    try { await del(`/api/skills/${id}`); toast('Skill removed'); load() }
    catch (ex) { toast({ text: ex.message }) }
  }

  return (
    <div className="grid gap-5 lg:grid-cols-2">
      <Panel title="Skill categories">
        <ul className="mb-4 divide-y divide-line">
          {categories.map(c => (
            <li key={c.id} className="flex items-center justify-between py-2 text-sm">
              <span><strong className="text-ink">{c.name}</strong><div className="text-xs text-muted">{c.description}</div></span>
              <button className="btn-danger btn-sm" onClick={() => removeCategory(c.id)}>✕</button>
            </li>
          ))}
        </ul>
        <form onSubmit={addCategory} className="grid gap-2">
          <input className="field-input" placeholder="Category name" required value={catForm.name}
                 onChange={(e) => setCatForm({ ...catForm, name: e.target.value })} />
          <input className="field-input" placeholder="Description" value={catForm.description}
                 onChange={(e) => setCatForm({ ...catForm, description: e.target.value })} />
          <button className="btn-ghost">＋ Add category</button>
        </form>
      </Panel>

      <Panel title={`Skills in catalogue (${skills.length})`}>
        <ul className="mb-4 max-h-64 divide-y divide-line overflow-y-auto">
          {skills.map(s => (
            <li key={s.id} className="flex items-center justify-between py-2 text-sm">
              <span><strong className="text-ink">{s.name}</strong>
                <div className="text-xs text-muted">{s.categoryName || 'uncategorised'}{s.description ? ` · ${s.description}` : ''}</div></span>
              <button className="btn-danger btn-sm" onClick={() => removeSkill(s.id)}>✕</button>
            </li>
          ))}
        </ul>
        <form onSubmit={addSkill} className="grid gap-2">
          <input className="field-input" placeholder="Skill name" required value={skillForm.name}
                 onChange={(e) => setSkillForm({ ...skillForm, name: e.target.value })} />
          <select className="field-input" required value={skillForm.categoryId}
                  onChange={(e) => setSkillForm({ ...skillForm, categoryId: e.target.value })}>
            <option value="">Choose category…</option>
            {categories.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}
          </select>
          <button className="btn-ghost">＋ Add skill</button>
        </form>
      </Panel>
    </div>
  )
}

/* ---------------- report review ---------------- */
function Reports({ toast }) {
  const [reports, setReports] = useState(null)
  const [filter, setFilter] = useState('PENDING')

  const load = () => get(`/api/admin/reports${filter ? `?status=${filter}` : ''}`).then(setReports).catch(() => setReports([]))
  useEffect(() => { load() }, [filter])

  if (reports === null) return <Spinner />

  const review = async (report, status) => {
    const note = prompt('Admin note for this report:', '') || ''
    try {
      await post(`/api/admin/reports/${report.id}/review`, { status, adminNote: note })
      toast(`Report ${status.toLowerCase()}`)
      load()
    } catch (ex) { toast({ text: ex.message }) }
  }

  return (
    <>
      <div className="mb-4 flex gap-2">
        {['PENDING', 'RESOLVED', 'DISMISSED', ''].map(f => (
          <button key={f || 'all'} className={`${filter === f ? 'btn-primary' : 'btn-secondary'} btn-sm`} onClick={() => setFilter(f)}>
            {f || 'all'}
          </button>
        ))}
      </div>
      {reports.length === 0 ? <EmptyState icon="🛡️" text="No reports in this queue." />
        : <div className="grid gap-4 md:grid-cols-2">
          {reports.map(r => (
            <Panel key={r.id}>
              <div className="mb-2 flex items-center justify-between">
                <strong className="text-ink">{r.contentType}{r.contentId ? ` #${r.contentId}` : ''}</strong>
                <StatusBadge value={r.status} />
              </div>
              <p className="text-sm"><strong>Reason:</strong> {r.reason}</p>
              {r.description && <p className="text-sm text-muted">“{r.description}”</p>}
              <p className="mt-1 text-xs text-muted">
                reported by {r.reporterName || `user #${r.reporterId || '?'}`}{r.reportedUserName ? ` · about ${r.reportedUserName}` : ''}
              </p>
              {r.adminNote && <p className="mt-1 rounded-lg bg-soft px-2 py-1 text-xs">Admin: {r.adminNote}</p>}
              {r.status === 'PENDING' && (
                <div className="mt-3 flex gap-2">
                  <button className="btn-primary btn-sm" onClick={() => review(r, 'RESOLVED')}>Resolve</button>
                  <button className="btn-secondary btn-sm" onClick={() => review(r, 'DISMISSED')}>Dismiss</button>
                </div>
              )}
            </Panel>
          ))}
        </div>}
    </>
  )
}

/* ---------------- audit log ---------------- */
function AuditLog() {
  const [log, setLog] = useState(null)
  useEffect(() => { get('/api/admin/audit-log').then(setLog).catch(() => setLog([])) }, [])
  if (log === null) return <Spinner />
  return (
    <Panel title="Admin audit trail (most recent 100)">
      {log.length === 0 ? <EmptyState icon="📜" text="No admin actions recorded yet." />
        : <ul className="divide-y divide-line text-sm">
          {log.map(a => (
            <li key={a.id} className="py-2">
              <strong className="text-brand">{a.action.replace(/_/g, ' ')}</strong> — {a.entityType}
              {a.entityId ? ` #${a.entityId}` : ''} · {a.adminName || ''}
              <div className="text-xs text-muted">{a.reason} · {new Date(a.createdAt).toLocaleString()}</div>
            </li>
          ))}
        </ul>}
    </Panel>
  )
}

/* ---------------- platform stats ---------------- */
function Stats() {
  const [stats, setStats] = useState(null)
  useEffect(() => { get('/api/admin/stats').then(setStats).catch(() => {}) }, [])
  if (!stats) return <Spinner />
  const card = (label, value, accent = '') => (
    <div className="panel text-center">
      <p className="font-display text-3xl font-extrabold text-ink">{value}</p>
      <p className="text-[11px] font-bold uppercase tracking-wide text-[#7a8599]">{label}</p>
    </div>
  )
  return (
    <div className="grid grid-cols-2 gap-4 sm:grid-cols-4">
      {card('Registered users', stats.users, 'text-brand')}
      {card('Active users', stats.activeUsers, 'text-[#17845c]')}
      {card('Projects', stats.projects)}
      {card('Published', stats.publishedProjects, 'text-[#17845c]')}
      {card('Tasks', stats.tasks)}
      {card('Open reports', stats.openReports, stats.openReports ? 'text-[#c34d4d]' : 'text-ink')}
      {card('Mentorship offers', stats.mentorshipOffers)}
    </div>
  )
}
