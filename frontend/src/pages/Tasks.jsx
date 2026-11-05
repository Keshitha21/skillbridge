import { useEffect, useMemo, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { PageHeading, Panel, StatusBadge, Field, EmptyState, useToast, Spinner } from '../components'
import { get, post, put } from '../api'

const NEXT = {
  TODO: ['IN_PROGRESS', 'BLOCKED'],
  IN_PROGRESS: ['READY_FOR_REVIEW', 'BLOCKED'],
  BLOCKED: ['IN_PROGRESS'],
  REOPENED: ['IN_PROGRESS', 'BLOCKED'],
  READY_FOR_REVIEW: [],
  APPROVED: ['READY_FOR_REVIEW'],
  CANCELLED: [],
}

/** U4 / F7–F8 — boards for my projects: create, assign, move status, submit for review, review, comments. */
export default function Tasks() {
  const toast = useToast()
  const [params, setParams] = useSearchParams()
  const [managed, setManaged] = useState([])
  const [mine, setMine] = useState([])
  const [projectIds, setProjectIds] = useState([])
  const activeProject = params.get('project')
  const [rows, setRows] = useState(null)
  const [form, setForm] = useState(null) // create/edit form when open
  const [review, setReview] = useState(null) // { task }
  const [commentTask, setCommentTask] = useState(null)
  const [comments, setComments] = useState([])

  useEffect(() => {
    Promise.allSettled([get('/api/tasks/managed'), get('/api/tasks/mine'), get('/api/projects?mine=true')])
      .then(([m, mine2, p]) => {
        if (m.status === 'fulfilled') setManaged(m.value)
        if (mine2.status === 'fulfilled') setMine(mine2.value)
        if (p.status === 'fulfilled') setProjectIds(p.value)
      })
  }, [])

  useEffect(() => {
    if (!activeProject) return setRows(null)
    get(`/api/projects/${activeProject}/tasks`).then(setRows).catch(() => setRows([]))
  }, [activeProject])

  const boardTasks = useMemo(() => {
    if (activeProject) return rows || []
    return managed.concat(mine).filter((t, i, all) => all.findIndex(x => x.id === t.id) === i)
  }, [activeProject, rows, managed, mine])

  const byProject = (t) => projectIds.find(p => p.id === t.projectId)

  const move = async (task, status, blockerNote) => {
    try {
      await post(`/api/tasks/${task.id}/status`, { status, blockerNote })
      toast(`Task moved to ${status.replace(/_/g, ' ').toLowerCase()}`)
      activeProject ? setRows(await get(`/api/projects/${activeProject}/tasks`)) : window.location.reload()
    } catch (ex) { toast({ text: ex.message }) }
  }

  const reloadBoard = async () => {
    if (activeProject) setRows(await get(`/api/projects/${activeProject}/tasks`))
  }

  const decideReview = async (decision, comment) => {
    try {
      await post(`/api/tasks/${review.id}/review`, { decision, comment })
      setReview(null)
      toast(decision === 'APPROVED' ? 'Approved 🎉' : 'Changes requested')
      reloadBoard()
    } catch (ex) { toast({ text: ex.message }) }
  }

  const openComments = async (task) => {
    setCommentTask(task)
    setComments(await get(`/api/tasks/${task.id}/comments`).catch(() => []))
  }

  const sendComment = async (e) => {
    e.preventDefault()
    const body = new FormData(e.target).get('body')
    try {
      await post(`/api/tasks/${commentTask.id}/comments`, { body })
      setComments(await get(`/api/tasks/${commentTask.id}/comments`))
      e.target.reset()
    } catch (ex) { toast({ text: ex.message }) }
  }

  const columns = activeProject
    ? ['TODO', 'IN_PROGRESS', 'BLOCKED', 'READY_FOR_REVIEW', 'APPROVED']
    : ['TODO', 'IN_PROGRESS', 'BLOCKED', 'READY_FOR_REVIEW']
  const canManage = (t) => t.myManagedRole !== undefined // managed lists come with manager rights

  return (
    <>
      <PageHeading title="Tasks" lead="Plan, assign and review work — U4 / F7–F8."
        action={activeProject &&
          <>
            <button className="btn-secondary btn-sm" onClick={() => setParams({})}>My whole view</button>
            <button className="btn-primary btn-sm" onClick={() => setForm({ NEW: true, projectId: activeProject })}>+ New task</button>
          </>} />

      {!activeProject && (
        <div className="panel mb-6">
          <h3 className="panel-title mb-2 text-base">Focus a project board</h3>
          <div className="flex flex-wrap gap-2">
            {projectIds.map(p => (
              <button key={p.id} className="btn-ghost btn-sm" onClick={() => setParams({ project: String(p.id) })}>
                {p.title} <StatusBadge value={p.status} />
              </button>
            ))}
            {projectIds.length === 0 && <span className="text-sm text-muted">Join or create a project to see task boards.</span>}
          </div>
        </div>
      )}

      {form && <TaskForm form={form} setForm={setForm} close={() => { setForm(null); reloadBoard() }} />}
      {review && (
        <Panel title={`Review: ${review.title}`} className="mb-6">
          {review.acceptanceCriteria && <p className="mb-3 text-sm text-muted">Acceptance criteria: {review.acceptanceCriteria}</p>}
          <form onSubmit={(e) => { e.preventDefault(); decideReview(new FormData(e.target).get('decision'), new FormData(e.target).get('comment')) }}>
            <div className="grid gap-4 md:grid-cols-2">
              <Field label="Decision">
                <select className="field-input" name="decision"><option value="APPROVED">Approve work</option><option value="CHANGES_REQUESTED">Request changes</option></select>
              </Field>
              <Field label="Comment">
                <input className="field-input" name="comment" placeholder="Looks great — or what needs changing" />
              </Field>
            </div>
            <div className="flex gap-2">
              <button className="btn-primary">Submit decision</button>
              <button type="button" className="btn-secondary" onClick={() => setReview(null)}>Cancel</button>
            </div>
          </form>
        </Panel>
      )}

      {rows === null && activeProject ? <Spinner /> : (
        <div className="overflow-x-auto pb-2">
          <div className="flex min-w-[940px] gap-4">
            {columns.map(col => (
              <div key={col} className="flex-1">
                <div className="mb-2 flex items-center justify-between px-1">
                  <strong className="text-xs font-bold tracking-wide text-[#7a8599]">{col.replace(/_/g, ' ')}</strong>
                  <span className="badge badge-sky">{boardTasks.filter(t => t.status === col).length}</span>
                </div>
                <div className="space-y-2">
                  {boardTasks.filter(t => t.status === col).map(t => (
                    <div key={t.id} className="panel p-3">
                      <div className="mb-1 flex items-start justify-between gap-2">
                        <strong className="text-sm text-ink">{t.title}</strong>
                        <div className="flex gap-1">
                          <StatusBadge value={t.priority} />
                          <span className="badge badge-sky">{t.projectTitle?.split(' ').map(w => w[0]).slice(0, 2).join('') || ''}</span>
                        </div>
                      </div>
                      <div className="text-xs text-muted">{t.assigneeName ? `👤 ${t.assigneeName}` : '🪹 unassigned'}{t.dueDate ? ` · ⏰ ${t.dueDate}` : ''}</div>
                      {t.blockerNote && <div className="mt-1 text-[11px] text-[#c34d4d]">🚧 {t.blockerNote}</div>}
                      <div className="mt-2 flex flex-wrap gap-1.5">
                        {(t.status === 'READY_FOR_REVIEW')
                          ? <button className="btn-primary btn-sm" onClick={() => setReview(t)}>Review</button>
                          : NEXT[t.status]?.map(s => (
                            <button key={s} className="btn-ghost btn-sm" onClick={() => {
                              if (s === 'BLOCKED') {
                                const note = prompt('Describe the blocker:', '')
                                if (note) move(t, s, note)
                              } else move(t, s)
                            }}>{s.replace(/_/g, ' ').toLowerCase()}</button>
                          ))}
                        <button className="btn-secondary btn-sm" onClick={() => openComments(t)}>💬 {t.status === 'READY_FOR_REVIEW' ? '' : ''}Comments</button>
                        {projectIds.find(p => p.id === t.projectId)?.myRole === 'OWNER' || projectIds.find(p => p.id === t.projectId)?.myRole === 'MANAGER'
                          ? <button className="btn-secondary btn-sm" onClick={() => setForm(t)}>edit</button>
                          : null}
                      </div>
                    </div>
                  ))}
                  {boardTasks.filter(t => t.status === col).length === 0 &&
                    <div className="panel p-3 text-center text-xs text-muted">—</div>}
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {commentTask && (
        <div className="fixed inset-0 z-40 grid place-items-center bg-black/30 p-4" onClick={() => setCommentTask(null)}>
          <div className="panel w-[min(560px,95vw)] max-h-[80vh] overflow-y-auto" onClick={(e) => e.stopPropagation()}>
            <h3 className="panel-title mb-3">{commentTask.title} — comments</h3>
            <div className="mb-4 space-y-3">
              {comments.map(c => (
                <div key={c.id} className="rounded-xl bg-pagebg p-3 text-sm">
                  <strong>{c.authorName}</strong>
                  <p className="mt-1">{c.body}</p>
                </div>
              ))}
              {comments.length === 0 && <EmptyState icon="💬" text="No comments yet." />}
            </div>
            <form className="flex gap-2" onSubmit={sendComment}>
              <input className="field-input" name="body" placeholder="Add a comment…" required maxLength={1000} />
              <button className="btn-primary">Post</button>
            </form>
          </div>
        </div>
      )}
    </>
  )
}

function TaskForm({ form, setForm, close }) {
  const toast = useToast()
  const isEdit = !form.NEW
  const [skills, setSkills] = useState([])
  const [members, setMembers] = useState([])
  const [suggestions, setSuggestions] = useState([])
  const [value, setValue] = useState(isEdit ? { ...form } : {
    projectId: form.projectId || Number(new URLSearchParams(window.location.search).get('project')), title: '',
    description: '', acceptanceCriteria: '', priority: 'MEDIUM', milestoneId: '', assigneeId: '', dependsOnId: '', dueDate: '',
  })
  const [error, setError] = useState('')

  useEffect(() => {
    if (!value.projectId) return
    get(`/api/projects/${value.projectId}/tasks`).catch(() => {})
    get(`/api/projects/${value.projectId}/milestones`).catch(() => {})
    Promise.allSettled([
      get(`/api/projects/${value.projectId}/members`),
      get(`/api/projects/${value.projectId}/milestones`),
      get(`/api/projects/${value.projectId}/tasks`),
      get(`/api/projects/${value.projectId}/assignee-suggestions`),
    ]).then(([m, ms, t, s]) => {
      if (m.status === 'fulfilled') setMembers(m.value)
      if (ms.status === 'fulfilled') setValue(v => ({ ...v, _milestones: ms.value }))
      if (t.status === 'fulfilled') setValue(v => ({ ...v, _tasks: t.value }))
      if (s.status === 'fulfilled') setSuggestions(s.value)
    })
  }, [value.projectId])

  const save = async (e) => {
    e.preventDefault()
    setError('')
    const body = {
      projectId: Number(value.projectId),
      title: value.title, description: value.description, acceptanceCriteria: value.acceptanceCriteria,
      priority: value.priority,
      milestoneId: value.milestoneId || null,
      assigneeId: value.assigneeId ? Number(value.assigneeId) : null,
      dependsOnId: value.dependsOnId ? Number(value.dependsOnId) : null,
      dueDate: value.dueDate || null,
    }
    try {
      if (isEdit) await put(`/api/tasks/${form.id}`, body)
      else await post(`/api/projects/${value.projectId}/tasks`, body)
      toast(isEdit ? 'Task updated ✅' : 'Task created 📋')
      close()
    } catch (ex) { setError(ex.message) }
  }

  return (
    <Panel title={isEdit ? 'Edit task' : 'Create task'} className="mb-6">
      {!isEdit && suggestions.length > 0 && (
        <div className="mb-4 rounded-xl bg-soft p-3 text-sm">
          <strong className="block">💡 Suggested assignees (skill match)</strong>
          <div className="mt-2 flex flex-wrap gap-2">
            {suggestions.map(s => (
              <button type="button" key={s.userId} className="btn-ghost btn-sm"
                      onClick={() => setValue({ ...value, assigneeId: s.userId })}
                      title={s.reason}>{s.fullName}</button>
            ))}
          </div>
        </div>
      )}
      <form onSubmit={save} className="grid gap-4 md:grid-cols-2">
        {isEdit && <div className="md:col-span-2 rounded-xl bg-soft px-3 py-2 text-xs">Project: <strong>{form.projectTitle || `#${form.projectId}`}</strong></div>}
        <Field label="Title">
          <input className="field-input" required value={value.title} onChange={(e) => setValue({ ...value, title: e.target.value })} />
        </Field>
        <Field label="Priority">
          <select className="field-input" value={value.priority} onChange={(e) => setValue({ ...value, priority: e.target.value })}>
            <option>LOW</option><option>MEDIUM</option><option>HIGH</option>
          </select>
        </Field>
        <Field label="Due date">
          <input className="field-input" type="date" value={value.dueDate || ''} onChange={(e) => setValue({ ...value, dueDate: e.target.value })} />
        </Field>
        <Field label="Assignee" hint="Only project members can be assigned.">
          <select className="field-input" value={value.assigneeId || ''} onChange={(e) => setValue({ ...value, assigneeId: e.target.value })}>
            <option value="">— unassigned —</option>
            {members.map(m => <option key={m.userId} value={m.userId}>{m.fullName}</option>)}
          </select>
        </Field>
        <Field label="Milestone">
          <select className="field-input" value={value.milestoneId || ''} onChange={(e) => setValue({ ...value, milestoneId: e.target.value })}>
            <option value="">— none —</option>
            {(value._milestones || []).map(m => <option key={m.id} value={m.id}>{m.title}</option>)}
          </select>
        </Field>
        <Field label="Depends on" hint="Circular chains are rejected by the backend.">
          <select className="field-input" value={value.dependsOnId || ''} onChange={(e) => setValue({ ...value, dependsOnId: e.target.value })}>
            <option value="">— nothing —</option>
            {(value._tasks || []).filter(t => !isEdit || t.id !== form.id).map(t => <option key={t.id} value={t.id}>{t.title}</option>)}
          </select>
        </Field>
        <div className="md:col-span-2">
          <Field label="Description">
            <textarea className="field-input" rows={2} value={value.description || ''} onChange={(e) => setValue({ ...value, description: e.target.value })} />
          </Field>
          <Field label="Acceptance criteria">
            <input className="field-input" value={value.acceptanceCriteria || ''} onChange={(e) => setValue({ ...value, acceptanceCriteria: e.target.value })} placeholder="What 'done' looks like" />
          </Field>
        </div>
        {error && <p className="md:col-span-2 rounded-lg bg-[#ffeded] px-3 py-2 text-sm text-[#c34d4d]">{error}</p>}
        <div className="md:col-span-2 flex gap-2">
          <button className="btn-primary">{isEdit ? 'Save changes' : 'Create task'}</button>
          <button type="button" className="btn-secondary" onClick={close}>Cancel</button>
        </div>
      </form>
    </Panel>
  )
}
