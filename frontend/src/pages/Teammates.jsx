import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { PageHeading, Panel, StatusBadge, EmptyState, Spinner, useToast } from '../components'
import { get, post } from '../api'
import { useAuth } from '../auth'

/** U2 / F5 — browse student profiles + skill-based suggestions for projects I manage. */
export default function Teammates() {
  const { user } = useAuth()
  const toast = useToast()
  const [students, setStudents] = useState(null)
  const [q, setQ] = useState('')
  const [skillId, setSkillId] = useState('')
  const [skills, setSkills] = useState([])
  const [myProjects, setMyProjects] = useState([])
  const [projSuggestions, setProjSuggestions] = useState({}) // projectId -> suggestions

  useEffect(() => {
    get('/api/skills').then(setSkills).catch(() => {})
    get('/api/projects?mine=true').then(setMyProjects).catch(() => {})
  }, [])

  useEffect(() => {
    const qs = new URLSearchParams()
    if (q) qs.set('q', q)
    if (skillId) qs.set('skillId', skillId)
    const timer = setTimeout(() => {
      get(`/api/students?${qs}`).then(setStudents).catch(() => setStudents([]))
    }, 250)
    return () => clearTimeout(timer)
  }, [q, skillId])

  const loadSuggestions = (projectId) => {
    post(`/api/projects/${projectId}/teammate-suggestions`, {})
      .then((list) => setProjSuggestions((s) => ({ ...s, [projectId]: list })))
      .catch((ex) => toast({ text: ex.message }))
  }

  const invite = async (projectId, userId, name) => {
    try {
      await post(`/api/projects/${projectId}/invitations`, { userId, message: '' })
      toast(`Invitation sent to ${name} ✉️`)
      loadSuggestions(projectId)
    } catch (ex) { toast({ text: ex.message }) }
  }

  return (
    <>
      <PageHeading title="Find teammates" lead="Browse students by skill and invite them to your projects (U2 / F5)." />

      <div className="panel mb-6 grid gap-3 md:grid-cols-3">
        <input className="field-input md:col-span-2" placeholder="Search by name, email or interest…"
               value={q} onChange={(e) => setQ(e.target.value)} />
        <select className="field-input" value={skillId} onChange={(e) => setSkillId(e.target.value)}>
          <option value="">Filter by skill</option>
          {skills.map(s => <option key={s.id} value={s.id}>{s.name}</option>)}
        </select>
      </div>

      {students === null ? <Spinner /> : (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {students.map(s => (
            <Link to={`/students/${s.userId}`} key={s.userId} className="panel block transition hover:-translate-y-px hover:shadow-card">
              <div className="flex items-center gap-3">
                <span className="grid h-11 w-11 place-items-center rounded-full bg-gradient-to-br from-soft to-[#dbe4ff] font-display font-extrabold text-brand">
                  {s.fullName.split(' ').map(w => w[0]).slice(0, 2).join('')}
                </span>
                <div>
                  <strong className="block text-ink">{s.fullName}</strong>
                  <span className="text-xs text-muted">{s.college || 'Student'} · {s.yearOfStudy ? `Year ${s.yearOfStudy}` : ''}</span>
                </div>
              </div>
              {s.bio && <p className="mt-3 line-clamp-2 text-sm text-muted">{s.bio}</p>}
              <div className="mt-3 flex flex-wrap gap-1.5">
                {(s.skills || []).slice(0, 4).map(t => (
                  <span key={t.skillId} className={`tag ${['ADVANCED', 'EXPERT'].includes(t.level) ? 'tag-teal' : ''}`}>{t.skillName} · {t.level.charAt(0)}{t.level.slice(1).toLowerCase()}</span>
                ))}
              </div>
              <div className="mt-3 text-xs text-muted">⏱ {({ ONE_5: '1–5', SIX_10: '6–10', ELEVEN_20: '11–20', TWENTY_PLUS: '20+' })[s.hoursPerWeek] || '?'} hrs/week available</div>
            </Link>
          ))}
          {students.length === 0 && <EmptyState text="No students match this search." />}
        </div>
      )}

      {myProjects.some(p => ['OWNER', 'MANAGER'].includes(p.myRole)) && (
        <Panel title="Suggested teammates for projects you manage" className="mt-8">
          <div className="grid gap-4 md:grid-cols-2">
            {myProjects.filter(p => ['OWNER', 'MANAGER'].includes(p.myRole)).map(p => (
              <div key={p.id} className="rounded-xl border border-line p-4">
                <div className="mb-2 flex items-center justify-between">
                  <strong className="text-ink">{p.title}</strong>
                  <button className="btn-ghost btn-sm" onClick={() => loadSuggestions(p.id)}>
                    {projSuggestions[p.id] ? 'Refresh' : 'Show suggestions'}
                  </button>
                </div>
                {!projSuggestions[p.id] && <p className="text-sm text-muted">Skill-matched students appear here.</p>}
                <div className="space-y-2">
                  {(projSuggestions[p.id] || []).slice(0, 4).map(s => (
                    <div key={s.userId} className="flex items-center justify-between gap-2 text-sm">
                      <div><strong>{s.fullName}</strong> <span className="badge badge-sky">{s.matchScore}</span>
                        <div className="text-xs text-muted">{s.reason}</div></div>
                      <button className="btn-primary btn-sm" onClick={() => invite(p.id, s.userId, s.fullName)}>Invite</button>
                    </div>
                  ))}
                </div>
              </div>
            ))}
          </div>
        </Panel>
      )}
    </>
  )
}
