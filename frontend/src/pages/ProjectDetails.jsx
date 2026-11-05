import { useEffect, useState } from 'react'
import { Link, useParams, useNavigate } from 'react-router-dom'
import { PageHeading, Panel, StatusBadge, EmptyState, Spinner, useToast, AvatarStack, Field } from '../components'
import { get, post, put, del } from '../api'

const TABS = ['Overview', 'Team', 'Chat', 'Files', 'Tasks', 'Progress']

export default function ProjectDetails() {
  const { id } = useParams()
  const navigate = useNavigate()
  const toast = useToast()
  const [tab, setTab] = useState('Overview')
  const [data, setData] = useState(null)
  const [error, setError] = useState('')
  const [chatBody, setChatBody] = useState('')
  const [joinMessage, setJoinMessage] = useState('')
  const [inviteEmail, setInviteEmail] = useState('')
  const [uploadTaskId, setUploadTaskId] = useState('')
  const [tasks, setTasks] = useState([])

  const load = () => {
    get(`/api/projects/${id}`).then((d) => { setData(d); setError('') })
      .catch((ex) => setError(ex.message))
    get(`/api/projects/${id}/tasks`).then(setTasks).catch(() => {})
  }
  useEffect(() => { load() }, [id])

  if (error) return <EmptyState icon="🔒" text={error} />
  if (!data) return <Spinner />

  const p = data.summary
  const isMember = !!p.myRole
  const isOwner = p.myRole === 'OWNER'
  const canManage = ['OWNER', 'MANAGER'].includes(p.myRole)

  const act = async (fn, message) => {
    try { await fn(); toast(message); load() } catch (ex) { toast({ text: ex.message }) }
  }
  const lifecycle = (endpoint, message) => act(() => post(`/api/projects/${id}/${endpoint}`), message)

  return (
    <>
      <PageHeading title={p.title}
        lead={`${p.ownerName} · ${p.category || 'General'} · deadline ${p.targetDeadline || 'not set'}`}
        action={<div className="flex flex-wrap items-center gap-2">
          <StatusBadge value={p.status} />
          {isOwner && <>
            <Link className="btn-secondary btn-sm" to={`/edit-project/${id}`}>Edit</Link>
            {p.status === 'DRAFT' && <button className="btn-primary btn-sm" onClick={() => lifecycle('publish', 'Project published 📣')}>Publish</button>}
            {p.status === 'PUBLISHED' && <button className="btn-secondary btn-sm" onClick={() => lifecycle('pause', 'Project paused')}>Pause</button>}
            {p.status !== 'COMPLETED' && p.status !== 'CANCELLED' &&
              <button className="btn-secondary btn-sm" onClick={() => lifecycle('complete', 'Project marked complete 🏁')}>Mark complete</button>}
            {p.status !== 'CANCELLED' && p.status !== 'COMPLETED' &&
              <button className="btn-danger btn-sm" onClick={() => lifecycle('cancel', 'Project cancelled')}>Cancel project</button>}
          </>}
          {!isMember && (
            <div className={`panel p-4 ${p.status === 'PUBLISHED' ? '' : 'pointer-events-none opacity-40'}`}>
              <input className="field-input mb-2" placeholder="Message to the owner (optional)"
                     value={joinMessage} onChange={(e) => setJoinMessage(e.target.value)} />
              <button className="btn-primary btn-sm w-full"
                      onClick={() => act(() => post(`/api/projects/${id}/join-requests`, { message: joinMessage }), 'Request sent ✉️')}
                      disabled={p.status !== 'PUBLISHED'}>Request to join</button>
            </div>
          )}
        </div>} />

      <div className="mb-6 flex flex-wrap gap-2">
        {TABS.map(t => (
          <button key={t} className={`${tab === t ? 'btn-primary' : 'btn-secondary'} btn-sm`} onClick={() => setTab(t)}>
            {t}{t === 'Tasks' && tasks.length ? ` (${tasks.length})` : ''}
          </button>
        ))}
      </div>
      {tab === 'Overview' && <Overview data={data} isPublic={!isMember} />}
      {tab === 'Team' && <Team data={data} isOwner={isOwner} canManage={canManage} id={id} load={load} act={act} />}
      {tab === 'Chat' && <Chat id={id} data={data} isMember={isMember} />}
      {tab === 'Files' && <Files id={id} isMember={isMember} tasks={tasks} load={load} uploadTaskId={uploadTaskId} setUploadTaskId={setUploadTaskId} />}
      {tab === 'Tasks' && <TasksTab tasks={tasks} navigate={navigate} />}
      {tab === 'Progress' && <ProgressTab data={data} />}
      {data.pendingRequests?.length > 0 && canManage && tab !== 'Overview' && (
        <Panel title="Join requests waiting for a decision" className="mt-6">
          <ul className="divide-y divide-line text-sm">
            {data.pendingRequests.map(r => (
              <li key={r.requestId} className="flex items-center justify-between gap-3 py-2">
                <span><strong>{r.requesterName}</strong> — “{r.message}”</span>
                <Link className="btn-ghost btn-sm" to="/requests">Review →</Link>
              </li>
            ))}
          </ul>
        </Panel>
      )}
    </>
  )
}

function Overview({ data, isPublic }) {
  const p = data.summary
  return (
    <div className="grid gap-5 lg:grid-cols-[1.5fr_1fr]">
      <Panel title="About this project">
        <p className="mb-4 whitespace-pre-line text-sm text-muted">{p.description}</p>
        {data.goals && isPublic === false && <>
          <h3 className="panel-title mb-2 text-base">Project goals</h3>
          <p className="whitespace-pre-line text-sm text-muted">{data.goals}</p>
        </>}
        {data.goals && isPublic && <p className="mt-3 rounded-xl bg-soft p-3 text-xs">🔒 Goals, team and workspace are only visible to approved members. {data.openRoles.length > 0 && `${data.openRoles.length} open role(s) available.`}</p>}
      </Panel>
      <div className="grid gap-5">
        <Panel title="Skills needed">
          <div className="flex flex-wrap gap-1.5">
            {data.skills.length
              ? data.skills.map(s => (
                <span key={s.skillId} className="tag">{s.name} · <span className="font-normal">{s.requiredLevel}</span></span>
              ))
              : <span className="text-sm text-muted">No specific skills listed.</span>}
          </div>
        </Panel>
        <Panel title="Open roles">
          {data.openRoles.length
            ? <ul className="space-y-2 text-sm">{data.openRoles.map(r => (
              <li key={r.roleId} className="flex justify-between rounded-lg bg-soft px-3 py-2"><strong>{r.title}</strong><span className="text-muted">{r.slots - r.filled} of {r.slots} open</span></li>
            ))}</ul>
            : <EmptyState icon="🚫" text="No open roles right now." />}
        </Panel>
        <Panel title="Milestones">
          {data.milestones?.length
            ? <ul className="space-y-2 text-sm">{data.milestones.map(m => (
              <li key={m.id} className="flex items-center justify-between"><span>{m.title}</span><StatusBadge value={m.status} /></li>
            ))}</ul>
            : <span className="text-sm text-muted">No milestones planned yet.</span>}
        </Panel>
      </div>
    </div>
  )
}

function Team({ data, isOwner, canManage, id, load, act }) {
  const [inviteUserId, setInviteUserId] = useState('')
  const [suggestions, setSuggestions] = useState([])
  useEffect(() => {
    if (!canManage) return
    post(`/api/projects/${id}/teammate-suggestions`, {}).then(setSuggestions).catch(() => {})
    get('/api/skills').catch(() => {})
  }, [id, canManage])

  return (
    <div className="grid gap-5 lg:grid-cols-2">
      <Panel title={`Team (${data.members?.length || 0})`}>
        <ul className="divide-y divide-line">
          {(data.members || []).map(m => (
            <li key={m.userId} className="flex items-center justify-between gap-2 py-2.5">
              <Link to={`/students/${m.userId}`} className="flex items-center gap-2 no-underline">
                <span className="grid h-9 w-9 place-items-center rounded-full bg-soft text-xs font-bold text-brand">
                  {m.fullName.split(' ').map(x => x[0]).slice(0, 2).join()}
                </span>
                <span><strong className="text-ink">{m.fullName}</strong>
                  <div className="text-xs text-muted">{m.email}{m.projectRoleTitle ? ` · ${m.projectRoleTitle}` : ''}</div></span>
              </Link>
              <div className="flex items-center gap-2">
                <StatusBadge value={m.role} />
                {isOwner && m.role !== 'OWNER' &&
                  <button className="btn-ghost btn-sm" onClick={() => act(() => post(`/api/projects/${id}/members/${m.userId}/promote`, {}), 'Promoted to manager ⭐')}>Promote</button>}
                {canManage && m.role !== 'OWNER' &&
                  <button className="btn-danger btn-sm" onClick={() => act(() => del(`/api/projects/${id}/members/${m.userId}`), 'Member removed')}>Remove</button>}
              </div>
            </li>
          ))}
          {data.members?.length === 0 && <EmptyState text="Members appear here once join requests are accepted." />}
        </ul>
      </Panel>
      <div className="grid gap-5">
        {canManage && (
          <Panel title="Invite a suggested student (U2/F5)">
            <div className="max-h-72 space-y-2 overflow-y-auto pr-1">
              {suggestions.map(s => (
                <div key={s.userId} className="flex items-center justify-between gap-2 rounded-xl bg-soft px-3 py-2 text-sm">
                  <div><strong>{s.fullName}</strong><div className="text-xs text-muted">{s.reason}</div></div>
                  <button className="btn-primary btn-sm" onClick={() => act(() => post(`/api/projects/${id}/invitations`, { userId: s.userId, message: `` }), 'Invitation sent ✉️')}>Invite</button>
                </div>
              ))}
              {suggestions.length === 0 && <EmptyState icon="💡" text="Everyone here is already part of the team." />}
            </div>
          </Panel>
        )}
      </div>
    </div>
  )
}

function Chat({ id, isMember }) {
  const [messages, setMessages] = useState([])
  const [body, setBody] = useState('')
  useEffect(() => {
    if (!isMember) return
    get(`/api/projects/${id}/messages`).then(setMessages).catch(() => {})
  }, [id, isMember])
  if (!isMember) return <EmptyState icon="🔒" text="Project chat is private to approved members." />

  const send = async (e) => {
    e.preventDefault()
    try {
      await post(`/api/projects/${id}/messages`, { body })
      setBody('')
      setMessages(await get(`/api/projects/${id}/messages`))
    } catch (ex) { console.log(ex) }
  }

  return (
    <Panel title="Project chat">
      <div className="mb-4 max-h-96 space-y-3 overflow-y-auto rounded-xl bg-pagebg p-4">
        {messages.map(m => (
          <div key={m.id} className="text-sm">
            <strong className="text-ink">{m.senderName}</strong>
            <span className="ml-2 text-xs text-muted">{new Date(m.createdAt).toLocaleString()}</span>
            <p className="mt-0.5 whitespace-pre-line">{m.body}</p>
          </div>
        ))}
        {messages.length === 0 && <EmptyState icon="💬" text="Say hi to your team!" />}
      </div>
      <form className="flex gap-2" onSubmit={send}>
        <input className="field-input" placeholder="Message the team…" value={body}
               onChange={(e) => setBody(e.target.value)} required maxLength={1000} />
        <button className="btn-primary">Send</button>
      </form>
    </Panel>
  )
}

function Files({ id, isMember, tasks, load, uploadTaskId, setUploadTaskId }) {
  const toast = useToast()
  const [files, setFiles] = useState([])
  useEffect(() => {
    if (!isMember) return
    get(`/api/projects/${id}/files`).then(setFiles).catch(() => {})
  }, [id, isMember])
  if (!isMember) return <EmptyState icon="🔒" text="Shared files are only visible to approved members." />

  const upload = async (e) => {
    e.preventDefault()
    const form = new FormData(e.target)
    try {
      await post(`/api/projects/${id}/files`, form)
      toast('File uploaded 📎')
      setFiles(await get(`/api/projects/${id}/files`))
    } catch (ex) { toast({ text: ex.message }) }
  }
  const remove = async (fileId) => {
    try { await del(`/api/files/${fileId}`); setFiles(await get(`/api/projects/${id}/files`)) } catch (ex) { toast({ text: ex.message }) }
  }
  // Download needs the auth header, so fetch it as a blob instead of a plain link.
  const download = async (f) => {
    try {
      const res = await api(`/api/files/${f.id}/download`, { raw: true })
      if (!res.ok) throw new Error(`Download failed (${res.status})`)
      const blob = await res.blob()
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = f.originalName || 'file'
      document.body.appendChild(a)
      a.click()
      a.remove()
      URL.revokeObjectURL(url)
    } catch (ex) { toast({ text: ex.message }) }
  }

  return (
    <div className="grid gap-5 lg:grid-cols-[1fr_1.4fr]">
      <Panel title="Upload a file">
        <form onSubmit={upload}>
          <Field label="Optional: link it to a task">
            <select className="field-input" value={uploadTaskId} onChange={(e) => setUploadTaskId(e.target.value)}>
              <option value="">— no specific task —</option>
              {tasks.map(t => <option key={t.id} value={t.id}>{t.title}</option>)}
            </select>
          </Field>
          <input className="field-input" type="file" name="file" required aria-label="Choose file" />
          {uploadTaskId && <input type="hidden" name="taskId" value={uploadTaskId} />}
          <button className="btn-primary mt-3">Upload</button>
        </form>
        <p className="mt-3 text-[11px] text-[#8993a5]">Allowed: pdf, doc(x), xls(x), ppt(x), txt, md, csv, images, zip — up to 10 MB.</p>
      </Panel>
      <Panel title={`Shared files (${files.length})`}>
        {files.length === 0 ? <EmptyState icon="📎" text="No files shared yet." />
          : <ul className="divide-y divide-line text-sm">
            {files.map(f => (
              <li key={f.id} className="flex items-center justify-between gap-3 py-2">
                <div>
                  <strong className="text-ink">{f.originalName}</strong>
                  <div className="text-xs text-muted">by {f.uploaderName} · {f.sizeBytes < 1024 ? `${f.sizeBytes} B` : `${(f.sizeBytes / 1024).toFixed(0)} KB`}{f.taskId ? ` · task #${f.taskId}` : ''}</div>
                </div>
                <div className="flex gap-2">
                  <button className="btn-ghost btn-sm" onClick={() => download(f)}>Download</button>
                  <button className="btn-danger btn-sm" onClick={() => remove(f.id)}>✕</button>
                </div>
              </li>
            ))}
          </ul>}
      </Panel>
    </div>
  )
}

function TasksTab({ tasks, navigate }) {
  const columns = ['TODO', 'IN_PROGRESS', 'BLOCKED', 'READY_FOR_REVIEW', 'APPROVED']
  return (
    <div className="overflow-x-auto pb-2">
      <div className="flex min-w-[900px] gap-4">
        {columns.map(col => (
          <div key={col} className="flex-1 rounded-2xl bg-surface/60 p-2">
            <div className="mb-2 flex items-center justify-between px-2 py-1">
              <strong className="text-xs font-bold tracking-wide text-[#7a8599]">{col.replace(/_/g, ' ')}</strong>
              <span className="badge badge-sky">{tasks.filter(t => t.status === col).length}</span>
            </div>
            <div className="space-y-2">
              {tasks.filter(t => t.status === col).map(t => (
                <button key={t.id} onClick={() => navigate(`/tasks?project=${t.projectId}&task=${t.id}`)}
                        className="panel w-full p-3 text-left transition hover:-translate-y-px">
                  <div className="mb-1.5 flex items-start justify-between gap-2">
                    <strong className="text-sm text-ink">{t.title}</strong>
                    <StatusBadge value={t.priority} />
                  </div>
                  <div className="text-xs text-muted">{t.assigneeName ? `👤 ${t.assigneeName}` : 'unassigned'}{t.dueDate ? ` · ⏰ ${t.dueDate}` : ''}</div>
                  {t.blockerNote && <div className="mt-1 text-[11px] text-[#c34d4d]">🚧 {t.blockerNote}</div>}
                </button>
              ))}
              {tasks.filter(t => t.status === col).length === 0 && <div className="p-3 text-center text-xs text-muted">—</div>}
            </div>
          </div>
        ))}
      </div>
    </div>
  )
}

function ProgressTab({ data }) {
  const pr = data.progress
  if (!pr) return <EmptyState icon="🔒" text="Progress is visible once you join the project." />
  const cell = (label, value, accent) => (
    <div className="rounded-xl bg-pagebg p-3 text-center">
      <p className={`font-display text-2xl font-extrabold ${accent}`}>{value}</p>
      <p className="text-[11px] font-bold uppercase tracking-wide text-[#7a8599]">{label}</p>
    </div>
  )
  return (
    <>
      <Panel title="Project health (U5 / F9)">
        <div className="mb-4">
          <div className="mb-1 flex justify-between text-sm font-semibold">
            <span>Overall progress</span><span className="text-brand">{pr.percentComplete}%</span>
          </div>
          <div className="h-3 rounded-full bg-soft">
            <div className="h-3 rounded-full bg-gradient-to-r from-[#4268ef] to-[#263fae] transition-all" style={{ width: `${pr.percentComplete}%` }} />
          </div>
        </div>
        <div className="grid grid-cols-2 gap-3 sm:grid-cols-4 lg:grid-cols-6">
          {cell('Tasks', pr.totalTasks)}
          {cell('To do', pr.todo, 'text-brand')}
          {cell('In progress', pr.inProgress, 'text-[#a96a16]')}
          {cell('Blocked', pr.blocked, pr.blocked ? 'text-[#c34d4d]' : 'text-ink')}
          {cell('Ready to review', pr.readyForReview, pr.readyForReview ? 'text-[#a96a16]' : 'text-ink')}
          {cell('Approved', pr.approved, 'text-[#17845c]')}
          {cell('Overdue', pr.overdue, pr.overdue ? 'text-[#c34d4d]' : 'text-ink')}
          {cell('Reopened', pr.reopened)}
          {cell('Cancelled', pr.cancelled)}
        </div>
      </Panel>
      <div className="mt-5 grid gap-5 lg:grid-cols-2">
        <Panel title="Upcoming work">
          {pr.upcomingDeadlines?.length
            ? <ul className="divide-y divide-line text-sm">{pr.upcomingDeadlines.map(d => (
              <li key={d.taskId} className="py-2"><strong>{d.title}</strong><div className="text-xs text-muted">{d.dueState}</div></li>
            ))}</ul>
            : <EmptyState icon="🌤️" text="No live deadlines right now." />}
        </Panel>
        <Panel title="Milestones">
          {pr.milestones?.length
            ? <ul className="space-y-3 text-sm">{pr.milestones.map(m => (
              <li key={m.milestoneId}>
                <div className="mb-1 flex justify-between"><strong>{m.title}</strong><StatusBadge value={m.status} /></div>
                <div className="h-2 rounded-full bg-soft">
                  <div className="h-2 rounded-full bg-teal" style={{ width: `${m.totalTasks ? (m.completedTasks / m.totalTasks) * 100 : 0}%` }} />
                </div>
                <span className="text-xs text-muted">{m.completedTasks}/{m.totalTasks} tasks approved</span>
              </li>
            ))}</ul>
            : <EmptyState text="No milestones yet." />}
        </Panel>
      </div>
    </>
  )
}
